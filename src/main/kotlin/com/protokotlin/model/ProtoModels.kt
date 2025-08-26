package com.protokotlin.model

data class ProtoFile(
    val fileName: String,
    val packageName: String?,
    val imports: List<String>,
    val messages: List<ProtoMessage>,
    val enums: List<ProtoEnum>,
    val services: List<ProtoService>
)

data class ProtoMessage(
    val name: String,
    val fields: List<ProtoField>,
    val oneofs: List<ProtoOneof>,
    val nestedMessages: List<ProtoMessage>,
    val nestedEnums: List<ProtoEnum>
)

data class ProtoField(
    val name: String,
    val type: ProtoType,
    val number: Int,
    val label: FieldLabel = FieldLabel.OPTIONAL,
    val defaultValue: String? = null
)

data class ProtoOneof(
    val name: String,
    val fields: List<ProtoField>
)

sealed class ProtoType {
    data class Scalar(val type: ScalarType) : ProtoType()
    data class Message(val name: String) : ProtoType()
    data class Enum(val name: String) : ProtoType()
    data class Map(val keyType: ScalarType, val valueType: ProtoType) : ProtoType()
}

enum class ScalarType {
    DOUBLE, FLOAT, INT32, INT64, UINT32, UINT64,
    SINT32, SINT64, FIXED32, FIXED64, SFIXED32, SFIXED64,
    BOOL, STRING, BYTES
}

enum class FieldLabel {
    OPTIONAL, REQUIRED, REPEATED
}

data class ProtoEnum(
    val name: String,
    val values: List<ProtoEnumValue>
)

data class ProtoEnumValue(
    val name: String,
    val number: Int
)

data class ProtoService(
    val name: String,
    val methods: List<ProtoMethod>
)

data class ProtoMethod(
    val name: String,
    val inputType: String,
    val outputType: String,
    val clientStreaming: Boolean = false,
    val serverStreaming: Boolean = false
)