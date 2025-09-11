# ML Training Guide - Smart SMS Filter

## Overview
This guide will walk you through creating, training, and deploying a custom ML model for SMS classification. No prior ML experience required!

## Phase 1: Dataset Creation Strategy

### 1.1 Synthetic Dataset Generation
We'll start by creating a large synthetic dataset using LLM APIs:

**Indian SMS Categories & Examples:**
- **OTP/Verification**: "123456 is your OTP for HDFC Bank. Valid for 10 mins."
- **Banking/Finance**: "Rs.5000 credited to A/c XX1234 on 11-JAN-25 by UPI/9876543210"
- **Promotional/Spam**: "URGENT! You've won Rs.50000! Call now to claim your prize!"
- **Transactional**: "Your order #12345 from Amazon is out for delivery"
- **Personal**: "Hey, are you coming to the party tonight?"

**Generation Strategy:**
1. Use ChatGPT/Claude to generate 1000+ examples per category
2. Include Indian banking terms, UPI patterns, DLT headers
3. Vary message lengths, formats, and styles
4. Include common misspellings and variations

### 1.2 Real Data Collection (Privacy-Safe)
```python
# We'll build a data collection component in your app that:
# 1. Hashes sender numbers (privacy protection)
# 2. Collects message patterns (not actual content)
# 3. Records user classifications
# 4. Exports anonymized training data
```

### 1.3 Public Datasets
- SMS Spam Collection Dataset (English)
- Indian SMS datasets (if available)
- Augment with Indian-specific patterns

## Phase 2: Training Environment Setup

### 2.1 Python Environment
```bash
# We'll set this up together:
python -m venv sms_classifier
pip install tensorflow transformers pandas scikit-learn
pip install datasets tokenizers
```

### 2.2 Model Architecture Choice
**Option 1: DistilBERT (Recommended)**
- Pre-trained on English text
- Fast inference, good for mobile
- ~66MB model size

**Option 2: Custom LSTM**
- Smaller model (~10MB)
- Custom vocabulary for SMS patterns
- Faster training

**Option 3: Hybrid Approach**
- Rule-based + Small neural network
- Best of both worlds

### 2.3 Training Pipeline Structure
```
data/
├── raw_sms_data.csv
├── processed/
│   ├── train.csv
│   ├── val.csv
│   └── test.csv
training/
├── data_preprocessing.py
├── model_architecture.py
├── train.py
├── evaluate.py
└── convert_to_tflite.py
models/
├── checkpoint/
├── best_model.h5
└── sms_classifier.tflite  # Final mobile model
```

## Phase 3: Data Preprocessing

### 3.1 Text Cleaning Pipeline
```python
def preprocess_sms(text):
    # Remove special characters but keep SMS-specific patterns
    # Normalize numbers (Rs.1000 -> Rs.AMOUNT)
    # Handle common abbreviations
    # Tokenization for model input
```

### 3.2 Feature Engineering
- Message length
- Number patterns (OTP, amounts, phone numbers)
- Time-based features
- Sender patterns
- Special character ratios

### 3.3 Label Encoding
```python
CATEGORIES = {
    'INBOX': 0,      # Important messages
    'FILTERED': 1,   # Spam/promotional
    'NEEDS_REVIEW': 2  # Uncertain
}
```

## Phase 4: Model Training Process

### 4.1 Training Configuration
```python
# Hyperparameters we'll tune:
BATCH_SIZE = 32
LEARNING_RATE = 2e-5
EPOCHS = 10
MAX_LENGTH = 128  # SMS are short
DROPOUT = 0.3
```

### 4.2 Training Steps
1. **Data Loading**: Load and split dataset
2. **Preprocessing**: Tokenization, padding
3. **Model Creation**: Define architecture
4. **Training Loop**: With validation
5. **Evaluation**: Precision, recall, F1-score
6. **Model Selection**: Best checkpoint

### 4.3 Evaluation Metrics
```python
# We'll track these metrics:
- Accuracy: Overall correctness
- Precision: True positives / (True positives + False positives)
- Recall: True positives / (True positives + False negatives)
- F1-Score: Harmonic mean of precision and recall
- Confusion Matrix: See which categories are confused
```

## Phase 5: Mobile Optimization

### 5.1 Model Quantization
```python
# Convert to TensorFlow Lite with quantization
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()
```

### 5.2 Size Optimization
- Target: <20MB model size
- Techniques: Pruning, quantization, knowledge distillation
- Benchmark: <100ms inference time on device

### 5.3 Android Integration
```kotlin
// We'll implement:
class TFLiteClassifier {
    fun loadModel(assetManager: AssetManager)
    fun preprocessText(sms: String): FloatArray
    fun classify(input: FloatArray): Classification
}
```

## Phase 6: Continuous Learning Pipeline

### 6.1 Feedback Collection
```kotlin
// User corrections feed back into training data
data class UserCorrection(
    val originalClassification: Category,
    val userCorrection: Category,
    val messageFeatures: MessageFeatures,
    val confidence: Float
)
```

### 6.2 Retraining Strategy
- Collect 100+ corrections → retrain
- A/B test new model vs current
- Gradual rollout if performance improves

### 6.3 Privacy-Preserving Learning
- Federated learning concepts
- On-device feature extraction
- No raw message content leaves device

## Phase 7: Performance Monitoring

### 7.1 Metrics to Track
- Classification accuracy over time
- User correction rates
- Model inference time
- Memory usage
- Battery impact

### 7.2 A/B Testing Framework
```kotlin
// We'll implement model comparison
class ClassifierManager {
    fun classifyWithAB(message: SmsMessage): Classification
    fun recordPerformance(modelId: String, metrics: Metrics)
    fun shouldSwitchModel(): Boolean
}
```

## Getting Started - Next Steps

1. **Start with Rule-Based System**: Get basic classification working
2. **Collect Initial Data**: Use your real SMS messages (privacy-safe)
3. **Generate Synthetic Dataset**: Create diverse training examples
4. **Set Up Training Environment**: Python + TensorFlow setup
5. **Train First Model**: Simple LSTM or DistilBERT
6. **Integrate and Test**: Deploy to Android app
7. **Iterate and Improve**: Based on real usage data

## Timeline Estimate
- **Week 1**: Rule-based classifier + data collection setup
- **Week 2**: Dataset creation and Python environment
- **Week 3**: Model training and optimization
- **Week 4**: Android integration and testing
- **Ongoing**: Continuous improvement and retraining

---

**Note**: I'll guide you through each step with detailed instructions, code examples, and troubleshooting help. No ML experience required!
