package com.example.bikeradar.classes;

import java.sql.Time;

public class LocationTime {
    private String location;
    private String time;

    public LocationTime(String location, String time) {
        this.location = location;
        this.time = time;
    }

    public String getLocation() {
        return location;
    }

    public String getTime() {
        return time;
    }
}
