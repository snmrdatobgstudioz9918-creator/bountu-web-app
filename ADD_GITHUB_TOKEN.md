# Add GitHub Token for Private Repository

## If You Want to Keep the Repository Private

### Step 1: Create a GitHub Personal Access Token

1. Go to https://github.com/settings/tokens
2. Click **Generate new token** → **Generate new token (classic)**
3. Give it a name: "Bountu App Access"
4. Select scopes:
   - ✅ `repo` (Full control of private repositories)
5. Click **Generate token**
6. **COPY THE TOKEN** (you won't see it again!)

### Step 2: Add Token to App

You have two options:

#### Option A: Hardcode (Not Recommended for Production)

Edit `GitPackageManager.kt`:

```kotlin
private fun cloneRepository(repoUrl: String): GitResult<Boolean> = withContext(Dispatchers.IO) {
    try {
        Log.d(TAG, "Cloning repository from $repoUrl to ${localRepoPath.absolutePath}")
        
        // Ensure parent directory exists
        localRepoPath.parentFile?.mkdirs()
        
        // Clone using JGit with authentication
        Git.cloneRepository()
            .setURI(repoUrl)
            .setDirectory(localRepoPath)
            .setCloneAllBranches(false)
            .setBranch("main")
            .setCredentialsProvider(
                UsernamePasswordCredentialsProvider(
                    "YOUR_GITHUB_USERNAME",
                    "YOUR_GITHUB_TOKEN_HERE"
                )
            )
            .setProgressMonitor(object : ProgressMonitor {
                // ... rest of code
```

#### Option B: Store in Secure Storage (Recommended)

1. Add token to encrypted preferences
2. Retrieve at runtime
3. Use with CredentialsProvider

### Step 3: Rebuild and Test

```bash
./gradlew assembleDebug
```

## ⚠️ Security Warning

**NEVER commit tokens to Git!**

- Add to `.gitignore`
- Use environment variables
- Use Android Keystore for production

## Recommended: Make Repository Public Instead

For a package repository, it's better to make it public:
- No authentication needed
- Faster access
- Better for users
- No security concerns

Just go to repository Settings → Change visibility → Make public
