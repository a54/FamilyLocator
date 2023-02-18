package com.a54.familylocator;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationService extends Service {

    final String TAG = "LocationService";


    private LocationCallback locationCallback;

    private FusedLocationProviderClient client;

    LocationRequest locationRequest;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent foreIntent = new Intent(this, LocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(foreIntent);
        }



        return super.onStartCommand(intent, flags, startId);
    }

    public void startListeningLocation(){

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {

                    Intent locationIntent = new Intent("location-update-message");
                    locationIntent.putExtra("longitude", location.getLongitude());
                    locationIntent.putExtra("latitude", location.getLatitude());
                    locationIntent.putExtra("altitude", location.getAltitude());
                    locationIntent.putExtra("accuracy", location.getAccuracy());

                    broadcastManager.sendBroadcast(locationIntent);
                }
            }
        };

        client = LocationServices.getFusedLocationProviderClient(this);

        createLocationRequest();
        startLocationUpdates();
    }

    protected void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY);
    }


    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        client.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
