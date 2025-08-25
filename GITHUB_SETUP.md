# GitHub Repository Setup Guide

This document explains how to set up the ProtoKotlin repository on GitHub for the CI/CD workflows to work properly.

## 📋 Repository Settings

### 1. Actions Permissions
Go to **Settings** → **Actions** → **General**:

✅ **Actions permissions**: Allow all actions and reusable workflows
✅ **Workflow permissions**: 
- Select "Read and write permissions"
- Check "Allow GitHub Actions to create and approve pull requests"

### 2. GitHub Packages
Go to **Settings** → **Actions** → **General**:

✅ Ensure "Read and write permissions" is selected for `GITHUB_TOKEN`

### 3. Branch Protection (Optional but Recommended)
Go to **Settings** → **Branches**:

Add rule for `main` branch:
✅ Require status checks to pass before merging
✅ Require branches to be up to date before merging
✅ Status checks: Select "test" (CI workflow)

## 🚀 Initial Repository Setup

### 1. Create Repository
```bash
# On GitHub, create a new repository named 'protokotlin'
# Then locally:

git remote add origin https://github.com/markusfluer/protokotlin.git
git branch -M main
git push -u origin main
```

### 2. First Release
```bash
# Create first release tag
git tag v1.0.0
git push origin v1.0.0
```

This will trigger the release workflow and publish the first version.

## 🔧 Troubleshooting Permissions

### Issue: "Resource not accessible by integration"

**Solution 1: Repository Settings**
1. Go to **Settings** → **Actions** → **General**
2. Set **Workflow permissions** to "Read and write permissions"
3. Enable "Allow GitHub Actions to create and approve pull requests"

**Solution 2: Fine-grained Personal Access Token**
If using a fork or having permission issues:
1. Create a fine-grained Personal Access Token
2. Grant permissions: Contents (write), Metadata (read), Packages (write)
3. Add as repository secret named `GITHUB_TOKEN`

**Solution 3: Organization Settings**
If repository is in an organization:
1. Check organization-level Actions permissions
2. Ensure Actions are enabled for the repository
3. Check package permissions in organization settings

## 📦 Using Published Packages

### From GitHub Packages

Users need to authenticate to use packages:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/markusfluer/protokotlin")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
        gradlePluginPortal()
    }
}
```

**gradle.properties:**
```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

### Creating GitHub Token for Users

Users need a Personal Access Token with `read:packages` permission:

1. Go to GitHub Settings → Developer settings → Personal access tokens
2. Generate new token (classic)
3. Select `read:packages` scope
4. Use as password in credentials above

## 🔍 Monitoring

### Workflow Status
- **Actions Tab**: Monitor all workflow runs
- **Packages Tab**: View published packages
- **Releases**: View created releases

### Workflow Badges
Add to README:
```markdown
[![CI](https://github.com/markusfluer/protokotlin/actions/workflows/ci.yml/badge.svg)](https://github.com/markusfluer/protokotlin/actions/workflows/ci.yml)
[![Release](https://github.com/markusfluer/protokotlin/actions/workflows/release.yml/badge.svg)](https://github.com/markusfluer/protokotlin/actions/workflows/release.yml)
```

## 📚 Repository Structure

```
protokotlin/
├── .github/
│   ├── workflows/           # CI/CD workflows
│   ├── ISSUE_TEMPLATE/     # Issue templates
│   └── PULL_REQUEST_TEMPLATE.md
├── src/                    # Source code
├── test-plugin-project/    # Test project (gitignored)
├── build.gradle.kts       # Build configuration
├── LICENSE               # MIT License
├── README.md            # Main documentation
├── PLUGIN_USAGE.md      # Plugin usage guide
├── GITHUB_WORKFLOW.md   # Workflow documentation
└── GITHUB_SETUP.md      # This file
```

## 🎯 Next Steps

1. ✅ Create GitHub repository
2. ✅ Push code to repository  
3. ✅ Configure repository permissions
4. ✅ Create first release tag
5. ✅ Verify workflows run successfully
6. ✅ Test package installation
7. ✅ Update documentation with actual repository URLs

## ⚠️ Security Notes

- Never commit personal access tokens
- Use repository secrets for sensitive data
- Regularly rotate access tokens
- Review workflow permissions periodically