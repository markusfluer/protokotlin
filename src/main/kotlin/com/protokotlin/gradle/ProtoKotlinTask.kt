package com.protokotlin.gradle

import com.protokotlin.compiler.ProtoCompiler
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
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
    
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val protoPath: ConfigurableFileCollection
    
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
        
        // Build proto search paths: proto directory + additional paths
        val searchPaths = mutableListOf<File>()
        searchPaths.add(protoDirFile)
        searchPaths.addAll(protoPath.files)
        
        logger.info("ProtoKotlin: Proto search paths: ${searchPaths.map { it.absolutePath }}")
        
        // Use ProtoCompiler for full feature support including well-known types
        val compiler = ProtoCompiler(pkg, searchPaths)
        
        try {
            val generatedFiles = compiler.compile(protoFiles)
            
            generatedFiles.forEach { (fileName, content) ->
                val outputFile = File(outputDirFile, fileName)
                outputFile.parentFile?.mkdirs()
                outputFile.writeText(content)
                logger.info("Generated: ${outputFile.relativeTo(project.projectDir)}")
            }
            
            logger.lifecycle("ProtoKotlin: Successfully generated ${generatedFiles.size} Kotlin file(s) from ${protoFiles.size} proto file(s)")
        } catch (e: Exception) {
            throw org.gradle.api.GradleException("Failed to process proto files: ${e.message}", e)
        }
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