# Serialization Test for Protobuf Timestamp Fix

## Test Case

The issue you're experiencing suggests that despite our fixes, there might still be a problem with how the protobuf binary data is being deserialized.

## Key Checks

1. **Verify the plugin generates proper protobuf structures** (not direct kotlinx.datetime.Instant)
2. **Confirm @OptIn annotations are included** 
3. **Test actual serialization/deserialization** with binary protobuf data

## Possible Issues

### 1. Stale Generated Code
If your HeyThere app is still using old generated code that directly mapped to `kotlinx.datetime.Instant`, you need to:

```bash
# Regenerate the protobuf classes
./gradlew generateProtoKotlin --rerun-tasks
```

### 2. Wrong Serialization Format
The error suggests the deserializer is expecting ISO-8601 string but getting protobuf binary.

**Previous (broken) approach:**
```kotlin  
@Serializable
data class InboxItem(
    val createdAt: Instant? = null  // ❌ This tries to deserialize binary as string
)
```

**Fixed approach:**
```kotlin
@Serializable  
data class Timestamp(
    @ProtoNumber(1) val seconds: Long = 0L,
    @ProtoNumber(2) val nanos: Int = 0
)

@Serializable
data class InboxItem(
    val createdAt: Timestamp? = null  // ✅ This properly deserializes protobuf binary
)
```

### 3. Server/Client Compatibility
Ensure your server is sending:
- **Protobuf binary format** (not JSON) 
- **Correct field numbers** (seconds=1, nanos=2)
- **Proper protobuf Timestamp structure**

## Quick Fix Steps

1. **Update ProtoKotlin version** to 2.1.1+ with the timestamp fixes
2. **Regenerate your protobuf classes** completely
3. **Update your code** to use the new Timestamp structure:

```kotlin
// Old usage (causing errors):
val instant = inboxItem.createdAt  // Instant?

// New usage (working):  
val instant = inboxItem.createdAt?.toInstant()  // Convert when needed
```

## Testing Protobuf Compatibility

You can test if the serialization works by creating a simple test:

```kotlin
@Test
fun testProtobufTimestampSerialization() {
    val timestamp = Timestamp(seconds = 1640995200L, nanos = 0) // Jan 1, 2022
    val inboxItem = InboxItem(
        id = "test",
        message = "Hello", 
        createdAt = timestamp
    )
    
    // Serialize to protobuf binary
    val bytes = ProtoBuf.encodeToByteArray(inboxItem)
    
    // Deserialize from protobuf binary  
    val decoded = ProtoBuf.decodeFromByteArray<InboxItem>(bytes)
    
    assertEquals(timestamp, decoded.createdAt)
}
```

If this test passes, the fix is working. If it fails, there are still issues with the protobuf structure generation.