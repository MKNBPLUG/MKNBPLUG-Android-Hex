package com.moko.mknbplughex.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;

import com.google.gson.Gson;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplughex.AppConstants;
import com.moko.mknbplughex.base.BaseActivity;
import com.moko.mknbplughex.databinding.ActivityAddDeviceBinding;
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
 * @date: 2023/7/5 20:14
 * @des: add device
 */
public class AddDeviceActivity extends BaseActivity<ActivityAddDeviceBinding> {
    private final String FILTER_ASCII = "[ -~]*";
    private MQTTConfig appMqttConfig;
    private Handler mHandler;
    private String mac;

    @Override
    protected void onCreate() {
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }
            return null;
        };
        mBind.etSubscribeTopic.setFilters(new InputFilter[]{filter, new InputFilter.LengthFilter(128)});
        mBind.etPublishTopic.setFilters(new InputFilter[]{filter, new InputFilter.LengthFilter(128)});
        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected ActivityAddDeviceBinding getViewBinding() {
        return ActivityAddDeviceBinding.inflate(getLayoutInflater());
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
        if (!mac.equalsIgnoreCase(deviceId)) return;
        if (cmd == MQTTConstants.READ_MSG_ID_SWITCH_INFO) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 6) {
                ToastUtils.showToast(this, "Add device failed！");
                return;
            }
            int switch_state = data[0];
            if (switch_state == 1) {
                //设备添加成功
                Intent intent = new Intent(this, AddDeviceSuccessActivity.class);
                intent.putExtra("mac", mac);
                intent.putExtra("subscribe", mBind.etSubscribeTopic.getText().toString());
                intent.putExtra("publish", mBind.etPublishTopic.getText().toString());
                startActivity(intent);
                finish();
            } else {
                ToastUtils.showToast(this, "Add device failed！");
            }
        }
    }

    public void onBack(View view) {
        finish();
    }

    public void onSave(View view) {
        if (isWindowLocked()) return;
        if (isValid()) {
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Add device failed！");
            }, 90 * 1000);
            mac = mBind.etMac.getText().toString().toUpperCase();
            byte[] message = MQTTMessageAssembler.assembleReadSwitchInfo(mac);
            try {
                MQTTSupport.getInstance().publish(mBind.etSubscribeTopic.getText().toString(), message, appMqttConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            ToastUtils.showToast(this, "Para Error");
        }
    }

    private boolean isValid() {
        if (TextUtils.isEmpty(mBind.etMac.getText()) || mBind.etMac.getText().length() != 12)
            return false;
        if (TextUtils.isEmpty(mBind.etSubscribeTopic.getText())) return false;
        return !TextUtils.isEmpty(mBind.etPublishTopic.getText());
    }
}
