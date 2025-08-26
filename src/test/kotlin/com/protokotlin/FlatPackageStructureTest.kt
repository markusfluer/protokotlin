package com.protokotlin

import com.protokotlin.compiler.ProtoCompiler
import kotlin.test.Test
import kotlin.test.assertTrue
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * Tests for the flatPackageStructure configuration option that fixes
 * import path resolution issues by putting all types in the same package.
 */
class FlatPackageStructureTest {
    
    @Test
    fun testFlatPackageStructureEnabled() {
        val tempProtoDir = createTempDirectory("proto").toFile()
        
        try {
            // Create common.proto with types
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
            
            // Create messaging.proto that imports common.proto
            val messagingProtoContent = """
                syntax = "proto3";
                
                package heythere.v1;
                
                message SendHeyThereRequest {
                  string friend_code = 1;
                  string word = 2;
                  Mood mood = 3;
                }
                
                message GetInboxResponse {
                  repeated InboxItem items = 1;
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
            
            println("Generated files with flat structure:")
            generatedFiles.forEach { (fileName, content) ->
                println("=== $fileName ===")
                println(content)
                println()
            }
            
            // Verify all files use the same package
            val sendHeyThereRequestFile = generatedFiles["SendHeyThereRequest.kt"]
            assertTrue(sendHeyThereRequestFile != null, "SendHeyThereRequest.kt should be generated")
            
            // Should use flat package structure (with backticks around 'data' keyword)
            assertTrue(sendHeyThereRequestFile!!.contains("package de.markusfluer.heythere.`data`.proto"))
            
            // Should not have nested package like "heythere_v1"
            assertTrue(!sendHeyThereRequestFile.contains("heythere_v1"), 
                "Should not contain nested heythere_v1 package")
            
            // Should properly reference Mood type without import issues
            assertTrue(sendHeyThereRequestFile.contains("mood: Mood? = null"), 
                "Should reference Mood type without nested package prefix")
            
            // Check InboxItem reference in GetInboxResponse
            val getInboxResponseFile = generatedFiles["GetInboxResponse.kt"]
            assertTrue(getInboxResponseFile != null, "GetInboxResponse.kt should be generated")
            
            assertTrue(getInboxResponseFile!!.contains("package de.markusfluer.heythere.`data`.proto"))
            assertTrue(getInboxResponseFile.contains("items: List<InboxItem> = emptyList()"), 
                "Should reference InboxItem without nested package prefix")
            
        } finally {
            tempProtoDir.deleteRecursively()
        }
    }
    
    @Test
    fun testFlatPackageStructureDisabled() {
        val tempProtoDir = createTempDirectory("proto").toFile()
        
        try {
            // Create the same proto files
            val commonProtoContent = """
                syntax = "proto3";
                
                package heythere.v1;
                
                enum Mood {
                  MOOD_UNSPECIFIED = 0;
                  HAPPY = 1;
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
            
            println("Generated files with nested structure:")
            generatedFiles.forEach { (fileName, content) ->
                println("=== $fileName ===")
                println(content)
                println()
            }
            
            // Verify legacy behavior creates nested packages
            val sendHeyThereRequestFile = generatedFiles["SendHeyThereRequest.kt"]
            assertTrue(sendHeyThereRequestFile != null, "SendHeyThereRequest.kt should be generated")
            
            // Should use nested package structure (legacy behavior)
            assertTrue(sendHeyThereRequestFile!!.contains("heythere_v1"), 
                "Legacy behavior should contain heythere_v1 package")
            
        } finally {
            tempProtoDir.deleteRecursively()
        }
    }
}