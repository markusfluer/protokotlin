# ProtoKotlin Changelog

## v2.1.4 - Version Alignment Release

### 🔄 Version Numbering Fix
- ✅ **Version Alignment**: Updated version to v2.1.4 to maintain correct sequential numbering
- ✅ **No Functional Changes**: This release contains no code changes from v2.1.3
- ✅ **Packaging Update**: Ensures proper version sequence for future releases

### 📋 Summary
This is a maintenance release to align the version numbering after the previous v2.1.3 release. 
All functionality remains identical to v2.1.3 - both critical bug fixes are included:
- @ProtoOneOf annotation fix for oneof serialization
- Single-line message parsing fix

## v2.1.3 - Critical Oneof & Single-Line Message Fixes

### 🚨 Critical Bug Fixes
- ✅ **@ProtoOneOf Annotation**: Fixed missing `@ProtoOneOf` annotation on oneof fields causing null deserialization
- ✅ **Oneof Serialization**: Oneof fields now deserialize correctly from protobuf binary data
- ✅ **Single-Line Message Parsing**: Fixed critical bug where single-line message definitions were incorrectly parsed as empty messages
- ✅ **Data Class Generation**: Messages like `message GetMeResponse { Profile profile = 1; }` now correctly generate as `data class` instead of `object`
- ✅ **kotlinx.serialization.protobuf Compliance**: Generated oneof fields follow proper kotlinx.serialization requirements

### 🛠️ Technical Fix Details
- ✅ **Added ProtoOneOf Import**: Included `kotlinx.serialization.protobuf.ProtoOneOf` in generated imports
- ✅ **Enhanced Parser Logic**: Updated `parseMessage()` to handle single-line message definitions with fields
- ✅ **Brace Content Extraction**: Parser now extracts and processes content between braces on the same line
- ✅ **Oneof Field Annotation**: Applied `@ProtoOneOf` annotation to all oneof field properties
- ✅ **Sealed Class Structure Preserved**: Maintained existing sealed class generation with correct proto numbers

### 🧪 Enhanced Testing
- ✅ **OneofAnnotationTest**: Comprehensive unit tests for oneof field generation
- ✅ **Multiple Oneof Support**: Tests for messages with multiple oneof fields
- ✅ **Annotation Verification**: Tests ensure @ProtoOneOf annotation is correctly applied

### 🎯 Impact
- **✅ Fixes Critical Serialization Bug**: Oneof fields were always deserializing as null in v2.1.2
- **✅ No Breaking Changes**: Only adds missing annotation, maintains existing API
- **✅ Immediate Fix**: Simple regeneration of proto classes resolves the issue

### 🔄 Migration for v2.1.2 Users
```bash
# Update ProtoKotlin version to v2.1.3
# Regenerate proto classes
./gradlew clean generateProtoKotlin

# Verify @ProtoOneOf annotation is present in generated oneof fields
```

### Generated Code Fix
```kotlin
// v2.1.2 (BROKEN - missing annotation):
public val payload: Payload? = null,

// v2.1.3 (FIXED - with annotation):
@ProtoOneOf
public val payload: Payload? = null,
```

## v2.1.2 - Protobuf Timestamp/Duration Support & OptIn Annotations

### ⏰ Major Timestamp/Duration Fix
- ✅ **Protobuf-Compatible Structures**: `google.protobuf.Timestamp` now generates proper `Timestamp(seconds: Long, nanos: Int)` structure
- ✅ **Protobuf-Compatible Structures**: `google.protobuf.Duration` now generates proper `Duration(seconds: Long, nanos: Int)` structure
- ✅ **Fixes Serialization Errors**: Resolves `DateTimeFormatException` when deserializing protobuf binary data
- ✅ **Conversion Utilities**: Built-in `toInstant()`, `fromInstant()`, `toDuration()`, `fromDuration()` methods

### 🔧 kotlinx.serialization.protobuf Compatibility
- ✅ **@OptIn Annotations**: Automatic inclusion of `@OptIn(ExperimentalSerializationApi::class)` on all generated classes
- ✅ **Experimental API Support**: Proper handling of kotlinx.serialization.protobuf experimental features
- ✅ **Enhanced Code Generation**: All `@Serializable` classes include required OptIn annotations

### 🛠️ Technical Improvements
- ✅ **Well-Known Types Registry**: Google protobuf types registered as proper message structures with seconds/nanos fields
- ✅ **Automatic Type Inclusion**: Referenced well-known types automatically included in generated ProtoMessages.kt
- ✅ **Binary Serialization Compatibility**: Generated structures work correctly with protobuf binary encoding/decoding

### 📖 Comprehensive Documentation
- ✅ **Migration Guide**: Complete guide for updating from direct kotlinx.datetime.Instant usage
- ✅ **Troubleshooting Documentation**: Detailed solutions for payload transformation issues
- ✅ **Serialization Testing Guide**: Examples and test patterns for protobuf compatibility

### 🎯 Benefits
- **✅ No More Deserialization Errors**: Fixes runtime failures when server sends protobuf binary data
- **✅ True Protobuf Compatibility**: Generated code works with actual protobuf wire format
- **✅ Easy Migration**: Utility functions provide seamless conversion to kotlinx types when needed
- **✅ Type Safety**: Compile-time validation of protobuf structure integrity

### 🔄 Migration Required
```kotlin
// Old (causes runtime errors):
val instant: Instant? = message.createdAt

// New (works correctly):
val instant: Instant? = message.createdAt?.toInstant()
```

## v2.1.1 - Organized File Structure & Duplicate Prevention

### 🗂️ Major File Structure Improvement
- ✅ **All Messages in ProtoMessages.kt**: No more duplicate message definitions across files
- ✅ **Enums in Separate Files**: Each enum gets its own `.kt` file (e.g., `Status.kt`, `Mood.kt`)
- ✅ **Oneofs in Separate Files**: Each oneof sealed class gets its own `.kt` file
- ✅ **Clean Organization**: Prevents code duplication and improves maintainability

### 🛠️ Technical Improvements
- ✅ **Fixed MessageScheduler**: Eliminated duplicate type generation across multiple files
- ✅ **Enhanced Generator Methods**: Made `generateDataClass` and `generateOneof` public for better reuse
- ✅ **Centralized Message Generation**: `generateAllMessagesFile` method creates single consolidated file
- ✅ **Updated Test Suite**: All tests updated to expect new file structure

### 📁 New File Structure
```
Generated Output:
├── ProtoMessages.kt    # All message data classes
├── Status.kt          # Individual enum files
├── Mood.kt           # Individual enum files
└── MyOneof.kt        # Individual oneof sealed classes
```

### 🎯 Benefits
- **No Duplicates**: Each type defined exactly once
- **Better Organization**: Logical separation of messages, enums, and oneofs
- **Easier Navigation**: Find all messages in one place, enums in dedicated files
- **Improved Maintainability**: Clean, predictable file structure

## v2.1.0 - Flat Package Structure & Cross-File References

### 🚀 Major New Features
- ✅ **Flat Package Structure**: New `flatPackageStructure` configuration option prevents nested package issues like `heythere_v1.heythere_v1`
- ✅ **Cross-File Type References**: Types from different proto files in the same package now properly resolve without imports
- ✅ **Package Structure Consistency**: All generated files use consistent package declarations

### 🛠️ Technical Improvements
- ✅ **Centralized Package Utils**: Created `PackageUtils` utility class for consistent package name resolution
- ✅ **Enhanced TypeRegistry**: Improved cross-file type resolution logic
- ✅ **Better Import Resolution**: Fixed nested package import generation in `KotlinGenerator`

### 🧪 Comprehensive Testing  
- ✅ **FlatPackageStructureTest**: Validates flat vs nested package behavior
- ✅ **CrossReferenceTest**: Verifies cross-file type references work correctly
- ✅ **PluginFlatPackageTest**: Tests Gradle plugin integration with new features
- ✅ **DebugCrossRefTest**: Detailed behavior verification and debugging

### 📝 Configuration Example
```kotlin
protokotlin {
    protoDir.set(file("src/main/proto"))
    packageName.set("com.example.generated")
    flatPackageStructure.set(true)  // NEW: Prevents nested package issues
}
```

### 🎯 Migration Benefits
- Resolves HeyThere app kotlinx.serialization migration blockers
- Clean import paths without nested package structures
- Better cross-file type resolution for complex proto projects

## v2.0.3 - Plugin Integration Fix

### 🛠️ Critical Bug Fix
- ✅ **Fixed Plugin Timestamp Conversion**: The Gradle plugin now correctly transforms `google.protobuf.Timestamp` to `kotlinx.datetime.Instant`
- ✅ **Plugin ProtoCompiler Integration**: Plugin now uses the full ProtoCompiler pipeline instead of basic KotlinGenerator
- ✅ **Complete Feature Parity**: Plugin now supports all CLI features including import resolution, oneof support, and message scheduling

### 🚀 Plugin Enhancements
- ✅ **Proto Path Support**: Added `protoPath` configuration for additional import directories (like `protoc -I`)
- ✅ **Comprehensive Integration Tests**: Added PluginIntegrationTest.kt to validate plugin behavior
- ✅ **Enhanced Documentation**: Updated PLUGIN_USAGE.md with new configuration options

### 🔧 Technical Changes
- **ProtoKotlinTask**: Now uses ProtoCompiler instead of direct KotlinGenerator
- **ProtoKotlinExtension**: Added `protoPath: ConfigurableFileCollection` property
- **ProtoKotlinPlugin**: Updated to wire protoPath configuration

### 📝 Configuration Example
```kotlin
protokotlin {
    protoDir.set(file("src/main/proto"))
    packageName.set("com.example.generated")
    protoPath.setFrom(files("proto/imports"))  // New: Import paths
}
```

## v2.0.2 - Enhanced Test Coverage

### 🧪 Testing Improvements
- ✅ **Comprehensive Unit Tests**: Added extensive unit tests for `google.protobuf.Timestamp` to `kotlinx.datetime.Instant` conversion
- ✅ **Well-Known Types Testing**: Complete test coverage for all Google Protocol Buffer well-known types
- ✅ **Integration Testing**: End-to-end tests through ProtoCompiler pipeline
- ✅ **Edge Case Coverage**: Tests for oneofs, maps, repeated fields, and mixed well-known types

### 📋 Test Files Added
- **WellKnownTypesTest.kt**: Comprehensive integration tests for timestamp/duration conversions
- **TimestampConversionTest.kt**: Focused unit tests for specific type resolution and mapping

### 🔍 Validated Features
- Auto-conversion of timestamps to kotlinx.datetime.Instant
- Auto-conversion of durations to kotlin.time.Duration  
- Proper import generation and field typing
- Proto annotation preservation
- Collection support with well-known types
- Full pipeline integration

## v2.0.1 - Empty Message Fix

### 🛠️ Bug Fixes
- ✅ **Fixed Empty Class Generation**: Empty proto messages now generate as `@Serializable object` instead of invalid `data class()` syntax
- ✅ **Improved Code Validity**: All generated Kotlin code is now syntactically correct and compiles successfully

### 📝 Code Generation Updates
- **Empty Messages**: `message Empty {}` → `@Serializable public object Empty`
- **Regular Messages**: `message User { string name = 1; }` → `@Serializable public data class User(...)`
- **Oneof Messages**: Messages with oneofs continue to generate as data classes with proper constructor parameters

## v2.0.0 - Java 21 & Enhanced Type Support

### 🔧 Breaking Changes
- **Java 21 Required**: Updated from Java 24 to Java 21 for better compatibility
- **Enhanced Type Safety**: Improved complex type generation and validation

### 🛠️ Bug Fixes
- ✅ **Fixed Map Type Parsing**: Resolved critical bug where `map<string, int32>` types were incorrectly parsed due to whitespace handling
- ✅ **Enhanced Error Handling**: Added better error messages for invalid type resolution
- ✅ **Improved Field Parsing**: Fixed parsing of complex types spanning multiple tokens

### 📦 Compatibility
- ✅ **Java 21 Support**: Full compatibility with Java 21 runtime
- ✅ **Gradle Integration**: Seamless integration with modern Gradle versions
- ✅ **Build Optimization**: Improved build performance and reliability

### 🎯 Type Coverage Verified
- ✅ **All Scalar Types**: Complete support for all Protocol Buffer scalar types
- ✅ **Map Types**: Full support for maps with any key/value combinations
- ✅ **Nested Structures**: Deep nesting of messages and enums
- ✅ **Oneof Fields**: Type-safe sealed class generation
- ✅ **Well-Known Types**: Google Protocol Buffer well-known types with proper Kotlin mappings

## v1.1.0 - Import Resolution & Oneof Support

### 🚀 Major New Features
- ✅ **Import Resolution**: Full support for multi-file proto projects with cross-file type references
- ✅ **Oneof Fields**: Complete oneof support generating type-safe sealed classes
- ✅ **Google Well-Known Types**: Support for `google.protobuf.Timestamp` → `kotlinx.datetime.Instant`, `google.protobuf.Duration` → `kotlin.time.Duration`
- ✅ **Message Scheduler**: Intelligent file generation - each message gets its own `.kt` file
- ✅ **Enhanced CLI**: Added `--proto-path`/`-I` option for import directories

### 🏗️ Architecture Improvements  
- ✅ **TypeRegistry**: Cross-file type resolution system
- ✅ **ProtoCompiler**: Multi-pass parsing with dependency resolution
- ✅ **MessageScheduler**: Organized code generation preventing duplication
- ✅ **Import Path Support**: Configurable proto search paths

### 🎯 Oneof Implementation
- ✅ **Sealed Classes**: Type-safe oneof representation using Kotlin sealed classes
- ✅ **Proper Field Numbers**: Maintains proto field numbering in sealed class options
- ✅ **Cross-Type References**: Oneof options can reference other message types
- ✅ **Clean Generation**: No duplicate message definitions

### 📦 Dependencies Added
- ✅ **kotlinx-datetime**: For proper timestamp handling
- ✅ **Enhanced Type Mapping**: Well-known proto types map to appropriate Kotlin types

### 🛠️ Bug Fixes
- ✅ **Fixed Message Field Parsing**: Resolved brace counting issues in proto parser
- ✅ **Import Resolution**: Types from imported files now properly resolved
- ✅ **File Organization**: Each message generated in separate file instead of lumped together

### 💼 Production Readiness
- ✅ **Real-World Testing**: Validated with complex multi-file proto projects
- ✅ **Type Safety**: Full compile-time type checking for oneof usage
- ✅ **Clean Code Generation**: No duplication, proper imports, organized structure

## v1.0.0 - Initial Release

### Features
- ✅ **CLI Application**: Complete command-line interface built with Clikt
- ✅ **Proto3 Parser**: Full support for parsing Protocol Buffer v3 files
- ✅ **Kotlin Code Generation**: Generates clean, idiomatic Kotlin data classes
- ✅ **kotlinx.serialization Compatible**: Generated classes work with kotlinx.serialization.protobuf
- ✅ **Single File Processing**: Process individual .proto files
- ✅ **Directory Processing**: Process entire directories of .proto files with `--dir` option
- ✅ **Proto3 Compliant**: Follows kotlinx.serialization.protobuf best practices

### Supported Proto Features
- ✅ **Messages**: Complete message parsing and generation
- ✅ **Enums**: Full enum support with proper constants
- ✅ **Scalar Types**: All proto3 scalar types (string, int32, int64, bool, double, float, bytes, etc.)
- ✅ **Repeated Fields**: Lists with proper `@ProtoPacked` annotations
- ✅ **Nested Messages**: Support for nested message definitions
- ✅ **Nested Enums**: Support for nested enum definitions
- ✅ **Field Numbers**: Proper `@ProtoNumber` annotation support
- ✅ **Map Types**: Basic map type support
- ✅ **Services**: Service parsing (for future RPC generation)

### Code Generation Features
- ✅ **Nullable Fields**: Proto3 fields are nullable with `null` defaults
- ✅ **Empty List Defaults**: Repeated fields default to `emptyList()`
- ✅ **@ProtoPacked**: Repeated fields include packed encoding annotation
- ✅ **@ProtoNumber**: All fields have explicit field numbering
- ✅ **@Serializable**: All classes are marked as serializable
- ✅ **CamelCase Conversion**: snake_case proto fields become camelCase Kotlin properties
- ✅ **Package Support**: Configurable Kotlin package names

### Command Line Interface
```bash
# Single file
./gradlew run --args="file.proto -o output -p com.example"

# Directory processing  
./gradlew run --args="--dir proto_files/ -o output -p com.example"

# Options:
# --dir, -d    : Directory of .proto files
# -o, --output : Output directory (default: .)  
# -p, --package: Package name (default: "generated")
```

### Technical Implementation
- ✅ **Kotlin/JVM**: Built with Kotlin 1.9.22
- ✅ **Gradle Build**: Modern Gradle Kotlin DSL
- ✅ **Dependencies**: Clikt (CLI), KotlinPoet (code generation), kotlinx.serialization
- ✅ **Testing**: JUnit test suite with example proto files
- ✅ **Java 21**: Compatible with modern JVM versions

### Example Output
**Input** (`person.proto`):
```protobuf
syntax = "proto3";
message Person {
    string name = 1;
    int32 age = 2;
    repeated string hobbies = 4;
}
```

**Generated** (`Person.kt`):
```kotlin
@Serializable
data class Person(
  @ProtoNumber(1)
  val name: String? = null,
  @ProtoNumber(2) 
  val age: Int? = null,
  @ProtoNumber(4)
  @ProtoPacked
  val hobbies: List<String> = emptyList(),
)
```

### Development Milestones

#### Phase 1: Core Infrastructure ✅
- [x] Project setup with Gradle
- [x] CLI argument parsing with Clikt
- [x] Basic proto file parsing
- [x] Kotlin code generation with KotlinPoet

#### Phase 2: Feature Complete ✅  
- [x] Enum support
- [x] Nested message support
- [x] Field type mapping (scalars, messages, enums)
- [x] Repeated field handling
- [x] Map type support
- [x] Service definition parsing

#### Phase 3: Usability Enhancements ✅
- [x] Directory processing (`--dir` option)
- [x] Batch file processing
- [x] Error handling and validation
- [x] Progress reporting

#### Phase 4: kotlinx.serialization Compliance ✅
- [x] Research official documentation
- [x] Nullable field generation for proto3
- [x] `@ProtoPacked` annotation for lists
- [x] Proper default value handling
- [x] Updated test suite

### Quality Assurance
- ✅ **Unit Tests**: Comprehensive test coverage
- ✅ **Integration Tests**: End-to-end CLI testing
- ✅ **Real-world Examples**: Tested with complex proto files
- ✅ **Documentation**: Complete README and usage examples

### Known Limitations
- Service methods are parsed but not yet used for RPC generation
- Map types use basic implementation (could be enhanced)
- No proto2 specific features (focuses on proto3)

## Future Roadmap (v2.0)
- [ ] **RPC Generation**: Generate Kotlin interfaces from service definitions
- [ ] **Proto2 Support**: Add proto2 specific features (required/optional keywords)
- [ ] **Advanced Map Support**: Enhanced map type generation
- [ ] **Import Resolution**: Handle proto file imports and dependencies  
- [ ] **Custom Annotations**: Support for custom protobuf annotations
- [ ] **Validation**: Add proto file validation before generation