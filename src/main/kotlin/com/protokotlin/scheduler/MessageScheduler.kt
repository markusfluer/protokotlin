package com.protokotlin.scheduler

import com.protokotlin.model.*
import com.protokotlin.resolver.TypeRegistry
import com.protokotlin.generator.KotlinGenerator
import com.protokotlin.util.PackageUtils

/**
 * Schedules and organizes message generation across multiple proto files.
 * Collects all types first, then generates them in separate files.
 */
class MessageScheduler(
    private val basePackageName: String,
    private val typeRegistry: TypeRegistry,
    private val flatPackageStructure: Boolean = false
) {
    
    // Collected types from all proto files
    private val messages = mutableListOf<ScheduledMessage>()
    private val enums = mutableListOf<ScheduledEnum>()
    private val oneofs = mutableListOf<ScheduledOneof>()
    
    data class ScheduledMessage(
        val message: ProtoMessage,
        val packageName: String,
        val sourceFile: String
    )
    
    data class ScheduledEnum(
        val enum: ProtoEnum,
        val packageName: String,
        val sourceFile: String
    )
    
    data class ScheduledOneof(
        val oneof: ProtoOneof,
        val packageName: String,
        val sourceFile: String,
        val parentMessage: String
    )
    
    /**
     * Schedule all types from a proto file for generation
     */
    fun scheduleFile(protoFile: ProtoFile) {
        val kotlinPackage = PackageUtils.combinePackageNames(basePackageName, protoFile.packageName, flatPackageStructure)
        
        // Schedule top-level messages
        protoFile.messages.forEach { message ->
            scheduleMessage(message, kotlinPackage, protoFile.fileName)
        }
        
        // Schedule top-level enums
        protoFile.enums.forEach { enum ->
            enums.add(ScheduledEnum(enum, kotlinPackage, protoFile.fileName))
        }
    }
    
    private fun scheduleMessage(
        message: ProtoMessage, 
        packageName: String, 
        sourceFile: String,
        parentContext: String = ""
    ) {
        val messageContext = if (parentContext.isNotEmpty()) "$parentContext.${message.name}" else message.name
        
        // Schedule the main message
        messages.add(ScheduledMessage(message, packageName, sourceFile))
        
        // Schedule oneofs within this message
        message.oneofs.forEach { oneof ->
            oneofs.add(ScheduledOneof(oneof, packageName, sourceFile, messageContext))
        }
        
        // Schedule nested messages recursively
        message.nestedMessages.forEach { nestedMessage ->
            scheduleMessage(nestedMessage, packageName, sourceFile, messageContext)
        }
        
        // Schedule nested enums
        message.nestedEnums.forEach { nestedEnum ->
            enums.add(ScheduledEnum(nestedEnum, packageName, sourceFile))
        }
    }
    
    /**
     * Generate all scheduled types into separate files
     */
    fun generateAll(): Map<String, String> {
        val generatedFiles = mutableMapOf<String, String>()
        
        // Generate each message in its own file
        messages.forEach { scheduled ->
            val fileName = "${scheduled.message.name}.kt"
            val content = generateMessageFile(scheduled)
            generatedFiles[fileName] = content
        }
        
        // Generate each top-level enum in its own file (nested enums are included with their parent message)
        enums.filter { it.packageName == PackageUtils.combinePackageNames(basePackageName, PackageUtils.extractProtoPackage(it.packageName), flatPackageStructure) }
            .forEach { scheduled ->
                val fileName = "${scheduled.enum.name}.kt"
                val content = generateEnumFile(scheduled)
                generatedFiles[fileName] = content
            }
        
        return generatedFiles
    }
    
    private fun generateMessageFile(scheduled: ScheduledMessage): String {
        // Create a simplified proto file context for the generator
        val contextFile = ProtoFile(
            fileName = scheduled.sourceFile,
            packageName = PackageUtils.extractProtoPackage(scheduled.packageName),
            imports = emptyList(),
            messages = emptyList(), // Don't include other messages to avoid duplication
            enums = emptyList(),
            services = emptyList()
        )
        
        // Create generator for this specific message
        val generator = KotlinGenerator(scheduled.packageName, typeRegistry, flatPackageStructure)
        
        // Generate using the existing generator
        val fileSpec = generator.generateSingleMessageFile(scheduled.message, contextFile)
        return fileSpec.toString()
    }
    
    private fun generateEnumFile(scheduled: ScheduledEnum): String {
        // Create generator for this specific enum
        val generator = KotlinGenerator(scheduled.packageName, typeRegistry, flatPackageStructure)
        
        // Create a simplified proto file context
        val contextFile = ProtoFile(
            fileName = scheduled.sourceFile,
            packageName = PackageUtils.extractProtoPackage(scheduled.packageName),
            imports = emptyList(),
            messages = emptyList(),
            enums = listOf(scheduled.enum),
            services = emptyList()
        )
        
        val fileSpec = generator.generateSingleEnumFile(scheduled.enum, contextFile)
        return fileSpec.toString()
    }
    
    
}