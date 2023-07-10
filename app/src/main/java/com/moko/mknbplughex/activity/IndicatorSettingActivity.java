package com.moko.mknbplughex.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import com.google.gson.Gson;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplughex.AppConstants;
import com.moko.mknbplughex.R;
import com.moko.mknbplughex.base.BaseActivity;
import com.moko.mknbplughex.databinding.ActivityIndicatorSettingBinding;
import com.moko.mknbplughex.dialog.BottomDialog;
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

import java.util.ArrayList;
import java.util.Arrays;

public class IndicatorSettingActivity extends BaseActivity<ActivityIndicatorSettingBinding> {
    private MQTTConfig appMqttConfig;
    private MokoDevice mMokoDevice;
    private Handler mHandler;
    private boolean mServerConnectingStatus;
    private int mServerConnectedSelected;
    private boolean mPowerStatus;
    private boolean mPowerProtectStatus;
    private ArrayList<String> mServerConnectedValues;
    private int mDeviceType;

    @Override
    protected void onCreate() {
        mServerConnectedValues = new ArrayList<>();
        mServerConnectedValues.add("OFF");
        mServerConnectedValues.add("Solid blue for 5 seconds");
        mServerConnectedValues.add("Solid blue");
        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        mHandler = new Handler(Looper.getMainLooper());
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getServerConnectingStatus();
        getServerConnectedStatus();
        getPowerStatus();
        getPowerProtectStatus();
        getDeviceType();
    }

    @Override
    protected ActivityIndicatorSettingBinding getViewBinding() {
        return ActivityIndicatorSettingBinding.inflate(getLayoutInflater());
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
        if (!mMokoDevice.deviceId.equals(deviceId)) return;
        mMokoDevice.isOnline = true;
        if (cmd == MQTTConstants.MSG_ID_NET_CONNECTING_STATUS && flag == 0) {
            if (dataLength != 1) return;
            mServerConnectingStatus = data[0] == 1;
            mBind.ivServerConnecting.setImageResource(mServerConnectingStatus ? R.drawable.checkbox_open : R.drawable.checkbox_close);
        }
        if (cmd == MQTTConstants.MSG_ID_NET_CONNECTED_STATUS && flag == 0) {
            if (dataLength != 1) return;
            mServerConnectedSelected = data[0];
            mBind.tvServerConnected.setText(mServerConnectedValues.get(mServerConnectedSelected));
        }
        if (cmd == MQTTConstants.MSG_ID_POWER_SWITCH_STATUS && flag == 0) {
            if (dataLength != 1) return;
            mPowerStatus = data[0] == 1;
            mBind.ivIndicatorStatus.setImageResource(mPowerStatus ? R.drawable.checkbox_open : R.drawable.checkbox_close);
            mBind.tvIndicatorColor.setVisibility(mPowerStatus ? View.VISIBLE : View.GONE);
        }
        if (cmd == MQTTConstants.MSG_ID_POWER_PROTECT && flag == 0) {
            if (dataLength != 1) return;
            mPowerProtectStatus = data[0] == 1;
            mBind.ivProtectionSignal.setImageResource(mPowerProtectStatus ? R.drawable.checkbox_open : R.drawable.checkbox_close);
        }
        if (cmd == MQTTConstants.READ_MSG_ID_DEVICE_TYPE) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 1) return;
            mDeviceType = data[0];
        }
        if (flag == 1 && (cmd == MQTTConstants.MSG_ID_NET_CONNECTING_STATUS
                || cmd == MQTTConstants.MSG_ID_NET_CONNECTED_STATUS
                || cmd == MQTTConstants.MSG_ID_POWER_SWITCH_STATUS
                || cmd == MQTTConstants.MSG_ID_POWER_PROTECT)) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 1) return;
            if (data[0] == 0) {
                ToastUtils.showToast(this, "Set up failed");
                return;
            }
            ToastUtils.showToast(this, "Set up succeed");
            if (cmd == MQTTConstants.MSG_ID_NET_CONNECTING_STATUS)
                mBind.ivServerConnecting.setImageResource(mServerConnectingStatus ? R.drawable.checkbox_open : R.drawable.checkbox_close);
            if (cmd == MQTTConstants.MSG_ID_NET_CONNECTED_STATUS)
                mBind.tvServerConnected.setText(mServerConnectedValues.get(mServerConnectedSelected));
            if (cmd == MQTTConstants.MSG_ID_POWER_SWITCH_STATUS) {
                mBind.ivIndicatorStatus.setImageResource(mPowerStatus ? R.drawable.checkbox_open : R.drawable.checkbox_close);
                mBind.tvIndicatorColor.setVisibility(mPowerStatus ? View.VISIBLE : View.GONE);
            }
            if (cmd == MQTTConstants.MSG_ID_POWER_PROTECT)
                mBind.ivProtectionSignal.setImageResource(mPowerProtectStatus ? R.drawable.checkbox_open : R.drawable.checkbox_close);
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

    private void getServerConnectingStatus() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadNetConnectingStatus(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getServerConnectedStatus() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadNetConnectedStatus(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getPowerStatus() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadPowerStatus(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getPowerProtectStatus() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadPowerProtectStatus(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getDeviceType() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadDeviceType(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setServerConnectingStatus() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteNetConnectingStatus(mMokoDevice.deviceId, mServerConnectingStatus ? 1 : 0);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setServerConnectedStatus() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteNetConnectedStatus(mMokoDevice.deviceId, mServerConnectedSelected);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setPowerStatus() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWritePowerStatus(mMokoDevice.deviceId, mPowerStatus ? 1 : 0);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setPowerProtectStatus() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWritePowerProtectStatus(mMokoDevice.deviceId, mPowerProtectStatus ? 1 : 0);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    public void onServerConnecting(View view) {
        if (isWindowLocked()) return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        mServerConnectingStatus = !mServerConnectingStatus;
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        setServerConnectingStatus();
    }

    public void onSelectServerConnected(View view) {
        if (isWindowLocked()) return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mServerConnectedValues, mServerConnectedSelected);
        dialog.setListener(value -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(this, R.string.network_error);
                return;
            }
            mServerConnectedSelected = value;
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            setServerConnectedStatus();
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onIndicatorStatus(View view) {
        if (isWindowLocked()) return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, IndicatorColorActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE_TYPE, mDeviceType);
        startActivity(i);
    }


    public void onPowerStatus(View view) {
        if (isWindowLocked()) return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        mPowerStatus = !mPowerStatus;
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        setPowerStatus();
    }

    public void onProtectionSignal(View view) {
        if (isWindowLocked()) return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        mPowerProtectStatus = !mPowerProtectStatus;
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        setPowerProtectStatus();
    }
}
