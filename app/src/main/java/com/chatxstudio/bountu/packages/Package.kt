package com.chatxstudio.bountu.packages

import java.io.Serializable

/**
 * Represents a software package in the Bountu package management system
 */
data class Package(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val longDescription: String,
    val category: PackageCategory,
    val size: Long, // Size in bytes
    val dependencies: List<String> = emptyList(),
    val conflicts: List<String> = emptyList(),
    val maintainer: String = "SN-Mrdatobg",
    val homepage: String = "",
    val license: String = "GPL-3.0",
    val architecture: Architecture = Architecture.ALL,
    val platform: Platform = Platform.BOTH,
    val installScript: String = "",
    val uninstallScript: String = "",
    val binaryUrl: String = "",
    val checksumSHA256: String = "",
    val isInstalled: Boolean = false,
    val installedVersion: String? = null,
    val needsUpdate: Boolean = false,
    val needsMaintenance: Boolean = false,
    val maintenanceReason: String = "",
    val tags: List<String> = emptyList(),
    val screenshots: List<String> = emptyList(),
    val changelog: String = ""
) : Serializable

/**
 * Package categories similar to Ubuntu/Debian
 */
enum class PackageCategory(val displayName: String) {
    SYSTEM("System Utilities"),
    NETWORK("Network Tools"),
    DEVELOPMENT("Development"),
    EDITORS("Text Editors"),
    SHELLS("Shells"),
    COMPRESSION("Compression Tools"),
    SECURITY("Security Tools"),
    DATABASE("Databases"),
    WEB("Web Servers"),
    PROGRAMMING("Programming Languages"),
    VERSION_CONTROL("Version Control"),
    MULTIMEDIA("Multimedia"),
    DOCUMENTATION("Documentation"),
    LIBRARIES("Libraries"),
    UTILITIES("Utilities"),
    GAMES("Games"),
    EDUCATION("Education"),
    SCIENCE("Science & Math")
}

/**
 * Supported architectures
 */
enum class Architecture(val displayName: String) {
    ARM("ARM"),
    ARM64("ARM64"),
    X86("x86"),
    X86_64("x86_64"),
    ALL("All Architectures")
}

/**
 * Target platforms
 */
enum class Platform(val displayName: String) {
    ANDROID("Android"),
    WINDOWS("Windows"),
    BOTH("Android & Windows")
}

/**
 * Package installation status
 */
enum class InstallationStatus {
    NOT_INSTALLED,
    INSTALLING,
    INSTALLED,
    UPDATING,
    UNINSTALLING,
    FAILED,
    NEEDS_UPDATE,
    NEEDS_MAINTENANCE
}

/**
 * Package repository information
 */
data class Repository(
    val id: String,
    val name: String,
    val url: String,
    val description: String,
    val enabled: Boolean = true,
    val priority: Int = 100,
    val gpgKey: String = ""
)

/**
 * Package installation result
 */
sealed class InstallationResult {
    data class Success(val message: String) : InstallationResult()
    data class Failure(val error: String, val details: String = "") : InstallationResult()
    data class RequiresDependencies(val dependencies: List<String>) : InstallationResult()
    data class HasConflicts(val conflicts: List<String>) : InstallationResult()
}

/**
 * Package search filter
 */
data class PackageFilter(
    val query: String = "",
    val category: PackageCategory? = null,
    val platform: Platform? = null,
    val installedOnly: Boolean = false,
    val updatesOnly: Boolean = false,
    val maintenanceOnly: Boolean = false
)
