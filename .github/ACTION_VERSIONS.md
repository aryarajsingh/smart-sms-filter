# GitHub Actions Version Reference

## Current Versions (Updated: January 13, 2025)

All GitHub Actions in this repository use the latest stable versions to avoid deprecation issues.

### Actions Used

| Action | Version | Purpose |
|--------|---------|---------|
| `actions/checkout` | v4 | Check out repository code |
| `actions/setup-java` | v4 | Set up JDK for Android builds |
| `actions/cache` | v4 | Cache Gradle dependencies |
| `actions/upload-artifact` | v4 | Upload build artifacts (APKs, test results) |

### Important Notes

⚠️ **GitHub deprecated v3 actions on December 5, 2024**
- Any workflow using v3 will automatically fail
- Always use v4 or later for all actions

### Migration Guide

If you need to update actions in the future:

```yaml
# Old (deprecated)
uses: actions/upload-artifact@v3

# New (current)
uses: actions/upload-artifact@v4
```

### Version Policy

- Always use the latest major version of GitHub-provided actions
- Pin to major version (e.g., `@v4`) not specific tags for automatic security updates
- Check for deprecation notices in GitHub's changelog

### Checking for Updates

To check if actions need updating:
1. Visit the action's repository (e.g., https://github.com/actions/upload-artifact)
2. Check the latest release version
3. Update the workflow files if a new major version is available

### Resources

- [GitHub Actions Changelog](https://github.blog/changelog/label/actions/)
- [actions/upload-artifact](https://github.com/actions/upload-artifact)
- [actions/checkout](https://github.com/actions/checkout)
- [actions/setup-java](https://github.com/actions/setup-java)
- [actions/cache](https://github.com/actions/cache)