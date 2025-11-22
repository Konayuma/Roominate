package com.roominate.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;

/**
 * Helper class for high-accuracy location tracking using FusedLocationProviderClient.
 * Handles permissions, settings checks (GPS enabled), and location updates with PRIORITY_HIGH_ACCURACY.
 */
public class LocationHelper {
    private static final String TAG = "LocationHelper";
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    public static final int REQUEST_CHECK_SETTINGS = 1002;

    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    /**
     * Interface for receiving location updates.
     */
    public interface LocationUpdateListener {
        void onLocationReceived(Location location);
        void onLocationError(String error);
    }

    public LocationHelper(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        setupLocationRequest();
    }

    /**
     * Configure LocationRequest with HIGH_ACCURACY priority for GPS precision.
     */
    private void setupLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // Use GPS for best accuracy
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000); // 5 seconds
        locationRequest.setMaxWaitTime(20000); // 20 seconds
    }

    /**
     * Check if location permissions are granted.
     */
    public boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request location permissions from the user.
     * Call this from your Activity.
     */
    public void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    /**
     * Check if location settings (GPS) are enabled and prompt user if not.
     * This ensures the device has high-accuracy mode enabled.
     */
    public void checkLocationSettings(Activity activity, LocationSettingsCallback callback) {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(context);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(activity, locationSettingsResponse -> {
            // All location settings are satisfied, proceed
            callback.onSettingsChecked(true);
        });

        task.addOnFailureListener(activity, e -> {
            if (e instanceof ResolvableApiException) {
                // Location settings are not satisfied, show dialog to user
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    Log.e(TAG, "Error showing location settings dialog", sendEx);
                    callback.onSettingsChecked(false);
                }
            } else {
                callback.onSettingsChecked(false);
            }
        });
    }

    /**
     * Start receiving location updates with high accuracy.
     */
    public void startLocationUpdates(LocationUpdateListener listener) {
        if (!hasLocationPermission()) {
            listener.onLocationError("Location permission not granted");
            return;
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        Log.d(TAG, "Location update: " + location.getLatitude() + ", " + location.getLongitude()
                                + " (accuracy: " + location.getAccuracy() + "m)");
                        listener.onLocationReceived(location);
                    }
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when requesting location updates", e);
            listener.onLocationError("Security exception: " + e.getMessage());
        }
    }

    /**
     * Get the last known location (fast but may be stale).
     */
    public void getLastKnownLocation(LocationUpdateListener listener) {
        if (!hasLocationPermission()) {
            listener.onLocationError("Location permission not granted");
            return;
        }

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            listener.onLocationReceived(location);
                        } else {
                            listener.onLocationError("No last known location available");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get last location", e);
                        listener.onLocationError("Failed to get location: " + e.getMessage());
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when getting last location", e);
            listener.onLocationError("Security exception: " + e.getMessage());
        }
    }

    /**
     * Stop receiving location updates to save battery.
     */
    public void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
    }

    /**
     * Interface for location settings check callback.
     */
    public interface LocationSettingsCallback {
        void onSettingsChecked(boolean satisfied);
    }
}
