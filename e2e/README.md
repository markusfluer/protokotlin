# ProtoKotlin E2E Test

This directory contains a complete end-to-end example demonstrating ProtoKotlin's oneof field support and serialization capabilities.

## What's Tested

1. **Proto Definition**: Complex message with oneof fields
2. **Code Generation**: ProtoKotlin generates proper Kotlin classes
3. **@ProtoOneOf Annotation**: Critical fix for oneof serialization  
4. **File Structure**: Messages in ProtoMessages.kt, oneofs in separate sealed class files
5. **Serialization**: kotlinx.serialization.protobuf compatibility

## Files

- `test_message.proto` - Proto definition with oneof fields
- `google/protobuf/timestamp.proto` - Well-known type for testing
- `E2ETest.kt` - Complete end-to-end test
- `generated/` - Output directory for generated Kotlin code

## Test Results

✅ **ProtoKotlin v2.1.3** successfully:

- Generates `@ProtoOneOf` annotations on oneof fields (**CRITICAL FIX**)
- Creates proper sealed classes for oneof options  
- Uses single-field oneof variants to avoid "only 1 element" error
- Maintains correct proto field numbers
- Produces kotlinx.serialization.protobuf compatible code
- Organizes code into logical file structure
- Resolves real-world HeyThere application oneof deserialization issues

## Real-World Impact

This E2E test suite validates fixes for actual issues encountered in the HeyThere application:

1. **Null Payload Bug**: Before v2.1.3, oneof fields were always deserializing as null
2. **Serialization Compatibility**: Missing @ProtoOneOf annotation broke kotlinx.serialization.protobuf
3. **"Only 1 Element" Error**: Multi-field oneof variants caused runtime exceptions

All of these issues are now resolved and validated by the E2E tests.

## Key Fix

The critical fix in v2.1.3 was adding the missing `@ProtoOneOf` annotation to oneof fields, which caused them to always deserialize as null in previous versions.

**Before (v2.1.2 - BROKEN):**
```kotlin
public val payload: Payload? = null,
```

**After (v2.1.3 - FIXED):**  
```kotlin
@ProtoOneOf
public val payload: Payload? = null,
```

This annotation tells kotlinx.serialization.protobuf how to properly handle oneof fields during binary serialization/deserialization.