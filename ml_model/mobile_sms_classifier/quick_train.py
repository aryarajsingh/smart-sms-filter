#!/usr/bin/env python3
"""
Quick Training Script for Mobile SMS Classifier
Uses existing Android app patterns + synthetic data generation
Target: Train a working model in <10 minutes
"""

import os
import sys
import logging
import pandas as pd
import numpy as np
from pathlib import Path
from sklearn.model_selection import train_test_split

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def create_training_data_from_android_patterns():
    """Create training data based on Android app classification patterns"""
    
    logger.info("Creating training data based on Android app patterns...")
    
    # SMS categories (matching mobile_classifier.py)
    categories = {
        'INBOX': 0, 'SPAM': 1, 'OTP': 2, 
        'BANKING': 3, 'ECOMMERCE': 4, 'NEEDS_REVIEW': 5
    }
    
    # Data based on Android app patterns (SimpleMessageClassifier.kt)
    training_data = []
    
    # OTP Messages (Category 2)
    otp_messages = [
        "123456 is your OTP for HDFC Bank. Valid for 10 minutes. Do not share.",
        "Your verification code is 987654. Valid for 5 minutes only.",
        "OTP: 456789 for SBI login. Do not share with anyone.",
        "Use 234567 to verify your account. One Time Password valid for 15 minutes.",
        "876543 is your security code for PayTM. Don't share with anyone.",
        "Dear Customer, 345678 is your OTP for ICICI Bank. Valid till 10 mins.",
        "Your OTP is 567890 for Amazon login. Valid for 10 minutes only.",
        "Verification code: 678901. Valid for 3 minutes. Do not share.",
        "123789 is your authentication code for Google. Expires in 5 minutes.",
        "Your login OTP is 456123. Valid for 10 minutes. Keep it confidential."
    ]
    for msg in otp_messages * 10:  # Multiply to get more samples
        training_data.append((msg, categories['OTP']))
    
    # Banking Messages (Category 3) 
    banking_messages = [
        "Rs.5000 debited from A/c XX1234 on 15-JAN-25 by UPI/9876543210. Balance: Rs.25000",
        "Rs.2500 credited to your account XX5678 from JOHN via UPI. Balance Rs.50000",
        "Dear Customer, Rs.1200 has been debited from A/c 9012 for electricity bill. Balance Rs.12500",
        "IMPS transfer of Rs.10000 from HDFC A/c XX3456 to RAVI successful. Ref: UPI12345",
        "Your SBI account XX7890 credited with Rs.7500 from SALARY on 15-JAN-25. Available bal Rs.75000",
        "ATM withdrawal of Rs.3000 from SBI ATM. A/c XX1111 Balance Rs.22000",
        "EMI of Rs.15000 debited from A/c XX2222 for home loan. Balance Rs.45000",
        "Interest of Rs.250 credited to your savings account XX3333. Balance Rs.18000",
        "RTGS transfer of Rs.50000 from A/c XX4444 to BUSINESS ACCOUNT successful",
        "Card payment of Rs.1500 at AMAZON INDIA declined. Insufficient balance."
    ]
    for msg in banking_messages * 8:
        training_data.append((msg, categories['BANKING']))
    
    # Spam Messages (Category 1)
    spam_messages = [
        "🎉 CONGRATULATIONS! You've won Rs.50000! Call 9876543210 to claim your prize NOW!",
        "URGENT: You've won iPhone 15! Click http://bit.ly/claim123 to claim within 24 hours!",
        "LIMITED TIME OFFER! 90% OFF on everything! Shop now at tinyurl.com/offer",
        "FREE Laptop for first 100 customers! Call 8888888888 immediately!",
        "WINNER WINNER! You're selected for Car worth Rs.500000! Call 7777777777 now!",
        "HURRY! Last chance to win 1 LAKH CASH! SMS WIN to 56789",
        "Congratulations winner! You won lottery prize. Click here to claim bonus money",
        "URGENT ALERT: Your account will be suspended. Click link to verify immediately",
        "FLASH SALE: Get 70% discount on mobiles. Limited time offer. Buy now!",
        "You are pre-approved for credit card with 5 lakh limit. Call now for instant approval"
    ]
    for msg in spam_messages * 12:
        training_data.append((msg, categories['SPAM']))
    
    # E-commerce Messages (Category 4)
    ecommerce_messages = [
        "Your Amazon order #12345678 is out for delivery. Expected by 6 PM. Track: amzn.to/track",
        "Order #87654321 delivered successfully. Rate your experience on Flipkart app",
        "Your Myntra order #45678912 is shipped. Estimated delivery: 18-Jan-25",
        "Payment of Rs.2999 for order #11111111 confirmed. Thank you for shopping with us!",
        "Zomato: Your order #22222222 worth Rs.350 is being prepared for delivery",
        "Swiggy delivery update: Your order from McDonald's will arrive in 20 minutes",
        "Your Uber ride receipt: Rs.150 for trip on 15-Jan-25. Rate your driver",
        "OLA Money: Rs.200 added to wallet. Use OLA200 for 20% cashback on next ride",
        "BigBasket: Your grocery order #33333333 will be delivered tomorrow morning",
        "Your refund of Rs.1500 for order #44444444 has been processed to your account"
    ]
    for msg in ecommerce_messages * 6:
        training_data.append((msg, categories['ECOMMERCE']))
    
    # Inbox Messages (Category 0) - Important personal/business
    inbox_messages = [
        "Hi, are you free for lunch today? Let me know!",
        "Meeting rescheduled to 3 PM tomorrow. Please confirm attendance.",
        "Happy birthday! Hope you have a wonderful day ahead!",
        "Don't forget about the presentation at 10 AM today. Good luck!",
        "Thanks for your help with the project. Really appreciate it!",
        "Can you please send me the report by EOD? Thanks!",
        "Flight delay notification: Your flight AI 101 is delayed by 2 hours",
        "Your PNR 1234567890 for train journey on 20-Jan-25 is confirmed",
        "Appointment reminder: Doctor visit tomorrow at 4 PM. Bring reports",
        "Your electricity bill of Rs.1200 is due on 25th. Please pay to avoid disconnection"
    ]
    for msg in inbox_messages * 8:
        training_data.append((msg, categories['INBOX']))
    
    # Needs Review Messages (Category 5) - Ambiguous
    review_messages = [
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
    for msg in review_messages * 5:
        training_data.append((msg, categories['NEEDS_REVIEW']))
    
    logger.info(f"Generated {len(training_data)} training samples")
    return training_data

def train_lightweight_model():
    """Train a lightweight model suitable for mobile deployment"""
    
    try:
        import tensorflow as tf
        from tensorflow.keras.models import Sequential
        from tensorflow.keras.layers import Dense, Embedding, LSTM, Dropout
        from tensorflow.keras.preprocessing.text import Tokenizer
        from tensorflow.keras.preprocessing.sequence import pad_sequences
        from sklearn.metrics import classification_report, confusion_matrix
        
        logger.info("Starting model training...")
        
        # Create training data
        training_data = create_training_data_from_android_patterns()
        
        # Separate texts and labels
        texts = [item[0] for item in training_data]
        labels = [item[1] for item in training_data]
        
        # Convert to DataFrame for easier handling
        df = pd.DataFrame({'text': texts, 'label': labels})
        logger.info(f"Dataset shape: {df.shape}")
        logger.info(f"Label distribution:\n{df['label'].value_counts().sort_index()}")
        
        # Split data
        X_train, X_test, y_train, y_test = train_test_split(
            df['text'], df['label'], test_size=0.2, random_state=42, stratify=df['label']
        )
        
        # Tokenization
        logger.info("Tokenizing text...")
        max_words = 5000  # Reduced for mobile deployment
        max_len = 100     # Most SMS are much shorter
        
        tokenizer = Tokenizer(num_words=max_words, oov_token='<OOV>')
        tokenizer.fit_on_texts(X_train)
        
        # Convert text to sequences
        X_train_seq = tokenizer.texts_to_sequences(X_train)
        X_test_seq = tokenizer.texts_to_sequences(X_test)
        
        # Pad sequences
        X_train_pad = pad_sequences(X_train_seq, maxlen=max_len, padding='post', truncating='post')
        X_test_pad = pad_sequences(X_test_seq, maxlen=max_len, padding='post', truncating='post')
        
        logger.info(f"Training shape: {X_train_pad.shape}")
        logger.info(f"Test shape: {X_test_pad.shape}")
        
        # Build lightweight model (LSTM instead of DistilBERT for quick training)
        logger.info("Building lightweight LSTM model...")
        model = Sequential([
            Embedding(max_words, 64, input_length=max_len),  # Smaller embedding
            LSTM(32, return_sequences=False),  # Smaller LSTM
            Dropout(0.5),
            Dense(16, activation='relu'),
            Dropout(0.3),
            Dense(6, activation='softmax')  # 6 categories
        ])
        
        model.compile(
            optimizer='adam',
            loss='sparse_categorical_crossentropy',
            metrics=['accuracy']
        )
        
        logger.info("Model architecture:")
        model.summary()
        
        # Train model
        logger.info("Training model...")
        history = model.fit(
            X_train_pad, y_train,
            batch_size=32,
            epochs=5,  # Quick training
            validation_split=0.2,
            verbose=1
        )
        
        # Evaluate model
        logger.info("Evaluating model...")
        test_loss, test_acc = model.evaluate(X_test_pad, y_test, verbose=0)
        logger.info(f"Test accuracy: {test_acc:.4f}")
        
        # Predictions
        y_pred = model.predict(X_test_pad, verbose=0)
        y_pred_classes = np.argmax(y_pred, axis=1)
        
        # Classification report
        category_names = ['INBOX', 'SPAM', 'OTP', 'BANKING', 'ECOMMERCE', 'NEEDS_REVIEW']
        print("\nClassification Report:")
        print(classification_report(y_test, y_pred_classes, target_names=category_names))
        
        # Save model
        models_dir = Path("models")
        models_dir.mkdir(exist_ok=True)
        
        model_path = models_dir / "lightweight_sms_classifier.h5"
        model.save(str(model_path))
        logger.info(f"Model saved to {model_path}")
        
        # Save tokenizer
        import pickle
        tokenizer_path = models_dir / "tokenizer.pkl"
        with open(tokenizer_path, 'wb') as f:
            pickle.dump(tokenizer, f)
        logger.info(f"Tokenizer saved to {tokenizer_path}")
        
        # Convert to TensorFlow Lite
        logger.info("Converting to TensorFlow Lite...")
        converter = tf.lite.TFLiteConverter.from_keras_model(model)
        converter.optimizations = [tf.lite.Optimize.DEFAULT]  # Dynamic quantization
        
        tflite_model = converter.convert()
        
        tflite_path = models_dir / "lightweight_sms_classifier.tflite"
        with open(tflite_path, 'wb') as f:
            f.write(tflite_model)
        
        size_mb = len(tflite_model) / (1024 * 1024)
        logger.info(f"TensorFlow Lite model saved to {tflite_path}")
        logger.info(f"TFLite model size: {size_mb:.1f}MB")
        
        # Test TFLite model
        logger.info("Testing TensorFlow Lite model...")
        interpreter = tf.lite.Interpreter(model_content=tflite_model)
        interpreter.allocate_tensors()
        
        # Test with a sample
        sample_text = "Your OTP is 123456. Valid for 10 minutes."
        sample_seq = tokenizer.texts_to_sequences([sample_text])
        sample_pad = pad_sequences(sample_seq, maxlen=max_len, padding='post', truncating='post')
        
        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()
        
        interpreter.set_tensor(input_details[0]['index'], sample_pad.astype(np.float32))
        interpreter.invoke()
        
        output_data = interpreter.get_tensor(output_details[0]['index'])
        predicted_class = np.argmax(output_data[0])
        confidence = np.max(output_data[0])
        
        logger.info(f"Sample prediction: '{sample_text}'")
        logger.info(f"Predicted category: {category_names[predicted_class]} (confidence: {confidence:.3f})")
        
        # Create metadata
        metadata = {
            'model_type': 'LSTM',
            'categories': category_names,
            'max_words': max_words,
            'max_len': max_len,
            'test_accuracy': float(test_acc),
            'model_size_mb': float(size_mb),
            'training_samples': len(training_data)
        }
        
        import json
        metadata_path = models_dir / "model_metadata.json"
        with open(metadata_path, 'w') as f:
            json.dump(metadata, f, indent=2)
        logger.info(f"Metadata saved to {metadata_path}")
        
        logger.info("🎉 Training completed successfully!")
        logger.info(f"📱 Mobile-ready model: {size_mb:.1f}MB")
        logger.info(f"🎯 Test accuracy: {test_acc:.1%}")
        
        return {
            'model_path': str(model_path),
            'tflite_path': str(tflite_path),
            'tokenizer_path': str(tokenizer_path),
            'test_accuracy': test_acc,
            'model_size_mb': size_mb
        }
        
    except ImportError as e:
        logger.error(f"Missing dependencies: {e}")
        logger.error("Please run: pip install tensorflow scikit-learn pandas numpy")
        return None
    except Exception as e:
        logger.error(f"Training failed: {e}")
        return None

def main():
    """Main training function"""
    logger.info("🚀 Quick SMS Classifier Training")
    logger.info("=" * 50)
    
    # Train model
    result = train_lightweight_model()
    
    if result:
        print("\n" + "="*50)
        print("✅ SUCCESS: Mobile SMS Classifier Trained!")
        print("="*50)
        print(f"📁 Model files:")
        print(f"  • Keras model: {result['model_path']}")
        print(f"  • TFLite model: {result['tflite_path']} ({result['model_size_mb']:.1f}MB)")
        print(f"  • Tokenizer: {result['tokenizer_path']}")
        print(f"🎯 Test accuracy: {result['test_accuracy']:.1%}")
        print(f"📱 Ready for Android integration!")
        print("="*50)
    else:
        print("\n❌ Training failed. Please check the logs above.")

if __name__ == "__main__":
    main()