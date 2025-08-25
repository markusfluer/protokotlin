# ProtoKotlin Development Documentation

## Project Overview
ProtoKotlin is a CLI tool that compiles Protocol Buffer files to read-only Kotlin DTOs compatible with kotlinx.serialization.protobuf.

## Architecture

### Core Components

#### 1. CLI Layer (`com.protokotlin.Main`)
- **Framework**: Clikt for command-line parsing
- **Arguments**: Single file or `--dir` for directory processing
- **Options**: Output directory (`-o`), package name (`-p`)
- **Validation**: Ensures mutually exclusive file/directory input

#### 2. Parser Layer (`com.protokotlin.parser.ProtoParser`)
- **Input**: Raw .proto file content
- **Output**: Structured `ProtoFile` model
- **Features**:
  - Proto3 syntax support
  - Message, enum, and service parsing
  - Field type resolution
  - Nested structure handling

#### 3. Model Layer (`com.protokotlin.model`)
Data classes representing parsed proto structures:
- `ProtoFile`: Root container
- `ProtoMessage`: Message definitions with fields
- `ProtoEnum`: Enum definitions with values  
- `ProtoField`: Individual field with type and metadata
- `ProtoType`: Sealed class for type system
- `ScalarType`: Enum for proto scalar types

#### 4. Generator Layer (`com.protokotlin.generator.KotlinGenerator`)
- **Framework**: KotlinPoet for code generation
- **Input**: `ProtoFile` model
- **Output**: Map of filename to Kotlin code
- **Features**:
  - Data class generation
  - Enum class generation
  - Annotation handling (`@Serializable`, `@ProtoNumber`, `@ProtoPacked`)

## Key Design Decisions

### 1. Proto3 Compliance
**Decision**: Generate nullable fields with `null` defaults
**Rationale**: kotlinx.serialization.protobuf documentation specifies proto3 fields are implicitly optional

```kotlin
// Generated code
val name: String? = null  // Proto3 compliant
// NOT: val name: String = ""  // Wrong for proto3
```

### 2. List Handling  
**Decision**: Use `@ProtoPacked` annotation and `emptyList()` defaults
**Rationale**: Documentation specifies packed encoding and empty list requirements

```kotlin
@ProtoNumber(4)
@ProtoPacked
val hobbies: List<String> = emptyList()
```

### 3. Field Numbering
**Decision**: Always use explicit `@ProtoNumber` annotations
**Rationale**: Prevents field numbering issues during proto evolution

### 4. Directory Processing
**Decision**: Recursive directory traversal with `.proto` extension filter
**Rationale**: Supports complex project structures with nested proto files

## Development Workflow

### 1. Adding New Proto Features
1. **Model Update**: Add new types to `ProtoModels.kt`
2. **Parser Update**: Extend `ProtoParser.kt` parsing logic
3. **Generator Update**: Add generation logic to `KotlinGenerator.kt`
4. **Test**: Create test cases and example proto files

### 2. Code Generation Changes
1. **Research**: Check kotlinx.serialization documentation for requirements
2. **Implement**: Update `KotlinGenerator.kt`
3. **Test**: Run existing tests and create new ones
4. **Verify**: Generate examples and check output

### 3. CLI Changes
1. **Update**: Modify `Main.kt` argument parsing
2. **Validate**: Add proper error handling
3. **Document**: Update README usage section
4. **Test**: Manual CLI testing with various inputs

## Testing Strategy

### 1. Unit Tests (`ExampleTest.kt`)
- Tests core parsing and generation functionality
- Uses simple proto content for verification
- Checks for required annotations and structures

### 2. Integration Tests (CLI)
- Manual testing with example proto files
- Directory processing validation
- Error condition testing

### 3. Real-world Examples
- Complex proto files in `test_protos/`
- Multiple message types, enums, and nested structures
- Batch processing verification

## Build System

### Gradle Configuration
- **Kotlin Version**: 1.9.22
- **JVM Target**: Java 21
- **Dependencies**:
  - `kotlinx-serialization-protobuf`: 1.6.2
  - `clikt`: 4.2.2 (CLI framework)
  - `kotlinpoet`: 1.15.3 (code generation)

### Key Build Features
- Application plugin for easy CLI execution
- Test configuration with JUnit Platform
- Fat JAR generation for distribution

## Performance Considerations

### 1. Memory Usage
- Streaming proto file processing (reads entire file to memory)
- Could be optimized for very large files with streaming parser

### 2. Generation Speed
- KotlinPoet generates efficient code
- Directory processing is parallelizable (future enhancement)

### 3. Output Size  
- Generated code is minimal and clean
- Only necessary imports and annotations

## Error Handling

### 1. Parse Errors
- Graceful handling of malformed proto files
- Clear error messages with context

### 2. File System Errors
- Directory creation with `mkdirs()`
- File permission validation

### 3. CLI Validation
- Mutually exclusive argument checking
- Required parameter validation

## Code Style

### 1. Kotlin Conventions
- Data classes for immutable models
- Sealed classes for type hierarchies
- Extension functions where appropriate

### 2. Generated Code Style
- KotlinPoet default formatting
- Consistent annotation placement
- Proper import management

## Debugging Tips

### 1. Parser Issues
- Add debug prints in parsing loops
- Validate regex patterns with test strings
- Check line-by-line processing

### 2. Generation Issues
- Inspect intermediate `ProtoFile` model
- Use KotlinPoet's `toString()` for code preview
- Verify annotation imports

### 3. CLI Issues
- Test with `--help` flag first
- Validate file paths and permissions
- Check argument parsing with simple cases

## Future Architecture Considerations

### 1. Plugin System
- Extensible code generation for different targets
- Custom annotation support

### 2. Dependency Resolution
- Proto file import handling
- Cross-file type references

### 3. Performance Optimizations
- Incremental generation
- Parallel processing
- Memory-efficient parsing