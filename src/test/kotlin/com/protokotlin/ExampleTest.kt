package com.protokotlin

import com.protokotlin.generator.KotlinGenerator
import com.protokotlin.parser.ProtoParser
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExampleTest {
    
    @Test
    fun testSimpleProtoFile() {
        val protoContent = """
            syntax = "proto3";
            
            package example;
            
            message Person {
                string name = 1;
                int32 age = 2;
                bool active = 3;
            }
        """.trimIndent()
        
        val parser = ProtoParser()
        val protoFile = parser.parse(protoContent, "person.proto")
        
        assertNotNull(protoFile)
        assertTrue(protoFile.messages.size == 1)
        assertTrue(protoFile.messages[0].name == "Person")
        assertTrue(protoFile.messages[0].fields.size == 3)
        
        val generator = KotlinGenerator("com.example")
        val generatedFiles = generator.generate(protoFile)
        
        assertTrue(generatedFiles.containsKey("Person.kt"))
        val personClass = generatedFiles["Person.kt"]!!
        
        assertTrue(personClass.contains("@Serializable"))
        assertTrue(personClass.contains("data class Person"))
        // Proto3 fields should be nullable with null defaults
        assertTrue(personClass.contains("name: String? = null"))
        assertTrue(personClass.contains("age: Int? = null"))
        assertTrue(personClass.contains("active: Boolean? = null"))
    }
}