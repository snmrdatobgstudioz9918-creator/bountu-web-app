package com.chatxstudio.bountu.packages

/**
 * Package download URLs
 * These are real URLs to precompiled binaries
 */
object PackageUrls {
    
    // Termux package repository
    private const val TERMUX_REPO = "https://packages-cf.termux.dev/apt/termux-main"
    
    // GitHub releases for custom builds
    private const val GITHUB_RELEASES = "https://github.com"
    
    /**
     * Get download URL for a package
     */
    fun getPackageUrl(packageId: String, version: String, arch: String): String {
        return when (packageId) {
            // System utilities
            "busybox" -> "$TERMUX_REPO/binary-$arch/busybox_${version}_$arch.deb"
            "coreutils" -> "$TERMUX_REPO/binary-$arch/coreutils_${version}_$arch.deb"
            "findutils" -> "$TERMUX_REPO/binary-$arch/findutils_${version}_$arch.deb"
            "grep" -> "$TERMUX_REPO/binary-$arch/grep_${version}_$arch.deb"
            "sed" -> "$TERMUX_REPO/binary-$arch/sed_${version}_$arch.deb"
            "gawk" -> "$TERMUX_REPO/binary-$arch/gawk_${version}_$arch.deb"
            
            // Network tools
            "curl" -> "$TERMUX_REPO/binary-$arch/curl_${version}_$arch.deb"
            "wget" -> "$TERMUX_REPO/binary-$arch/wget_${version}_$arch.deb"
            "netcat" -> "$TERMUX_REPO/binary-$arch/netcat-openbsd_${version}_$arch.deb"
            "openssh" -> "$TERMUX_REPO/binary-$arch/openssh_${version}_$arch.deb"
            "nmap" -> "$TERMUX_REPO/binary-$arch/nmap_${version}_$arch.deb"
            "iputils" -> "$TERMUX_REPO/binary-$arch/iputils_${version}_$arch.deb"
            "traceroute" -> "$TERMUX_REPO/binary-$arch/traceroute_${version}_$arch.deb"
            
            // Development tools
            "git" -> "$TERMUX_REPO/binary-$arch/git_${version}_$arch.deb"
            "make" -> "$TERMUX_REPO/binary-$arch/make_${version}_$arch.deb"
            "cmake" -> "$TERMUX_REPO/binary-$arch/cmake_${version}_$arch.deb"
            "gcc" -> "$TERMUX_REPO/binary-$arch/gcc_${version}_$arch.deb"
            "clang" -> "$TERMUX_REPO/binary-$arch/clang_${version}_$arch.deb"
            
            // Text editors
            "vim" -> "$TERMUX_REPO/binary-$arch/vim_${version}_$arch.deb"
            "nano" -> "$TERMUX_REPO/binary-$arch/nano_${version}_$arch.deb"
            "emacs" -> "$TERMUX_REPO/binary-$arch/emacs_${version}_$arch.deb"
            
            // Programming languages
            "python3" -> "$TERMUX_REPO/binary-$arch/python_${version}_$arch.deb"
            "nodejs" -> "$TERMUX_REPO/binary-$arch/nodejs_${version}_$arch.deb"
            "ruby" -> "$TERMUX_REPO/binary-$arch/ruby_${version}_$arch.deb"
            "golang" -> "$TERMUX_REPO/binary-$arch/golang_${version}_$arch.deb"
            "rust" -> "$TERMUX_REPO/binary-$arch/rust_${version}_$arch.deb"
            "openjdk" -> "$TERMUX_REPO/binary-$arch/openjdk-17_${version}_$arch.deb"
            
            // Shells
            "bash" -> "$TERMUX_REPO/binary-$arch/bash_${version}_$arch.deb"
            "zsh" -> "$TERMUX_REPO/binary-$arch/zsh_${version}_$arch.deb"
            "fish" -> "$TERMUX_REPO/binary-$arch/fish_${version}_$arch.deb"
            
            // Compression
            "gzip" -> "$TERMUX_REPO/binary-$arch/gzip_${version}_$arch.deb"
            "bzip2" -> "$TERMUX_REPO/binary-$arch/bzip2_${version}_$arch.deb"
            "xz" -> "$TERMUX_REPO/binary-$arch/xz-utils_${version}_$arch.deb"
            "zip" -> "$TERMUX_REPO/binary-$arch/zip_${version}_$arch.deb"
            "tar" -> "$TERMUX_REPO/binary-$arch/tar_${version}_$arch.deb"
            "p7zip" -> "$TERMUX_REPO/binary-$arch/p7zip_${version}_$arch.deb"
            
            // Web servers
            "nginx" -> "$TERMUX_REPO/binary-$arch/nginx_${version}_$arch.deb"
            "apache2" -> "$TERMUX_REPO/binary-$arch/apache2_${version}_$arch.deb"
            
            // Databases
            "sqlite3" -> "$TERMUX_REPO/binary-$arch/sqlite_${version}_$arch.deb"
            "postgresql" -> "$TERMUX_REPO/binary-$arch/postgresql_${version}_$arch.deb"
            "mariadb" -> "$TERMUX_REPO/binary-$arch/mariadb_${version}_$arch.deb"
            "redis" -> "$TERMUX_REPO/binary-$arch/redis_${version}_$arch.deb"
            
            // Security
            "openssl" -> "$TERMUX_REPO/binary-$arch/openssl_${version}_$arch.deb"
            "gnupg" -> "$TERMUX_REPO/binary-$arch/gnupg_${version}_$arch.deb"
            "wireshark" -> "$TERMUX_REPO/binary-$arch/tshark_${version}_$arch.deb"
            
            // Libraries
            "libcurl" -> "$TERMUX_REPO/binary-$arch/libcurl_${version}_$arch.deb"
            "libssl" -> "$TERMUX_REPO/binary-$arch/openssl_${version}_$arch.deb"
            "zlib" -> "$TERMUX_REPO/binary-$arch/zlib_${version}_$arch.deb"
            
            else -> ""
        }
    }
    
    /**
     * Alternative download sources (mirrors)
     */
    fun getMirrorUrl(packageId: String, version: String, arch: String): List<String> {
        return listOf(
            "https://grimler.se/termux-packages-24/binary-$arch/${packageId}_${version}_$arch.deb",
            "https://dl.bintray.com/termux/termux-packages-24/binary-$arch/${packageId}_${version}_$arch.deb"
        )
    }
}
