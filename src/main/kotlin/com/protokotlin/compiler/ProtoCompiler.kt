package com.protokotlin.compiler

import com.protokotlin.generator.KotlinGenerator
import com.protokotlin.model.ProtoFile
import com.protokotlin.parser.ProtoParser
import com.protokotlin.resolver.TypeRegistry
import com.protokotlin.scheduler.MessageScheduler
import java.io.File

/**
 * Orchestrates the compilation of proto files with import resolution
 */
class ProtoCompiler(
    private val basePackageName: String,
    private val protoSearchPaths: List<File> = listOf(File("."))
) {
    private val parser = ProtoParser()
    private val typeRegistry = TypeRegistry()
    private val parsedFiles = mutableMapOf<String, ProtoFile>()
    
    /**
     * Compile a set of proto files with import resolution
     */
    fun compile(protoFiles: List<File>): Map<String, String> {
        // First pass: Parse all files and register types
        val allFiles = collectAllRequiredFiles(protoFiles)
        parseAllFiles(allFiles)
        
        // Second pass: Generate code with resolved types
        return generateCode(protoFiles)
    }
    
    /**
     * Collect all required files including imports
     */
    private fun collectAllRequiredFiles(initialFiles: List<File>): Set<File> {
        val allFiles = mutableSetOf<File>()
        val toProcess = initialFiles.toMutableList()
        val processed = mutableSetOf<String>()
        
        while (toProcess.isNotEmpty()) {
            val file = toProcess.removeAt(0)
            val canonicalPath = file.canonicalPath
            
            if (canonicalPath in processed) continue
            processed.add(canonicalPath)
            
            allFiles.add(file)
            
            // Parse to get imports
            if (file.exists() && file.extension == "proto") {
                val content = file.readText()
                val protoFile = parser.parse(content, file.name)
                
                // Add imported files to process queue
                protoFile.imports.forEach { importPath ->
                    val importFile = findProtoFile(importPath)
                    if (importFile != null && importFile.canonicalPath !in processed) {
                        toProcess.add(importFile)
                    }
                }
            }
        }
        
        return allFiles
    }
    
    /**
     * Find a proto file in search paths
     */
    private fun findProtoFile(importPath: String): File? {
        // Skip Google well-known types for now (will handle separately)
        if (importPath.startsWith("google/protobuf/")) {
            return null
        }
        
        // Search in all search paths
        for (searchPath in protoSearchPaths) {
            val file = File(searchPath, importPath)
            if (file.exists()) {
                return file
            }
            
            // Also try relative to the current working directory
            val relativeFile = File(importPath)
            if (relativeFile.exists()) {
                return relativeFile
            }
        }
        
        return null
    }
    
    /**
     * Parse all files and register types
     */
    private fun parseAllFiles(files: Set<File>) {
        files.forEach { file ->
            if (file.exists() && file.extension == "proto") {
                val content = file.readText()
                val protoFile = parser.parse(content, file.path)
                parsedFiles[file.path] = protoFile
                typeRegistry.registerFile(protoFile)
            }
        }
    }
    
    /**
     * Generate code for the specified files (not imports) using scheduler
     */
    private fun generateCode(targetFiles: List<File>): Map<String, String> {
        // Create scheduler for organized generation
        val scheduler = MessageScheduler(basePackageName, typeRegistry)
        
        // Schedule all target files for generation
        targetFiles.forEach { file ->
            val protoFile = parsedFiles[file.path]
            if (protoFile != null) {
                scheduler.scheduleFile(protoFile)
            }
        }
        
        // Generate all scheduled types
        return scheduler.generateAll()
    }
    
    /**
     * Combine base package name with proto package
     */
    private fun combinePackageNames(basePackage: String, protoPackage: String?): String {
        return if (protoPackage != null) {
            "$basePackage.${protoPackage.replace(".", "_")}"
        } else {
            basePackage
        }
    }
}