#!/usr/bin/env python3
"""
Gemma-based SMS Classification System

This module implements SMS classification using Google's Gemma model,
completely separate from the Android app for independent development and testing.
"""

import torch
from transformers import AutoTokenizer, AutoModelForCausalLM, GenerationConfig
import yaml
import logging
import json
import re
from pathlib import Path
from typing import Dict, List, Tuple, Optional
from dataclasses import dataclass
from datetime import datetime

@dataclass
class ClassificationResult:
    """Result of SMS classification"""
    category: str
    confidence: float
    reasoning: str
    processing_time_ms: float

class GemmaSmsClassifier:
    """SMS classifier using Google's Gemma model"""
    
    def __init__(self, config_path: str = "config/config.yaml"):
        """
        Initialize the Gemma SMS classifier
        
        Args:
            config_path: Path to configuration file
        """
        self.config = self._load_config(config_path)
        self.logger = self._setup_logging()
        
        self.tokenizer = None
        self.model = None
        self.generation_config = None
        
        # Classification categories
        self.categories = self.config['classification']['categories']
        self.confidence_threshold = self.config['classification']['confidence_threshold']
        self.review_threshold = self.config['classification']['review_threshold']
        
        self.logger.info("Gemma SMS Classifier initialized")
    
    def _load_config(self, config_path: str) -> Dict:
        """Load configuration from YAML file"""
        with open(config_path, 'r') as f:
            return yaml.safe_load(f)
    
    def _setup_logging(self) -> logging.Logger:
        """Setup logging configuration"""
        logging.basicConfig(
            level=getattr(logging, self.config['logging']['level']),
            format=self.config['logging']['format'],
            handlers=[
                logging.FileHandler(self.config['logging']['file']),
                logging.StreamHandler()
            ]
        )
        return logging.getLogger(__name__)
    
    def load_model(self) -> None:
        """Load the Gemma model and tokenizer"""
        try:
            self.logger.info(f"Loading Gemma model: {self.config['model']['name']}")
            
            # Load tokenizer
            self.tokenizer = AutoTokenizer.from_pretrained(
                self.config['model']['name'],
                trust_remote_code=True
            )
            
            # Add padding token if not present
            if self.tokenizer.pad_token is None:
                self.tokenizer.pad_token = self.tokenizer.eos_token
            
            # Load model with specified configurations
            model_kwargs = {
                "trust_remote_code": True,
                "torch_dtype": torch.float16 if torch.cuda.is_available() else torch.float32,
            }
            
            # Add quantization if specified
            if self.config['model']['load_in_8bit']:
                model_kwargs['load_in_8bit'] = True
            elif self.config['model']['load_in_4bit']:
                model_kwargs['load_in_4bit'] = True
            
            self.model = AutoModelForCausalLM.from_pretrained(
                self.config['model']['name'],
                **model_kwargs
            )
            
            # Setup generation configuration
            self.generation_config = GenerationConfig(
                max_new_tokens=100,
                temperature=self.config['model']['temperature'],
                do_sample=self.config['model']['do_sample'],
                pad_token_id=self.tokenizer.eos_token_id,
                eos_token_id=self.tokenizer.eos_token_id,
            )
            
            # Move to device
            device = self.config['model']['device']
            if device == "cpu" and torch.cuda.is_available():
                device = "cuda"
            
            if device != "cpu":
                self.model.to(device)
            
            self.logger.info(f"Model loaded successfully on device: {device}")
            
        except Exception as e:
            self.logger.error(f"Failed to load model: {e}")
            raise
    
    def _create_classification_prompt(self, sms_text: str, sender: str = "Unknown") -> str:
        """
        Create a structured prompt for SMS classification
        
        Args:
            sms_text: The SMS message content
            sender: The sender information
            
        Returns:
            Formatted prompt for the model
        """
        prompt = f\"\"\"You are an expert SMS classifier. Your task is to classify SMS messages into one of these categories:

INBOX: Important personal or business messages (legitimate communications)
SPAM: Promotional, marketing, or unwanted messages 
NEEDS_REVIEW: Messages that are unclear or require manual review

Analyze this SMS message and provide your classification:

Sender: {sender}
Message: "{sms_text}"

Please respond in this exact JSON format:
{{
    "category": "INBOX/SPAM/NEEDS_REVIEW",
    "confidence": 0.95,
    "reasoning": "Brief explanation of why you chose this category"
}}

Classification:\"\"\"

        return prompt
    
    def _parse_model_response(self, response_text: str) -> Dict:
        """
        Parse the model's response to extract classification result
        
        Args:
            response_text: Raw response from the model
            
        Returns:
            Parsed classification result
        """
        try:
            # Try to find JSON in the response
            json_match = re.search(r'\{[^{}]*\}', response_text)
            if json_match:
                json_str = json_match.group()
                result = json.loads(json_str)
                
                # Validate required fields
                if all(key in result for key in ['category', 'confidence', 'reasoning']):
                    # Ensure category is valid
                    if result['category'] not in self.categories:
                        result['category'] = 'NEEDS_REVIEW'
                    
                    # Ensure confidence is a float between 0 and 1
                    try:
                        result['confidence'] = min(1.0, max(0.0, float(result['confidence'])))
                    except (ValueError, TypeError):
                        result['confidence'] = 0.5
                    
                    return result
        
        except Exception as e:
            self.logger.warning(f"Failed to parse model response: {e}")
        
        # Fallback parsing
        response_lower = response_text.lower()
        
        # Simple keyword-based fallback
        if any(keyword in response_lower for keyword in ['spam', 'promotional', 'marketing']):
            category = 'SPAM'
            confidence = 0.6
        elif any(keyword in response_lower for keyword in ['important', 'inbox', 'legitimate']):
            category = 'INBOX'
            confidence = 0.6
        else:
            category = 'NEEDS_REVIEW'
            confidence = 0.3
        
        return {
            'category': category,
            'confidence': confidence,
            'reasoning': 'Fallback classification due to parsing error'
        }
    
    def classify_sms(self, sms_text: str, sender: str = "Unknown") -> ClassificationResult:
        """
        Classify an SMS message using the Gemma model
        
        Args:
            sms_text: The SMS message content
            sender: The sender information (optional)
            
        Returns:
            Classification result
        """
        if not self.model or not self.tokenizer:
            raise RuntimeError("Model not loaded. Call load_model() first.")
        
        start_time = datetime.now()
        
        try:
            # Create the prompt
            prompt = self._create_classification_prompt(sms_text, sender)
            
            # Tokenize the input
            inputs = self.tokenizer(
                prompt,
                return_tensors="pt",
                truncation=True,
                max_length=self.config['model']['max_length']
            )
            
            # Move inputs to the same device as the model
            if next(self.model.parameters()).device.type != "cpu":
                inputs = {k: v.to(next(self.model.parameters()).device) for k, v in inputs.items()}
            
            # Generate response
            with torch.no_grad():
                outputs = self.model.generate(
                    **inputs,
                    generation_config=self.generation_config,
                    pad_token_id=self.tokenizer.pad_token_id
                )
            
            # Decode the response
            response = self.tokenizer.decode(
                outputs[0][inputs['input_ids'].shape[1]:],
                skip_special_tokens=True
            )
            
            # Parse the response
            parsed_result = self._parse_model_response(response)
            
            # Apply confidence thresholds
            if parsed_result['confidence'] < self.review_threshold:
                parsed_result['category'] = 'NEEDS_REVIEW'
                parsed_result['reasoning'] += ' (Low confidence)'
            
            # Calculate processing time
            processing_time = (datetime.now() - start_time).total_seconds() * 1000
            
            result = ClassificationResult(
                category=parsed_result['category'],
                confidence=parsed_result['confidence'],
                reasoning=parsed_result['reasoning'],
                processing_time_ms=processing_time
            )
            
            self.logger.info(f"SMS classified as {result.category} with confidence {result.confidence:.2f}")
            
            return result
            
        except Exception as e:
            self.logger.error(f"Classification failed: {e}")
            
            # Fallback result
            processing_time = (datetime.now() - start_time).total_seconds() * 1000
            return ClassificationResult(
                category='NEEDS_REVIEW',
                confidence=0.0,
                reasoning=f'Classification failed: {str(e)}',
                processing_time_ms=processing_time
            )
    
    def classify_batch(self, sms_list: List[Dict[str, str]]) -> List[ClassificationResult]:
        """
        Classify multiple SMS messages
        
        Args:
            sms_list: List of dictionaries with 'text' and 'sender' keys
            
        Returns:
            List of classification results
        """
        results = []
        
        for sms in sms_list:
            result = self.classify_sms(
                sms_text=sms.get('text', ''),
                sender=sms.get('sender', 'Unknown')
            )
            results.append(result)
        
        return results
    
    def get_model_info(self) -> Dict:
        """Get information about the loaded model"""
        if not self.model:
            return {"status": "Model not loaded"}
        
        device = next(self.model.parameters()).device
        num_parameters = sum(p.numel() for p in self.model.parameters())
        
        return {
            "model_name": self.config['model']['name'],
            "device": str(device),
            "num_parameters": f"{num_parameters:,}",
            "categories": self.categories,
            "confidence_threshold": self.confidence_threshold,
            "review_threshold": self.review_threshold
        }

def main():
    """Test the Gemma SMS classifier"""
    print("🚀 Initializing Gemma SMS Classifier")
    
    # Initialize classifier
    classifier = GemmaSmsClassifier()
    
    # Load model
    print("📥 Loading Gemma model...")
    classifier.load_model()
    
    # Test messages
    test_messages = [
        {
            "text": "🎉 CONGRATULATIONS! You've won $1000! Click here to claim: bit.ly/claim123",
            "sender": "PROMO123"
        },
        {
            "text": "Hi, are you free for lunch today? Let me know!",
            "sender": "+1234567890"
        },
        {
            "text": "Your OTP is 123456. Valid for 10 minutes. Do not share this code.",
            "sender": "BANK-AUTH"
        },
        {
            "text": "Limited time offer! 90% discount on everything! Buy now!",
            "sender": "SALES"
        }
    ]
    
    print("\n📱 Testing SMS Classification:")
    print("=" * 50)
    
    # Classify each message
    for i, msg in enumerate(test_messages, 1):
        print(f"\nTest {i}:")
        print(f"Sender: {msg['sender']}")
        print(f"Message: {msg['text'][:60]}...")
        
        result = classifier.classify_sms(msg['text'], msg['sender'])
        
        print(f"Category: {result.category}")
        print(f"Confidence: {result.confidence:.2f}")
        print(f"Reasoning: {result.reasoning}")
        print(f"Processing: {result.processing_time_ms:.0f}ms")
    
    # Show model info
    print("\n" + "=" * 50)
    print("📊 Model Information:")
    model_info = classifier.get_model_info()
    for key, value in model_info.items():
        print(f"{key}: {value}")
    
    print("\n✅ Testing completed!")

if __name__ == "__main__":
    main()