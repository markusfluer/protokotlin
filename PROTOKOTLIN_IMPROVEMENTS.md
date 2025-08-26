# ProtoKotlin Improvements Required

This document outlines the specific changes needed in ProtoKotlin to fully support the HeyThere app's protobuf to kotlinx.serialization migration.

## Current Status

ProtoKotlin 2.0.3 has successfully generated 70 kotlinx.serialization DTOs from 8 proto files, including:
- ✅ All basic request/response types
- ✅ Complex nested types (InboxItem, BuddyInfo, ContactInfo)
- ✅ Sealed classes for `oneof` fields
- ✅ Timestamp conversion to kotlinx.datetime.Instant
- ✅ Empty message handling with `object` instead of `data class()`

## Issues to Fix

### 1. Import Path Resolution

**Problem:** Generated files have broken import statements that reference non-existent nested packages.

**Examples of Current Issues:**
```kotlin
// In LoginResponse.kt
import de.markusfluer.heythere.`data`.proto.heythere_v1.heythere_v1.UserState

// In HeyThere.kt  
public val mood: Mood? = null  // Unresolved reference 'Mood'

// In OAuth2LoginRequest.kt
import de.markusfluer.heythere.`data`.proto.heythere_v1.heythere_v1.AuthProvider
```

**Root Cause:** ProtoKotlin is generating nested package paths like `heythere_v1.heythere_v1` instead of resolving to the actual generated file locations.

**Required Fix:**
```kotlin
// Should generate:
import de.markusfluer.heythere.`data`.proto.UserState
import de.markusfluer.heythere.`data`.proto.Mood  
import de.markusfluer.heythere.`data`.proto.AuthProvider

// Instead of:
import de.markusfluer.heythere.`data`.proto.heythere_v1.heythere_v1.UserState
```

**Implementation Steps:**
1. **Fix import resolution logic** - When generating imports for referenced types, resolve to the actual generated file package, not the proto package structure
2. **Create a type mapping table** during generation that maps proto types to their generated Kotlin file locations
3. **Use relative imports** when types are in the same output package

### 2. Cross-File Type References

**Problem:** Types defined in different proto files (like `common.proto`) are not being properly referenced by files that import them.

**Example:**
```protobuf
// common.proto
message InboxItem {
  string id = 1;
  string from_friend_code = 2; 
  google.protobuf.Timestamp at = 3;
  oneof payload {
    HeyThere hey_there = 4;
    ThinkingOfYou thinking_of_you = 5;
  }
  bool unread = 6;
}

enum Mood {
  MOOD_UNSPECIFIED = 0;
  HAPPY = 1;
  LOVE = 2;
  // ...
}
```

```protobuf  
// messaging.proto
import "heythere/v1/common.proto";

message SendHeyThereRequest {
  string friend_code = 1;
  string word = 2;
  Mood mood = 3;  // References Mood from common.proto
}
```

**Current Generated Output (Incorrect):**
```kotlin
// In SendHeyThereRequest.kt
@Serializable
public data class SendHeyThereRequest(
  @ProtoNumber(3)
  public val mood: Mood? = null,  // Unresolved reference 'Mood'
)
```

**Required Generated Output (Correct):**
```kotlin
// In SendHeyThereRequest.kt
import de.markusfluer.heythere.`data`.proto.Mood

@Serializable
public data class SendHeyThereRequest(
  @ProtoNumber(3)
  public val mood: Mood? = null,
)
```

**Implementation Steps:**
1. **Parse proto imports** and build a dependency graph of which files depend on which types
2. **Resolve imported types** to their generated Kotlin equivalents during code generation
3. **Generate proper import statements** for cross-file references

### 3. Package Structure Consistency

**Problem:** Generated files are inconsistent in their package declarations.

**Current Mixed Output:**
```kotlin
// Some files:
package de.markusfluer.heythere.`data`.proto.heythere_v1

// Other files:
package de.markusfluer.heythere.`data`.proto
```

**Required Consistent Output:**
```kotlin
// All files should use:
package de.markusfluer.heythere.`data`.proto
```

**Implementation Steps:**
1. **Standardize package generation** - Use the configured `packageName` consistently across all generated files
2. **Remove proto path influence** - Don't let proto file structure affect the final Kotlin package structure
3. **Update configuration** - Ensure the plugin configuration properly sets the output package for all files

## Configuration Changes Needed

### Current Working Configuration:
```kotlin
protokotlin {
    protoDir.set(file("../proto/heythere/v1"))
    packageName.set("de.markusfluer.heythere.data.proto")
    outputDir.set(file("src/commonMain/kotlin/de/markusfluer/heythere/data/proto"))
}
```

### Proposed Enhanced Configuration:
```kotlin
protokotlin {
    protoDir.set(file("../proto/heythere/v1"))
    packageName.set("de.markusfluer.heythere.data.proto")
    outputDir.set(file("src/commonMain/kotlin/de/markusfluer/heythere/data/proto"))
    
    // New options to consider:
    flatPackageStructure.set(true)  // Don't create nested packages from proto structure
    resolveImports.set(true)        // Automatically resolve cross-file type references
    kotlinxDatetime.set(true)       // Convert google.protobuf.Timestamp to kotlinx.datetime.Instant
}
```

## Testing the Fixes

### Test Cases to Validate:

1. **Import Resolution Test:**
```kotlin
// Should compile successfully:
val loginResponse = LoginResponse(
    accessToken = "token",
    state = UserState.ACTIVE  // UserState should be properly imported
)
```

2. **Cross-File Reference Test:**
```kotlin
// Should compile successfully:
val request = SendHeyThereRequest(
    friendCode = "ABC123",
    word = "Hello",
    mood = Mood.HAPPY  // Mood should be properly imported from common.proto
)
```

3. **Complex Type Test:**
```kotlin
// Should compile successfully:
val inboxItem = InboxItem(
    id = "msg-123",
    fromFriendCode = "DEF456", 
    at = Clock.System.now(),  // kotlinx.datetime.Instant
    payload = Payload.HeyThere(  // Sealed class should work
        value = HeyThere(word = "Hi", mood = Mood.HAPPY)
    ),
    unread = true
)
```

## Implementation Priority

### High Priority (Blocking):
1. **Fix import path resolution** - Critical for compilation
2. **Resolve cross-file type references** - Critical for using types from common.proto

### Medium Priority (Quality of Life):
1. **Standardize package structure** - Improves maintainability
2. **Enhanced configuration options** - Better developer experience

### Low Priority (Future Enhancements):
1. **Validation of generated code** - Compile-time checks
2. **Better error messages** - When proto parsing fails
3. **Documentation generation** - KDoc comments from proto comments

## Expected Outcome

After implementing these fixes, the kotlinx.serialization migration will be fully functional with:
- ✅ All 70+ generated DTOs compiling successfully
- ✅ Proper type safety and IDE support
- ✅ Clean, maintainable generated code
- ✅ Full compatibility with existing HeyThere backend API

The generated services (`AuthServiceNew`, `MessagingServiceNew`, `BuddiesServiceNew`) will work seamlessly with the corrected DTOs, completing the migration from protobuf-java to kotlinx.serialization.protobuf.