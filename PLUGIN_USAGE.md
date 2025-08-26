# ProtoKotlin Gradle Plugin Usage

The ProtoKotlin Gradle Plugin automatically generates Kotlin DTOs from Protocol Buffer files in your Gradle project.

## Quick Start

### 1. Apply the Plugin

Add to your `build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm")
    id("de.markusfluer.protokotlin.plugin") version "1.1.0"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.6.2")
}
```

### 2. Configure the Plugin

```kotlin
protokotlin {
    protoDir.set(file("src/main/proto"))           // Default: src/main/proto
    outputDir.set(file("build/generated/protokotlin")) // Default: build/generated/source/protokotlin
    packageName.set("com.example.generated")        // Default: "generated"
}
```

### 3. Create Proto Files

Create your `.proto` files in `src/main/proto/`:

```protobuf
// src/main/proto/user.proto
syntax = "proto3";

message User {
    string name = 1;
    int32 age = 2;
    bool active = 3;
}
```

### 4. Build Your Project

```bash
./gradlew build
```

The plugin will:
1. ✅ Automatically find all `.proto` files in your configured directory
2. ✅ Generate Kotlin DTOs with kotlinx.serialization annotations
3. ✅ Add generated sources to your Kotlin source set
4. ✅ Run before compilation automatically

## Configuration Options

### Full Configuration Example

```kotlin
protokotlin {
    // Input directory containing .proto files
    protoDir.set(file("src/main/protobuf"))
    
    // Output directory for generated Kotlin files  
    outputDir.set(file("src/generated/kotlin"))
    
    // Package name for generated classes
    packageName.set("com.mycompany.api.generated")
}
```

### Configuration Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `protoDir` | `DirectoryProperty` | `src/main/proto` | Directory containing `.proto` files |
| `outputDir` | `DirectoryProperty` | `build/generated/source/protokotlin` | Output directory for generated Kotlin files |
| `packageName` | `Property<String>` | `"generated"` | Package name for generated Kotlin classes |

## Generated Code Features

The plugin generates Kotlin data classes with:

✅ **kotlinx.serialization Compatible**
- `@Serializable` annotation on all classes
- `@ProtoNumber` annotations for field numbering
- `@ProtoPacked` annotations for repeated fields

✅ **Proto3 Compliant**
- Nullable fields with `null` defaults
- Proper list handling with `emptyList()` defaults

✅ **Clean Code**
- CamelCase property names (from snake_case proto fields)
- Proper imports and formatting
- Read-only data classes

### Example Generated Code

**Input** (`user.proto`):
```protobuf
syntax = "proto3";

message User {
    string user_name = 1;
    int32 age = 2;
    repeated string roles = 3;
}
```

**Generated** (`User.kt`):
```kotlin
package com.example.generated

import kotlin.Int
import kotlin.String
import kotlin.collections.List
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoPacked

@Serializable
data class User(
  @ProtoNumber(1)
  val userName: String? = null,
  @ProtoNumber(2)
  val age: Int? = null,
  @ProtoNumber(3)
  @ProtoPacked
  val roles: List<String> = emptyList(),
)
```

## Tasks

The plugin registers the following Gradle tasks:

### `generateProtoKotlin`
Generates Kotlin DTOs from Protocol Buffer files.

```bash
# Run generation manually
./gradlew generateProtoKotlin

# Clean and regenerate
./gradlew clean generateProtoKotlin
```

**Task Properties:**
- **Type**: `ProtoKotlinTask`
- **Group**: `protokotlin`
- **Dependencies**: Automatically runs before `compileKotlin`
- **Caching**: Supports Gradle build caching
- **Up-to-date**: Only runs when proto files change

## Integration with Build Process

The plugin automatically integrates with your Kotlin build:

1. **Source Set Integration**: Generated files are added to the main Kotlin source set
2. **Dependency Management**: `compileKotlin` depends on `generateProtoKotlin`
3. **Incremental Builds**: Only regenerates when proto files change
4. **Build Caching**: Supports Gradle build cache for faster builds

## Directory Structure

### Recommended Project Structure

```
my-project/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   ├── kotlin/           # Your Kotlin source code
│   │   └── proto/            # Your .proto files
│   │       ├── user.proto
│   │       └── order.proto
│   └── test/kotlin/          # Your test code
└── build/
    └── generated/
        └── source/
            └── protokotlin/  # Generated Kotlin DTOs
                ├── User.kt
                └── Order.kt
```

### Custom Directory Structure

```kotlin
protokotlin {
    protoDir.set(file("protobuf"))           # Custom proto directory
    outputDir.set(file("src/generated"))      # Custom output directory
}
```

## Advanced Usage

### Multiple Proto Directories

If you have proto files in multiple directories, you can configure multiple source sets:

```kotlin
// Configure additional source directories
sourceSets {
    main {
        proto {
            srcDirs("src/main/proto", "src/main/api-proto")
        }
    }
}
```

### IDE Integration

Generated files will be automatically recognized by IDEs:

- **IntelliJ IDEA**: Generated sources appear in Project view
- **VS Code**: Works with Kotlin language server
- **Auto-completion**: Full IDE support for generated classes

## Troubleshooting

### Common Issues

**1. No proto files found**
```
> No .proto files found in /path/to/proto
```
- Check that `protoDir` points to correct directory
- Ensure `.proto` files have correct extension

**2. Compilation errors**
```
> Unresolved reference: GeneratedClass
```
- Run `./gradlew generateProtoKotlin` manually
- Check that generated files are in correct package

**3. Plugin not found**
```
> Plugin [id: 'de.markusfluer.protokotlin.plugin'] was not found
```
- Check plugin version in `plugins` block
- Ensure plugin is published to repository

### Debug Information

Enable debug logging:

```bash
./gradlew generateProtoKotlin --info
```

This will show:
- Proto files found
- Generated file paths
- Processing progress

## Dependencies

The plugin automatically handles most dependencies, but ensure you have:

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.6.2")
}
```

## Version Compatibility

| ProtoKotlin Plugin | Gradle | Kotlin | kotlinx.serialization |
|-------------------|--------|--------|--------------------|
| 1.0.0 | 7.0+ | 1.9.0+ | 1.6.0+ |