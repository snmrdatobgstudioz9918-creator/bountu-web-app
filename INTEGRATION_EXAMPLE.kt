// INTEGRATION EXAMPLE: How to use Git-based package management in Bountu
// This file shows you how to integrate GitPackageManager into your existing app

package com.chatxstudio.bountu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.chatxstudio.bountu.git.GitPackageManager
import com.chatxstudio.bountu.git.GitResult
import com.chatxstudio.bountu.ui.GitPackagesScreen
import com.chatxstudio.bountu.ui.GitVsFirebaseScreen
import kotlinx.coroutines.launch

// ============================================================================
// OPTION 1: Add Git Packages Screen to your existing navigation
// ============================================================================

/*
In your MainScreen.kt or wherever you have navigation:

@Composable
fun MainScreen(...) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen() }
        composable("packages") { PackagesScreen() }

        // ADD THESE NEW ROUTES:
        composable("git_packages") {
            GitPackagesScreen()
        }
        composable("git_vs_firebase") {
            GitVsFirebaseScreen()
        }
    }
}

// Then add a button to navigate:
Button(onClick = { navController.navigate("git_packages") }) {
    Text("Git Packages (Beta)")
}
*/

// ============================================================================
// OPTION 2: Replace FirebaseManager with GitPackageManager
// ============================================================================

/*
In your MainActivity.kt, replace:

val firebaseManager = remember { FirebaseManager(context) }

With:

val gitManager = remember { GitPackageManager(context) }

// Initialize Git repository instead of Firebase
LaunchedEffect(Unit) {
    when (val result = gitManager.initialize()) {
        is GitResult.Success -> {
            // Repository initialized successfully
            Log.d("MainActivity", "Git repository ready")
        }
        is GitResult.Error -> {
            // Handle error
            Log.e("MainActivity", "Git init failed: ${result.message}")
        }
    }
}

// Get maintenance status from Git instead of Firebase
LaunchedEffect(Unit) {
    when (val result = gitManager.getMaintenanceStatus()) {
        is GitResult.Success -> {
            maintenanceStatus = result.data
        }
        is GitResult.Error -> {
            Log.e("MainActivity", "Failed to get maintenance status: ${result.message}")
        }
    }
}

// Get app config from Git instead of Firebase
LaunchedEffect(Unit) {
    when (val result = gitManager.getAppConfig()) {
        is GitResult.Success -> {
            val config = result.data
            // Use config.minVersion, config.latestVersion, etc.
        }
        is GitResult.Error -> {
            Log.e("MainActivity", "Failed to get app config: ${result.message}")
        }
    }
}
*/

// ============================================================================
// OPTION 3: Use both Firebase and Git (Hybrid Approach)
// ============================================================================

/*
Keep Firebase as fallback, use Git as primary:

val firebaseManager = remember { FirebaseManager(context) }
val gitManager = remember { GitPackageManager(context) }

var useGit by remember { mutableStateOf(true) }

LaunchedEffect(Unit) {
    if (useGit) {
        // Try Git first
        when (val result = gitManager.initialize()) {
            is GitResult.Success -> {
                // Git is working, use it
                val status = gitManager.getMaintenanceStatus()
            }
            is GitResult.Error -> {
                // Git failed, fallback to Firebase
                useGit = false
                firebaseManager.initialize()
            }
        }
    } else {
        // Use Firebase
        firebaseManager.initialize()
    }
}
*/

// ============================================================================
// OPTION 4: Standalone Git Package Manager Activity
// ============================================================================

class GitPackageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GitPackagesScreen()
            }
        }
    }
}

// ============================================================================
// EXAMPLE: Complete Integration in MainActivity
// ============================================================================

@Composable
fun MainScreenWithGit() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Initialize Git Package Manager
    val gitManager = remember { GitPackageManager(context) }

    var gitStatus by remember { mutableStateOf("Not initialized") }
    var packages by remember { mutableStateOf<List<String>>(emptyList()) }

    // Initialize on start
    LaunchedEffect(Unit) {
        gitStatus = "Initializing..."

        // For testing, use local repository
        val repoUrl = "file:///C:/Users/dato/bountu-packages-repo"

        // For production, use GitHub
        // val repoUrl = "https://github.com/YOUR_USERNAME/bountu-packages.git"

        when (val result = gitManager.initialize(repoUrl)) {
            is GitResult.Success -> {
                gitStatus = "Initialized successfully"

                // Load packages
                when (val packagesResult = gitManager.listPackages()) {
                    is GitResult.Success -> {
                        packages = packagesResult.data
                        gitStatus = "Loaded ${packagesResult.data.size} packages"
                    }
                    is GitResult.Error -> {
                        gitStatus = "Error loading packages: ${packagesResult.message}"
                    }
                }
            }
            is GitResult.Error -> {
                gitStatus = "Initialization failed: ${result.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Git Status: $gitStatus")

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    gitStatus = "Syncing..."
                    when (val result = gitManager.syncRepository()) {
                        is GitResult.Success -> {
                            val sync = result.data
                            gitStatus = if (sync.hasUpdates) {
                                "Updated to ${sync.afterCommit.take(8)}"
                            } else {
                                "Already up to date"
                            }
                        }
                        is GitResult.Error -> {
                            gitStatus = "Sync failed: ${result.message}"
                        }
                    }
                }
            }
        ) {
            Text("Sync Repository")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Packages: ${packages.size}")
        packages.forEach { packageId ->
            Text("- $packageId")
        }
    }
}

// ============================================================================
// TESTING CHECKLIST
// ============================================================================

/*
✅ 1. Sync Gradle files (should download kotlinx-serialization-json)
✅ 2. Rebuild project (Build → Rebuild Project)
✅ 3. Add GitPackagesScreen to your navigation
✅ 4. Run the app and test:
   - Click "Initialize" button
   - Should clone the local repository
   - Click "Sync" button
   - Should show "Already up to date"
   - View the 5 sample packages
   - Click on a package to see details

✅ 5. Test offline:
   - Initialize once
   - Turn off WiFi
   - Restart app
   - Should still work with cached data!

✅ 6. Update a package:
   - Edit C:/Users/dato/bountu-packages-repo/packages/busybox/metadata.json
   - Change version to "1.36.2"
   - Commit: git commit -am "Update busybox"
   - In app, click "Sync"
   - Should detect the update!
*/

// ============================================================================
// NEXT STEPS
// ============================================================================

/*
1. Push repository to GitHub:
   cd C:/Users/dato/bountu-packages-repo
   git remote add origin https://github.com/YOUR_USERNAME/bountu-packages.git
   git push -u origin main

2. Update app to use GitHub URL:
   gitManager.initialize("https://github.com/YOUR_USERNAME/bountu-packages.git")

3. Add more packages to the repository

4. Implement package installation functionality

5. Add background sync for automatic updates

6. Create repository management UI (add/remove repos)

7. Implement Git LFS for large binary files

8. Add GPG commit signing for security
*/

// ============================================================================
// TROUBLESHOOTING
// ============================================================================

/*
Problem: "Repository not initialized"
Solution: Make sure Git is installed and accessible from command line

Problem: Clone fails
Solution: Check repository URL, ensure Git is in PATH

Problem: Sync shows no updates
Solution: This is normal if nothing changed. Make a commit to test.

Problem: Gradle sync fails
Solution:
- File → Invalidate Caches / Restart
- Check internet connection
- Verify Kotlin version compatibility

Problem: Serialization errors
Solution: Make sure kotlin("plugin.serialization") is in plugins block
*/
