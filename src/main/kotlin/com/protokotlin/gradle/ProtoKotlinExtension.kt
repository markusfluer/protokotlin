package com.protokotlin.gradle

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

abstract class ProtoKotlinExtension {
    
    abstract val protoDir: DirectoryProperty
    
    abstract val outputDir: DirectoryProperty
    
    abstract val packageName: Property<String>
    
    /**
     * Additional directories to search for imported proto files.
     * Similar to protoc's -I/--proto_path option.
     */
    abstract val protoPath: ConfigurableFileCollection
    
    /**
     * Whether to use a flat package structure.
     * If true, all generated files use the same packageName regardless of proto package structure.
     * If false, proto packages create nested Kotlin packages (default legacy behavior).
     */
    abstract val flatPackageStructure: Property<Boolean>
}