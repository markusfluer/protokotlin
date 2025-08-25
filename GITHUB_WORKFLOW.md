# GitHub CI/CD Workflow Documentation

This document explains the GitHub Actions workflows set up for ProtoKotlin.

## 🔄 Workflows Overview

### 1. **CI Workflow** (`.github/workflows/ci.yml`)
**Trigger:** Push to `main`/`develop` branches, Pull Requests to `main`

**Actions:**
- ✅ Run tests with JDK 21
- ✅ Build the plugin
- ✅ Verify plugin functionality with test project
- ✅ Upload build artifacts
- ✅ Generate test reports

**Status:** Runs on every push and PR to ensure code quality.

### 2. **Release Workflow** (`.github/workflows/release.yml`) 
**Trigger:** Git tags matching `v*` pattern (e.g., `v1.0.0`)

**Actions:**
- ✅ Extract version from git tag
- ✅ Update `build.gradle.kts` with tag version
- ✅ Run tests
- ✅ Build and publish to GitHub Packages
- ✅ Create fat JAR for CLI usage
- ✅ Create GitHub Release with artifacts

**Usage:** Create and push a git tag to trigger a release.

### 3. **Manual Publish Workflow** (`.github/workflows/publish.yml`)
**Trigger:** Manual dispatch or release published

**Actions:**
- ✅ Publish plugin to GitHub Packages
- ✅ Support manual version input
- ✅ Generate package summary

**Usage:** For manual publishing without creating a release.

## 📦 GitHub Packages Publishing

The workflows automatically publish to GitHub Packages:

**Package URL:** `https://maven.pkg.github.com/markusfluer/protokotlin`

**Plugin Coordinates:**
- **Group:** `de.markusfluer.protokotlin`
- **Artifact:** `protokotlin`
- **Plugin ID:** `de.markusfluer.protokotlin.plugin`

## 🚀 Creating a Release

### Method 1: Git Tags (Recommended)

```bash
# Create and push a new tag
git tag v1.0.1
git push origin v1.0.1
```

This will:
1. Trigger the release workflow
2. Build and test the code
3. Publish to GitHub Packages
4. Create a GitHub Release with JAR file
5. Generate release notes

### Method 2: GitHub UI

1. Go to GitHub repository
2. Click "Releases" → "Create a new release"
3. Create a new tag (e.g., `v1.0.1`)
4. Fill in release notes
5. Click "Publish release"

### Method 3: Manual Workflow

1. Go to "Actions" tab on GitHub
2. Select "Publish" workflow
3. Click "Run workflow"
4. Enter version number
5. Click "Run workflow"

## 🔧 Required Secrets

The workflows use built-in GitHub secrets:

- `GITHUB_TOKEN` - Automatically provided by GitHub
- `GITHUB_ACTOR` - Automatically provided by GitHub

**No additional secrets needed!** GitHub automatically provides the necessary permissions.

## 📋 Workflow Permissions

Each workflow has specific permissions:

### CI Workflow
```yaml
permissions:
  contents: read      # Read repository content
```

### Release/Publish Workflows
```yaml
permissions:
  contents: write     # Create releases
  packages: write     # Publish to GitHub Packages
```

## 🧪 Testing in Workflows

The CI includes comprehensive testing:

1. **Unit Tests**: `./gradlew test`
2. **Integration Tests**: Plugin functionality verification
3. **Build Verification**: Ensure artifacts can be created
4. **Test Reports**: JUnit test result publishing

## 📁 Artifacts

### CI Artifacts (7-day retention)
- Build JARs (`build/libs/`)
- Test reports (`build/reports/`)

### Release Artifacts (Permanent)
- Fat JAR for CLI usage (`protokotlin-VERSION-all.jar`)
- Plugin JAR (published to GitHub Packages)

## 🔍 Monitoring

### Build Status
Monitor workflow status in the "Actions" tab of the GitHub repository.

### Package Status
Check published packages at:
`https://github.com/markusfluer/protokotlin/packages`

## 🐛 Troubleshooting

### Common Issues

**1. Permission Denied**
- Ensure repository has Actions enabled
- Check workflow permissions are correctly set

**2. Version Conflicts**
- Each release must have a unique version
- Delete and recreate tags if needed

**3. Test Failures**
- Check test reports in workflow artifacts
- Fix tests before creating releases

**4. Publishing Failures**
- Verify GitHub Packages permissions
- Check for version conflicts in packages

### Debug Steps

1. Check workflow logs in Actions tab
2. Download and examine build artifacts
3. Run workflows locally using `act` tool
4. Test plugin functionality manually

## 📚 Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [GitHub Packages Documentation](https://docs.github.com/en/packages)
- [Gradle Publishing Plugin](https://docs.gradle.org/current/userguide/publishing_gradle_plugins.html)