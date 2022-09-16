package edu.utwente.trackingapp;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class Antena {
    private String name;
    private double latitude;
    private double longitude;
    private String[] macAddresses;
    private double rssi;
    private double distance;
    private ArrayList<LatLng> radialLocations;
    private double correctionFactor = 1;

    public double getCorrectionFactor() {
        return correctionFactor;
    }

    public void setCorrectionFactor(double correctionFactor) {
        this.correctionFactor = correctionFactor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String[] getMacAddresses() {
        return macAddresses;
    }

    public void setMacAddresses(String[] macAddresses) {
        this.macAddresses = macAddresses;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getRssi() {
        return rssi;
    }

    public void setRssi(double rssi) {
        this.rssi = rssi;
    }

    public ArrayList<LatLng> getRadialLocations() {
        return radialLocations;
    }

    public void setRadialLocations(ArrayList<LatLng> radialLocations) {
        this.radialLocations = radialLocations;
    }
}
