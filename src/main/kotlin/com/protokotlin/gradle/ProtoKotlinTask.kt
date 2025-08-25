package com.protokotlin.gradle

import com.protokotlin.generator.KotlinGenerator
import com.protokotlin.parser.ProtoParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

@CacheableTask
abstract class ProtoKotlinTask : DefaultTask() {
    
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val protoDir: DirectoryProperty
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @get:Input
    abstract val packageName: Property<String>
    
    init {
        description = "Generate Kotlin DTOs from Protocol Buffer files"
        group = "protokotlin"
        
        // Set default values
        packageName.convention("generated")
    }
    
    @TaskAction
    fun execute() {
        val protoDirFile = protoDir.get().asFile
        val outputDirFile = outputDir.get().asFile
        val pkg = packageName.get()
        
        logger.info("ProtoKotlin: Processing proto files from ${protoDirFile.absolutePath}")
        logger.info("ProtoKotlin: Output directory: ${outputDirFile.absolutePath}")
        logger.info("ProtoKotlin: Package name: $pkg")
        
        // Find all .proto files
        val protoFiles = findProtoFiles(protoDirFile)
        
        if (protoFiles.isEmpty()) {
            logger.warn("No .proto files found in ${protoDirFile.absolutePath}")
            return
        }
        
        logger.info("ProtoKotlin: Found ${protoFiles.size} proto file(s)")
        
        // Ensure output directory exists
        if (!outputDirFile.exists()) {
            outputDirFile.mkdirs()
        }
        
        val parser = ProtoParser()
        val generator = KotlinGenerator(pkg)
        var totalGeneratedFiles = 0
        
        protoFiles.forEach { protoFile ->
            logger.info("Processing ${protoFile.name}")
            
            try {
                val protoContent = protoFile.readText()
                val parsedProto = parser.parse(protoContent, protoFile.name)
                val generatedFiles = generator.generate(parsedProto)
                
                generatedFiles.forEach { (fileName, content) ->
                    val outputFile = File(outputDirFile, fileName)
                    outputFile.parentFile?.mkdirs()
                    outputFile.writeText(content)
                    logger.info("Generated: ${outputFile.relativeTo(project.projectDir)}")
                }
                
                totalGeneratedFiles += generatedFiles.size
            } catch (e: Exception) {
                throw org.gradle.api.GradleException("Failed to process ${protoFile.name}: ${e.message}", e)
            }
        }
        
        logger.lifecycle("ProtoKotlin: Successfully generated $totalGeneratedFiles Kotlin file(s) from ${protoFiles.size} proto file(s)")
    }
    
    private fun findProtoFiles(directory: File): List<File> {
        if (!directory.exists() || !directory.isDirectory) {
            return emptyList()
        }
        
        return directory.walkTopDown()
            .filter { it.isFile && it.extension == "proto" }
            .toList()
    }
}