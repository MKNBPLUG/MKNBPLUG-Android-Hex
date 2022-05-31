package com.moko.support.hex.event;

public class MQTTMessageArrivedEvent {

    private String topic;
    private byte[] message;

    public MQTTMessageArrivedEvent(String topic, byte[] message) {
        this.topic = topic;
        this.message = message;
    }


    public String getTopic() {
        return topic;
    }

    public byte[] getMessage() {
        return message;
    }
}
