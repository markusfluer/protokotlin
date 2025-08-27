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
        
        // Schedule EVERY message as a separate file (one class per file)
        messages.add(ScheduledMessage(message, packageName, sourceFile))
        
        // Schedule oneofs within this message as separate files
        message.oneofs.forEach { oneof ->
            oneofs.add(ScheduledOneof(oneof, packageName, sourceFile, messageContext))
        }
        
        // Schedule nested messages recursively as separate files
        message.nestedMessages.forEach { nestedMessage ->
            scheduleMessage(nestedMessage, packageName, sourceFile, messageContext)
        }
        
        // Schedule nested enums as separate files (one class per file)
        message.nestedEnums.forEach { nestedEnum ->
            enums.add(ScheduledEnum(nestedEnum, packageName, sourceFile))
        }
    }
    
    /**
     * Generate all scheduled types into separate files
     * All messages go into ProtoMessages.kt, other types get individual files
     */
    fun generateAll(): Map<String, String> {
        val generatedFiles = mutableMapOf<String, String>()
        
        // Generate ALL messages in a single ProtoMessages.kt file
        if (messages.isNotEmpty()) {
            val content = generateAllMessagesFile()
            generatedFiles["ProtoMessages.kt"] = content
        }
        
        // Generate each enum in its own file
        enums.forEach { scheduled ->
            val fileName = "${scheduled.enum.name}.kt"
            val content = generateEnumFile(scheduled)
            generatedFiles[fileName] = content
        }
        
        // Generate each oneof in its own file
        oneofs.forEach { scheduled ->
            val fileName = "${toPascalCase(scheduled.oneof.name)}.kt"
            val content = generateOneofFile(scheduled)
            generatedFiles[fileName] = content
        }
        
        return generatedFiles
    }
    
    private fun generateAllMessagesFile(): String {
        // Use the appropriate package name based on flatPackageStructure setting
        val packageName = if (flatPackageStructure) {
            // All messages use the same base package  
            basePackageName
        } else {
            // Use the first message's package (they should all be the same in this case)
            messages.firstOrNull()?.packageName ?: basePackageName
        }
        
        // Create a generator for all messages
        val generator = KotlinGenerator(packageName, typeRegistry, flatPackageStructure)
        
        // Create a file that contains all messages
        val fileBuilder = com.squareup.kotlinpoet.FileSpec.builder(packageName, "ProtoMessages")
        
        // First, check if we need to include well-known types
        val referencedWellKnownTypes = findReferencedWellKnownTypes()
        
        // Add well-known types if referenced
        referencedWellKnownTypes.forEach { wellKnownTypeName ->
            val resolvedType = typeRegistry.resolveType(wellKnownTypeName, "", "")
            if (resolvedType is TypeRegistry.ResolvedType.Message) {
                val contextFile = ProtoFile(
                    fileName = resolvedType.info.file,
                    packageName = resolvedType.info.packageName,
                    imports = emptyList(),
                    messages = emptyList(),
                    enums = emptyList(),
                    services = emptyList()
                )
                
                val wellKnownClass = generator.generateDataClass(resolvedType.info.message, contextFile)
                fileBuilder.addType(wellKnownClass)
            }
        }
        
        // Add all user-defined messages to the single file
        messages.forEach { scheduled ->
            val contextFile = ProtoFile(
                fileName = scheduled.sourceFile,
                packageName = PackageUtils.extractProtoPackage(scheduled.packageName),
                imports = emptyList(),
                messages = emptyList(),
                enums = emptyList(),
                services = emptyList()
            )
            
            // Generate only the message class (no nested types)
            val messageClass = generator.generateDataClass(scheduled.message, contextFile)
            fileBuilder.addType(messageClass)
        }
        
        return fileBuilder.build().toString()
    }
    
    /**
     * Find all well-known types referenced by scheduled messages
     */
    private fun findReferencedWellKnownTypes(): Set<String> {
        val referencedTypes = mutableSetOf<String>()
        
        messages.forEach { scheduled ->
            findReferencedWellKnownTypesInMessage(scheduled.message, referencedTypes)
        }
        
        return referencedTypes
    }
    
    /**
     * Recursively find well-known types referenced in a message
     */
    private fun findReferencedWellKnownTypesInMessage(message: ProtoMessage, referencedTypes: MutableSet<String>) {
        message.fields.forEach { field ->
            when (field.type) {
                is ProtoType.Message -> {
                    if (field.type.name.startsWith("google.protobuf.")) {
                        referencedTypes.add(field.type.name)
                    }
                }
                is ProtoType.Map -> {
                    if (field.type.valueType is ProtoType.Message) {
                        val messageType = field.type.valueType as ProtoType.Message
                        if (messageType.name.startsWith("google.protobuf.")) {
                            referencedTypes.add(messageType.name)
                        }
                    }
                }
                else -> { /* ignore scalars and enums */ }
            }
        }
        
        // Check nested messages recursively
        message.nestedMessages.forEach { nestedMessage ->
            findReferencedWellKnownTypesInMessage(nestedMessage, referencedTypes)
        }
        
        // Check oneof fields
        message.oneofs.forEach { oneof ->
            oneof.fields.forEach { field ->
                when (field.type) {
                    is ProtoType.Message -> {
                        if (field.type.name.startsWith("google.protobuf.")) {
                            referencedTypes.add(field.type.name)
                        }
                    }
                    else -> { /* ignore scalars and enums */ }
                }
            }
        }
    }
    
    private fun generateOneofFile(scheduled: ScheduledOneof): String {
        val generator = KotlinGenerator(scheduled.packageName, typeRegistry, flatPackageStructure)
        
        val contextFile = ProtoFile(
            fileName = scheduled.sourceFile,
            packageName = PackageUtils.extractProtoPackage(scheduled.packageName),
            imports = emptyList(),
            messages = emptyList(),
            enums = emptyList(),
            services = emptyList()
        )
        
        val fileBuilder = com.squareup.kotlinpoet.FileSpec.builder(scheduled.packageName, toPascalCase(scheduled.oneof.name))
        val oneofClass = generator.generateOneof(scheduled.oneof, contextFile)
        fileBuilder.addType(oneofClass)
        
        return fileBuilder.build().toString()
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
    
    private fun toPascalCase(snakeCase: String): String {
        return snakeCase.split("_").joinToString("") { part ->
            part.replaceFirstChar { it.uppercase() }
        }
    }
    
}