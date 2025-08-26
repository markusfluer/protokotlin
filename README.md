# ProtoKotlin

A CLI tool that compiles Protocol Buffer files to read-only Kotlin DTOs compatible with kotlinx.serialization.protobuf.

## Features

- Parse .proto files and generate Kotlin data classes
- Support for scalar types, messages, enums, and repeated fields
- Process single files or entire directories of .proto files
- Compatible with kotlinx.serialization.protobuf
- Command-line interface for easy integration

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

Will generate `Person.kt`:

```kotlin
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

## Building

```bash
./gradlew build
```

## Testing

```bash
./gradlew test
```

## Project Status

✅ **Production Ready** - v1.0.0 Complete

See [CHANGELOG.md](CHANGELOG.md) for detailed feature list and [DEVELOPMENT.md](DEVELOPMENT.md) for technical documentation.

### Latest Features (v1.0.0)
- 🔄 **Proto3 Compliance**: Updated field generation to follow kotlinx.serialization.protobuf best practices
- 📁 **Directory Processing**: Added `--dir` option for batch processing multiple .proto files  
- 🏷️ **Enhanced Annotations**: Added `@ProtoPacked` for lists and proper nullable field handling
- 🔌 **Gradle Plugin**: Complete Gradle plugin for seamless project integration
- ✅ **Comprehensive Testing**: Full test suite with real-world examples

## Gradle Plugin

ProtoKotlin is now available as a Gradle plugin! See [PLUGIN_USAGE.md](PLUGIN_USAGE.md) for complete documentation.

### Quick Plugin Setup

```kotlin
plugins {
    kotlin("jvm")
    id("de.markusfluer.protokotlin.plugin") version "1.1.0"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.6.2")
}

protokotlin {
    protoDir.set(file("src/main/proto"))
    packageName.set("com.example.generated")
}
```

The plugin automatically:
- ✅ Finds all `.proto` files in your project
- ✅ Generates Kotlin DTOs with proper annotations
- ✅ Integrates with your build process
- ✅ Supports incremental builds and caching