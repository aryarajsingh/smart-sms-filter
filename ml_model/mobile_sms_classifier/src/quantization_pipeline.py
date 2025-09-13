#!/usr/bin/env python3
"""
Model Quantization Pipeline for Mobile SMS Classifier
Convert trained models to optimized TensorFlow Lite format for smartphone deployment
Target: <20MB size, <100ms inference, >90% accuracy retention
"""

import tensorflow as tf
import numpy as np
import json
import logging
import time
import os
import tempfile
from pathlib import Path
from typing import Dict, List, Tuple, Optional, Any
from dataclasses import dataclass
import matplotlib.pyplot as plt
import seaborn as sns

# Import mobile classifier
from mobile_classifier import MobileSmsClassifier, MobileClassificationResult

logger = logging.getLogger(__name__)

@dataclass
class QuantizationMetrics:
    """Metrics comparing original and quantized models"""
    original_size_mb: float
    quantized_size_mb: float
    size_reduction_ratio: float
    original_accuracy: float
    quantized_accuracy: float
    accuracy_retention: float
    original_inference_ms: float
    quantized_inference_ms: float
    speedup_ratio: float
    quantization_method: str

@dataclass
class TFLiteModelInfo:
    """Information about the TensorFlow Lite model"""
    model_path: str
    size_mb: float
    input_shape: Tuple[int, ...]
    output_shape: Tuple[int, ...]
    quantization_type: str
    inference_time_ms: float
    accuracy: float
    metadata: Dict[str, Any]

class ModelQuantizer:
    """
    Quantizes trained SMS classification models for mobile deployment
    Supports multiple quantization strategies optimized for smartphones
    """
    
    QUANTIZATION_METHODS = {
        'dynamic': 'Dynamic Range Quantization',
        'int8': 'Integer Quantization (INT8)',
        'float16': 'Float16 Quantization',
        'int8_fallback': 'INT8 with Float Fallback'
    }
    
    def __init__(self, classifier: MobileSmsClassifier):
        """
        Initialize quantizer with trained classifier
        
        Args:
            classifier: Trained MobileSmsClassifier instance
        """
        self.classifier = classifier
        self.quantization_metrics = {}
        
        if classifier.model is None:
            raise ValueError("Classifier must have a trained model")
        
        logger.info("Model quantizer initialized")
    
    def create_representative_dataset(self, texts: List[str], batch_size: int = 1) -> tf.data.Dataset:
        """
        Create representative dataset for calibration during quantization
        
        Args:
            texts: Sample texts for calibration
            batch_size: Batch size for calibration
            
        Returns:
            TensorFlow dataset for calibration
        """
        # Preprocess texts using the classifier's preprocessing
        processed_data = self.classifier.preprocess_data(texts)
        
        def representative_data_gen():
            """Generator for representative data"""
            for i in range(0, len(texts), batch_size):
                end_idx = min(i + batch_size, len(texts))
                yield [
                    processed_data['input_ids'][i:end_idx],
                    processed_data['attention_mask'][i:end_idx]
                ]
        
        return representative_data_gen
    
    def quantize_dynamic(self, output_path: str) -> TFLiteModelInfo:
        """
        Apply dynamic range quantization (fastest, moderate compression)
        Best for: Balanced performance and size reduction
        """
        logger.info("Applying dynamic range quantization...")
        
        # Create converter
        converter = tf.lite.TFLiteConverter.from_keras_model(self.classifier.model)
        
        # Enable dynamic range quantization
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        
        # Convert model
        start_time = time.time()
        quantized_model = converter.convert()
        conversion_time = time.time() - start_time
        
        # Save model
        with open(output_path, 'wb') as f:
            f.write(quantized_model)
        
        # Get model info
        model_info = self._get_tflite_model_info(
            output_path, quantized_model, 'dynamic', conversion_time
        )
        
        logger.info(f"Dynamic quantization complete: {model_info.size_mb:.1f}MB")
        return model_info
    
    def quantize_int8(self, 
                     output_path: str, 
                     calibration_texts: List[str],
                     use_fallback: bool = True) -> TFLiteModelInfo:
        """
        Apply integer quantization with INT8 precision
        Best for: Maximum size reduction and speed
        
        Args:
            output_path: Path to save quantized model
            calibration_texts: Texts for calibration
            use_fallback: Whether to allow float fallback for unsupported ops
        """
        logger.info("Applying INT8 quantization...")
        
        # Create converter
        converter = tf.lite.TFLiteConverter.from_keras_model(self.classifier.model)
        
        # Enable quantization
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        
        # Set up representative dataset for calibration
        converter.representative_dataset = self.create_representative_dataset(calibration_texts)
        
        if use_fallback:
            # Allow float fallback for unsupported operations
            converter.target_spec.supported_types = [tf.int8]
            quantization_type = 'int8_fallback'
        else:
            # Force INT8 for everything
            converter.target_spec.supported_types = [tf.int8]
            converter.inference_input_type = tf.int8
            converter.inference_output_type = tf.int8
            quantization_type = 'int8'
        
        # Convert model
        start_time = time.time()
        try:
            quantized_model = converter.convert()
            conversion_time = time.time() - start_time
        except Exception as e:
            logger.error(f"INT8 quantization failed: {e}")
            if not use_fallback:
                logger.info("Retrying with float fallback...")
                return self.quantize_int8(output_path, calibration_texts, use_fallback=True)
            raise
        
        # Save model
        with open(output_path, 'wb') as f:
            f.write(quantized_model)
        
        # Get model info
        model_info = self._get_tflite_model_info(
            output_path, quantized_model, quantization_type, conversion_time
        )
        
        logger.info(f"INT8 quantization complete: {model_info.size_mb:.1f}MB")
        return model_info
    
    def quantize_float16(self, output_path: str) -> TFLiteModelInfo:
        """
        Apply float16 quantization
        Best for: Good balance between size and accuracy
        """
        logger.info("Applying Float16 quantization...")
        
        # Create converter
        converter = tf.lite.TFLiteConverter.from_keras_model(self.classifier.model)
        
        # Enable float16 quantization
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.target_spec.supported_types = [tf.float16]
        
        # Convert model
        start_time = time.time()
        quantized_model = converter.convert()
        conversion_time = time.time() - start_time
        
        # Save model
        with open(output_path, 'wb') as f:
            f.write(quantized_model)
        
        # Get model info
        model_info = self._get_tflite_model_info(
            output_path, quantized_model, 'float16', conversion_time
        )
        
        logger.info(f"Float16 quantization complete: {model_info.size_mb:.1f}MB")
        return model_info
    
    def quantize_all_methods(self, 
                           output_dir: str,
                           calibration_texts: List[str]) -> Dict[str, TFLiteModelInfo]:
        """
        Apply all quantization methods and compare results
        
        Args:
            output_dir: Directory to save quantized models
            calibration_texts: Texts for calibration
            
        Returns:
            Dictionary mapping quantization method to model info
        """
        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)
        
        results = {}
        
        # Dynamic quantization
        try:
            dynamic_path = output_path / "mobile_classifier_dynamic.tflite"
            results['dynamic'] = self.quantize_dynamic(str(dynamic_path))
        except Exception as e:
            logger.error(f"Dynamic quantization failed: {e}")
        
        # Float16 quantization
        try:
            float16_path = output_path / "mobile_classifier_float16.tflite"
            results['float16'] = self.quantize_float16(str(float16_path))
        except Exception as e:
            logger.error(f"Float16 quantization failed: {e}")
        
        # INT8 quantization with fallback
        try:
            int8_path = output_path / "mobile_classifier_int8.tflite"
            results['int8'] = self.quantize_int8(str(int8_path), calibration_texts, use_fallback=True)
        except Exception as e:
            logger.error(f"INT8 quantization failed: {e}")
        
        logger.info(f"Generated {len(results)} quantized models")
        return results
    
    def _get_tflite_model_info(self, 
                              model_path: str, 
                              model_content: bytes,
                              quantization_type: str,
                              conversion_time: float) -> TFLiteModelInfo:
        """Get detailed information about TensorFlow Lite model"""
        
        # Calculate size
        size_mb = len(model_content) / (1024 * 1024)
        
        # Create interpreter to get shape info
        interpreter = tf.lite.Interpreter(model_content=model_content)
        interpreter.allocate_tensors()
        
        # Get input/output details
        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()
        
        input_shape = tuple(input_details[0]['shape'])
        output_shape = tuple(output_details[0]['shape'])
        
        # Basic metadata
        metadata = {
            'conversion_time_seconds': conversion_time,
            'input_details': input_details,
            'output_details': output_details,
            'num_tensors': len(interpreter.get_tensor_details())
        }
        
        return TFLiteModelInfo(
            model_path=model_path,
            size_mb=size_mb,
            input_shape=input_shape,
            output_shape=output_shape,
            quantization_type=quantization_type,
            inference_time_ms=0.0,  # Will be measured separately
            accuracy=0.0,  # Will be evaluated separately
            metadata=metadata
        )
    
    def benchmark_tflite_model(self, 
                              model_path: str, 
                              test_texts: List[str],
                              num_runs: int = 100) -> Dict[str, float]:
        """
        Benchmark TensorFlow Lite model performance
        
        Args:
            model_path: Path to TFLite model
            test_texts: Test texts for benchmarking
            num_runs: Number of inference runs for timing
            
        Returns:
            Performance metrics
        """
        # Load model
        with open(model_path, 'rb') as f:
            model_content = f.read()
        
        interpreter = tf.lite.Interpreter(model_content=model_content)
        interpreter.allocate_tensors()
        
        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()
        
        # Benchmark inference time
        times = []
        sample_text = test_texts[0] if test_texts else "Test message"
        
        # Preprocess sample
        processed = self.classifier.preprocess_data([sample_text])
        
        for _ in range(num_runs):
            start_time = time.time()
            
            # Set input tensors
            interpreter.set_tensor(
                input_details[0]['index'], 
                processed['input_ids'].numpy()
            )
            interpreter.set_tensor(
                input_details[1]['index'], 
                processed['attention_mask'].numpy()
            )
            
            # Run inference
            interpreter.invoke()
            
            # Get output
            _ = interpreter.get_tensor(output_details[0]['index'])
            
            times.append((time.time() - start_time) * 1000)  # Convert to ms
        
        # Calculate statistics
        avg_time = np.mean(times)
        min_time = np.min(times)
        max_time = np.max(times)
        std_time = np.std(times)
        
        return {
            'avg_inference_time_ms': avg_time,
            'min_inference_time_ms': min_time,
            'max_inference_time_ms': max_time,
            'std_inference_time_ms': std_time,
            'total_benchmark_time_s': sum(times) / 1000
        }
    
    def evaluate_quantized_models(self,
                                 quantized_models: Dict[str, TFLiteModelInfo],
                                 test_texts: List[str],
                                 test_labels: List[int]) -> Dict[str, QuantizationMetrics]:
        """
        Evaluate all quantized models and compare with original
        
        Args:
            quantized_models: Dictionary of quantized model infos
            test_texts: Test texts for evaluation
            test_labels: True labels for evaluation
            
        Returns:
            Dictionary of quantization metrics for each method
        """
        logger.info("Evaluating quantized models...")
        
        # Get original model metrics
        original_size = self.classifier._calculate_model_size()
        original_metrics = self.classifier.evaluate_model(test_texts, test_labels)
        original_accuracy = original_metrics['accuracy']
        original_inference_time = original_metrics['avg_inference_time_ms']
        
        evaluation_results = {}
        
        for method, model_info in quantized_models.items():
            logger.info(f"Evaluating {method} quantization...")
            
            # Benchmark inference time
            benchmark_results = self.benchmark_tflite_model(
                model_info.model_path, test_texts[:10]  # Use subset for benchmarking
            )
            
            # Evaluate accuracy using TFLite model
            quantized_accuracy = self._evaluate_tflite_accuracy(
                model_info.model_path, test_texts, test_labels
            )
            
            # Calculate metrics
            size_reduction = original_size / model_info.size_mb
            accuracy_retention = quantized_accuracy / original_accuracy
            speedup = original_inference_time / benchmark_results['avg_inference_time_ms']
            
            metrics = QuantizationMetrics(
                original_size_mb=original_size,
                quantized_size_mb=model_info.size_mb,
                size_reduction_ratio=size_reduction,
                original_accuracy=original_accuracy,
                quantized_accuracy=quantized_accuracy,
                accuracy_retention=accuracy_retention,
                original_inference_ms=original_inference_time,
                quantized_inference_ms=benchmark_results['avg_inference_time_ms'],
                speedup_ratio=speedup,
                quantization_method=method
            )
            
            evaluation_results[method] = metrics
            
            logger.info(f"{method}: {model_info.size_mb:.1f}MB "
                       f"({size_reduction:.1f}x smaller), "
                       f"{quantized_accuracy:.3f} accuracy "
                       f"({accuracy_retention:.1%} retention)")
        
        return evaluation_results
    
    def _evaluate_tflite_accuracy(self,
                                 model_path: str,
                                 test_texts: List[str],
                                 test_labels: List[int]) -> float:
        """Evaluate accuracy of TensorFlow Lite model"""
        
        # Load model
        with open(model_path, 'rb') as f:
            model_content = f.read()
        
        interpreter = tf.lite.Interpreter(model_content=model_content)
        interpreter.allocate_tensors()
        
        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()
        
        correct_predictions = 0
        
        for text, true_label in zip(test_texts, test_labels):
            # Preprocess input
            processed = self.classifier.preprocess_data([text])
            
            # Set input tensors
            interpreter.set_tensor(
                input_details[0]['index'], 
                processed['input_ids'].numpy()
            )
            interpreter.set_tensor(
                input_details[1]['index'], 
                processed['attention_mask'].numpy()
            )
            
            # Run inference
            interpreter.invoke()
            
            # Get prediction
            output_data = interpreter.get_tensor(output_details[0]['index'])
            predicted_label = np.argmax(output_data[0])
            
            if predicted_label == true_label:
                correct_predictions += 1
        
        accuracy = correct_predictions / len(test_texts)
        return accuracy
    
    def select_best_quantization(self, 
                               evaluation_results: Dict[str, QuantizationMetrics],
                               size_threshold_mb: float = 20.0,
                               accuracy_threshold: float = 0.90) -> str:
        """
        Select the best quantization method based on constraints
        
        Args:
            evaluation_results: Results from evaluate_quantized_models
            size_threshold_mb: Maximum allowed model size in MB
            accuracy_threshold: Minimum accuracy retention ratio
            
        Returns:
            Best quantization method name
        """
        logger.info(f"Selecting best quantization (size <= {size_threshold_mb}MB, "
                   f"accuracy >= {accuracy_threshold:.1%})")
        
        valid_methods = []
        
        for method, metrics in evaluation_results.items():
            if (metrics.quantized_size_mb <= size_threshold_mb and 
                metrics.accuracy_retention >= accuracy_threshold):
                valid_methods.append((method, metrics))
        
        if not valid_methods:
            logger.warning("No quantization method meets all constraints!")
            # Fallback: choose the one with best size-accuracy trade-off
            best_method = min(evaluation_results.keys(), 
                            key=lambda m: evaluation_results[m].quantized_size_mb)
            logger.warning(f"Falling back to {best_method}")
            return best_method
        
        # Among valid methods, choose the one with smallest size
        best_method = min(valid_methods, key=lambda x: x[1].quantized_size_mb)[0]
        
        logger.info(f"Selected quantization method: {best_method}")
        return best_method
    
    def create_quantization_report(self,
                                 evaluation_results: Dict[str, QuantizationMetrics],
                                 output_path: str = "quantization_report.json") -> None:
        """Create detailed quantization report"""
        
        report = {
            'summary': {
                'original_model_size_mb': None,
                'original_accuracy': None,
                'quantization_methods_tested': list(evaluation_results.keys()),
                'best_method_by_size': None,
                'best_method_by_accuracy': None,
                'recommendation': None
            },
            'detailed_results': {},
            'performance_comparison': {}
        }
        
        if evaluation_results:
            # Fill summary
            first_result = next(iter(evaluation_results.values()))
            report['summary']['original_model_size_mb'] = first_result.original_size_mb
            report['summary']['original_accuracy'] = first_result.original_accuracy
            
            # Best methods
            best_size = min(evaluation_results.keys(), 
                          key=lambda k: evaluation_results[k].quantized_size_mb)
            best_accuracy = max(evaluation_results.keys(),
                              key=lambda k: evaluation_results[k].accuracy_retention)
            
            report['summary']['best_method_by_size'] = best_size
            report['summary']['best_method_by_accuracy'] = best_accuracy
            
            # Detailed results
            for method, metrics in evaluation_results.items():
                report['detailed_results'][method] = {
                    'quantized_size_mb': metrics.quantized_size_mb,
                    'size_reduction_ratio': metrics.size_reduction_ratio,
                    'quantized_accuracy': metrics.quantized_accuracy,
                    'accuracy_retention': metrics.accuracy_retention,
                    'inference_time_ms': metrics.quantized_inference_ms,
                    'speedup_ratio': metrics.speedup_ratio
                }
        
        # Save report
        with open(output_path, 'w') as f:
            json.dump(report, f, indent=2)
        
        logger.info(f"Quantization report saved to {output_path}")
    
    def plot_quantization_comparison(self,
                                   evaluation_results: Dict[str, QuantizationMetrics],
                                   save_path: str = None) -> None:
        """Create visualization comparing quantization methods"""
        
        if not evaluation_results:
            logger.warning("No evaluation results to plot")
            return
        
        fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(14, 10))
        
        methods = list(evaluation_results.keys())
        
        # Model sizes
        sizes = [evaluation_results[m].quantized_size_mb for m in methods]
        original_size = evaluation_results[methods[0]].original_size_mb
        
        ax1.bar(methods, sizes, color='skyblue', alpha=0.7)
        ax1.axhline(y=original_size, color='red', linestyle='--', 
                   label=f'Original ({original_size:.1f}MB)')
        ax1.axhline(y=20, color='orange', linestyle='--', 
                   label='Mobile Target (20MB)')
        ax1.set_title('Model Size Comparison')
        ax1.set_ylabel('Size (MB)')
        ax1.legend()
        ax1.tick_params(axis='x', rotation=45)
        
        # Accuracy retention
        accuracies = [evaluation_results[m].accuracy_retention for m in methods]
        
        ax2.bar(methods, accuracies, color='lightgreen', alpha=0.7)
        ax2.axhline(y=1.0, color='red', linestyle='--', label='Original Accuracy')
        ax2.axhline(y=0.9, color='orange', linestyle='--', label='90% Threshold')
        ax2.set_title('Accuracy Retention')
        ax2.set_ylabel('Accuracy Retention Ratio')
        ax2.legend()
        ax2.tick_params(axis='x', rotation=45)
        
        # Inference time
        inference_times = [evaluation_results[m].quantized_inference_ms for m in methods]
        original_time = evaluation_results[methods[0]].original_inference_ms
        
        ax3.bar(methods, inference_times, color='lightcoral', alpha=0.7)
        ax3.axhline(y=original_time, color='red', linestyle='--',
                   label=f'Original ({original_time:.1f}ms)')
        ax3.axhline(y=100, color='orange', linestyle='--', 
                   label='Mobile Target (100ms)')
        ax3.set_title('Inference Time Comparison')
        ax3.set_ylabel('Inference Time (ms)')
        ax3.legend()
        ax3.tick_params(axis='x', rotation=45)
        
        # Size vs Accuracy trade-off
        ax4.scatter(sizes, accuracies, s=100, alpha=0.7)
        for i, method in enumerate(methods):
            ax4.annotate(method, (sizes[i], accuracies[i]), 
                        xytext=(5, 5), textcoords='offset points')
        
        ax4.axvline(x=20, color='orange', linestyle='--', alpha=0.5, label='Size Target')
        ax4.axhline(y=0.9, color='orange', linestyle='--', alpha=0.5, label='Accuracy Target')
        ax4.set_xlabel('Model Size (MB)')
        ax4.set_ylabel('Accuracy Retention')
        ax4.set_title('Size vs Accuracy Trade-off')
        ax4.legend()
        ax4.grid(True, alpha=0.3)
        
        plt.tight_layout()
        
        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            logger.info(f"Quantization comparison plot saved to {save_path}")
        
        plt.show()

def main():
    """Test the quantization pipeline"""
    print("🔧 Model Quantization Pipeline for Mobile Deployment")
    print("=" * 60)
    
    # This would normally load a trained classifier
    # For demo, we'll create one with the architecture
    print("\n🏗️ Creating mobile classifier architecture...")
    classifier = MobileSmsClassifier()
    classifier.load_tokenizer()
    
    # Create model (in real usage, this would be trained)
    model = classifier.create_model()
    print(f"Model parameters: {model.count_params():,}")
    
    # Sample test data
    test_texts = [
        "Your OTP is 123456. Valid for 10 minutes.",
        "CONGRATULATIONS! You won Rs.50000! Call now!",
        "Hi, are you free for lunch today?",
        "Rs.2500 debited from A/c XX1234 via UPI.",
        "Your Amazon order is out for delivery."
    ]
    
    print(f"\n📊 Target Specifications:")
    print(f"📱 Model size: <20MB")
    print(f"⚡ Inference time: <100ms")
    print(f"🎯 Accuracy retention: >90%")
    
    print(f"\n🔧 Available quantization methods:")
    for method, description in ModelQuantizer.QUANTIZATION_METHODS.items():
        print(f"  • {method}: {description}")
    
    print(f"\n✅ Quantization pipeline ready!")
    print(f"📋 Next steps:")
    print(f"1. Train the mobile classifier")
    print(f"2. Run quantize_all_methods() with calibration data")
    print(f"3. Evaluate and select best quantization")
    print(f"4. Deploy to Android app")

if __name__ == "__main__":
    main()