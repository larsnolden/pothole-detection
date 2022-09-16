package edu.utwente.trackingapp;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.scan.BleScanRuleConfig;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.utwente.trackingapp.databinding.ActivityMapsBinding;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

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

    private TextView GPSCoordinates;

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    private Button ScanButton;
    private boolean isScanning = false;
    private TextView CountIBeacons;
    private ArrayList<Antena> availablePOIs = new ArrayList<Antena>();
    private ArrayList<Marker> poiMarkers = new ArrayList<Marker>();
    private Marker currentPossition;

    int countKnownScannedDevices = 0;
    private TextView IndoorCoordinates;

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
        IndoorCoordinates = findViewById(R.id.indoorCoordinatesText);

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

        CountIBeacons = findViewById(R.id.valueIBeacons);

        Import antennasImport = new Import();
        Antena[] knownAntennas = antennasImport.getAntennas();

        BleManager.getInstance().init(getApplication());

        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                //.setServiceUuids(serviceUuids)
                //.setDeviceName(true, names)
                //.setDeviceMac(mac)
                .setAutoConnect(false)
                .setScanTimeOut(7500)
                .build();

        BleManager.getInstance().initScanRule(scanRuleConfig);

        ScanButton = findViewById(R.id.ToggleScan);

        ScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentPossition != null) {

                    currentPossition.remove();
                }
                    BleManager.getInstance().scan(new BleScanCallback() {
                        @Override
                        public void onScanStarted(boolean success) {
                            System.out.println("Scan started:" + success);
                        }

                        @Override
                        public void onScanning(BleDevice bleDevice) {
                            for (Antena antena: knownAntennas) {
                                for (String macAddress: antena.getMacAddresses()) {
                                    if (macAddress.equals(bleDevice.getMac().toUpperCase())) {
                                        System.out.println("Familiar Device: " + bleDevice.getMac().toLowerCase() +
                                                " rssi: " + bleDevice.getRssi());
                                        System.out.println("From antenna: " + antena.getName());
                                        countKnownScannedDevices += 1;
                                        double rssi = bleDevice.getRssi();
                                        if(antena.getRssi() < rssi) {

                                            System.out.println("prev rssi:" + antena.getRssi() + " prev dist:" + calculateDistance(60, antena.getRssi()));
                                            System.out.println("new better rssi:" + rssi + " new distance:" + calculateDistance(60, rssi));

                                            antena.setRssi(rssi);
                                            if (!availablePOIs.contains(antena)) {
                                                availablePOIs.add(antena);
                                            }
                                        } else {
                                            System.out.println("There is a device from the same antenna with a higher signal");
                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        public void onScanFinished(List<BleDevice> scanResultList) {
//                            System.out.println("allDevices" + scanResultList.toString());
                            CountIBeacons.setText(String.valueOf((countKnownScannedDevices)));
                            updateMap();
                        }
                    });
            }
        });

    }

    void updateMap() {

        for (Marker oldMarker: poiMarkers) {
            oldMarker.remove();
        }
        poiMarkers.clear();

        for(Antena poi: availablePOIs) {
            poi.setDistance(calculateDistanceFromRssi(60, poi.getRssi(), poi.getCorrectionFactor()));
            poi.setRadialLocations(calculatePossibleRadialLocations(poi.getLongitude(), poi.getLatitude(), poi.getDistance()));
            System.out.println("poi found" + poi.getName() + " " + poi.getDistance());

            LatLng possition = new LatLng(poi.getLatitude(), poi.getLongitude());
            String name = "Antena " + poi.getName();

            System.out.println(name + " Distance: " + poi.getDistance());

            Marker markerName = mMap.addMarker(new MarkerOptions()
                    .position(possition)
                    .title(name + " Distance: " + poi.getDistance()));

            poiMarkers.add(markerName);
        }

        LatLng currentLatLng = determineLocation(availablePOIs);
        currentPossition = mMap.addMarker(new MarkerOptions()
                .position(currentLatLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                .title("Current Possition"));

        IndoorCoordinates.setText(currentLatLng.latitude + " "+ currentLatLng.longitude);

        for (Antena poi: availablePOIs) {
            poi.setRssi(-1000);
        }
        countKnownScannedDevices = 0;
        availablePOIs.clear();
    }


    LatLng determineLocation(ArrayList<Antena> availablePOIs) {
        ArrayList<LatLng> smallestTrianglePoints = new ArrayList<LatLng>();
        double minTriangleArea = 100000;


        Collections.sort(availablePOIs, new Comparator<Antena>() {
                    @Override public int compare(Antena bo1, Antena bo2) {
                        return (bo1.getRssi() <  bo2.getRssi() ? 1:-1);
                    }
        });

        ArrayList<Antena> best3POIs = new ArrayList<Antena>();
        best3POIs.add(availablePOIs.get(0));
        best3POIs.add(availablePOIs.get(1));
        best3POIs.add(availablePOIs.get(2));

        System.out.println("Best");
        System.out.println(best3POIs.get(0).getName() + " dist:" + best3POIs.get(0).getDistance());
        System.out.println(best3POIs.get(1).getName() + " dist:" + best3POIs.get(1).getDistance());
        System.out.println(best3POIs.get(2).getName() + " dist:" + best3POIs.get(2).getDistance());

        // TODO: take the 3 highest signal strength poi's
        for(Antena poi1: best3POIs) {
            System.out.println("poi" + poi1.toString());
            for (LatLng radialLocationA : poi1.getRadialLocations()) {
                for (Antena poi2 : best3POIs) {
                    if(poi1.getName() != poi2.getName()) {
                        for (LatLng radialLocationB : poi2.getRadialLocations()) {
                            for(Antena poi3: best3POIs) {
                                if(poi2.getName() != poi3.getName()) {
                                    for (LatLng radialLocationC : poi3.getRadialLocations()) {
                                        double distanceAB = calculateDistanceBetweenPoints(radialLocationA, radialLocationB);
                                        double distanceAC = calculateDistanceBetweenPoints(radialLocationA, radialLocationC);
                                        double distanceBC = calculateDistanceBetweenPoints(radialLocationB, radialLocationC);

                                        // Herons formula
                                        double area = (distanceAB+distanceAC+distanceBC)/2.0d;
                                        double totalTriangleArea = Math.sqrt(area* (area - distanceAB) * (area - distanceAC) * (area - distanceBC));
                                        if(totalTriangleArea < minTriangleArea) {
                                            //  update the new smallest found triangle
                                            smallestTrianglePoints.clear();
                                            smallestTrianglePoints.add(radialLocationA);
                                            smallestTrianglePoints.add(radialLocationB);
                                            smallestTrianglePoints.add(radialLocationC);
                                            minTriangleArea = totalTriangleArea;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("smallest triangle" + smallestTrianglePoints.get(0) + " " +  smallestTrianglePoints.get(1) + " " +  smallestTrianglePoints.get(2));
        return getCentroid(smallestTrianglePoints.get(0), smallestTrianglePoints.get(1), smallestTrianglePoints.get(2));
    }


    LatLng getCentroid(LatLng pos1, LatLng pos2, LatLng pos3) {
        // centroid of triangle
        double latCenter = (pos1.latitude + pos2.latitude + pos3.latitude)/3;
        double longCenter = (pos1.longitude + pos2.longitude + pos3.longitude)/3;
        return new LatLng(latCenter, longCenter);
    }

    double metersToLatLong(double meters) {
        double radiusEarth = 6371000;
        return meters*360/(2*Math.PI*radiusEarth);
    }

    ArrayList<LatLng> calculatePossibleRadialLocations(double centerLong, double centerLat, double radius) {
        ArrayList<LatLng> radialLocations = new ArrayList<LatLng>();
        for(int theta=0; theta <= 100; theta++) {
            double xCoord = cos(Math.toRadians(theta)) * radius;
            double yCoord = sin(Math.toRadians(theta)) * radius;

            double latitude = centerLat + metersToLatLong(xCoord);
            double longitude = centerLong + metersToLatLong(yCoord);
            System.out.println("lat: " + latitude + ",  " + "lon: " + longitude);
            radialLocations.add(new LatLng(latitude, longitude));
        }
        return radialLocations;
    }

    protected static double calculateDistanceFromRssi(int measuredPower, double rssi, double correctionFactor) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine distance, return -1.
        }
        double ratio = rssi*1.0/measuredPower;
        if (ratio < 1.0) {
            return Math.pow(ratio,10);
        }
        else {
            double distance =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return distance * correctionFactor;
        }
    }

    public static double calculateDistanceBetweenPoints(LatLng pos1, LatLng pos2)
    {
        // The math module contains a function
        // named toRadians which converts from
        // degrees to radians.
        double lon1 = Math.toRadians(pos1.longitude);
        double lon2 = Math.toRadians(pos2.longitude);
        double lat1 = Math.toRadians(pos1.latitude);
        double lat2 = Math.toRadians(pos2.latitude);

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

        return(c * r);
    }

    protected static double calculateDistance(int measuredPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine distance, return -1.
        }
        double ratio = rssi*1.0/measuredPower;
        if (ratio < 1.0) {
            return Math.pow(ratio,10);
        }
        else {
            double distance =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return distance;
        }
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
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
    }

/**
    Handler handler = new Handler();
    Runnable runnable;
    int delay = 100000;

    @Override
    protected void onResume() {
        super.onResume();
        if (mRequestingLocationUpdates && checkPermissions()) {
            startLocationUpdates();

            handler.postDelayed(runnable = new Runnable() {
                public void run() {
                    handler.postDelayed(runnable, delay);
                    System.out.println("handler");
                }
            }, delay);
            super.onResume();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mRequestingLocationUpdates) {
            stopLocationUpdates();
        }
        handler.removeCallbacks(runnable);
    }
    */

    @Override
    protected void onPause() {
        super.onPause();
        if (mRequestingLocationUpdates) {
            stopLocationUpdates();
        }
    }
}