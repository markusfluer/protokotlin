# Timestamp and Duration Conversion Solution

## Problem Summary

The user reported serialization issues when using `google.protobuf.Timestamp` fields in their protobuf messages:

```
Failed to parse inbox response
kotlinx.datetime.DateTimeFormatException: Failed to parse an instant from '������'
    at kotlinx.datetime.Instant$Companion.parse(Instant.kt:79)
    ...
```

## Root Cause Analysis

The issue was that ProtoKotlin was attempting to directly convert `google.protobuf.Timestamp` to `kotlinx.datetime.Instant` in the type system. However:

1. **Protobuf wire format**: `google.protobuf.Timestamp` is serialized as a protobuf message with two fields:
   - `int64 seconds = 1;` - seconds since Unix epoch 
   - `int32 nanos = 2;` - nanosecond fractions

2. **kotlinx.serialization expectation**: `kotlinx.datetime.Instant` expects to be serialized as an ISO-8601 string format

3. **Incompatibility**: The protobuf binary format (`seconds` + `nanos` fields) cannot be directly deserialized into the string-based `kotlinx.datetime.Instant` format.

## Solution Implemented

### 1. Generate Proper Protobuf Structures

Instead of mapping `google.protobuf.Timestamp` directly to `kotlinx.datetime.Instant`, we now:

1. **Register well-known types as proper protobuf messages** in `TypeRegistry.registerWellKnownTypes()`:

```kotlin
val timestampMessage = ProtoMessage(
    name = "Timestamp",
    fields = listOf(
        ProtoField(
            name = "seconds",
            type = ProtoType.Scalar(ScalarType.INT64),
            number = 1,
            label = FieldLabel.OPTIONAL
        ),
        ProtoField(
            name = "nanos", 
            type = ProtoType.Scalar(ScalarType.INT32),
            number = 2,
            label = FieldLabel.OPTIONAL
        )
    ),
    oneofs = emptyList(),
    nestedMessages = emptyList(), 
    nestedEnums = emptyList()
)
```

2. **Generate proper data classes** that match protobuf structure:

```kotlin
@Serializable
data class Timestamp(
    @ProtoNumber(1)
    val seconds: Long = 0L,
    @ProtoNumber(2)
    val nanos: Int = 0
)

@Serializable  
data class Duration(
    @ProtoNumber(1)
    val seconds: Long = 0L,
    @ProtoNumber(2)
    val nanos: Int = 0
)
```

### 2. Include Well-Known Types When Referenced

Modified `MessageScheduler.generateAllMessagesFile()` to:

1. **Scan for referenced well-known types** in user messages
2. **Automatically include** `Timestamp` and `Duration` classes when they're used
3. **Generate in ProtoMessages.kt** alongside user-defined messages

### 3. Provide Conversion Utilities

Created `WellKnownTypes.kt` utility class with conversion methods:

```kotlin
// Protobuf Timestamp to kotlinx.datetime.Instant
fun Timestamp.toInstant(): Instant {
    return Instant.fromEpochSeconds(seconds, nanos.toLong())
}

// kotlinx.datetime.Instant to Protobuf Timestamp  
fun Timestamp.Companion.fromInstant(instant: Instant): Timestamp {
    return Timestamp(
        seconds = instant.epochSeconds,
        nanos = instant.nanosecondsOfSecond
    )
}
```

## Generated Code Changes

### Before (Problematic)
```kotlin
// This was causing serialization errors
@Serializable
data class TestMessage(
    @ProtoNumber(1) val name: String? = null,
    @ProtoNumber(2) val createdAt: Instant? = null  // ❌ Wrong type
)
```

### After (Fixed)  
```kotlin
@Serializable
data class Timestamp(
    @ProtoNumber(1) val seconds: Long = 0L,
    @ProtoNumber(2) val nanos: Int = 0
)

@Serializable
data class TestMessage(
    @ProtoNumber(1) val name: String? = null,
    @ProtoNumber(2) val createdAt: Timestamp? = null  // ✅ Correct protobuf structure
)
```

## Usage Guide

### 1. Protobuf Definition
```protobuf
syntax = "proto3";
import "google/protobuf/timestamp.proto";

message Event {
    string name = 1;
    google.protobuf.Timestamp created_at = 2;
}
```

### 2. Generated Kotlin
The plugin now automatically generates both the protobuf-compatible structure AND your message:

```kotlin
@Serializable
data class Timestamp(
    @ProtoNumber(1) val seconds: Long = 0L,
    @ProtoNumber(2) val nanos: Int = 0  
)

@Serializable
data class Event(
    @ProtoNumber(1) val name: String? = null,
    @ProtoNumber(2) val createdAt: Timestamp? = null
)
```

### 3. Converting to kotlinx Types
```kotlin
import com.protokotlin.util.Timestamp
import com.protokotlin.util.toInstant
import com.protokotlin.util.fromInstant

// Convert protobuf Timestamp to kotlinx Instant
val event: Event = // ... from protobuf deserialization
val kotlinxInstant = event.createdAt?.toInstant()

// Convert kotlinx Instant to protobuf Timestamp
val now = Clock.System.now()
val protobufTimestamp = Timestamp.fromInstant(now)
val newEvent = Event(name = "Test", createdAt = protobufTimestamp)
```

## Technical Changes Made

### Files Modified:
1. **TypeRegistry.kt** - Added `registerWellKnownTypes()` method
2. **MessageScheduler.kt** - Added well-known type inclusion logic  
3. **KotlinGenerator.kt** - Updated to handle well-known types as messages
4. **WellKnownTypesTest.kt** - Updated tests to expect protobuf structures

### Files Created:
1. **WellKnownTypes.kt** - Utility classes and conversion functions

## Benefits

1. **✅ Serialization Compatibility** - Generated code works with kotlinx.serialization.protobuf
2. **✅ Proper Protobuf Compliance** - Matches official protobuf Timestamp/Duration structure  
3. **✅ Type Safety** - Compile-time checks for field structure
4. **✅ Easy Conversion** - Utility functions for kotlinx type conversion
5. **✅ Automatic Generation** - Well-known types included automatically when referenced

## Migration Guide

Existing code using the old approach will need updates:

### Before
```kotlin
// This would cause runtime errors
val instant: Instant? = message.createdAt
```

### After  
```kotlin
// Convert to kotlinx type when needed
val instant: Instant? = message.createdAt?.toInstant()
```

## Troubleshooting Payload Transformation Issues

### Problem: Still Getting Deserialization Errors
If you're still seeing errors like:
```
kotlinx.datetime.DateTimeFormatException: Failed to parse an instant from '������'
```

This indicates **stale generated code** or **caching issues**.

### Solution Steps:

1. **Force Regenerate Classes:**
   ```bash
   ./gradlew clean
   ./gradlew generateProtoKotlin --rerun-tasks
   ./gradlew build
   ```

2. **Verify Generated Code:**
   Check that your generated `ProtoMessages.kt` contains:
   ```kotlin
   @OptIn(ExperimentalSerializationApi::class)
   @Serializable
   data class Timestamp(
       @ProtoNumber(1) val seconds: Long = 0L,
       @ProtoNumber(2) val nanos: Int = 0
   )
   
   @OptIn(ExperimentalSerializationApi::class) 
   @Serializable
   data class InboxItem(
       @ProtoNumber(3) val createdAt: Timestamp? = null  // NOT Instant!
   )
   ```

3. **Update Code References:**
   ```kotlin
   // Old (broken):
   val time: Instant? = inboxItem.createdAt
   
   // New (working):
   val time: Instant? = inboxItem.createdAt?.toInstant()
   ```

4. **Test Serialization:**
   ```kotlin
   @Test 
   fun testProtobufTimestamp() {
       val item = InboxItem(
           id = "test",
           createdAt = Timestamp(seconds = 1640995200L, nanos = 0)
       )
       
       // This should work without errors:
       val bytes = ProtoBuf.encodeToByteArray(item)
       val decoded = ProtoBuf.decodeFromByteArray<InboxItem>(bytes)
       assertEquals(item.createdAt, decoded.createdAt)
   }
   ```

### Important Notes:
- **@OptIn Annotation**: Required for kotlinx.serialization.protobuf experimental API
- **Protobuf Binary Format**: Server must send proper protobuf binary, not JSON
- **Field Structure**: Timestamp must have `seconds` and `nanos` fields, not direct `Instant`

The solution ensures full compatibility with protobuf serialization while providing easy conversion to kotlinx types when needed.