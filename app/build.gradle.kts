plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization")
}

android {
    namespace = "com.chatxstudio.bountu"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.chatxstudio.bountu"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Use Gradle properties if provided; otherwise fall back to debug keystore for internal builds
            val keystorePath: String? = project.findProperty("RELEASE_STORE_FILE") as String?
            val keystorePassword: String? = project.findProperty("RELEASE_STORE_PASSWORD") as String?
            val keyAliasProp: String? = project.findProperty("RELEASE_KEY_ALIAS") as String?
            val keyPasswordProp: String? = project.findProperty("RELEASE_KEY_PASSWORD") as String?

            if (keystorePath != null && keystorePassword != null && keyAliasProp != null && keyPasswordProp != null) {
                // Resolve relative to project root if needed
                val direct = file(keystorePath)
                val fromRoot = file("${rootDir}/${keystorePath}")
                storeFile = if (direct.exists()) direct else fromRoot
                storePassword = keystorePassword
                keyAlias = keyAliasProp
                keyPassword = keyPasswordProp
            } else {
                // Fallback to debug keystore (for internal testing only)
                storeFile = file("${rootDir}/app/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)
    implementation("androidx.compose.material:material-icons-extended")

    // Kotlinx Serialization for JSON parsing (used by Git package manager)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // Apache Commons Compress for TAR.GZ and DEB extraction
    implementation("org.apache.commons:commons-compress:1.26.0")
    
    // JGit for Git operations on Android
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
