package com.example.spotwelderapp;

public class Device {
    private String name;
    private String address;

    public Device(String deviceName, String deviceAddress) {
        name = deviceName;
        address = deviceAddress;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }
}
