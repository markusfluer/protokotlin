package com.protokotlin

import com.protokotlin.gradle.ProtoKotlinTask
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertTrue
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * Test the Gradle plugin with flat package structure to fix import resolution issues
 */
class PluginFlatPackageTest {
    
    @Test
    fun testPluginWithFlatPackageStructure() {
        val project = ProjectBuilder.builder().build()
        
        val tempProtoDir = createTempDirectory("proto").toFile()
        val tempOutputDir = createTempDirectory("output").toFile()
        
        try {
            // Create proto files with cross-references
            val commonProtoContent = """
                syntax = "proto3";
                
                package heythere.v1;
                
                enum Mood {
                  MOOD_UNSPECIFIED = 0;
                  HAPPY = 1;
                  LOVE = 2;
                }
                
                message InboxItem {
                  string id = 1;
                  string from_friend_code = 2;
                  bool unread = 3;
                }
            """.trimIndent()
            
            val messagingProtoContent = """
                syntax = "proto3";
                
                package heythere.v1;
                
                message SendHeyThereRequest {
                  string friend_code = 1;
                  string word = 2;
                  Mood mood = 3;  // Cross-reference to common.proto
                }
                
                message GetInboxResponse {
                  repeated InboxItem items = 1;  // Cross-reference to common.proto
                }
            """.trimIndent()
            
            val commonFile = File(tempProtoDir, "common.proto")
            commonFile.writeText(commonProtoContent)
            
            val messagingFile = File(tempProtoDir, "messaging.proto")
            messagingFile.writeText(messagingProtoContent)
            
            // Create and configure the task with flat package structure
            val task = project.tasks.create("testGenerate", ProtoKotlinTask::class.java)
            task.protoDir.set(tempProtoDir)
            task.outputDir.set(tempOutputDir)
            task.packageName.set("de.markusfluer.heythere.data.proto")
            task.flatPackageStructure.set(true)  // Enable flat package structure
            
            // Execute the task
            task.execute()
            
            // Verify the generated files
            val sendHeyThereFile = File(tempOutputDir, "SendHeyThereRequest.kt")
            assertTrue(sendHeyThereFile.exists(), "SendHeyThereRequest.kt should be generated")
            
            val sendHeyThereContent = sendHeyThereFile.readText()
            println("Generated SendHeyThereRequest.kt:")
            println(sendHeyThereContent)
            
            // Verify flat package structure
            assertTrue(sendHeyThereContent.contains("package de.markusfluer.heythere.`data`.proto"), 
                "Should use flat package structure")
            
            // Should not contain nested package like heythere_v1
            assertTrue(!sendHeyThereContent.contains("heythere_v1"), 
                "Should not contain nested heythere_v1 package")
            
            // Should reference Mood without import (same package)
            assertTrue(sendHeyThereContent.contains("mood: Mood? = null"), 
                "Should reference Mood type without nested package")
            
            // Check GetInboxResponse
            val getInboxFile = File(tempOutputDir, "GetInboxResponse.kt")
            assertTrue(getInboxFile.exists(), "GetInboxResponse.kt should be generated")
            
            val getInboxContent = getInboxFile.readText()
            println("Generated GetInboxResponse.kt:")
            println(getInboxContent)
            
            // Should reference InboxItem without import (same package)
            assertTrue(getInboxContent.contains("items: List<InboxItem> = emptyList()"), 
                "Should reference InboxItem type without nested package")
            
            // Verify all files use same package
            assertTrue(getInboxContent.contains("package de.markusfluer.heythere.`data`.proto"), 
                "Should use same flat package structure")
            
        } finally {
            // Cleanup
            tempProtoDir.deleteRecursively()
            tempOutputDir.deleteRecursively()
        }
    }
}