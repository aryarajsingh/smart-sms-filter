# Documentation Structure Guide

## Overview

This guide defines the documentation structure for the Smart SMS Filter project to prevent discrepancies and maintain consistency.

## 📋 Documentation Hierarchy

### Primary Source (Edit These)
All documentation should be edited in the `android/` directory:

```
android/
├── README.md           # Main project documentation (PRIMARY SOURCE)
├── CHANGELOG.md        # Version history (PRIMARY SOURCE)
├── CONTRIBUTING.md     # Contribution guidelines
├── LICENSE            # License information
└── docs/
    ├── API.md         # API documentation
    ├── ARCHITECTURE.md # Technical architecture
    └── SETUP.md       # Development setup guide
```

### Auto-Generated (Never Edit Directly)
Root directory files are automatically synchronized:

```
smart-sms-filter/ (root)
├── README.md          # AUTO-SYNCED from android/README.md
├── CHANGELOG.md       # AUTO-SYNCED from android/CHANGELOG.md
└── [other files]      # Project-wide documentation
```

## 🔄 Synchronization Rules

### Automatic Sync Triggers
Documentation is automatically synchronized when:

1. **Pre-commit Hook**: When committing changes to android docs
2. **GitHub Actions**: When pushing to master/main branch
3. **Manual Sync**: Running `./scripts/sync-docs.ps1`

### What Gets Synced

| Source File | Destination | Transformations |
|------------|-------------|-----------------|
| `android/README.md` | `README.md` | • Add sync header<br>• Fix relative paths<br>• Add project structure |
| `android/CHANGELOG.md` | `CHANGELOG.md` | None (direct copy) |

## 🛠️ Setup Instructions

### 1. Install Git Hooks (One-time Setup)
```powershell
# Run from project root
./scripts/install-hooks.ps1
```

This installs a pre-commit hook that automatically syncs documentation.

### 2. Manual Sync (When Needed)
```powershell
# Run from project root
./scripts/sync-docs.ps1
```

### 3. Force Sync (Override Checks)
```powershell
# Force update even if files appear identical
./scripts/sync-docs.ps1 -Force
```

## 📝 Documentation Guidelines

### When Adding New Documentation

1. **Always create in `android/` directory first**
2. **Update this guide** if adding new sync rules
3. **Test sync** before committing

### File Naming Conventions

- `README.md` - Main project documentation
- `CHANGELOG.md` - Version history (follow Keep a Changelog format)
- `CONTRIBUTING.md` - How to contribute
- `*.md` - All documentation in Markdown format
- UPPERCASE for root-level docs
- lowercase for subdirectory docs

### Content Guidelines

#### README.md Structure
1. Project title and badges
2. Overview
3. What's New (latest version)
4. Quick Start
5. Features
6. Technical Stack
7. Project Structure
8. Installation
9. Usage
10. API Documentation (link)
11. Contributing (link)
12. License
13. Credits

#### CHANGELOG.md Structure
- Follow [Keep a Changelog](https://keepachangelog.com/) format
- Sections: Added, Changed, Deprecated, Removed, Fixed, Security
- Latest version at top
- Date format: YYYY-MM-DD

## 🚨 Common Issues & Solutions

### Issue: Root README not updating
**Solution**: 
```powershell
# Force sync
./scripts/sync-docs.ps1 -Force

# Check git status
git status

# Commit if needed
git add README.md CHANGELOG.md
git commit -m "docs: Sync documentation"
```

### Issue: Merge conflicts in root docs
**Solution**: 
```powershell
# Always resolve by taking android version
git checkout --theirs android/README.md
./scripts/sync-docs.ps1
git add README.md
git commit
```

### Issue: Pre-commit hook not working
**Solution**:
```powershell
# Reinstall hooks
./scripts/install-hooks.ps1

# Or manually sync before commit
./scripts/sync-docs.ps1
```

## 🔍 Verification Checklist

Before pushing changes:

- [ ] All edits made in `android/` directory
- [ ] Sync script ran successfully
- [ ] Root files show sync header
- [ ] No manual edits to root README/CHANGELOG
- [ ] Git hooks installed and working
- [ ] CI/CD workflow enabled on GitHub

## 📊 Documentation Health Metrics

Track these to ensure documentation quality:

1. **Sync Frequency**: Should match commit frequency
2. **Discrepancy Count**: Should be zero
3. **Build Status**: All CI checks passing
4. **Coverage**: All features documented
5. **Freshness**: Updated with each release

## 🤝 Contributing to Documentation

1. Fork the repository
2. Create a feature branch
3. Edit files in `android/` directory only
4. Run sync script
5. Commit with clear message
6. Submit pull request

## 📚 Additional Resources

- [Markdown Guide](https://www.markdownguide.org/)
- [Keep a Changelog](https://keepachangelog.com/)
- [Semantic Versioning](https://semver.org/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)

---

**Remember**: The `android/` directory is the single source of truth for all documentation. Never edit root documentation files directly!
