package com.moko.mknbplughex.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.google.gson.Gson;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplughex.AppConstants;
import com.moko.mknbplughex.R;
import com.moko.mknbplughex.R2;
import com.moko.mknbplughex.base.BaseActivity;
import com.moko.mknbplughex.entity.MokoDevice;
import com.moko.mknbplughex.utils.SPUtils;
import com.moko.mknbplughex.utils.ToastUtils;
import com.moko.support.hex.MQTTConstants;
import com.moko.support.hex.MQTTMessageAssembler;
import com.moko.support.hex.MQTTSupport;
import com.moko.support.hex.entity.MQTTConfig;
import com.moko.support.hex.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;


public class OverCurrentProtectionActivity extends BaseActivity {


    @BindView(R2.id.cb_overcurrent_protection)
    CheckBox cbOvercurrentProtection;
    @BindView(R2.id.et_current_threshold)
    EditText etCurrentThreshold;
    @BindView(R2.id.et_time_threshold)
    EditText etTimeThreshold;
    private MQTTConfig appMqttConfig;
    private MokoDevice mMokoDevice;
    private Handler mHandler;
    private int mDeviceType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overcurrent_protection);
        ButterKnife.bind(this);
        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        mDeviceType = getIntent().getIntExtra(AppConstants.EXTRA_KEY_DEVICE_TYPE, 0);
        if (mDeviceType == 0) {
            etCurrentThreshold.setHint("1-192");
        } else if (mDeviceType == 1) {
            etCurrentThreshold.setHint("1-180");
        } else {
            etCurrentThreshold.setHint("1-156");
        }
        mHandler = new Handler(Looper.getMainLooper());
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getOverCurrentProtection();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        // ?????????????????????????????????
        final String topic = event.getTopic();
        final byte[] message = event.getMessage();
        if (message.length < 8)
            return;
        int header = message[0] & 0xFF;// 0xED
        int flag = message[1] & 0xFF;// read or write
        int cmd = message[2] & 0xFF;
        int deviceIdLength = message[3] & 0xFF;
        String deviceId = new String(Arrays.copyOfRange(message, 4, 4 + deviceIdLength));
        int dataLength = MokoUtils.toInt(Arrays.copyOfRange(message, 4 + deviceIdLength, 6 + deviceIdLength));
        byte[] data = Arrays.copyOfRange(message, 6 + deviceIdLength, 6 + deviceIdLength + dataLength);
        if (header != 0xED)
            return;
        if (!mMokoDevice.deviceId.equals(deviceId))
            return;
        mMokoDevice.isOnline = true;
        if (cmd == MQTTConstants.MSG_ID_OVER_CURRENT_PROTECTION && flag == 0) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 3)
                return;
            cbOvercurrentProtection.setChecked(data[0] == 1);
            etCurrentThreshold.setText(String.valueOf(data[1] & 0xFF));
            etTimeThreshold.setText(String.valueOf(data[2] & 0xFF));
        }
        if (cmd == MQTTConstants.MSG_ID_OVER_CURRENT_PROTECTION && flag == 1) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 1)
                return;
            if (data[0] == 0) {
                ToastUtils.showToast(this, "Set up failed");
                return;
            }
            ToastUtils.showToast(this, "Set up succeed");
        }
        if (cmd == MQTTConstants.NOTIFY_MSG_ID_OVERLOAD_OCCUR
                || cmd == MQTTConstants.NOTIFY_MSG_ID_OVER_VOLTAGE_OCCUR
                || cmd == MQTTConstants.NOTIFY_MSG_ID_UNDER_VOLTAGE_OCCUR
                || cmd == MQTTConstants.NOTIFY_MSG_ID_OVER_CURRENT_OCCUR) {
            if (dataLength != 6)
                return;
            if (data[5] == 1)
                finish();
        }
    }


//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
//        String deviceId = event.getDeviceId();
//        if (!mMokoDevice.deviceId.equals(deviceId)) {
//            return;
//        }
//        boolean online = event.isOnline();
//        if (!online)
//            finish();
//    }


    public void onBack(View view) {
        finish();
    }


    private void getOverCurrentProtection() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadOverCurrentProtection(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onSave(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        int max = 192;
        if (mDeviceType == 1) {
            max = 180;
        } else if (mDeviceType == 2) {
            max = 156;
        }
        String currentThresholdStr = etCurrentThreshold.getText().toString();
        if (TextUtils.isEmpty(currentThresholdStr)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        int currentThreshold = Integer.parseInt(currentThresholdStr);
        if (currentThreshold < 1 || currentThreshold > max) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        String timeThresholdStr = etTimeThreshold.getText().toString();
        if (TextUtils.isEmpty(timeThresholdStr)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        int timeThreshold = Integer.parseInt(timeThresholdStr);
        if (timeThreshold < 1 || timeThreshold > 30) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        setOverloadProtection(currentThreshold, timeThreshold);
    }

    private void setOverloadProtection(int currentThreshold, int timeThreshold) {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteOverCurrentProtection(mMokoDevice.deviceId, cbOvercurrentProtection.isChecked() ? 1 : 0, currentThreshold, timeThreshold);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
