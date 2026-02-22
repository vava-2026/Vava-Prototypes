package org.example.demo.map;

public class Point
{
    public double longitude;
    public double latitude;

    public Point(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public double getLon() {
        return longitude;
    }

    public double getLat() {
        return latitude;
    }
}
