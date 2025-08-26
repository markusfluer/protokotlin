package com.protokotlin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.protokotlin.compiler.ProtoCompiler
import com.protokotlin.generator.KotlinGenerator
import com.protokotlin.parser.ProtoParser
import java.io.File

class ProtoKotlinCli : CliktCommand(
    name = "protokotlin",
    help = "Compile Protocol Buffer files to Kotlin DTOs compatible with kotlinx.serialization.protobuf"
) {
    private val inputFile by argument(
        name = "input",
        help = "Path to the .proto file (optional if --dir is used)"
    ).file(mustExist = true, canBeDir = false).optional()
    
    private val inputDir by option(
        "--dir", "-d",
        help = "Directory containing .proto files to process"
    ).file(mustExist = true, canBeDir = true)
    
    private val protoPath by option(
        "--proto-path", "-I",
        help = "Add a directory to the proto import path (can be used multiple times)"
    ).file(mustExist = true, canBeDir = true).multiple()
    
    private val outputDir by option(
        "-o", "--output",
        help = "Output directory for generated Kotlin files"
    ).file(canBeDir = true).default(File("."))
    
    private val packageName by option(
        "-p", "--package",
        help = "Package name for generated Kotlin files"
    ).default("generated")
    
    override fun run() {
        // Validate arguments
        if (inputFile == null && inputDir == null) {
            echo("Error: Must specify either an input file or --dir option", err = true)
            return
        }
        
        if (inputFile != null && inputDir != null) {
            echo("Error: Cannot specify both input file and --dir option", err = true)
            return
        }
        
        try {
            val protoFiles = when {
                inputFile != null -> listOf(inputFile!!)
                inputDir != null -> findProtoFiles(inputDir!!)
                else -> emptyList()
            }
            
            if (protoFiles.isEmpty()) {
                echo("No .proto files found to process", err = true)
                return
            }
            
            echo("Processing ${protoFiles.size} file(s)...")
            
            // Setup proto search paths
            val searchPaths = mutableListOf<File>()
            
            // Add explicit proto paths
            searchPaths.addAll(protoPath)
            
            // Add directory of input files as search path
            if (inputFile != null) {
                inputFile!!.parentFile?.let { searchPaths.add(it) }
            }
            if (inputDir != null) {
                searchPaths.add(inputDir!!)
            }
            
            // Add current directory as fallback
            searchPaths.add(File("."))
            
            // Use the new ProtoCompiler for multi-file support
            val compiler = ProtoCompiler(packageName, searchPaths)
            val generatedFiles = compiler.compile(protoFiles)
            
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            var totalGeneratedFiles = 0
            generatedFiles.forEach { (fileName, content) ->
                val outputFile = File(outputDir, fileName)
                outputFile.parentFile?.mkdirs()
                outputFile.writeText(content)
                echo("Generated: ${outputFile.path}")
                totalGeneratedFiles++
            }
            
            echo("✓ Successfully generated $totalGeneratedFiles file(s) from ${protoFiles.size} .proto file(s)")
        } catch (e: Exception) {
            echo("Error: ${e.message}", err = true)
            throw e
        }
    }
    
    private fun findProtoFiles(directory: File): List<File> {
        return directory.walkTopDown()
            .filter { it.isFile && it.extension == "proto" }
            .toList()
    }
}

fun main(args: Array<String>) = ProtoKotlinCli().main(args)