package edu.utwente.trackingapp;

public class Beacon {

    private String name;
    private double latitude;
    private double longitude;
    private String macAddress;
    private double distance;

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

    public String getMacAddresses() {
        return macAddress;
    }

    public void setMacAddresses(String macAddresses) {
        this.macAddress = macAddresses;
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

    private double rssi;




}
