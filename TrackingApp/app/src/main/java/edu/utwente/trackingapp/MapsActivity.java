package edu.utwente.trackingapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.utwente.trackingapp.databinding.ActivityMapsBinding;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener {

    private static final int REQUEST_CHECK_SETTINGS = 100;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final String TAG = MapsActivity.class.getSimpleName();

    // location
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
    private boolean mRequestingLocationUpdates = false;

    private Boolean RecordingIsActive = false;
    private ArrayList<LatLng> potholesCoordinates;
    private ArrayList<LatLng> routeCoordinates;
    private Polyline drawRoute;

    private TextView AccelerationXText;
    private TextView AccelerationYText;
    private TextView AccelerationZText;
    private TextView GPSCoordinates;
    private Button ToggleRecordButton;
    private EditText ThresholdInput;

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    // accelerometer
    List<SensorEntryData> sensorEntries = new ArrayList<SensorEntryData>();
    private SensorManager mSensorManager;
    private Sensor accelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        GPSCoordinates = findViewById(R.id.coordinatesText);

        AccelerationXText = findViewById(R.id.accelerationX);
        AccelerationYText = findViewById(R.id.accelerationY);
        AccelerationZText = findViewById(R.id.accelerationZ);

        ToggleRecordButton = findViewById(R.id.ToggleRecordButton);
        ToggleRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(RecordingIsActive) {
                    ToggleRecordButton.setText("Start");
                    RecordingIsActive = false;
                }
                else {
                    ToggleRecordButton.setText("Stop");
                    RecordingIsActive = true;
                }
            }
        });

        // Get an instance of the SensorManager
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //  Accelerometer
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(MapsActivity.this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        routeCoordinates = new ArrayList<LatLng>();

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mCurrentLocation = locationResult.getLastLocation();

                double latitude = mCurrentLocation.getLatitude();
                double longitude = mCurrentLocation.getLongitude();

                GPSCoordinates.setText("Location: lat: " + latitude + ", long: " + longitude);
            }
        };

        mLocationRequest = LocationRequest.create()
                .setInterval(UPDATE_INTERVAL_IN_MILLISECONDS)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        mRequestingLocationUpdates = true;
                        startLocationUpdates();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        if (response.isPermanentlyDenied()) {
                            openSettings();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();

    }

    public void onSensorChanged(SensorEvent event) {
        // we received a sensor event. it is a good practice to check
        // that we received the proper event
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //Log.d("event values", String.valueOf(event.values[0]));

            double x = event.values[0];
            double y = event.values[1];
            double z = event.values[2];

            AccelerationXText.setText(String.valueOf(x));
            AccelerationYText.setText(String.valueOf(y));
            AccelerationZText.setText(String.valueOf(z));

            if(RecordingIsActive) {
                double latitude = mCurrentLocation.getLatitude();
                double longitude = mCurrentLocation.getLongitude();
                sensorEntries.add(new SensorEntryData(x, y, z, latitude, longitude));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void openSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void startLocationUpdates() {
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        mMap.setMyLocationEnabled(true);
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade location settings");
                                try {
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "Pending unable to execute request");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be fixed. Fix manually in Settings";
                                Log.e(TAG, errorMessage);

                                Toast.makeText(MapsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.
                removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, task -> Log.d(TAG, "Location updates stopped!"));
    }

    private boolean checkPermissions() {
        int permissionState = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

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
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
    }


    Handler handler = new Handler();
    Runnable runnable;
    int delay = 250;
    SensorEntryData previousAveragedSensorEntry;

    @Override
    protected void onResume() {
        super.onResume();
        if (mRequestingLocationUpdates && checkPermissions()) {
            startLocationUpdates();

            handler.postDelayed(runnable = new Runnable() {
                public void run() {
                    handler.postDelayed(runnable, delay);
                    if (RecordingIsActive) {

                        double latitude = mCurrentLocation.getLatitude();
                        double longitude = mCurrentLocation.getLongitude();

                        if (!routeCoordinates.isEmpty()) {
                            int indexLastElement = routeCoordinates.size() - 1;
                            if (latitude != routeCoordinates.get(indexLastElement).latitude
                                    || longitude != routeCoordinates.get(indexLastElement).longitude) {
                                routeCoordinates.add(new LatLng(latitude, longitude));
                            }

                            if (drawRoute != null) drawRoute.remove();
                            PolylineOptions polylineOptions = new PolylineOptions()
                                    .addAll(routeCoordinates);
                            drawRoute = mMap.addPolyline(polylineOptions);

                        } else {
                            routeCoordinates.add(new LatLng(latitude, longitude));
                        }

                        SensorEntryData averagedSensorEntry = averagingSensorData(sensorEntries);
                        Log.d("avg Z: ", String.valueOf(averagedSensorEntry.getZ()));
                        sensorEntries = new ArrayList<SensorEntryData>();

                        ThresholdInput = findViewById(R.id.threshold);
                        float threshold = Float.parseFloat(ThresholdInput.getText().toString());

                        if (previousAveragedSensorEntry == null) {
                            previousAveragedSensorEntry = averagedSensorEntry;
                        } else if (potentialPothole(previousAveragedSensorEntry, averagedSensorEntry, threshold)) {
                            addPothole(averagedSensorEntry);
                        }
                    } else {
                        if (drawRoute != null) drawRoute.remove();
                        routeCoordinates.clear();
                    }
                }
            }, delay);
            super.onResume();
        }
    }

    public SensorEntryData averagingSensorData(List<SensorEntryData> sensorEntries) {

        double sumX = 0;
        double sumY = 0;
        double sumZ = 0;

        for (SensorEntryData sensorEntry: sensorEntries) {
            sumX += sensorEntry.getX();
            sumY += sensorEntry.getY();
            sumZ += sensorEntry.getZ();
        }

        int entriesLength = sensorEntries.size();
        double avgX = sumX/entriesLength;
        double avgY = sumY/entriesLength;
        double avgZ = sumZ/entriesLength;

        double latitude = mCurrentLocation.getLatitude();
        double longitude = mCurrentLocation.getLongitude();

        return new SensorEntryData(avgX, avgY, avgZ, latitude, longitude);
    }

    public boolean potentialPothole(SensorEntryData previous, SensorEntryData current, float threshold) {

        double zAxisPrevious = previous.getZ();
        double zAxisCurrent = current.getZ();

        double absolute_difference = Math.abs(Math.abs(zAxisPrevious) - Math.abs(zAxisCurrent));
        return absolute_difference > threshold;

    }

    private void addPothole(SensorEntryData averagedSensorEntry) {

        if (potholesCoordinates == null) {
            potholesCoordinates = new ArrayList<LatLng>();
        }

        if (!clusteringPotholes(averagedSensorEntry)) {
            LatLng potholeCoordinatesForDrawing =
                    new LatLng(averagedSensorEntry.getLatitude(), averagedSensorEntry.getLongitude());

            System.out.println("adding pothole");
            System.out.println("long: " + averagedSensorEntry.getLongitude());
            System.out.println("lat: " + averagedSensorEntry.getLatitude());

            potholesCoordinates.add(potholeCoordinatesForDrawing);

            mMap.addMarker(new MarkerOptions()
                    .position(potholeCoordinatesForDrawing)
                    .title("pothole")
                    .visible(true));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(potholeCoordinatesForDrawing, 16));
            Toast.makeText(MapsActivity.this, "pothole detected", Toast.LENGTH_SHORT).show();
        } else {
            System.out.println("clustered pothole");
            Toast.makeText(MapsActivity.this, "pothole clustered", Toast.LENGTH_SHORT).show();
        }


    }

    private boolean clusteringPotholes(SensorEntryData averagedSensorEntry) {

        if (potholesCoordinates.isEmpty()) {
            return false;
        }

        double latitude = averagedSensorEntry.getLatitude();
        double longitude = averagedSensorEntry.getLongitude();

        for (LatLng potholeCoordinate: potholesCoordinates) {
            if (distance(latitude, potholeCoordinate.latitude, longitude, potholeCoordinate.longitude) < 0.02) {
                return true;
            }
        }
        return false;
    }

    public static double distance(double lat1, double lat2, double lon1, double lon2)
    {

        // The math module contains a function
        // named toRadians which converts from
        // degrees to radians.
        lon1 = Math.toRadians(lon1);
        lon2 = Math.toRadians(lon2);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // Haversine formula
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2),2);

        double c = 2 * Math.asin(Math.sqrt(a));

        // Radius of earth in kilometers. Use 3956
        // for miles
        double r = 6371;

        // calculate the result
        System.out.println("Distance between potential pothole:");
        return(c * r);
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mRequestingLocationUpdates) {
            stopLocationUpdates();
        }
        handler.removeCallbacks(runnable);
    }
}