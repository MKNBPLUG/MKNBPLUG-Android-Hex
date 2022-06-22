package com.moko.support.hex;

import android.text.TextUtils;

import com.elvishew.xlog.XLog;
import com.moko.ble.lib.utils.MokoUtils;

import androidx.annotation.IntRange;

public class MQTTMessageAssembler {


    public static byte[] assembleConfigClearOverloadStatus(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.CONFIG_MSG_ID_CLEAR_OVERLOAD_PROTECTION;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleConfigClearOverVoltageStatus(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.CONFIG_MSG_ID_CLEAR_OVER_VOLTAGE_PROTECTION;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleConfigClearOverCurrentStatus(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.CONFIG_MSG_ID_CLEAR_OVER_CURRENT_PROTECTION;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleConfigClearUnderVoltageStatus(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.CONFIG_MSG_ID_CLEAR_UNDER_VOLTAGE_PROTECTION;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteSwitchInfo(String deviceId, int onOff) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 1;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.CONFIG_MSG_ID_SWITCH_STATE;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) onOff;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteTimer(String deviceId, @IntRange(from = 1, to = 86400) int countdown) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 4;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] countdownBytes = MokoUtils.toByteArray(countdown, dataLength);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.CONFIG_MSG_ID_COUNTDOWN;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        for (int i = 0; i < countdownBytes.length; i++) {
            message[i + 6 + deviceIdLength] = countdownBytes[i];
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadManufacturer(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.READ_MSG_ID_MANUFACTURER;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadProductModel(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.READ_MSG_ID_PRODUCT_MODEL;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadHardwareVersion(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.READ_MSG_ID_HARDWARE_VERSION;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadFirmwareVersion(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.READ_MSG_ID_FIRMWARE_VERSION;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadMac(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.READ_MSG_ID_MAC;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadIMEI(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.READ_MSG_ID_IMEI;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadICCID(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.READ_MSG_ID_ICCID;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadSwitchInfo(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.READ_MSG_ID_SWITCH_INFO;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadPowerInfo(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.READ_MSG_ID_POWER_INFO;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadEnergyHourly(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.READ_MSG_ID_ENERGY_HOURLY;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadEnergyDaily(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.READ_MSG_ID_ENERGY_DAILY;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadEnergyTotal(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.READ_MSG_ID_ENERGY_TOTAL;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleConfigEnergyClear(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.CONFIG_MSG_ID_ENERGY_CLEAR;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadButtonControlEnable(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_BUTTON_CONTROL_ENABLE;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteButtonControlEnable(String deviceId, int enable) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 1;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_BUTTON_CONTROL_ENABLE;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) enable;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }


    public static byte[] assembleWriteReset(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.CONFIG_MSG_ID_RESET;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadPowerOnDefault(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_POWER_ON_DEFAULT;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWritePowerOnDefault(String deviceId, int status) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 1;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_POWER_ON_DEFAULT;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) status;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadReportInterval(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_REPORT_INTERVAL;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteReportInterval(String deviceId, int switchInterval, int countdownInterval) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 8;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] switchIntervalBytes = MokoUtils.toByteArray(switchInterval, 4);
        byte[] countdownIntervalBytes = MokoUtils.toByteArray(countdownInterval, 4);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_REPORT_INTERVAL;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        for (int i = 0; i < 4; i++) {
            message[i + 6 + deviceIdLength] = switchIntervalBytes[i];
        }
        for (int i = 0; i < 4; i++) {
            message[i + 10 + deviceIdLength] = countdownIntervalBytes[i];
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadPowerReportSetting(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_POWER_REPORT_SETTING;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWritePowerReportSetting(String deviceId, int reportInterval, int reportThreshold) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 5;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] reportIntervalBytes = MokoUtils.toByteArray(reportInterval, 4);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_POWER_REPORT_SETTING;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        for (int i = 0; i < 4; i++) {
            message[i + 6 + deviceIdLength] = reportIntervalBytes[i];
        }
        message[10 + deviceIdLength] = (byte) reportThreshold;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadEnergyReportParams(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_ENERGY_REPORT_PARAMS;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteEnergyReportParams(String deviceId, int storageInterval, int storageThreshold, int reportInterval) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 4;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] reportIntervalBytes = MokoUtils.toByteArray(reportInterval, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_ENERGY_REPORT_PARAMS;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) storageInterval;
        message[7 + deviceIdLength] = (byte) storageThreshold;
        for (int i = 0; i < 2; i++) {
            message[i + 8 + deviceIdLength] = reportIntervalBytes[i];
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadConnectionTimeout(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_CONNECTION_TIMEOUT;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteConnectionTimeout(String deviceId, int timeout) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 2;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] timeoutBytes = MokoUtils.toByteArray(timeout, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_CONNECTION_TIMEOUT;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = timeoutBytes[0];
        message[7 + deviceIdLength] = timeoutBytes[1];
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadNTPParams(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_NTP_PARAMS;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteNTPParams(String deviceId, int enable, int interval, String url) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 3 + url.getBytes().length;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] intervalBytes = MokoUtils.toByteArray(interval, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_NTP_PARAMS;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) enable;
        message[7 + deviceIdLength] = intervalBytes[0];
        message[8 + deviceIdLength] = intervalBytes[1];
        if (dataLength > 3) {
            int urlLength = url.getBytes().length;
            for (int i = 0; i < urlLength; i++) {
                message[9 + deviceIdLength + i] = url.getBytes()[i];
            }
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadTimeZone(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_TIMEZONE;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteTimeZone(String deviceId, int timeZone) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 1;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_TIMEZONE;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) timeZone;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadSystemTime(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_SYSTEM_TIME;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteSystemTime(String deviceId, int time) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 4;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] timeBytes = MokoUtils.toByteArray(time, 4);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_SYSTEM_TIME;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        for (int i = 0; i < 4; i++) {
            message[i + 6 + deviceIdLength] = timeBytes[i];
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadDeviceType(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.READ_MSG_ID_DEVICE_TYPE;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadOverloadProtection(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_OVER_LOAD_PROTECTION;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteOverloadProtection(String deviceId, int enable, int powerThreshold, int timeThreshold) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 4;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] powerThresholdBytes = MokoUtils.toByteArray(powerThreshold, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_OVER_LOAD_PROTECTION;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) enable;
        for (int i = 0; i < 2; i++) {
            message[i + 7 + deviceIdLength] = powerThresholdBytes[i];
        }
        message[9 + deviceIdLength] = (byte) timeThreshold;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadOverVoltageProtection(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_OVER_VOLTAGE_PROTECTION;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteOverVoltageProtection(String deviceId, int enable, int voltageThreshold, int timeThreshold) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 4;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] voltageThresholdBytes = MokoUtils.toByteArray(voltageThreshold, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_OVER_VOLTAGE_PROTECTION;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) enable;
        for (int i = 0; i < 2; i++) {
            message[i + 7 + deviceIdLength] = voltageThresholdBytes[i];
        }
        message[9 + deviceIdLength] = (byte) timeThreshold;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadUnderVoltageProtection(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_UNDER_VOLTAGE_PROTECTION;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteUnderVoltageProtection(String deviceId, int enable, int voltageThreshold, int timeThreshold) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 3;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_UNDER_VOLTAGE_PROTECTION;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) enable;
        message[7 + deviceIdLength] = (byte) voltageThreshold;
        message[8 + deviceIdLength] = (byte) timeThreshold;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadOverCurrentProtection(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_OVER_CURRENT_PROTECTION;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteOverCurrentProtection(String deviceId, int enable, int currentThreshold, int timeThreshold) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 3;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_OVER_CURRENT_PROTECTION;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) enable;
        message[7 + deviceIdLength] = (byte) currentThreshold;
        message[8 + deviceIdLength] = (byte) timeThreshold;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadLoadStatusNotify(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_LOAD_NOTIFY_ENABLE;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteLoadStatusNotify(String deviceId, int start, int stop) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 2;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_LOAD_NOTIFY_ENABLE;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) start;
        message[7 + deviceIdLength] = (byte) stop;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadNetConnectingStatus(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_NET_CONNECTING_STATUS;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteNetConnectingStatus(String deviceId, int serverConnectingStatus) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 1;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_NET_CONNECTING_STATUS;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) serverConnectingStatus;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadNetConnectedStatus(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_NET_CONNECTED_STATUS;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteNetConnectedStatus(String deviceId, int serverConnectedStatus) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 1;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_NET_CONNECTED_STATUS;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) serverConnectedStatus;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadPowerStatus(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_POWER_SWITCH_STATUS;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWritePowerStatus(String deviceId, int powerStatus) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 1;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_POWER_SWITCH_STATUS;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) powerStatus;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadPowerProtectStatus(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_POWER_PROTECT;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWritePowerProtectStatus(String deviceId, int powerProtectStatus) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 1;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_POWER_PROTECT;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) powerProtectStatus;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadIndicatorColor(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_INDICATOR_STATUS_COLOR;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteIndicatorColor(String deviceId, int ledState, int blue, int green,
                                                     int yellow, int orange, int red, int purple) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 13;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] blueBytes = MokoUtils.toByteArray(blue, 2);
        byte[] greenBytes = MokoUtils.toByteArray(green, 2);
        byte[] yellowBytes = MokoUtils.toByteArray(yellow, 2);
        byte[] orangeBytes = MokoUtils.toByteArray(orange, 2);
        byte[] redBytes = MokoUtils.toByteArray(red, 2);
        byte[] purpleBytes = MokoUtils.toByteArray(purple, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_INDICATOR_STATUS_COLOR;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) ledState;
        message[7 + deviceIdLength] = blueBytes[0];
        message[8 + deviceIdLength] = blueBytes[1];
        message[9 + deviceIdLength] = greenBytes[0];
        message[10 + deviceIdLength] = greenBytes[1];
        message[11 + deviceIdLength] = yellowBytes[0];
        message[12 + deviceIdLength] = yellowBytes[1];
        message[13 + deviceIdLength] = orangeBytes[0];
        message[14 + deviceIdLength] = orangeBytes[1];
        message[15 + deviceIdLength] = redBytes[0];
        message[16 + deviceIdLength] = redBytes[1];
        message[17 + deviceIdLength] = purpleBytes[0];
        message[18 + deviceIdLength] = purpleBytes[1];
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadDeviceStatus(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.READ_MSG_ID_DEVICE_STATUS;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteMQTTHost(String deviceId, String host) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = host.getBytes().length;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_HOST;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        for (int i = 0; i < dataLength; i++) {
            message[6 + deviceIdLength + i] = host.getBytes()[i];
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteMQTTPort(String deviceId, int port) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 2;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] portBytes = MokoUtils.toByteArray(port, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_PORT;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = portBytes[0];
        message[7 + deviceIdLength] = portBytes[1];
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteMQTTUsername(String deviceId, String username) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = TextUtils.isEmpty(username) ? 0 : username.getBytes().length;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_USERNAME;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        if (dataLength > 0) {
            for (int i = 0; i < dataLength; i++) {
                message[6 + deviceIdLength + i] = username.getBytes()[i];
            }
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteMQTTPassword(String deviceId, String password) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = TextUtils.isEmpty(password) ? 0 : password.getBytes().length;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_PASSWORD;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        if (dataLength > 0) {
            for (int i = 0; i < dataLength; i++) {
                message[6 + deviceIdLength + i] = password.getBytes()[i];
            }
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteMQTTClientId(String deviceId, String clientId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = clientId.getBytes().length;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_CLIENT_ID;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        for (int i = 0; i < dataLength; i++) {
            message[6 + deviceIdLength + i] = clientId.getBytes()[i];
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteMQTTCleanSession(String deviceId, int enable) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 1;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_CLEAN_SESSION;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) enable;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteMQTTKeepAlive(String deviceId, int keepAlive) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 1;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_KEEP_ALIVE;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) keepAlive;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteMQTTQos(String deviceId, int qos) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 1;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_QOS;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) qos;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteMQTTSubscribeTopic(String deviceId, String topic) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = topic.getBytes().length;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_SUBSCRIBE_TOPIC;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        for (int i = 0; i < dataLength; i++) {
            message[6 + deviceIdLength + i] = topic.getBytes()[i];
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteMQTTPublishTopic(String deviceId, String topic) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = topic.getBytes().length;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_PUBLISH_TOPIC;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        for (int i = 0; i < dataLength; i++) {
            message[6 + deviceIdLength + i] = topic.getBytes()[i];
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteLWTEnable(String deviceId, int enable) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 1;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_LWT_ENABLE;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) enable;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteLWTQos(String deviceId, int qos) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 1;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_LWT_QOS;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) qos;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteLWTRetain(String deviceId, int retain) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 1;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_LWT_RETAIN;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) retain;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteLWTTopic(String deviceId, String topic) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = topic.getBytes().length;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_LWT_TOPIC;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        for (int i = 0; i < dataLength; i++) {
            message[6 + deviceIdLength + i] = topic.getBytes()[i];
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteLWTPayload(String deviceId, String payload) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = payload.getBytes().length;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_LWT_MESSAGE;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        for (int i = 0; i < dataLength; i++) {
            message[6 + deviceIdLength + i] = payload.getBytes()[i];
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteMQTTEncryptionType(String deviceId, int type) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 1;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_SSL;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) type;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }


    public static byte[] assembleWriteAPN(String deviceId, String apn) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = TextUtils.isEmpty(apn) ? 0 : apn.getBytes().length;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_APN;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        if (dataLength > 0) {
            for (int i = 0; i < dataLength; i++) {
                message[6 + deviceIdLength + i] = apn.getBytes()[i];
            }
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteAPNUsername(String deviceId, String username) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = TextUtils.isEmpty(username) ? 0 : username.getBytes().length;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_APN_USERNAME;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        if (dataLength > 0) {
            for (int i = 0; i < dataLength; i++) {
                message[6 + deviceIdLength + i] = username.getBytes()[i];
            }
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteAPNPassword(String deviceId, String password) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = TextUtils.isEmpty(password) ? 0 : password.getBytes().length;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_APN_PASSWORD;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        if (dataLength > 0) {
            for (int i = 0; i < dataLength; i++) {
                message[6 + deviceIdLength + i] = password.getBytes()[i];
            }
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteNetworkPriority(String deviceId, int priority) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        int dataLength = 1;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.MSG_ID_NETWORK_PRIORITY;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = (byte) priority;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteCACertFile(String deviceId, String host, int port, String caFilePath) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] hostBytes = host.getBytes();
        int hostLength = hostBytes.length;
        byte[] caFilePathBytes = caFilePath.getBytes();
        int caFilePathLength = caFilePathBytes.length;
        byte[] portBytes = MokoUtils.toByteArray(port, 2);
        int dataLength = 4 + hostLength + caFilePathLength;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.CONFIG_MSG_ID_CA_CERTIFICATE;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = portBytes[0];
        message[7 + deviceIdLength] = portBytes[1];
        message[8 + deviceIdLength] = (byte) hostLength;
        for (int i = 0; i < hostLength; i++) {
            message[9 + deviceIdLength + i] = hostBytes[i];
        }
        message[9 + deviceIdLength + hostLength] = (byte) caFilePathLength;
        for (int i = 0; i < caFilePathLength; i++) {
            message[10 + deviceIdLength + hostLength + i] = caFilePathBytes[i];
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteSelfSingleServerCertificates(String deviceId, String host, int port
            , String caFilePath
            , String certFilePath
            , String keyFilePath) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] hostBytes = host.getBytes();
        int hostLength = hostBytes.length;
        byte[] caFilePathBytes = caFilePath.getBytes();
        int caFilePathLength = caFilePathBytes.length;
        byte[] certFilePathBytes = certFilePath.getBytes();
        int certFilePathLength = certFilePathBytes.length;
        byte[] keyFilePathBytes = keyFilePath.getBytes();
        int keyFilePathLength = keyFilePathBytes.length;
        byte[] portBytes = MokoUtils.toByteArray(port, 2);
        int dataLength = 6 + hostLength + caFilePathLength + certFilePathLength + keyFilePathLength;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.CONFIG_MSG_ID_SELF_SIGNED_SERVER_CERTIFICATES;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = portBytes[0];
        message[7 + deviceIdLength] = portBytes[1];
        message[8 + deviceIdLength] = (byte) hostLength;
        for (int i = 0; i < hostLength; i++) {
            message[9 + deviceIdLength + i] = hostBytes[i];
        }
        message[9 + deviceIdLength + hostLength] = (byte) caFilePathLength;
        for (int i = 0; i < caFilePathLength; i++) {
            message[10 + deviceIdLength + hostLength + i] = caFilePathBytes[i];
        }
        message[10 + deviceIdLength + hostLength + caFilePathLength] = (byte) certFilePathLength;
        for (int i = 0; i < certFilePathLength; i++) {
            message[11 + deviceIdLength + hostLength + caFilePathLength + i] = certFilePathBytes[i];
        }
        message[11 + deviceIdLength + hostLength + caFilePathLength + certFilePathLength] = (byte) keyFilePathLength;
        for (int i = 0; i < keyFilePathLength; i++) {
            message[12 + deviceIdLength + hostLength + caFilePathLength + certFilePathLength + i] = keyFilePathBytes[i];
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteMQTTConfigFinish(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.CONFIG_MSG_ID_MQTT_CONFIG_FINISH;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteDeviceReconnect(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.CONFIG_MSG_ID_MQTT_RECONNECT;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteOTA(String deviceId, String host, int port, String filePath) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] hostBytes = host.getBytes();
        int hostLength = hostBytes.length;
        byte[] filePathBytes = filePath.getBytes();
        int filePathLength = filePathBytes.length;
        byte[] portBytes = MokoUtils.toByteArray(port, 2);
        int dataLength = 4 + hostLength + filePathLength;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.CONFIG_MSG_ID_OTA;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = portBytes[0];
        message[7 + deviceIdLength] = portBytes[1];
        message[8 + deviceIdLength] = (byte) hostLength;
        for (int i = 0; i < hostLength; i++) {
            message[9 + deviceIdLength + i] = hostBytes[i];
        }
        message[9 + deviceIdLength + hostLength] = (byte) filePathLength;
        for (int i = 0; i < filePathLength; i++) {
            message[10 + deviceIdLength + hostLength + i] = filePathBytes[i];
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }


    public static byte[] assembleWriteCaFileOTA(String deviceId, String host, int port, String filePath) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] hostBytes = host.getBytes();
        int hostLength = hostBytes.length;
        byte[] filePathBytes = filePath.getBytes();
        int filePathLength = filePathBytes.length;
        byte[] portBytes = MokoUtils.toByteArray(port, 2);
        int dataLength = 4 + hostLength + filePathLength;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.CONFIG_MSG_ID_OTA_ONE_WAY;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = portBytes[0];
        message[7 + deviceIdLength] = portBytes[1];
        message[8 + deviceIdLength] = (byte) hostLength;
        for (int i = 0; i < hostLength; i++) {
            message[9 + deviceIdLength + i] = hostBytes[i];
        }
        message[9 + deviceIdLength + hostLength] = (byte) filePathLength;
        for (int i = 0; i < filePathLength; i++) {
            message[10 + deviceIdLength + hostLength + i] = filePathBytes[i];
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleWriteSelfSingleCertOTA(String deviceId, String host, int port
            , String caFilePath
            , String certFilePath
            , String keyFilePath) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] hostBytes = host.getBytes();
        int hostLength = hostBytes.length;
        byte[] caFilePathBytes = caFilePath.getBytes();
        int caFilePathLength = caFilePathBytes.length;
        byte[] certFilePathBytes = certFilePath.getBytes();
        int certFilePathLength = certFilePathBytes.length;
        byte[] keyFilePathBytes = keyFilePath.getBytes();
        int keyFilePathLength = keyFilePathBytes.length;
        byte[] portBytes = MokoUtils.toByteArray(port, 2);
        int dataLength = 6 + hostLength + caFilePathLength + certFilePathLength + keyFilePathLength;
        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
        byte[] message = new byte[6 + deviceIdLength + dataLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x01;
        message[2] = (byte) MQTTConstants.CONFIG_MSG_ID_OTA_BOTH_WAY;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = dataLengthBytes[0];
        message[5 + deviceIdLength] = dataLengthBytes[1];
        message[6 + deviceIdLength] = portBytes[0];
        message[7 + deviceIdLength] = portBytes[1];
        message[8 + deviceIdLength] = (byte) hostLength;
        for (int i = 0; i < hostLength; i++) {
            message[9 + deviceIdLength + i] = hostBytes[i];
        }
        message[9 + deviceIdLength + hostLength] = (byte) caFilePathLength;
        for (int i = 0; i < caFilePathLength; i++) {
            message[10 + deviceIdLength + hostLength + i] = caFilePathBytes[i];
        }
        message[10 + deviceIdLength + hostLength + caFilePathLength] = (byte) certFilePathLength;
        for (int i = 0; i < certFilePathLength; i++) {
            message[11 + deviceIdLength + hostLength + caFilePathLength + i] = certFilePathBytes[i];
        }
        message[11 + deviceIdLength + hostLength + caFilePathLength + certFilePathLength] = (byte) keyFilePathLength;
        for (int i = 0; i < keyFilePathLength; i++) {
            message[12 + deviceIdLength + hostLength + caFilePathLength + certFilePathLength + i] = keyFilePathBytes[i];
        }
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }


    public static byte[] assembleReadMQTTHost(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_HOST;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadMQTTPort(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_PORT;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadMQTTUsername(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_USERNAME;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadMQTTPassword(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_PASSWORD;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadMQTTClientId(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_CLIENT_ID;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadMQTTCleanSession(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_CLEAN_SESSION;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadMQTTKeepAlive(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_KEEP_ALIVE;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadMQTTQos(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_QOS;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadMQTTSubscribeTopic(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_SUBSCRIBE_TOPIC;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadMQTTPublishTopic(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_PUBLISH_TOPIC;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadLWTEnable(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_LWT_ENABLE;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadLWTQos(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_LWT_QOS;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadLWTRetain(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_LWT_RETAIN;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadLWTTopic(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_LWT_TOPIC;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadLWTPayload(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_LWT_MESSAGE;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }

    public static byte[] assembleReadMQTTEncryptionType(String deviceId) {
        byte[] deviceIdBytes = deviceId.getBytes();
        int deviceIdLength = deviceId.length();
        byte[] message = new byte[6 + deviceIdLength];
        message[0] = (byte) 0xED;
        message[1] = (byte) 0x00;
        message[2] = (byte) MQTTConstants.MSG_ID_MQTT_SSL;
        message[3] = (byte) deviceIdLength;
        for (int i = 0; i < deviceIdBytes.length; i++) {
            message[i + 4] = deviceIdBytes[i];
        }
        message[4 + deviceIdLength] = 0;
        message[5 + deviceIdLength] = 0;
        XLog.e("app_to_device--->" + MokoUtils.bytesToHexString(message));
        return message;
    }
}
