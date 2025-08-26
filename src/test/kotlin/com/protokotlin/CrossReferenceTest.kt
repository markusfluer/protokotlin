package com.protokotlin

import com.protokotlin.compiler.ProtoCompiler
import kotlin.test.Test
import kotlin.test.assertTrue
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * Tests for cross-file type references to fix import resolution issues.
 */
class CrossReferenceTest {
    
    @Test
    fun testCrossFileReferenceWithFlatPackage() {
        val tempProtoDir = createTempDirectory("proto").toFile()
        
        try {
            // Create common.proto with types that will be referenced by other files
            val commonProtoContent = """
                syntax = "proto3";
                
                package heythere.v1;
                
                enum Mood {
                  MOOD_UNSPECIFIED = 0;
                  HAPPY = 1;
                  LOVE = 2;
                  SAD = 3;
                }
                
                message InboxItem {
                  string id = 1;
                  string from_friend_code = 2;
                  bool unread = 3;
                }
            """.trimIndent()
            
            // Create messaging.proto that references types from common.proto
            val messagingProtoContent = """
                syntax = "proto3";
                
                package heythere.v1;
                
                message SendHeyThereRequest {
                  string friend_code = 1;
                  string word = 2;
                  Mood mood = 3;  // Should reference Mood from common.proto
                }
                
                message GetInboxResponse {
                  repeated InboxItem items = 1;  // Should reference InboxItem from common.proto
                }
            """.trimIndent()
            
            val commonFile = File(tempProtoDir, "common.proto")
            commonFile.writeText(commonProtoContent)
            
            val messagingFile = File(tempProtoDir, "messaging.proto")
            messagingFile.writeText(messagingProtoContent)
            
            // Test with flatPackageStructure enabled
            val compiler = ProtoCompiler(
                "de.markusfluer.heythere.data.proto", 
                listOf(tempProtoDir),
                flatPackageStructure = true
            )
            
            val generatedFiles = compiler.compile(listOf(commonFile, messagingFile))
            
            println("Generated files with cross-references:")
            generatedFiles.forEach { (fileName, content) ->
                println("=== $fileName ===")
                println(content)
                println()
            }
            
            println("Total generated files: ${generatedFiles.size}")
            println("Generated file names: ${generatedFiles.keys.joinToString(", ")}")
            
            // Verify messages are in ProtoMessages.kt
            val protoMessagesFile = generatedFiles["ProtoMessages.kt"]
            assertTrue(protoMessagesFile != null, "ProtoMessages.kt should be generated")
            
            // All files should use flat package structure
            assertTrue(protoMessagesFile!!.contains("package de.markusfluer.heythere.`data`.proto"),
                "Should use flat package structure")
            
            // Should properly reference Mood type from common.proto
            assertTrue(protoMessagesFile.contains("mood: Mood? = null"),
                "Should reference Mood type correctly")
            
            // Should contain GetInboxResponse that references InboxItem
            assertTrue(protoMessagesFile.contains("items: List<InboxItem> = emptyList()"),
                "Should reference InboxItem type correctly")
            
            // Verify enum files are generated separately
            assertTrue(generatedFiles.containsKey("Mood.kt"), "Mood.kt should be generated")
            
            // Verify all files use the same package (no cross-package imports needed)
            val moodFile = generatedFiles["Mood.kt"]
            assertTrue(moodFile!!.contains("package de.markusfluer.heythere.`data`.proto"),
                "Mood should use same flat package structure")
            
        } finally {
            tempProtoDir.deleteRecursively()
        }
    }
    
    @Test
    fun testCrossFileReferenceWithNestedPackage() {
        val tempProtoDir = createTempDirectory("proto").toFile()
        
        try {
            // Same proto content as above
            val commonProtoContent = """
                syntax = "proto3";
                
                package heythere.v1;
                
                enum Mood {
                  MOOD_UNSPECIFIED = 0;
                  HAPPY = 1;
                }
                
                message InboxItem {
                  string id = 1;
                  bool unread = 2;
                }
            """.trimIndent()
            
            val messagingProtoContent = """
                syntax = "proto3";
                
                package heythere.v1;
                
                message SendHeyThereRequest {
                  string friend_code = 1;
                  Mood mood = 2;
                }
            """.trimIndent()
            
            val commonFile = File(tempProtoDir, "common.proto")
            commonFile.writeText(commonProtoContent)
            
            val messagingFile = File(tempProtoDir, "messaging.proto")
            messagingFile.writeText(messagingProtoContent)
            
            // Test with flatPackageStructure disabled (legacy behavior)
            val compiler = ProtoCompiler(
                "de.markusfluer.heythere.data.proto", 
                listOf(tempProtoDir),
                flatPackageStructure = false
            )
            
            val generatedFiles = compiler.compile(listOf(commonFile, messagingFile))
            
            println("Generated files with nested packages:")
            generatedFiles.forEach { (fileName, content) ->
                println("=== $fileName ===")
                println(content)
                println()
            }
            
            // Verify the legacy behavior still generates nested package structures
            val protoMessagesFile = generatedFiles["ProtoMessages.kt"]
            assertTrue(protoMessagesFile != null, "ProtoMessages.kt should be generated")
            
            // Should use nested package structure (legacy behavior)
            assertTrue(protoMessagesFile!!.contains("heythere_v1"),
                "Legacy behavior should contain heythere_v1 package")
            
        } finally {
            tempProtoDir.deleteRecursively()
        }
    }
}