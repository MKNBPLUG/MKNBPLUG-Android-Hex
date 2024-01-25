package com.moko.mknbplughex.activity;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioGroup;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplughex.AppConstants;
import com.moko.mknbplughex.R;
import com.moko.mknbplughex.adapter.EnergyListAdapter;
import com.moko.mknbplughex.base.BaseActivity;
import com.moko.mknbplughex.databinding.ActivityEnergyBinding;
import com.moko.mknbplughex.dialog.AlertMessageDialog;
import com.moko.mknbplughex.entity.MokoDevice;
import com.moko.mknbplughex.utils.SPUtils;
import com.moko.mknbplughex.utils.ToastUtils;
import com.moko.mknbplughex.utils.Utils;
import com.moko.support.hex.MQTTConstants;
import com.moko.support.hex.MQTTMessageAssembler;
import com.moko.support.hex.MQTTSupport;
import com.moko.support.hex.entity.EnergyInfo;
import com.moko.support.hex.entity.MQTTConfig;
import com.moko.support.hex.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class EnergyActivity extends BaseActivity<ActivityEnergyBinding> implements RadioGroup.OnCheckedChangeListener {
    private EnergyListAdapter adapter;
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private List<EnergyInfo> energyInfoList;
    private Handler mHandler;

    @Override
    protected void onCreate() {
        if (getIntent().getExtras() != null) {
            mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
            mBind.tvTitle.setText(mMokoDevice.name);
        }
        String mqttConfigAppStr = SPUtils.getStringValue(EnergyActivity.this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mHandler = new Handler(Looper.getMainLooper());
        energyInfoList = new ArrayList<>();
        adapter = new EnergyListAdapter();
        adapter.openLoadAnimation();
        adapter.replaceData(energyInfoList);
        mBind.rvEnergy.setLayoutManager(new LinearLayoutManager(this));
        mBind.rvEnergy.setAdapter(adapter);
        mBind.rgEnergy.setOnCheckedChangeListener(this);
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getEnergyHourly();
    }

    @Override
    protected ActivityEnergyBinding getViewBinding() {
        return ActivityEnergyBinding.inflate(getLayoutInflater());
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
        if (cmd == MQTTConstants.NOTIFY_MSG_ID_ENERGY_HOURLY
                || cmd == MQTTConstants.READ_MSG_ID_ENERGY_HOURLY) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength == 0) return;
            if (!mBind.rbHourly.isChecked()) return;
            int timestamp = MokoUtils.toInt(Arrays.copyOfRange(data, 0, 4));
            int timeZone = data[4];
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timestamp * 1000L);
            int min = (Math.abs(timeZone) % 2 == 1) ? 30 : 0;
            String timeZoneId = timeZone < 0 ?
                    String.format("GMT-%02d:%02d", Math.abs(timeZone) / 2, min)
                    : String.format("GMT+%02d:%02d", Math.abs(timeZone) / 2, min);
            String dateStr = Utils.calendar2strDate(calendar, AppConstants.PATTERN_YYYY_MM_DD_HH_MM, timeZoneId);
            mBind.tvDuration.setText(String.format("00:00 to %s:00,%s", dateStr.substring(11, 13), dateStr.substring(5, 10)));
            byte[] energyBytes = Arrays.copyOfRange(data, 6, dataLength);
            energyInfoList.clear();
            int energyDataSum = 0;
            for (int i = 0, j = 0; i < energyBytes.length; i += 2, j++) {
                int energyInt = MokoUtils.toInt(Arrays.copyOfRange(energyBytes, i, 2 + i));
                energyDataSum += energyInt;
                EnergyInfo energyInfo = new EnergyInfo();
                energyInfo.time = String.format("%02d:00", j);
                energyInfo.value = MokoUtils.getDecimalFormat("0.##").format(energyInt * 0.01f);
                energyInfoList.add(0, energyInfo);
            }
            adapter.replaceData(energyInfoList);
            mBind.tvEnergyTotal.setText(MokoUtils.getDecimalFormat("0.##").format(energyDataSum * 0.01f));
        }
        if (cmd == MQTTConstants.NOTIFY_MSG_ID_ENERGY_DAILY
                || cmd == MQTTConstants.READ_MSG_ID_ENERGY_DAILY) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength < 8 || dataLength > 66) return;
            if (!mBind.rbDaily.isChecked()) return;
            int timestamp = MokoUtils.toInt(Arrays.copyOfRange(data, 0, 4));
            int timeZone = data[4];
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timestamp * 1000L);
            int min = (Math.abs(timeZone) % 2 == 1) ? 30 : 00;
            String timeZoneId = timeZone < 0 ?
                    String.format("GMT-%02d:%02d", Math.abs(timeZone) / 2, min)
                    : String.format("GMT+%02d:%02d", Math.abs(timeZone) / 2, min);
            String end = Utils.calendar2strDate(calendar, AppConstants.PATTERN_YYYY_MM_DD_HH_MM, timeZoneId);
            Calendar startCalendar = (Calendar) calendar.clone();
            int count = data[5] & 0xFF;
            startCalendar.add(Calendar.DAY_OF_MONTH, -(count - 1));
            String start = Utils.calendar2strDate(startCalendar, AppConstants.PATTERN_YYYY_MM_DD_HH_MM, timeZoneId);
            mBind.tvDuration.setText(String.format("%s to %s", start.substring(5, 10), end.substring(5, 10)));
            byte[] energyBytes = Arrays.copyOfRange(data, 6, dataLength);
            energyInfoList.clear();
            int energyDataSum = 0;
            for (int i = 0; i < energyBytes.length; i += 2) {
                int energyInt = MokoUtils.toInt(Arrays.copyOfRange(energyBytes, i, 2 + i));
                energyDataSum += energyInt;
                EnergyInfo energyInfo = new EnergyInfo();
                String dateStr = Utils.calendar2strDate(calendar, AppConstants.PATTERN_YYYY_MM_DD_HH_MM, timeZoneId);
                energyInfo.time = dateStr.substring(5, 10);
                energyInfo.value = MokoUtils.getDecimalFormat("0.##").format(energyInt * 0.01f);
                energyInfoList.add(energyInfo);
                calendar.add(Calendar.DAY_OF_MONTH, -1);
            }
            adapter.replaceData(energyInfoList);
            mBind.tvEnergyTotal.setText(MokoUtils.getDecimalFormat("0.##").format(energyDataSum * 0.01f));
        }
        if (cmd == MQTTConstants.NOTIFY_MSG_ID_ENERGY_TOTAL
                || cmd == MQTTConstants.READ_MSG_ID_ENERGY_TOTAL) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 9) return;
            if (!mBind.rbTotally.isChecked()) return;
            int total = MokoUtils.toInt(Arrays.copyOfRange(data, 5, dataLength));
            mBind.tvEnergyTotal.setText(MokoUtils.getDecimalFormat("0.##").format(total * 0.01f));
        }
        if (cmd == MQTTConstants.CONFIG_MSG_ID_ENERGY_CLEAR) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 1) return;
            if (data[0] == 0) {
                ToastUtils.showToast(this, "Set up failed");
                return;
            }
            energyInfoList.clear();
            adapter.replaceData(energyInfoList);
            mBind.tvEnergyTotal.setText("0");
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

    private void getEnergyHourly() {
        XLog.i("查询当天每小时电能");
        byte[] message = MQTTMessageAssembler.assembleReadEnergyHourly(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getEnergyDaily() {
        XLog.i("查询最近30天电能");
        byte[] message = MQTTMessageAssembler.assembleReadEnergyDaily(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getEnergyTotal() {
        XLog.i("查询总累计电能");
        byte[] message = MQTTMessageAssembler.assembleReadEnergyTotal(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.rb_hourly) {
            // 切换日
            mBind.clEnergy.setVisibility(View.VISIBLE);
            mBind.tvUnit.setText("Hour");
            mBind.tvEnergyDesc.setText("Today energy:");
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                finish();
            }, 30 * 1000);
            getEnergyHourly();
        } else if (checkedId == R.id.rb_daily) {
            // 切换月
            mBind.clEnergy.setVisibility(View.VISIBLE);
            mBind.tvUnit.setText("Date");
            mBind.tvEnergyDesc.setText("Last 30 days energy:");
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                finish();
            }, 30 * 1000);
            getEnergyDaily();
        } else if (checkedId == R.id.rb_totally) {
            // 切换总电能
            mBind.tvEnergyDesc.setText("Historical total energy:");
            mBind.clEnergy.setVisibility(View.GONE);
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                finish();
            }, 30 * 1000);
            getEnergyTotal();
        }
    }

    public void onEmpty(View view) {
        if (isWindowLocked()) return;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Reset Energy Data");
        dialog.setMessage("After reset, all energy data will be deleted, please confirm again whether to reset it？");
        dialog.setOnAlertConfirmListener(() -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(this, R.string.network_error);
                return;
            }
            if (!mMokoDevice.isOnline) {
                ToastUtils.showToast(this, R.string.device_offline);
                return;
            }
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            clearEnergy();
        });
        dialog.show(getSupportFragmentManager());
    }

    private void clearEnergy() {
        XLog.i("清除电能数据");
        byte[] message = MQTTMessageAssembler.assembleConfigEnergyClear(mMokoDevice.mac);
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
