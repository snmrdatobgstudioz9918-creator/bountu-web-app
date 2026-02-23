# Firebase Setup Guide for Bountu

## Quick Setup for Testing Maintenance & Error Handling

### Step 1: Firebase Console Setup

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your Bountu project
3. Navigate to **Realtime Database**

### Step 2: Database Structure

Create the following structure in your Firebase Realtime Database:

```json
{
  "maintenance": {
    "enabled": false,
    "title": "Maintenance Mode",
    "message": "We're making improvements to serve you better! The app will be back online soon.",
    "estimated_time": "2 hours",
    "allowed_versions": []
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

### Step 3: Database Rules

Set the following rules to allow read access:

```json
{
  "rules": {
    "maintenance": {
      ".read": true,
      ".write": false
    },
    "app_config": {
      ".read": true,
      ".write": false
    }
  }
}
```

**Note**: Only admins should have write access. Users only need read access.

---

## Testing Scenarios

### Test 1: Enable Maintenance Mode

**In Firebase Console:**
1. Go to Realtime Database
2. Navigate to `maintenance/enabled`
3. Change value from `false` to `true`
4. Save changes

**In App:**
1. Close and restart the app
2. You should see the **Maintenance Screen** with:
   - Orange wrench icon (animated)
   - Title: "Maintenance Mode"
   - Your custom message
   - Estimated time: "2 hours"
   - Retry button

**To Disable:**
1. Set `maintenance/enabled` back to `false`
2. Click "Retry Connection" in the app
3. App should load normally

---

### Test 2: Simulate Connection Error

**Method 1: No Internet**
1. Turn off WiFi and mobile data on your device
2. Launch the app
3. You should see the **Firebase Error Screen** with:
   - Red cloud-off icon (animated)
   - "Connection Failed" title
   - Error message: "Unable to connect to Firebase..."
   - Troubleshooting tips
   - Retry and Exit buttons

**Method 2: Airplane Mode**
1. Enable airplane mode
2. Launch the app
3. Same error screen should appear

**To Recover:**
1. Turn internet back on
2. Click "Retry Connection"
3. App should connect and load

---

### Test 3: Connection Timeout

**Simulate Slow Network:**
1. Use a network throttling tool (Chrome DevTools, Charles Proxy)
2. Set very slow connection speed
3. Launch the app
4. If Firebase doesn't respond within 10 seconds:
   - Timeout error appears
   - Message: "Connection timeout. Firebase servers may be unreachable."

---

### Test 4: Custom Maintenance Messages

**Update Firebase:**
```json
{
  "maintenance": {
    "enabled": true,
    "title": "🔧 Scheduled Maintenance",
    "message": "We're upgrading our servers to provide you with better performance and new features. Thank you for your patience!",
    "estimated_time": "30 minutes",
    "allowed_versions": []
  }
}
```

**Result:**
- Custom title and message appear on maintenance screen
- Estimated time shows "30 minutes"

---

## Firebase Configuration Files

### Required Files:

1. **google-services.json**
   - Location: `app/google-services.json`
   - Download from Firebase Console → Project Settings → Your Apps
   - Contains Firebase project configuration

2. **build.gradle (Project level)**
   ```gradle
   dependencies {
       classpath 'com.google.gms:google-services:4.4.0'
   }
   ```

3. **build.gradle (App level)**
   ```gradle
   plugins {
       id 'com.google.gms.google-services'
   }
   
   dependencies {
       implementation platform('com.google.firebase:firebase-bom:32.7.0')
       implementation 'com.google.firebase:firebase-database-ktx'
   }
   ```

---

## Monitoring & Analytics

### Check Connection Status

**In Code:**
```kotlin
val connectionResult = firebaseManager.checkConnectivity()
when (connectionResult) {
    is ConnectionResult.Connected -> Log.d("Firebase", "Connected")
    is ConnectionResult.Disconnected -> Log.d("Firebase", "Disconnected")
    is ConnectionResult.Timeout -> Log.d("Firebase", "Timeout")
    is ConnectionResult.Error -> Log.e("Firebase", "Error: ${connectionResult.message}")
}
```

### Monitor Maintenance Status

**Real-time Monitoring:**
```kotlin
firebaseManager.observeMaintenanceStatus()
    .collect { status ->
        if (status.isEnabled) {
            // Show maintenance screen
        } else {
            // Show main app
        }
    }
```

---

## Common Issues & Solutions

### Issue 1: "Firebase initialization failed"
**Causes:**
- Missing `google-services.json`
- Incorrect Firebase configuration
- Invalid API keys

**Solutions:**
1. Download fresh `google-services.json` from Firebase Console
2. Place it in `app/` directory
3. Sync Gradle files
4. Clean and rebuild project

---

### Issue 2: "Unable to connect to Firebase"
**Causes:**
- No internet connection
- Firebase service down
- Incorrect database URL
- Firewall blocking Firebase

**Solutions:**
1. Check device internet connection
2. Verify Firebase project is active
3. Check Firebase status page
4. Review database rules

---

### Issue 3: Maintenance screen not appearing
**Causes:**
- `maintenance/enabled` not set to `true`
- Database rules blocking read access
- App not checking maintenance status

**Solutions:**
1. Verify `maintenance/enabled` is `true` in Firebase
2. Check database rules allow read access
3. Restart the app completely
4. Clear app data and cache

---

### Issue 4: Stuck on loading screen
**Causes:**
- Firebase connection hanging
- Timeout not triggering
- Network issues

**Solutions:**
1. Wait for 10-second timeout
2. Check Firebase connectivity
3. Restart app
4. Check device internet connection

---

## Production Checklist

Before deploying to production:

- [ ] Firebase project properly configured
- [ ] `google-services.json` is correct and up-to-date
- [ ] Database rules are secure (read-only for users)
- [ ] Maintenance mode is disabled (`enabled: false`)
- [ ] Test all error scenarios
- [ ] Verify timeout settings are appropriate
- [ ] Test on multiple devices and network conditions
- [ ] Set up Firebase monitoring and alerts
- [ ] Document maintenance procedures for team
- [ ] Create rollback plan

---

## Maintenance Mode Best Practices

### Planning Maintenance:
1. **Notify users in advance** (push notification, email)
2. **Schedule during low-traffic hours**
3. **Provide accurate estimated time**
4. **Keep message clear and friendly**
5. **Test maintenance mode before enabling**

### During Maintenance:
1. **Monitor Firebase Console**
2. **Update estimated time if needed**
3. **Keep team informed**
4. **Test thoroughly before disabling**

### After Maintenance:
1. **Disable maintenance mode**
2. **Monitor for errors**
3. **Verify all features working**
4. **Thank users for patience**

---

## Emergency Procedures

### If Firebase Goes Down:
1. Users will see error screen automatically
2. They can retry connection
3. They can exit app if needed
4. Monitor Firebase status page
5. Communicate with users via other channels

### If Maintenance Takes Longer:
1. Update `estimated_time` in Firebase
2. Update `message` with new information
3. Users can retry to see updated info
4. Consider push notification with update

---

## Support & Resources

- **Firebase Documentation**: https://firebase.google.com/docs
- **Firebase Status**: https://status.firebase.google.com/
- **Firebase Console**: https://console.firebase.google.com/
- **Support**: Firebase Support in Console

---

## Quick Commands

### Enable Maintenance (Firebase CLI):
```bash
firebase database:set /maintenance/enabled true
```

### Disable Maintenance (Firebase CLI):
```bash
firebase database:set /maintenance/enabled false
```

### Check Current Status (Firebase CLI):
```bash
firebase database:get /maintenance
```

---

## Summary

✅ Firebase Realtime Database configured  
✅ Maintenance mode structure created  
✅ Database rules set for security  
✅ Testing procedures documented  
✅ Error handling verified  
✅ Production checklist ready  

Your app is now ready to handle maintenance mode and Firebase connectivity issues gracefully!
