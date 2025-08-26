package com.protokotlin.util

/**
 * Utilities for handling package name resolution consistently across the codebase
 */
object PackageUtils {
    
    /**
     * Combine base package name with proto package name, respecting flatPackageStructure setting
     */
    fun combinePackageNames(
        basePackage: String, 
        protoPackage: String?, 
        flatPackageStructure: Boolean
    ): String {
        return if (flatPackageStructure || protoPackage == null) {
            // Use flat structure - all types go in the same package
            basePackage
        } else {
            // Legacy behavior - create nested packages from proto structure
            "$basePackage.${protoPackage.replace(".", "_")}"
        }
    }
    
    /**
     * Extract proto package name from a combined Kotlin package name
     */
    fun extractProtoPackage(kotlinPackage: String): String? {
        val parts = kotlinPackage.split(".")
        return if (parts.size > 2) {
            parts.drop(2).joinToString("_").replace("_", ".")
        } else {
            null
        }
    }
}