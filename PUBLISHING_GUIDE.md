# ProtoKotlin Publishing Guide

This guide explains how to publish ProtoKotlin to various repositories so users can easily access your plugin.

## 🚀 Publishing Options

### 1. 📦 GitHub Packages (Already Configured!)

**Status**: ✅ Ready to use!  
**Accessibility**: Requires GitHub authentication  
**Best for**: Private/internal use or open source projects

#### How to Publish:
```bash
# Create a release tag
git tag v1.0.0
git push origin v1.0.0
```

This automatically triggers the GitHub Actions workflow that publishes to GitHub Packages.

#### How Users Install:
```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/markusfluer/protokotlin")
            credentials {
                username = "GITHUB_USERNAME"
                password = "GITHUB_TOKEN" // Personal Access Token with read:packages
            }
        }
        gradlePluginPortal()
    }
}

// build.gradle.kts  
plugins {
    id("de.markusfluer.protokotlin.plugin") version "1.0.0"
}
```

### 2. 🌐 Gradle Plugin Portal (Recommended for Public Use)

**Status**: ⚙️ Needs configuration  
**Accessibility**: Public, no authentication required  
**Best for**: Public plugins that anyone can use easily

#### Configuration Required:

1. **Get API Keys**:
   - Go to https://plugins.gradle.org/
   - Sign in with GitHub/Google
   - Go to your profile → API Keys
   - Generate key and secret

2. **Add to ~/.gradle/gradle.properties**:
   ```properties
   gradle.publish.key=YOUR_API_KEY
   gradle.publish.secret=YOUR_SECRET_KEY
   ```

3. **Update build.gradle.kts**:

Add the `com.gradle.plugin-publish` plugin and configure it:

```kotlin
plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    application
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.2.1" // Add this
}

// Add this configuration
gradlePlugin {
    website.set("https://github.com/markusfluer/protokotlin")
    vcsUrl.set("https://github.com/markusfluer/protokotlin.git")
    
    plugins {
        create("protokotlin") {
            id = "de.markusfluer.protokotlin.plugin"
            implementationClass = "com.protokotlin.gradle.ProtoKotlinPlugin"
            displayName = "ProtoKotlin Gradle Plugin"
            description = "Generate Kotlin DTOs from Protocol Buffer files compatible with kotlinx.serialization.protobuf"
            tags.set(listOf("protobuf", "kotlin", "serialization", "code-generation"))
        }
    }
}
```

#### How to Publish:
```bash
./gradlew publishPlugins
```

#### How Users Install (Much Simpler!):
```kotlin
// build.gradle.kts
plugins {
    id("de.markusfluer.protokotlin.plugin") version "1.0.0"
}
```

That's it! No repository configuration needed.

### 3. 📚 Maven Central (For Maximum Reach)

**Status**: ⚙️ Complex setup required  
**Accessibility**: Public, no authentication required  
**Best for**: Libraries that need maximum compatibility

#### Configuration Required:

This is more complex and requires:
1. Sonatype account setup
2. Domain verification or GitHub verification
3. GPG key for signing
4. More complex publishing configuration

**For now, I recommend starting with GitHub Packages or Gradle Plugin Portal.**

## 🎯 Recommended Publishing Strategy

### Phase 1: GitHub Packages (Immediate)
✅ **Already configured!** Just create a release tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

### Phase 2: Gradle Plugin Portal (Public Release)

1. **Sign up** at https://plugins.gradle.org/
2. **Get API credentials** from your profile
3. **Add plugin-publish plugin** to build.gradle.kts
4. **Publish** with `./gradlew publishPlugins`

### Phase 3: Maven Central (Optional, Later)
Consider this if you want maximum reach and compatibility.

## 📋 Step-by-Step: Publishing to Gradle Plugin Portal

### 1. Setup Account
- Go to https://plugins.gradle.org/
- Sign in with GitHub
- Navigate to your profile → API Keys
- Generate and save your key/secret

### 2. Configure Local Environment
Add to `~/.gradle/gradle.properties`:
```properties
gradle.publish.key=YOUR_KEY
gradle.publish.secret=YOUR_SECRET
```

### 3. Update Build Configuration

I can help you add the plugin-publish configuration. Would you like me to update your build.gradle.kts now?

### 4. Test and Publish
```bash
# Test the publishing setup
./gradlew publishPlugins --validate-only

# Actually publish (after testing)
./gradlew publishPlugins
```

### 5. Verify Publication
- Check https://plugins.gradle.org/ for your plugin
- Test installation in a new project

## ⚡ Quick Start: Immediate Publishing

**Want to publish RIGHT NOW?** Here's the fastest path:

### Option A: GitHub Packages (30 seconds)
```bash
git add -A
git commit -m "Ready for release"  
git tag v1.0.0
git push origin v1.0.0
```
✅ Done! Your plugin is now available on GitHub Packages.

### Option B: Gradle Plugin Portal (5 minutes)
1. Go to https://plugins.gradle.org/ and sign up
2. Get your API key from profile page
3. Let me know and I'll update your build.gradle.kts
4. Run `./gradlew publishPlugins`

## 🔧 Current Configuration Status

| Repository | Status | User Experience | Setup Complexity |
|------------|---------|-----------------|-------------------|
| **GitHub Packages** | ✅ Ready | Requires auth | ✅ Already done |
| **Gradle Plugin Portal** | ⚙️ Needs setup | Super easy | 🟡 5 minutes |
| **Maven Central** | ❌ Not configured | Easy | 🔴 Complex |

## 📊 Publishing Checklist

### Before Publishing:
- [ ] All tests pass (`./gradlew test`)
- [ ] Plugin works in test project
- [ ] Documentation is complete
- [ ] Version number is set correctly
- [ ] License file exists
- [ ] README explains usage

### After Publishing:
- [ ] Test installation from repository
- [ ] Update documentation with installation instructions  
- [ ] Create GitHub release with changelog
- [ ] Announce on relevant platforms

## 🚨 Important Notes

### Version Management
- Each publication needs a unique version
- Use semantic versioning (1.0.0, 1.0.1, 1.1.0)
- Can't republish the same version

### Plugin ID Consistency
Your plugin ID is: `de.markusfluer.protokotlin.plugin`
- This must be the same across all repositories
- Cannot be changed once published
- Should match your domain/GitHub username

### Testing Before Publishing
Always test locally:
```bash
./gradlew publishToMavenLocal
# Test with a sample project
```

## 🤝 Need Help?

If you want to proceed with any of these publishing options, I can:

1. **Update your build.gradle.kts** for Gradle Plugin Portal
2. **Help troubleshoot** any publishing issues  
3. **Create additional workflows** for automated publishing
4. **Set up Maven Central** configuration (if needed later)

Which publishing method would you like to start with?
