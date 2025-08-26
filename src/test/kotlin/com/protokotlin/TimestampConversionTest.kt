package com.protokotlin

import com.protokotlin.resolver.TypeRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Specific unit tests focused on google.protobuf.Timestamp to kotlinx.datetime.Instant conversion
 */
class TimestampConversionTest {
    
    @Test
    fun testTimestampTypeResolution() {
        val typeRegistry = TypeRegistry()
        
        val resolved = typeRegistry.resolveType(
            "google.protobuf.Timestamp", 
            "test.proto", 
            "example"
        )
        
        assertNotNull(resolved, "google.protobuf.Timestamp should be resolved")
        assertTrue(resolved is TypeRegistry.ResolvedType.Scalar, "Should resolve as scalar type")
        assertEquals("kotlinx.datetime.Instant", (resolved as TypeRegistry.ResolvedType.Scalar).name)
    }
    
    @Test
    fun testDurationTypeResolution() {
        val typeRegistry = TypeRegistry()
        
        val resolved = typeRegistry.resolveType(
            "google.protobuf.Duration",
            "test.proto", 
            "example"
        )
        
        assertNotNull(resolved, "google.protobuf.Duration should be resolved")
        assertTrue(resolved is TypeRegistry.ResolvedType.Scalar, "Should resolve as scalar type")
        assertEquals("kotlin.time.Duration", (resolved as TypeRegistry.ResolvedType.Scalar).name)
    }
    
    @Test
    fun testWellKnownTypeMapping() {
        val typeRegistry = TypeRegistry()
        
        // Test all supported well-known types
        val testCases = mapOf(
            "google.protobuf.Timestamp" to "kotlinx.datetime.Instant",
            "google.protobuf.Duration" to "kotlin.time.Duration",
            "google.protobuf.Any" to "string",
            "google.protobuf.StringValue" to "string",
            "google.protobuf.Int32Value" to "int32",
            "google.protobuf.Int64Value" to "int64",
            "google.protobuf.UInt32Value" to "uint32",
            "google.protobuf.UInt64Value" to "uint64",
            "google.protobuf.BoolValue" to "bool",
            "google.protobuf.FloatValue" to "float",
            "google.protobuf.DoubleValue" to "double",
            "google.protobuf.BytesValue" to "bytes"
        )
        
        testCases.forEach { (protoType, expectedKotlinType) ->
            val resolved = typeRegistry.resolveType(protoType, "test.proto", "example")
            assertNotNull(resolved, "$protoType should be resolved")
            assertTrue(resolved is TypeRegistry.ResolvedType.Scalar, "$protoType should resolve as scalar")
            assertEquals(expectedKotlinType, (resolved as TypeRegistry.ResolvedType.Scalar).name, 
                "Incorrect mapping for $protoType")
        }
    }
    
    @Test  
    fun testNonWellKnownTypeNotMapped() {
        val typeRegistry = TypeRegistry()
        
        // Test that non-well-known types are not mapped
        val resolved = typeRegistry.resolveType(
            "example.CustomMessage",
            "test.proto",
            "example"
        )
        
        // Should return null since it's not a well-known type and not registered
        // (In real usage, custom types would be registered via registerFile)
        assertTrue(resolved == null, "Custom types should not be mapped as well-known types")
    }
}