package com.protokotlin.generator

import com.protokotlin.model.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

class KotlinGenerator(private val packageName: String) {
    
    private val protoBufSerializableAnnotation = ClassName("kotlinx.serialization", "Serializable")
    private val protoNumberAnnotation = ClassName("kotlinx.serialization.protobuf", "ProtoNumber")
    private val protoPackedAnnotation = ClassName("kotlinx.serialization.protobuf", "ProtoPacked")
    
    fun generate(protoFile: ProtoFile): Map<String, String> {
        val files = mutableMapOf<String, String>()
        
        protoFile.messages.forEach { message ->
            val fileSpec = generateMessageFile(message)
            files["${message.name}.kt"] = fileSpec.toString()
        }
        
        protoFile.enums.forEach { enum ->
            val fileSpec = generateEnumFile(enum)
            files["${enum.name}.kt"] = fileSpec.toString()
        }
        
        return files
    }
    
    private fun generateMessageFile(message: ProtoMessage): FileSpec {
        val fileBuilder = FileSpec.builder(packageName, message.name)
        
        val dataClass = generateDataClass(message)
        fileBuilder.addType(dataClass)
        
        message.nestedMessages.forEach { nestedMessage ->
            val nestedClass = generateDataClass(nestedMessage)
            fileBuilder.addType(nestedClass)
        }
        
        message.nestedEnums.forEach { nestedEnum ->
            val enumClass = generateEnum(nestedEnum)
            fileBuilder.addType(enumClass)
        }
        
        return fileBuilder.build()
    }
    
    private fun generateEnumFile(enum: ProtoEnum): FileSpec {
        val fileBuilder = FileSpec.builder(packageName, enum.name)
        val enumClass = generateEnum(enum)
        fileBuilder.addType(enumClass)
        return fileBuilder.build()
    }
    
    private fun generateDataClass(message: ProtoMessage): TypeSpec {
        val classBuilder = TypeSpec.classBuilder(message.name)
            .addModifiers(KModifier.DATA)
            .addAnnotation(protoBufSerializableAnnotation)
        
        val constructorBuilder = FunSpec.constructorBuilder()
        
        message.fields.forEach { field ->
            val propertyType = mapProtoTypeToKotlin(field.type, field.label)
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
    
    private fun mapProtoTypeToKotlin(type: ProtoType, label: FieldLabel): TypeName {
        val baseType = when (type) {
            is ProtoType.Scalar -> mapScalarType(type.type)
            is ProtoType.Message -> ClassName(packageName, type.name)
            is ProtoType.Enum -> ClassName(packageName, type.name)
            is ProtoType.Map -> {
                val keyType = mapScalarType(type.keyType)
                val valueType = mapProtoTypeToKotlin(type.valueType, FieldLabel.OPTIONAL)
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
}