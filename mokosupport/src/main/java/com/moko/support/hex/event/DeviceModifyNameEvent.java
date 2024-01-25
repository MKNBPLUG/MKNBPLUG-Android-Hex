package com.moko.support.hex.event;

public class DeviceModifyNameEvent {
    private String deviceMac;
    private String name;

    public DeviceModifyNameEvent(String deviceMac) {
        this.deviceMac = deviceMac;
    }

    public String getDeviceMac() {
        return deviceMac;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
