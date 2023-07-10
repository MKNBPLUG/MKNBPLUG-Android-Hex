package com.moko.mknbplughex.activity;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import com.google.gson.Gson;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplughex.AppConstants;
import com.moko.mknbplughex.R;
import com.moko.mknbplughex.base.BaseActivity;
import com.moko.mknbplughex.databinding.ActivityEnergyStorageReportBinding;
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

public class EnergyStorageReportActivity extends BaseActivity<ActivityEnergyStorageReportBinding> {
    private MQTTConfig appMqttConfig;
    private MokoDevice mMokoDevice;
    private Handler mHandler;

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
        getEnergyStorageReport();
    }

    @Override
    protected ActivityEnergyStorageReportBinding getViewBinding() {
        return ActivityEnergyStorageReportBinding.inflate(getLayoutInflater());
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
        if (cmd == MQTTConstants.MSG_ID_ENERGY_REPORT_PARAMS && flag == 0) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 4) return;
            mBind.etEnergyStorageInterval.setText(String.valueOf(data[0] & 0xFF));
            mBind.etPowerChangeThreshold.setText(String.valueOf(data[1] & 0xFF));
            mBind.etEnergyReportInterval.setText(String.valueOf(MokoUtils.toInt(Arrays.copyOfRange(data, 2, 4))));
        }
        if (cmd == MQTTConstants.MSG_ID_ENERGY_REPORT_PARAMS && flag == 1) {
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

    private void getEnergyStorageReport() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadEnergyReportParams(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setEnergyStorageReport(int storageInterval, int storageThreshold, int reportInterval) {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteEnergyReportParams(mMokoDevice.deviceId, storageInterval, storageThreshold, reportInterval);
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
        if (isValid()) {
            String energyStorageIntervalStr = mBind.etEnergyStorageInterval.getText().toString();
            String powerChangeThresholdStr = mBind.etPowerChangeThreshold.getText().toString();
            String energyReportIntervalStr = mBind.etEnergyReportInterval.getText().toString();
            int energyStorageInterval = Integer.parseInt(energyStorageIntervalStr);
            int powerChangeThreshold = Integer.parseInt(powerChangeThresholdStr);
            int energyReportInterval = Integer.parseInt(energyReportIntervalStr);
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            setEnergyStorageReport(energyStorageInterval, powerChangeThreshold, energyReportInterval);
        } else {
            ToastUtils.showToast(this, "Para Error");
        }
    }

    private boolean isValid() {
        String energyStorageIntervalStr = mBind.etEnergyStorageInterval.getText().toString();
        String powerChangeThresholdStr = mBind.etPowerChangeThreshold.getText().toString();
        String energyReportIntervalStr = mBind.etEnergyReportInterval.getText().toString();
        if (TextUtils.isEmpty(energyStorageIntervalStr)
                || TextUtils.isEmpty(powerChangeThresholdStr)
                || TextUtils.isEmpty(energyReportIntervalStr)) {
            return false;
        }
        int energyStorageInterval = Integer.parseInt(energyStorageIntervalStr);
        if (energyStorageInterval < 1 || energyStorageInterval > 60) return false;
        int powerChangeThreshold = Integer.parseInt(powerChangeThresholdStr);
        if (powerChangeThreshold < 1 || powerChangeThreshold > 100) return false;
        int energyReportInterval = Integer.parseInt(energyReportIntervalStr);
        return energyReportInterval <= 43200;
    }
}
