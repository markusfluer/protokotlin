package com.protokotlin.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

abstract class ProtoKotlinExtension {
    
    abstract val protoDir: DirectoryProperty
    
    abstract val outputDir: DirectoryProperty
    
    abstract val packageName: Property<String>
}