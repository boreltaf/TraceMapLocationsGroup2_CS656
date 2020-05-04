package com.example.tracemaplocations;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.List;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private String TAG = MapsActivity.class.getSimpleName();
    BroadcastReceiver broadcastReceiver;

    private TextView txtActivity;
    private Button btnStart, btnStop, btnclearmap;

    private Accelerometer accelerometer;    //Accelerometer
    private Gyroscope gyroscope;            //Gyroscope

     private GoogleMap mMap;
    List<LatLng> locationsList = new ArrayList<>();   // array that hold LatLong objects

    SupportMapFragment mapFragment;

    LocationManager locationManager;
    LocationListener locationListener;
    LatLng userLatLng;
//test

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        accelerometer = new Accelerometer(this);        //Accelerometer code
        gyroscope = new Gyroscope(this);

//        startTracking();  // Start Tracking service without being called [via Button] Temp..


        accelerometer.setListener(new Accelerometer.Listener() {
            @Override
            public void onTranslation(float tx, float ty, float tz) {
                if(tx > 1.0f)
                {
                    // getWindow().getDecorView().setBackgroundColor(Color.BLACK);
                }
                else if(tx < -1.0f)
                {
                    // getWindow().getDecorView().setBackgroundColor(Color.BLACK);
                }
            }
        });
        gyroscope.setListener(new Gyroscope.Listener() {
            @Override
            public void onRotation(float rx, float ry, float rz) {
                if(rz > 1.0f)
                {
                    // getWindow().getDecorView().setBackgroundColor(Color.BLACK);
                }
                else if(rz < 1.0f)
                {
                    // getWindow().getDecorView().setBackgroundColor(Color.BLACK);
                }

            }
        });        //Accelerometer code End

/* Test Code Begin */
        txtActivity = findViewById(R.id.txt_activity);
        //txtConfidence = findViewById(R.id.txt_confidence);
        btnStart = findViewById(R.id.btn_start_tracking);
        btnStop = findViewById(R.id.btn_stop_tracking);
        btnclearmap = findViewById(R.id.btn_clearmap);

        btnStart.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                startTracking();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                stopTracking();
            }
        });

        btnclearmap.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                mMap.clear();
            }
        });

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.BROADCAST_DETECTED_ACTIVITY)) {
                    int type = intent.getIntExtra("type", -1);
                    int confidence = intent.getIntExtra("confidence", 0);
                    handleUserActivity(type, confidence);
                }
            }
        };
     //   startTracking();

    }

    @Override
    protected void onResume()             //Accelerometer code
    {
        super.onResume();

        accelerometer.register();
        gyroscope.register();

        /**/
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY));
        /**/

    }
    //Accelerometer code End

    @Override
    protected void onPause()
    {
        super.onPause();

        accelerometer.unregister();
        gyroscope.unregister();
        /**/
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        /**/
    }
/**/
    private void handleUserActivity(int type, int confidence) {
        String label = getString(R.string.activity_unknown);

        switch (type) {
            case DetectedActivity.IN_VEHICLE: {
                label = getString(R.string.vehicle);
                break;
            }
            case DetectedActivity.ON_BICYCLE: {
                label = getString(R.string.bicycle);
                break;
            }
            case DetectedActivity.ON_FOOT:
            case DetectedActivity.WALKING: {
                label = getString(R.string.walking);
            //    label = getString(R.string.activity_on_foot);
                break;
            }
            case DetectedActivity.RUNNING: {
                label = getString(R.string.running);
                break;
            }
            case DetectedActivity.STILL: {
                label = getString(R.string.still);
                break;
            }
            case DetectedActivity.TILTING: {
                label = getString(R.string.tilting);
                break;
            }
            case DetectedActivity.UNKNOWN: {
                label = getString(R.string.activity_unknown);
                break;
            }
        }

        Log.e(TAG, "User activity: " + label + ", Confidence: " + confidence);

        if (confidence > Constants.CONFIDENCE) {
            txtActivity.setText(label);
            //txtConfidence.setText("Confidence: " + confidence);
        }
    }

    private void startTracking() {
        Intent intent1 = new Intent(MapsActivity.this, BackgroundDetectedActivitiesService.class);
        startService(intent1);
        txtActivity.setText(R.string.trackingstart);
    }

    private void stopTracking() {
        Intent intent = new Intent(MapsActivity.this, BackgroundDetectedActivitiesService.class);
        stopService(intent);
        txtActivity.setText(R.string.trackingstop);
    }

/**/

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // stores user latlong
                userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                // mMap.clear(); // Clear old Markers on the map
                mMap.addMarker(new MarkerOptions().position(userLatLng).title("Your Location"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(userLatLng));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        askLocationPermission();

    }

    private void askLocationPermission() {
        Dexter.withActivity(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION).withListener(new PermissionListener()
        {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse response)
            {

                if (ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getBaseContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                {
                    return;
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 20000,
                        10, locationListener);       // min time is in ms(milliseconds)


                //get users last location to set default location marker on map
                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                userLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                // mMap.clear(); // Clear old Markers on the map
                mMap.addMarker(new MarkerOptions().position(userLatLng).title("Your Location"));
                // mMap.moveCamera(CameraUpdateFactory.newLatLng(userLatLng));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 17.0f));

            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse response)
            {

            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token)
            {
                token.continuePermissionRequest();
            }

        }).check();

    }

    public void clearMap(View view)
    {
        mMap.clear();
    }

}
