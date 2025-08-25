package com.protokotlin.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class ProtoKotlinPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        // Create the extension
        val extension = project.extensions.create("protokotlin", ProtoKotlinExtension::class.java)
        
        // Set default values
        extension.protoDir.convention(project.layout.projectDirectory.dir("src/main/proto"))
        extension.outputDir.convention(project.layout.buildDirectory.dir("generated/source/protokotlin"))
        extension.packageName.convention("generated")
        
        // Register the task
        val generateTask = project.tasks.register("generateProtoKotlin", ProtoKotlinTask::class.java) { task ->
            task.protoDir.set(extension.protoDir)
            task.outputDir.set(extension.outputDir)
            task.packageName.set(extension.packageName)
        }
        
        // Add generated sources to Kotlin source sets if Kotlin plugin is applied
        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            val sourceSets = project.extensions.getByName("sourceSets") as org.gradle.api.tasks.SourceSetContainer
            sourceSets.getByName("main").java.srcDir(generateTask.map { it.outputDir })
        }
        
        // Make compileKotlin depend on generateProtoKotlin
        project.tasks.matching { it.name == "compileKotlin" }.configureEach { 
            it.dependsOn(generateTask)
        }
        
        project.logger.info("ProtoKotlin plugin applied to project ${project.name}")
    }
}