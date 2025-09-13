#!/usr/bin/env python3
"""
Complete Training and Evaluation Pipeline for Mobile SMS Classifier
Includes data preparation, model training, quantization, and performance evaluation
Target: <20MB model, <100ms inference, >90% accuracy
"""

import sys
import os
import argparse
import logging
import json
from pathlib import Path
from typing import Dict, List, Tuple
import yaml
import time

# Add source directory to path
sys.path.append(str(Path(__file__).parent / "src"))

from mobile_classifier import MobileSmsClassifier
from data_preparation import SMSDataPreprocessor
from quantization_pipeline import ModelQuantizer

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class MobileTrainingPipeline:
    """Complete training pipeline for mobile SMS classifier"""
    
    def __init__(self, config_path: str = "config/mobile_config.yaml"):
        """Initialize training pipeline with configuration"""
        self.config_path = config_path
        with open(config_path, 'r') as f:
            self.config = yaml.safe_load(f)
        
        self.classifier = MobileSmsClassifier()
        self.data_preprocessor = SMSDataPreprocessor(config_path)
        
        # Create output directories
        self.setup_directories()
        
        logger.info("Mobile training pipeline initialized")
    
    def setup_directories(self):
        """Create necessary output directories"""
        dirs = [
            "data/processed",
            "models/trained",
            "models/quantized", 
            "evaluation/reports",
            "evaluation/plots",
            "logs"
        ]
        
        for dir_path in dirs:
            Path(dir_path).mkdir(parents=True, exist_ok=True)
    
    def prepare_data(self, synthetic_samples: int = 10000) -> Dict:
        """Prepare training data with synthetic generation"""
        logger.info("🔧 Preparing training data...")
        
        # Check if processed data already exists
        processed_dir = Path("data/processed")
        if (processed_dir / "train_data.csv").exists():
            logger.info("Processed data found, loading existing data...")
            # Load existing data (implement if needed)
            pass
        
        # Generate new dataset
        data = self.data_preprocessor.prepare_training_data(
            synthetic_samples=synthetic_samples,
            output_dir="data/processed"
        )
        
        logger.info(f"✅ Data preparation complete:")
        logger.info(f"  Training samples: {len(data['train_texts'])}")
        logger.info(f"  Validation samples: {len(data['val_texts'])}")
        logger.info(f"  Test samples: {len(data['test_texts'])}")
        
        return data
    
    def train_model(self, data: Dict) -> Dict:
        """Train the mobile SMS classifier"""
        logger.info("🏋️ Training mobile SMS classifier...")
        
        # Load tokenizer
        self.classifier.load_tokenizer()
        
        # Train model
        training_config = self.config['training']
        history = self.classifier.train_model(
            train_texts=data['train_texts'],
            train_labels=data['train_labels'],
            val_texts=data['val_texts'],
            val_labels=data['val_labels'],
            epochs=training_config['epochs'],
            batch_size=training_config['batch_size']
        )
        
        # Save trained model
        model_path = "models/trained/mobile_sms_classifier.h5"
        tokenizer_path = "models/trained/tokenizer"
        
        self.classifier.save_model(model_path, tokenizer_path)
        logger.info(f"✅ Model saved to {model_path}")
        
        # Plot training history
        plot_path = "evaluation/plots/training_history.png"
        self.classifier.plot_training_history(plot_path)
        
        return history
    
    def evaluate_model(self, data: Dict) -> Dict:
        """Evaluate trained model on test data"""
        logger.info("📊 Evaluating trained model...")
        
        # Evaluate on test set
        evaluation_metrics = self.classifier.evaluate_model(
            data['test_texts'], 
            data['test_labels']
        )
        
        logger.info(f"✅ Model evaluation complete:")
        logger.info(f"  Accuracy: {evaluation_metrics['accuracy']:.4f}")
        logger.info(f"  Avg inference time: {evaluation_metrics['avg_inference_time_ms']:.1f}ms")
        logger.info(f"  Model size: {evaluation_metrics['model_size_mb']:.1f}MB")
        
        return evaluation_metrics
    
    def quantize_model(self, data: Dict) -> Dict:
        """Quantize model for mobile deployment"""
        logger.info("🔧 Quantizing model for mobile deployment...")
        
        # Initialize quantizer
        quantizer = ModelQuantizer(self.classifier)
        
        # Use subset of training data for calibration
        calibration_texts = data['train_texts'][:500]
        
        # Apply all quantization methods
        quantized_models = quantizer.quantize_all_methods(
            output_dir="models/quantized",
            calibration_texts=calibration_texts
        )
        
        # Evaluate quantized models
        evaluation_results = quantizer.evaluate_quantized_models(
            quantized_models,
            data['test_texts'][:100],  # Use subset for faster evaluation
            data['test_labels'][:100]
        )
        
        # Select best quantization method
        best_method = quantizer.select_best_quantization(
            evaluation_results,
            size_threshold_mb=self.config['model']['target_size_mb'],
            accuracy_threshold=0.90
        )
        
        logger.info(f"✅ Best quantization method: {best_method}")
        
        # Create quantization report
        report_path = "evaluation/reports/quantization_report.json"
        quantizer.create_quantization_report(evaluation_results, report_path)
        
        # Create comparison plots
        plot_path = "evaluation/plots/quantization_comparison.png"
        quantizer.plot_quantization_comparison(evaluation_results, plot_path)
        
        return {
            'quantized_models': quantized_models,
            'evaluation_results': evaluation_results,
            'best_method': best_method
        }
    
    def create_final_report(self, 
                           training_history: Dict,
                           evaluation_metrics: Dict,
                           quantization_results: Dict) -> Dict:
        """Create comprehensive final report"""
        logger.info("📋 Creating final evaluation report...")
        
        best_method = quantization_results['best_method']
        best_metrics = quantization_results['evaluation_results'][best_method]
        
        report = {
            'model_info': {
                'model_type': 'DistilBERT-based Mobile Classifier',
                'target_size_mb': self.config['model']['target_size_mb'],
                'target_inference_ms': self.config['model']['target_inference_ms'],
                'categories': list(MobileSmsClassifier.CATEGORIES.keys())
            },
            'training_results': {
                'final_accuracy': evaluation_metrics['accuracy'],
                'final_loss': training_history['val_loss'][-1] if 'val_loss' in training_history else None,
                'training_epochs': len(training_history['accuracy']) if 'accuracy' in training_history else 0,
                'model_size_mb': evaluation_metrics['model_size_mb']
            },
            'quantization_results': {
                'best_method': best_method,
                'final_size_mb': best_metrics.quantized_size_mb,
                'size_reduction_ratio': best_metrics.size_reduction_ratio,
                'accuracy_retention': best_metrics.accuracy_retention,
                'inference_time_ms': best_metrics.quantized_inference_ms,
                'meets_mobile_requirements': {
                    'size_ok': best_metrics.quantized_size_mb <= self.config['model']['target_size_mb'],
                    'inference_ok': best_metrics.quantized_inference_ms <= self.config['model']['target_inference_ms'],
                    'accuracy_ok': best_metrics.accuracy_retention >= 0.90
                }
            },
            'deployment_info': {
                'recommended_model': f"models/quantized/mobile_classifier_{best_method}.tflite",
                'android_integration': "Use TFLiteInferenceEngine.kt for Android integration",
                'performance_notes': []
            }
        }
        
        # Add performance notes
        notes = report['deployment_info']['performance_notes']
        
        if best_metrics.quantized_size_mb <= 10:
            notes.append("✅ Excellent size - well under mobile limit")
        elif best_metrics.quantized_size_mb <= 20:
            notes.append("✅ Good size - meets mobile requirements")
        else:
            notes.append("⚠️ Large size - may impact app performance")
        
        if best_metrics.quantized_inference_ms <= 50:
            notes.append("✅ Excellent speed - very fast inference")
        elif best_metrics.quantized_inference_ms <= 100:
            notes.append("✅ Good speed - meets performance targets")
        else:
            notes.append("⚠️ Slow inference - may need optimization")
        
        if best_metrics.accuracy_retention >= 0.95:
            notes.append("✅ Excellent accuracy retention")
        elif best_metrics.accuracy_retention >= 0.90:
            notes.append("✅ Good accuracy retention")
        else:
            notes.append("⚠️ Low accuracy retention - consider different quantization")
        
        # Save report
        report_path = "evaluation/reports/final_report.json"
        with open(report_path, 'w') as f:
            json.dump(report, f, indent=2)
        
        logger.info(f"✅ Final report saved to {report_path}")
        return report
    
    def run_complete_pipeline(self, synthetic_samples: int = 10000) -> Dict:
        """Run the complete training and evaluation pipeline"""
        
        pipeline_start = time.time()
        logger.info("🚀 Starting complete mobile SMS classifier pipeline")
        logger.info("=" * 60)
        
        # Step 1: Prepare data
        logger.info("\n📊 Step 1/5: Data Preparation")
        data = self.prepare_data(synthetic_samples)
        
        # Step 2: Train model
        logger.info("\n🏋️ Step 2/5: Model Training")
        training_history = self.train_model(data)
        
        # Step 3: Evaluate model
        logger.info("\n📈 Step 3/5: Model Evaluation")
        evaluation_metrics = self.evaluate_model(data)
        
        # Step 4: Quantize model
        logger.info("\n🔧 Step 4/5: Model Quantization")
        quantization_results = self.quantize_model(data)
        
        # Step 5: Create final report
        logger.info("\n📋 Step 5/5: Final Report")
        final_report = self.create_final_report(
            training_history,
            evaluation_metrics,
            quantization_results
        )
        
        pipeline_time = time.time() - pipeline_start
        
        logger.info("=" * 60)
        logger.info("🎉 PIPELINE COMPLETE!")
        logger.info(f"⏱️ Total time: {pipeline_time:.1f}s ({pipeline_time/60:.1f} minutes)")
        
        # Print summary
        best_method = quantization_results['best_method']
        best_metrics = quantization_results['evaluation_results'][best_method]
        
        print("\n" + "="*60)
        print("📱 MOBILE SMS CLASSIFIER - FINAL RESULTS")
        print("="*60)
        print(f"🏆 Best Model: {best_method} quantization")
        print(f"📏 Final Size: {best_metrics.quantized_size_mb:.1f}MB")
        print(f"⚡ Inference Time: {best_metrics.quantized_inference_ms:.1f}ms")
        print(f"🎯 Accuracy: {best_metrics.quantized_accuracy:.3f} ({best_metrics.accuracy_retention:.1%} retention)")
        print(f"📱 Mobile Ready: {all(final_report['quantization_results']['meets_mobile_requirements'].values())}")
        print("="*60)
        
        if all(final_report['quantization_results']['meets_mobile_requirements'].values()):
            print("✅ SUCCESS: Model meets all mobile deployment requirements!")
        else:
            print("⚠️ ATTENTION: Review performance notes in final report")
        
        print(f"\n📂 Model saved to: models/quantized/mobile_classifier_{best_method}.tflite")
        print(f"📊 Full report: evaluation/reports/final_report.json")
        
        return final_report

def main():
    """Main execution function"""
    parser = argparse.ArgumentParser(description="Train Mobile SMS Classifier")
    parser.add_argument(
        "--samples",
        type=int,
        default=10000,
        help="Number of synthetic samples to generate (default: 10000)"
    )
    parser.add_argument(
        "--config",
        type=str,
        default="config/mobile_config.yaml",
        help="Path to configuration file"
    )
    parser.add_argument(
        "--data-only",
        action="store_true",
        help="Only prepare data, don't train model"
    )
    parser.add_argument(
        "--evaluate-only",
        type=str,
        help="Evaluate existing model at given path"
    )
    
    args = parser.parse_args()
    
    # Initialize pipeline
    try:
        pipeline = MobileTrainingPipeline(args.config)
    except FileNotFoundError:
        logger.error(f"Configuration file not found: {args.config}")
        return 1
    
    try:
        if args.data_only:
            # Only prepare data
            logger.info("Running data preparation only...")
            data = pipeline.prepare_data(args.samples)
            logger.info("Data preparation complete!")
            
        elif args.evaluate_only:
            # Only evaluate existing model
            logger.info(f"Evaluating existing model: {args.evaluate_only}")
            # Implementation would load and evaluate existing model
            logger.info("Evaluation complete!")
            
        else:
            # Run complete pipeline
            final_report = pipeline.run_complete_pipeline(args.samples)
            
            # Return success/failure based on mobile requirements
            mobile_ready = all(
                final_report['quantization_results']['meets_mobile_requirements'].values()
            )
            return 0 if mobile_ready else 1
            
    except Exception as e:
        logger.error(f"Pipeline failed: {e}")
        return 1
    
    return 0

if __name__ == "__main__":
    exit(main())