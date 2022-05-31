package com.moko.support.hex;

public class MQTTConstants {
    // CONFIG
    public static final int MSG_ID_POWER_ON_DEFAULT = 0x01;
    public static final int MSG_ID_REPORT_INTERVAL = 0x02;
    public static final int MSG_ID_POWER_REPORT_SETTING = 0x03;
    public static final int MSG_ID_ENERGY_REPORT_PARAMS = 0x04;
    public static final int MSG_ID_OVER_LOAD_PROTECTION = 0x05;
    public static final int MSG_ID_OVER_VOLTAGE_PROTECTION = 0x06;
    public static final int MSG_ID_UNDER_VOLTAGE_PROTECTION = 0x07;
    public static final int MSG_ID_OVER_CURRENT_PROTECTION = 0x08;
    public static final int MSG_ID_INDICATOR_STATUS_COLOR = 0x09;
    public static final int MSG_ID_NTP_PARAMS = 0x0A;
    public static final int MSG_ID_TIMEZONE = 0x0B;
    public static final int MSG_ID_LOAD_NOTIFY_ENABLE = 0x0C;
    public static final int MSG_ID_NET_CONNECTING_STATUS = 0x0D;
    public static final int MSG_ID_NET_CONNECTED_STATUS = 0x0E;
    public static final int MSG_ID_POWER_SWITCH_STATUS = 0x0F;
    public static final int MSG_ID_POWER_PROTECT = 0x10;
    public static final int MSG_ID_CONNECTION_TIMEOUT = 0x11;
    public static final int MSG_ID_BUTTON_CONTROL_ENABLE = 0x12;

    public static final int CONFIG_MSG_ID_CLEAR_OVERLOAD_PROTECTION = 0x21;
    public static final int CONFIG_MSG_ID_CLEAR_OVER_VOLTAGE_PROTECTION = 0x22;
    public static final int CONFIG_MSG_ID_CLEAR_UNDER_VOLTAGE_PROTECTION = 0x23;
    public static final int CONFIG_MSG_ID_CLEAR_OVER_CURRENT_PROTECTION = 0x24;
    public static final int CONFIG_MSG_ID_SWITCH_STATE = 0x25;
    public static final int READ_MSG_ID_SWITCH_INFO = 0x26;
    public static final int CONFIG_MSG_ID_COUNTDOWN = 0x27;
    public static final int CONFIG_MSG_ID_ENERGY_CLEAR = 0x28;
    public static final int CONFIG_MSG_ID_RESET = 0x29;
    public static final int CONFIG_MSG_ID_OTA = 0x2A;
    public static final int CONFIG_MSG_ID_OTA_ONE_WAY = 0x2B;
    public static final int CONFIG_MSG_ID_OTA_BOTH_WAY = 0x2C;
    public static final int READ_MSG_ID_DEVICE_STATUS = 0x2D;
    public static final int READ_MSG_ID_POWER_INFO = 0x2E;
    public static final int READ_MSG_ID_ENERGY_TOTAL = 0x2F;
    public static final int READ_MSG_ID_ENERGY_DAILY = 0x30;
    public static final int READ_MSG_ID_ENERGY_HOURLY = 0x31;
    // device info
    public static final int READ_MSG_ID_DEVICE_TYPE = 0x32;
    public static final int READ_MSG_ID_MANUFACTURER = 0x33;
    public static final int READ_MSG_ID_PRODUCT_MODEL = 0x34;
    public static final int READ_MSG_ID_HARDWARE_VERSION = 0x35;
    public static final int READ_MSG_ID_FIRMWARE_VERSION = 0x36;
    public static final int READ_MSG_ID_MAC = 0x37;
    public static final int READ_MSG_ID_IMEI = 0x38;
    public static final int READ_MSG_ID_ICCID = 0x39;

    public static final int MSG_ID_SYSTEM_TIME = 0x3A;
    public static final int READ_MSG_ID_WORK_MODE = 0x3B;

    // MQTT
    public static final int MSG_ID_MQTT_HOST = 0x61;
    public static final int MSG_ID_MQTT_PORT = 0x62;
    public static final int MSG_ID_MQTT_USERNAME = 0x63;
    public static final int MSG_ID_MQTT_PASSWORD = 0x64;
    public static final int MSG_ID_MQTT_CLIENT_ID = 0x65;
    public static final int MSG_ID_MQTT_CLEAN_SESSION = 0x66;
    public static final int MSG_ID_MQTT_KEEP_ALIVE = 0x67;
    public static final int MSG_ID_MQTT_QOS = 0x68;
    public static final int MSG_ID_MQTT_SUBSCRIBE_TOPIC = 0x69;
    public static final int MSG_ID_MQTT_PUBLISH_TOPIC = 0x6A;
    public static final int MSG_ID_LWT_ENABLE = 0x6B;
    public static final int MSG_ID_LWT_QOS = 0x6C;
    public static final int MSG_ID_LWT_RETAIN = 0x6D;
    public static final int MSG_ID_LWT_TOPIC = 0x6E;
    public static final int MSG_ID_LWT_MESSAGE = 0x6F;
    public static final int MSG_ID_MQTT_SSL = 0x70;
    public static final int MSG_ID_APN = 0x71;
    public static final int MSG_ID_APN_USERNAME = 0x72;
    public static final int MSG_ID_APN_PASSWORD = 0x73;
    public static final int MSG_ID_NETWORK_PRIORITY = 0x74;
    public static final int CONFIG_MSG_ID_CA_CERTIFICATE = 0x75;
    public static final int CONFIG_MSG_ID_SELF_SIGNED_SERVER_CERTIFICATES = 0x76;
    public static final int CONFIG_MSG_ID_MQTT_CONFIG_FINISH = 0x77;
    public static final int CONFIG_MSG_ID_MQTT_RECONNECT = 0x78;

    // NOTIFY
    public static final int NOTIFY_MSG_ID_SWITCH_STATE = 0x41;
    public static final int NOTIFY_MSG_ID_COUNTDOWN_INFO = 0x42;
    public static final int NOTIFY_MSG_ID_OTA_RESULT = 0x43;
    public static final int NOTIFY_MSG_ID_POWER_INFO = 0x44;
    public static final int NOTIFY_MSG_ID_ENERGY_TOTAL = 0x45;
    public static final int NOTIFY_MSG_ID_ENERGY_DAILY = 0x46;
    public static final int NOTIFY_MSG_ID_ENERGY_HOURLY = 0x47;
    public static final int NOTIFY_MSG_ID_OVERLOAD_OCCUR = 0x48;
    public static final int NOTIFY_MSG_ID_OVER_VOLTAGE_OCCUR = 0x49;
    public static final int NOTIFY_MSG_ID_UNDER_VOLTAGE_OCCUR = 0x4A;
    public static final int NOTIFY_MSG_ID_OVER_CURRENT_OCCUR = 0x4B;
    public static final int NOTIFY_MSG_ID_LOAD_STATUS_NOTIFY = 0x4C;
    public static final int NOTIFY_MSG_ID_RECONNECT_READY_RESULT = 0x4D;
}
