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

public class OTAActivity extends BaseActivity<ActivityOtaBinding> {
    private final String FILTER_ASCII = "[ -~]*";
    public static String TAG = OTAActivity.class.getSimpleName();
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private ArrayList<String> mValues;
    private int mSelected;
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
        mBind.etMasterHost.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), inputFilter});
        mBind.etOneWayHost.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), inputFilter});
        mBind.etBothWayHost.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), inputFilter});
        mBind.etMasterFilePath.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), inputFilter});
        mBind.etOneWayCaFilePath.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), inputFilter});
        mBind.etBothWayCaFilePath.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), inputFilter});
        mBind.etBothWayClientKeyFilePath.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), inputFilter});
        mBind.etBothWayClientCertFilePath.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), inputFilter});
        mHandler = new Handler(Looper.getMainLooper());
        String mqttConfigAppStr = SPUtils.getStringValue(OTAActivity.this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mValues = new ArrayList<>();
        mValues.add("Firmware");
        mValues.add("CA certificate");
        mValues.add("Self signed server certificates");
        mBind.tvUpdateType.setText(mValues.get(mSelected));
    }

    @Override
    protected ActivityOtaBinding getViewBinding() {
        return ActivityOtaBinding.inflate(getLayoutInflater());
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
        if (header != 0xED)
            return;
        if (!mMokoDevice.deviceId.equals(deviceId))
            return;
        mMokoDevice.isOnline = true;
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
            if (mSelected == 0) {
                setOTAFirmware();
            }
            if (mSelected == 1) {
                setOTAOneWay();
            }
            if (mSelected == 2) {
                setOTABothWay();
            }
        }
        if (cmd == MQTTConstants.NOTIFY_MSG_ID_OTA_RESULT) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingMessageDialog();
                mHandler.removeMessages(0);
            }
            if (data[5] != mSelected) return;
            if (data[6] == 1) {
                ToastUtils.showToast(this, R.string.update_success);
            } else {
                ToastUtils.showToast(this, R.string.update_failed);
            }
        }
        if (cmd == MQTTConstants.CONFIG_MSG_ID_OTA
                || cmd == MQTTConstants.CONFIG_MSG_ID_OTA_ONE_WAY
                || cmd == MQTTConstants.CONFIG_MSG_ID_OTA_BOTH_WAY) {
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
        if (mSelected == 0) {
            String hostStr = mBind.etMasterHost.getText().toString();
            String portStr = mBind.etMasterPort.getText().toString();
            String masterStr = mBind.etMasterFilePath.getText().toString();
            if (TextUtils.isEmpty(hostStr)) {
                ToastUtils.showToast(this, R.string.mqtt_verify_host);
                return;
            }
            if (TextUtils.isEmpty(portStr) || Integer.parseInt(portStr) < 1 || Integer.parseInt(portStr) > 65535) {
                ToastUtils.showToast(this, R.string.mqtt_verify_port_empty);
                return;
            }
            if (TextUtils.isEmpty(masterStr)) {
                ToastUtils.showToast(this, R.string.mqtt_verify_file_path);
                return;
            }
        }
        if (mSelected == 1) {
            String hostStr = mBind.etOneWayHost.getText().toString();
            String portStr = mBind.etOneWayPort.getText().toString();
            String oneWayStr = mBind.etOneWayCaFilePath.getText().toString();
            if (TextUtils.isEmpty(hostStr)) {
                ToastUtils.showToast(this, R.string.mqtt_verify_host);
                return;
            }
            if (TextUtils.isEmpty(portStr) || Integer.parseInt(portStr) < 1 || Integer.parseInt(portStr) > 65535) {
                ToastUtils.showToast(this, R.string.mqtt_verify_port_empty);
                return;
            }
            if (TextUtils.isEmpty(oneWayStr)) {
                ToastUtils.showToast(this, R.string.mqtt_verify_file_path);
                return;
            }
        }
        if (mSelected == 2) {
            String hostStr = mBind.etBothWayHost.getText().toString();
            String portStr = mBind.etBothWayPort.getText().toString();
            String bothWayCaStr = mBind.etBothWayCaFilePath.getText().toString();
            String bothWayClientKeyStr = mBind.etBothWayClientKeyFilePath.getText().toString();
            String bothWayClientCertStr = mBind.etBothWayClientCertFilePath.getText().toString();
            if (TextUtils.isEmpty(hostStr)) {
                ToastUtils.showToast(this, R.string.mqtt_verify_host);
                return;
            }
            if (TextUtils.isEmpty(portStr) || Integer.parseInt(portStr) < 1 || Integer.parseInt(portStr) > 65535) {
                ToastUtils.showToast(this, R.string.mqtt_verify_port_empty);
                return;
            }
            if (TextUtils.isEmpty(bothWayCaStr)
                    || TextUtils.isEmpty(bothWayClientKeyStr)
                    || TextUtils.isEmpty(bothWayClientCertStr)) {
                ToastUtils.showToast(this, R.string.mqtt_verify_file_path);
                return;
            }
        }
        XLog.i("检查设备状态");
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Setup failed, please try it again!");
        }, 30 * 1000);
        showLoadingProgressDialog();
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadDeviceStatus(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onSelectUpdateType(View view) {
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mValues, mSelected);
        dialog.setListener(value -> {
            mSelected = value;
            switch (value) {
                case 0:
                    mBind.llMasterFirmware.setVisibility(View.VISIBLE);
                    mBind.llOneWay.setVisibility(View.GONE);
                    mBind.llBothWay.setVisibility(View.GONE);
                    break;
                case 1:
                    mBind.llMasterFirmware.setVisibility(View.GONE);
                    mBind.llOneWay.setVisibility(View.VISIBLE);
                    mBind.llBothWay.setVisibility(View.GONE);
                    break;
                case 2:
                    mBind.llMasterFirmware.setVisibility(View.GONE);
                    mBind.llOneWay.setVisibility(View.GONE);
                    mBind.llBothWay.setVisibility(View.VISIBLE);
                    break;
            }
            mBind.tvUpdateType.setText(mValues.get(value));
        });
        dialog.show(getSupportFragmentManager());
    }

    private void setOTAFirmware() {
        String host = mBind.etMasterHost.getText().toString();
        String portStr = mBind.etMasterPort.getText().toString();
        String filePath = mBind.etMasterFilePath.getText().toString();

        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteOTA(mMokoDevice.deviceId
                , host
                , Integer.parseInt(portStr)
                , filePath);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setOTAOneWay() {
        String hostStr = mBind.etOneWayHost.getText().toString();
        String portStr = mBind.etOneWayPort.getText().toString();
        String oneWayStr = mBind.etOneWayCaFilePath.getText().toString();
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteCaFileOTA(mMokoDevice.deviceId
                , hostStr
                , Integer.parseInt(portStr)
                , oneWayStr);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setOTABothWay() {
        String hostStr = mBind.etBothWayHost.getText().toString();
        String portStr = mBind.etBothWayPort.getText().toString();
        String bothWayCaStr = mBind.etBothWayCaFilePath.getText().toString();
        String bothWayClientKeyStr = mBind.etBothWayClientKeyFilePath.getText().toString();
        String bothWayClientCertStr = mBind.etBothWayClientCertFilePath.getText().toString();
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteSelfSingleCertOTA(mMokoDevice.deviceId
                , hostStr
                , Integer.parseInt(portStr)
                , bothWayCaStr
                , bothWayClientCertStr
                , bothWayClientKeyStr);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
