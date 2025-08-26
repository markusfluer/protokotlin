package com.protokotlin

import com.protokotlin.gradle.ProtoKotlinTask
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * Test the Gradle plugin integration to ensure it properly uses ProtoCompiler
 * with TypeRegistry for well-known type conversions.
 */
class PluginIntegrationTest {
    
    @Test
    fun testPluginTimestampConversion() {
        val project = ProjectBuilder.builder().build()
        
        // Create temporary directories
        val tempProtoDir = createTempDirectory("proto").toFile()
        val tempOutputDir = createTempDirectory("output").toFile()
        
        try {
            // Create a proto file with timestamp
            val protoContent = """
                syntax = "proto3";
                
                package test;
                
                import "google/protobuf/timestamp.proto";
                
                message Event {
                    string name = 1;
                    google.protobuf.Timestamp created_at = 2;
                }
            """.trimIndent()
            
            val protoFile = File(tempProtoDir, "event.proto")
            protoFile.writeText(protoContent)
            
            // Create and configure the task
            val task = project.tasks.create("testGenerate", ProtoKotlinTask::class.java)
            task.protoDir.set(tempProtoDir)
            task.outputDir.set(tempOutputDir)
            task.packageName.set("com.example.test")
            
            // Execute the task
            task.execute()
            
            // Verify the generated file
            val generatedFile = File(tempOutputDir, "Event.kt")
            assertTrue(generatedFile.exists(), "Event.kt should be generated")
            
            val generatedContent = generatedFile.readText()
            println("Generated content:")
            println(generatedContent)
            
            // Verify timestamp conversion
            assertTrue(generatedContent.contains("import kotlinx.datetime.Instant"), 
                "Should import kotlinx.datetime.Instant")
            assertTrue(generatedContent.contains("createdAt: Instant? = null"), 
                "Should use Instant type for timestamp field")
            assertTrue(generatedContent.contains("@Serializable"), 
                "Should have @Serializable annotation")
            assertTrue(generatedContent.contains("package com.example.test"), 
                "Should use correct package name")
                
        } finally {
            // Cleanup
            tempProtoDir.deleteRecursively()
            tempOutputDir.deleteRecursively()
        }
    }
    
    @Test
    fun testPluginWithMultipleWellKnownTypes() {
        val project = ProjectBuilder.builder().build()
        
        val tempProtoDir = createTempDirectory("proto").toFile()
        val tempOutputDir = createTempDirectory("output").toFile()
        
        try {
            val protoContent = """
                syntax = "proto3";
                
                package test;
                
                import "google/protobuf/timestamp.proto";
                import "google/protobuf/duration.proto";
                
                message ComplexEvent {
                    string name = 1;
                    google.protobuf.Timestamp created_at = 2;
                    google.protobuf.Duration timeout = 3;
                    repeated google.protobuf.Timestamp events = 4;
                }
            """.trimIndent()
            
            val protoFile = File(tempProtoDir, "complex_event.proto")
            protoFile.writeText(protoContent)
            
            val task = project.tasks.create("testGenerate", ProtoKotlinTask::class.java)
            task.protoDir.set(tempProtoDir)
            task.outputDir.set(tempOutputDir)
            task.packageName.set("com.example.plugin")
            
            task.execute()
            
            val generatedFile = File(tempOutputDir, "ComplexEvent.kt")
            assertTrue(generatedFile.exists(), "ComplexEvent.kt should be generated")
            
            val content = generatedFile.readText()
            println("Generated ComplexEvent.kt:")
            println(content)
            
            // Verify all conversions
            assertTrue(content.contains("import kotlinx.datetime.Instant"))
            assertTrue(content.contains("import kotlin.time.Duration") || content.contains("kotlin.time.Duration"))
            assertTrue(content.contains("createdAt: Instant? = null"))
            assertTrue(content.contains("timeout:") && (content.contains("Duration?") || content.contains("kotlin.time.Duration?")))
            assertTrue(content.contains("events: List<Instant> = emptyList()"))
            
        } finally {
            tempProtoDir.deleteRecursively()
            tempOutputDir.deleteRecursively()
        }
    }
    
    @Test
    fun testPluginWithEmptyMessage() {
        val project = ProjectBuilder.builder().build()
        
        val tempProtoDir = createTempDirectory("proto").toFile()
        val tempOutputDir = createTempDirectory("output").toFile()
        
        try {
            val protoContent = """
                syntax = "proto3";
                
                package test;
                
                message Empty {
                    // No fields
                }
                
                message WithField {
                    string value = 1;
                }
            """.trimIndent()
            
            val protoFile = File(tempProtoDir, "mixed.proto")
            protoFile.writeText(protoContent)
            
            val task = project.tasks.create("testGenerate", ProtoKotlinTask::class.java)
            task.protoDir.set(tempProtoDir)
            task.outputDir.set(tempOutputDir)
            task.packageName.set("com.example.empty")
            
            task.execute()
            
            // Verify empty message generates as object
            val emptyFile = File(tempOutputDir, "Empty.kt")
            assertTrue(emptyFile.exists(), "Empty.kt should be generated")
            
            val emptyContent = emptyFile.readText()
            assertTrue(emptyContent.contains("object Empty"), "Empty message should be an object")
            
            // Verify regular message generates as data class
            val withFieldFile = File(tempOutputDir, "WithField.kt")
            assertTrue(withFieldFile.exists(), "WithField.kt should be generated")
            
            val withFieldContent = withFieldFile.readText()
            assertTrue(withFieldContent.contains("data class WithField"), "WithField should be a data class")
            
        } finally {
            tempProtoDir.deleteRecursively()
            tempOutputDir.deleteRecursively()
        }
    }
}