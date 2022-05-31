package com.moko.mknbplughex.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplughex.AppConstants;
import com.moko.mknbplughex.R;
import com.moko.mknbplughex.R2;
import com.moko.mknbplughex.base.BaseActivity;
import com.moko.mknbplughex.dialog.BottomDialog;
import com.moko.mknbplughex.entity.MokoDevice;
import com.moko.mknbplughex.utils.SPUtiles;
import com.moko.mknbplughex.utils.ToastUtils;
import com.moko.support.hex.MQTTConstants;
import com.moko.support.hex.MQTTMessageAssembler;
import com.moko.support.hex.MQTTSupport;
import com.moko.support.hex.entity.MQTTConfig;
import com.moko.support.hex.event.DeviceOnlineEvent;
import com.moko.support.hex.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SystemTimeActivity extends BaseActivity {

    @BindView(R2.id.tv_time_zone)
    TextView tvTimeZone;
    @BindView(R2.id.tv_device_time)
    TextView tvDeviceTime;
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;

    public Handler mHandler;
    public Handler mSyncTimeHandler;

    private ArrayList<String> mTimeZones;
    private int mSelectedTimeZone;
    private String mShowTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_time);
        ButterKnife.bind(this);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        mTimeZones = new ArrayList<>();
        for (int i = 0; i <= 52; i++) {
            if (i < 24) {
                mTimeZones.add(String.format("UTC-%02d:%02d", (24 - i) / 2, ((i % 2 == 1) ? 30 : 00)));
            } else if (i == 24) {
                mTimeZones.add("UTC+00:00");
            } else {
                mTimeZones.add(String.format("UTC+%02d:%02d", (i - 24) / 2, ((i % 2 == 1) ? 30 : 00)));
            }
        }
        mHandler = new Handler(Looper.getMainLooper());
        mSyncTimeHandler = new Handler(Looper.getMainLooper());
        Calendar calendar = Calendar.getInstance();
        mShowTime = MokoUtils.calendar2strDate(calendar, AppConstants.PATTERN_YYYY_MM_DD_HH_MM);
        mSelectedTimeZone = 40;
        tvDeviceTime.setText(String.format("Device time:%s %s", mShowTime, mTimeZones.get(mSelectedTimeZone)));
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getTimeZone();
        getSystemTime();
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
        if (cmd == MQTTConstants.MSG_ID_TIMEZONE && flag == 0) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 1)
                mSelectedTimeZone = data[0] + 24;
            tvTimeZone.setText(mTimeZones.get(mSelectedTimeZone));
            tvDeviceTime.setText(String.format("Device time:%s %s", mShowTime, mTimeZones.get(mSelectedTimeZone)));
        }
        if (cmd == MQTTConstants.MSG_ID_SYSTEM_TIME && flag == 0) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 4)
                return;
            int time = MokoUtils.toInt(data);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(time * 1000L);
            mShowTime = MokoUtils.calendar2strDate(calendar, AppConstants.PATTERN_YYYY_MM_DD_HH_MM);
            tvDeviceTime.setText(String.format("Device time:%s %s", mShowTime, mTimeZones.get(mSelectedTimeZone)));
            if (mSyncTimeHandler.hasMessages(0))
                mSyncTimeHandler.removeMessages(0);
            mSyncTimeHandler.postDelayed(() -> {
                getSystemTime();
            }, 30 * 1000);
        }
        if (flag == 1 && (cmd == MQTTConstants.MSG_ID_TIMEZONE
                || cmd == MQTTConstants.MSG_ID_SYSTEM_TIME)) {
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
        if (!online)
            finish();
    }

    public void onBack(View view) {
        finish();
    }

    private void getTimeZone() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadTimeZone(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setTimeZone() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        int time_zone = mSelectedTimeZone - 24;
        byte[] message = MQTTMessageAssembler.assembleWriteTimeZone(mMokoDevice.deviceId, time_zone);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private void getSystemTime() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadSystemTime(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setSystemTime() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        Calendar calendar = Calendar.getInstance();
        int time = (int) (calendar.getTimeInMillis() / 1000);
        mShowTime = MokoUtils.calendar2strDate(calendar, AppConstants.PATTERN_YYYY_MM_DD_HH_MM);
        tvDeviceTime.setText(String.format("Device time:%s %s", mShowTime, mTimeZones.get(mSelectedTimeZone)));
        byte[] message = MQTTMessageAssembler.assembleWriteSystemTime(mMokoDevice.deviceId, time);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onSelectTimeZoneClick(View view) {
        if (isWindowLocked())
            return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mTimeZones, mSelectedTimeZone);
        dialog.setListener(value -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(this, R.string.network_error);
                return;
            }
            mSelectedTimeZone = value;
            tvTimeZone.setText(mTimeZones.get(mSelectedTimeZone));
            tvDeviceTime.setText(String.format("Device time:%s %s", mShowTime, mTimeZones.get(mSelectedTimeZone)));
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            setTimeZone();
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onSyncClick(View view) {
        if (isWindowLocked())
            return;
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        setSystemTime();
    }

    public void onSyncTimeFromNTPClick(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, SyncTimeFromNTPActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSyncTimeHandler.hasMessages(0))
            mSyncTimeHandler.removeMessages(0);
        if (mHandler.hasMessages(0))
            mHandler.removeMessages(0);
    }

}
