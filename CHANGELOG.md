# ProtoKotlin Changelog

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