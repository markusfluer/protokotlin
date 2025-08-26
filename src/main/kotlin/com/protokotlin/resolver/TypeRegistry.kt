package com.protokotlin.resolver

import com.protokotlin.model.*

/**
 * Registry for managing types across multiple proto files.
 * Handles type resolution, import tracking, and cross-file references.
 */
class TypeRegistry {
    // Map of fully qualified type name to its definition
    private val messages = mutableMapOf<String, MessageTypeInfo>()
    private val enums = mutableMapOf<String, EnumTypeInfo>()
    
    // Map of file path to its parsed content
    private val files = mutableMapOf<String, ProtoFile>()
    
    // Map of file path to its dependencies (imports)
    private val fileDependencies = mutableMapOf<String, Set<String>>()
    
    data class MessageTypeInfo(
        val message: ProtoMessage,
        val packageName: String?,
        val file: String,
        val parentMessage: String? = null  // For nested messages
    )
    
    data class EnumTypeInfo(
        val enum: ProtoEnum,
        val packageName: String?,
        val file: String,
        val parentMessage: String? = null  // For nested enums
    )
    
    /**
     * Register a proto file and all its types
     */
    fun registerFile(file: ProtoFile) {
        files[file.fileName] = file
        fileDependencies[file.fileName] = file.imports.toSet()
        
        // Register all top-level messages and enums
        file.messages.forEach { message ->
            registerMessage(message, file.packageName, file.fileName)
        }
        
        file.enums.forEach { enum ->
            registerEnum(enum, file.packageName, file.fileName)
        }
    }
    
    private fun registerMessage(
        message: ProtoMessage, 
        packageName: String?, 
        fileName: String,
        parentMessage: String? = null
    ) {
        val qualifiedName = buildQualifiedName(packageName, parentMessage, message.name)
        messages[qualifiedName] = MessageTypeInfo(message, packageName, fileName, parentMessage)
        
        // Register nested messages
        message.nestedMessages.forEach { nested ->
            val nestedParent = if (parentMessage != null) "$parentMessage.${message.name}" else message.name
            registerMessage(nested, packageName, fileName, nestedParent)
        }
        
        // Register nested enums
        message.nestedEnums.forEach { nested ->
            val nestedParent = if (parentMessage != null) "$parentMessage.${message.name}" else message.name
            registerEnum(nested, packageName, fileName, nestedParent)
        }
    }
    
    private fun registerEnum(
        enum: ProtoEnum,
        packageName: String?,
        fileName: String,
        parentMessage: String? = null
    ) {
        val qualifiedName = buildQualifiedName(packageName, parentMessage, enum.name)
        enums[qualifiedName] = EnumTypeInfo(enum, packageName, fileName, parentMessage)
    }
    
    /**
     * Resolve a type reference from a given context (file and package)
     */
    fun resolveType(typeName: String, fromFile: String, fromPackage: String?): ResolvedType? {
        // Try to resolve in order:
        // 1. Google well-known types
        // 2. Current file's types (including nested)
        // 3. Current package's types
        // 4. Imported files' types
        // 5. Fully qualified name
        
        // Check for Google well-known types first
        val wellKnownType = mapWellKnownType(typeName)
        if (wellKnownType != null) {
            return ResolvedType.Scalar(wellKnownType)
        }
        
        val currentFile = files[fromFile] ?: return null
        
        // Check if it's a scalar type
        if (isScalarType(typeName)) {
            return ResolvedType.Scalar(typeName)
        }
        
        // Try local resolution first (same file)
        val localType = resolveLocalType(typeName, currentFile, fromPackage)
        if (localType != null) return localType
        
        // Try resolving from imports
        for (import in currentFile.imports) {
            val importedType = resolveImportedType(typeName, import)
            if (importedType != null) return importedType
        }
        
        // Try as fully qualified name
        return resolveFullyQualifiedType(typeName)
    }
    
    private fun resolveLocalType(typeName: String, file: ProtoFile, packageName: String?): ResolvedType? {
        // First try with package prefix (this searches across ALL files in the same package)
        val withPackage = buildQualifiedName(packageName, null, typeName)
        
        messages[withPackage]?.let { return ResolvedType.Message(it) }
        enums[withPackage]?.let { return ResolvedType.Enum(it) }
        
        // Try without package (for nested types or unqualified names)
        messages[typeName]?.let { return ResolvedType.Message(it) }
        enums[typeName]?.let { return ResolvedType.Enum(it) }
        
        return null
    }
    
    private fun resolveImportedType(typeName: String, importPath: String): ResolvedType? {
        val importedFile = files.values.find { it.fileName == importPath || it.fileName.endsWith(importPath) }
            ?: return null
        
        val packageName = importedFile.packageName
        val qualifiedName = buildQualifiedName(packageName, null, typeName)
        
        messages[qualifiedName]?.let { return ResolvedType.Message(it) }
        enums[qualifiedName]?.let { return ResolvedType.Enum(it) }
        
        return null
    }
    
    private fun resolveFullyQualifiedType(typeName: String): ResolvedType? {
        messages[typeName]?.let { return ResolvedType.Message(it) }
        enums[typeName]?.let { return ResolvedType.Enum(it) }
        return null
    }
    
    /**
     * Get all files that need to be processed together (following imports)
     */
    fun getAllDependencies(fileName: String): Set<String> {
        val visited = mutableSetOf<String>()
        val queue = mutableListOf(fileName)
        
        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            if (current in visited) continue
            
            visited.add(current)
            fileDependencies[current]?.forEach { dep ->
                if (dep !in visited) queue.add(dep)
            }
        }
        
        return visited
    }
    
    /**
     * Get the Kotlin package name for a type
     * Note: This returns the proto package name. The actual Kotlin package
     * should be computed by the caller considering flatPackageStructure and basePackage.
     */
    fun getKotlinPackage(type: ResolvedType): String? {
        return when (type) {
            is ResolvedType.Message -> type.info.packageName
            is ResolvedType.Enum -> type.info.packageName
            is ResolvedType.Scalar -> null
        }
    }
    
    /**
     * Get the Kotlin class name for a type (handling nested types)
     */
    fun getKotlinClassName(type: ResolvedType): String {
        return when (type) {
            is ResolvedType.Message -> {
                if (type.info.parentMessage != null) {
                    "${type.info.parentMessage.replace(".", "_")}_${type.info.message.name}"
                } else {
                    type.info.message.name
                }
            }
            is ResolvedType.Enum -> {
                if (type.info.parentMessage != null) {
                    "${type.info.parentMessage.replace(".", "_")}_${type.info.enum.name}"
                } else {
                    type.info.enum.name
                }
            }
            is ResolvedType.Scalar -> type.name
        }
    }
    
    private fun buildQualifiedName(packageName: String?, parentMessage: String?, typeName: String): String {
        val parts = mutableListOf<String>()
        packageName?.let { parts.add(it) }
        parentMessage?.let { parts.add(it) }
        parts.add(typeName)
        return parts.joinToString(".")
    }
    
    private fun isScalarType(typeName: String): Boolean {
        return typeName in listOf(
            "double", "float", "int32", "int64", "uint32", "uint64",
            "sint32", "sint64", "fixed32", "fixed64", "sfixed32", "sfixed64",
            "bool", "string", "bytes"
        )
    }
    
    /**
     * Map Google well-known types to Kotlin types
     */
    private fun mapWellKnownType(typeName: String): String? {
        return when (typeName) {
            "google.protobuf.Timestamp" -> "kotlinx.datetime.Instant"
            "google.protobuf.Duration" -> "kotlin.time.Duration"
            "google.protobuf.Any" -> "string"        // For now, map to String
            "google.protobuf.Empty" -> null          // Empty should be Unit but we'll handle separately
            "google.protobuf.StringValue" -> "string"
            "google.protobuf.Int32Value" -> "int32"
            "google.protobuf.Int64Value" -> "int64"
            "google.protobuf.UInt32Value" -> "uint32"
            "google.protobuf.UInt64Value" -> "uint64"
            "google.protobuf.BoolValue" -> "bool"
            "google.protobuf.FloatValue" -> "float"
            "google.protobuf.DoubleValue" -> "double"
            "google.protobuf.BytesValue" -> "bytes"
            else -> null
        }
    }
    
    sealed class ResolvedType {
        data class Message(val info: MessageTypeInfo) : ResolvedType()
        data class Enum(val info: EnumTypeInfo) : ResolvedType()
        data class Scalar(val name: String) : ResolvedType()
    }
}