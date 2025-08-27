package com.protokotlin

import com.protokotlin.compiler.ProtoCompiler
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test that specifically validates the production bug fix for single-line messages
 */
class ProductionBugFixTest {
    
    @Test
    fun testProductionSingleLineMessageBugFixed() {
        println("=== Production Bug Fix Validation ===")
        
        // These are the exact messages that were causing issues in production:
        val protoContent = """
            syntax = "proto3";
            
            // These were incorrectly generated as objects in the buggy version
            message GetMeResponse { Profile profile = 1; }
            message GetUserByCodeRequest { string friend_code = 1; }
            
            // These should remain as objects (correct behavior)  
            message GetMeRequest {}
            message SendHeyThereResponse {}
            
            // This should work (multi-line already worked)
            message SendHeyThereRequest {
                string friend_code = 1;
                string word = 2;
                Mood mood = 3;
            }
        """.trimIndent()
        
        val tempFile = kotlin.io.path.createTempFile("production_fix", ".proto")
        tempFile.toFile().writeText(protoContent)
        
        try {
            val compiler = ProtoCompiler("com.production.fix")
            val generated = compiler.compile(listOf(tempFile.toFile()))
            
            assertTrue(generated.containsKey("ProtoMessages.kt"), "Should generate ProtoMessages.kt")
            val protoMessages = generated["ProtoMessages.kt"]!!
            
            println("✅ Validating production bug fixes...")
            
            // Critical fix validation: These should now be data classes
            assertTrue(protoMessages.contains("data class GetMeResponse("), 
                "🐛➜✅ GetMeResponse should now be data class (was incorrectly object)")
            assertTrue(protoMessages.contains("data class GetUserByCodeRequest("), 
                "🐛➜✅ GetUserByCodeRequest should now be data class (was incorrectly object)")
            
            // These should remain objects (correct behavior)
            assertTrue(protoMessages.contains("object GetMeRequest"), 
                "✅ GetMeRequest should remain object (empty message)")
            assertTrue(protoMessages.contains("object SendHeyThereResponse"), 
                "✅ SendHeyThereResponse should remain object (empty message)")
            
            // Multi-line should still work correctly
            assertTrue(protoMessages.contains("data class SendHeyThereRequest("), 
                "✅ SendHeyThereRequest should be data class (multi-line worked before)")
            
            // Verify field parsing works correctly
            assertTrue(protoMessages.contains("profile: Profile?"), 
                "✅ GetMeResponse should have profile field")
            assertTrue(protoMessages.contains("friendCode: String?"), 
                "✅ GetUserByCodeRequest should have friendCode field")
            assertTrue(protoMessages.contains("word: String?"), 
                "✅ SendHeyThereRequest should have word field")
            
            println("\n🎉 PRODUCTION BUG FIX VALIDATED!")
            println("📋 Summary:")
            println("   - GetMeResponse: object ➜ data class ✅") 
            println("   - GetUserByCodeRequest: object ➜ data class ✅")
            println("   - GetMeRequest: object ➜ object ✅ (unchanged)")
            println("   - SendHeyThereResponse: object ➜ object ✅ (unchanged)")
            println("   - SendHeyThereRequest: data class ➜ data class ✅ (unchanged)")
            
            println("\n🔧 Root Cause Fixed:")
            println("   - Parser now correctly handles single-line message definitions")
            println("   - Messages with fields on same line as braces are properly parsed")
            println("   - Empty messages still generate as objects (correct)")
            
        } finally {
            tempFile.toFile().delete()
        }
    }
}