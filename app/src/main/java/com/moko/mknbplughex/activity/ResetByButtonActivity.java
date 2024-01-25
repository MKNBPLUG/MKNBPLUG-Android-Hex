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
import com.moko.mknbplughex.databinding.ActivityResetByButtonBinding;
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

/**
 * @author: jun.liu
 * @date: 2023/7/10 10:38
 * @des:
 */
public class ResetByButtonActivity extends BaseActivity<ActivityResetByButtonBinding> {
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private Handler mHandler;
    private int resetType;

    @Override
    protected ActivityResetByButtonBinding getViewBinding() {
        return ActivityResetByButtonBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreate() {
        if (getIntent().getExtras() != null) {
            mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        }
        mHandler = new Handler(Looper.getMainLooper());
        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        //首先读取设备的状态
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getResetByButton();
        mBind.imgMinute.setOnClickListener(v -> {
            if (resetType == 0) return;
            resetType = 0;
            setResetByButton(0);
        });
        mBind.imgAnyTime.setOnClickListener(v -> {
            if (resetType == 1) return;
            resetType = 1;
            setResetByButton(1);
        });
    }

    private void setResetByButton(int type) {
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        byte[] message = MQTTMessageAssembler.assembleWriteResetByButton(mMokoDevice.mac, type);
        try {
            MQTTSupport.getInstance().publish(getTopic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getResetByButton() {
        byte[] message = MQTTMessageAssembler.assembleReadResetByButton(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getTopic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
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
        if (cmd == MQTTConstants.MSG_ID_RESET_BY_BUTTON && flag == 0) {
            //读取设备工作模式
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 1) {
                ToastUtils.showToast(this, "fail");
                finish();
                return;
            }
            setImgType(data[0]);
            resetType = data[0];
        }
        if (cmd == MQTTConstants.MSG_ID_RESET_BY_BUTTON && flag == 1) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 1 || data[0] == 0) {
                ToastUtils.showToast(this, "Set up failed");
                return;
            }
            ToastUtils.showToast(this, "Set up succeed");
            setImgType(resetType);
        }
    }

    private String getTopic() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        return appTopic;
    }

    private void setImgType(int type) {
        if (type == 1) {
            mBind.imgMinute.setImageResource(R.drawable.checkbox_close);
            mBind.imgAnyTime.setImageResource(R.drawable.checkbox_open);
        } else {
            mBind.imgMinute.setImageResource(R.drawable.checkbox_open);
            mBind.imgAnyTime.setImageResource(R.drawable.checkbox_close);
        }
    }

    public void onBack(View view) {
        finish();
    }
}
