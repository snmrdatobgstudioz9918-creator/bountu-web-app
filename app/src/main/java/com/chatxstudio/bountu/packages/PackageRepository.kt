package com.chatxstudio.bountu.packages

/**
 * Package repository containing prebuilt packages optimized for Android and Windows
 * Similar to Ubuntu's APT repository system
 */
object PackageRepository {
    
    /**
     * Get all available packages
     * DEPRECATED: Use GitPackageManager to fetch real packages from repository
     */
    @Deprecated("Use GitPackageManager.listPackages() instead")
    fun getAllPackages(): List<Package> {
        // Return empty list - all packages should come from Git repository
        return emptyList()
    }
    
    // System Utilities
    private fun createBusyBoxPackage() = Package(
        id = "busybox",
        name = "BusyBox",
        version = "1.36.1",
        description = "Swiss Army knife of embedded Linux",
        longDescription = "BusyBox combines tiny versions of many common UNIX utilities into a single small executable. " +
                "It provides replacements for most of the utilities you usually find in GNU fileutils, shellutils, etc. " +
                "Optimized for Android and Windows environments.",
        category = PackageCategory.SYSTEM,
        size = 2_500_000, // 2.5 MB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-2.0",
        tags = listOf("system", "utilities", "essential", "unix"),
        isInstalled = true,
        installedVersion = "1.36.1"
    )
    
    private fun createCoreUtilsPackage() = Package(
        id = "coreutils",
        name = "GNU Core Utilities",
        version = "9.4",
        description = "Basic file, shell and text manipulation utilities",
        longDescription = "The GNU Core Utilities are the basic file, shell and text manipulation utilities " +
                "of the GNU operating system. These are the core utilities which are expected to exist on every system. " +
                "Includes: ls, cp, mv, rm, cat, chmod, chown, mkdir, rmdir, touch, and many more.",
        category = PackageCategory.SYSTEM,
        size = 8_500_000, // 8.5 MB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-3.0",
        tags = listOf("system", "utilities", "gnu", "essential")
    )
    
    private fun createFindUtilsPackage() = Package(
        id = "findutils",
        name = "GNU Find Utilities",
        version = "4.9.0",
        description = "Utilities for finding files",
        longDescription = "The GNU Find Utilities are the basic directory searching utilities. " +
                "Includes find, xargs, and locate commands.",
        category = PackageCategory.SYSTEM,
        size = 1_800_000, // 1.8 MB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-3.0",
        tags = listOf("system", "search", "utilities")
    )
    
    private fun createGrepPackage() = Package(
        id = "grep",
        name = "GNU Grep",
        version = "3.11",
        description = "Pattern matching utility",
        longDescription = "GNU grep is a tool for searching text files for lines matching a given pattern. " +
                "Supports regular expressions and is optimized for performance.",
        category = PackageCategory.SYSTEM,
        size = 850_000, // 850 KB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-3.0",
        tags = listOf("text", "search", "regex")
    )
    
    private fun createSedPackage() = Package(
        id = "sed",
        name = "GNU Sed",
        version = "4.9",
        description = "Stream editor for filtering and transforming text",
        longDescription = "sed is a stream editor. A stream editor is used to perform basic text transformations " +
                "on an input stream (a file or input from a pipeline).",
        category = PackageCategory.SYSTEM,
        size = 650_000, // 650 KB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-3.0",
        tags = listOf("text", "editor", "stream")
    )
    
    private fun createAwkPackage() = Package(
        id = "gawk",
        name = "GNU Awk",
        version = "5.3.0",
        description = "Pattern scanning and text processing language",
        longDescription = "GNU Awk is a pattern scanning and processing language. It allows you to write tiny " +
                "but effective programs in the form of statements that define text patterns to be searched for.",
        category = PackageCategory.SYSTEM,
        size = 1_200_000, // 1.2 MB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-3.0",
        tags = listOf("text", "processing", "scripting")
    )
    
    // Network Tools
    private fun createCurlPackage() = Package(
        id = "curl",
        name = "cURL",
        version = "8.5.0",
        description = "Command line tool for transferring data with URLs",
        longDescription = "curl is a command line tool and library for transferring data with URLs. " +
                "Supports HTTP, HTTPS, FTP, FTPS, SCP, SFTP, TFTP, DICT, TELNET, LDAP and more. " +
                "Essential for API testing and file downloads.",
        category = PackageCategory.NETWORK,
        size = 3_200_000, // 3.2 MB
        dependencies = listOf("libcurl", "openssl"),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "MIT",
        tags = listOf("network", "http", "download", "api"),
        isInstalled = true,
        installedVersion = "8.4.0",
        needsUpdate = true,
        needsMaintenance = true,
        maintenanceReason = "Security update available - CVE-2024-XXXX"
    )
    
    private fun createWgetPackage() = Package(
        id = "wget",
        name = "GNU Wget",
        version = "1.21.4",
        description = "Network downloader",
        longDescription = "GNU Wget is a free utility for non-interactive download of files from the Web. " +
                "It supports HTTP, HTTPS, and FTP protocols, as well as retrieval through HTTP proxies.",
        category = PackageCategory.NETWORK,
        size = 2_800_000, // 2.8 MB
        dependencies = listOf("openssl"),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-3.0",
        tags = listOf("network", "download", "http", "ftp")
    )
    
    private fun createNetcatPackage() = Package(
        id = "netcat",
        name = "Netcat",
        version = "1.226",
        description = "TCP/IP swiss army knife",
        longDescription = "Netcat is a featured networking utility which reads and writes data across network " +
                "connections, using the TCP/IP protocol. Useful for debugging and network exploration.",
        category = PackageCategory.NETWORK,
        size = 450_000, // 450 KB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "BSD",
        tags = listOf("network", "tcp", "debugging")
    )
    
    private fun createOpenSSHPackage() = Package(
        id = "openssh",
        name = "OpenSSH",
        version = "9.6p1",
        description = "OpenSSH client and server",
        longDescription = "OpenSSH is the premier connectivity tool for remote login with the SSH protocol. " +
                "It encrypts all traffic to eliminate eavesdropping, connection hijacking, and other attacks. " +
                "Includes ssh, scp, sftp, ssh-keygen, and more.",
        category = PackageCategory.NETWORK,
        size = 5_500_000, // 5.5 MB
        dependencies = listOf("openssl"),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "BSD",
        tags = listOf("network", "ssh", "security", "remote"),
        isInstalled = true,
        installedVersion = "9.5p1",
        needsUpdate = true,
        needsMaintenance = true,
        maintenanceReason = "Critical security patch needed - Update to 9.6p1"
    )
    
    private fun createNmapPackage() = Package(
        id = "nmap",
        name = "Nmap",
        version = "7.94",
        description = "Network exploration tool and security scanner",
        longDescription = "Nmap is a free and open source utility for network discovery and security auditing. " +
                "It can rapidly scan large networks and determine what hosts are available, what services they offer, " +
                "what operating systems they are running, and more.",
        category = PackageCategory.NETWORK,
        size = 12_000_000, // 12 MB
        dependencies = listOf("libpcap"),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-2.0",
        tags = listOf("network", "security", "scanning", "audit")
    )
    
    private fun createPingPackage() = Package(
        id = "iputils",
        name = "IP Utilities",
        version = "20231222",
        description = "Network monitoring tools including ping",
        longDescription = "The iputils package contains basic utilities for monitoring a network, including ping. " +
                "These tools use ICMP messages to detect and troubleshoot network issues.",
        category = PackageCategory.NETWORK,
        size = 850_000, // 850 KB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "BSD",
        tags = listOf("network", "monitoring", "icmp")
    )
    
    private fun createTraceroutePackage() = Package(
        id = "traceroute",
        name = "Traceroute",
        version = "2.1.3",
        description = "Traces the route taken by packets over an IP network",
        longDescription = "Traceroute tracks the route packets take from an IP network on their way to a given host. " +
                "It utilizes the IP protocol's time to live (TTL) field and attempts to elicit an ICMP response.",
        category = PackageCategory.NETWORK,
        size = 650_000, // 650 KB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-2.0",
        tags = listOf("network", "routing", "diagnostic")
    )
    
    // Development Tools
    private fun createGitPackage() = Package(
        id = "git",
        name = "Git",
        version = "2.43.0",
        description = "Distributed version control system",
        longDescription = "Git is a free and open source distributed version control system designed to handle " +
                "everything from small to very large projects with speed and efficiency. " +
                "Essential for modern software development.",
        category = PackageCategory.VERSION_CONTROL,
        size = 15_000_000, // 15 MB
        dependencies = listOf("curl", "openssl"),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-2.0",
        tags = listOf("vcs", "development", "git", "essential")
    )
    
    private fun createMakePackage() = Package(
        id = "make",
        name = "GNU Make",
        version = "4.4.1",
        description = "Build automation tool",
        longDescription = "GNU Make is a tool which controls the generation of executables and other non-source files " +
                "of a program from the program's source files. Make gets its knowledge of how to build your program " +
                "from a file called the makefile.",
        category = PackageCategory.DEVELOPMENT,
        size = 1_500_000, // 1.5 MB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-3.0",
        tags = listOf("build", "development", "automation")
    )
    
    private fun createCMakePackage() = Package(
        id = "cmake",
        name = "CMake",
        version = "3.28.1",
        description = "Cross-platform build system generator",
        longDescription = "CMake is an open-source, cross-platform family of tools designed to build, test and package " +
                "software. CMake is used to control the software compilation process using simple platform and " +
                "compiler independent configuration files.",
        category = PackageCategory.DEVELOPMENT,
        size = 25_000_000, // 25 MB
        dependencies = listOf("make"),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "BSD",
        tags = listOf("build", "development", "cross-platform")
    )
    
    private fun createGCCPackage() = Package(
        id = "gcc",
        name = "GNU Compiler Collection",
        version = "13.2.0",
        description = "C, C++, and other language compilers",
        longDescription = "The GNU Compiler Collection includes front ends for C, C++, Objective-C, Fortran, Ada, Go, " +
                "and D, as well as libraries for these languages. GCC was originally written as the compiler for the " +
                "GNU operating system.",
        category = PackageCategory.DEVELOPMENT,
        size = 85_000_000, // 85 MB
        dependencies = listOf("make", "binutils"),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-3.0",
        tags = listOf("compiler", "c", "cpp", "development")
    )
    
    private fun createClangPackage() = Package(
        id = "clang",
        name = "Clang",
        version = "17.0.6",
        description = "C language family frontend for LLVM",
        longDescription = "Clang is a C, C++, and Objective-C compiler which encompasses preprocessing, parsing, " +
                "optimization, code generation, assembly, and linking. It is designed to be highly compatible with GCC.",
        category = PackageCategory.DEVELOPMENT,
        size = 95_000_000, // 95 MB
        dependencies = listOf("llvm"),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "Apache-2.0",
        tags = listOf("compiler", "c", "cpp", "llvm")
    )
    
    // Text Editors
    private fun createVimPackage() = Package(
        id = "vim",
        name = "Vim",
        version = "9.0",
        description = "Highly configurable text editor",
        longDescription = "Vim is a highly configurable text editor built to make creating and changing any kind of " +
                "text very efficient. It is included as 'vi' with most UNIX systems and with Apple OS X. " +
                "Optimized for both Android and Windows.",
        category = PackageCategory.EDITORS,
        size = 3_500_000, // 3.5 MB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "Vim",
        tags = listOf("editor", "vim", "text", "development"),
        isInstalled = true,
        installedVersion = "9.0"
    )
    
    private fun createNanoPackage() = Package(
        id = "nano",
        name = "GNU Nano",
        version = "7.2",
        description = "Small, friendly text editor",
        longDescription = "GNU nano is a small and friendly text editor. Besides basic text editing, nano offers many " +
                "extra features like an interactive search and replace, go to line and column number, auto-indentation, " +
                "feature toggles, internationalization support, and filename tab completion.",
        category = PackageCategory.EDITORS,
        size = 850_000, // 850 KB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-3.0",
        tags = listOf("editor", "text", "simple")
    )
    
    private fun createEmacPackage() = Package(
        id = "emacs",
        name = "GNU Emacs",
        version = "29.1",
        description = "Extensible, customizable text editor",
        longDescription = "GNU Emacs is an extensible, customizable, free/libre text editor — and more. " +
                "At its core is an interpreter for Emacs Lisp, a dialect of the Lisp programming language " +
                "with extensions to support text editing.",
        category = PackageCategory.EDITORS,
        size = 45_000_000, // 45 MB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-3.0",
        tags = listOf("editor", "emacs", "lisp", "extensible")
    )
    
    // Programming Languages
    private fun createPython3Package() = Package(
        id = "python3",
        name = "Python 3",
        version = "3.11.7",
        description = "Python programming language interpreter",
        longDescription = "Python is a high-level, interpreted, general-purpose programming language. " +
                "Its design philosophy emphasizes code readability with the use of significant indentation. " +
                "Includes pip package manager and standard library.",
        category = PackageCategory.PROGRAMMING,
        size = 35_000_000, // 35 MB
        dependencies = listOf("openssl", "libz"),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "PSF",
        tags = listOf("python", "programming", "scripting", "interpreter"),
        isInstalled = true,
        installedVersion = "3.11.5",
        needsUpdate = true,
        needsMaintenance = true,
        maintenanceReason = "Deprecated dependencies detected - Update recommended"
    )
    
    private fun createNodeJSPackage() = Package(
        id = "nodejs",
        name = "Node.js",
        version = "20.11.0",
        description = "JavaScript runtime built on Chrome's V8 engine",
        longDescription = "Node.js is an open-source, cross-platform JavaScript runtime environment that executes " +
                "JavaScript code outside a web browser. Includes npm package manager.",
        category = PackageCategory.PROGRAMMING,
        size = 42_000_000, // 42 MB
        dependencies = listOf("openssl"),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "MIT",
        tags = listOf("nodejs", "javascript", "runtime", "npm")
    )
    
    private fun createRubyPackage() = Package(
        id = "ruby",
        name = "Ruby",
        version = "3.3.0",
        description = "Dynamic, open source programming language",
        longDescription = "Ruby is a dynamic, open source programming language with a focus on simplicity and " +
                "productivity. It has an elegant syntax that is natural to read and easy to write. " +
                "Includes gem package manager.",
        category = PackageCategory.PROGRAMMING,
        size = 28_000_000, // 28 MB
        dependencies = listOf("openssl", "libz"),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "Ruby",
        tags = listOf("ruby", "programming", "scripting", "gem")
    )
    
    private fun createGoPackage() = Package(
        id = "golang",
        name = "Go",
        version = "1.21.6",
        description = "Go programming language",
        longDescription = "Go is an open source programming language that makes it easy to build simple, reliable, " +
                "and efficient software. Designed at Google, Go is statically typed and compiled.",
        category = PackageCategory.PROGRAMMING,
        size = 125_000_000, // 125 MB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "BSD",
        tags = listOf("go", "golang", "programming", "compiled")
    )
    
    private fun createRustPackage() = Package(
        id = "rust",
        name = "Rust",
        version = "1.75.0",
        description = "Systems programming language",
        longDescription = "Rust is a multi-paradigm, general-purpose programming language that emphasizes performance, " +
                "type safety, and concurrency. Includes cargo package manager and rustc compiler.",
        category = PackageCategory.PROGRAMMING,
        size = 165_000_000, // 165 MB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "MIT/Apache-2.0",
        tags = listOf("rust", "programming", "systems", "cargo")
    )
    
    private fun createJavaPackage() = Package(
        id = "openjdk",
        name = "OpenJDK",
        version = "21.0.1",
        description = "Open-source implementation of Java Platform",
        longDescription = "OpenJDK is a free and open-source implementation of the Java Platform, Standard Edition. " +
                "It is the official reference implementation of Java SE since version 7.",
        category = PackageCategory.PROGRAMMING,
        size = 185_000_000, // 185 MB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-2.0",
        tags = listOf("java", "jdk", "programming", "jvm")
    )
    
    // Shells
    private fun createBashPackage() = Package(
        id = "bash",
        name = "GNU Bash",
        version = "5.2.21",
        description = "GNU Bourne Again SHell",
        longDescription = "Bash is the GNU Project's shell—the Bourne Again SHell. This is an sh-compatible shell " +
                "that incorporates useful features from the Korn shell (ksh) and the C shell (csh).",
        category = PackageCategory.SHELLS,
        size = 2_800_000, // 2.8 MB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-3.0",
        tags = listOf("shell", "bash", "terminal", "scripting")
    )
    
    private fun createZshPackage() = Package(
        id = "zsh",
        name = "Z Shell",
        version = "5.9",
        description = "Powerful shell with scripting language",
        longDescription = "Zsh is a shell designed for interactive use, although it is also a powerful scripting " +
                "language. Many of the useful features of bash, ksh, and tcsh were incorporated into zsh.",
        category = PackageCategory.SHELLS,
        size = 3_200_000, // 3.2 MB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "MIT",
        tags = listOf("shell", "zsh", "terminal", "scripting")
    )
    
    private fun createFishPackage() = Package(
        id = "fish",
        name = "Fish Shell",
        version = "3.7.0",
        description = "Friendly interactive shell",
        longDescription = "fish is a smart and user-friendly command line shell for Linux, macOS, and the rest of " +
                "the family. fish includes features like syntax highlighting, autosuggest-as-you-type, and fancy tab completions.",
        category = PackageCategory.SHELLS,
        size = 4_500_000, // 4.5 MB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-2.0",
        tags = listOf("shell", "fish", "terminal", "interactive")
    )
    
    // Compression Tools
    private fun createGzipPackage() = Package(
        id = "gzip",
        name = "Gzip",
        version = "1.13",
        description = "GNU compression utility",
        longDescription = "gzip is a single-file/stream lossless data compression utility, where the resulting " +
                "compressed file generally has the suffix .gz.",
        category = PackageCategory.COMPRESSION,
        size = 450_000, // 450 KB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-3.0",
        tags = listOf("compression", "gzip", "archive")
    )
    
    private fun createBzip2Package() = Package(
        id = "bzip2",
        name = "Bzip2",
        version = "1.0.8",
        description = "High-quality data compressor",
        longDescription = "bzip2 is a freely available, patent free, high-quality data compressor. " +
                "It typically compresses files to within 10% to 15% of the best available techniques.",
        category = PackageCategory.COMPRESSION,
        size = 350_000, // 350 KB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "BSD",
        tags = listOf("compression", "bzip2", "archive")
    )
    
    private fun createXzPackage() = Package(
        id = "xz",
        name = "XZ Utils",
        version = "5.4.5",
        description = "LZMA compression utilities",
        longDescription = "XZ Utils is free general-purpose data compression software with a high compression ratio. " +
                "XZ Utils were written for POSIX-like systems, but also work on some not-so-POSIX systems.",
        category = PackageCategory.COMPRESSION,
        size = 850_000, // 850 KB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "Public Domain",
        tags = listOf("compression", "xz", "lzma", "archive")
    )
    
    private fun createZipPackage() = Package(
        id = "zip",
        name = "Info-ZIP",
        version = "3.0",
        description = "ZIP archive utilities",
        longDescription = "Info-ZIP's purpose is to provide free, portable, high-quality versions of the Zip and UnZip " +
                "compressor-archiver utilities that are compatible with the DOS-based PKZIP by PKWARE, Inc.",
        category = PackageCategory.COMPRESSION,
        size = 650_000, // 650 KB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "BSD",
        tags = listOf("compression", "zip", "archive")
    )
    
    private fun createTarPackage() = Package(
        id = "tar",
        name = "GNU Tar",
        version = "1.35",
        description = "Archiving utility",
        longDescription = "GNU tar is an archiving program designed to store multiple files in a single archive file, " +
                "and to manipulate such archives. The archive can be either a regular file or a device.",
        category = PackageCategory.COMPRESSION,
        size = 1_200_000, // 1.2 MB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-3.0",
        tags = listOf("archive", "tar", "backup")
    )
    
    private fun create7zipPackage() = Package(
        id = "p7zip",
        name = "7-Zip",
        version = "23.01",
        description = "File archiver with high compression ratio",
        longDescription = "7-Zip is a file archiver with a high compression ratio. The main features of 7-Zip are: " +
                "High compression ratio in 7z format with LZMA and LZMA2 compression, supported formats include 7z, ZIP, GZIP, BZIP2, and TAR.",
        category = PackageCategory.COMPRESSION,
        size = 2_500_000, // 2.5 MB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "LGPL",
        tags = listOf("compression", "7zip", "archive", "lzma")
    )
    
    // Web Servers
    private fun createNginxPackage() = Package(
        id = "nginx",
        name = "Nginx",
        version = "1.24.0",
        description = "High-performance web server",
        longDescription = "nginx is a web server that can also be used as a reverse proxy, load balancer, mail proxy " +
                "and HTTP cache. Known for its high performance, stability, rich feature set, simple configuration, " +
                "and low resource consumption.",
        category = PackageCategory.WEB,
        size = 5_500_000, // 5.5 MB
        dependencies = listOf("openssl", "libz"),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "BSD",
        tags = listOf("web", "server", "nginx", "http")
    )
    
    private fun createApachePackage() = Package(
        id = "apache2",
        name = "Apache HTTP Server",
        version = "2.4.58",
        description = "Popular web server",
        longDescription = "The Apache HTTP Server is a powerful and flexible HTTP/1.1 compliant web server. " +
                "Originally designed as a replacement for the NCSA HTTP Server, it has grown to be the most popular " +
                "web server on the Internet.",
        category = PackageCategory.WEB,
        size = 8_500_000, // 8.5 MB
        dependencies = listOf("openssl", "libz"),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "Apache-2.0",
        tags = listOf("web", "server", "apache", "http")
    )
    
    // Databases
    private fun createSQLitePackage() = Package(
        id = "sqlite3",
        name = "SQLite",
        version = "3.45.0",
        description = "Embedded SQL database engine",
        longDescription = "SQLite is a C-language library that implements a small, fast, self-contained, " +
                "high-reliability, full-featured, SQL database engine. SQLite is the most used database engine in the world.",
        category = PackageCategory.DATABASE,
        size = 2_800_000, // 2.8 MB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "Public Domain",
        tags = listOf("database", "sqlite", "sql", "embedded")
    )
    
    private fun createPostgreSQLPackage() = Package(
        id = "postgresql",
        name = "PostgreSQL",
        version = "16.1",
        description = "Advanced open source database",
        longDescription = "PostgreSQL is a powerful, open source object-relational database system with over 35 years " +
                "of active development that has earned it a strong reputation for reliability, feature robustness, " +
                "and performance.",
        category = PackageCategory.DATABASE,
        size = 45_000_000, // 45 MB
        dependencies = listOf("openssl", "libz"),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "PostgreSQL",
        tags = listOf("database", "postgresql", "sql", "relational")
    )
    
    private fun createMariaDBPackage() = Package(
        id = "mariadb",
        name = "MariaDB",
        version = "11.2.2",
        description = "MySQL-compatible database server",
        longDescription = "MariaDB Server is one of the most popular open source relational databases. " +
                "It's made by the original developers of MySQL and guaranteed to stay open source. " +
                "It is part of most cloud offerings and the default in most Linux distributions.",
        category = PackageCategory.DATABASE,
        size = 55_000_000, // 55 MB
        dependencies = listOf("openssl", "libz"),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-2.0",
        tags = listOf("database", "mariadb", "mysql", "sql")
    )
    
    private fun createRedisPackage() = Package(
        id = "redis",
        name = "Redis",
        version = "7.2.4",
        description = "In-memory data structure store",
        longDescription = "Redis is an open source, in-memory data structure store, used as a database, cache, " +
                "and message broker. It supports data structures such as strings, hashes, lists, sets, sorted sets " +
                "with range queries, bitmaps, hyperloglogs, geospatial indexes, and streams.",
        category = PackageCategory.DATABASE,
        size = 3_500_000, // 3.5 MB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "BSD",
        tags = listOf("database", "redis", "cache", "nosql")
    )
    
    // Security Tools
    private fun createOpenSSLPackage() = Package(
        id = "openssl",
        name = "OpenSSL",
        version = "3.2.0",
        description = "Cryptography and SSL/TLS toolkit",
        longDescription = "OpenSSL is a robust, commercial-grade, and full-featured toolkit for the Transport Layer " +
                "Security (TLS) and Secure Sockets Layer (SSL) protocols. It is also a general-purpose cryptography library.",
        category = PackageCategory.SECURITY,
        size = 5_500_000, // 5.5 MB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "Apache-2.0",
        tags = listOf("security", "ssl", "tls", "crypto", "essential")
    )
    
    private fun createGnuPGPackage() = Package(
        id = "gnupg",
        name = "GNU Privacy Guard",
        version = "2.4.4",
        description = "Complete implementation of OpenPGP standard",
        longDescription = "GnuPG is a complete and free implementation of the OpenPGP standard. GnuPG allows you to " +
                "encrypt and sign your data and communications; it features a versatile key management system.",
        category = PackageCategory.SECURITY,
        size = 8_500_000, // 8.5 MB
        dependencies = listOf("libgcrypt"),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-3.0",
        tags = listOf("security", "gpg", "encryption", "pgp")
    )
    
    private fun createWiresharkPackage() = Package(
        id = "wireshark",
        name = "Wireshark",
        version = "4.2.0",
        description = "Network protocol analyzer",
        longDescription = "Wireshark is the world's foremost and widely-used network protocol analyzer. " +
                "It lets you see what's happening on your network at a microscopic level and is the de facto standard " +
                "across many commercial and non-profit enterprises.",
        category = PackageCategory.SECURITY,
        size = 85_000_000, // 85 MB
        dependencies = listOf("libpcap"),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "GPL-2.0",
        tags = listOf("security", "network", "analyzer", "packet")
    )
    
    // Libraries
    private fun createLibCurlPackage() = Package(
        id = "libcurl",
        name = "libcurl",
        version = "8.5.0",
        description = "Client-side URL transfer library",
        longDescription = "libcurl is a free and easy-to-use client-side URL transfer library, supporting DICT, FILE, " +
                "FTP, FTPS, GOPHER, GOPHERS, HTTP, HTTPS, IMAP, IMAPS, LDAP, LDAPS, MQTT, POP3, POP3S, RTMP, RTMPS, " +
                "RTSP, SCP, SFTP, SMB, SMBS, SMTP, SMTPS, TELNET, TFTP, WS and WSS.",
        category = PackageCategory.LIBRARIES,
        size = 1_800_000, // 1.8 MB
        dependencies = listOf("openssl"),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "MIT",
        tags = listOf("library", "curl", "network", "http")
    )
    
    private fun createLibSSLPackage() = Package(
        id = "libssl",
        name = "libssl",
        version = "3.2.0",
        description = "SSL/TLS library",
        longDescription = "libssl is the SSL/TLS library from OpenSSL. It provides the client and server-side " +
                "implementations for SSLv3 and TLS.",
        category = PackageCategory.LIBRARIES,
        size = 2_500_000, // 2.5 MB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "Apache-2.0",
        tags = listOf("library", "ssl", "tls", "security")
    )
    
    private fun createLibZPackage() = Package(
        id = "zlib",
        name = "zlib",
        version = "1.3.1",
        description = "Compression library",
        longDescription = "zlib is a software library used for data compression. zlib was written by Jean-loup Gailly " +
                "and Mark Adler and is an abstraction of the DEFLATE compression algorithm used in their gzip file " +
                "compression program.",
        category = PackageCategory.LIBRARIES,
        size = 350_000, // 350 KB
        dependencies = emptyList(),
        architecture = Architecture.ALL,
        platform = Platform.BOTH,
        license = "Zlib",
        tags = listOf("library", "compression", "deflate")
    )
}
