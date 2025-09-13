#!/usr/bin/env python3
"""
Data Collection Script for Smart SMS Filter ML Model

This script extracts SMS data from the Android app's database and 
prepares it for machine learning training.
"""

import sqlite3
import pandas as pd
import numpy as np
import os
import sys
from pathlib import Path
from typing import List, Tuple, Optional
import logging
from datetime import datetime
import json

# Add project root to path
sys.path.append(str(Path(__file__).parent.parent.parent))

class SmsDataCollector:
    """Collects and preprocesses SMS data from the Android app database"""
    
    def __init__(self, db_path: str = None):
        """
        Initialize the data collector
        
        Args:
            db_path: Path to the SQLite database file from Android app
        """
        self.db_path = db_path
        self.logger = self._setup_logger()
        
        # Default database path (adjust based on your Android app's data location)
        if not self.db_path:
            # This would typically be extracted from Android device
            # For now, we'll create a sample path
            self.db_path = "../../android/app/databases/sms_database.db"
    
    def _setup_logger(self) -> logging.Logger:
        """Setup logging for data collection"""
        logger = logging.getLogger('SmsDataCollector')
        logger.setLevel(logging.INFO)
        
        if not logger.handlers:
            handler = logging.StreamHandler()
            formatter = logging.Formatter(
                '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
            )
            handler.setFormatter(formatter)
            logger.addHandler(handler)
        
        return logger
    
    def connect_database(self) -> sqlite3.Connection:
        """Connect to the SMS database"""
        try:
            if not os.path.exists(self.db_path):
                self.logger.error(f"Database file not found: {self.db_path}")
                raise FileNotFoundError(f"Database file not found: {self.db_path}")
            
            conn = sqlite3.connect(self.db_path)
            self.logger.info(f"Connected to database: {self.db_path}")
            return conn
        
        except Exception as e:
            self.logger.error(f"Failed to connect to database: {e}")
            raise
    
    def extract_sms_data(self) -> pd.DataFrame:
        """
        Extract SMS messages from the database
        
        Returns:
            DataFrame containing SMS messages with features
        """
        try:
            conn = self.connect_database()
            
            # Query to extract SMS data - adjust column names based on your schema
            query = """
            SELECT 
                id,
                sender,
                content,
                timestamp,
                category,
                is_read,
                is_important,
                thread_id,
                is_outgoing,
                is_deleted,
                is_archived
            FROM sms_messages 
            WHERE is_deleted = 0
            ORDER BY timestamp DESC
            """
            
            df = pd.read_sql_query(query, conn)
            conn.close()
            
            self.logger.info(f"Extracted {len(df)} SMS messages from database")
            return df
        
        except Exception as e:
            self.logger.error(f"Failed to extract SMS data: {e}")
            raise
    
    def create_sample_data(self) -> pd.DataFrame:
        """
        Create sample SMS data for development and testing
        This function generates realistic SMS data for model development
        """
        self.logger.info("Creating sample SMS data for development...")
        
        # Sample spam messages
        spam_messages = [
            "🎉 CONGRATULATIONS! You've won $1000! Click here to claim: bit.ly/claim123",
            "URGENT: Your account will be closed! Call 1-800-SCAM now!",
            "Free iPhone! You're our lucky winner! Reply YES to claim your prize now!",
            "Limited time offer! 90% discount on everything! Buy now!",
            "You have been selected for a special offer! Act fast!",
            "WINNER! Claim your cash prize now! Call immediately!",
            "Exclusive deal just for you! Don't miss out!",
            "Your phone number has won! Contact us to collect your reward!",
            "Amazing opportunity to make money from home! Start today!",
            "Flash sale! Everything must go! Hurry while stocks last!"
        ]
        
        # Sample inbox (legitimate) messages
        inbox_messages = [
            "Hi, are you free for lunch today?",
            "Meeting moved to 3 PM. Please confirm.",
            "Your OTP is 123456. Valid for 10 minutes.",
            "Thanks for dinner last night! Had a great time.",
            "Can you pick up milk on your way home?",
            "Your order has been delivered. Thank you for shopping with us.",
            "Reminder: Doctor appointment tomorrow at 2 PM",
            "Your credit card payment is due in 3 days",
            "Happy birthday! Hope you have a wonderful day!",
            "The package you ordered will arrive tomorrow",
            "Your flight is on time. Gate B12, boarding at 6:30 PM",
            "Bank alert: $500 credited to your account",
            "Your subscription expires in 7 days. Renew now to continue service."
        ]
        
        # Sample needs review messages (ambiguous)
        review_messages = [
            "Special discount for you! Save 20% on your next purchase",
            "New features available in our app. Update now!",
            "Your reward points expire soon. Use them now!",
            "Weekly newsletter with latest updates and offers",
            "Invitation to exclusive member event this weekend",
            "Survey: Help us improve our service. 5 minutes only",
            "New product launch! Be the first to try it",
            "Your monthly statement is ready for download",
        ]
        
        # Create DataFrame with sample data
        data = []
        
        # Add spam messages
        for i, msg in enumerate(spam_messages):
            data.append({
                'id': i + 1,
                'sender': f'SPAM{i:03d}',
                'content': msg,
                'timestamp': int(datetime.now().timestamp() * 1000) - (i * 3600000),
                'category': 'SPAM',
                'is_read': np.random.choice([0, 1]),
                'is_important': 0,
                'thread_id': f'thread_spam_{i}',
                'is_outgoing': 0,
                'is_deleted': 0,
                'is_archived': 0
            })
        
        # Add inbox messages
        for i, msg in enumerate(inbox_messages):
            data.append({
                'id': len(spam_messages) + i + 1,
                'sender': f'+1555{i:04d}' if i < 5 else f'Contact{i}',
                'content': msg,
                'timestamp': int(datetime.now().timestamp() * 1000) - (i * 7200000),
                'category': 'INBOX',
                'is_read': np.random.choice([0, 1]),
                'is_important': np.random.choice([0, 1], p=[0.8, 0.2]),
                'thread_id': f'thread_inbox_{i}',
                'is_outgoing': 0,
                'is_deleted': 0,
                'is_archived': 0
            })
        
        # Add needs review messages
        for i, msg in enumerate(review_messages):
            data.append({
                'id': len(spam_messages) + len(inbox_messages) + i + 1,
                'sender': f'Service{i:03d}',
                'content': msg,
                'timestamp': int(datetime.now().timestamp() * 1000) - (i * 5400000),
                'category': 'NEEDS_REVIEW',
                'is_read': np.random.choice([0, 1]),
                'is_important': 0,
                'thread_id': f'thread_review_{i}',
                'is_outgoing': 0,
                'is_deleted': 0,
                'is_archived': 0
            })
        
        df = pd.DataFrame(data)
        self.logger.info(f"Created sample dataset with {len(df)} messages")
        return df
    
    def preprocess_data(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Preprocess the SMS data for ML training
        
        Args:
            df: Raw SMS DataFrame
            
        Returns:
            Preprocessed DataFrame ready for feature engineering
        """
        self.logger.info("Preprocessing SMS data...")
        
        # Convert timestamp to datetime
        df['datetime'] = pd.to_datetime(df['timestamp'], unit='ms')
        
        # Extract time-based features
        df['hour'] = df['datetime'].dt.hour
        df['day_of_week'] = df['datetime'].dt.dayofweek
        df['is_weekend'] = df['day_of_week'].isin([5, 6]).astype(int)
        
        # Message length features
        df['message_length'] = df['content'].str.len()
        df['word_count'] = df['content'].str.split().str.len()
        df['avg_word_length'] = df['content'].apply(
            lambda x: np.mean([len(word) for word in x.split()]) if x.split() else 0
        )
        
        # Sender features
        df['sender_is_number'] = df['sender'].str.contains(r'^\+?\d+$', na=False).astype(int)
        df['sender_length'] = df['sender'].str.len()
        
        # Content analysis features
        df['has_url'] = df['content'].str.contains(r'http|www|\.com|bit\.ly', case=False, na=False).astype(int)
        df['has_phone'] = df['content'].str.contains(r'\b\d{3}[-.]?\d{3}[-.]?\d{4}\b', na=False).astype(int)
        df['exclamation_count'] = df['content'].str.count('!')
        df['question_count'] = df['content'].str.count('\?')
        df['capital_ratio'] = df['content'].apply(
            lambda x: sum(1 for c in x if c.isupper()) / len(x) if x else 0
        )
        
        # Clean text for NLP
        df['content_clean'] = df['content'].str.lower()
        df['content_clean'] = df['content_clean'].str.replace(r'[^\w\s]', ' ', regex=True)
        df['content_clean'] = df['content_clean'].str.replace(r'\s+', ' ', regex=True)
        df['content_clean'] = df['content_clean'].str.strip()
        
        self.logger.info("Data preprocessing completed")
        return df
    
    def save_processed_data(self, df: pd.DataFrame, output_path: str = None) -> str:
        """
        Save processed data to file
        
        Args:
            df: Processed DataFrame
            output_path: Output file path
            
        Returns:
            Path to saved file
        """
        if not output_path:
            output_path = "data/processed/sms_data_processed.csv"
        
        # Ensure output directory exists
        os.makedirs(os.path.dirname(output_path), exist_ok=True)
        
        # Save to CSV
        df.to_csv(output_path, index=False)
        
        # Save metadata
        metadata = {
            'total_messages': len(df),
            'categories': df['category'].value_counts().to_dict(),
            'date_range': {
                'start': df['datetime'].min().isoformat(),
                'end': df['datetime'].max().isoformat()
            },
            'features': list(df.columns),
            'created_at': datetime.now().isoformat()
        }
        
        metadata_path = output_path.replace('.csv', '_metadata.json')
        with open(metadata_path, 'w') as f:
            json.dump(metadata, f, indent=2)
        
        self.logger.info(f"Processed data saved to: {output_path}")
        self.logger.info(f"Metadata saved to: {metadata_path}")
        
        return output_path

def main():
    """Main function to run data collection"""
    collector = SmsDataCollector()
    
    try:
        # Try to extract real data first, fall back to sample data
        try:
            df = collector.extract_sms_data()
        except (FileNotFoundError, Exception):
            print("⚠️  Real database not found. Creating sample data for development...")
            df = collector.create_sample_data()
        
        # Preprocess the data
        df_processed = collector.preprocess_data(df)
        
        # Save processed data
        output_path = collector.save_processed_data(df_processed)
        
        print(f"\n✅ Data collection completed successfully!")
        print(f"📊 Total messages: {len(df_processed)}")
        print(f"📁 Data saved to: {output_path}")
        print("\nCategory distribution:")
        print(df_processed['category'].value_counts())
        
        return output_path
        
    except Exception as e:
        print(f"❌ Error during data collection: {e}")
        raise

if __name__ == "__main__":
    main()