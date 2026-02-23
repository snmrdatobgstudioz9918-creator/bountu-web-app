# Git Repository Setup for Bountu

## Overview

Bountu uses a Git repository to manage package metadata, maintenance status, and app configuration. This allows for dynamic updates without requiring app updates.

## Current Status

The app is configured to work **without** a Git repository. If the repository is not available, the app will:
- Continue to function normally
- Use default maintenance status (disabled)
- Log warnings but not crash

## Setting Up Your Own Repository

### 1. Create a GitHub Repository

Create a new repository with the following structure:

```
bountu-packages/
├── config/
│   ├── maintenance.json
│   └── app_config.json
└── packages/
    ├── package1/
    │   └── metadata.json
    ├── package2/
    │   └── metadata.json
    └── ...
```

### 2. Configuration Files

#### `config/maintenance.json`
```json
{
  "isEnabled": false,
  "title": "Scheduled Maintenance",
  "message": "We're performing scheduled maintenance to improve your experience.",
  "estimatedTime": "2 hours",
  "allowedVersions": []
}
```

#### `config/app_config.json`
```json
{
  "minVersion": "1.0",
  "latestVersion": "1.0",
  "forceUpdate": false,
  "updateMessage": "A new version is available!",
  "enabledFeatures": ["terminal", "packages", "themes"]
}
```

#### `packages/{package-id}/metadata.json`
```json
{
  "id": "package-id",
  "name": "Package Name",
  "version": "1.0.0",
  "description": "Package description",
  "category": "utilities",
  "size": 1048576,
  "dependencies": [],
  "downloadUrl": "https://example.com/package.zip",
  "checksumSha256": "abc123..."
}
```

### 3. Update the App

In `MainActivity.kt` and `GitPackagesScreen.kt`, update the repository URL:

```kotlin
val repoUrl = "https://github.com/YOUR_USERNAME/bountu-packages.git"
```

### 4. Test the Integration

1. Build and run the app
2. Check the logs for Git initialization messages
3. Verify that the app loads configuration from your repository

## Troubleshooting

### Repository Not Found

If you see errors like:
```
org.eclipse.jgit.errors.NoRemoteRepositoryException: not found
```

**Solution**: The app will continue to work with default settings. Make sure your repository URL is correct and publicly accessible.

### Permission Denied

If you see:
```
java.io.IOException: error=13, Permission denied
```

**Solution**: This error has been fixed by using JGit instead of native git commands.

### Clone Timeout

If cloning takes too long:
- Ensure you have a stable internet connection
- Consider using a shallow clone (already configured with `setDepth(1)`)
- Check if the repository is too large

## Features

### Dynamic Maintenance Mode

Enable maintenance mode by updating `config/maintenance.json`:
```json
{
  "isEnabled": true,
  "title": "Maintenance in Progress",
  "message": "We're updating our servers. Please check back soon!",
  "estimatedTime": "30 minutes",
  "allowedVersions": []
}
```

### Force Updates

Force users to update by modifying `config/app_config.json`:
```json
{
  "minVersion": "1.1",
  "latestVersion": "1.2",
  "forceUpdate": true,
  "updateMessage": "Critical security update required!",
  "enabledFeatures": ["terminal", "packages", "themes"]
}
```

### Package Distribution

Add new packages by creating a new folder in `packages/` with a `metadata.json` file.

## Benefits

1. **No App Updates Required**: Change configuration without releasing new versions
2. **Centralized Management**: Manage all app settings from one repository
3. **Version Control**: Track all changes with Git history
4. **Rollback Support**: Easily revert to previous configurations
5. **Offline Support**: App caches repository data locally

## Security Notes

- The repository should be public or use authentication
- Validate all downloaded packages with checksums
- Use HTTPS URLs for package downloads
- Consider signing packages for additional security

## Optional: Private Repository

To use a private repository, you'll need to add authentication:

```kotlin
Git.cloneRepository()
    .setURI(repoUrl)
    .setDirectory(localRepoPath)
    .setCredentialsProvider(
        UsernamePasswordCredentialsProvider("username", "token")
    )
    .call()
```

**Note**: Store credentials securely, never hardcode them in the app!
