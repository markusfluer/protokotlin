# ProtoKotlin Oneof Field @ProtoOneOf Annotation Fix

## Problem Summary

ProtoKotlin v2.1.2 was generating incorrect Kotlin code for protobuf oneof fields, causing oneof payloads to always deserialize as null. The issue was that the generated oneof fields were missing the required `@ProtoOneOf` annotation.

## Root Cause

When ProtoKotlin encountered a protobuf oneof field like:

```protobuf
message InboxItem {
  oneof payload {
    HeyThere hey_there = 4;
    ThinkingOfYou thinking_of_you = 5;
  }
}
```

It was generating:

```kotlin
// INCORRECT - Missing @ProtoOneOf annotation
public val payload: Payload? = null,
```

Instead of:

```kotlin
// CORRECT - With @ProtoOneOf annotation  
@ProtoOneOf
public val payload: Payload? = null,
```

## Fix Implemented

### 1. Added ProtoOneOf Import
Added the necessary import to the KotlinGenerator:

```kotlin
private val protoOneOfAnnotation = ClassName("kotlinx.serialization.protobuf", "ProtoOneOf")
```

### 2. Applied @ProtoOneOf Annotation to Oneof Fields
Modified the oneof field generation in `generateDataClass()` method:

```kotlin
// Add oneof fields as properties
message.oneofs.forEach { oneof ->
    val propertyBuilder = PropertySpec.builder(propertyName, oneofTypeName.copy(nullable = true))
        .initializer(propertyName)
        .addAnnotation(protoOneOfAnnotation) // ✅ Added @ProtoOneOf annotation
    
    classBuilder.addProperty(propertyBuilder.build())
}
```

### 3. Generated Code Structure

Now generates correct code with both the field annotation and sealed class:

```kotlin
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class InboxItem(
    @ProtoNumber(1) val id: String? = null,
    @ProtoNumber(2) val message: String? = null,
    @ProtoOneOf val payload: Payload? = null  // ✅ @ProtoOneOf annotation added
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
sealed class Payload {
    @Serializable
    data class HeyThere(
        @ProtoNumber(4) val value: HeyThere?
    ) : Payload()

    @Serializable  
    data class ThinkingOfYou(
        @ProtoNumber(5) val value: ThinkingOfYou?
    ) : Payload()
}
```

## Testing

### Unit Tests Added
- `OneofAnnotationTest.kt` - Comprehensive tests for oneof field generation
- Tests single oneof per message
- Tests multiple oneofs in same message  
- Verifies @ProtoOneOf annotation presence
- Verifies sealed class generation with correct proto numbers

### Test Cases
```kotlin
// Single oneof test
message TestMessage {
    oneof payload {
        TypeA type_a = 2;
        TypeB type_b = 3; 
    }
}

// Multiple oneofs test
message ComplexMessage {
    oneof first_choice {
        Option1 opt1 = 2;
        Option2 opt2 = 3;
    }
    oneof second_choice {
        Option3 opt3 = 4;
        Option4 opt4 = 5;
    }
}
```

## Impact and Benefits

### ✅ **Fixed Serialization**
- Oneof fields now deserialize correctly from protobuf binary data
- No more null payloads when server sends valid oneof values

### ✅ **kotlinx.serialization.protobuf Compliance**
- Generated code follows kotlinx.serialization.protobuf requirements
- @ProtoOneOf annotation correctly identifies oneof fields to the serializer

### ✅ **Backward Compatible**
- Existing sealed class structure remains the same
- Only adds the missing annotation, no breaking changes

### ✅ **Comprehensive Testing**
- Unit tests ensure the fix works for various oneof scenarios
- Prevents regression in future releases

## Migration

### For Existing Users
1. **Update to ProtoKotlin v2.1.3** (contains the fix)
2. **Regenerate your protobuf classes**:
   ```bash
   ./gradlew clean generateProtoKotlin
   ```
3. **Verify generated code** includes `@ProtoOneOf` annotations on oneof fields
4. **Test serialization/deserialization** to confirm oneofs work correctly

### No Code Changes Required
The fix is purely in code generation - no changes needed to your usage patterns:

```kotlin
// Usage remains the same
val item = InboxItem(
    id = "123",
    message = "Hello",
    payload = Payload.HeyThere(HeyThere("value"))
)

// Serialization now works correctly  
val bytes = ProtoBuf.encodeToByteArray(item)
val decoded = ProtoBuf.decodeFromByteArray<InboxItem>(bytes)
assertEquals(item.payload, decoded.payload) // ✅ Now passes
```

## Files Modified

1. **KotlinGenerator.kt** - Added @ProtoOneOf annotation to oneof fields
2. **OneofAnnotationTest.kt** - Added comprehensive unit tests 
3. **Documentation updates** - Updated changelog and version

This fix resolves the critical serialization bug that made ProtoKotlin unusable for proto definitions containing oneof fields.