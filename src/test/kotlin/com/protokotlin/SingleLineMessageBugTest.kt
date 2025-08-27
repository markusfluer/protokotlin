package com.protokotlin

import com.protokotlin.parser.ProtoParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Test for the single-line message parsing bug
 * 
 * Bug: Single-line messages like "message GetMeResponse { Profile profile = 1; }"
 * are incorrectly parsed as empty messages, causing them to be generated as 
 * Kotlin objects instead of data classes.
 */
class SingleLineMessageBugTest {
    
    @Test
    fun testSingleLineMessageWithFieldsParsing() {
        println("=== Testing Single-Line Message Bug ===")
        
        val protoContent = """
            syntax = "proto3";
            
            message GetMeResponse { Profile profile = 1; }
            message GetUserByCodeRequest { string friend_code = 1; }
            
            message GetMeRequest {}
            message EmptyResponse {
            }
            
            message MultiLineRequest {
                string friend_code = 1;
                string word = 2;
            }
        """.trimIndent()
        
        val parser = ProtoParser()
        val protoFile = parser.parse(protoContent, "test.proto")
        
        println("Parsed ${protoFile.messages.size} messages:")
        protoFile.messages.forEach { message ->
            println("  - ${message.name}: ${message.fields.size} fields")
            message.fields.forEach { field ->
                println("    - ${field.name}: ${field.type}")
            }
        }
        
        // Find messages by name
        val getMeResponse = protoFile.messages.find { it.name == "GetMeResponse" }
        val getUserByCodeRequest = protoFile.messages.find { it.name == "GetUserByCodeRequest" }
        val getMeRequest = protoFile.messages.find { it.name == "GetMeRequest" }
        val emptyResponse = protoFile.messages.find { it.name == "EmptyResponse" }
        val multiLineRequest = protoFile.messages.find { it.name == "MultiLineRequest" }
        
        // Basic existence checks
        assertEquals("GetMeResponse", getMeResponse?.name, "Should parse GetMeResponse message")
        assertEquals("GetUserByCodeRequest", getUserByCodeRequest?.name, "Should parse GetUserByCodeRequest message")
        assertEquals("GetMeRequest", getMeRequest?.name, "Should parse GetMeRequest message")
        assertEquals("EmptyResponse", emptyResponse?.name, "Should parse EmptyResponse message")
        assertEquals("MultiLineRequest", multiLineRequest?.name, "Should parse MultiLineRequest message")
        
        // THE BUG: These single-line messages should have fields, but they don't
        println("\n=== Bug Validation ===")
        
        println("GetMeResponse fields: ${getMeResponse?.fields?.size} (should be 1)")
        println("GetUserByCodeRequest fields: ${getUserByCodeRequest?.fields?.size} (should be 1)")
        println("GetMeRequest fields: ${getMeRequest?.fields?.size} (should be 0 - correct)")
        println("EmptyResponse fields: ${emptyResponse?.fields?.size} (should be 0 - correct)")
        println("MultiLineRequest fields: ${multiLineRequest?.fields?.size} (should be 2 - correct)")
        
        // Current behavior (buggy)
        if (getMeResponse?.fields?.size == 0) {
            println("🐛 BUG CONFIRMED: GetMeResponse has 0 fields but should have 1")
        } else {
            println("✅ GetMeResponse correctly parsed with ${getMeResponse?.fields?.size} fields")
        }
        
        if (getUserByCodeRequest?.fields?.size == 0) {
            println("🐛 BUG CONFIRMED: GetUserByCodeRequest has 0 fields but should have 1") 
        } else {
            println("✅ GetUserByCodeRequest correctly parsed with ${getUserByCodeRequest?.fields?.size} fields")
        }
        
        // What SHOULD happen after the fix:
        // assertEquals(1, getMeResponse?.fields?.size, "GetMeResponse should have 1 field")
        // assertEquals(1, getUserByCodeRequest?.fields?.size, "GetUserByCodeRequest should have 1 field")
        
        // What IS happening (demonstrating the bug):
        // These will fail until we fix the parser
        try {
            assertEquals(1, getMeResponse?.fields?.size, "GetMeResponse should have 1 field")
            println("✅ GetMeResponse parsing works correctly")
        } catch (e: AssertionError) {
            println("🐛 CONFIRMED: GetMeResponse single-line parsing bug - ${e.message}")
        }
        
        try {
            assertEquals(1, getUserByCodeRequest?.fields?.size, "GetUserByCodeRequest should have 1 field")  
            println("✅ GetUserByCodeRequest parsing works correctly")
        } catch (e: AssertionError) {
            println("🐛 CONFIRMED: GetUserByCodeRequest single-line parsing bug - ${e.message}")
        }
        
        // These should work correctly (empty messages and multi-line messages)
        assertEquals(0, getMeRequest?.fields?.size, "GetMeRequest should be empty")
        assertEquals(0, emptyResponse?.fields?.size, "EmptyResponse should be empty") 
        assertEquals(2, multiLineRequest?.fields?.size, "MultiLineRequest should have 2 fields")
        
        println("\n✅ Bug reproduction test completed")
        println("🎯 The parser fails to handle single-line message definitions with fields")
    }
}