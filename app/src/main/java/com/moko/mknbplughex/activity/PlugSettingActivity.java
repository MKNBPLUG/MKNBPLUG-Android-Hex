package com.moko.mknbplughex.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplughex.AppConstants;
import com.moko.mknbplughex.R;
import com.moko.mknbplughex.R2;
import com.moko.mknbplughex.base.BaseActivity;
import com.moko.mknbplughex.db.DBTools;
import com.moko.mknbplughex.dialog.AlertMessageDialog;
import com.moko.mknbplughex.dialog.CustomDialog;
import com.moko.mknbplughex.entity.MokoDevice;
import com.moko.mknbplughex.utils.SPUtils;
import com.moko.mknbplughex.utils.ToastUtils;
import com.moko.support.hex.MQTTConstants;
import com.moko.support.hex.MQTTMessageAssembler;
import com.moko.support.hex.MQTTSupport;
import com.moko.support.hex.MokoSupport;
import com.moko.support.hex.entity.MQTTConfig;
import com.moko.support.hex.event.DeviceDeletedEvent;
import com.moko.support.hex.event.DeviceModifyNameEvent;
import com.moko.support.hex.event.DeviceOnlineEvent;
import com.moko.support.hex.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;

import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;

public class PlugSettingActivity extends BaseActivity {
    private final String FILTER_ASCII = "[ -~]*";
    public static String TAG = PlugSettingActivity.class.getSimpleName();
    @BindView(R2.id.iv_button_control)
    ImageView ivButtonControl;
    @BindView(R2.id.rl_debug_mode)
    RelativeLayout rlDebugMode;
    @BindView(R2.id.rl_modify_network)
    RelativeLayout rlModifyNetwork;
    @BindView(R2.id.rl_ota)
    RelativeLayout rlOta;


    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;

    private Handler mHandler;
    private InputFilter filter;
    private boolean mButtonControlEnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plug_setting);
        ButterKnife.bind(this);
        filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }

            return null;
        };
        if (getIntent().getExtras() != null) {
            mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        }
        assert mMokoDevice != null;
        rlDebugMode.setVisibility(mMokoDevice.deviceMode == 2 ? View.VISIBLE : View.GONE);
        rlModifyNetwork.setVisibility(mMokoDevice.deviceMode == 2 ? View.GONE : View.VISIBLE);
        rlOta.setVisibility(mMokoDevice.deviceMode == 2 ? View.GONE : View.VISIBLE);
        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mHandler = new Handler(Looper.getMainLooper());
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getButtonControlEnable();
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
        if (cmd == MQTTConstants.MSG_ID_BUTTON_CONTROL_ENABLE && flag == 0) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            mButtonControlEnable = data[0] == 1;
            ivButtonControl.setImageResource(mButtonControlEnable ? R.drawable.checkbox_open : R.drawable.checkbox_close);
        }
        if (cmd == MQTTConstants.MSG_ID_BUTTON_CONTROL_ENABLE && flag == 1) {
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
            ivButtonControl.setImageResource(mButtonControlEnable ? R.drawable.checkbox_open : R.drawable.checkbox_close);
            ToastUtils.showToast(this, "Set up succeed");
        }
        if (cmd == MQTTConstants.CONFIG_MSG_ID_RESET) {
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
            if (TextUtils.isEmpty(appMqttConfig.topicSubscribe)) {
                // 取消订阅
                try {
                    MQTTSupport.getInstance().unSubscribe(mMokoDevice.topicPublish);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
            XLog.i(String.format("删除设备:%s", mMokoDevice.name));
            DBTools.getInstance(this).deleteDevice(mMokoDevice);
            EventBus.getDefault().post(new DeviceDeletedEvent(mMokoDevice.id));
            ivButtonControl.postDelayed(() -> {
                dismissLoadingProgressDialog();
                // 跳转首页，刷新数据
                Intent intent = new Intent(this, HEXMainActivity.class);
                intent.putExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY, TAG);
                intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_ID, mMokoDevice.deviceId);
                startActivity(intent);
            }, 500);
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceModifyNameEvent(DeviceModifyNameEvent event) {
        // 修改了设备名称
        String deviceId = event.getDeviceId();
        if (deviceId.equals(mMokoDevice.deviceId)) {
            mMokoDevice.name = event.getName();
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


    public void onEditName(View view) {
        if (isWindowLocked())
            return;
        View content = LayoutInflater.from(this).inflate(R.layout.modify_name, null);
        final EditText etDeviceName = content.findViewById(R.id.et_device_name);
        String deviceName = etDeviceName.getText().toString();
        etDeviceName.setText(deviceName);
        etDeviceName.setSelection(deviceName.length());
        etDeviceName.setFilters(new InputFilter[]{filter, new InputFilter.LengthFilter(20)});
        CustomDialog dialog = new CustomDialog.Builder(this)
                .setContentView(content)
                .setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = etDeviceName.getText().toString();
                        if (TextUtils.isEmpty(name)) {
                            ToastUtils.showToast(PlugSettingActivity.this, R.string.more_modify_name_tips);
                            return;
                        }
                        mMokoDevice.name = name;
                        DBTools.getInstance(PlugSettingActivity.this).updateDevice(mMokoDevice);
                        DeviceModifyNameEvent event = new DeviceModifyNameEvent(mMokoDevice.deviceId);
                        event.setName(name);
                        EventBus.getDefault().post(event);
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.show();
        etDeviceName.postDelayed(() -> showKeyboard(etDeviceName), 300);
    }

    private void getButtonControlEnable() {
        XLog.i("读取按键控制功能开关");
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadButtonControlEnable(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setButtonControlEnable() {
        XLog.i("设置按键控制功能开关");
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteButtonControlEnable(mMokoDevice.deviceId, mButtonControlEnable ? 1 : 0);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    //弹出软键盘
    public void showKeyboard(EditText editText) {
        //其中editText为dialog中的输入框的 EditText
        if (editText != null) {
            //设置可获得焦点
            editText.setFocusable(true);
            editText.setFocusableInTouchMode(true);
            //请求获得焦点
            editText.requestFocus();
            //调用系统输入法
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.showSoftInput(editText, 0);
        }
    }

    public void onButtonControlEnable(View view) {
        if (isWindowLocked())
            return;
        mButtonControlEnable = !mButtonControlEnable;
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        setButtonControlEnable();
    }

    public void onRemove(View view) {
        if (isWindowLocked())
            return;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Remove Device");
        dialog.setMessage("Please confirm again whether to \n remove the device,the device \n will be deleted from the device list.");
        dialog.setOnAlertConfirmListener(() -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(this, R.string.network_error);
                return;
            }
            showLoadingProgressDialog();
            if (TextUtils.isEmpty(appMqttConfig.topicSubscribe)) {
                // 取消订阅
                try {
                    MQTTSupport.getInstance().unSubscribe(mMokoDevice.topicPublish);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
            XLog.i(String.format("删除设备:%s", mMokoDevice.name));
            DBTools.getInstance(this).deleteDevice(mMokoDevice);
            EventBus.getDefault().post(new DeviceDeletedEvent(mMokoDevice.id));
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                // 跳转首页，刷新数据
                Intent intent = new Intent(this, HEXMainActivity.class);
                intent.putExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY, TAG);
                intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_ID, mMokoDevice.deviceId);
                startActivity(intent);
            }, 500);
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onReset(View view) {
        if (isWindowLocked())
            return;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Reset Device");
        dialog.setMessage("After reset, the device will be \n removed from the device list, and \n relevant data will be totally cleared.");
        dialog.setOnAlertConfirmListener(() -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(this, R.string.network_error);
                return;
            }
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            XLog.i("重置设备");
            String appTopic;
            if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
                appTopic = mMokoDevice.topicSubscribe;
            } else {
                appTopic = appMqttConfig.topicPublish;
            }
            byte[] message = MQTTMessageAssembler.assembleWriteReset(mMokoDevice.deviceId);
            try {
                MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onModifyPowerStatus(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, PowerOnDefaultActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onPeriodReportClick(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, PeriodicalReportActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onPowerReportSettingClick(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, PowerReportSettingActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onEnergyStorageReportClick(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, EnergyStorageReportActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onConnTimeoutSettingClick(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, ConnectionTimeoutActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onSystemTimeClick(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, SystemTimeActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onProtectionSwitchClick(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, ProtectionSwitchActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onNotificationSwitchClick(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, LoadStatusNotifyActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onIndicatorSettingClick(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, IndicatorSettingActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onModifyNetworkMQTTClick(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, ModifyMQTTSettingsActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onOTA(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, OTAActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onMQTTSettingForDevice(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, SettingForDeviceActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onDeviceInfo(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, DeviceInfoActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onDebugModeClick(View view) {
        if (isWindowLocked())
            return;
        StringBuffer macSB = new StringBuffer(mMokoDevice.mac);
        macSB.insert(2, ":");
        macSB.insert(5, ":");
        macSB.insert(8, ":");
        macSB.insert(11, ":");
        macSB.insert(14, ":");
        // 进入Debug模式
        showLoadingProgressDialog();
        rlDebugMode.postDelayed(() -> MokoSupport.getInstance().connDevice(macSB.toString()), 500);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        String action = event.getAction();
        if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Connection Failed, please try again");
        }
        if (MokoConstants.ACTION_DISCOVER_SUCCESS.equals(action)) {
            dismissLoadingProgressDialog();
            // 进入Debug模式
            Intent intent = new Intent(this, LogDataActivity.class);
            intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_MAC, mMokoDevice.mac);
            startActivityForResult(intent, AppConstants.REQUEST_CODE_LOG);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.REQUEST_CODE_LOG) {
            if (resultCode == RESULT_OK) {
                showLoadingProgressDialog();
                if (TextUtils.isEmpty(appMqttConfig.topicSubscribe)) {
                    // 取消订阅
                    try {
                        MQTTSupport.getInstance().unSubscribe(mMokoDevice.topicPublish);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
                XLog.i(String.format("删除设备:%s", mMokoDevice.name));
                DBTools.getInstance(this).deleteDevice(mMokoDevice);
                EventBus.getDefault().post(new DeviceDeletedEvent(mMokoDevice.id));
                ivButtonControl.postDelayed(() -> {
                    dismissLoadingProgressDialog();
                    // 跳转首页，刷新数据
                    Intent intent = new Intent(this, HEXMainActivity.class);
                    intent.putExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY, TAG);
                    intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_ID, mMokoDevice.deviceId);
                    startActivity(intent);
                }, 500);
            }
        }
    }
}
