# Bountu App - Error Handling & Maintenance Flow

## Application Launch Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                        APP LAUNCH                                │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    LOADING SCREEN                                │
│  • Animated "BOUNTU" logo                                        │
│  • Progress bar (0% → 100%)                                      │
│  • "made by SN-Mrdatobg" with rainbow effect                     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                 INITIALIZE FIREBASE                              │
│  • FirebaseApp.initializeApp()                                   │
│  • Enable persistence                                            │
│  • Set up database reference                                     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│              CHECK FIREBASE CONNECTIVITY                         │
│  • Connect to .info/connected                                    │
│  • Wait up to 10 seconds                                         │
│  • Return ConnectionResult                                       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                    ┌─────────┴─────────┐
                    ↓                   ↓
            ┌───────────────┐   ┌──────────────┐
            │   CONNECTED   │   │    ERROR     │
            └───────────────┘   └──────────────┘
                    ↓                   ↓
                    │           ┌───────────────────────────────┐
                    │           │  FIREBASE ERROR SCREEN        │
                    │           │  • Red cloud-off icon         │
                    │           │  • "Connection Failed"        │
                    │           │  • Error message              │
                    │           │  • Troubleshooting tips       │
                    │           │  • [Retry] [Exit] buttons     │
                    │           └───────────────────────────────┘
                    │                   ↓
                    │           ┌───────┴────────┐
                    │           ↓                ↓
                    │       [RETRY]          [EXIT]
                    │           ↓                ↓
                    │      (Go back to     (Close app)
                    │       connectivity
                    │         check)
                    ↓
┌─────────────────────────────────────────────────────────────────┐
│              GET MAINTENANCE STATUS                              │
│  • Read from /maintenance path                                   │
│  • Check if enabled = true                                       │
│  • Get title, message, estimated_time                            │
└─────────────────────────────────────────────────────────────────┘
                    ↓
            ┌───────┴────────┐
            ↓                ↓
    ┌──────────────┐  ┌─────────────┐
    │   ENABLED    │  │  DISABLED   │
    └──────────────┘  └─────────────┘
            ↓                ↓
┌───────────────────────┐   │
│ MAINTENANCE SCREEN    │   │
│ • Orange wrench icon  │   │
│ • Custom title        │   │
│ • Custom message      │   │
│ • Estimated time      │   │
│ • [Retry] button      │   │
└───────────────────────┘   │
            ↓                │
        [RETRY]              │
            ↓                │
    (Check status again)     │
            │                │
            └────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────────────┐
│              CHECK PERMISSIONS                                   │
│  • Internet                                                      │
│  • Network State                                                 │
│  • Notifications (Android 13+)                                   │
└─────────────────────────────────────────────────────────────────┘
                    ↓
            ┌───────┴────────┐
            ↓                ↓
    ┌──────────────┐  ┌─────────────────┐
    │   GRANTED    │  │    DENIED       │
    └──────────────┘  └─────────────────┘
            ↓                ↓
            │        ┌───────────────────────┐
            │        │ PERMISSION SCREEN     │
            │        │ • Lock icon           │
            │        │ • Permission list     │
            │        │ • [Grant] button      │
            │        └───────────────────────┘
            │                ↓
            │        [GRANT PERMISSIONS]
            │                ↓
            │        (Request permissions)
            │                ↓
            └────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────────────┐
│                      MAIN APP                                    │
│  • Communication Manager                                         │
│  • Security Manager                                              │
│  • Theme Manager                                                 │
│  • Full app functionality                                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## Error Types & Responses

### 1. Connection Timeout (10 seconds)
```
Firebase not responding
        ↓
ConnectionResult.Timeout
        ↓
FirebaseErrorScreen
        ↓
Message: "Connection timeout. Firebase servers may be unreachable."
```

### 2. No Internet Connection
```
Device offline
        ↓
ConnectionResult.Disconnected
        ↓
FirebaseErrorScreen
        ↓
Message: "Unable to connect to Firebase. Please check your internet connection."
```

### 3. Firebase Initialization Error
```
Firebase setup failed
        ↓
Exception thrown
        ↓
FirebaseErrorScreen
        ↓
Message: "Firebase initialization failed: [error details]"
```

### 4. Database Error
```
Database read failed
        ↓
ConnectionResult.Error(message)
        ↓
FirebaseErrorScreen
        ↓
Message: [Custom error message]
```

---

## State Management

### Firebase Connection States
```kotlin
sealed class ConnectionResult {
    object Connected      // ✅ Firebase is connected
    object Disconnected   // ❌ No internet connection
    object Timeout        // ⏱️ Connection took too long
    data class Error      // ⚠️ Specific error occurred
}
```

### Maintenance States
```kotlin
data class MaintenanceStatus(
    val isEnabled: Boolean,        // true = show maintenance screen
    val title: String,             // Custom title
    val message: String,           // Custom message
    val estimatedTime: String,     // "2 hours", "30 minutes", etc.
    val allowedVersions: List      // Versions that can bypass
)
```

### App States
```kotlin
// State priority (highest to lowest):
1. firebaseError != null          → FirebaseErrorScreen
2. maintenanceStatus.isEnabled    → MaintenanceScreen
3. !permissionsGranted            → PermissionScreen
4. else                           → MainScreen
```

---

## Screen Components

### Loading Screen
```
┌─────────────────────────────────┐
│                                 │
│                                 │
│          B O U N T U            │
│      (animated gradient)        │
│                                 │
│     ▓▓▓▓▓▓▓▓▓▓░░░░░░░░         │
│            75%                  │
│                                 │
│   made by SN-Mrdatobg          │
│   (rainbow animation)           │
│                                 │
└─────────────────────────────────┘
```

### Firebase Error Screen
```
┌─────────────────────────────────┐
│                                 │
│           ☁️ (pulsing)          │
│                                 │
│      Connection Failed          │
│                                 │
│  ┌───────────────────────────┐ │
│  │ ⚠️ Unable to connect      │ │
│  │                           │ │
│  │ [Error message here]      │ │
│  │                           │ │
│  │ Troubleshooting:          │ │
│  │ • Check internet          │ │
│  │ • Verify Firebase config  │ │
│  │ • Check google-services   │ │
│  │ • Try again later         │ │
│  └───────────────────────────┘ │
│                                 │
│  ┌─────────────────────────┐   │
│  │   🔄 Retry Connection   │   │
│  └─────────────────────────┘   │
│                                 │
│  ┌─────────────────────────┐   │
│  │   🚪 Exit App           │   │
│  └─────────────────────────┘   │
│                                 │
└─────────────────────────────────┘
```

### Maintenance Screen
```
┌─────────────────────────────────┐
│                                 │
│           🔧 (pulsing)          │
│                                 │
│      Maintenance Mode           │
│                                 │
│  ┌───────────────────────────┐ │
│  │                           │ │
│  │ We're making improvements │ │
│  │ to serve you better!      │ │
│  │                           │ │
│  │ ─────────────────────────│ │
│  │                           │ │
│  │ ⏰ Estimated: 2 hours     │ │
│  │                           │ │
│  └───────────────────────────┘ │
│                                 │
│  ┌─────────────────────────┐   │
│  │   🔄 Retry Connection   │   │
│  └─────────────────────────┘   │
│                                 │
│  We apologize for the          │
│  inconvenience.                │
│  Please check back soon!       │
│                                 │
└─────────────────────────────────┘
```

### Permission Screen
```
┌─────────────────────────────────┐
│                                 │
│              🔒                 │
│                                 │
│    Permissions Required         │
│                                 │
│  Bountu needs the following    │
│  permissions to function:       │
│                                 │
│  ┌───────────────────────────┐ │
│  │ ✓ Internet                │ │
│  │   Required for comms      │ │
│  │                           │ │
│  │ ✓ Network State           │ │
│  │   Check connectivity      │ │
│  │                           │ │
│  │ ✓ Notifications           │ │
│  │   Show important alerts   │ │
│  └───────────────────────────┘ │
│                                 │
│  ┌─────────────────────────┐   │
│  │   Grant Permissions     │   │
│  └─────────────────────────┘   │
│                                 │
└─────────────────────────────────┘
```

---

## Timing & Delays

### Loading Screen
- **Progress animation**: 30ms per 5% increment
- **Total duration**: ~600ms (0% → 100%)
- **Hold at 100%**: 500ms
- **Total loading time**: ~1.1 seconds

### Connection Check
- **Timeout**: 10 seconds maximum
- **Typical response**: 1-3 seconds
- **Retry delay**: Immediate (user-triggered)

### Animations
- **Pulse animation**: 1.5 seconds cycle
- **Scale animation**: 2 seconds cycle
- **Color shift**: 3 seconds cycle
- **Glow effect**: 1 second cycle

---

## User Actions

### From Firebase Error Screen:
1. **Retry Connection**
   - Clears error state
   - Resets loading
   - Attempts new connection
   - Shows loading screen
   - Returns to connectivity check

2. **Exit App**
   - Calls `finish()`
   - Closes application
   - User must relaunch manually

### From Maintenance Screen:
1. **Retry Connection**
   - Shows loading screen
   - Fetches latest maintenance status
   - If disabled → proceeds to app
   - If enabled → shows screen again

### From Permission Screen:
1. **Grant Permissions**
   - Launches system permission dialog
   - Requests all required permissions
   - If granted → proceeds to app
   - If denied → shows warning dialog

---

## Firebase Database Paths

```
/
├── maintenance/
│   ├── enabled (boolean)
│   ├── title (string)
│   ├── message (string)
│   ├── estimated_time (string)
│   └── allowed_versions/ (array)
│       ├── 0: "1.0.0"
│       └── 1: "1.0.1"
│
└── app_config/
    ├── min_version (string)
    ├── latest_version (string)
    ├── force_update (boolean)
    ├── update_message (string)
    └── features/ (array)
        ├── 0: "chat"
        ├── 1: "notifications"
        └── 2: "themes"
```

---

## Monitoring Points

### Key Metrics to Track:
1. **Connection Success Rate**
   - % of successful Firebase connections
   - Average connection time
   - Timeout frequency

2. **Error Frequency**
   - Connection errors per day
   - Error types distribution
   - Peak error times

3. **Maintenance Impact**
   - Users affected
   - Retry attempts
   - Duration of maintenance

4. **User Actions**
   - Retry button clicks
   - Exit button clicks
   - Permission grant rate

---

## Summary

✅ **4 Main Screens**: Loading, Error, Maintenance, Permission  
✅ **3 Error Types**: Timeout, Disconnected, Error  
✅ **2 User Actions**: Retry, Exit  
✅ **1 Goal**: Smooth user experience even during issues  

The flow ensures users are never stuck without information or options!
