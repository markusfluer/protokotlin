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
}