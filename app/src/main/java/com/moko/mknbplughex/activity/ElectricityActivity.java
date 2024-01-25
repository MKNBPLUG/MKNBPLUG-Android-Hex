package com.moko.mknbplughex.activity;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplughex.AppConstants;
import com.moko.mknbplughex.base.BaseActivity;
import com.moko.mknbplughex.databinding.ActivityElectricityManagerBinding;
import com.moko.mknbplughex.entity.MokoDevice;
import com.moko.mknbplughex.utils.SPUtils;
import com.moko.mknbplughex.utils.Utils;
import com.moko.support.hex.MQTTConstants;
import com.moko.support.hex.MQTTMessageAssembler;
import com.moko.support.hex.MQTTSupport;
import com.moko.support.hex.entity.MQTTConfig;
import com.moko.support.hex.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;

public class ElectricityActivity extends BaseActivity<ActivityElectricityManagerBinding> {
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
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
        getElectricity();
    }

    @Override
    protected ActivityElectricityManagerBinding getViewBinding() {
        return ActivityElectricityManagerBinding.inflate(getLayoutInflater());
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
        if (cmd == MQTTConstants.READ_MSG_ID_POWER_INFO) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 11) return;
            float voltage = MokoUtils.toInt(Arrays.copyOfRange(data, 0, 2)) * 0.1f;
            int current = MokoUtils.toIntSigned(Arrays.copyOfRange(data, 2, 4));
            float power = MokoUtils.toInt(Arrays.copyOfRange(data, 4, 8)) * 0.1f;
            float frequency = MokoUtils.toInt(Arrays.copyOfRange(data, 8, 10)) * 0.01f;
            float power_factor = data[10] * 0.01f;
            mBind.tvCurrent.setText(String.valueOf(current));
            mBind.tvVoltage.setText(Utils.getDecimalFormat("0.#").format(voltage));
            mBind.tvPower.setText(Utils.getDecimalFormat("0.#").format(power));
            mBind.tvPowerFactor.setText(Utils.getDecimalFormat("0.##").format(power_factor));
            mBind.tvFrequency.setText(Utils.getDecimalFormat("0.##").format(frequency));
        }
        if (cmd == MQTTConstants.NOTIFY_MSG_ID_POWER_INFO) {
            if (dataLength != 16) return;
            float voltage = MokoUtils.toInt(Arrays.copyOfRange(data, 5, 7)) * 0.1f;
            int current = MokoUtils.toIntSigned(Arrays.copyOfRange(data, 7, 9));
            float power = MokoUtils.toInt(Arrays.copyOfRange(data, 9, 13)) * 0.1f;
            float frequency = MokoUtils.toInt(Arrays.copyOfRange(data, 13, 15)) * 0.01f;
            float power_factor = data[15] * 0.01f;
            mBind.tvCurrent.setText(String.valueOf(current));
            mBind.tvVoltage.setText(Utils.getDecimalFormat("0.#").format(voltage));
            mBind.tvPower.setText(Utils.getDecimalFormat("0.#").format(power));
            mBind.tvPowerFactor.setText(Utils.getDecimalFormat("0.##").format(power_factor));
            mBind.tvFrequency.setText(Utils.getDecimalFormat("0.##").format(frequency));
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

    private void getElectricity() {
        XLog.i("读取电量数据");
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadPowerInfo(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
