# Maintenance Screen & Firebase Error Handling

## Overview
The Bountu app includes comprehensive error handling and maintenance mode features to ensure a smooth user experience even when Firebase connectivity issues occur or when the app is under maintenance.

## Features Implemented

### 1. **Maintenance Screen** 
Located in: `app/src/main/java/com/chatxstudio/bountu/ui/MaintenanceScreen.kt`

#### What it does:
- Displays when Firebase maintenance mode is enabled
- Shows customizable maintenance message
- Displays estimated downtime
- Provides retry functionality
- Animated UI with pulsing maintenance icon

#### Firebase Configuration:
To enable maintenance mode, set the following in Firebase Realtime Database:

```json
{
  "maintenance": {
    "enabled": true,
    "title": "Maintenance Mode",
    "message": "The app is currently under maintenance. Please try again later.",
    "estimated_time": "2 hours",
    "allowed_versions": []
  }
}
```

#### Features:
- ✅ Animated maintenance icon (wrench)
- ✅ Custom title and message from Firebase
- ✅ Estimated time display
- ✅ Retry button to check if maintenance is over
- ✅ Beautiful gradient background
- ✅ Responsive design

---

### 2. **Firebase Error Screen**
Located in: `app/src/main/java/com/chatxstudio/bountu/ui/MaintenanceScreen.kt`

#### What it does:
- Displays when Firebase connection fails
- Shows detailed error messages
- Provides troubleshooting tips
- Offers retry and exit options
- Animated error icon

#### Error Types Handled:
1. **Connection Timeout** - Firebase servers unreachable
2. **Disconnected** - No internet connection
3. **Initialization Error** - Firebase setup issues
4. **General Errors** - Any other Firebase errors

#### Features:
- ✅ Animated cloud-off icon
- ✅ Detailed error message display
- ✅ Troubleshooting checklist:
  - Check internet connection
  - Verify Firebase configuration
  - Ensure google-services.json is present
  - Try again later
- ✅ Retry button to attempt reconnection
- ✅ Exit button to close the app
- ✅ Beautiful gradient background

---

### 3. **Firebase Manager**
Located in: `app/src/main/java/com/chatxstudio/bountu/firebase/FirebaseManager.kt`

#### Connectivity Checking:
```kotlin
suspend fun checkConnectivity(): ConnectionResult
```

Returns one of:
- `ConnectionResult.Connected` - Successfully connected
- `ConnectionResult.Disconnected` - No connection
- `ConnectionResult.Timeout` - Connection timeout (10 seconds)
- `ConnectionResult.Error(message)` - Error with details

#### Maintenance Status:
```kotlin
suspend fun getMaintenanceStatus(): MaintenanceStatus
```

Returns maintenance configuration from Firebase.

#### Real-time Monitoring:
```kotlin
fun observeMaintenanceStatus(): Flow<MaintenanceStatus>
```

Continuously monitors maintenance status changes.

---

### 4. **MainActivity Integration**
Located in: `app/src/main/java/com/chatxstudio/bountu/MainActivity.kt`

#### Flow:
1. **App Launch** → Show loading screen
2. **Initialize Firebase** → Check connectivity
3. **Check Connection Status**:
   - ❌ **Error** → Show `FirebaseErrorScreen`
   - ✅ **Connected** → Check maintenance status
4. **Check Maintenance**:
   - 🔧 **Enabled** → Show `MaintenanceScreen`
   - ✅ **Disabled** → Show main app
5. **Check Permissions** → Request if needed
6. **Load Main App** → Normal operation

#### Error Handling Code:
```kotlin
when {
    firebaseError != null -> {
        FirebaseErrorScreen(
            errorMessage = firebaseError!!,
            onRetry = { /* retry logic */ },
            onExit = { finish() }
        )
    }
    maintenanceStatus?.isEnabled == true -> {
        MaintenanceScreen(
            maintenanceStatus = maintenanceStatus!!,
            onRetry = { /* check status again */ }
        )
    }
    else -> {
        // Show main app
    }
}
```

---

## Testing

### Test Maintenance Mode:
1. Open Firebase Console
2. Go to Realtime Database
3. Set `maintenance/enabled` to `true`
4. Restart the app
5. You should see the maintenance screen

### Test Connection Errors:
1. Turn off internet connection
2. Launch the app
3. You should see the Firebase error screen
4. Turn on internet
5. Click "Retry Connection"

### Test Timeout:
1. Use a very slow network connection
2. Launch the app
3. If Firebase doesn't respond within 10 seconds, timeout error appears

---

## UI Design

### Color Scheme:
- **Background**: Dark gradient (`#0D1117` → `#161B22`)
- **Cards**: Dark surface (`#1C2128`)
- **Primary Text**: White
- **Secondary Text**: Light gray (`#ADBACB`)
- **Success**: Green (`#238636`)
- **Error**: Red (`#DA3633`)
- **Warning**: Orange (`#FFA500`)
- **Info**: Blue (`#58A6FF`)

### Animations:
- **Pulsing Icons**: Alpha and scale animations
- **Smooth Transitions**: FastOutSlowInEasing
- **Loading Progress**: Linear progress indicator

---

## Error Messages

### Connection Errors:
- "Unable to connect to Firebase. Please check your internet connection."
- "Connection timeout. Firebase servers may be unreachable."
- "Firebase initialization failed: [error details]"

### Maintenance Messages:
- Customizable via Firebase
- Default: "The app is currently under maintenance. Please try again later."

---

## Best Practices

### For Developers:
1. Always test with Firebase emulator first
2. Ensure `google-services.json` is properly configured
3. Test all error scenarios before release
4. Monitor Firebase connection status in production

### For Administrators:
1. Set clear maintenance messages
2. Provide accurate estimated time
3. Test maintenance mode before enabling
4. Notify users in advance when possible

---

## Firebase Database Structure

```json
{
  "maintenance": {
    "enabled": false,
    "title": "Maintenance Mode",
    "message": "We're making improvements to serve you better!",
    "estimated_time": "2 hours",
    "allowed_versions": ["1.0.0", "1.0.1"]
  },
  "app_config": {
    "min_version": "1.0",
    "latest_version": "1.0",
    "force_update": false,
    "update_message": "A new version is available. Please update.",
    "features": ["chat", "notifications", "themes"]
  }
}
```

---

## Troubleshooting

### Issue: Firebase Error on Launch
**Solution**: 
1. Check `google-services.json` is in `app/` folder
2. Verify Firebase project is properly configured
3. Ensure internet connection is available
4. Check Firebase Console for service status

### Issue: Maintenance Screen Not Showing
**Solution**:
1. Verify `maintenance/enabled` is set to `true` in Firebase
2. Check Firebase Realtime Database rules allow read access
3. Ensure app has internet connection
4. Try clearing app data and restarting

### Issue: Stuck on Loading Screen
**Solution**:
1. Check Firebase connectivity
2. Verify timeout settings (default: 10 seconds)
3. Check device internet connection
4. Review Firebase logs for errors

---

## Future Enhancements

### Planned Features:
- [ ] Push notifications for maintenance alerts
- [ ] Scheduled maintenance mode
- [ ] Version-specific maintenance (allow certain versions)
- [ ] Analytics for error tracking
- [ ] Offline mode with cached data
- [ ] Custom error messages per error type
- [ ] Maintenance countdown timer
- [ ] Status page integration

---

## Credits
**Developer**: SN-Mrdatobg  
**Project**: Bountu  
**Framework**: Jetpack Compose  
**Backend**: Firebase Realtime Database

---

## Summary

✅ **Maintenance Screen** - Fully implemented and functional  
✅ **Firebase Error Handling** - Comprehensive error detection and display  
✅ **Connection Monitoring** - Real-time Firebase connectivity checks  
✅ **User-Friendly UI** - Beautiful, animated screens with clear messaging  
✅ **Retry Functionality** - Users can retry connections easily  
✅ **Exit Option** - Users can close app if errors persist  

The app is production-ready with robust error handling and maintenance capabilities!
