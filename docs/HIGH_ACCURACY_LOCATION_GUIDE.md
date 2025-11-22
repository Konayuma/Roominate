# High-Accuracy Location Implementation Guide

## Overview

This guide documents the implementation of high-accuracy GPS location tracking in the Roominate Android app using Google Play Services Location API with `PRIORITY_HIGH_ACCURACY`.

**Status**: ✅ Complete  
**Date**: November 22, 2025  
**Components**: LocationHelper utility, MapActivity integration

---

## What Changed

### 1. Added Dependency

**File**: `app/build.gradle`

Added Google Play Services Location library:

```gradle
implementation 'com.google.android.gms:play-services-location:21.0.1'
```

### 2. Created LocationHelper Utility

**File**: `app/src/main/java/com/roominate/utils/LocationHelper.java`

A reusable helper class that provides:

- **High-accuracy GPS tracking** using `FusedLocationProviderClient` with `PRIORITY_HIGH_ACCURACY`
- **Permission handling** for `ACCESS_FINE_LOCATION`
- **Settings checks** to ensure GPS is enabled (prompts user if not)
- **Location updates** with configurable intervals
- **Last known location** retrieval for quick fallback
- **Battery-efficient** lifecycle management

**Key Configuration**:
- Priority: `PRIORITY_HIGH_ACCURACY` (uses GPS for best accuracy)
- Update interval: 10 seconds
- Fastest interval: 5 seconds
- Max wait time: 20 seconds

### 3. Integrated into MapActivity

**File**: `app/src/main/java/com/roominate/activities/tenant/MapActivity.java`

Added functionality:

- Requests location permission on startup
- Checks if GPS is enabled (prompts user to enable high-accuracy mode)
- Displays "You are here" marker showing user's current location
- Centers map on user location on first update
- Shows location accuracy in marker snippet
- Stops location updates when activity pauses (battery optimization)

---

## How It Works

### Permission Flow

1. **App starts** → Check if `ACCESS_FINE_LOCATION` permission granted
2. **Not granted** → Request permission from user
3. **Granted** → Check location settings (GPS enabled?)
4. **Settings satisfied** → Start location updates
5. **Settings not satisfied** → Show system dialog to enable GPS

### Location Updates

1. `FusedLocationProviderClient` requests location with `PRIORITY_HIGH_ACCURACY`
2. Android uses GPS satellites for precise coordinates
3. Location callback receives updates every 5-10 seconds
4. MapActivity updates "You are here" marker position
5. Accuracy displayed in meters (e.g., "Accuracy: 15m")

### Battery Optimization

- Location updates stop when activity is paused/destroyed
- Updates resume when activity returns to foreground
- Uses efficient FusedLocationProviderClient (better than LocationManager)

---

## Testing

### Manual Testing Checklist

1. **Permission Request**
   - [ ] Launch app → MapActivity requests location permission
   - [ ] Deny permission → Map shows properties only, no user location
   - [ ] Grant permission → GPS activates, user marker appears

2. **GPS Settings**
   - [ ] Disable GPS → App shows dialog to enable high-accuracy location
   - [ ] Enable GPS → User location updates start

3. **Accuracy Verification**
   - [ ] Check marker snippet shows accuracy (should be <50m outdoors)
   - [ ] Move device → Marker position updates within ~10 seconds
   - [ ] Indoor vs outdoor → Accuracy degrades indoors (expected)

4. **Battery Usage**
   - [ ] Background activity → Location updates stop when MapActivity paused
   - [ ] Return to app → Updates resume

### Logcat Monitoring

```powershell
adb logcat | Select-String "LocationHelper|MapActivity"
```

**Expected logs**:
```
LocationHelper: Location update: -12.9605, 28.6480 (accuracy: 15.0m)
MapActivity: User location: -12.9605, 28.6480 (accuracy: 15.0m)
```

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| No user marker appears | Permission denied | Grant `ACCESS_FINE_LOCATION` in app settings |
| Low accuracy (>100m) | GPS disabled or indoors | Enable high-accuracy mode, test outdoors |
| Marker doesn't update | Location updates not started | Check logcat for errors |
| High battery drain | Updates not stopped on pause | Verify `onPause()` calls `stopLocationUpdates()` |

---

## Usage in Other Activities

To use high-accuracy location in other activities (e.g., EditProfileActivity):

```java
// 1. Initialize LocationHelper
LocationHelper locationHelper = new LocationHelper(this);

// 2. Check permission
if (!locationHelper.hasLocationPermission()) {
    locationHelper.requestLocationPermission(this);
    return;
}

// 3. Check settings and start updates
locationHelper.checkLocationSettings(this, satisfied -> {
    if (satisfied) {
        locationHelper.startLocationUpdates(new LocationHelper.LocationUpdateListener() {
            @Override
            public void onLocationReceived(Location location) {
                // Use location.getLatitude() and location.getLongitude()
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                float accuracy = location.getAccuracy();
            }

            @Override
            public void onLocationError(String error) {
                Log.e(TAG, "Location error: " + error);
            }
        });
    }
});

// 4. Stop updates in onPause()
@Override
protected void onPause() {
    super.onPause();
    locationHelper.stopLocationUpdates();
}
```

---

## API Reference

### LocationHelper Methods

| Method | Description | Usage |
|--------|-------------|-------|
| `hasLocationPermission()` | Check if permission granted | Before requesting location |
| `requestLocationPermission(Activity)` | Request permission | In onCreate or when needed |
| `checkLocationSettings(Activity, Callback)` | Verify GPS enabled | Before starting updates |
| `startLocationUpdates(Listener)` | Begin high-accuracy tracking | After permission + settings check |
| `getLastKnownLocation(Listener)` | Get cached location (fast) | Quick fallback, may be stale |
| `stopLocationUpdates()` | Stop tracking | In onPause/onDestroy |

### Constants

- `LOCATION_PERMISSION_REQUEST_CODE = 1001`
- `REQUEST_CHECK_SETTINGS = 1002`

---

## Next Steps (Optional Enhancements)

1. **Custom User Icon**: Add a blue dot drawable for user location marker
2. **Geofencing**: Alert users when properties are nearby
3. **Distance Calculation**: Show distance from user to each property
4. **Route Planning**: Integrate Google Directions API for navigation
5. **Background Location**: Track location even when app is backgrounded (requires `ACCESS_BACKGROUND_LOCATION`)

---

## Files Modified

- ✅ `app/build.gradle` - Added play-services-location dependency
- ✅ `app/src/main/java/com/roominate/utils/LocationHelper.java` - Created helper class
- ✅ `app/src/main/java/com/roominate/activities/tenant/MapActivity.java` - Integrated location tracking

---

## Related Documentation

- `docs/GEOCODING_IMPLEMENTATION_GUIDE.md` - Property geocoding and address resolution
- `docs/TESTING_AND_VERIFICATION_GUIDE.md` - General testing procedures

---

## References

- [Google Play Services Location API](https://developers.google.com/android/reference/com/google/android/gms/location/package-summary)
- [FusedLocationProviderClient](https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient)
- [Location Permissions Best Practices](https://developer.android.com/training/location/permissions)
