# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Building and Running
```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run the CLI application with arguments
./gradlew run --args="<proto-file> -o <output-dir> -p <package-name>"

# Process a directory of proto files
./gradlew run --args="--dir <proto-directory> -o <output-dir> -p <package-name>"

# Build a fat JAR for distribution
./gradlew fatJar

# Clean and rebuild
./gradlew clean build
```

### Gradle Plugin Testing
```bash
# Test the Gradle plugin in test-plugin-project
cd test-plugin-project
../gradlew generateProtoKotlin
```

### Publishing
```bash
# Publish to Maven Local for testing
./gradlew publishToMavenLocal

# Publish plugin to Gradle Plugin Portal
./gradlew publishPlugins
```

## Architecture Overview

ProtoKotlin is a Protocol Buffer to Kotlin DTO generator with both CLI and Gradle plugin interfaces. The project follows a clean layered architecture:

### Core Components

1. **Parser Layer** (`com.protokotlin.parser.ProtoParser`): Parses .proto files into an internal AST model
   - Handles proto3 syntax
   - Supports messages, enums, and nested structures
   - Resolves field types and numbers

2. **Model Layer** (`com.protokotlin.model`): Domain models representing proto structures
   - `ProtoFile`: Root container for parsed content
   - `ProtoMessage`: Message definitions with fields
   - `ProtoEnum`: Enum definitions
   - `ProtoField`: Individual fields with type information
   - `ProtoType`: Sealed class hierarchy for type system

3. **Generator Layer** (`com.protokotlin.generator.KotlinGenerator`): Generates Kotlin code using KotlinPoet
   - Creates data classes with kotlinx.serialization annotations
   - Handles nullable fields for proto3 compliance
   - Adds `@ProtoNumber` and `@ProtoPacked` annotations

4. **CLI Interface** (`com.protokotlin.Main`): Command-line interface using Clikt
   - Supports single file or directory processing
   - Configurable output directory and package name

5. **Gradle Plugin** (`com.protokotlin.gradle`): Gradle integration
   - `ProtoKotlinPlugin`: Main plugin class
   - `ProtoKotlinTask`: Generation task
   - `ProtoKotlinExtension`: Configuration DSL

### Key Design Patterns

- **Proto3 Compliance**: All fields are nullable with null defaults (except repeated fields which use emptyList())
- **Annotation Strategy**: Every field has explicit `@ProtoNumber`, lists have `@ProtoPacked`  
- **Code Generation**: Uses KotlinPoet for clean, idiomatic Kotlin output
- **Import Resolution**: Multi-file proto projects supported with cross-file type references
- **Google Well-Known Types**: `google.protobuf.Timestamp` → `kotlinx.datetime.Instant`, `google.protobuf.Duration` → `kotlin.time.Duration`
- **Build Integration**: Plugin automatically hooks into Kotlin compilation

### File Organization

- `src/main/kotlin/com/protokotlin/`: Main source code
  - `Main.kt`: CLI entry point
  - `parser/`: Proto parsing logic
  - `model/`: Domain models
  - `generator/`: Code generation
  - `gradle/`: Gradle plugin implementation
- `src/test/`: Test code
- `test-plugin-project/`: Integration test for Gradle plugin
- Example proto files in root: `example.proto`, `debug_test.proto`
- Test proto files: `test_protos/` directory

## Important Conventions

- All generated Kotlin DTOs are immutable data classes
- Field names are converted from snake_case to camelCase
- Generated files follow Kotlin naming conventions (PascalCase for classes)
- Proto packages are mapped to Kotlin package hierarchy (with base package prefix)
- Cross-file imports are automatically resolved
- Google well-known types are mapped to proper Kotlin equivalents with appropriate imports