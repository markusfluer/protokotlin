package com.protokotlin

import com.protokotlin.compiler.ProtoCompiler
import com.protokotlin.generator.KotlinGenerator
import com.protokotlin.parser.ProtoParser
import com.protokotlin.resolver.TypeRegistry
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertContains

class WellKnownTypesTest {
    
    @Test
    fun testTimestampConversion() {
        val protoContent = """
            syntax = "proto3";
            
            package example;
            
            import "google/protobuf/timestamp.proto";
            
            message Event {
                string name = 1;
                google.protobuf.Timestamp created_at = 2;
                google.protobuf.Timestamp updated_at = 3;
            }
        """.trimIndent()
        
        // Create a temporary file for testing
        val tempFile = kotlin.io.path.createTempFile("test", ".proto")
        tempFile.toFile().writeText(protoContent)
        
        try {
            // Test using ProtoCompiler (the main entry point)
            val compiler = ProtoCompiler("com.example")
            val generatedFiles = compiler.compile(listOf(tempFile.toFile()))
            
            assertNotNull(generatedFiles)
            assertTrue(generatedFiles.containsKey("ProtoMessages.kt"))
            
            val protoMessagesFile = generatedFiles["ProtoMessages.kt"]!!
            println("Generated ProtoMessages.kt:")
            println(protoMessagesFile)
            
            // Verify Timestamp structure is generated
            assertContains(protoMessagesFile, "data class Timestamp")
            assertContains(protoMessagesFile, "seconds: Long")
            assertContains(protoMessagesFile, "nanos: Int")
            
            // Verify Event message uses Timestamp fields
            assertContains(protoMessagesFile, "createdAt: Timestamp? = null")
            assertContains(protoMessagesFile, "updatedAt: Timestamp? = null")
            
            // Verify @Serializable annotation is present
            assertContains(protoMessagesFile, "@Serializable")
            
            // Verify proto field numbers
            assertContains(protoMessagesFile, "@ProtoNumber(2)")
            assertContains(protoMessagesFile, "@ProtoNumber(3)")
        } finally {
            // Clean up
            tempFile.toFile().delete()
        }
    }
    
    @Test
    fun testDurationConversion() {
        val protoContent = """
            syntax = "proto3";
            
            package example;
            
            import "google/protobuf/duration.proto";
            
            message Task {
                string name = 1;
                google.protobuf.Duration timeout = 2;
                google.protobuf.Duration estimated_duration = 3;
            }
        """.trimIndent()
        
        // Create a temporary file for testing
        val tempFile = kotlin.io.path.createTempFile("test", ".proto")
        tempFile.toFile().writeText(protoContent)
        
        try {
            // Test using ProtoCompiler (the main entry point)
            val compiler = ProtoCompiler("com.example")
            val generatedFiles = compiler.compile(listOf(tempFile.toFile()))
            
            assertNotNull(generatedFiles)
            assertTrue(generatedFiles.containsKey("ProtoMessages.kt"))
            
            val protoMessagesFile = generatedFiles["ProtoMessages.kt"]!!
            println("Generated ProtoMessages.kt:")
            println(protoMessagesFile)
            
            // Verify Duration structure is generated
            assertContains(protoMessagesFile, "data class Duration")
            assertContains(protoMessagesFile, "seconds: Long")
            assertContains(protoMessagesFile, "nanos: Int")
            
            // Verify Task message uses Duration fields
            assertContains(protoMessagesFile, "timeout: Duration? = null")
            assertContains(protoMessagesFile, "estimatedDuration: Duration? = null")
            
            // Verify @Serializable annotation
            assertContains(protoMessagesFile, "@Serializable")
        } finally {
            // Clean up
            tempFile.toFile().delete()
        }
    }
    
    @Test
    fun testMixedWellKnownTypes() {
        val protoContent = """
            syntax = "proto3";
            
            package example;
            
            import "google/protobuf/timestamp.proto";
            import "google/protobuf/duration.proto";
            import "google/protobuf/any.proto";
            
            message ComplexMessage {
                string name = 1;
                google.protobuf.Timestamp created_at = 2;
                google.protobuf.Duration processing_time = 3;
                google.protobuf.Any metadata = 4;
                repeated google.protobuf.Timestamp event_times = 5;
                map<string, google.protobuf.Duration> phase_durations = 6;
            }
        """.trimIndent()
        
        val typeRegistry = TypeRegistry()
        val parser = ProtoParser()
        val protoFile = parser.parse(protoContent, "complex.proto")
        
        typeRegistry.registerFile(protoFile)
        
        val generator = KotlinGenerator("com.example", typeRegistry)
        val generatedFiles = generator.generate(protoFile)
        
        assertNotNull(generatedFiles)
        assertTrue(generatedFiles.containsKey("ComplexMessage.kt"))
        
        val complexClass = generatedFiles["ComplexMessage.kt"]!!
        println("Generated ComplexMessage.kt:")
        println(complexClass)
        
        // Verify imports for all well-known types
        assertContains(complexClass, "import kotlinx.datetime.Instant")
        assertContains(complexClass, "import kotlin.time.Duration")
        assertContains(complexClass, "import kotlin.String")
        assertContains(complexClass, "import kotlin.collections.List")
        assertContains(complexClass, "import kotlin.collections.Map")
        
        // Verify individual field conversions
        assertContains(complexClass, "createdAt: Instant? = null")
        assertContains(complexClass, "processingTime: Duration? = null")
        assertContains(complexClass, "metadata: String? = null")  // Any maps to String
        
        // Verify repeated and map conversions
        assertContains(complexClass, "eventTimes: List<Instant> = emptyList()")
        assertContains(complexClass, "phaseDurations: Map<String, Duration?>? = null")
        
        // Verify annotations
        assertContains(complexClass, "@Serializable")
        assertContains(complexClass, "@ProtoPacked")  // For repeated fields
    }
    
    @Test
    fun testOneofWithWellKnownTypes() {
        val protoContent = """
            syntax = "proto3";
            
            package example;
            
            import "google/protobuf/timestamp.proto";
            import "google/protobuf/duration.proto";
            
            message TimeValue {
                oneof time_type {
                    google.protobuf.Timestamp timestamp = 1;
                    google.protobuf.Duration duration = 2;
                    string text_time = 3;
                }
            }
        """.trimIndent()
        
        val typeRegistry = TypeRegistry()
        val parser = ProtoParser()
        val protoFile = parser.parse(protoContent, "time_value.proto")
        
        typeRegistry.registerFile(protoFile)
        
        val generator = KotlinGenerator("com.example", typeRegistry)
        val generatedFiles = generator.generate(protoFile)
        
        assertNotNull(generatedFiles)
        assertTrue(generatedFiles.containsKey("TimeValue.kt"))
        
        val timeValueClass = generatedFiles["TimeValue.kt"]!!
        println("Generated TimeValue.kt:")
        println(timeValueClass)
        
        // Verify imports
        assertContains(timeValueClass, "import kotlinx.datetime.Instant")
        // Duration might be imported or used inline - both are acceptable
        
        // Verify sealed class generation for oneof
        assertContains(timeValueClass, "sealed class TimeType")
        
        // Verify oneof options with correct types
        assertContains(timeValueClass, "value`: Instant?")  // Timestamp option
        assertContains(timeValueClass, "value`: kotlin.time.Duration?")  // Duration option (inline)
        assertContains(timeValueClass, "value`: String?")   // text_time option
    }
    
    @Test
    fun testCompilerIntegrationWithWellKnownTypes() {
        val protoContent = """
            syntax = "proto3";
            
            package example;
            
            import "google/protobuf/timestamp.proto";
            
            message User {
                string name = 1;
                google.protobuf.Timestamp last_login = 2;
            }
        """.trimIndent()
        
        // Test using ProtoCompiler (the main entry point)
        val compiler = ProtoCompiler("com.example")
        
        // Create a temporary file for testing
        val tempFile = kotlin.io.path.createTempFile("test", ".proto")
        tempFile.toFile().writeText(protoContent)
        
        try {
            val generatedFiles = compiler.compile(listOf(tempFile.toFile()))
            
            assertNotNull(generatedFiles)
            assertTrue(generatedFiles.containsKey("ProtoMessages.kt"))
            
            val userClass = generatedFiles["ProtoMessages.kt"]!!
            println("Generated User.kt via ProtoCompiler:")
            println(userClass)
            
            // Verify the conversion worked through the full pipeline
            assertContains(userClass, "import kotlinx.datetime.Instant")
            assertContains(userClass, "lastLogin: Instant? = null")
            assertContains(userClass, "@Serializable")
        } finally {
            // Clean up
            tempFile.toFile().delete()
        }
    }
    
    @Test
    fun testTypeRegistryWellKnownTypeResolution() {
        val typeRegistry = TypeRegistry()
        
        // Test timestamp resolution
        val timestampResolved = typeRegistry.resolveType(
            "google.protobuf.Timestamp", 
            "test.proto", 
            "example"
        )
        
        assertNotNull(timestampResolved)
        assertTrue(timestampResolved is TypeRegistry.ResolvedType.Scalar)
        assertTrue((timestampResolved as TypeRegistry.ResolvedType.Scalar).name == "kotlinx.datetime.Instant")
        
        // Test duration resolution
        val durationResolved = typeRegistry.resolveType(
            "google.protobuf.Duration",
            "test.proto",
            "example"
        )
        
        assertNotNull(durationResolved)
        assertTrue(durationResolved is TypeRegistry.ResolvedType.Scalar)
        assertTrue((durationResolved as TypeRegistry.ResolvedType.Scalar).name == "kotlin.time.Duration")
        
        // Test other well-known type
        val anyResolved = typeRegistry.resolveType(
            "google.protobuf.Any",
            "test.proto", 
            "example"
        )
        
        assertNotNull(anyResolved)
        assertTrue(anyResolved is TypeRegistry.ResolvedType.Scalar)
        assertTrue((anyResolved as TypeRegistry.ResolvedType.Scalar).name == "string")
    }
}