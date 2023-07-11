package com.moko.mknbplughex.activity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplughex.AppConstants;
import com.moko.mknbplughex.R;
import com.moko.mknbplughex.base.BaseActivity;
import com.moko.mknbplughex.databinding.ActivityPlugBinding;
import com.moko.mknbplughex.dialog.AlertMessageDialog;
import com.moko.mknbplughex.dialog.TimerDialog;
import com.moko.mknbplughex.entity.MokoDevice;
import com.moko.mknbplughex.utils.SPUtils;
import com.moko.mknbplughex.utils.ToastUtils;
import com.moko.support.hex.MQTTConstants;
import com.moko.support.hex.MQTTMessageAssembler;
import com.moko.support.hex.MQTTSupport;
import com.moko.support.hex.entity.MQTTConfig;
import com.moko.support.hex.event.DeviceModifyNameEvent;
import com.moko.support.hex.event.DeviceOnlineEvent;
import com.moko.support.hex.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;

public class PlugActivity extends BaseActivity<ActivityPlugBinding> {
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private Handler mHandler;
    private boolean mIsOver;

    @Override
    protected void onCreate() {
        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        mBind.tvTitle.setText(mMokoDevice.name);
        mHandler = new Handler(Looper.getMainLooper());
        changeSwitchState();
        if (mMokoDevice.isOverload
                || mMokoDevice.isOverVoltage
                || mMokoDevice.isOverCurrent
                || mMokoDevice.isUnderVoltage) {
            showOverDialog();
            return;
        }
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            EventBus.getDefault().post(new DeviceOnlineEvent(mMokoDevice.mac, false));
            dismissLoadingProgressDialog();
            finish();
        }, 90 * 1000);
        getSwitchInfo();
    }

    @Override
    protected ActivityPlugBinding getViewBinding() {
        return ActivityPlugBinding.inflate(getLayoutInflater());
    }

    String mOverStatus;

    private void showOverDialog() {
        if (mIsOver) return;
        if (mMokoDevice.isOverload)
            mOverStatus = "overload";
        if (mMokoDevice.isOverVoltage)
            mOverStatus = "overvoltage";
        if (mMokoDevice.isOverCurrent)
            mOverStatus = "overcurrent";
        if (mMokoDevice.isUnderVoltage)
            mOverStatus = "undervoltage";
        String message = String.format("Detect the socket %s, please confirm whether to exit the %s status?", mOverStatus, mOverStatus);
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Warning");
        dialog.setMessage(message);
        dialog.setOnAlertCancelListener(this::finish);
        dialog.setOnAlertConfirmListener(this::showClearOverStatusDialog);
        dialog.show(getSupportFragmentManager());
        mIsOver = true;
    }

    private void showClearOverStatusDialog() {
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Warning");
        dialog.setMessage(String.format("If YES, the socket will exit %s status, and please make sure it is within the protection threshold. If NO, you need manually reboot it to exit this status.", mOverStatus));
        dialog.setOnAlertCancelListener(this::finish);
        dialog.setOnAlertConfirmListener(() -> {
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                EventBus.getDefault().post(new DeviceOnlineEvent(mMokoDevice.mac, false));
                dismissLoadingProgressDialog();
                finish();
            }, 90 * 1000);
            clearOverStatus();
        });
        dialog.show(getSupportFragmentManager());
    }

    private void clearOverStatus() {
        XLog.i("清除过载状态");
        if (mMokoDevice.isOverload) {
            byte[] message = MQTTMessageAssembler.assembleConfigClearOverloadStatus(mMokoDevice.mac);
            try {
                MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
            return;
        }
        if (mMokoDevice.isOverVoltage) {
            byte[] message = MQTTMessageAssembler.assembleConfigClearOverVoltageStatus(mMokoDevice.mac);
            try {
                MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
            return;
        }
        if (mMokoDevice.isOverCurrent) {
            byte[] message = MQTTMessageAssembler.assembleConfigClearOverCurrentStatus(mMokoDevice.mac);
            try {
                MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
            return;
        }
        if (mMokoDevice.isUnderVoltage) {
            byte[] message = MQTTMessageAssembler.assembleConfigClearUnderVoltageStatus(mMokoDevice.mac);
            try {
                MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
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
        if (!mMokoDevice.mac.equalsIgnoreCase(deviceId)) return;
        mMokoDevice.isOnline = true;
        if (cmd == MQTTConstants.NOTIFY_MSG_ID_SWITCH_STATE) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 11) return;
            // 启动设备定时离线，90s收不到应答则认为离线
            mMokoDevice.on_off = data[5] == 1;
            mMokoDevice.isOverload = data[7] == 1;
            mMokoDevice.isOverCurrent = data[8] == 1;
            mMokoDevice.isOverVoltage = data[9] == 1;
            mMokoDevice.isUnderVoltage = data[10] == 1;
            changeSwitchState();
            if (mMokoDevice.isOverload
                    || mMokoDevice.isOverVoltage
                    || mMokoDevice.isUnderVoltage
                    || mMokoDevice.isOverCurrent) {
                showOverDialog();
            }
            return;
        }
        if (cmd == MQTTConstants.READ_MSG_ID_SWITCH_INFO) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 6) return;
            // 启动设备定时离线，90s收不到应答则认为离线
            mMokoDevice.on_off = data[0] == 1;
            mMokoDevice.isOverload = data[2] == 1;
            mMokoDevice.isOverCurrent = data[3] == 1;
            mMokoDevice.isOverVoltage = data[4] == 1;
            mMokoDevice.isUnderVoltage = data[5] == 1;
            changeSwitchState();
            if (mMokoDevice.isOverload
                    || mMokoDevice.isOverVoltage
                    || mMokoDevice.isUnderVoltage
                    || mMokoDevice.isOverCurrent) {
                showOverDialog();
            }
            return;
        }
        if (cmd == MQTTConstants.NOTIFY_MSG_ID_COUNTDOWN_INFO) {
            if (dataLength != 10) return;
            int switch_state = data[5];
            int countdown = MokoUtils.toInt(Arrays.copyOfRange(data, 6, 10));
            if (countdown == 0) {
                mBind.tvTimerState.setVisibility(View.GONE);
            } else {
                int hour = countdown / 3600;
                int minute = (countdown % 3600) / 60;
                int second = (countdown % 3600) % 60;
                mBind.tvTimerState.setVisibility(View.VISIBLE);
                String timer = String.format("Device will turn %s after %02d:%02d:%02d", switch_state == 1 ? "on" : "off", hour, minute, second);
                mBind.tvTimerState.setText(timer);
            }
            return;
        }
        if (cmd == MQTTConstants.NOTIFY_MSG_ID_OVERLOAD_OCCUR) {
            if (dataLength != 6) return;
            mMokoDevice.isOverload = data[5] == 1;
            mMokoDevice.on_off = false;
            if (mMokoDevice.isOverload) {
                showOverDialog();
            } else {
                mIsOver = false;
            }
            return;
        }
        if (cmd == MQTTConstants.NOTIFY_MSG_ID_OVER_VOLTAGE_OCCUR) {
            if (dataLength != 6) return;
            mMokoDevice.isOverVoltage = data[5] == 1;
            mMokoDevice.on_off = false;
            if (mMokoDevice.isOverVoltage) {
                showOverDialog();
            } else {
                mIsOver = false;
            }
            return;
        }
        if (cmd == MQTTConstants.NOTIFY_MSG_ID_UNDER_VOLTAGE_OCCUR) {
            if (dataLength != 6) return;
            mMokoDevice.isUnderVoltage = data[5] == 1;
            mMokoDevice.on_off = false;
            if (mMokoDevice.isUnderVoltage) {
                showOverDialog();
            } else {
                mIsOver = false;
            }
            return;
        }
        if (cmd == MQTTConstants.NOTIFY_MSG_ID_OVER_CURRENT_OCCUR) {
            if (dataLength != 6) return;
            mMokoDevice.isOverCurrent = data[5] == 1;
            mMokoDevice.on_off = false;
            if (mMokoDevice.isOverCurrent) {
                showOverDialog();
            } else {
                mIsOver = false;
            }
            return;
        }
        if (cmd == MQTTConstants.NOTIFY_MSG_ID_LOAD_STATUS_NOTIFY) {
            if (dataLength != 6) return;
            boolean loadStatus = data[5] == 1;
            ToastUtils.showToast(PlugActivity.this, loadStatus ? "Load starts work！" : "Load stops work！");
            return;
        }
        if (cmd == MQTTConstants.CONFIG_MSG_ID_CLEAR_OVERLOAD_PROTECTION
                || cmd == MQTTConstants.CONFIG_MSG_ID_CLEAR_OVER_VOLTAGE_PROTECTION
                || cmd == MQTTConstants.CONFIG_MSG_ID_CLEAR_UNDER_VOLTAGE_PROTECTION
                || cmd == MQTTConstants.CONFIG_MSG_ID_CLEAR_OVER_CURRENT_PROTECTION) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 1) return;
            if (data[0] == 0) {
                ToastUtils.showToast(this, "Set up failed");
                return;
            }
            mIsOver = false;
            ToastUtils.showToast(this, "Set up succeed");
            return;
        }
        if (cmd == MQTTConstants.CONFIG_MSG_ID_SWITCH_STATE
                || cmd == MQTTConstants.CONFIG_MSG_ID_COUNTDOWN) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 1) return;
            if (data[0] == 0) {
                ToastUtils.showToast(this, "Set up failed");
            } else {
                if (cmd == MQTTConstants.CONFIG_MSG_ID_SWITCH_STATE)
                    getSwitchInfo();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceModifyNameEvent(DeviceModifyNameEvent event) {
        // 修改了设备名称
        String deviceMac = event.getDeviceMac();
        if (deviceMac.equalsIgnoreCase(mMokoDevice.mac)) {
            mMokoDevice.name = event.getName();
            mBind.tvTitle.setText(mMokoDevice.name);
        }
    }

    private void changeSwitchState() {
        mBind.rlTitle.setBackgroundColor(ContextCompat.getColor(this, mMokoDevice.on_off ? R.color.blue_0188cc : R.color.black_303a4b));
        mBind.llBg.setBackgroundColor(ContextCompat.getColor(this, mMokoDevice.on_off ? R.color.grey_f2f2f2 : R.color.black_303a4b));
        mBind.ivSwitchState.setImageDrawable(ContextCompat.getDrawable(this, mMokoDevice.on_off ? R.drawable.plug_switch_on : R.drawable.plug_switch_off));
        String switchState = "";
        if (!mMokoDevice.isOnline) {
            switchState = getString(R.string.plug_switch_offline);
        } else if (mMokoDevice.on_off) {
            switchState = getString(R.string.plug_switch_on);
        } else {
            switchState = getString(R.string.plug_switch_off);
        }
        mBind.tvSwitchState.setText(switchState);
        mBind.tvSwitchState.setTextColor(ContextCompat.getColor(this, mMokoDevice.on_off ? R.color.blue_0188cc : R.color.grey_808080));

        Drawable drawablePower = ContextCompat.getDrawable(this, mMokoDevice.on_off ? R.drawable.power_on : R.drawable.power_off);
        drawablePower.setBounds(0, 0, drawablePower.getMinimumWidth(), drawablePower.getMinimumHeight());
        mBind.tvDevicePower.setCompoundDrawables(null, drawablePower, null, null);
        mBind.tvDevicePower.setTextColor(ContextCompat.getColor(this, mMokoDevice.on_off ? R.color.blue_0188cc : R.color.grey_808080));
        Drawable drawableTimer = ContextCompat.getDrawable(this, mMokoDevice.on_off ? R.drawable.timer_on : R.drawable.timer_off);
        drawableTimer.setBounds(0, 0, drawableTimer.getMinimumWidth(), drawableTimer.getMinimumHeight());
        mBind.tvDeviceTimer.setCompoundDrawables(null, drawableTimer, null, null);
        mBind.tvDeviceTimer.setTextColor(ContextCompat.getColor(this, mMokoDevice.on_off ? R.color.blue_0188cc : R.color.grey_808080));
        Drawable drawableEnergy = ContextCompat.getDrawable(this, mMokoDevice.on_off ? R.drawable.energy_on : R.drawable.energy_off);
        drawableEnergy.setBounds(0, 0, drawableEnergy.getMinimumWidth(), drawableEnergy.getMinimumHeight());
        mBind.tvDeviceEnergy.setCompoundDrawables(null, drawableEnergy, null, null);
        mBind.tvDeviceEnergy.setTextColor(ContextCompat.getColor(this, mMokoDevice.on_off ? R.color.blue_0188cc : R.color.grey_808080));
        mBind.tvTimerState.setTextColor(ContextCompat.getColor(this, mMokoDevice.on_off ? R.color.blue_0188cc : R.color.grey_808080));
    }

    public void onBack(View view) {
        finish();
    }

    public void onPlugSetting(View view) {
        if (isWindowLocked()) return;
        // Energy
        Intent intent = new Intent(this, PlugSettingActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(intent);
    }

    public void onTimerClick(View view) {
        if (isWindowLocked()) return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(PlugActivity.this, R.string.network_error);
            return;
        }
        if (!mMokoDevice.isOnline) {
            ToastUtils.showToast(PlugActivity.this, R.string.device_offline);
            return;
        }
        TimerDialog timerDialog = new TimerDialog();
        timerDialog.setOnoff(mMokoDevice.on_off);
        timerDialog.setListener(dialog -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(PlugActivity.this, R.string.network_error);
                return;
            }
            if (!mMokoDevice.isOnline) {
                ToastUtils.showToast(PlugActivity.this, R.string.device_offline);
                return;
            }
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(PlugActivity.this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            setTimer(dialog.getWvHour(), dialog.getWvMinute());
            dialog.dismiss();
        });
        timerDialog.show(getSupportFragmentManager());
    }

    private void setTimer(int hour, int minute) {
        int countdown = hour * 3600 + minute * 60;
        byte[] message = MQTTMessageAssembler.assembleWriteTimer(mMokoDevice.mac, countdown);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onPowerClick(View view) {
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(PlugActivity.this, R.string.network_error);
            return;
        }
        if (!mMokoDevice.isOnline) {
            ToastUtils.showToast(PlugActivity.this, R.string.device_offline);
            return;
        }
        // Power
        Intent intent = new Intent(this, ElectricityActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(intent);
    }

    public void onEnergyClick(View view) {
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mMokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        // Energy
        Intent intent = new Intent(this, EnergyActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(intent);
    }

    public void onSwitchClick(View view) {
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mMokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        XLog.i("切换开关");
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        changeSwitch();
    }

    private void changeSwitch() {
        mMokoDevice.on_off = !mMokoDevice.on_off;
        byte[] message = MQTTMessageAssembler.assembleWriteSwitchInfo(mMokoDevice.mac, mMokoDevice.on_off ? 1 : 0);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getSwitchInfo() {
        XLog.i("读取开关状态");
        byte[] message = MQTTMessageAssembler.assembleReadSwitchInfo(mMokoDevice.mac);
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
