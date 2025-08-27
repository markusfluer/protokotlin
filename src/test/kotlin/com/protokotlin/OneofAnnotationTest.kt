package com.protokotlin

import com.protokotlin.compiler.ProtoCompiler
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertContains

class OneofAnnotationTest {
    
    @Test
    fun testOneofProtoOneOfAnnotation() {
        val protoContent = """
            syntax = "proto3";
            
            package test;
            
            message TypeA {
                string value = 1;
            }
            
            message TypeB {
                int32 number = 1;
            }
            
            message TestMessage {
                string id = 1;
                oneof payload {
                    TypeA type_a = 2;
                    TypeB type_b = 3;
                }
            }
        """.trimIndent()
        
        // Create a temporary file for testing
        val tempFile = kotlin.io.path.createTempFile("test", ".proto")
        tempFile.toFile().writeText(protoContent)
        
        try {
            // Test using ProtoCompiler (the main entry point)
            val compiler = ProtoCompiler("com.test")
            val generatedFiles = compiler.compile(listOf(tempFile.toFile()))
            
            assertNotNull(generatedFiles)
            assertTrue(generatedFiles.containsKey("ProtoMessages.kt"))
            
            val protoMessagesFile = generatedFiles["ProtoMessages.kt"]!!
            println("Generated ProtoMessages.kt:")
            println(protoMessagesFile)
            
            // Verify @ProtoOneOf import is included
            assertContains(protoMessagesFile, "import kotlinx.serialization.protobuf.ProtoOneOf")
            
            // Verify @ProtoOneOf annotation is used on oneof fields
            assertContains(protoMessagesFile, "@ProtoOneOf")
            
            // Verify the oneof field has the annotation
            assertTrue(protoMessagesFile.contains("@ProtoOneOf") && protoMessagesFile.contains("payload:"))
            
            // Verify sealed class is generated for oneof
            assertContains(protoMessagesFile, "sealed class Payload")
            
            // Verify proto numbers are correct in oneof options
            assertContains(protoMessagesFile, "@ProtoNumber(2)")
            assertContains(protoMessagesFile, "@ProtoNumber(3)")
            
            // Verify @Serializable annotation is present
            assertContains(protoMessagesFile, "@Serializable")
            
            // Verify @OptIn annotation is present
            assertContains(protoMessagesFile, "@OptIn(ExperimentalSerializationApi::class)")
            
        } finally {
            // Clean up
            tempFile.toFile().delete()
        }
    }
    
    @Test
    fun testMultipleOneofsInSameMessage() {
        val protoContent = """
            syntax = "proto3";
            
            package test;
            
            message Option1 { string value = 1; }
            message Option2 { int32 value = 1; }
            message Option3 { bool value = 1; }
            message Option4 { float value = 1; }
            
            message ComplexMessage {
                string name = 1;
                oneof first_choice {
                    Option1 opt1 = 2;
                    Option2 opt2 = 3;
                }
                oneof second_choice {
                    Option3 opt3 = 4;
                    Option4 opt4 = 5;
                }
            }
        """.trimIndent()
        
        val tempFile = kotlin.io.path.createTempFile("test", ".proto")
        tempFile.toFile().writeText(protoContent)
        
        try {
            val compiler = ProtoCompiler("com.test")
            val generatedFiles = compiler.compile(listOf(tempFile.toFile()))
            
            assertNotNull(generatedFiles)
            assertTrue(generatedFiles.containsKey("ProtoMessages.kt"))
            
            val protoMessagesFile = generatedFiles["ProtoMessages.kt"]!!
            
            // Should have @ProtoOneOf annotation for both oneof fields
            val protoOneOfCount = protoMessagesFile.split("@ProtoOneOf").size - 1
            assertTrue(protoOneOfCount >= 2, "Should have at least 2 @ProtoOneOf annotations for 2 oneof fields")
            
            // Should have both firstChoice and secondChoice fields
            assertContains(protoMessagesFile, "firstChoice:")
            assertContains(protoMessagesFile, "secondChoice:")
            
            // Should have both sealed classes
            assertContains(protoMessagesFile, "sealed class FirstChoice")
            assertContains(protoMessagesFile, "sealed class SecondChoice")
            
        } finally {
            tempFile.toFile().delete()
        }
    }
}