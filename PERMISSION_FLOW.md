# 🔐 Permission Request Flow

## User Journey

```
┌─────────────────────────────────────────────────────────────┐
│                     APP LAUNCH                              │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              ENHANCED LOADING SCREEN                        │
│                                                             │
│                    BOUNTU (Glowing)                         │
│                  ▓▓▓▓▓▓▓▓▓▓░░░░░                           │
│                       75%                                   │
│                                                             │
│              made by SN-Mrdatobg                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                            ↓
                   (Check Permissions)
                            ↓
                    ┌───────────────┐
                    │ All Granted?  │
                    └───────────────┘
                      ↙           ↘
                   YES              NO
                    ↓                ↓
        ┌──────────────────┐   ┌──────────────────────┐
        │   MAIN SCREEN    │   │  PERMISSION SCREEN   │
        │                  │   │                      │
        │  Terminal        │   │  🔒 Lock Icon        │
        │  Themes          │   │                      │
        │  Security        │   │  Permissions Needed  │
        │  Connection      │   │                      │
        │  Packages        │   │  ✓ Internet          │
        │                  │   │  ✓ Network State     │
        └──────────────────┘   │  ✓ Notifications     │
                               │                      │
                               │  [Grant Permissions] │
                               └──────────────────────┘
                                        ↓
                               (User Clicks Button)
                                        ↓
                          ┌──────────────────────────┐
                          │  SYSTEM PERMISSION       │
                          │  DIALOG (Android)        │
                          │                          │
                          │  Allow Bountu to:        │
                          │  • Access Internet       │
                          │  • Check Network         │
                          │  • Send Notifications    │
                          │                          │
                          │  [Allow]  [Deny]         │
                          └──────────────────────────┘
                                   ↙        ↘
                              ALLOW          DENY
                                ↓             ↓
                    ┌──────────────────┐  ┌──────────────────┐
                    │   MAIN SCREEN    │  │  WARNING DIALOG  │
                    │   (Full Access)  │  │                  │
                    └──────────────────┘  │  Some permissions│
                                          │  were denied.    │
                                          │  App may not     │
                                          │  function fully. │
                                          │                  │
                                          │      [OK]        │
                                          └──────────────────┘
                                                   ↓
                                          ┌──────────────────┐
                                          │ PERMISSION SCREEN│
                                          │ (Try Again)      │
                                          └──────────────────┘
```

## Permission Screen Details

### Layout
```
┌─────────────────────────────────────────┐
│                                         │
│              🔒 (64dp Icon)             │
│                                         │
│        Permissions Required             │
│                                         │
│  Bountu needs the following            │
│  permissions to function properly:      │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │ ✓ Internet                        │ │
│  │   Required for communication      │ │
│  │   with Windows                    │ │
│  │                                   │ │
│  │ ✓ Network State                   │ │
│  │   Check network connectivity      │ │
│  │                                   │ │
│  │ ✓ Notifications (Android 13+)     │ │
│  │   Show important alerts           │ │
│  └───────────────────────────────────┘ │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │      Grant Permissions            │ │
│  └───────────────────────────────────┘ │
│                                         │
└─────────────────────────────────────────┘
```

## Permission States

### State 1: All Permissions Granted ✅
```kotlin
permissionsGranted = true
showPermissionDialog = false
→ Show Main Screen
```

### State 2: Permissions Not Granted ❌
```kotlin
permissionsGranted = false
showPermissionDialog = false
→ Show Permission Screen
```

### State 3: Permissions Denied After Request ⚠️
```kotlin
permissionsGranted = false
showPermissionDialog = true
→ Show Warning Dialog + Permission Screen
```

## Code Flow

```kotlin
LaunchedEffect(Unit) {
    // 1. Define required permissions
    val requiredPermissions = mutableListOf(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE
    )
    
    // 2. Add Android 13+ permission
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    
    // 3. Check if all granted
    val allGranted = requiredPermissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) 
            == PackageManager.PERMISSION_GRANTED
    }
    
    // 4. Update state
    if (allGranted) {
        permissionsGranted = true
    } else {
        permissionLauncher.launch(requiredPermissions.toTypedArray())
    }
}
```

## Permission Launcher

```kotlin
val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
) { permissions ->
    // Check if all permissions granted
    permissionsGranted = permissions.values.all { it }
    
    // Show dialog if any denied
    if (!permissionsGranted) {
        showPermissionDialog = true
    }
}
```

## Android Manifest

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## Permission Rationale

### Internet Permission
- **Why**: Required for TCP/IP communication with Windows PC
- **Usage**: Sending commands, receiving responses
- **Critical**: Yes - app cannot function without it

### Network State Permission
- **Why**: Check if device is connected to network
- **Usage**: Validate connection before attempting communication
- **Critical**: Yes - prevents unnecessary connection attempts

### Post Notifications Permission (Android 13+)
- **Why**: Show important alerts and status updates
- **Usage**: Connection status, command results, errors
- **Critical**: No - app works without it, but UX is degraded

## User Experience Considerations

### ✅ Good Practices Implemented:
1. **Clear Explanation**: Each permission has a description
2. **Visual Hierarchy**: Icons and cards make it easy to scan
3. **Single Request**: All permissions requested at once
4. **Graceful Degradation**: App shows permission screen if denied
5. **Retry Option**: User can try again from permission screen
6. **No Blocking**: User sees what's needed before granting

### 🎨 Visual Design:
- Lock icon indicates security/permissions
- Check marks show what will be granted
- Card layout groups related information
- Primary button for main action
- Material 3 design system

### 📱 Platform Compatibility:
- Android 6.0+ (API 23+): Runtime permissions
- Android 13+ (API 33+): POST_NOTIFICATIONS
- Backward compatible with older versions

---

**Security Note**: All permissions are used only for their stated purpose and follow Android's best practices for permission handling.
