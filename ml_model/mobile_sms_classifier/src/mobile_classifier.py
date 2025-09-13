#!/usr/bin/env python3
"""
Mobile SMS Classification System
Optimized for smartphone deployment with TensorFlow Lite support.
Target: <20MB model size, <100ms inference time
"""

import tensorflow as tf
from transformers import (
    DistilBertTokenizer, 
    TFDistilBertForSequenceClassification,
    DistilBertConfig
)
import numpy as np
import pandas as pd
import json
import logging
import time
from pathlib import Path
from typing import Dict, List, Tuple, Optional, Union
from dataclasses import dataclass
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, confusion_matrix
import matplotlib.pyplot as plt
import seaborn as sns

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

@dataclass
class MobileClassificationResult:
    """Result of mobile SMS classification"""
    category: str
    category_id: int
    confidence: float
    probabilities: Dict[str, float]
    processing_time_ms: float
    model_version: str

class MobileSmsClassifier:
    """
    Smartphone-optimized SMS classifier using DistilBERT
    Designed for TensorFlow Lite deployment on Android devices
    """
    
    # SMS Categories with Indian-specific focus
    CATEGORIES = {
        'INBOX': 0,      # Important personal/business messages
        'SPAM': 1,       # Promotional/spam messages
        'OTP': 2,        # OTP and verification codes
        'BANKING': 3,    # Banking and financial messages
        'ECOMMERCE': 4,  # Shopping and delivery notifications
        'NEEDS_REVIEW': 5 # Uncertain messages
    }
    
    CATEGORY_NAMES = list(CATEGORIES.keys())
    
    def __init__(self, model_name: str = "distilbert-base-uncased"):
        """
        Initialize mobile SMS classifier
        
        Args:
            model_name: Base model to use (DistilBERT recommended for mobile)
        """
        self.model_name = model_name
        self.max_length = 128  # Optimal for SMS (most are <160 chars)
        self.model = None
        self.tokenizer = None
        self.tflite_model = None
        self.model_version = "1.0.0"
        
        # Performance tracking
        self.training_history = None
        self.evaluation_metrics = None
        
        logger.info(f"Mobile SMS Classifier initialized with {model_name}")
    
    def load_tokenizer(self):
        """Load and configure tokenizer"""
        self.tokenizer = DistilBertTokenizer.from_pretrained(self.model_name)
        logger.info("Tokenizer loaded successfully")
    
    def create_model(self) -> tf.keras.Model:
        """
        Create mobile-optimized DistilBERT model for SMS classification
        """
        # Configure DistilBERT for mobile deployment
        config = DistilBertConfig.from_pretrained(
            self.model_name,
            num_labels=len(self.CATEGORIES),
            hidden_dropout_prob=0.1,  # Reduced dropout for better mobile performance
            attention_probs_dropout_prob=0.1
        )
        
        # Load base model
        base_model = TFDistilBertForSequenceClassification.from_pretrained(
            self.model_name,
            config=config
        )
        
        # Create custom model optimized for mobile
        input_ids = tf.keras.layers.Input(shape=(self.max_length,), dtype=tf.int32, name="input_ids")
        attention_mask = tf.keras.layers.Input(shape=(self.max_length,), dtype=tf.int32, name="attention_mask")
        
        # DistilBERT outputs
        outputs = base_model.distilbert(input_ids=input_ids, attention_mask=attention_mask)
        sequence_output = outputs.last_hidden_state
        
        # Mobile-optimized classification head
        pooled_output = tf.keras.layers.GlobalAveragePooling1D()(sequence_output)
        
        # Smaller dense layers for mobile efficiency
        dense1 = tf.keras.layers.Dense(256, activation='relu', name='dense1')(pooled_output)
        dropout1 = tf.keras.layers.Dropout(0.2)(dense1)
        
        dense2 = tf.keras.layers.Dense(128, activation='relu', name='dense2')(dropout1)
        dropout2 = tf.keras.layers.Dropout(0.1)(dense2)
        
        # Output layer
        predictions = tf.keras.layers.Dense(
            len(self.CATEGORIES), 
            activation='softmax', 
            name='predictions'
        )(dropout2)
        
        model = tf.keras.Model(
            inputs=[input_ids, attention_mask],
            outputs=predictions
        )
        
        # Compile with mobile-friendly settings
        model.compile(
            optimizer=tf.keras.optimizers.Adam(learning_rate=2e-5),
            loss='sparse_categorical_crossentropy',
            metrics=['accuracy', 'sparse_top_k_categorical_accuracy']
        )
        
        self.model = model
        logger.info(f"Mobile model created with {model.count_params():,} parameters")
        return model
    
    def preprocess_data(self, texts: List[str], labels: Optional[List[int]] = None) -> Dict:
        """
        Preprocess SMS texts for model training/inference
        
        Args:
            texts: List of SMS messages
            labels: Optional list of labels (for training)
            
        Returns:
            Dictionary with tokenized inputs
        """
        # Tokenize texts
        encodings = self.tokenizer(
            texts,
            truncation=True,
            padding='max_length',
            max_length=self.max_length,
            return_tensors='tf'
        )
        
        result = {
            'input_ids': encodings['input_ids'],
            'attention_mask': encodings['attention_mask']
        }
        
        if labels is not None:
            result['labels'] = tf.constant(labels)
        
        return result
    
    def train_model(self, 
                   train_texts: List[str], 
                   train_labels: List[int],
                   val_texts: List[str],
                   val_labels: List[int],
                   epochs: int = 3,
                   batch_size: int = 16) -> Dict:
        """
        Train the mobile SMS classifier
        
        Args:
            train_texts: Training SMS messages
            train_labels: Training labels
            val_texts: Validation SMS messages  
            val_labels: Validation labels
            epochs: Number of training epochs
            batch_size: Training batch size
            
        Returns:
            Training history
        """
        if self.model is None:
            self.create_model()
        
        if self.tokenizer is None:
            self.load_tokenizer()
        
        # Preprocess data
        train_data = self.preprocess_data(train_texts, train_labels)
        val_data = self.preprocess_data(val_texts, val_labels)
        
        # Create datasets
        train_dataset = tf.data.Dataset.from_tensor_slices({
            'input_ids': train_data['input_ids'],
            'attention_mask': train_data['attention_mask'],
            'labels': train_data['labels']
        }).batch(batch_size).prefetch(tf.data.AUTOTUNE)
        
        val_dataset = tf.data.Dataset.from_tensor_slices({
            'input_ids': val_data['input_ids'],
            'attention_mask': val_data['attention_mask'],
            'labels': val_data['labels']
        }).batch(batch_size).prefetch(tf.data.AUTOTUNE)
        
        # Callbacks for mobile optimization
        callbacks = [
            tf.keras.callbacks.EarlyStopping(
                monitor='val_accuracy',
                patience=2,
                restore_best_weights=True
            ),
            tf.keras.callbacks.ReduceLROnPlateau(
                monitor='val_loss',
                factor=0.5,
                patience=1,
                min_lr=1e-7
            ),
            tf.keras.callbacks.ModelCheckpoint(
                'mobile_sms_classifier_best.h5',
                monitor='val_accuracy',
                save_best_only=True,
                save_weights_only=False
            )
        ]
        
        logger.info(f"Starting training for {epochs} epochs...")
        
        # Train model
        history = self.model.fit(
            train_dataset,
            validation_data=val_dataset,
            epochs=epochs,
            callbacks=callbacks,
            verbose=1
        )
        
        self.training_history = history.history
        logger.info("Training completed successfully")
        
        return self.training_history
    
    def evaluate_model(self, test_texts: List[str], test_labels: List[int]) -> Dict:
        """
        Evaluate the trained model
        
        Args:
            test_texts: Test SMS messages
            test_labels: Test labels
            
        Returns:
            Evaluation metrics
        """
        if self.model is None:
            raise ValueError("Model not trained. Call train_model() first.")
        
        # Preprocess test data
        test_data = self.preprocess_data(test_texts)
        
        # Make predictions
        start_time = time.time()
        predictions = self.model.predict([
            test_data['input_ids'], 
            test_data['attention_mask']
        ])
        inference_time = (time.time() - start_time) * 1000 / len(test_texts)
        
        # Get predicted classes
        predicted_classes = np.argmax(predictions, axis=1)
        
        # Calculate metrics
        report = classification_report(
            test_labels, 
            predicted_classes,
            target_names=self.CATEGORY_NAMES,
            output_dict=True
        )
        
        cm = confusion_matrix(test_labels, predicted_classes)
        
        self.evaluation_metrics = {
            'classification_report': report,
            'confusion_matrix': cm.tolist(),
            'accuracy': report['accuracy'],
            'avg_inference_time_ms': inference_time,
            'model_size_mb': self._calculate_model_size()
        }
        
        logger.info(f"Model evaluation completed:")
        logger.info(f"Accuracy: {report['accuracy']:.4f}")
        logger.info(f"Avg inference time: {inference_time:.2f}ms")
        logger.info(f"Model size: {self.evaluation_metrics['model_size_mb']:.1f}MB")
        
        return self.evaluation_metrics
    
    def classify_sms(self, sms_text: str, sender: str = "Unknown") -> MobileClassificationResult:
        """
        Classify a single SMS message
        
        Args:
            sms_text: SMS message content
            sender: Sender information (optional)
            
        Returns:
            Classification result
        """
        if self.model is None:
            raise ValueError("Model not loaded. Train or load a model first.")
        
        start_time = time.time()
        
        # Preprocess input
        processed_data = self.preprocess_data([sms_text])
        
        # Make prediction
        prediction = self.model.predict([
            processed_data['input_ids'],
            processed_data['attention_mask']
        ], verbose=0)
        
        processing_time = (time.time() - start_time) * 1000
        
        # Extract results
        probabilities = prediction[0]
        predicted_class = np.argmax(probabilities)
        confidence = float(probabilities[predicted_class])
        
        # Create probability dictionary
        prob_dict = {
            category: float(prob) 
            for category, prob in zip(self.CATEGORY_NAMES, probabilities)
        }
        
        result = MobileClassificationResult(
            category=self.CATEGORY_NAMES[predicted_class],
            category_id=predicted_class,
            confidence=confidence,
            probabilities=prob_dict,
            processing_time_ms=processing_time,
            model_version=self.model_version
        )
        
        logger.debug(f"SMS classified as {result.category} with confidence {confidence:.3f}")
        
        return result
    
    def convert_to_tflite(self, 
                         output_path: str = "mobile_sms_classifier.tflite",
                         quantization: str = "dynamic") -> str:
        """
        Convert trained model to TensorFlow Lite for mobile deployment
        
        Args:
            output_path: Path to save TFLite model
            quantization: Type of quantization ("dynamic", "int8", "float16")
            
        Returns:
            Path to saved TFLite model
        """
        if self.model is None:
            raise ValueError("No model to convert. Train a model first.")
        
        logger.info(f"Converting model to TensorFlow Lite with {quantization} quantization...")
        
        # Create TFLite converter
        converter = tf.lite.TFLiteConverter.from_keras_model(self.model)
        
        # Apply quantization
        if quantization == "dynamic":
            converter.optimizations = [tf.lite.Optimize.DEFAULT]
        elif quantization == "int8":
            converter.optimizations = [tf.lite.Optimize.DEFAULT]
            converter.target_spec.supported_types = [tf.int8]
        elif quantization == "float16":
            converter.optimizations = [tf.lite.Optimize.DEFAULT]
            converter.target_spec.supported_types = [tf.float16]
        
        # Convert model
        tflite_model = converter.convert()
        
        # Save model
        output_path = Path(output_path)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        
        with open(output_path, 'wb') as f:
            f.write(tflite_model)
        
        # Calculate size
        size_mb = len(tflite_model) / (1024 * 1024)
        
        logger.info(f"TFLite model saved to {output_path}")
        logger.info(f"Model size: {size_mb:.1f}MB")
        
        self.tflite_model = tflite_model
        return str(output_path)
    
    def load_tflite_model(self, model_path: str):
        """Load TensorFlow Lite model for inference"""
        with open(model_path, 'rb') as f:
            self.tflite_model = f.read()
        
        # Create interpreter
        self.interpreter = tf.lite.Interpreter(model_content=self.tflite_model)
        self.interpreter.allocate_tensors()
        
        logger.info(f"TFLite model loaded from {model_path}")
    
    def classify_with_tflite(self, sms_text: str) -> MobileClassificationResult:
        """
        Classify SMS using TensorFlow Lite model (mobile-optimized)
        
        Args:
            sms_text: SMS message content
            
        Returns:
            Classification result
        """
        if self.tflite_model is None:
            raise ValueError("TFLite model not loaded. Call load_tflite_model() first.")
        
        start_time = time.time()
        
        # Preprocess input
        processed_data = self.preprocess_data([sms_text])
        
        # Get input/output details
        input_details = self.interpreter.get_input_details()
        output_details = self.interpreter.get_output_details()
        
        # Set input tensors
        self.interpreter.set_tensor(
            input_details[0]['index'], 
            processed_data['input_ids'].numpy()
        )
        self.interpreter.set_tensor(
            input_details[1]['index'], 
            processed_data['attention_mask'].numpy()
        )
        
        # Run inference
        self.interpreter.invoke()
        
        # Get outputs
        predictions = self.interpreter.get_tensor(output_details[0]['index'])
        processing_time = (time.time() - start_time) * 1000
        
        # Extract results
        probabilities = predictions[0]
        predicted_class = np.argmax(probabilities)
        confidence = float(probabilities[predicted_class])
        
        # Create probability dictionary
        prob_dict = {
            category: float(prob) 
            for category, prob in zip(self.CATEGORY_NAMES, probabilities)
        }
        
        result = MobileClassificationResult(
            category=self.CATEGORY_NAMES[predicted_class],
            category_id=predicted_class,
            confidence=confidence,
            probabilities=prob_dict,
            processing_time_ms=processing_time,
            model_version=f"{self.model_version}-tflite"
        )
        
        return result
    
    def _calculate_model_size(self) -> float:
        """Calculate model size in MB"""
        if self.model is None:
            return 0.0
        
        # Save model temporarily to calculate size
        temp_path = "temp_model.h5"
        self.model.save(temp_path)
        size_mb = Path(temp_path).stat().st_size / (1024 * 1024)
        Path(temp_path).unlink()  # Delete temp file
        
        return size_mb
    
    def save_model(self, model_path: str, tokenizer_path: str = None):
        """Save the trained model and tokenizer"""
        if self.model is None:
            raise ValueError("No model to save")
        
        # Save model
        self.model.save(model_path)
        
        # Save tokenizer
        if tokenizer_path and self.tokenizer:
            self.tokenizer.save_pretrained(tokenizer_path)
        
        # Save metadata
        metadata = {
            'model_version': self.model_version,
            'categories': self.CATEGORIES,
            'max_length': self.max_length,
            'model_name': self.model_name,
            'training_history': self.training_history,
            'evaluation_metrics': self.evaluation_metrics
        }
        
        metadata_path = Path(model_path).parent / "model_metadata.json"
        with open(metadata_path, 'w') as f:
            json.dump(metadata, f, indent=2)
        
        logger.info(f"Model saved to {model_path}")
        logger.info(f"Metadata saved to {metadata_path}")
    
    def load_model(self, model_path: str, tokenizer_path: str = None):
        """Load a saved model and tokenizer"""
        # Load model
        self.model = tf.keras.models.load_model(model_path)
        
        # Load tokenizer
        if tokenizer_path:
            self.tokenizer = DistilBertTokenizer.from_pretrained(tokenizer_path)
        else:
            self.load_tokenizer()
        
        # Load metadata
        metadata_path = Path(model_path).parent / "model_metadata.json"
        if metadata_path.exists():
            with open(metadata_path, 'r') as f:
                metadata = json.load(f)
            
            self.model_version = metadata.get('model_version', '1.0.0')
            self.max_length = metadata.get('max_length', 128)
            self.training_history = metadata.get('training_history')
            self.evaluation_metrics = metadata.get('evaluation_metrics')
        
        logger.info(f"Model loaded from {model_path}")
    
    def plot_training_history(self, save_path: str = None):
        """Plot training history"""
        if self.training_history is None:
            logger.warning("No training history available")
            return
        
        fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(12, 8))
        
        # Accuracy
        ax1.plot(self.training_history['accuracy'], label='Training')
        ax1.plot(self.training_history['val_accuracy'], label='Validation')
        ax1.set_title('Model Accuracy')
        ax1.set_xlabel('Epoch')
        ax1.set_ylabel('Accuracy')
        ax1.legend()
        
        # Loss
        ax2.plot(self.training_history['loss'], label='Training')
        ax2.plot(self.training_history['val_loss'], label='Validation')
        ax2.set_title('Model Loss')
        ax2.set_xlabel('Epoch')
        ax2.set_ylabel('Loss')
        ax2.legend()
        
        # Top-K Accuracy
        if 'sparse_top_k_categorical_accuracy' in self.training_history:
            ax3.plot(self.training_history['sparse_top_k_categorical_accuracy'], label='Training')
            ax3.plot(self.training_history['val_sparse_top_k_categorical_accuracy'], label='Validation')
            ax3.set_title('Top-K Accuracy')
            ax3.set_xlabel('Epoch')
            ax3.set_ylabel('Top-K Accuracy')
            ax3.legend()
        
        # Confusion Matrix
        if self.evaluation_metrics and 'confusion_matrix' in self.evaluation_metrics:
            cm = np.array(self.evaluation_metrics['confusion_matrix'])
            sns.heatmap(cm, annot=True, fmt='d', cmap='Blues', ax=ax4)
            ax4.set_title('Confusion Matrix')
            ax4.set_xlabel('Predicted')
            ax4.set_ylabel('Actual')
        
        plt.tight_layout()
        
        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            logger.info(f"Training plots saved to {save_path}")
        
        plt.show()

def main():
    """Test the mobile SMS classifier"""
    print("📱 Mobile SMS Classifier Test")
    print("=" * 50)
    
    # Initialize classifier
    classifier = MobileSmsClassifier()
    classifier.load_tokenizer()
    
    # Test messages with Indian SMS patterns
    test_messages = [
        "Your OTP is 123456. Valid for 10 minutes. Do not share.",
        "Congratulations! You won ₹50,000! Call now to claim your prize!",
        "Hi mom, I'll be late for dinner tonight. Don't wait for me.",
        "Dear Customer, Rs.5000 has been debited from A/c XX1234 on 15-Jan-25",
        "Flash Sale! 70% off everything! Limited time offer. Shop now!",
        "Your Amazon order #1234567890 will be delivered today between 10-6 PM",
        "URGENT: Update your KYC details to avoid account suspension",
        "Meeting rescheduled to 3 PM tomorrow. Please confirm attendance."
    ]
    
    # Expected categories (for testing)
    expected_categories = [
        "OTP", "SPAM", "INBOX", "BANKING", 
        "SPAM", "ECOMMERCE", "SPAM", "INBOX"
    ]
    
    # Create a simple model for testing (normally you'd train with real data)
    print("\n🏗️ Creating mobile model architecture...")
    model = classifier.create_model()
    print(f"Model parameters: {model.count_params():,}")
    
    # Simulate inference (model isn't trained yet)
    print("\n📊 Model Architecture Summary:")
    model.summary()
    
    print(f"\n✅ Mobile SMS Classifier setup complete!")
    print(f"📏 Target model size: <20MB")
    print(f"⚡ Target inference time: <100ms")
    print(f"🎯 Categories: {', '.join(MobileSmsClassifier.CATEGORY_NAMES)}")
    
    # Show next steps
    print("\n📋 Next Steps:")
    print("1. Prepare training data with Indian SMS patterns")
    print("2. Train the model using train_model()")
    print("3. Convert to TensorFlow Lite using convert_to_tflite()")
    print("4. Deploy to Android app")

if __name__ == "__main__":
    main()