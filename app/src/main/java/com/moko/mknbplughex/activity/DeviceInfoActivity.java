package com.moko.mknbplughex.activity;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import com.google.gson.Gson;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplughex.AppConstants;
import com.moko.mknbplughex.base.BaseActivity;
import com.moko.mknbplughex.databinding.ActivityDeviceInfoBinding;
import com.moko.mknbplughex.entity.MokoDevice;
import com.moko.mknbplughex.utils.SPUtils;
import com.moko.support.hex.MQTTConstants;
import com.moko.support.hex.MQTTMessageAssembler;
import com.moko.support.hex.MQTTSupport;
import com.moko.support.hex.entity.MQTTConfig;
import com.moko.support.hex.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;

public class DeviceInfoActivity extends BaseActivity<ActivityDeviceInfoBinding> {
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    public Handler mHandler;

    @Override
    protected void onCreate() {
        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        mHandler = new Handler(Looper.getMainLooper());
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getManufacturer();
    }

    @Override
    protected ActivityDeviceInfoBinding getViewBinding() {
        return ActivityDeviceInfoBinding.inflate(getLayoutInflater());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        // 更新所有设备的网络状态
        final byte[] message = event.getMessage();
        if (message.length < 8) return;
        int header = message[0] & 0xFF;// 0xED
        int flag = message[1] & 0xFF;// read or write
        int cmd = message[2] & 0xFF;
        int deviceIdLength = message[3] & 0xFF;
        String deviceId = new String(Arrays.copyOfRange(message, 4, 4 + deviceIdLength));
        int dataLength = MokoUtils.toInt(Arrays.copyOfRange(message, 4 + deviceIdLength, 6 + deviceIdLength));
        byte[] data = Arrays.copyOfRange(message, 6 + deviceIdLength, 6 + deviceIdLength + dataLength);
        if (header != 0xED) return;
        if (!mMokoDevice.mac.equalsIgnoreCase(deviceId)) return;
        mMokoDevice.isOnline = true;
        if (cmd == MQTTConstants.READ_MSG_ID_MANUFACTURER) {
            if (dataLength == 0) return;
            mBind.tvManufacturer.setText(new String(data));
            getProductModel();
        }
        if (cmd == MQTTConstants.READ_MSG_ID_PRODUCT_MODEL) {
            if (dataLength == 0) return;
            mBind.tvProductModel.setText(new String(data));
            getHardwareVersion();
        }
        if (cmd == MQTTConstants.READ_MSG_ID_HARDWARE_VERSION) {
            if (dataLength == 0) return;
            mBind.tvDeviceHardwareVersion.setText(new String(data));
            getFirmwareVersion();
        }
        if (cmd == MQTTConstants.READ_MSG_ID_FIRMWARE_VERSION) {
            if (dataLength == 0) return;
            mBind.tvDeviceFirmwareVersion.setText(new String(data));
            getMac();
        }
        if (cmd == MQTTConstants.READ_MSG_ID_MAC) {
            if (dataLength == 0) return;
            StringBuilder macSB = new StringBuilder(MokoUtils.bytesToHexString(data));
            macSB.insert(2, ":");
            macSB.insert(5, ":");
            macSB.insert(8, ":");
            macSB.insert(11, ":");
            macSB.insert(14, ":");
            mBind.tvDeviceMac.setText(macSB.toString().toUpperCase());
            getIMEI();
        }
        if (cmd == MQTTConstants.READ_MSG_ID_IMEI) {
            if (dataLength == 0) return;
            mBind.tvDeviceImei.setText(new String(data).toUpperCase());
            getICCID();
        }
        if (cmd == MQTTConstants.READ_MSG_ID_ICCID) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength == 0) return;
            mBind.tvDeviceIccid.setText(new String(data).toUpperCase());
        }
        if (cmd == MQTTConstants.NOTIFY_MSG_ID_OVERLOAD_OCCUR
                || cmd == MQTTConstants.NOTIFY_MSG_ID_OVER_VOLTAGE_OCCUR
                || cmd == MQTTConstants.NOTIFY_MSG_ID_UNDER_VOLTAGE_OCCUR
                || cmd == MQTTConstants.NOTIFY_MSG_ID_OVER_CURRENT_OCCUR) {
            if (dataLength != 6) return;
            if (data[5] == 1) finish();
        }
    }

    public void onBack(View view) {
        finish();
    }

    private void getManufacturer() {
        byte[] message = MQTTMessageAssembler.assembleReadManufacturer(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getProductModel() {
        byte[] message = MQTTMessageAssembler.assembleReadProductModel(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getHardwareVersion() {
        byte[] message = MQTTMessageAssembler.assembleReadHardwareVersion(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getFirmwareVersion() {
        byte[] message = MQTTMessageAssembler.assembleReadFirmwareVersion(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getMac() {
        byte[] message = MQTTMessageAssembler.assembleReadMac(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getIMEI() {
        byte[] message = MQTTMessageAssembler.assembleReadIMEI(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getICCID() {
        byte[] message = MQTTMessageAssembler.assembleReadICCID(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private String getAppTopTic() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        return appTopic;
    }
}
