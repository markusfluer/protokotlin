# ProtoKotlin Changelog

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