# Setup GitHub Repository for Bountu Packages

## Current Issue

The repository `bountu-packages-global` exists locally but hasn't been pushed to GitHub yet, causing a 404 error.

## Solution: Push Your Local Repository to GitHub

### Step 1: Create Repository on GitHub

1. Go to https://github.com/new
2. Repository name: `bountu-packages-global`
3. Description: "Package repository for Bountu app"
4. **Make it PUBLIC** ✅
5. **DO NOT** initialize with README, .gitignore, or license
6. Click **Create repository**

### Step 2: Push Your Local Repository

Open terminal in `C:\Users\dato\bountu-packages-global` and run:

```bash
# Initialize git if not already done
git init

# Add all files
git add .

# Commit
git commit -m "Initial commit: Add package repository structure"

# Add remote
git remote add origin https://github.com/SN-Mrdatobg/bountu-packages-global.git

# Push to GitHub
git push -u origin main
```

If you get an error about branch name, try:
```bash
git branch -M main
git push -u origin main
```

### Step 3: Verify on GitHub

1. Go to https://github.com/SN-Mrdatobg/bountu-packages-global
2. You should see your files
3. Make sure it's **PUBLIC** (check Settings → Visibility)

### Step 4: Test in App

1. Open Bountu app
2. Go to Packages tab
3. Tap refresh button
4. Packages should load from GitHub!

## Alternative: Create Repository from Scratch

If you want to start fresh:

```bash
# Create directory
mkdir bountu-packages-global
cd bountu-packages-global

# Initialize git
git init

# Create structure
mkdir -p config packages

# Create config files
cat > config/maintenance.json << 'EOF'
{
  "isEnabled": false,
  "title": "Scheduled Maintenance",
  "message": "We're performing scheduled maintenance.",
  "estimatedTime": "2 hours",
  "allowedVersions": []
}
EOF

cat > config/app_config.json << 'EOF'
{
  "minVersion": "1.0",
  "latestVersion": "1.0",
  "forceUpdate": false,
  "updateMessage": "A new version is available!",
  "enabledFeatures": ["terminal", "packages", "themes", "security"]
}
EOF

# Create README
cat > README.md << 'EOF'
# Bountu Packages Global Repository

This repository contains package metadata for the Bountu app.

## Structure

- `config/` - App configuration files
- `packages/` - Package metadata files

## Adding a Package

Create a directory in `packages/` with a `metadata.json` file.

Example: `packages/curl/metadata.json`
EOF

# Add example package
mkdir -p packages/curl
cat > packages/curl/metadata.json << 'EOF'
{
  "id": "curl",
  "name": "cURL",
  "version": "8.5.0",
  "description": "Command line tool for transferring data with URLs",
  "category": "networking",
  "size": 2097152,
  "dependencies": [],
  "downloadUrl": "https://github.com/SN-Mrdatobg/bountu-packages-global/releases/download/v1.0/curl-8.5.0-android.zip",
  "checksumSha256": "placeholder-checksum"
}
EOF

# Commit
git add .
git commit -m "Initial commit"

# Create on GitHub and push
git remote add origin https://github.com/SN-Mrdatobg/bountu-packages-global.git
git branch -M main
git push -u origin main
```

## Quick Test: Use a Different Public Repository

If you want to test immediately without setting up your own repo, you can temporarily use a test repository.

Edit these files in your Bountu project:

1. `app/src/main/java/com/chatxstudio/bountu/git/GitPackageManager.kt`:
```kotlin
private const val DEFAULT_REPO_URL = "https://github.com/termux/termux-packages.git"
```

2. `app/src/main/java/com/chatxstudio/bountu/MainActivity.kt`:
```kotlin
val repoUrl = "https://github.com/termux/termux-packages.git"
```

This will use Termux's public repository to test the Git functionality.

## Troubleshooting

### Error: "remote origin already exists"
```bash
git remote remove origin
git remote add origin https://github.com/SN-Mrdatobg/bountu-packages-global.git
```

### Error: "failed to push some refs"
```bash
git pull origin main --rebase
git push -u origin main
```

### Error: "repository not found"
- Make sure you created the repository on GitHub
- Check the URL is correct
- Make sure it's PUBLIC

### Still getting 404?
- Wait a few minutes after creating the repository
- Clear app data and try again
- Check GitHub repository exists and is public
