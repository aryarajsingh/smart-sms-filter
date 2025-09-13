#!/usr/bin/env python3
"""
Setup script for Smart SMS Filter ML Model
This script sets up the development environment and project structure.
"""

import os
import sys
import subprocess
from pathlib import Path

def create_project_structure():
    """Create the ML project directory structure"""
    
    # Define directory structure
    directories = [
        "data/raw",
        "data/processed", 
        "data/external",
        "models/trained",
        "models/experiments",
        "notebooks",
        "src/data",
        "src/features", 
        "src/models",
        "src/evaluation",
        "src/utils",
        "tests",
        "configs",
        "logs",
        "reports/figures"
    ]
    
    # Create directories
    for directory in directories:
        Path(directory).mkdir(parents=True, exist_ok=True)
        print(f"✓ Created directory: {directory}")
    
    # Create __init__.py files for Python packages
    init_files = [
        "src/__init__.py",
        "src/data/__init__.py",
        "src/features/__init__.py",
        "src/models/__init__.py", 
        "src/evaluation/__init__.py",
        "src/utils/__init__.py",
        "tests/__init__.py"
    ]
    
    for init_file in init_files:
        Path(init_file).touch()
        print(f"✓ Created: {init_file}")

def create_config_files():
    """Create configuration files"""
    
    # Create .gitignore for ML projects
    gitignore_content = """
# Byte-compiled / optimized / DLL files
__pycache__/
*.py[cod]
*$py.class

# ML Model files
*.pkl
*.joblib
*.h5
*.pb

# Data files
data/raw/*
data/processed/*
!data/raw/.gitkeep
!data/processed/.gitkeep

# Jupyter Notebook checkpoints
.ipynb_checkpoints

# Environment
venv/
env/
.env

# Logs
logs/*.log
*.log

# IDE
.vscode/
.idea/

# OS
.DS_Store
Thumbs.db

# Model artifacts
models/trained/*
!models/trained/.gitkeep
models/experiments/*
!models/experiments/.gitkeep

# Reports
reports/figures/*
!reports/figures/.gitkeep
"""
    
    with open(".gitignore", "w") as f:
        f.write(gitignore_content.strip())
    print("✓ Created .gitignore")
    
    # Create placeholder files
    placeholders = [
        "data/raw/.gitkeep",
        "data/processed/.gitkeep", 
        "models/trained/.gitkeep",
        "models/experiments/.gitkeep",
        "reports/figures/.gitkeep"
    ]
    
    for placeholder in placeholders:
        Path(placeholder).touch()

def setup_python_environment():
    """Setup Python virtual environment"""
    
    print("\n🐍 Setting up Python environment...")
    
    try:
        # Check if virtual environment already exists
        if Path("venv").exists():
            print("✓ Virtual environment already exists")
        else:
            # Create virtual environment
            subprocess.run([sys.executable, "-m", "venv", "venv"], check=True)
            print("✓ Created virtual environment")
        
        # Determine activation script path based on OS
        if os.name == 'nt':  # Windows
            activate_script = "venv\\Scripts\\activate"
            pip_path = "venv\\Scripts\\pip"
        else:  # Unix-like
            activate_script = "venv/bin/activate"
            pip_path = "venv/bin/pip"
        
        print(f"✓ Virtual environment created. Activate with: {activate_script}")
        print(f"✓ Install dependencies with: {pip_path} install -r requirements.txt")
        
    except subprocess.CalledProcessError as e:
        print(f"❌ Error setting up Python environment: {e}")
        return False
    
    return True

def main():
    """Main setup function"""
    
    print("🚀 Setting up Smart SMS Filter ML Model Environment\n")
    
    # Create project structure
    print("📁 Creating project structure...")
    create_project_structure()
    
    # Create configuration files
    print("\n⚙️ Creating configuration files...")
    create_config_files()
    
    # Setup Python environment
    setup_python_environment()
    
    print("\n✅ Setup complete!")
    print("\nNext steps:")
    print("1. Activate virtual environment:")
    if os.name == 'nt':
        print("   venv\\Scripts\\activate")
    else:
        print("   source venv/bin/activate")
    print("2. Install dependencies:")
    print("   pip install -r requirements.txt")
    print("3. Run the data collection script (when ready)")
    print("\n🎯 Ready to build your SMS classification model!")

if __name__ == "__main__":
    main()