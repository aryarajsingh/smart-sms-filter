#!/usr/bin/env python3
"""
Feature Engineering for Smart SMS Filter ML Model

This script implements comprehensive feature extraction for SMS classification,
including TF-IDF, sender patterns, content analysis, and domain-specific features.
"""

import pandas as pd
import numpy as np
import re
from typing import Dict, List, Tuple, Any
import logging
from pathlib import Path
import joblib

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.preprocessing import StandardScaler, LabelEncoder
from textblob import TextBlob
import nltk

# Download required NLTK data
try:
    nltk.data.find('corpora/stopwords')
except LookupError:
    nltk.download('stopwords', quiet=True)

try:
    nltk.data.find('tokenizers/punkt')
except LookupError:
    nltk.download('punkt', quiet=True)

from nltk.corpus import stopwords
from nltk.tokenize import word_tokenize

class SmsFeatureEngineer:
    """Comprehensive feature engineering for SMS classification"""
    
    def __init__(self, max_tfidf_features: int = 5000):
        """
        Initialize the feature engineer
        
        Args:
            max_tfidf_features: Maximum number of TF-IDF features
        """
        self.max_tfidf_features = max_tfidf_features
        self.logger = self._setup_logger()
        
        # Initialize feature extractors
        self.tfidf_vectorizer = None
        self.scaler = StandardScaler()
        self.label_encoder = LabelEncoder()
        
        # SMS-specific keywords and patterns
        self.spam_keywords = self._get_spam_keywords()
        self.urgency_keywords = self._get_urgency_keywords()
        self.financial_keywords = self._get_financial_keywords()
        self.promotional_keywords = self._get_promotional_keywords()
        
        # Stop words
        self.stop_words = set(stopwords.words('english'))
    
    def _setup_logger(self) -> logging.Logger:
        """Setup logging"""
        logger = logging.getLogger('SmsFeatureEngineer')
        logger.setLevel(logging.INFO)
        
        if not logger.handlers:
            handler = logging.StreamHandler()
            formatter = logging.Formatter(
                '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
            )
            handler.setFormatter(formatter)
            logger.addHandler(handler)
        
        return logger
    
    def _get_spam_keywords(self) -> List[str]:
        """Get spam indicator keywords"""
        return [
            'winner', 'congratulations', 'prize', 'free', 'urgent', 'claim',
            'limited', 'offer', 'deal', 'discount', 'sale', 'buy', 'win',
            'selected', 'lucky', 'reward', 'cash', 'money', 'earn', 'rich',
            'guaranteed', 'risk-free', 'amazing', 'incredible', 'exclusive'
        ]
    
    def _get_urgency_keywords(self) -> List[str]:
        """Get urgency indicator keywords"""
        return [
            'urgent', 'immediate', 'asap', 'hurry', 'quick', 'fast', 'now',
            'today', 'expire', 'deadline', 'limited time', 'act now',
            'don\'t wait', 'hurry up', 'last chance', 'final notice'
        ]
    
    def _get_financial_keywords(self) -> List[str]:
        """Get financial/banking related keywords"""
        return [
            'bank', 'account', 'credit', 'debit', 'payment', 'transaction',
            'balance', 'amount', 'deposit', 'withdraw', 'transfer', 'loan',
            'card', 'otp', 'pin', 'password', 'verify', 'security'
        ]
    
    def _get_promotional_keywords(self) -> List[str]:
        """Get promotional message keywords"""
        return [
            'subscribe', 'unsubscribe', 'newsletter', 'promotion', 'marketing',
            'advertisement', 'brand', 'product', 'service', 'company',
            'business', 'store', 'shop', 'buy', 'purchase', 'order'
        ]
    
    def extract_text_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Extract comprehensive text-based features"""
        self.logger.info("Extracting text features...")
        
        df = df.copy()
        
        # Basic text statistics
        df['char_count'] = df['content'].str.len()
        df['word_count'] = df['content'].str.split().str.len()
        df['sentence_count'] = df['content'].apply(lambda x: len(re.split(r'[.!?]+', x)))
        df['avg_word_length'] = df['content'].apply(
            lambda x: np.mean([len(word) for word in x.split()]) if x.split() else 0
        )
        
        # Character analysis
        df['digit_count'] = df['content'].str.count(r'\d')
        df['special_char_count'] = df['content'].str.count(r'[^a-zA-Z0-9\s]')
        df['uppercase_count'] = df['content'].str.count(r'[A-Z]')
        df['uppercase_ratio'] = df['uppercase_count'] / df['char_count'].replace(0, 1)
        
        # Punctuation analysis
        df['exclamation_count'] = df['content'].str.count('!')
        df['question_count'] = df['content'].str.count(r'\?')
        df['period_count'] = df['content'].str.count(r'\.')
        df['comma_count'] = df['content'].str.count(',')
        
        # URL and contact analysis
        df['url_count'] = df['content'].str.count(r'http[s]?://|www\.|[a-zA-Z0-9-]+\.(com|org|net|edu|gov)')
        df['phone_count'] = df['content'].str.count(r'\b\d{3}[-.]?\d{3}[-.]?\d{4}\b')
        df['email_count'] = df['content'].str.count(r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b')
        
        # SMS-specific patterns
        df['has_shortcode'] = df['content'].str.contains(r'\b\d{4,6}\b', na=False).astype(int)
        df['has_currency'] = df['content'].str.contains(r'[$£€₹¥]|\b(dollar|pound|euro|rupee|yen)s?\b', case=False, na=False).astype(int)
        df['has_percentage'] = df['content'].str.contains(r'\d+%', na=False).astype(int)
        
        # Keyword-based features
        df['spam_keyword_count'] = df['content_clean'].apply(
            lambda x: sum(1 for keyword in self.spam_keywords if keyword in x.lower())
        )
        df['urgency_keyword_count'] = df['content_clean'].apply(
            lambda x: sum(1 for keyword in self.urgency_keywords if keyword in x.lower())
        )
        df['financial_keyword_count'] = df['content_clean'].apply(
            lambda x: sum(1 for keyword in self.financial_keywords if keyword in x.lower())
        )
        df['promotional_keyword_count'] = df['content_clean'].apply(
            lambda x: sum(1 for keyword in self.promotional_keywords if keyword in x.lower())
        )
        
        return df
    
    def extract_sender_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Extract sender-based features"""
        self.logger.info("Extracting sender features...")
        
        df = df.copy()
        
        # Basic sender analysis
        df['sender_length'] = df['sender'].str.len()
        df['sender_is_number'] = df['sender'].str.match(r'^\+?\d+$').astype(int)
        df['sender_is_shortcode'] = df['sender'].str.match(r'^\d{4,6}$').astype(int)
        df['sender_has_letters'] = df['sender'].str.contains(r'[a-zA-Z]', na=False).astype(int)
        df['sender_has_special_chars'] = df['sender'].str.contains(r'[^a-zA-Z0-9+]', na=False).astype(int)
        
        # International number patterns
        df['sender_is_international'] = df['sender'].str.startswith('+').astype(int)
        df['sender_country_code_length'] = df['sender'].apply(
            lambda x: len(re.findall(r'^\+(\d{1,3})', str(x))[0]) if x.startswith('+') and re.findall(r'^\+(\d{1,3})', str(x)) else 0
        )
        
        # Sender patterns common in spam
        df['sender_all_caps'] = df['sender'].apply(lambda x: x.isupper() and x.isalpha()).astype(int)
        df['sender_mixed_case'] = df['sender'].apply(
            lambda x: any(c.isupper() for c in x) and any(c.islower() for c in x) and x.isalpha()
        ).astype(int)
        
        return df
    
    def extract_temporal_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Extract time-based features"""
        self.logger.info("Extracting temporal features...")
        
        df = df.copy()
        
        # Basic time features (already computed in preprocessing)
        # Add more sophisticated temporal features
        
        # Time of day categories
        df['time_category'] = pd.cut(
            df['hour'], 
            bins=[0, 6, 12, 18, 24], 
            labels=['night', 'morning', 'afternoon', 'evening'],
            right=False
        )
        
        # Business hours (9 AM to 5 PM on weekdays)
        df['is_business_hours'] = (
            (df['hour'] >= 9) & (df['hour'] < 17) & (df['day_of_week'] < 5)
        ).astype(int)
        
        # Late night messages (often spam)
        df['is_late_night'] = ((df['hour'] >= 23) | (df['hour'] < 6)).astype(int)
        
        # Peak spam hours (based on common spam patterns)
        df['is_spam_peak_hour'] = df['hour'].isin([10, 11, 14, 15, 16]).astype(int)
        
        return df
    
    def extract_sentiment_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Extract sentiment and emotion features"""
        self.logger.info("Extracting sentiment features...")
        
        df = df.copy()
        
        # Basic sentiment analysis using TextBlob
        df['sentiment_polarity'] = df['content_clean'].apply(
            lambda x: TextBlob(x).sentiment.polarity
        )
        df['sentiment_subjectivity'] = df['content_clean'].apply(
            lambda x: TextBlob(x).sentiment.subjectivity
        )
        
        # Sentiment categories
        df['sentiment_positive'] = (df['sentiment_polarity'] > 0.1).astype(int)
        df['sentiment_negative'] = (df['sentiment_polarity'] < -0.1).astype(int)
        df['sentiment_neutral'] = (
            (df['sentiment_polarity'] >= -0.1) & (df['sentiment_polarity'] <= 0.1)
        ).astype(int)
        
        # Excitement/enthusiasm indicators
        df['excitement_score'] = (
            df['exclamation_count'] * 0.3 +
            df['uppercase_ratio'] * 0.4 +
            (df['sentiment_polarity'] > 0.5).astype(int) * 0.3
        )
        
        return df
    
    def create_tfidf_features(self, df: pd.DataFrame, fit: bool = True) -> Tuple[np.ndarray, List[str]]:
        """Create TF-IDF features from text content"""
        self.logger.info("Creating TF-IDF features...")
        
        if fit or self.tfidf_vectorizer is None:
            self.tfidf_vectorizer = TfidfVectorizer(
                max_features=self.max_tfidf_features,
                stop_words='english',
                ngram_range=(1, 2),  # Include bigrams
                min_df=2,  # Ignore terms that appear in less than 2 documents
                max_df=0.95,  # Ignore terms that appear in more than 95% of documents
                lowercase=True,
                strip_accents='unicode'
            )
            tfidf_features = self.tfidf_vectorizer.fit_transform(df['content_clean'])
        else:
            tfidf_features = self.tfidf_vectorizer.transform(df['content_clean'])
        
        # Get feature names
        feature_names = [f'tfidf_{name}' for name in self.tfidf_vectorizer.get_feature_names_out()]
        
        return tfidf_features.toarray(), feature_names
    
    def engineer_features(self, df: pd.DataFrame, fit: bool = True) -> Tuple[pd.DataFrame, np.ndarray, List[str]]:
        """
        Complete feature engineering pipeline
        
        Args:
            df: Input DataFrame with SMS data
            fit: Whether to fit transformers (True for training, False for inference)
            
        Returns:
            Tuple of (structured_features_df, tfidf_features_array, all_feature_names)
        """
        self.logger.info("Starting comprehensive feature engineering...")
        
        # Start with input DataFrame
        features_df = df.copy()
        
        # Extract all feature types
        features_df = self.extract_text_features(features_df)
        features_df = self.extract_sender_features(features_df)
        features_df = self.extract_temporal_features(features_df)
        features_df = self.extract_sentiment_features(features_df)
        
        # Handle categorical features
        if fit:
            # Encode time category
            features_df['time_category_encoded'] = self.label_encoder.fit_transform(
                features_df['time_category'].astype(str)
            )
        else:
            features_df['time_category_encoded'] = self.label_encoder.transform(
                features_df['time_category'].astype(str)
            )
        
        # Create TF-IDF features
        tfidf_features, tfidf_feature_names = self.create_tfidf_features(features_df, fit=fit)
        
        # Select numerical features for structured learning
        numerical_features = [
            'char_count', 'word_count', 'sentence_count', 'avg_word_length',
            'digit_count', 'special_char_count', 'uppercase_count', 'uppercase_ratio',
            'exclamation_count', 'question_count', 'period_count', 'comma_count',
            'url_count', 'phone_count', 'email_count', 'has_shortcode', 'has_currency', 'has_percentage',
            'spam_keyword_count', 'urgency_keyword_count', 'financial_keyword_count', 'promotional_keyword_count',
            'sender_length', 'sender_is_number', 'sender_is_shortcode', 'sender_has_letters', 'sender_has_special_chars',
            'sender_is_international', 'sender_country_code_length', 'sender_all_caps', 'sender_mixed_case',
            'hour', 'day_of_week', 'is_weekend', 'is_business_hours', 'is_late_night', 'is_spam_peak_hour',
            'time_category_encoded',
            'sentiment_polarity', 'sentiment_subjectivity', 'sentiment_positive', 'sentiment_negative', 'sentiment_neutral',
            'excitement_score'
        ]
        
        # Scale numerical features
        structured_features = features_df[numerical_features].fillna(0)
        
        if fit:
            structured_features_scaled = self.scaler.fit_transform(structured_features)
        else:
            structured_features_scaled = self.scaler.transform(structured_features)
        
        structured_features_df = pd.DataFrame(
            structured_features_scaled,
            columns=numerical_features,
            index=features_df.index
        )
        
        # Combine all feature names
        all_feature_names = numerical_features + tfidf_feature_names
        
        self.logger.info(f"Feature engineering completed: {len(numerical_features)} structured + {len(tfidf_feature_names)} TF-IDF features")
        
        return structured_features_df, tfidf_features, all_feature_names
    
    def save_feature_extractors(self, output_dir: str):
        """Save fitted feature extractors for later use"""
        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)
        
        # Save TF-IDF vectorizer
        if self.tfidf_vectorizer:
            joblib.dump(self.tfidf_vectorizer, output_path / 'tfidf_vectorizer.joblib')
        
        # Save scaler
        joblib.dump(self.scaler, output_path / 'feature_scaler.joblib')
        
        # Save label encoder
        joblib.dump(self.label_encoder, output_path / 'label_encoder.joblib')
        
        self.logger.info(f"Feature extractors saved to: {output_dir}")
    
    def load_feature_extractors(self, input_dir: str):
        """Load fitted feature extractors"""
        input_path = Path(input_dir)
        
        # Load TF-IDF vectorizer
        tfidf_path = input_path / 'tfidf_vectorizer.joblib'
        if tfidf_path.exists():
            self.tfidf_vectorizer = joblib.load(tfidf_path)
        
        # Load scaler
        scaler_path = input_path / 'feature_scaler.joblib'
        if scaler_path.exists():
            self.scaler = joblib.load(scaler_path)
        
        # Load label encoder
        encoder_path = input_path / 'label_encoder.joblib'
        if encoder_path.exists():
            self.label_encoder = joblib.load(encoder_path)
        
        self.logger.info(f"Feature extractors loaded from: {input_dir}")

def main():
    """Main function to demonstrate feature engineering"""
    import sys
    sys.path.append(str(Path(__file__).parent.parent))
    
    # Load processed data
    data_path = "data/processed/sms_data_processed.csv"
    df = pd.read_csv(data_path)
    
    print(f"📊 Loaded {len(df)} SMS messages for feature engineering")
    
    # Initialize feature engineer
    feature_engineer = SmsFeatureEngineer(max_tfidf_features=1000)
    
    # Engineer features
    structured_features, tfidf_features, feature_names = feature_engineer.engineer_features(df, fit=True)
    
    # Combine all features
    all_features = np.hstack([structured_features.values, tfidf_features])
    
    print(f"\n✅ Feature engineering completed!")
    print(f"📈 Total features: {all_features.shape[1]}")
    print(f"   - Structured features: {structured_features.shape[1]}")
    print(f"   - TF-IDF features: {tfidf_features.shape[1]}")
    
    # Save features and extractors
    output_dir = "models/experiments"
    np.save(f"{output_dir}/features.npy", all_features)
    np.save(f"{output_dir}/labels.npy", df['category'].values)
    
    feature_engineer.save_feature_extractors(output_dir)
    
    with open(f"{output_dir}/feature_names.txt", 'w') as f:
        for name in feature_names:
            f.write(f"{name}\n")
    
    print(f"💾 Features and extractors saved to: {output_dir}")
    
    return all_features, df['category'].values, feature_names

if __name__ == "__main__":
    main()