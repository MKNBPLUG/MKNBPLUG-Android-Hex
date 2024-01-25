package com.moko.mknbplughex.activity;

import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplughex.AppConstants;
import com.moko.mknbplughex.R;
import com.moko.mknbplughex.base.BaseActivity;
import com.moko.mknbplughex.databinding.ActivityOtaBinding;
import com.moko.mknbplughex.dialog.AlertMessageDialog;
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

public class OTAActivity extends BaseActivity<ActivityOtaBinding> {
    private final String FILTER_ASCII = "[ -~]*";
    public static String TAG = OTAActivity.class.getSimpleName();
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private Handler mHandler;

    @Override
    protected void onCreate() {
        if (getIntent().getExtras() != null) {
            mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        }
        InputFilter inputFilter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }
            return null;
        };
        mBind.etOtaUrl.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256), inputFilter});
        mHandler = new Handler(Looper.getMainLooper());
        String mqttConfigAppStr = SPUtils.getStringValue(OTAActivity.this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        //首先读取设备的状态
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        readDeviceStatus();
    }

    @Override
    protected ActivityOtaBinding getViewBinding() {
        return ActivityOtaBinding.inflate(getLayoutInflater());
    }

    private void readDeviceStatus() {
        byte[] message = MQTTMessageAssembler.assembleReadDeviceWorkMode(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
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
        if (cmd == MQTTConstants.READ_MSG_ID_WORK_MODE) {
            //读取设备工作模式
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 1) {
                ToastUtils.showToast(this, "get work mode fail");
                finish();
                return;
            }
            if (data[0] == 1) {
                //debug mode
                AlertMessageDialog dialog = new AlertMessageDialog();
                dialog.setMessage("Device is in debug mode, OTA is unvailable!");
                dialog.setCancelGone();
                dialog.setConfirm("OK");
                dialog.setOnAlertConfirmListener(this::finish);
                dialog.show(getSupportFragmentManager());
            }
        }

        if (cmd == MQTTConstants.READ_MSG_ID_DEVICE_STATUS) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 1) return;
            if (data[0] != 0) {
                ToastUtils.showToast(this, "Device is OTA, please wait");
                return;
            }
            XLog.i("升级固件");
            mHandler.postDelayed(() -> {
                dismissLoadingMessageDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 190 * 1000);
            showLoadingMessageDialog("waiting");
            setOTAFirmware();
        }
        if (cmd == MQTTConstants.NOTIFY_MSG_ID_OTA_RESULT) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingMessageDialog();
                mHandler.removeMessages(0);
            }
            if (data[6] == 1) {
                ToastUtils.showToast(this, R.string.update_success);
            } else {
                ToastUtils.showToast(this, R.string.update_failed);
            }
        }
        if (cmd == MQTTConstants.CONFIG_MSG_ID_OTA) {
            if (dataLength != 1) return;
            if (data[0] == 0) {
                dismissLoadingMessageDialog();
                ToastUtils.showToast(this, "Set up failed");
                return;
            }
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

    public void startUpdate(View view) {
        if (isWindowLocked()) return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mMokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        if (TextUtils.isEmpty(mBind.etOtaUrl.getText())) {
            ToastUtils.showToast(this, "params error");
            return;
        }
        XLog.i("检查设备状态");
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Setup failed, please try it again!");
        }, 30 * 1000);
        showLoadingProgressDialog();
        byte[] message = MQTTMessageAssembler.assembleReadDeviceStatus(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setOTAFirmware() {
        String filePath = mBind.etOtaUrl.getText().toString();
        byte[] message = MQTTMessageAssembler.assembleWriteOTA(mMokoDevice.mac, filePath);
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
