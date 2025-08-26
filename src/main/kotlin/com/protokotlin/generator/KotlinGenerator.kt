package com.protokotlin.generator

import com.protokotlin.model.*
import com.protokotlin.resolver.TypeRegistry
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

class KotlinGenerator(
    private val packageName: String,
    private val typeRegistry: TypeRegistry? = null
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
    
    private fun generateDataClass(message: ProtoMessage, protoFile: ProtoFile): TypeSpec {
        val classBuilder = TypeSpec.classBuilder(message.name)
            .addModifiers(KModifier.DATA)
            .addAnnotation(protoBufSerializableAnnotation)
        
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
                        val kotlinPackage = typeRegistry.getKotlinPackage(resolvedType)
                        val className = typeRegistry.getKotlinClassName(resolvedType)
                        return if (kotlinPackage != null && kotlinPackage != packageName) {
                            // Type from different package/file
                            ClassName(combinePackageNames(packageName.substringBeforeLast(".${protoFile.packageName?.replace(".", "_") ?: ""}"), kotlinPackage), className)
                        } else {
                            ClassName(packageName, className)
                        }
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
                        val kotlinPackage = typeRegistry.getKotlinPackage(resolvedType)
                        val className = typeRegistry.getKotlinClassName(resolvedType)
                        return if (kotlinPackage != null && kotlinPackage != packageName) {
                            ClassName(combinePackageNames(packageName.substringBeforeLast(".${protoFile.packageName?.replace(".", "_") ?: ""}"), kotlinPackage), className)
                        } else {
                            ClassName(packageName, className)
                        }
                    }
                }
            }
        }
        
        // Fallback to local resolution
        return ClassName(packageName, typeName)
    }
    
    private fun resolveEnumType(typeName: String, protoFile: ProtoFile?): TypeName {
        // If we have a type registry, try to resolve the type
        if (typeRegistry != null && protoFile != null) {
            val resolvedType = typeRegistry.resolveType(typeName, protoFile.fileName, protoFile.packageName)
            if (resolvedType != null) {
                when (resolvedType) {
                    is TypeRegistry.ResolvedType.Enum -> {
                        val kotlinPackage = typeRegistry.getKotlinPackage(resolvedType)
                        val className = typeRegistry.getKotlinClassName(resolvedType)
                        return if (kotlinPackage != null && kotlinPackage != packageName) {
                            // Type from different package/file
                            ClassName(combinePackageNames(packageName.substringBeforeLast(".${protoFile.packageName?.replace(".", "_") ?: ""}"), kotlinPackage), className)
                        } else {
                            ClassName(packageName, className)
                        }
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
                        val kotlinPackage = typeRegistry.getKotlinPackage(resolvedType)
                        val className = typeRegistry.getKotlinClassName(resolvedType)
                        return if (kotlinPackage != null && kotlinPackage != packageName) {
                            ClassName(combinePackageNames(packageName.substringBeforeLast(".${protoFile.packageName?.replace(".", "_") ?: ""}"), kotlinPackage), className)
                        } else {
                            ClassName(packageName, className)
                        }
                    }
                }
            }
        }
        
        // Fallback to local resolution
        return ClassName(packageName, typeName)
    }
    
    private fun combinePackageNames(basePackage: String, protoPackage: String?): String {
        return if (protoPackage != null) {
            "$basePackage.${protoPackage.replace(".", "_")}"
        } else {
            basePackage
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
    
    private fun generateOneof(oneof: ProtoOneof, protoFile: ProtoFile): TypeSpec {
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
     */
    fun generateSingleMessageFile(message: ProtoMessage, protoFile: ProtoFile): FileSpec {
        val fileBuilder = FileSpec.builder(packageName, message.name)
        
        val dataClass = generateDataClass(message, protoFile)
        fileBuilder.addType(dataClass)
        
        // Collect message types referenced by oneofs to avoid duplication
        val oneofReferencedMessages = message.oneofs.flatMap { oneof ->
            oneof.fields.mapNotNull { field ->
                when (field.type) {
                    is ProtoType.Message -> field.type.name
                    else -> null
                }
            }
        }.toSet()
        
        // Add nested messages (but skip those referenced by oneofs - they should be separate files)
        message.nestedMessages.forEach { nestedMessage ->
            if (nestedMessage.name !in oneofReferencedMessages) {
                val nestedClass = generateDataClass(nestedMessage, protoFile)
                fileBuilder.addType(nestedClass)
            }
        }
        
        // Add nested enums (these are always included as they're typically small)
        message.nestedEnums.forEach { nestedEnum ->
            val enumClass = generateEnum(nestedEnum)
            fileBuilder.addType(enumClass)
        }
        
        // Add oneofs as sealed classes
        message.oneofs.forEach { oneof ->
            val oneofClass = generateOneof(oneof, protoFile)
            fileBuilder.addType(oneofClass)
        }
        
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