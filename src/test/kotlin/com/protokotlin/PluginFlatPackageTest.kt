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
            val protoMessagesFile = File(tempOutputDir, "ProtoMessages.kt")
            assertTrue(protoMessagesFile.exists(), "ProtoMessages.kt should be generated")
            
            val protoMessagesContent = protoMessagesFile.readText()
            println("Generated ProtoMessages.kt:")
            println(protoMessagesContent)
            
            // Verify flat package structure
            assertTrue(protoMessagesContent.contains("package de.markusfluer.heythere.`data`.proto"), 
                "Should use flat package structure")
            
            // Should not contain nested package like heythere_v1
            assertTrue(!protoMessagesContent.contains("heythere_v1"), 
                "Should not contain nested heythere_v1 package")
            
            // Should reference Mood without import (same package)
            assertTrue(protoMessagesContent.contains("mood: Mood? = null"), 
                "Should reference Mood type without nested package")
            
            // Should contain GetInboxResponse with InboxItem reference
            assertTrue(protoMessagesContent.contains("items: List<InboxItem> = emptyList()"), 
                "Should reference InboxItem type without nested package")
                
            // Check that enum files are generated separately
            val moodFile = File(tempOutputDir, "Mood.kt")
            assertTrue(moodFile.exists(), "Mood.kt should be generated")
            
        } finally {
            // Cleanup
            tempProtoDir.deleteRecursively()
            tempOutputDir.deleteRecursively()
        }
    }
}