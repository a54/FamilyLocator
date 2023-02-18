package com.a54.familylocator;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.FOREGROUND_SERVICE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.GeoObject;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Circle;
import com.yandex.mapkit.geometry.Geometry;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;

import com.yandex.mapkit.map.CircleMapObject;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.runtime.logging.Logger;

/**
 * В этом примере показывается карта и камера выставляется на указанную точку.
 * Не забудьте запросить необходимые разрешения.
 */
public class MapsActivity extends Activity {

    final String TAG = "MapsActivity";

    /**
     * Замените "your_api_key" валидным API-ключом.
     * Ключ можно получить на сайте https://developer.tech.yandex.ru/
     */
    private final String MAPKIT_API_KEY = "7c8e8b64-18ff-4410-9e85-121d3adbc782";
    private final Point TARGET_LOCATION = new Point(59.945933, 30.320045);


    private MapView mapView;

    final PlacemarkMapObject[] currentPerson = {null};
    final CircleMapObject[] currentPersonPrecision = {null};


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        /**
         * Задайте API-ключ перед инициализацией MapKitFactory.
         * Рекомендуется устанавливать ключ в методе Application.onCreate(),
         * но в данном примере он устанавливается в Activity.
         */
        MapKitFactory.setApiKey(MAPKIT_API_KEY);
        /**
         * Инициализация библиотеки для загрузки необходимых нативных библиотек.
         * Рекомендуется инициализировать библиотеку MapKit в методе Activity.onCreate()
         * Инициализация в методе Application.onCreate() может привести к лишним вызовам и увеличенному использованию батареи.
         */
        MapKitFactory.initialize(this);
        // Создание MapView.
        setContentView(R.layout.activity_maps);
        super.onCreate(savedInstanceState);
        mapView = (MapView) findViewById(R.id.mapview);

        // Перемещение камеры в центр Санкт-Петербурга.
        mapView.getMap().move(
                new CameraPosition(TARGET_LOCATION, 14.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 5),
                null);
    }

    @Override
    protected void onStop() {
        // Вызов onStop нужно передавать инстансам MapView и MapKit.
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        // This registers messageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(messageReceiver, new IntentFilter("location-update-message"));
    }

    @Override
    protected void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        super.onPause();
    }

    // Handling the received Intents for the "my-integer" event
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            double latitude = intent.getDoubleExtra("latitude", -1); // -1 is going to be used as the default value
            double longitude = intent.getDoubleExtra("longitude", -1);
            double altitude = intent.getDoubleExtra("altitude", 0);
            float accuracy = intent.getFloatExtra("accuracy", 10000);
            updateMapObjects(latitude, longitude, altitude, accuracy);
        }
    };

    void updateMapObjects(double latitude, double longitude, double altitude, float accuracy){
        Point currentPoint = new Point(latitude, longitude);
        mapView = (MapView) findViewById(R.id.mapview);

        // Перемещение камеры в полученную локацию.
        mapView.getMap().move(
                new CameraPosition(currentPoint, 14.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 5),
                null);

        if (currentPerson[0] ==null){
            currentPerson[0] =mapView.getMap().getMapObjects().addPlacemark(currentPoint);
        }
        currentPerson[0].setGeometry(currentPoint);
        currentPerson[0].setText(String.valueOf(accuracy));

        Circle personPrecisionCircle = new Circle(currentPoint, accuracy<25?25:accuracy);


        if (currentPersonPrecision[0]== null){
            currentPersonPrecision[0]= mapView.getMap().getMapObjects().addCircle(personPrecisionCircle, Color.WHITE, 1, Color.argb(50,66,66,99));
        }

        currentPersonPrecision[0].setGeometry(personPrecisionCircle);

        Log.i(TAG, String.format("map objects updated: longitude %s, latitude %s, altitude %s, accuracy %s", longitude, latitude, altitude, accuracy));

    }

    @Override
    protected void onStart() {
        // Вызов onStart нужно передавать инстансам MapView и MapKit.
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();







        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            Logger.error("!!!!!!!!!!!! Нет разрешения на использование геолокации, делаю запрос ");
            requestPermission();
            return;
        } else {
            //TODO send start to service

            //LocationService.startListeningLocation
        }





    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, ACCESS_BACKGROUND_LOCATION, FOREGROUND_SERVICE}, 1);
    }



}