# ProtoKotlin

A CLI tool and Gradle plugin that compiles Protocol Buffer files to read-only Kotlin DTOs compatible with kotlinx.serialization.protobuf.

## Features

- Parse .proto files and generate Kotlin data classes
- Support for scalar types, messages, enums, repeated fields, and oneofs
- Cross-file type references and imports
- Configurable package structure (flat or nested)
- Process single files or entire directories of .proto files
- Well-known types support (Timestamp, Duration, etc.)
- Compatible with kotlinx.serialization.protobuf
- Command-line interface and Gradle plugin for easy integration

## Usage

```bash
# Build the application
./gradlew build

# Process a single proto file
./gradlew run --args="path/to/your/file.proto -o output/directory -p com.yourpackage"

# Process all .proto files in a directory
./gradlew run --args="--dir path/to/proto/directory -o output/directory -p com.yourpackage"
```

### Command line options:

- `input`: Path to the .proto file (optional if --dir is used)
- `--dir, -d`: Directory containing .proto files to process (alternative to single file input)
- `-o, --output`: Output directory for generated Kotlin files (default: current directory)  
- `-p, --package`: Package name for generated Kotlin files (default: "generated")

**Note:** You must specify either a single input file OR the --dir option, but not both.

## Example

Given a proto file `person.proto`:

```protobuf
syntax = "proto3";

package example;

message Person {
    string name = 1;
    int32 age = 2;
    bool active = 3;
}
```

**Single file processing:**
```bash
./gradlew run --args="person.proto -o src/main/kotlin -p com.example"
```

**Directory processing:**
```bash
./gradlew run --args="--dir proto_files/ -o src/main/kotlin -p com.example"
```

Will generate `ProtoMessages.kt` containing all messages, plus separate files for enums:

```kotlin
// ProtoMessages.kt
package com.example

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class Person(
  @ProtoNumber(1)
  val name: String = "",
  @ProtoNumber(2) 
  val age: Int = 0,
  @ProtoNumber(3)
  val active: Boolean = false
)
```

**File Structure:**
- All **messages** → `ProtoMessages.kt`
- Each **enum** → Individual `.kt` files (e.g., `Status.kt`)
- Each **oneof** → Individual `.kt` files

## Building

```bash
./gradlew build
```

## Testing

```bash
./gradlew test
```

## Project Status

✅ **Production Ready** - v2.1.1 Complete

See [CHANGELOG.md](CHANGELOG.md) for detailed feature list and [DEVELOPMENT.md](DEVELOPMENT.md) for technical documentation.

### Latest Features (v2.1.1)
- 🗂️ **Organized File Structure**: All messages in `ProtoMessages.kt`, enums and oneofs in separate files
- 📦 **Flat Package Structure**: New `flatPackageStructure` option prevents nested package issues
- 🔗 **Cross-File Type References**: Types from different proto files now properly resolve
- 🏗️ **Package Consistency**: Standardized package structure across all generated files
- 🧪 **Enhanced Testing**: Comprehensive test coverage for all new features
- 🛠️ **Better Maintainability**: Centralized package resolution utilities

## Gradle Plugin

ProtoKotlin is now available as a Gradle plugin! See [PLUGIN_USAGE.md](PLUGIN_USAGE.md) for complete documentation.

### Quick Plugin Setup

```kotlin
plugins {
    kotlin("jvm")
    id("de.markusfluer.protokotlin.plugin") version "2.1.1"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.6.2")
}

protokotlin {
    protoDir.set(file("src/main/proto"))
    packageName.set("com.example.generated")
    flatPackageStructure.set(true)  // NEW: Prevents nested package issues
}
```

The plugin automatically:
- ✅ Finds all `.proto` files in your project
- ✅ Generates Kotlin DTOs with proper annotations
- ✅ Integrates with your build process
- ✅ Supports incremental builds and caching
- ✅ Handles cross-file type references
- ✅ Configurable package structure (flat or nested)

## Configuration Options

### flatPackageStructure (New in v2.1.0)

Controls how proto packages are mapped to Kotlin packages:

```kotlin
protokotlin {
    flatPackageStructure.set(true)   // Recommended for most projects
    flatPackageStructure.set(false)  // Legacy nested structure
}
```

**Flat Package Structure (recommended):**
- All generated types use the same base package
- Prevents import resolution issues
- Cleaner generated code

```kotlin
// All files use: package com.example.generated
@Serializable
data class SendRequest(
    val mood: Mood? = null  // Direct reference, no imports needed
)
```

**Nested Package Structure (legacy):**
- Creates nested packages from proto structure
- May require additional imports for cross-file references

```kotlin  
// Files use: package com.example.generated.mypackage_v1
@Serializable
data class SendRequest(
    val mood: com.example.generated.mypackage_v1.Mood? = null
)
```