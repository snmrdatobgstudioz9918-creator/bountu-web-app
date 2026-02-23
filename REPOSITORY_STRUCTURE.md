# Bountu Packages Repository Structure

## Overview

This document describes the structure of the `bountu-packages-global` Git repository that Bountu uses to fetch real-time package information and downloads.

## Repository URL

```
https://github.com/SN-Mrdatobg/bountu-packages-global.git
```

## Directory Structure

```
bountu-packages-global/
├── config/
│   ├── maintenance.json       # App maintenance status
│   └── app_config.json        # App configuration
└── packages/
    ├── curl/
    │   └── metadata.json      # Package metadata for curl
    ├── wget/
    │   └── metadata.json      # Package metadata for wget
    ├── git/
    │   └── metadata.json      # Package metadata for git
    └── ...                    # More packages
```

## Configuration Files

### `config/maintenance.json`

Controls app-wide maintenance mode:

```json
{
  "isEnabled": false,
  "title": "Scheduled Maintenance",
  "message": "We're performing scheduled maintenance to improve your experience.",
  "estimatedTime": "2 hours",
  "allowedVersions": []
}
```

**Fields:**
- `isEnabled` (boolean): Enable/disable maintenance mode
- `title` (string): Maintenance screen title
- `message` (string): Detailed maintenance message
- `estimatedTime` (string): Estimated downtime
- `allowedVersions` (array): App versions that can bypass maintenance

### `config/app_config.json`

Global app configuration:

```json
{
  "minVersion": "1.0",
  "latestVersion": "1.0",
  "forceUpdate": false,
  "updateMessage": "A new version is available!",
  "enabledFeatures": ["terminal", "packages", "themes", "security"]
}
```

**Fields:**
- `minVersion` (string): Minimum supported app version
- `latestVersion` (string): Latest available version
- `forceUpdate` (boolean): Force users to update
- `updateMessage` (string): Update prompt message
- `enabledFeatures` (array): List of enabled features

## Package Metadata

### `packages/{package-id}/metadata.json`

Each package has its own directory with a `metadata.json` file:

```json
{
  "id": "curl",
  "name": "cURL",
  "version": "8.5.0",
  "description": "Command line tool for transferring data with URLs",
  "category": "networking",
  "size": 2097152,
  "dependencies": ["openssl", "zlib"],
  "downloadUrl": "https://github.com/SN-Mrdatobg/bountu-packages-global/releases/download/v1.0/curl-8.5.0-android.zip",
  "checksumSha256": "abc123def456..."
}
```

**Required Fields:**
- `id` (string): Unique package identifier (lowercase, no spaces)
- `name` (string): Display name
- `version` (string): Package version (semantic versioning)
- `description` (string): Short description
- `category` (string): Package category
- `size` (long): Package size in bytes
- `dependencies` (array): List of dependency package IDs
- `downloadUrl` (string): Direct download URL for the package
- `checksumSha256` (string): SHA-256 checksum for verification

**Supported Categories:**
- `utilities` - General utilities
- `development` - Development tools
- `networking` - Network tools
- `system` - System utilities
- `multimedia` - Audio/video tools
- `security` - Security tools
- `editors` - Text editors
- `shells` - Shell programs
- `compression` - Compression tools
- `database` - Database systems
- `web` - Web servers
- `programming` - Programming languages
- `version_control` - Version control systems
- `documentation` - Documentation tools
- `libraries` - Software libraries
- `games` - Games
- `education` - Educational software
- `science` - Scientific tools

## Package Binary Format

Packages should be distributed as:
- **ZIP files** (`.zip`) - Recommended
- **TAR.GZ files** (`.tar.gz`) - Supported
- **DEB packages** (`.deb`) - Supported

### Package Contents Structure

```
package-name.zip
├── bin/              # Executable binaries
│   └── program
├── lib/              # Shared libraries
│   └── libexample.so
└── share/            # Data files
    └── docs/
```

## Hosting Package Binaries

### Option 1: GitHub Releases (Recommended)

1. Create a release in your repository
2. Upload package binaries as release assets
3. Use the release asset URL in `downloadUrl`

Example:
```
https://github.com/SN-Mrdatobg/bountu-packages-global/releases/download/v1.0/curl-8.5.0-android.zip
```

### Option 2: External CDN

Host binaries on a CDN and reference the URL:
```json
{
  "downloadUrl": "https://cdn.example.com/packages/curl-8.5.0-android.zip"
}
```

### Option 3: Git LFS (Large File Storage)

For very large packages, use Git LFS:
```bash
git lfs track "*.zip"
git lfs track "*.tar.gz"
```

## How the App Uses the Repository

### 1. Initial Clone

On first launch, the app clones the repository:
```kotlin
gitManager.initialize("https://github.com/SN-Mrdatobg/bountu-packages-global.git")
```

### 2. Sync Updates

The app periodically syncs to get latest packages:
```kotlin
gitManager.syncRepository()
```

### 3. List Packages

Fetch all available packages:
```kotlin
val packages = gitManager.listPackages()
```

### 4. Get Package Metadata

Load specific package information:
```kotlin
val metadata = gitManager.getPackageMetadata("curl")
```

### 5. Download and Install

Download the package binary and install:
```kotlin
packageManager.installPackage("curl")
```

## Adding a New Package

### Step 1: Create Package Directory

```bash
mkdir -p packages/mypackage
```

### Step 2: Create metadata.json

```bash
cat > packages/mypackage/metadata.json << EOF
{
  "id": "mypackage",
  "name": "My Package",
  "version": "1.0.0",
  "description": "A useful package",
  "category": "utilities",
  "size": 1048576,
  "dependencies": [],
  "downloadUrl": "https://github.com/SN-Mrdatobg/bountu-packages-global/releases/download/v1.0/mypackage-1.0.0.zip",
  "checksumSha256": "your-sha256-checksum-here"
}
EOF
```

### Step 3: Build Package Binary

Create a ZIP file with your binaries:
```bash
zip -r mypackage-1.0.0.zip bin/ lib/ share/
```

### Step 4: Calculate Checksum

```bash
sha256sum mypackage-1.0.0.zip
```

### Step 5: Upload to GitHub Releases

1. Go to your repository on GitHub
2. Click "Releases" → "Create a new release"
3. Tag: `v1.0`
4. Upload `mypackage-1.0.0.zip`
5. Publish release

### Step 6: Commit and Push

```bash
git add packages/mypackage/metadata.json
git commit -m "Add mypackage v1.0.0"
git push origin main
```

### Step 7: Test in App

1. Open Bountu app
2. Go to "Packages" tab
3. Tap refresh button
4. Search for "mypackage"
5. Install and test

## Updating a Package

### Update Version

Edit `packages/mypackage/metadata.json`:
```json
{
  "version": "1.1.0",
  "downloadUrl": "https://github.com/SN-Mrdatobg/bountu-packages-global/releases/download/v1.1/mypackage-1.1.0.zip",
  "checksumSha256": "new-checksum-here"
}
```

### Commit Changes

```bash
git add packages/mypackage/metadata.json
git commit -m "Update mypackage to v1.1.0"
git push origin main
```

## Security Best Practices

### 1. Always Provide Checksums

Calculate SHA-256 checksums for all packages:
```bash
sha256sum package.zip
```

### 2. Use HTTPS URLs

Always use HTTPS for download URLs:
```
✅ https://github.com/...
❌ http://example.com/...
```

### 3. Verify Package Contents

Before uploading, verify package contents:
```bash
unzip -l package.zip
```

### 4. Sign Releases (Optional)

Sign your Git tags for additional security:
```bash
git tag -s v1.0 -m "Release v1.0"
```

## Troubleshooting

### Package Not Showing in App

1. Check metadata.json syntax (use JSON validator)
2. Ensure package ID is unique
3. Verify repository is public
4. Check app logs for errors

### Download Fails

1. Verify downloadUrl is accessible
2. Check file size matches metadata
3. Ensure checksum is correct
4. Test URL in browser

### Installation Fails

1. Check package format (ZIP/TAR.GZ/DEB)
2. Verify package structure (bin/, lib/, share/)
3. Check file permissions
4. Review app logs

## Example Packages

### cURL
```json
{
  "id": "curl",
  "name": "cURL",
  "version": "8.5.0",
  "description": "Command line tool for transferring data with URLs",
  "category": "networking",
  "size": 2097152,
  "dependencies": ["openssl", "zlib"],
  "downloadUrl": "https://github.com/SN-Mrdatobg/bountu-packages-global/releases/download/v1.0/curl-8.5.0-android.zip",
  "checksumSha256": "abc123..."
}
```

### Git
```json
{
  "id": "git",
  "name": "Git",
  "version": "2.43.0",
  "description": "Distributed version control system",
  "category": "version_control",
  "size": 15728640,
  "dependencies": ["curl", "openssl"],
  "downloadUrl": "https://github.com/SN-Mrdatobg/bountu-packages-global/releases/download/v1.0/git-2.43.0-android.zip",
  "checksumSha256": "def456..."
}
```

### Python
```json
{
  "id": "python",
  "name": "Python",
  "version": "3.12.0",
  "description": "High-level programming language",
  "category": "programming",
  "size": 52428800,
  "dependencies": ["openssl", "sqlite"],
  "downloadUrl": "https://github.com/SN-Mrdatobg/bountu-packages-global/releases/download/v1.0/python-3.12.0-android.zip",
  "checksumSha256": "ghi789..."
}
```

## Support

For issues or questions:
- GitHub Issues: https://github.com/SN-Mrdatobg/bountu-packages-global/issues
- Email: support@bountu.app

## License

Packages may have different licenses. Check individual package metadata for license information.
