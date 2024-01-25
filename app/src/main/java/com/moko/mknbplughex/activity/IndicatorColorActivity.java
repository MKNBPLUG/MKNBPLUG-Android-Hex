package com.moko.mknbplughex.activity;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplughex.AppConstants;
import com.moko.mknbplughex.R;
import com.moko.mknbplughex.base.BaseActivity;
import com.moko.mknbplughex.databinding.ActivityIndicatorColorBinding;
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

import cn.carbswang.android.numberpickerview.library.NumberPickerView;

public class IndicatorColorActivity extends BaseActivity<ActivityIndicatorColorBinding> implements NumberPickerView.OnValueChangeListener {
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private Handler mHandler;
    private int maxValue = 4416;

    @Override
    protected void onCreate() {
        if (getIntent().getExtras() != null) {
            mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        }
        int deviceType = getIntent().getIntExtra(AppConstants.EXTRA_KEY_DEVICE_TYPE, 0);
        if (deviceType == 1) {
            maxValue = 2160;
        }
        if (deviceType == 2) {
            maxValue = 3588;
        }
        mBind.npvColorSettings.setMinValue(0);
        mBind.npvColorSettings.setMaxValue(8);
        mBind.npvColorSettings.setValue(0);
        mBind.npvColorSettings.setOnValueChangedListener(this);
        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mHandler = new Handler(Looper.getMainLooper());
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getColorSettings();
    }

    @Override
    protected ActivityIndicatorColorBinding getViewBinding() {
        return ActivityIndicatorColorBinding.inflate(getLayoutInflater());
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
        String deviceId = MokoUtils.bytesToHexString(Arrays.copyOfRange(message, 4, 4 + deviceIdLength));
        int dataLength = MokoUtils.toInt(Arrays.copyOfRange(message, 4 + deviceIdLength, 6 + deviceIdLength));
        byte[] data = Arrays.copyOfRange(message, 6 + deviceIdLength, 6 + deviceIdLength + dataLength);
        if (header != 0xED) return;
        if (!mMokoDevice.mac.equalsIgnoreCase(deviceId)) return;
        mMokoDevice.isOnline = true;
        if (cmd == MQTTConstants.MSG_ID_INDICATOR_STATUS_COLOR && flag == 0) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 13) return;
            mBind.npvColorSettings.setValue(data[0] & 0xFF);
            if ((data[0] & 0xFF) > 1) {
                mBind.llColorSettings.setVisibility(View.GONE);
            } else {
                mBind.llColorSettings.setVisibility(View.VISIBLE);
            }
            mBind.etBlue.setText(String.valueOf(MokoUtils.toInt(Arrays.copyOfRange(data, 1, 3))));
            mBind.etGreen.setText(String.valueOf(MokoUtils.toInt(Arrays.copyOfRange(data, 3, 5))));
            mBind.etYellow.setText(String.valueOf(MokoUtils.toInt(Arrays.copyOfRange(data, 5, 7))));
            mBind.etOrange.setText(String.valueOf(MokoUtils.toInt(Arrays.copyOfRange(data, 7, 9))));
            mBind.etRed.setText(String.valueOf(MokoUtils.toInt(Arrays.copyOfRange(data, 9, 11))));
            mBind.etPurple.setText(String.valueOf(MokoUtils.toInt(Arrays.copyOfRange(data, 11, 13))));
            mBind.etBlue.setSelection(mBind.etBlue.getText().length());
            mBind.etGreen.setSelection(mBind.etGreen.getText().length());
            mBind.etYellow.setSelection(mBind.etYellow.getText().length());
            mBind.etOrange.setSelection(mBind.etOrange.getText().length());
            mBind.etRed.setSelection(mBind.etRed.getText().length());
            mBind.etPurple.setSelection(mBind.etPurple.getText().length());
        }
        if (cmd == MQTTConstants.MSG_ID_INDICATOR_STATUS_COLOR && flag == 1) {
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

    private void getColorSettings() {
        XLog.i("读取颜色范围");
        byte[] message = MQTTMessageAssembler.assembleReadIndicatorColor(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onValueChange(NumberPickerView picker, int oldVal, int newVal) {
        if (newVal > 1) {
            mBind.llColorSettings.setVisibility(View.GONE);
        } else {
            mBind.llColorSettings.setVisibility(View.VISIBLE);
        }
    }

    public void onSave(View view) {
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        setLEDColor();
    }

    private void setLEDColor() {
        String blue = mBind.etBlue.getText().toString();
        String green = mBind.etGreen.getText().toString();
        String yellow = mBind.etYellow.getText().toString();
        String orange = mBind.etOrange.getText().toString();
        String red = mBind.etRed.getText().toString();
        String purple = mBind.etPurple.getText().toString();
        if (TextUtils.isEmpty(blue)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        if (TextUtils.isEmpty(green)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        if (TextUtils.isEmpty(yellow)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        if (TextUtils.isEmpty(orange)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        if (TextUtils.isEmpty(red)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        if (TextUtils.isEmpty(purple)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        int blueValue = Integer.parseInt(blue);
        if (blueValue < 2 || blueValue > (maxValue - 5)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }

        int greenValue = Integer.parseInt(green);
        if (greenValue <= blueValue || greenValue > (maxValue - 4)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }

        int yellowValue = Integer.parseInt(yellow);
        if (yellowValue <= greenValue || yellowValue > (maxValue - 3)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }

        int orangeValue = Integer.parseInt(orange);
        if (orangeValue <= yellowValue || orangeValue > (maxValue - 2)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }

        int redValue = Integer.parseInt(red);
        if (redValue <= orangeValue || redValue > (maxValue - 1)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }

        int purpleValue = Integer.parseInt(purple);
        if (purpleValue <= redValue || purpleValue > maxValue) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        XLog.i("设置颜色范围");
        byte[] message = MQTTMessageAssembler.assembleWriteIndicatorColor(mMokoDevice.mac, mBind.npvColorSettings.getValue()
                , blueValue, greenValue, yellowValue, orangeValue, redValue, purpleValue);
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
