package edu.utwente.trackingapp;

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
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final int REQUEST_CHECK_SETTINGS = 100;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String FILE_NAME = "tracking-app-logs.txt";

    // location
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
    private boolean mRequestingLocationUpdates = false;

    private Boolean RecordingIsActive = false;

    private TextView AddressText;
    private TextView AccelerationXText;
    private TextView AccelerationYText;
    private TextView AccelerationZText;

    private Button ToggleButton;
    private Button AnalyseButton;
    private Button goToMapButton;

    private EditText ThresholdInput;

    // accelerometer
    List<float[]> accelerations = new ArrayList<float[]>();
    private SensorManager mSensorManager;
    private Sensor accelerometer;

    List<float[]> potholeLocations = new ArrayList<float[]>();

    Context context = this;

    // File
    FileOutputStream stream;

    public MainActivity() throws FileNotFoundException {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //  location
        AddressText = findViewById(R.id.addressText);

        AccelerationXText = findViewById(R.id.accelerationX);
        AccelerationYText = findViewById(R.id.accelerationY);
        AccelerationZText = findViewById(R.id.accelerationZ);

        ToggleButton = findViewById(R.id.button);
        AnalyseButton = findViewById(R.id.button2);
        AnalyseButton.setVisibility(View.GONE);

        ThresholdInput = findViewById(R.id.threshold);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mCurrentLocation = locationResult.getLastLocation();

                double latitude = mCurrentLocation.getLatitude();
                double longitude = mCurrentLocation.getLongitude();

                if(RecordingIsActive) {
                    writeMeasurement(accelerations, new double[] {latitude, longitude});
                }

                AddressText.setText("Location: " + latitude + ", " + longitude);
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

        // Get an instance of the SensorManager
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //  Accelerometer
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
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
                                    rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "Pending unable to execute request");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be fixed. Fix manually in Settings";
                                Log.e(TAG, errorMessage);

                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
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

    public void onAccuracyChanged(Sensor sensor, int i) {
        // do nothing
    }

    public void writeMeasurement(List<float[]> accelerations, double[] location) {
        File path = context.getFilesDir();
        File logsFile = new File(path, FILE_NAME);
        try {
            stream = new FileOutputStream(logsFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            try {
                for(float[] acceleration : accelerations) {
//                    Log.d("acceleration: ", String.valueOf(acceleration[0]));
                    String writeString = String.join("=",
                            String.valueOf(acceleration[0]),
                            String.valueOf(acceleration[1]),
                            String.valueOf(acceleration[2]),
                            String.valueOf(location[0]),
                            String.valueOf(location[1]),
                            "\n"
                    );
                    stream.write(writeString.getBytes());
                }
            } catch (IOException e) {
                Toast.makeText(this, "File write failed", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        } finally {
            try {
                stream.close();
                //  empty accelerations
                accelerations = new ArrayList<float[]>();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void onSensorChanged(SensorEvent event) {
        // we received a sensor event. it is a good practice to check
        // that we received the proper event
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //Log.d("event values", String.valueOf(event.values[0]));

            AccelerationXText.setText(String.valueOf(event.values[0]));
            AccelerationYText.setText(String.valueOf(event.values[1]));
            AccelerationZText.setText(String.valueOf(event.values[2]));

            if(RecordingIsActive) {
                accelerations.add(new float[] {event.values[0], event.values[1], event.values[2]});
            }
        }
    }

    public void onClickToggleBtn(View v)
    {
        if(RecordingIsActive) {
            Toast.makeText(this, "Stoped Recording", Toast.LENGTH_SHORT).show();
            ToggleButton.setText("Start Recording");
            RecordingIsActive = false;
            AnalyseButton.setVisibility(View.VISIBLE);
        }
        else {
            Toast.makeText(this, "Started Recording", Toast.LENGTH_SHORT).show();
            AnalyseButton.setVisibility(View.GONE);
            ToggleButton.setText("Stop Recording");
            RecordingIsActive = true;
        }
    }

    public void onClickAnalyseBtn(View v) {
       float threshold = Float.parseFloat(ThresholdInput.getText().toString());
       analysis(threshold);
       String toastText = String.format("Found %s potholes", potholeLocations.size());
       Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show();
       potholeLocations = new ArrayList<float[]>();
    }

    public void analysis(float threshold) {
        //  read file
        try {
            File path = context.getFilesDir();
            File logsFile = new File(path, FILE_NAME);
            BufferedReader br = new BufferedReader(new FileReader(logsFile));
            String line;
            Float z_accel_prev = 0f;
            while ((line = br.readLine()) != null) {
                String[] entries = line.split("=");
                Float z_accel = Float.parseFloat(entries[2]);
                float absolute_difference = Math.abs(Math.abs(z_accel) - Math.abs(z_accel_prev));
                if(absolute_difference > threshold) {
                    float latitude = Float.parseFloat(entries[3]);
                    float longitude =  Float.parseFloat(entries[4]);
                    potholeLocations.add(new float[] {absolute_difference, latitude, longitude});
                }
                z_accel_prev = z_accel;
            }
            br.close();
        }
        catch (IOException e) {
            Toast.makeText(this, "Analysis Failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void openMaps() {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRequestingLocationUpdates && checkPermissions()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRequestingLocationUpdates) {
            stopLocationUpdates();
        }
    }

}