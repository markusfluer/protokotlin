# ProtoKotlin Shortcomings & Missing Features

This document outlines the current limitations of ProtoKotlin discovered when attempting to use it with real-world proto projects. These issues prevent ProtoKotlin from being production-ready for multi-file proto projects.

## Critical Issues (Blockers)

### 1. Import Resolution & Cross-File Type References
**Impact**: 🔴 Critical - Prevents use with multi-file proto projects

**Current State**: 
- Imports are parsed but completely ignored during code generation
- Types from imported files are treated as unknown message types
- No mechanism to resolve types across multiple proto files

**Example Problem**:
```protobuf
// common.proto
enum UserState {
  ACTIVE = 0;
  SUSPENDED = 1;
}

// auth.proto
import "common.proto";
message LoginResponse {
  UserState state = 1;  // UserState is not resolved
}
```

**Required Implementation**:
- Build a type registry from all proto files
- Resolve type references during parsing
- Generate appropriate Kotlin import statements
- Handle relative and absolute import paths

### 2. Oneof Field Support
**Impact**: 🔴 Critical - Common pattern in proto3

**Current State**: 
- Oneof fields are not parsed at all
- No code generation strategy for oneofs

**Example Problem**:
```protobuf
message InboxItem {
  string id = 1;
  oneof payload {
    HeyThere hey_there = 4;
    ThinkingOfYou thinking_of_you = 5;
  }
}
```

**Required Implementation**:
- Parse oneof blocks in messages
- Generate sealed classes or use kotlinx.serialization's polymorphic support
- Handle field numbering within oneofs
- Support oneof presence checking

### 3. Google Well-Known Types
**Impact**: 🔴 Critical - Used in nearly all production proto files

**Current State**: 
- No special handling for google.protobuf.* types
- Timestamp, Duration, Any, etc. are treated as unknown messages

**Example Problem**:
```protobuf
import "google/protobuf/timestamp.proto";
message Event {
  google.protobuf.Timestamp created_at = 1;
}
```

**Required Implementation**:
- Map well-known types to Kotlin/Java equivalents:
  - `google.protobuf.Timestamp` → `Instant` or custom serializer
  - `google.protobuf.Duration` → `Duration` 
  - `google.protobuf.Any` → Special handling
  - `google.protobuf.Empty` → `Unit` or empty class
- Include protobuf-java-util or implement custom serializers

## Major Issues

### 4. Package Namespace Handling
**Impact**: 🟡 Major - Affects code organization

**Current State**: 
- Proto package declarations are parsed but ignored
- All generated files use the CLI-specified package
- No hierarchical package structure

**Example Problem**:
```protobuf
package heythere.v1;  // Ignored, should map to com.heythere.v1
```

**Required Implementation**:
- Use proto package as base for Kotlin package
- Allow package prefix configuration
- Handle package name transformations

### 5. Explicit Optional Fields (Proto3)
**Impact**: 🟡 Major - Proto3 compliance

**Current State**: 
- All fields treated as implicitly optional
- No distinction between explicit and implicit optional

**Example Problem**:
```protobuf
message User {
  optional string email = 1;    // Explicit optional - has presence
  string name = 2;               // Implicit optional - no presence
}
```

**Required Implementation**:
- Parse explicit optional keyword
- Generate different code for explicit vs implicit optional
- Consider using wrapper types for presence tracking

### 6. Service Code Generation
**Impact**: 🟡 Major - Required for gRPC

**Current State**: 
- Services are parsed but not generated
- No stub/skeleton generation for RPC

**Required Implementation**:
- Generate service interfaces
- Consider gRPC-Kotlin integration
- Support streaming RPCs

## Minor Issues

### 7. Map Field Support
**Impact**: 🟢 Minor - Convenience feature

**Current State**: 
- Basic map parsing exists but untested
- Generation might not handle all cases

**Example Problem**:
```protobuf
message Config {
  map<string, string> settings = 1;
}
```

**Required Implementation**:
- Verify map field generation
- Ensure proper kotlinx.serialization annotations
- Test with various key/value types

### 8. Field Default Values
**Impact**: 🟢 Minor - Proto2 compatibility

**Current State**: 
- No support for custom default values
- All fields use type defaults

**Example Problem**:
```protobuf
message Settings {
  int32 timeout = 1 [default = 30];
}
```

### 9. Field Options & Annotations
**Impact**: 🟢 Minor - Advanced features

**Current State**: 
- Field options are not parsed
- No support for deprecated, custom options, etc.

**Example Problem**:
```protobuf
message User {
  string old_field = 1 [deprecated = true];
}
```

### 10. Nested Type Scoping
**Impact**: 🟢 Minor - Code organization

**Current State**: 
- Nested messages/enums generated as top-level classes
- No proper scoping within parent class

## Implementation Priority

### Phase 1: Critical Blockers (Must Have)
1. **Import Resolution & Cross-File Types** - Without this, multi-file projects impossible
2. **Oneof Support** - Very common in real proto files
3. **Google Well-Known Types** - Standard in production

### Phase 2: Major Features (Should Have)  
4. **Package Namespace Handling** - Important for code organization
5. **Explicit Optional Fields** - Proto3 compliance
6. **Service Generation** - Needed for full gRPC support

### Phase 3: Nice to Have
7. **Map Field Verification** - May already work
8. **Default Values** - Proto2 compatibility
9. **Field Options** - Advanced features
10. **Nested Type Scoping** - Code organization

## Testing Requirements

Each feature implementation should include:
- Unit tests with example proto files
- Integration tests with real-world proto structures
- Compatibility tests with protoc-generated code
- Serialization/deserialization round-trip tests

## Breaking Changes

Implementing these features may require:
- Changes to the `ProtoParser` architecture
- New model classes for oneof, options, etc.
- Redesign of the type resolution system
- Changes to CLI arguments for multi-file processing

## Recommended Approach

1. **Start with Import Resolution**: Build a multi-pass parser that first collects all types, then resolves references
2. **Add Oneof Support**: Extend parser and generator for oneof blocks
3. **Integrate Well-Known Types**: Create a mapping registry for Google types
4. **Test with Real Projects**: Use heythere-proto as a test case throughout

## Success Criteria

ProtoKotlin should be able to:
- ✅ Process all proto files in heythere-proto project
- ✅ Generate compilable Kotlin code
- ✅ Produce DTOs compatible with kotlinx.serialization.protobuf
- ✅ Successfully serialize/deserialize messages matching protoc output