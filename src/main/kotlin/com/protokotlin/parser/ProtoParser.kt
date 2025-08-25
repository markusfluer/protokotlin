package com.protokotlin.parser

import com.protokotlin.model.*

class ProtoParser {
    
    fun parse(content: String, fileName: String): ProtoFile {
        val lines = content.lines()
        var packageName: String? = null
        val imports = mutableListOf<String>()
        val messages = mutableListOf<ProtoMessage>()
        val enums = mutableListOf<ProtoEnum>()
        val services = mutableListOf<ProtoService>()
        
        var currentIndex = 0
        while (currentIndex < lines.size) {
            val line = lines[currentIndex].trim()
            
            when {
                line.startsWith("syntax") -> {
                    currentIndex++
                }
                line.startsWith("package ") -> {
                    packageName = extractPackageName(line)
                    currentIndex++
                }
                line.startsWith("import ") -> {
                    imports.add(extractImport(line))
                    currentIndex++
                }
                line.startsWith("message ") -> {
                    val (message, nextIndex) = parseMessage(lines, currentIndex)
                    messages.add(message)
                    currentIndex = nextIndex
                }
                line.startsWith("enum ") -> {
                    val (enum, nextIndex) = parseEnum(lines, currentIndex)
                    enums.add(enum)
                    currentIndex = nextIndex
                }
                line.startsWith("service ") -> {
                    val (service, nextIndex) = parseService(lines, currentIndex)
                    services.add(service)
                    currentIndex = nextIndex
                }
                else -> currentIndex++
            }
        }
        
        return ProtoFile(
            fileName = fileName,
            packageName = packageName,
            imports = imports,
            messages = messages,
            enums = enums,
            services = services
        )
    }
    
    private fun extractPackageName(line: String): String {
        return line.substringAfter("package ")
            .substringBefore(";")
            .trim()
    }
    
    private fun extractImport(line: String): String {
        return line.substringAfter("import ")
            .substringBefore(";")
            .trim()
            .removeSurrounding("\"")
    }
    
    private fun parseMessage(lines: List<String>, startIndex: Int): Pair<ProtoMessage, Int> {
        val firstLine = lines[startIndex].trim()
        val messageName = firstLine.substringAfter("message ")
            .substringBefore("{")
            .trim()
        
        val fields = mutableListOf<ProtoField>()
        val nestedMessages = mutableListOf<ProtoMessage>()
        val nestedEnums = mutableListOf<ProtoEnum>()
        
        var currentIndex = startIndex + 1
        var braceCount = 1
        
        while (currentIndex < lines.size && braceCount > 0) {
            val line = lines[currentIndex].trim()
            
            when {
                line.contains("{") -> braceCount += line.count { it == '{' }
                line.contains("}") -> {
                    braceCount -= line.count { it == '}' }
                    if (braceCount == 0) {
                        currentIndex++
                        break
                    }
                }
                line.startsWith("message ") -> {
                    val (nestedMessage, nextIndex) = parseMessage(lines, currentIndex)
                    nestedMessages.add(nestedMessage)
                    currentIndex = nextIndex - 1
                }
                line.startsWith("enum ") -> {
                    val (nestedEnum, nextIndex) = parseEnum(lines, currentIndex)
                    nestedEnums.add(nestedEnum)
                    currentIndex = nextIndex - 1
                }
                line.matches(Regex("^(optional |required |repeated )?.*=.*")) -> {
                    parseField(line)?.let { fields.add(it) }
                }
            }
            currentIndex++
        }
        
        return ProtoMessage(
            name = messageName,
            fields = fields,
            nestedMessages = nestedMessages,
            nestedEnums = nestedEnums
        ) to currentIndex
    }
    
    private fun parseField(line: String): ProtoField? {
        val cleanLine = line.substringBefore("//").substringBefore(";").trim()
        if (cleanLine.isEmpty()) return null
        
        val label = when {
            cleanLine.startsWith("repeated ") -> FieldLabel.REPEATED
            cleanLine.startsWith("required ") -> FieldLabel.REQUIRED
            cleanLine.startsWith("optional ") -> FieldLabel.OPTIONAL
            else -> FieldLabel.OPTIONAL
        }
        
        val withoutLabel = cleanLine
            .removePrefix("repeated ")
            .removePrefix("required ")
            .removePrefix("optional ")
            .trim()
        
        val parts = withoutLabel.split(Regex("\\s+"))
        if (parts.size < 3) return null
        
        val typeStr = parts[0]
        val fieldName = parts[1]
        val numberStr = parts.last().substringAfter("=").trim()
        val fieldNumber = numberStr.toIntOrNull() ?: return null
        
        val type = parseType(typeStr)
        
        return ProtoField(
            name = fieldName,
            type = type,
            number = fieldNumber,
            label = label
        )
    }
    
    private fun parseType(typeStr: String): ProtoType {
        return when {
            typeStr.startsWith("map<") -> {
                val mapContent = typeStr.substringAfter("map<").substringBefore(">")
                val (keyType, valueType) = mapContent.split(",").map { it.trim() }
                ProtoType.Map(
                    keyType = parseScalarType(keyType) ?: ScalarType.STRING,
                    valueType = parseType(valueType)
                )
            }
            else -> {
                parseScalarType(typeStr)?.let { ProtoType.Scalar(it) }
                    ?: ProtoType.Message(typeStr)
            }
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
    
    private fun parseEnum(lines: List<String>, startIndex: Int): Pair<ProtoEnum, Int> {
        val firstLine = lines[startIndex].trim()
        val enumName = firstLine.substringAfter("enum ")
            .substringBefore("{")
            .trim()
        
        val values = mutableListOf<ProtoEnumValue>()
        var currentIndex = startIndex + 1
        
        while (currentIndex < lines.size) {
            val line = lines[currentIndex].trim()
            
            if (line.contains("}")) {
                currentIndex++
                break
            }
            
            if (line.matches(Regex("\\s*\\w+\\s*=\\s*\\d+.*"))) {
                val parts = line.substringBefore(";").split("=")
                if (parts.size == 2) {
                    val name = parts[0].trim()
                    val number = parts[1].trim().toIntOrNull() ?: 0
                    values.add(ProtoEnumValue(name, number))
                }
            }
            
            currentIndex++
        }
        
        return ProtoEnum(enumName, values) to currentIndex
    }
    
    private fun parseService(lines: List<String>, startIndex: Int): Pair<ProtoService, Int> {
        val firstLine = lines[startIndex].trim()
        val serviceName = firstLine.substringAfter("service ")
            .substringBefore("{")
            .trim()
        
        val methods = mutableListOf<ProtoMethod>()
        var currentIndex = startIndex + 1
        
        while (currentIndex < lines.size) {
            val line = lines[currentIndex].trim()
            
            if (line.contains("}")) {
                currentIndex++
                break
            }
            
            if (line.startsWith("rpc ")) {
                val methodName = line.substringAfter("rpc ")
                    .substringBefore("(")
                    .trim()
                
                val signature = line.substringAfter("(").substringBefore(")")
                val clientStreaming = signature.startsWith("stream ")
                val inputType = signature.removePrefix("stream ").trim()
                
                val returns = line.substringAfter("returns").substringAfter("(").substringBefore(")")
                val serverStreaming = returns.startsWith("stream ")
                val outputType = returns.removePrefix("stream ").trim()
                
                methods.add(ProtoMethod(
                    name = methodName,
                    inputType = inputType,
                    outputType = outputType,
                    clientStreaming = clientStreaming,
                    serverStreaming = serverStreaming
                ))
            }
            
            currentIndex++
        }
        
        return ProtoService(serviceName, methods) to currentIndex
    }
}