# AI Model - TensorFlow Lite for Android

## Overview

This directory contains the AI model components for Smart SMS Filter's Android implementation. The model is based on Gemma 2B, fine-tuned and quantized for on-device mobile inference using TensorFlow Lite.

## Model Specifications

### Base Model
- **Architecture**: Gemma 2B (fine-tuned)
- **Framework**: TensorFlow Lite
- **Quantization**: 4-bit integer quantization
- **Model Size**: ~50MB (optimized for mobile)
- **Inference Time**: <100ms on average Android device

### Input/Output
- **Input**: Tokenized SMS text (max 512 tokens)
- **Output**: Classification probabilities
  - `spam`: Promotional/spam probability (0.0 - 1.0)
  - `important`: Important message probability (0.0 - 1.0) 
  - `uncertain`: Needs human review probability (0.0 - 1.0)

## Directory Structure

```
models/
├── tflite/
│   ├── sms_classifier_model.tflite    # Production model
│   ├── sms_classifier_dev.tflite      # Development model
│   ├── vocab.txt                      # Tokenizer vocabulary
│   └── model_metadata.json            # Model configuration
├── training/
│   ├── train_model.py                 # Training script
│   ├── data_preprocessing.py          # Data preparation
│   ├── model_architecture.py          # Model definition
│   └── convert_to_tflite.py          # TFLite conversion
├── datasets/
│   ├── synthetic_sms_data.csv         # LLM-generated training data
│   ├── public_spam_datasets/          # Public dataset sources
│   └── indian_sms_patterns.txt        # India-specific patterns
└── README.md                          # This file
```

## Model Development Workflow

### 1. Dataset Preparation
```bash
# Generate synthetic SMS data using LLM
python training/data_preprocessing.py --generate-synthetic --size 100000

# Combine with public datasets
python training/data_preprocessing.py --combine-datasets --output datasets/combined_training_data.csv
```

### 2. Model Training
```bash
# Train the base model
python training/train_model.py --data datasets/combined_training_data.csv --epochs 10

# Fine-tune for Indian SMS patterns
python training/train_model.py --finetune --indian-patterns datasets/indian_sms_patterns.txt
```

### 3. TensorFlow Lite Conversion
```bash
# Convert to TFLite with quantization
python training/convert_to_tflite.py --model checkpoints/best_model.h5 --quantize 4bit
```

### 4. Android Integration
```bash
# Copy model to Android assets
cp tflite/sms_classifier_model.tflite ../android/app/src/main/assets/
```

## Training Data Sources

### Synthetic Data Generation
- **LLM Used**: Gemini 2.5 Pro
- **Data Points**: 100,000+ diverse SMS examples
- **Categories**: Spam, Important (OTP, Banking, Personal), Promotional
- **Languages**: English, Hindi (Romanized), Mixed

### Public Datasets
- SMS Spam Collection Dataset (UCI)
- Enron SMS Dataset (filtered)
- Indian SMS datasets (public sources)

### India-Specific Patterns
- Banking SMS formats (SBI, HDFC, ICICI, etc.)
- OTP patterns from major services
- E-commerce notifications (Flipkart, Amazon, etc.)
- Government service messages
- Telecom operator messages

## Model Performance

### Target Metrics
- **Accuracy**: >95% on test set
- **Precision** (Spam): >90%
- **Recall** (Important): >98% (minimize false negatives)
- **F1-Score**: >93%
- **Inference Time**: <100ms on mid-range Android device

### Testing Protocol
1. Cross-validation on training data
2. Hold-out test set evaluation
3. Real-world SMS testing on Android devices
4. User feedback integration testing

## Model Deployment

### Android Integration
```kotlin
// Model loading in Android
class MessageClassifier(context: Context) {
    private val interpreter = Interpreter(loadModelFile(context, "sms_classifier_model.tflite"))
    
    fun classify(message: String): ClassificationResult {
        // Tokenize and classify SMS
    }
}
```

### Model Updates
- Models versioned with semantic versioning
- Over-the-air updates through app updates
- A/B testing for model improvements
- User feedback loop for continuous learning

## Optimization Strategies

### Mobile Optimization
- 4-bit quantization reduces model size by 75%
- Pruning removes unnecessary connections
- Knowledge distillation from larger models
- Hardware-specific optimizations (ARM NEON)

### Performance Monitoring
- Track inference times per device type
- Monitor memory usage during classification
- Battery impact measurement
- Model accuracy tracking in production

## Security & Privacy

### On-Device Processing
- No SMS data sent to external servers
- All classification happens locally
- Model files stored securely in app assets
- User feedback processed locally

### Model Protection
- Model obfuscation techniques applied
- Secure model loading and verification
- Protection against model extraction
- Encrypted model storage (future enhancement)

## Development Tools

### Required Dependencies
```python
# Python environment
tensorflow>=2.13.0
transformers>=4.30.0
numpy>=1.24.0
pandas>=2.0.0
scikit-learn>=1.3.0
```

### Testing Tools
- TensorFlow Lite benchmark tool
- Android profiling tools
- Custom SMS generation scripts
- Model validation utilities

## Future Enhancements

### Model Improvements
- Multi-language support (Hindi, Tamil, Bengali)
- Context-aware classification
- Personalization based on user behavior
- Federated learning implementation

### Technical Upgrades
- Dynamic model updates
- Edge TPU optimization
- Quantization to INT8
- Model compression improvements

## Contributing

1. Follow TensorFlow best practices
2. Test models on diverse Android devices
3. Validate against Indian SMS patterns
4. Document all changes and improvements
5. Run full test suite before commits

## License

Model files and training code follow the same license as the main project.
