# 🚀 Bountu - Quick Reference Guide

## 📦 Package Installation

### Toggle Real Installation
**File**: `app/src/main/java/com/chatxstudio/bountu/packages/PackageManager.kt`
```kotlin
private const val USE_REAL_INSTALLATION = true  // Real packages
// private const val USE_REAL_INSTALLATION = false  // Mock mode
```

### Install Package
```kotlin
val result = packageManager.installPackage("python3")
```

---

## 🔥 Firebase Maintenance

### Enable Maintenance Mode
**Firebase Console** → Realtime Database → `/maintenance/enabled` → `true`

### Disable Maintenance Mode
**Firebase Console** → Realtime Database → `/maintenance/enabled` → `false`

### Update Message
```json
{
  "maintenance": {
    "enabled": true,
    "title": "Emergency Maintenance",
    "message": "Critical update in progress!",
    "estimated_time": "15 minutes"
  }
}
```

---

## 🎨 App Behavior

### App Loads When:
- ✅ Firebase connected
- ✅ Maintenance disabled
- ✅ Permissions granted

### App Blocked When:
- ❌ Firebase disconnected → Error Screen
- ❌ Maintenance enabled → Maintenance Screen
- ❌ Permissions denied → Permission Screen

---

## 🔧 Key Files

| File | Purpose |
|------|---------|
| `MainActivity.kt` | Main entry point, Firebase check |
| `FirebaseManager.kt` | Firebase connection & maintenance |
| `PackageManager.kt` | Package installation system |
| `PackageInstaller.kt` | Real package downloads |
| `MaintenanceScreen.kt` | Maintenance UI |
| `FirebaseErrorScreen.kt` | Error UI |
| `MainScreen.kt` | Main app UI |

---

## 📊 Firebase Structure

```
/
├── maintenance/
│   ├── enabled: false
│   ├── title: "Maintenance Mode"
│   ├── message: "..."
│   └── estimated_time: "30 minutes"
│
└── app_config/
    ├── min_version: "1.0"
    ├── latest_version: "1.0"
    └── features: [...]
```

---

## 🎯 Quick Actions

### Test Maintenance Mode
1. Firebase Console → `/maintenance/enabled` → `true`
2. Restart app
3. See maintenance screen

### Test Error Screen
1. Turn off internet
2. Launch app
3. See error screen

### Enable Real Packages
1. Open `PackageManager.kt`
2. Set `USE_REAL_INSTALLATION = true`
3. Rebuild app

---

**Made by SN-Mrdatobg** 🚀
