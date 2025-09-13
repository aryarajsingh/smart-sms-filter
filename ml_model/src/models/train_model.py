#!/usr/bin/env python3
"""
ML Model Training and Evaluation for Smart SMS Filter

This script implements multiple ML algorithms and compares their performance
for SMS classification into Inbox, Spam, and Needs Review categories.
"""

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path
import joblib
import json
from datetime import datetime
from typing import Dict, List, Tuple, Any
import logging
import warnings

# ML Libraries
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.svm import SVC
from sklearn.naive_bayes import MultinomialNB
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import (
    train_test_split, cross_val_score, StratifiedKFold,
    GridSearchCV, RandomizedSearchCV
)
from sklearn.metrics import (
    accuracy_score, precision_score, recall_score, f1_score,
    classification_report, confusion_matrix, roc_auc_score,
    roc_curve, precision_recall_curve, average_precision_score
)
from sklearn.preprocessing import LabelEncoder
from sklearn.utils.class_weight import compute_class_weight

# Suppress warnings for cleaner output
warnings.filterwarnings('ignore')

class SmsClassifierTrainer:
    """Comprehensive SMS classifier training and evaluation"""
    
    def __init__(self, random_state: int = 42):
        """
        Initialize the trainer
        
        Args:
            random_state: Random state for reproducibility
        """
        self.random_state = random_state
        self.logger = self._setup_logger()
        
        # Initialize models
        self.models = self._initialize_models()
        self.trained_models = {}
        self.model_scores = {}
        self.best_model = None
        self.best_model_name = None
        
        # Label encoder for target classes
        self.label_encoder = LabelEncoder()
        
    def _setup_logger(self) -> logging.Logger:
        """Setup logging"""
        logger = logging.getLogger('SmsClassifierTrainer')
        logger.setLevel(logging.INFO)
        
        if not logger.handlers:
            handler = logging.StreamHandler()
            formatter = logging.Formatter(
                '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
            )
            handler.setFormatter(formatter)
            logger.addHandler(handler)
        
        return logger
    
    def _initialize_models(self) -> Dict[str, Any]:
        """Initialize ML models with default parameters"""
        return {
            'Random Forest': RandomForestClassifier(
                n_estimators=100,
                max_depth=10,
                min_samples_split=5,
                min_samples_leaf=2,
                random_state=self.random_state,
                class_weight='balanced',
                n_jobs=-1
            ),
            'SVM': SVC(
                kernel='rbf',
                C=1.0,
                gamma='scale',
                random_state=self.random_state,
                class_weight='balanced',
                probability=True
            ),
            # Note: MultinomialNB removed due to negative values in scaled features
            # 'Naive Bayes': MultinomialNB(
            #     alpha=1.0,
            #     fit_prior=True
            # ),
            'Logistic Regression': LogisticRegression(
                C=1.0,
                max_iter=1000,
                random_state=self.random_state,
                class_weight='balanced',
                solver='liblinear'
            ),
            'Gradient Boosting': GradientBoostingClassifier(
                n_estimators=100,
                learning_rate=0.1,
                max_depth=6,
                random_state=self.random_state
            )
        }
    
    def load_data(self, features_path: str, labels_path: str) -> Tuple[np.ndarray, np.ndarray]:
        """
        Load features and labels
        
        Args:
            features_path: Path to features numpy array
            labels_path: Path to labels numpy array
            
        Returns:
            Tuple of (features, encoded_labels)
        """
        self.logger.info("Loading training data...")
        
        features = np.load(features_path)
        labels = np.load(labels_path, allow_pickle=True)
        
        # Encode labels to numeric
        labels_encoded = self.label_encoder.fit_transform(labels)
        
        self.logger.info(f"Loaded {features.shape[0]} samples with {features.shape[1]} features")
        self.logger.info(f"Label distribution: {dict(zip(self.label_encoder.classes_, np.bincount(labels_encoded)))}")
        
        return features, labels_encoded
    
    def split_data(self, X: np.ndarray, y: np.ndarray, test_size: float = 0.2) -> Tuple:
        """Split data into training and testing sets"""
        self.logger.info(f"Splitting data: {1-test_size:.0%} training, {test_size:.0%} testing")
        
        return train_test_split(
            X, y,
            test_size=test_size,
            random_state=self.random_state,
            stratify=y  # Maintain class distribution
        )
    
    def train_models(self, X_train: np.ndarray, y_train: np.ndarray) -> Dict[str, Any]:
        """
        Train all models
        
        Args:
            X_train: Training features
            y_train: Training labels
            
        Returns:
            Dictionary of trained models
        """
        self.logger.info("Training models...")
        
        for name, model in self.models.items():
            self.logger.info(f"Training {name}...")
            
            try:
                model.fit(X_train, y_train)
                self.trained_models[name] = model
                self.logger.info(f"{name} training completed")
            except Exception as e:
                self.logger.error(f"Error training {name}: {e}")
        
        return self.trained_models
    
    def evaluate_models(self, X_test: np.ndarray, y_test: np.ndarray) -> Dict[str, Dict]:
        """
        Evaluate all trained models
        
        Args:
            X_test: Test features
            y_test: Test labels
            
        Returns:
            Dictionary of model evaluation scores
        """
        self.logger.info("Evaluating models...")
        
        for name, model in self.trained_models.items():
            try:
                # Make predictions
                y_pred = model.predict(X_test)
                y_pred_proba = None
                
                # Get probabilities if available
                if hasattr(model, 'predict_proba'):
                    y_pred_proba = model.predict_proba(X_test)
                
                # Calculate metrics
                scores = {
                    'accuracy': accuracy_score(y_test, y_pred),
                    'precision_macro': precision_score(y_test, y_pred, average='macro', zero_division=0),
                    'recall_macro': recall_score(y_test, y_pred, average='macro', zero_division=0),
                    'f1_macro': f1_score(y_test, y_pred, average='macro', zero_division=0),
                    'precision_weighted': precision_score(y_test, y_pred, average='weighted', zero_division=0),
                    'recall_weighted': recall_score(y_test, y_pred, average='weighted', zero_division=0),
                    'f1_weighted': f1_score(y_test, y_pred, average='weighted', zero_division=0)
                }
                
                # Add AUC if probabilities available and multiclass
                if y_pred_proba is not None and len(np.unique(y_test)) <= 3:
                    try:
                        scores['auc_macro'] = roc_auc_score(y_test, y_pred_proba, 
                                                          multi_class='ovr', average='macro')
                        scores['auc_weighted'] = roc_auc_score(y_test, y_pred_proba, 
                                                             multi_class='ovr', average='weighted')
                    except Exception:
                        scores['auc_macro'] = 0.0
                        scores['auc_weighted'] = 0.0
                
                # Store detailed results
                scores['predictions'] = y_pred
                scores['true_labels'] = y_test
                scores['probabilities'] = y_pred_proba
                
                self.model_scores[name] = scores
                
                self.logger.info(f"{name} - Accuracy: {scores['accuracy']:.4f}, F1: {scores['f1_weighted']:.4f}")
                
            except Exception as e:
                self.logger.error(f"Error evaluating {name}: {e}")
        
        return self.model_scores
    
    def cross_validate_models(self, X: np.ndarray, y: np.ndarray, cv: int = 5) -> Dict[str, Dict]:
        """
        Perform cross-validation for all models
        
        Args:
            X: Features
            y: Labels
            cv: Number of CV folds
            
        Returns:
            Cross-validation scores for each model
        """
        self.logger.info(f"Performing {cv}-fold cross-validation...")
        
        cv_scores = {}
        skf = StratifiedKFold(n_splits=cv, shuffle=True, random_state=self.random_state)
        
        for name, model in self.models.items():
            try:
                # Accuracy scores
                accuracy_scores = cross_val_score(model, X, y, cv=skf, scoring='accuracy', n_jobs=-1)
                
                # F1 scores
                f1_scores = cross_val_score(model, X, y, cv=skf, scoring='f1_weighted', n_jobs=-1)
                
                # Precision scores
                precision_scores = cross_val_score(model, X, y, cv=skf, scoring='precision_weighted', n_jobs=-1)
                
                # Recall scores
                recall_scores = cross_val_score(model, X, y, cv=skf, scoring='recall_weighted', n_jobs=-1)
                
                cv_scores[name] = {
                    'accuracy_mean': accuracy_scores.mean(),
                    'accuracy_std': accuracy_scores.std(),
                    'f1_mean': f1_scores.mean(),
                    'f1_std': f1_scores.std(),
                    'precision_mean': precision_scores.mean(),
                    'precision_std': precision_scores.std(),
                    'recall_mean': recall_scores.mean(),
                    'recall_std': recall_scores.std(),
                    'accuracy_scores': accuracy_scores.tolist(),
                    'f1_scores': f1_scores.tolist(),
                    'precision_scores': precision_scores.tolist(),
                    'recall_scores': recall_scores.tolist()
                }
                
                self.logger.info(f"{name} CV - Accuracy: {accuracy_scores.mean():.4f} ± {accuracy_scores.std():.4f}")
                
            except Exception as e:
                self.logger.error(f"Error in CV for {name}: {e}")
        
        return cv_scores
    
    def select_best_model(self) -> Tuple[str, Any]:
        """
        Select the best model based on weighted F1 score
        
        Returns:
            Tuple of (best_model_name, best_model)
        """
        if not self.model_scores:
            raise ValueError("No models have been evaluated yet")
        
        best_score = 0
        best_name = None
        
        for name, scores in self.model_scores.items():
            score = scores.get('f1_weighted', 0)
            if score > best_score:
                best_score = score
                best_name = name
        
        self.best_model_name = best_name
        self.best_model = self.trained_models[best_name]
        
        self.logger.info(f"Best model: {best_name} with F1 score: {best_score:.4f}")
        
        return best_name, self.best_model
    
    def create_visualizations(self, output_dir: str):
        """Create and save visualization plots"""
        self.logger.info("Creating visualizations...")
        
        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)
        
        # Model comparison plot
        self._plot_model_comparison(output_path)
        
        # Confusion matrices for best model
        if self.best_model_name and self.best_model_name in self.model_scores:
            self._plot_confusion_matrix(output_path)
        
        # Feature importance (if available)
        self._plot_feature_importance(output_path)
    
    def _plot_model_comparison(self, output_path: Path):
        """Plot model comparison"""
        if not self.model_scores:
            return
        
        models = list(self.model_scores.keys())
        metrics = ['accuracy', 'precision_weighted', 'recall_weighted', 'f1_weighted']
        
        fig, axes = plt.subplots(2, 2, figsize=(12, 10))
        axes = axes.ravel()
        
        for i, metric in enumerate(metrics):
            scores = [self.model_scores[model].get(metric, 0) for model in models]
            
            axes[i].bar(models, scores, color='skyblue', alpha=0.7)
            axes[i].set_title(f'{metric.replace("_", " ").title()}')
            axes[i].set_ylabel('Score')
            axes[i].set_ylim(0, 1)
            axes[i].tick_params(axis='x', rotation=45)
            
            # Add value labels on bars
            for j, score in enumerate(scores):
                axes[i].text(j, score + 0.01, f'{score:.3f}', 
                           ha='center', va='bottom')
        
        plt.tight_layout()
        plt.savefig(output_path / 'model_comparison.png', dpi=300, bbox_inches='tight')
        plt.close()
    
    def _plot_confusion_matrix(self, output_path: Path):
        """Plot confusion matrix for best model"""
        if not self.best_model_name or self.best_model_name not in self.model_scores:
            return
        
        scores = self.model_scores[self.best_model_name]
        y_true = scores['true_labels']
        y_pred = scores['predictions']
        
        # Create confusion matrix
        cm = confusion_matrix(y_true, y_pred)
        
        plt.figure(figsize=(8, 6))
        sns.heatmap(cm, annot=True, fmt='d', cmap='Blues',
                   xticklabels=self.label_encoder.classes_,
                   yticklabels=self.label_encoder.classes_)
        plt.title(f'Confusion Matrix - {self.best_model_name}')
        plt.xlabel('Predicted')
        plt.ylabel('Actual')
        plt.tight_layout()
        plt.savefig(output_path / 'confusion_matrix.png', dpi=300, bbox_inches='tight')
        plt.close()
    
    def _plot_feature_importance(self, output_path: Path):
        """Plot feature importance for tree-based models"""
        if not self.best_model or not hasattr(self.best_model, 'feature_importances_'):
            return
        
        # Load feature names
        feature_names_path = Path("models/experiments/feature_names.txt")
        if not feature_names_path.exists():
            return
        
        with open(feature_names_path, 'r') as f:
            feature_names = [line.strip() for line in f]
        
        importances = self.best_model.feature_importances_
        
        # Get top 20 most important features
        indices = np.argsort(importances)[-20:]
        
        plt.figure(figsize=(10, 8))
        plt.barh(range(len(indices)), importances[indices], alpha=0.7)
        plt.yticks(range(len(indices)), [feature_names[i] for i in indices])
        plt.xlabel('Feature Importance')
        plt.title(f'Top 20 Feature Importances - {self.best_model_name}')
        plt.tight_layout()
        plt.savefig(output_path / 'feature_importance.png', dpi=300, bbox_inches='tight')
        plt.close()
    
    def generate_report(self, output_dir: str, cv_scores: Dict = None):
        """Generate comprehensive evaluation report"""
        output_path = Path(output_dir)
        
        report = {
            'timestamp': datetime.now().isoformat(),
            'dataset_info': {
                'total_samples': len(self.model_scores[list(self.model_scores.keys())[0]]['true_labels']),
                'n_features': None,  # Will be updated if available
                'class_distribution': {str(k): int(v) for k, v in zip(
                    self.label_encoder.classes_,
                    np.bincount(self.model_scores[list(self.model_scores.keys())[0]]['true_labels'])
                )}
            },
            'model_performance': {},
            'best_model': {
                'name': self.best_model_name,
                'scores': self.model_scores.get(self.best_model_name, {}) if self.best_model_name else {}
            },
            'cross_validation': cv_scores or {}
        }
        
        # Add model performance (excluding large arrays)
        for name, scores in self.model_scores.items():
            report['model_performance'][name] = {
                k: v for k, v in scores.items() 
                if k not in ['predictions', 'true_labels', 'probabilities']
            }
        
        # Save report
        with open(output_path / 'evaluation_report.json', 'w') as f:
            json.dump(report, f, indent=2, default=str)
        
        # Generate text summary
        with open(output_path / 'evaluation_summary.txt', 'w') as f:
            f.write("SMS Classification Model Evaluation Summary\n")
            f.write("=" * 50 + "\n\n")
            
            f.write(f"Evaluation Date: {report['timestamp']}\n")
            f.write(f"Total Samples: {report['dataset_info']['total_samples']}\n")
            f.write(f"Class Distribution: {report['dataset_info']['class_distribution']}\n\n")
            
            f.write("Model Performance:\n")
            f.write("-" * 20 + "\n")
            for name, scores in self.model_scores.items():
                f.write(f"{name}:\n")
                f.write(f"  Accuracy: {scores.get('accuracy', 0):.4f}\n")
                f.write(f"  Precision: {scores.get('precision_weighted', 0):.4f}\n")
                f.write(f"  Recall: {scores.get('recall_weighted', 0):.4f}\n")
                f.write(f"  F1-Score: {scores.get('f1_weighted', 0):.4f}\n\n")
            
            if self.best_model_name:
                f.write(f"Best Model: {self.best_model_name}\n")
                best_scores = self.model_scores[self.best_model_name]
                f.write(f"Best F1-Score: {best_scores.get('f1_weighted', 0):.4f}\n")
        
        self.logger.info(f"Evaluation report saved to: {output_path}")
    
    def save_best_model(self, output_dir: str):
        """Save the best trained model and encoders"""
        if not self.best_model:
            self.logger.error("No best model selected")
            return
        
        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)
        
        # Save best model
        joblib.dump(self.best_model, output_path / 'best_model.joblib')
        
        # Save label encoder
        joblib.dump(self.label_encoder, output_path / 'label_encoder.joblib')
        
        # Save model metadata
        metadata = {
            'model_name': self.best_model_name,
            'model_type': type(self.best_model).__name__,
            'performance': self.model_scores.get(self.best_model_name, {}),
            'classes': self.label_encoder.classes_.tolist(),
            'timestamp': datetime.now().isoformat()
        }
        
        with open(output_path / 'model_metadata.json', 'w') as f:
            json.dump(metadata, f, indent=2, default=str)
        
        self.logger.info(f"Best model saved to: {output_path}")

def main():
    """Main training pipeline"""
    print("🚀 Starting SMS Classification Model Training")
    print("=" * 50)
    
    # Initialize trainer
    trainer = SmsClassifierTrainer(random_state=42)
    
    # Load data
    features, labels = trainer.load_data(
        features_path="models/experiments/features.npy",
        labels_path="models/experiments/labels.npy"
    )
    
    # Split data
    X_train, X_test, y_train, y_test = trainer.split_data(features, labels, test_size=0.2)
    
    # Train models
    trained_models = trainer.train_models(X_train, y_train)
    print(f"\n✅ Trained {len(trained_models)} models successfully")
    
    # Evaluate models
    model_scores = trainer.evaluate_models(X_test, y_test)
    print(f"📊 Evaluated {len(model_scores)} models")
    
    # Cross-validation
    cv_scores = trainer.cross_validate_models(features, labels, cv=5)
    print("📈 Cross-validation completed")
    
    # Select best model
    best_name, best_model = trainer.select_best_model()
    print(f"🏆 Best model: {best_name}")
    
    # Create output directory
    output_dir = "models/trained"
    Path(output_dir).mkdir(parents=True, exist_ok=True)
    
    # Generate visualizations
    trainer.create_visualizations(output_dir)
    print("📊 Visualizations created")
    
    # Generate report
    trainer.generate_report(output_dir, cv_scores)
    print("📋 Evaluation report generated")
    
    # Save best model
    trainer.save_best_model(output_dir)
    print("💾 Best model saved")
    
    print("\n" + "=" * 50)
    print("🎉 Training pipeline completed successfully!")
    print(f"📁 All outputs saved to: {output_dir}")
    
    return trainer

if __name__ == "__main__":
    main()