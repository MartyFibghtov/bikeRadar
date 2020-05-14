package com.example.bikeradar.classes;

import com.google.android.gms.maps.model.LatLngBounds;

import java.util.Arrays;

public class Bike {
    public String name;
    public String photo_url;
    public String objectId;
    public String phone_number;
    LatLngBounds location;
    LocationTime[] history;


    public LocationTime get_current_location(){
        String time = "8:00";
        String location = "хуй знает где";
        LocationTime locationTime = new LocationTime(location, time);
        return locationTime;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhotoUrl(String photo_url) {
        this.photo_url = photo_url;
    }

    public void setPhoneNumber(String phone_number) {
        this.phone_number = phone_number;
    }

    public void setLocation(LatLngBounds location) {
        this.location = location;
    }

    public void setHistory(LocationTime[] history) {
        this.history = history;
    }

    @Override
    public String toString() {
        return "Bike{" +
                "name='" + name + '\'' +
                ", photo_url='" + photo_url + '\'' +
                ", objectId='" + objectId + '\'' +
                ", phone_number='" + phone_number + '\'' +
                ", location=" + location +
                ", history=" + Arrays.toString(history) +
                '}';
    }
}
