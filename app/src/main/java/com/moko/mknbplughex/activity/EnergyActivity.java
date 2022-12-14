package com.moko.mknbplughex.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplughex.AppConstants;
import com.moko.mknbplughex.R;
import com.moko.mknbplughex.R2;
import com.moko.mknbplughex.adapter.EnergyListAdapter;
import com.moko.mknbplughex.base.BaseActivity;
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

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;


public class EnergyActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener {

    @BindView(R2.id.rg_energy)
    RadioGroup rgEnergy;
    @BindView(R2.id.tv_energy_total)
    TextView tvEnergyTotal;
    @BindView(R2.id.tv_duration)
    TextView tvDuration;
    @BindView(R2.id.tv_unit)
    TextView tvUnit;
    @BindView(R2.id.rv_energy)
    RecyclerView rvEnergy;
    @BindView(R2.id.rb_hourly)
    RadioButton rbHourly;
    @BindView(R2.id.rb_daily)
    RadioButton rbDaily;
    @BindView(R2.id.rb_totally)
    RadioButton rbTotally;
    @BindView(R2.id.cl_energy)
    ConstraintLayout clEnergy;
    @BindView(R2.id.tv_energy_desc)
    TextView tvEnergyDesc;
    @BindView(R2.id.tv_title)
    TextView tvTitle;
    private EnergyListAdapter adapter;
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private List<EnergyInfo> energyInfoList;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_energy);
        ButterKnife.bind(this);
        if (getIntent().getExtras() != null) {
            mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
            tvTitle.setText(mMokoDevice.name);
        }
        String mqttConfigAppStr = SPUtils.getStringValue(EnergyActivity.this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mHandler = new Handler(Looper.getMainLooper());
        energyInfoList = new ArrayList<>();
        adapter = new EnergyListAdapter();
        adapter.openLoadAnimation();
        adapter.replaceData(energyInfoList);
        rvEnergy.setLayoutManager(new LinearLayoutManager(this));
        rvEnergy.setAdapter(adapter);
        rgEnergy.setOnCheckedChangeListener(this);
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getEnergyHourly();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        // ?????????????????????????????????
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
        if (cmd == MQTTConstants.NOTIFY_MSG_ID_ENERGY_HOURLY
                || cmd == MQTTConstants.READ_MSG_ID_ENERGY_HOURLY) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength == 0) return;
            if (!rbHourly.isChecked()) return;
            int timestamp = MokoUtils.toInt(Arrays.copyOfRange(data, 0, 4));
            int timeZone = data[4];
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timestamp * 1000L);
            int min = (Math.abs(timeZone) % 2 == 1) ? 30 : 0;
            String timeZoneId = timeZone < 0 ?
                    String.format("GMT-%02d:%02d", Math.abs(timeZone) / 2, min)
                    : String.format("GMT+%02d:%02d", Math.abs(timeZone) / 2, min);
            String dateStr = Utils.calendar2strDate(calendar, AppConstants.PATTERN_YYYY_MM_DD_HH_MM, timeZoneId);
            tvDuration.setText(String.format("00:00 to %s:00,%s", dateStr.substring(11, 13), dateStr.substring(5, 10)));
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
            tvEnergyTotal.setText(MokoUtils.getDecimalFormat("0.##").format(energyDataSum * 0.01f));
        }
        if (cmd == MQTTConstants.NOTIFY_MSG_ID_ENERGY_DAILY
                || cmd == MQTTConstants.READ_MSG_ID_ENERGY_DAILY) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength < 8 || dataLength > 66) return;
            if (!rbDaily.isChecked()) return;
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
            tvDuration.setText(String.format("%s to %s", start.substring(5, 10), end.substring(5, 10)));
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
            tvEnergyTotal.setText(MokoUtils.getDecimalFormat("0.##").format(energyDataSum * 0.01f));
        }
        if (cmd == MQTTConstants.NOTIFY_MSG_ID_ENERGY_TOTAL
                || cmd == MQTTConstants.READ_MSG_ID_ENERGY_TOTAL) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 9) return;
            if (!rbTotally.isChecked()) return;
            int total = MokoUtils.toInt(Arrays.copyOfRange(data, 5, dataLength));
            tvEnergyTotal.setText(MokoUtils.getDecimalFormat("0.##").format(total * 0.01f));
        }
        if (cmd == MQTTConstants.CONFIG_MSG_ID_ENERGY_CLEAR) {
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
            energyInfoList.clear();
            adapter.replaceData(energyInfoList);
            tvEnergyTotal.setText("0");
            ToastUtils.showToast(this, "Set up succeed");
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

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
//        String deviceId = event.getDeviceId();
//        if (!mMokoDevice.deviceId.equals(deviceId)) {
//            return;
//        }
//        boolean online = event.isOnline();
//        if (!online)
//            finish();
//    }


    public void onBack(View view) {
        finish();
    }

    private void getEnergyHourly() {
        XLog.i("???????????????????????????");
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadEnergyHourly(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getEnergyDaily() {
        XLog.i("????????????30?????????");
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadEnergyDaily(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getEnergyTotal() {
        XLog.i("?????????????????????");
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadEnergyTotal(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.rb_hourly) {
            // ?????????
            clEnergy.setVisibility(View.VISIBLE);
            tvUnit.setText("Hour");
            tvEnergyDesc.setText("Today energy:");
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                finish();
            }, 30 * 1000);
            getEnergyHourly();
        } else if (checkedId == R.id.rb_daily) {
            // ?????????
            clEnergy.setVisibility(View.VISIBLE);
            tvUnit.setText("Date");
            tvEnergyDesc.setText("Last 30 days energy:");
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                finish();
            }, 30 * 1000);
            getEnergyDaily();
        } else if (checkedId == R.id.rb_totally) {
            // ???????????????
            tvEnergyDesc.setText("Historical total energy:");
            clEnergy.setVisibility(View.GONE);
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                finish();
            }, 30 * 1000);
            getEnergyTotal();
        }
    }

    public void onEmpty(View view) {
        if (isWindowLocked()) {
            return;
        }
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Reset Energy Data");
        dialog.setMessage("After reset, all energy data will be deleted, please confirm again whether to reset it???");
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
        XLog.i("??????????????????");
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleConfigEnergyClear(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
