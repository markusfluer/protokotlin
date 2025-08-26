package com.protokotlin

import com.protokotlin.compiler.ProtoCompiler
import kotlin.test.Test
import java.io.File

/**
 * Simple debug test to check cross-file reference behavior
 */
class DebugCrossRefTest {
    
    @Test
    fun debugCrossFileReferences() {
        val tempDir = File.createTempFile("proto", "").parentFile
        val testDir = File(tempDir, "debug_test_${System.currentTimeMillis()}")
        testDir.mkdirs()
        
        try {
            val commonFile = File(testDir, "common.proto")
            commonFile.writeText("""
                syntax = "proto3";
                package heythere.v1;
                
                enum Mood {
                  MOOD_UNSPECIFIED = 0;
                  HAPPY = 1;
                }
                
                message InboxItem {
                  string id = 1;
                }
            """.trimIndent())
            
            val messagingFile = File(testDir, "messaging.proto")
            messagingFile.writeText("""
                syntax = "proto3";
                package heythere.v1;
                
                message SendHeyThereRequest {
                  string friend_code = 1;
                  Mood mood = 2;
                }
            """.trimIndent())
            
            // Test with flat package structure
            val compiler1 = ProtoCompiler(
                "de.test.proto",
                listOf(testDir),
                flatPackageStructure = true
            )
            
            val generated1 = compiler1.compile(listOf(commonFile, messagingFile))
            
            println("=== Generated Files (Flat Package Structure) ===")
            generated1.forEach { (name, content) ->
                println("--- $name ---")
                println(content)
                println()
            }
            
            // Test with nested package structure
            val compiler2 = ProtoCompiler(
                "de.test.proto",
                listOf(testDir),
                flatPackageStructure = false
            )
            
            val generated2 = compiler2.compile(listOf(commonFile, messagingFile))
            
            println("=== Generated Files (Nested Package Structure) ===")
            generated2.forEach { (name, content) ->
                println("--- $name ---")
                println(content)
                println()
            }
            
            val generated = generated1
            
            // Check if Mood is properly referenced
            val sendHeyThereContent = generated["SendHeyThereRequest.kt"]
            println("SendHeyThereRequest content check:")
            println("Contains 'mood: Mood': ${sendHeyThereContent?.contains("mood: Mood")}")
            println("All content: $sendHeyThereContent")
            
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            testDir.deleteRecursively()
        }
    }
}