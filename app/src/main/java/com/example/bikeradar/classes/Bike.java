package com.example.bikeradar.classes;

public class Bike {
    public String name;
    public String photo_url;
    public String objectId;
    public String phone_number;
    String location;
    LocationTime[] history;


    public LocationTime get_current_location(){
        String time = "8:00";
        String location = "хуй знает где";
        LocationTime locationTime = new LocationTime(location, time);
        return locationTime;

    }
}
