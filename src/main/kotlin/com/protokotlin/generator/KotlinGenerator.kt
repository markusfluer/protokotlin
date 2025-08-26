package com.protokotlin.generator

import com.protokotlin.model.*
import com.protokotlin.resolver.TypeRegistry
import com.protokotlin.util.PackageUtils
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

class KotlinGenerator(
    private val packageName: String,
    private val typeRegistry: TypeRegistry? = null,
    private val flatPackageStructure: Boolean = false
) {
    
    private val protoBufSerializableAnnotation = ClassName("kotlinx.serialization", "Serializable")
    private val protoNumberAnnotation = ClassName("kotlinx.serialization.protobuf", "ProtoNumber")
    private val protoPackedAnnotation = ClassName("kotlinx.serialization.protobuf", "ProtoPacked")
    
    fun generate(protoFile: ProtoFile): Map<String, String> {
        val files = mutableMapOf<String, String>()
        
        protoFile.messages.forEach { message ->
            val fileSpec = generateMessageFile(message, protoFile)
            files["${message.name}.kt"] = fileSpec.toString()
        }
        
        protoFile.enums.forEach { enum ->
            val fileSpec = generateEnumFile(enum, protoFile)
            files["${enum.name}.kt"] = fileSpec.toString()
        }
        
        return files
    }
    
    private fun generateMessageFile(message: ProtoMessage, protoFile: ProtoFile): FileSpec {
        val fileBuilder = FileSpec.builder(packageName, message.name)
        
        val dataClass = generateDataClass(message, protoFile)
        fileBuilder.addType(dataClass)
        
        message.nestedMessages.forEach { nestedMessage ->
            val nestedClass = generateDataClass(nestedMessage, protoFile)
            fileBuilder.addType(nestedClass)
        }
        
        message.nestedEnums.forEach { nestedEnum ->
            val enumClass = generateEnum(nestedEnum)
            fileBuilder.addType(enumClass)
        }
        
        message.oneofs.forEach { oneof ->
            val oneofClass = generateOneof(oneof, protoFile)
            fileBuilder.addType(oneofClass)
        }
        
        return fileBuilder.build()
    }
    
    private fun generateEnumFile(enum: ProtoEnum, protoFile: ProtoFile): FileSpec {
        val fileBuilder = FileSpec.builder(packageName, enum.name)
        val enumClass = generateEnum(enum)
        fileBuilder.addType(enumClass)
        return fileBuilder.build()
    }
    
    fun generateDataClass(message: ProtoMessage, protoFile: ProtoFile): TypeSpec {
        // Check if message is empty (no fields and no oneofs)
        val isEmpty = message.fields.isEmpty() && message.oneofs.isEmpty()
        
        val classBuilder = if (isEmpty) {
            // Generate object for empty messages since data classes require at least one parameter
            TypeSpec.objectBuilder(message.name)
                .addAnnotation(protoBufSerializableAnnotation)
        } else {
            TypeSpec.classBuilder(message.name)
                .addModifiers(KModifier.DATA)
                .addAnnotation(protoBufSerializableAnnotation)
        }
        
        // Skip constructor setup for empty objects
        if (isEmpty) {
            return classBuilder.build()
        }
        
        val constructorBuilder = FunSpec.constructorBuilder()
        
        message.fields.forEach { field ->
            val propertyType = mapProtoTypeToKotlin(field.type, field.label, protoFile)
            val propertyName = toCamelCase(field.name)
            
            val parameterBuilder = ParameterSpec.builder(propertyName, propertyType)
            
            if (field.label == FieldLabel.REPEATED) {
                parameterBuilder.defaultValue("emptyList()")
            } else if (field.label == FieldLabel.OPTIONAL) {
                // Proto3: All fields are implicitly optional and should default to null
                parameterBuilder.defaultValue("null")
            }
            
            constructorBuilder.addParameter(parameterBuilder.build())
            
            val propertyBuilder = PropertySpec.builder(propertyName, propertyType)
                .initializer(propertyName)
                .addAnnotation(
                    AnnotationSpec.builder(protoNumberAnnotation)
                        .addMember("${field.number}")
                        .build()
                )
            
            // Add @ProtoPacked for repeated fields for proper decoding
            if (field.label == FieldLabel.REPEATED) {
                propertyBuilder.addAnnotation(protoPackedAnnotation)
            }
            
            classBuilder.addProperty(propertyBuilder.build())
        }
        
        // Add oneof fields as properties
        message.oneofs.forEach { oneof ->
            val propertyName = toCamelCase(oneof.name)
            val oneofTypeName = ClassName(packageName, toPascalCase(oneof.name))
            
            val parameterBuilder = ParameterSpec.builder(propertyName, oneofTypeName.copy(nullable = true))
                .defaultValue("null")
            
            constructorBuilder.addParameter(parameterBuilder.build())
            
            val propertyBuilder = PropertySpec.builder(propertyName, oneofTypeName.copy(nullable = true))
                .initializer(propertyName)
            
            classBuilder.addProperty(propertyBuilder.build())
        }
        
        classBuilder.primaryConstructor(constructorBuilder.build())
        
        return classBuilder.build()
    }
    
    private fun generateEnum(enum: ProtoEnum): TypeSpec {
        val enumBuilder = TypeSpec.enumBuilder(enum.name)
            .addAnnotation(protoBufSerializableAnnotation)
        
        enum.values.forEach { value ->
            enumBuilder.addEnumConstant(value.name)
        }
        
        return enumBuilder.build()
    }
    
    private fun mapProtoTypeToKotlin(type: ProtoType, label: FieldLabel, currentFile: ProtoFile? = null): TypeName {
        val baseType = when (type) {
            is ProtoType.Scalar -> mapScalarType(type.type)
            is ProtoType.Message -> resolveMessageType(type.name, currentFile)
            is ProtoType.Enum -> resolveEnumType(type.name, currentFile) 
            is ProtoType.Map -> {
                val keyType = mapScalarType(type.keyType)
                val valueType = mapProtoTypeToKotlin(type.valueType, FieldLabel.OPTIONAL, currentFile)
                MAP.parameterizedBy(keyType, valueType)
            }
        }
        
        return when (label) {
            FieldLabel.REPEATED -> LIST.parameterizedBy(baseType)
            FieldLabel.OPTIONAL -> {
                // Proto3: All optional fields should be nullable with null defaults
                baseType.copy(nullable = true)
            }
            FieldLabel.REQUIRED -> baseType
        }
    }
    
    private fun mapScalarType(scalarType: ScalarType): TypeName {
        return when (scalarType) {
            ScalarType.DOUBLE -> DOUBLE
            ScalarType.FLOAT -> FLOAT
            ScalarType.INT32, ScalarType.SINT32, ScalarType.SFIXED32 -> INT
            ScalarType.INT64, ScalarType.SINT64, ScalarType.SFIXED64 -> LONG
            ScalarType.UINT32, ScalarType.FIXED32 -> INT
            ScalarType.UINT64, ScalarType.FIXED64 -> LONG
            ScalarType.BOOL -> BOOLEAN
            ScalarType.STRING -> STRING
            ScalarType.BYTES -> ClassName("kotlin", "ByteArray")
        }
    }
    
    private fun toCamelCase(snakeCase: String): String {
        return snakeCase.split("_").mapIndexed { index, part ->
            if (index == 0) part else part.replaceFirstChar { it.uppercase() }
        }.joinToString("")
    }
    
    private fun toPascalCase(snakeCase: String): String {
        return snakeCase.split("_").joinToString("") { part ->
            part.replaceFirstChar { it.uppercase() }
        }
    }
    
    private fun resolveMessageType(typeName: String, protoFile: ProtoFile?): TypeName {
        // If we have a type registry, try to resolve the type
        if (typeRegistry != null && protoFile != null) {
            val resolvedType = typeRegistry.resolveType(typeName, protoFile.fileName, protoFile.packageName)
            if (resolvedType != null) {
                when (resolvedType) {
                    is TypeRegistry.ResolvedType.Message -> {
                        val className = typeRegistry.getKotlinClassName(resolvedType)
                        return resolveTypePackage(resolvedType, className)
                    }
                    is TypeRegistry.ResolvedType.Scalar -> {
                        // Handle well-known types mapped to scalars or special types
                        val scalarType = parseScalarType(resolvedType.name)
                        return if (scalarType != null) {
                            mapScalarType(scalarType)
                        } else {
                            // Handle special well-known types
                            mapWellKnownTypeToKotlin(resolvedType.name) ?: ClassName(packageName, typeName)
                        }
                    }
                    is TypeRegistry.ResolvedType.Enum -> {
                        // Handle if message type was actually an enum
                        val className = typeRegistry.getKotlinClassName(resolvedType)
                        return resolveTypePackage(resolvedType, className)
                    }
                }
            }
        }
        
        // Fallback to local resolution
        if (typeName.isBlank()) {
            throw IllegalArgumentException("Empty type name in resolveMessageType for file: ${protoFile?.fileName}")
        }
        return ClassName(packageName, typeName)
    }
    
    private fun resolveEnumType(typeName: String, protoFile: ProtoFile?): TypeName {
        // If we have a type registry, try to resolve the type
        if (typeRegistry != null && protoFile != null) {
            val resolvedType = typeRegistry.resolveType(typeName, protoFile.fileName, protoFile.packageName)
            if (resolvedType != null) {
                when (resolvedType) {
                    is TypeRegistry.ResolvedType.Enum -> {
                        val className = typeRegistry.getKotlinClassName(resolvedType)
                        return resolveTypePackage(resolvedType, className)
                    }
                    is TypeRegistry.ResolvedType.Scalar -> {
                        // Handle well-known types mapped to scalars or special types
                        val scalarType = parseScalarType(resolvedType.name)
                        return if (scalarType != null) {
                            mapScalarType(scalarType)
                        } else {
                            // Handle special well-known types
                            mapWellKnownTypeToKotlin(resolvedType.name) ?: ClassName(packageName, typeName)
                        }
                    }
                    is TypeRegistry.ResolvedType.Message -> {
                        // Handle if enum type was actually a message
                        val className = typeRegistry.getKotlinClassName(resolvedType)
                        return resolveTypePackage(resolvedType, className)
                    }
                }
            }
        }
        
        // Fallback to local resolution
        if (typeName.isBlank()) {
            throw IllegalArgumentException("Empty type name in resolveEnumType for file: ${protoFile?.fileName}")
        }
        return ClassName(packageName, typeName)
    }
    
    
    /**
     * Extract the base package name from the current package name
     */
    private fun getBasePackageName(): String {
        // The packageName might already contain proto package structure in legacy mode
        // For flat mode, it's just the base package
        // For legacy mode, we need to extract the base part
        return if (flatPackageStructure) {
            packageName
        } else {
            // In legacy mode, packageName might be "base.proto_package", we want just "base"
            val parts = packageName.split(".")
            // Find the first occurrence of a proto package pattern (contains underscore)
            val protoPackageStartIndex = parts.indexOfFirst { it.contains("_") }
            if (protoPackageStartIndex > 0) {
                parts.subList(0, protoPackageStartIndex).joinToString(".")
            } else {
                packageName
            }
        }
    }
    
    /**
     * Resolve the package for a type, respecting flatPackageStructure setting
     */
    private fun resolveTypePackage(resolvedType: TypeRegistry.ResolvedType, className: String): ClassName {
        return if (flatPackageStructure) {
            // All types in same package when using flat structure
            ClassName(packageName, className)
        } else {
            // Use the type's original package structure
            val protoPackage = typeRegistry!!.getKotlinPackage(resolvedType)
            val kotlinPackage = PackageUtils.combinePackageNames(getBasePackageName(), protoPackage, flatPackageStructure)
            ClassName(kotlinPackage, className)
        }
    }
    
    private fun parseScalarType(type: String): ScalarType? {
        return when (type) {
            "double" -> ScalarType.DOUBLE
            "float" -> ScalarType.FLOAT
            "int32" -> ScalarType.INT32
            "int64" -> ScalarType.INT64
            "uint32" -> ScalarType.UINT32
            "uint64" -> ScalarType.UINT64
            "sint32" -> ScalarType.SINT32
            "sint64" -> ScalarType.SINT64
            "fixed32" -> ScalarType.FIXED32
            "fixed64" -> ScalarType.FIXED64
            "sfixed32" -> ScalarType.SFIXED32
            "sfixed64" -> ScalarType.SFIXED64
            "bool" -> ScalarType.BOOL
            "string" -> ScalarType.STRING
            "bytes" -> ScalarType.BYTES
            else -> null
        }
    }
    
    private fun mapWellKnownTypeToKotlin(typeName: String): TypeName? {
        return when (typeName) {
            "kotlinx.datetime.Instant" -> ClassName("kotlinx.datetime", "Instant")
            "kotlin.time.Duration" -> ClassName("kotlin.time", "Duration")
            else -> null
        }
    }
    
    fun generateOneof(oneof: ProtoOneof, protoFile: ProtoFile): TypeSpec {
        val sealedClassName = toPascalCase(oneof.name)
        
        val sealedClassBuilder = TypeSpec.classBuilder(sealedClassName)
            .addModifiers(KModifier.SEALED)
            .addAnnotation(protoBufSerializableAnnotation)
        
        // Add nested data classes for each oneof option
        oneof.fields.forEach { field ->
            val optionClassName = toPascalCase(field.name)
            val fieldType = mapProtoTypeToKotlin(field.type, FieldLabel.OPTIONAL, protoFile)
            
            val optionClass = TypeSpec.classBuilder(optionClassName)
                .addModifiers(KModifier.DATA)
                .superclass(ClassName(packageName, sealedClassName))
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter("value", fieldType)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("value", fieldType)
                        .initializer("value")
                        .addAnnotation(
                            AnnotationSpec.builder(protoNumberAnnotation)
                                .addMember("${field.number}")
                                .build()
                        )
                        .build()
                )
                .build()
            
            sealedClassBuilder.addType(optionClass)
        }
        
        return sealedClassBuilder.build()
    }
    
    /**
     * Generate a single message file (for scheduler)
     * One class per file - no nested types included
     */
    fun generateSingleMessageFile(message: ProtoMessage, protoFile: ProtoFile): FileSpec {
        val fileBuilder = FileSpec.builder(packageName, message.name)
        
        // Generate only the main message class - no nested types
        val dataClass = generateDataClass(message, protoFile)
        fileBuilder.addType(dataClass)
        
        // DO NOT include nested messages - they get their own files
        // DO NOT include nested enums - they get their own files  
        // DO NOT include oneofs - they get their own files
        
        return fileBuilder.build()
    }
    
    /**
     * Generate a single enum file (for scheduler)
     */
    fun generateSingleEnumFile(enum: ProtoEnum, protoFile: ProtoFile): FileSpec {
        val fileBuilder = FileSpec.builder(packageName, enum.name)
        val enumClass = generateEnum(enum)
        fileBuilder.addType(enumClass)
        return fileBuilder.build()
    }
}