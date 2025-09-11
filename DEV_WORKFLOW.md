# Development Workflow - Smart SMS Filter

This document outlines the development workflow for maintaining the Smart SMS Filter project with version control and AI assistant context persistence.

## Repository Information
- **GitHub**: https://github.com/aryarajsingh/smart-sms-filter.git
- **Current Version**: 1.1.0 (versionCode: 2)
- **Branch**: master

## Version Management

### Semantic Versioning
- **MAJOR**: Breaking changes or significant rewrites
- **MINOR**: New features or enhancements  
- **PATCH**: Bug fixes or small improvements

### Automated Version Bumping
Use the PowerShell script for consistent version updates:

```powershell
# Patch version (1.1.0 -> 1.1.1)
.\scripts\version-bump.ps1 -Type patch -Message "Fix message filtering bug"

# Minor version (1.1.0 -> 1.2.0) 
.\scripts\version-bump.ps1 -Type minor -Message "Add bottom tab navigation"

# Major version (1.1.0 -> 2.0.0)
.\scripts\version-bump.ps1 -Type major -Message "Integrate AI model classification"
```

This script automatically:
- Updates `versionCode` and `versionName` in `android/app/build.gradle`
- Adds entry to `CHANGELOG.md`
- Creates git commit with standardized message
- Creates version tag

### Manual Version Updates
If needed, manually update these files:
1. `android/app/build.gradle` - versionCode and versionName
2. `CHANGELOG.md` - Add new version entry
3. Commit and tag: `git tag v1.x.x`

## Git Workflow

### Standard Development
```bash
# Make changes to code
git add .
git commit -m "feat: description of changes"

# Push to GitHub
git push origin master
```

### Release Workflow
```bash
# Use version bump script (recommended)
.\scripts\version-bump.ps1 -Type minor -Message "New feature description"

# Push release
git push origin master --tags
```

## AI Assistant Context Persistence

To maintain development context across Warp sessions:

### Key Context Files
- **README.md** - Current project state, architecture, known issues
- **CHANGELOG.md** - Version history and feature tracking
- **DEV_WORKFLOW.md** - This file with workflow instructions
- **ARCHITECTURE.md** - Technical architecture details

### Session Startup Checklist
When starting a new development session:

1. **Check current state**: Read README.md "Current Development Context" section
2. **Review recent changes**: Check CHANGELOG.md latest entries
3. **Verify build status**: Ensure app builds with `./gradlew assembleDebug`
4. **Check git status**: `git status` and `git log --oneline -5`

### Context Updates
When significant changes are made:

1. **Update README.md**: Modify "Current Development Context" section
2. **Update CHANGELOG.md**: Add version entry with changes
3. **Commit documentation**: Include doc updates in feature commits

## Build and Testing

### Build Commands
```bash
# Debug build
cd android && ./gradlew assembleDebug

# Install to device
./gradlew installDebug

# Clean build
./gradlew clean assembleDebug
```

### Testing Strategy
- **Manual Testing**: Install and test on physical device
- **Feature Testing**: Test new features thoroughly before version bump
- **Regression Testing**: Verify existing features still work

## Current Development Status

### ✅ Completed (v1.1.0)
- Premium Welcome Screen with animations
- Enhanced Onboarding with user preferences
- Rule-based classification with preference integration
- iOS-inspired UI design system
- Git repository setup with GitHub connection

### 🔄 In Progress
- UI polish continuation (bottom tabs, animations)
- Testing current implementation

### 📋 Planned
- AI model integration (Gemma 2B)
- Advanced UI features (translucency, physics animations)
- Performance optimization

### 🐛 Known Issues
- Learn More button needs implementation
- AI model architecture ready but not integrated

## Team Collaboration

### For AI Assistants (Warp/Claude)
- Always check current context in README.md before starting
- Update documentation when making significant changes
- Use version bump script for releases
- Test builds after major changes

### Branch Strategy
- **master**: Main development branch (stable)
- **feature/***: Feature branches (if needed for complex features)
- **hotfix/***: Critical fixes (if needed)

## Deployment

### GitHub Integration
- All commits pushed to: https://github.com/aryarajsingh/smart-sms-filter.git
- Tags created for each version release
- Release notes maintained in CHANGELOG.md

### Future CI/CD
- Potential GitHub Actions for automated builds
- Automated testing pipeline
- Play Store deployment automation
