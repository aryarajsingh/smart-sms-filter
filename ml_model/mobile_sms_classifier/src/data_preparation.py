#!/usr/bin/env python3
"""
Data Preparation for Mobile SMS Classifier
Prepares and augments training data for smartphone deployment
"""

import pandas as pd
import numpy as np
import re
import json
import yaml
import logging
from pathlib import Path
from typing import List, Dict, Tuple, Optional
from dataclasses import dataclass
from sklearn.model_selection import train_test_split
from sklearn.utils.class_weight import compute_class_weight
import random
from datetime import datetime

logger = logging.getLogger(__name__)

@dataclass
class SMSDataset:
    """Container for SMS dataset with metadata"""
    texts: List[str]
    labels: List[int]
    senders: List[str]
    categories: List[str]
    metadata: Dict

class IndianSMSDataGenerator:
    """Generate synthetic Indian SMS data for training"""
    
    # Indian SMS patterns and templates
    TEMPLATES = {
        'OTP': [
            "{otp} is your OTP for {service}. Valid for {time} minutes. Do not share.",
            "Dear Customer, {otp} is your verification code for {service}. Valid till {time} mins.",
            "Use {otp} to verify your account for {service}. One Time Password valid for {time} minutes.",
            "{otp} is your security code for {service}. Don't share with anyone.",
            "Your OTP is {otp} for {service} login. Valid for {time} minutes only."
        ],
        'BANKING': [
            "Rs.{amount} debited from A/c XX{account} on {date} by UPI/{upi_id}. Balance: Rs.{balance}",
            "Rs.{amount} credited to your account XX{account} from {sender_name} via UPI. Balance Rs.{balance}",
            "Dear Customer, Rs.{amount} has been debited from A/c {account} for {purpose}. Balance Rs.{balance}",
            "IMPS transfer of Rs.{amount} from {bank} A/c XX{account} to {recipient} successful. Ref: {ref}",
            "Your {bank} account XX{account} credited with Rs.{amount} from {sender} on {date}. Available bal Rs.{balance}"
        ],
        'SPAM': [
            "🎉 CONGRATULATIONS! You've won Rs.{amount}! Call {phone} to claim your prize NOW!",
            "URGENT: You've won {prize}! Click {url} to claim within {time} hours!",
            "LIMITED TIME OFFER! {discount}% OFF on everything! Shop now at {url}",
            "FREE {gift} for first {number} customers! Call {phone} immediately!",
            "WINNER WINNER! You're selected for {prize} worth Rs.{amount}! Call {phone} now!"
        ],
        'ECOMMERCE': [
            "Your {store} order #{order_id} is out for delivery. Expected by {time}. Track: {url}",
            "Order #{order_id} delivered successfully. Rate your experience: {url}",
            "Your {store} order #{order_id} is shipped. Estimated delivery: {date}",
            "Payment of Rs.{amount} for order #{order_id} confirmed. Thank you for shopping with {store}!",
            "{store}: Your order #{order_id} worth Rs.{amount} is being prepared for dispatch."
        ],
        'INBOX': [
            "Hi {name}, are you free for {event} {time}? Let me know!",
            "Meeting rescheduled to {time} tomorrow. Please confirm attendance.",
            "Happy birthday {name}! Hope you have a wonderful day!",
            "Don't forget about the {event} at {time} today. See you there!",
            "Thanks for your help with the {task}. Really appreciate it!"
        ]
    }
    
    # Sample data for template filling
    SAMPLE_DATA = {
        'otp': ['123456', '987654', '456789', '234567', '876543'],
        'service': ['HDFC Bank', 'SBI', 'ICICI', 'PayTM', 'Amazon', 'Google', 'Facebook'],
        'time': ['5', '10', '15', '3', '30'],
        'amount': ['1000', '2500', '5000', '750', '12000', '350', '8900'],
        'account': ['1234', '5678', '9012', '3456'],
        'balance': ['25000', '50000', '12500', '75000', '100000'],
        'bank': ['SBI', 'HDFC', 'ICICI', 'AXIS', 'PNB'],
        'upi_id': ['9876543210', '8765432109', '7654321098'],
        'phone': ['9876543210', '8888888888', '7777777777'],
        'url': ['bit.ly/claim123', 'tinyurl.com/offer', 'short.link/deal'],
        'discount': ['50', '70', '90', '30', '60'],
        'gift': ['iPhone', 'Laptop', 'Cash Prize', 'Gold Coin'],
        'prize': ['iPhone 15', 'Car', 'Laptop', '1 Lakh Cash'],
        'number': ['100', '50', '25', '200'],
        'store': ['Amazon', 'Flipkart', 'Myntra', 'Zomato'],
        'order_id': ['12345678', '87654321', '45678912'],
        'name': ['Raj', 'Priya', 'Amit', 'Neha', 'Vikash'],
        'event': ['lunch', 'meeting', 'party', 'dinner'],
        'task': ['project', 'presentation', 'report'],
        'date': ['15-Jan-25', '20-Feb-25', '10-Mar-25'],
        'ref': ['UPI12345', 'IMPS67890', 'NEFT54321']
    }
    
    def generate_synthetic_data(self, samples_per_category: int = 1000) -> SMSDataset:
        """Generate synthetic SMS data for training"""
        texts = []
        labels = []
        senders = []
        categories = []
        
        # Category mapping
        category_map = {
            'INBOX': 0, 'SPAM': 1, 'OTP': 2, 
            'BANKING': 3, 'ECOMMERCE': 4, 'NEEDS_REVIEW': 5
        }
        
        for category, label in category_map.items():
            if category == 'NEEDS_REVIEW':
                # Generate ambiguous messages
                for _ in range(samples_per_category // 2):
                    text = self._generate_ambiguous_message()
                    sender = self._generate_sender(category)
                    texts.append(text)
                    labels.append(label)
                    senders.append(sender)
                    categories.append(category)
                continue
            
            templates = self.TEMPLATES.get(category, [])
            for _ in range(samples_per_category):
                template = random.choice(templates)
                text = self._fill_template(template)
                sender = self._generate_sender(category)
                
                texts.append(text)
                labels.append(label)
                senders.append(sender)
                categories.append(category)
        
        # Shuffle the data
        combined = list(zip(texts, labels, senders, categories))
        random.shuffle(combined)
        texts, labels, senders, categories = zip(*combined)
        
        return SMSDataset(
            texts=list(texts),
            labels=list(labels),
            senders=list(senders),
            categories=list(categories),
            metadata={
                'total_samples': len(texts),
                'samples_per_category': samples_per_category,
                'generation_date': datetime.now().isoformat(),
                'category_distribution': {cat: labels.count(i) for cat, i in category_map.items()}
            }
        )
    
    def _fill_template(self, template: str) -> str:
        """Fill template with random sample data"""
        # Find all placeholders in the template
        placeholders = re.findall(r'\{(\w+)\}', template)
        
        # Fill each placeholder with random sample data
        filled = template
        for placeholder in placeholders:
            if placeholder in self.SAMPLE_DATA:
                value = random.choice(self.SAMPLE_DATA[placeholder])
                filled = filled.replace(f'{{{placeholder}}}', value)
        
        return filled
    
    def _generate_sender(self, category: str) -> str:
        """Generate realistic sender based on category"""
        senders = {
            'OTP': ['VK-HDFC', 'SBI', 'VK-ICICI', 'PAYTM', 'AMAZON', 'GOOGLE'],
            'BANKING': ['SBI', 'HDFC', 'ICICI', 'AXIS', 'PNB', 'CANARA'],
            'SPAM': ['PROMO1', 'OFFER2', '9876543210', 'DEAL99'],
            'ECOMMERCE': ['AMAZON', 'FLIPKART', 'MYNTRA', 'ZOMATO'],
            'INBOX': ['+919876543210', '+918765432109', 'Mom', 'Dad', 'Boss'],
            'NEEDS_REVIEW': ['UNKNOWN', '+919999999999', 'SERVICE']
        }
        
        return random.choice(senders.get(category, ['UNKNOWN']))
    
    def _generate_ambiguous_message(self) -> str:
        """Generate ambiguous messages that require manual review"""
        ambiguous_messages = [
            "Please update your details to continue service.",
            "Important notice regarding your account.",
            "Action required for your profile verification.",
            "Your request has been processed successfully.",
            "Thank you for your recent transaction.",
            "We have received your application.",
            "Your booking is confirmed for today.",
            "Please contact customer service for assistance.",
            "Your subscription will expire soon.",
            "New features available in your account."
        ]
        return random.choice(ambiguous_messages)

class SMSDataPreprocessor:
    """Preprocess SMS data for mobile classifier training"""
    
    def __init__(self, config_path: str = "config/mobile_config.yaml"):
        with open(config_path, 'r') as f:
            self.config = yaml.safe_load(f)
        
        self.indian_patterns = self.config['indian_patterns']
    
    def load_existing_data(self, data_path: str) -> Optional[pd.DataFrame]:
        """Load existing SMS data from CSV"""
        try:
            df = pd.read_csv(data_path)
            logger.info(f"Loaded {len(df)} samples from {data_path}")
            return df
        except FileNotFoundError:
            logger.warning(f"No existing data found at {data_path}")
            return None
    
    def preprocess_text(self, text: str) -> str:
        """Clean and preprocess SMS text"""
        # Remove extra whitespace
        text = re.sub(r'\s+', ' ', text).strip()
        
        # Normalize currency symbols
        text = re.sub(r'₹|Rs\.?', 'Rs', text)
        
        # Normalize phone numbers (keep format but anonymize)
        text = re.sub(r'\b\d{10,12}\b', 'PHONE', text)
        
        # Normalize account numbers
        text = re.sub(r'A/c\s*\w*(\d{4})\d*', r'A/c XX\1', text)
        
        # Normalize amounts (keep magnitude)
        def normalize_amount(match):
            amount = match.group(1)
            if len(amount) > 4:
                return f"Rs {amount[:2]}K+"
            return match.group(0)
        
        text = re.sub(r'Rs\s*(\d+)', normalize_amount, text)
        
        return text
    
    def augment_data(self, texts: List[str], labels: List[int], 
                    augmentation_factor: float = 0.2) -> Tuple[List[str], List[int]]:
        """Apply data augmentation to increase training data diversity"""
        augmented_texts = texts.copy()
        augmented_labels = labels.copy()
        
        num_augmentations = int(len(texts) * augmentation_factor)
        
        for _ in range(num_augmentations):
            idx = random.randint(0, len(texts) - 1)
            original_text = texts[idx]
            original_label = labels[idx]
            
            # Apply random augmentation
            augmented_text = self._apply_augmentation(original_text)
            
            augmented_texts.append(augmented_text)
            augmented_labels.append(original_label)
        
        logger.info(f"Applied data augmentation: {len(texts)} -> {len(augmented_texts)}")
        return augmented_texts, augmented_labels
    
    def _apply_augmentation(self, text: str) -> str:
        """Apply random augmentation to text"""
        augmentations = [
            self._add_typos,
            self._change_case,
            self._add_punctuation,
            self._abbreviate_words
        ]
        
        # Apply random augmentation
        augmentation = random.choice(augmentations)
        return augmentation(text)
    
    def _add_typos(self, text: str) -> str:
        """Add random typos"""
        words = text.split()
        if len(words) > 3:
            # Replace random word with typo version
            idx = random.randint(0, len(words) - 1)
            word = words[idx]
            if len(word) > 3:
                # Simple character substitution
                pos = random.randint(1, len(word) - 2)
                word_list = list(word)
                word_list[pos] = random.choice('aeiou')
                words[idx] = ''.join(word_list)
        
        return ' '.join(words)
    
    def _change_case(self, text: str) -> str:
        """Randomly change case"""
        if random.random() < 0.5:
            return text.upper()
        return text.lower()
    
    def _add_punctuation(self, text: str) -> str:
        """Add or modify punctuation"""
        if not text.endswith('.') and random.random() < 0.5:
            return text + '.'
        return text
    
    def _abbreviate_words(self, text: str) -> str:
        """Apply common SMS abbreviations"""
        abbreviations = {
            'you': 'u',
            'your': 'ur', 
            'are': 'r',
            'for': '4',
            'to': '2',
            'and': '&'
        }
        
        for full, abbrev in abbreviations.items():
            if random.random() < 0.3:  # 30% chance
                text = re.sub(rf'\b{full}\b', abbrev, text, flags=re.IGNORECASE)
        
        return text
    
    def compute_class_weights(self, labels: List[int]) -> Dict[int, float]:
        """Compute class weights for imbalanced data"""
        unique_labels = np.unique(labels)
        weights = compute_class_weight('balanced', classes=unique_labels, y=labels)
        return {label: weight for label, weight in zip(unique_labels, weights)}
    
    def prepare_training_data(self, 
                            synthetic_samples: int = 5000,
                            existing_data_path: Optional[str] = None,
                            output_dir: str = "data/processed") -> Dict:
        """Prepare complete training dataset"""
        
        # Create output directory
        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)
        
        # Generate synthetic data
        generator = IndianSMSDataGenerator()
        synthetic_data = generator.generate_synthetic_data(synthetic_samples // 6)  # Distribute across categories
        
        logger.info(f"Generated {len(synthetic_data.texts)} synthetic samples")
        
        # Load existing data if available
        all_texts = synthetic_data.texts
        all_labels = synthetic_data.labels
        all_senders = synthetic_data.senders
        
        if existing_data_path:
            existing_df = self.load_existing_data(existing_data_path)
            if existing_df is not None:
                # Assuming existing data has columns: text, label, sender
                all_texts.extend(existing_df['text'].tolist())
                all_labels.extend(existing_df['label'].tolist())
                if 'sender' in existing_df.columns:
                    all_senders.extend(existing_df['sender'].tolist())
        
        # Preprocess texts
        processed_texts = [self.preprocess_text(text) for text in all_texts]
        
        # Apply data augmentation
        if self.config['data']['augmentation_enabled']:
            processed_texts, all_labels = self.augment_data(processed_texts, all_labels)
        
        # Split data
        train_texts, temp_texts, train_labels, temp_labels = train_test_split(
            processed_texts, all_labels, 
            test_size=0.3, 
            stratify=all_labels,
            random_state=42
        )
        
        val_texts, test_texts, val_labels, test_labels = train_test_split(
            temp_texts, temp_labels,
            test_size=0.5,
            stratify=temp_labels,
            random_state=42
        )
        
        # Compute class weights
        class_weights = self.compute_class_weights(train_labels)
        
        # Save datasets
        datasets = {
            'train': {'texts': train_texts, 'labels': train_labels},
            'val': {'texts': val_texts, 'labels': val_labels}, 
            'test': {'texts': test_texts, 'labels': test_labels}
        }
        
        for split, data in datasets.items():
            df = pd.DataFrame(data)
            df.to_csv(output_path / f"{split}_data.csv", index=False)
        
        # Save metadata
        metadata = {
            'total_samples': len(processed_texts),
            'train_samples': len(train_texts),
            'val_samples': len(val_texts),
            'test_samples': len(test_texts),
            'class_weights': class_weights,
            'category_mapping': {
                'INBOX': 0, 'SPAM': 1, 'OTP': 2,
                'BANKING': 3, 'ECOMMERCE': 4, 'NEEDS_REVIEW': 5
            },
            'preprocessing_config': self.config,
            'generation_info': synthetic_data.metadata
        }
        
        with open(output_path / "dataset_metadata.json", 'w') as f:
            json.dump(metadata, f, indent=2)
        
        logger.info(f"Dataset preparation complete:")
        logger.info(f"Train: {len(train_texts)}, Val: {len(val_texts)}, Test: {len(test_texts)}")
        logger.info(f"Data saved to: {output_path}")
        
        return {
            'train_texts': train_texts,
            'train_labels': train_labels,
            'val_texts': val_texts,
            'val_labels': val_labels,
            'test_texts': test_texts,
            'test_labels': test_labels,
            'class_weights': class_weights,
            'metadata': metadata
        }

def main():
    """Test data preparation"""
    print("📊 SMS Data Preparation for Mobile Classifier")
    print("=" * 50)
    
    # Initialize preprocessor
    preprocessor = SMSDataPreprocessor()
    
    # Prepare training data
    print("\n🏗️ Preparing training data...")
    data = preprocessor.prepare_training_data(
        synthetic_samples=1000,  # Small test dataset
        output_dir="data/processed"
    )
    
    print(f"\n✅ Data preparation complete!")
    print(f"📈 Training samples: {len(data['train_texts'])}")
    print(f"📊 Validation samples: {len(data['val_texts'])}")
    print(f"🧪 Test samples: {len(data['test_texts'])}")
    
    # Show sample data
    print(f"\n📱 Sample SMS messages:")
    for i in range(min(5, len(data['train_texts']))):
        text = data['train_texts'][i]
        label = data['train_labels'][i]
        category = list(data['metadata']['category_mapping'].keys())[
            list(data['metadata']['category_mapping'].values()).index(label)
        ]
        print(f"{category}: {text[:80]}...")
    
    print(f"\n⚖️ Class weights: {data['class_weights']}")

if __name__ == "__main__":
    main()