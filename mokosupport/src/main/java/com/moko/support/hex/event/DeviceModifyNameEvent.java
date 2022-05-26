package com.moko.support.hex.event;

public class DeviceModifyNameEvent {

    private String deviceId;
    private String name;

    public DeviceModifyNameEvent(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
