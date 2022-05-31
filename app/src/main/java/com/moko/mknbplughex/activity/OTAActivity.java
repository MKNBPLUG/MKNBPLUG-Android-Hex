package com.moko.mknbplughex.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplughex.AppConstants;
import com.moko.mknbplughex.R;
import com.moko.mknbplughex.R2;
import com.moko.mknbplughex.base.BaseActivity;
import com.moko.mknbplughex.dialog.BottomDialog;
import com.moko.mknbplughex.entity.MQTTConfig;
import com.moko.mknbplughex.entity.MokoDevice;
import com.moko.mknbplughex.utils.SPUtiles;
import com.moko.mknbplughex.utils.ToastUtils;
import com.moko.support.hex.MQTTConstants;
import com.moko.support.hex.MQTTMessageAssembler;
import com.moko.support.hex.MQTTSupport;
import com.moko.support.hex.event.DeviceOnlineEvent;
import com.moko.support.hex.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

public class OTAActivity extends BaseActivity {
    private final String FILTER_ASCII = "[ -~]*";

    public static String TAG = OTAActivity.class.getSimpleName();
    @BindView(R2.id.tv_update_type)
    TextView tvUpdateType;
    @BindView(R2.id.et_master_host)
    EditText etMasterHost;
    @BindView(R2.id.et_master_port)
    EditText etMasterPort;
    @BindView(R2.id.et_master_file_path)
    EditText etMasterFilePath;
    @BindView(R2.id.ll_master_firmware)
    LinearLayout llMasterFirmware;
    @BindView(R2.id.et_one_way_host)
    EditText etOneWayHost;
    @BindView(R2.id.et_one_way_port)
    EditText etOneWayPort;
    @BindView(R2.id.et_one_way_ca_file_path)
    EditText etOneWayCaFilePath;
    @BindView(R2.id.ll_one_way)
    LinearLayout llOneWay;
    @BindView(R2.id.et_both_way_host)
    EditText etBothWayHost;
    @BindView(R2.id.et_both_way_port)
    EditText etBothWayPort;
    @BindView(R2.id.et_both_way_ca_file_path)
    EditText etBothWayCaFilePath;
    @BindView(R2.id.et_both_way_client_key_file_path)
    EditText etBothWayClientKeyFilePath;
    @BindView(R2.id.et_both_way_client_cert_file_path)
    EditText etBothWayClientCertFilePath;
    @BindView(R2.id.ll_both_way)
    LinearLayout llBothWay;


    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private ArrayList<String> mValues;
    private int mSelected;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ota);
        ButterKnife.bind(this);
        if (getIntent().getExtras() != null) {
            mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        }
        InputFilter inputFilter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }

            return null;
        };
        etMasterHost.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), inputFilter});
        etOneWayHost.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), inputFilter});
        etBothWayHost.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), inputFilter});
        etMasterFilePath.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100), inputFilter});
        etOneWayCaFilePath.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100), inputFilter});
        etBothWayCaFilePath.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100), inputFilter});
        etBothWayClientKeyFilePath.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100), inputFilter});
        etBothWayClientCertFilePath.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100), inputFilter});
        mHandler = new Handler(Looper.getMainLooper());
        String mqttConfigAppStr = SPUtiles.getStringValue(OTAActivity.this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mValues = new ArrayList<>();
        mValues.add("Firmware");
        mValues.add("CA certificate");
        mValues.add("Self signed server certificates");
        tvUpdateType.setText(mValues.get(mSelected));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        // 更新所有设备的网络状态
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
        if (cmd == MQTTConstants.READ_MSG_ID_DEVICE_STATUS) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 0) {
                return;
            }
            if (data[0] != 0) {
                ToastUtils.showToast(this, "Device is OTA, please wait");
                return;
            }
            XLog.i("升级固件");
            mHandler.postDelayed(() -> {
                dismissLoadingMessageDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 50 * 1000);
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
            if (data[5] != mSelected)
                return;
            if (data[6] == 1) {
                ToastUtils.showToast(this, R.string.update_success);
            } else {
                ToastUtils.showToast(this, R.string.update_failed);
            }
        }
        if (cmd == MQTTConstants.CONFIG_MSG_ID_OTA
                || cmd == MQTTConstants.CONFIG_MSG_ID_OTA_ONE_WAY
                || cmd == MQTTConstants.CONFIG_MSG_ID_OTA_BOTH_WAY) {
            if (dataLength != 1)
                return;
            if (data[0] == 0) {
                ToastUtils.showToast(this, "Set up failed");
                return;
            }
        }
        if (cmd == MQTTConstants.NOTIFY_MSG_ID_OVERLOAD_OCCUR
                || cmd == MQTTConstants.NOTIFY_MSG_ID_OVER_VOLTAGE_OCCUR
                || cmd == MQTTConstants.NOTIFY_MSG_ID_UNDER_VOLTAGE_OCCUR
                || cmd == MQTTConstants.NOTIFY_MSG_ID_OVER_CURRENT_OCCUR) {
            if (dataLength != 6)
                return;
            if (message[5] == 1)
                finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
        String deviceId = event.getDeviceId();
        if (!mMokoDevice.deviceId.equals(deviceId)) {
            return;
        }
        boolean online = event.isOnline();
        if (!online) {
            finish();
        }
    }

    public void onBack(View view) {
        finish();
    }

    public void startUpdate(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mMokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        if (mSelected == 0) {
            String hostStr = etMasterHost.getText().toString();
            String portStr = etMasterPort.getText().toString();
            String masterStr = etMasterFilePath.getText().toString();
            if (TextUtils.isEmpty(hostStr)) {
                ToastUtils.showToast(this, R.string.mqtt_verify_host);
                return;
            }
            if (TextUtils.isEmpty(portStr) || Integer.parseInt(portStr) > 65535) {
                ToastUtils.showToast(this, R.string.mqtt_verify_port_empty);
                return;
            }
            if (TextUtils.isEmpty(masterStr)) {
                ToastUtils.showToast(this, R.string.mqtt_verify_file_path);
                return;
            }
        }
        if (mSelected == 1) {
            String hostStr = etOneWayHost.getText().toString();
            String portStr = etOneWayPort.getText().toString();
            String oneWayStr = etOneWayCaFilePath.getText().toString();
            if (TextUtils.isEmpty(hostStr)) {
                ToastUtils.showToast(this, R.string.mqtt_verify_host);
                return;
            }
            if (TextUtils.isEmpty(portStr) || Integer.parseInt(portStr) > 65535) {
                ToastUtils.showToast(this, R.string.mqtt_verify_port_empty);
                return;
            }
            if (TextUtils.isEmpty(oneWayStr)) {
                ToastUtils.showToast(this, R.string.mqtt_verify_file_path);
                return;
            }
        }
        if (mSelected == 2) {
            String hostStr = etBothWayHost.getText().toString();
            String portStr = etBothWayPort.getText().toString();
            String bothWayCaStr = etBothWayCaFilePath.getText().toString();
            String bothWayClientKeyStr = etBothWayClientKeyFilePath.getText().toString();
            String bothWayClientCertStr = etBothWayClientCertFilePath.getText().toString();
            if (TextUtils.isEmpty(hostStr)) {
                ToastUtils.showToast(this, R.string.mqtt_verify_host);
                return;
            }
            if (TextUtils.isEmpty(portStr) || Integer.parseInt(portStr) > 65535) {
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
                    llMasterFirmware.setVisibility(View.VISIBLE);
                    llOneWay.setVisibility(View.GONE);
                    llBothWay.setVisibility(View.GONE);
                    break;
                case 1:
                    llMasterFirmware.setVisibility(View.GONE);
                    llOneWay.setVisibility(View.VISIBLE);
                    llBothWay.setVisibility(View.GONE);
                    break;
                case 2:
                    llMasterFirmware.setVisibility(View.GONE);
                    llOneWay.setVisibility(View.GONE);
                    llBothWay.setVisibility(View.VISIBLE);
                    break;
            }
            tvUpdateType.setText(mValues.get(value));
        });
        dialog.show(getSupportFragmentManager());
    }

    private void setOTAFirmware() {
        String host = etMasterHost.getText().toString();
        String portStr = etMasterPort.getText().toString();
        String filePath = etMasterFilePath.getText().toString();

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
        String hostStr = etOneWayHost.getText().toString();
        String portStr = etOneWayPort.getText().toString();
        String oneWayStr = etOneWayCaFilePath.getText().toString();
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
        String hostStr = etBothWayHost.getText().toString();
        String portStr = etBothWayPort.getText().toString();
        String bothWayCaStr = etBothWayCaFilePath.getText().toString();
        String bothWayClientKeyStr = etBothWayClientKeyFilePath.getText().toString();
        String bothWayClientCertStr = etBothWayClientCertFilePath.getText().toString();
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
