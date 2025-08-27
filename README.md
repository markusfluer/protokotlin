# ProtoKotlin

A CLI tool and Gradle plugin that compiles Protocol Buffer files to read-only Kotlin DTOs compatible with kotlinx.serialization.protobuf.

## Features

- **🚀 Parse .proto files** and generate Kotlin data classes
- **📦 Support for all proto3 types**: scalar types, messages, enums, repeated fields, and oneofs
- **🔗 Cross-file type references** and imports with intelligent dependency resolution
- **⚙️ Configurable package structure** (flat or nested) for better organization
- **📁 Batch processing** - single files or entire directories of .proto files
- **⏰ Google well-known types** - Proper protobuf-compatible Timestamp, Duration support
- **🔧 kotlinx.serialization.protobuf compatible** with @OptIn annotations for experimental API
- **💻 Dual interface** - Command-line tool and Gradle plugin for seamless integration

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

### Basic Message Generation

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

Will generate:

```kotlin
// ProtoMessages.kt
package com.example

import kotlin.OptIn
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Person(
  @ProtoNumber(1)
  val name: String? = null,
  @ProtoNumber(2) 
  val age: Int? = null,
  @ProtoNumber(3)
  val active: Boolean? = null
)
```

### Google Well-Known Types Support

For proto files with timestamps:

```protobuf
syntax = "proto3";

package example;

import "google/protobuf/timestamp.proto";

message Event {
    string name = 1;
    google.protobuf.Timestamp created_at = 2;
}
```

Generates protobuf-compatible structures:

```kotlin
// ProtoMessages.kt
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Timestamp(
    @ProtoNumber(1) val seconds: Long = 0L,
    @ProtoNumber(2) val nanos: Int = 0
) {
    fun toInstant(): kotlinx.datetime.Instant = 
        kotlinx.datetime.Instant.fromEpochSeconds(seconds, nanos.toLong())
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable 
data class Event(
    @ProtoNumber(1) val name: String? = null,
    @ProtoNumber(2) val createdAt: Timestamp? = null
)
```

### Usage with Conversion
```kotlin
val event = Event(name = "Test", createdAt = Timestamp.now())

// Convert to kotlinx.datetime.Instant when needed
val instant = event.createdAt?.toInstant()

// Serialization works with protobuf binary format
val bytes = ProtoBuf.encodeToByteArray(event)
val decoded = ProtoBuf.decodeFromByteArray<Event>(bytes)
```

**File Structure:**
- All **messages** → `ProtoMessages.kt` (including well-known types when used)
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

✅ **Production Ready** - v2.1.2 Complete

See [CHANGELOG.md](CHANGELOG.md) for detailed feature list and [DEVELOPMENT.md](DEVELOPMENT.md) for technical documentation.

### Latest Features (v2.1.2)
- ⏰ **Proper Timestamp/Duration Support**: Generates protobuf-compatible structures with seconds/nanos fields
- 🔧 **@OptIn Annotations**: Automatic inclusion of `@OptIn(ExperimentalSerializationApi::class)` for kotlinx.serialization.protobuf
- 🔄 **Utility Conversions**: Built-in conversion functions to/from kotlinx.datetime and kotlin.time types
- 🛠️ **Serialization Compatibility**: Fixes payload transformation issues and binary deserialization errors
- 📖 **Comprehensive Documentation**: Complete troubleshooting guides and migration instructions
- 🗂️ **Organized File Structure**: All messages in `ProtoMessages.kt`, enums and oneofs in separate files
- 📦 **Flat Package Structure**: Prevents nested package issues for better import resolution

## Gradle Plugin

ProtoKotlin is now available as a Gradle plugin! See [PLUGIN_USAGE.md](PLUGIN_USAGE.md) for complete documentation.

### Quick Plugin Setup

```kotlin
plugins {
    kotlin("jvm")
    id("de.markusfluer.protokotlin.plugin") version "2.1.2"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")  // For timestamp conversion utilities
}

protokotlin {
    protoDir.set(file("src/main/proto"))
    packageName.set("com.example.generated")
    flatPackageStructure.set(true)  // Recommended: Prevents nested package issues
}
```

The plugin automatically:
- ✅ Finds all `.proto` files in your project
- ✅ Generates Kotlin DTOs with proper annotations
- ✅ Includes `@OptIn(ExperimentalSerializationApi::class)` for kotlinx.serialization.protobuf
- ✅ Integrates with your build process
- ✅ Supports incremental builds and caching
- ✅ Handles cross-file type references
- ✅ Generates protobuf-compatible Timestamp/Duration structures
- ✅ Configurable package structure (flat or nested)

## Google Well-Known Types

ProtoKotlin properly handles Google protobuf well-known types by generating protobuf-compatible structures:

### Supported Types
- `google.protobuf.Timestamp` → `Timestamp(seconds: Long, nanos: Int)`
- `google.protobuf.Duration` → `Duration(seconds: Long, nanos: Int)`

### Conversion Utilities
```kotlin
// Convert protobuf Timestamp to kotlinx.datetime.Instant
val instant = timestamp.toInstant()

// Create protobuf Timestamp from kotlinx.datetime.Instant  
val timestamp = Timestamp.fromInstant(Clock.System.now())

// Convert protobuf Duration to kotlin.time.Duration
val duration = protoDuration.toDuration()

// Create protobuf Duration from kotlin.time.Duration
val protoDuration = ProtoDuration.fromDuration(30.seconds)
```

### Why This Approach?
- **✅ Protobuf Binary Compatibility**: Works with actual protobuf serialization
- **✅ No Deserialization Errors**: Prevents `DateTimeFormatException` when server sends binary data
- **✅ Type Safety**: Compile-time validation of protobuf structure
- **✅ Easy Conversion**: Utility functions for kotlinx types when needed

> **Important**: Direct mapping to `kotlinx.datetime.Instant` causes serialization errors because protobuf uses binary encoding (seconds/nanos fields) while `Instant` expects ISO-8601 strings.

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