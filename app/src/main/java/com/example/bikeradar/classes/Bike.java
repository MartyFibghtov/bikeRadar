package com.example.bikeradar.classes;

public class Bike {
    public String name;
    public String objectId;
    public String[] ownerId;
    String photo_url;
    String phone_number;
    String location;
    LocationTime[] history;

    public LocationTime get_current_location(){
        String time = "8:00";
        String location = "хуй знает где";
        LocationTime locationTime = new LocationTime(location, time);
        return locationTime;

    }

    public void setOwnerId(String[] ownerId) {
        this.ownerId = ownerId;
    }

    public String[] getOwnerId() {
        return ownerId;
    }
}
