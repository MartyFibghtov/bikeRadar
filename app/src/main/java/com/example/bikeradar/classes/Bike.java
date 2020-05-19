package com.example.bikeradar.classes;


public class Bike {
    public String name;
    public String photo_url;
    public String objectId;
    public String phone_number;




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


    @Override
    public String toString() {
        return "Bike{" +
                "name='" + name + '\'' +
                ", photo_url='" + photo_url + '\'' +
                ", objectId='" + objectId + '\'' +
                ", phone_number='" + phone_number + '\'' +
                '}';
    }
}
