package com.example.bikeradar.classes;

public class SMS{
    public String id;
    public String text;
    public String phone;

    public SMS(String id, String text, String phone) {
        this.id = id;
        this.text = text;
        this.phone = phone;
    }

    @Override
    public String toString() {
        return "SMS{" +
                "id='" + id + '\'' +
                ", text='" + text + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }

}
