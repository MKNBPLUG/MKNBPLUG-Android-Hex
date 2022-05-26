package com.moko.support.hex.event;

public class DeviceUpdateEvent {

    private String deviceId;

    public DeviceUpdateEvent(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }
}
