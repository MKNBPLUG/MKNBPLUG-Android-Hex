package com.moko.mknbplughex.activity;

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
import com.moko.mknbplughex.entity.MokoDevice;
import com.moko.mknbplughex.utils.SPUtiles;
import com.moko.support.hex.MQTTConstants;
import com.moko.support.hex.MQTTMessageAssembler;
import com.moko.support.hex.MQTTSupport;
import com.moko.support.hex.entity.MQTTConfig;
import com.moko.support.hex.event.DeviceOnlineEvent;
import com.moko.support.hex.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;


public class SettingForDeviceActivity extends BaseActivity {

    public static String TAG = SettingForDeviceActivity.class.getSimpleName();
    @BindView(R2.id.tv_type)
    TextView tvType;
    @BindView(R2.id.tv_host)
    TextView tvHost;
    @BindView(R2.id.tv_port)
    TextView tvPort;
    @BindView(R2.id.tv_client_id)
    TextView tvClientId;
    @BindView(R2.id.tv_user_name)
    TextView tvUserName;
    @BindView(R2.id.tv_password)
    TextView tvPassword;
    @BindView(R2.id.tv_clean_session)
    TextView tvCleanSession;
    @BindView(R2.id.tv_qos)
    TextView tvQos;
    @BindView(R2.id.tv_keep_alive)
    TextView tvKeepAlive;
    @BindView(R2.id.tv_lwt)
    TextView tvLwt;
    @BindView(R2.id.tv_lwt_retain)
    TextView tvLwtRetain;
    @BindView(R2.id.tv_lwt_qos)
    TextView tvLwtQos;
    @BindView(R2.id.tv_lwt_topic)
    TextView tvLwtTopic;
    @BindView(R2.id.tv_lwt_payload)
    TextView tvLwtPayload;
    @BindView(R2.id.tv_device_id)
    TextView tvDeviceId;
    @BindView(R2.id.tv_subscribe_topic)
    TextView tvSubscribeTopic;
    @BindView(R2.id.tv_publish_topic)
    TextView tvPublishTopic;

    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;

    public Handler mHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_for_device);
        ButterKnife.bind(this);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);

        mHandler = new Handler(Looper.getMainLooper());
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getMQTTHost();
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
        if (cmd == MQTTConstants.MSG_ID_MQTT_HOST && flag == 0) {
            if (dataLength < 1 || dataLength > 64) {
                return;
            }
            tvHost.setText(new String(data));
            tvDeviceId.setText(mMokoDevice.deviceId);
            getMQTTPort();
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_PORT && flag == 0) {
            if (dataLength != 2) {
                return;
            }
            tvPort.setText(String.valueOf(MokoUtils.toInt(data)));
            getMQTTUsername();
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_USERNAME && flag == 0) {
            if (dataLength > 0) {
                tvUserName.setText(new String(data));
            }
            getMQTTPassword();
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_PASSWORD && flag == 0) {
            if (dataLength > 0) {
                tvPassword.setText(new String(data));
            }
            getMQTTClientId();
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_CLIENT_ID && flag == 0) {
            if (dataLength < 1 || dataLength > 64) {
                return;
            }
            tvClientId.setText(new String(data));
            getMQTTCleanSession();
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_CLEAN_SESSION && flag == 0) {
            if (dataLength != 1) {
                return;
            }
            tvCleanSession.setText(data[0] == 0 ? "NO" : "YES");
            getMQTTQos();
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_QOS && flag == 0) {
            if (dataLength != 1) {
                return;
            }
            tvQos.setText(String.valueOf(data[0]));
            getMQTTSubscribeTopic();
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_SUBSCRIBE_TOPIC && flag == 0) {
            if (dataLength < 1 || dataLength > 128) {
                return;
            }
            tvSubscribeTopic.setText(new String(data));
            getMQTTPublishTopic();
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_PUBLISH_TOPIC && flag == 0) {
            if (dataLength < 1 || dataLength > 128) {
                return;
            }
            tvPublishTopic.setText(new String(data));
            getMQTTKeepAlive();
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_KEEP_ALIVE && flag == 0) {
            if (dataLength != 1) {
                return;
            }
            tvKeepAlive.setText(String.valueOf(data[0] & 0xFF));
            getLWTEnable();
        }
        if (cmd == MQTTConstants.MSG_ID_LWT_ENABLE && flag == 0) {
            if (dataLength != 1) {
                return;
            }
            tvLwt.setText(String.valueOf(data[0] & 0xFF));
            getLWTQos();
        }
        if (cmd == MQTTConstants.MSG_ID_LWT_QOS && flag == 0) {
            if (dataLength != 1) {
                return;
            }
            tvLwtQos.setText(String.valueOf(data[0] & 0xFF));
            getLWTRetain();
        }
        if (cmd == MQTTConstants.MSG_ID_LWT_RETAIN && flag == 0) {
            if (dataLength != 1) {
                return;
            }
            tvLwtRetain.setText(String.valueOf(data[0] & 0xFF));
            getLWTTopic();
        }
        if (cmd == MQTTConstants.MSG_ID_LWT_TOPIC && flag == 0) {
            if (dataLength < 1 || dataLength > 128) {
                return;
            }
            tvLwtRetain.setText(new String(data));
            getLWTPayload();
        }
        if (cmd == MQTTConstants.MSG_ID_LWT_MESSAGE && flag == 0) {
            if (dataLength < 1 || dataLength > 128) {
                return;
            }
            tvLwtPayload.setText(new String(data));
            getMQTTEncryptionType();
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_SSL && flag == 0) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 1) {
                return;
            }
            if (data[0] == 0) {
                tvType.setText(getString(R.string.mqtt_connct_mode_tcp));
            } else {
                tvType.setText("SSL");
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


    private void getMQTTHost() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadMQTTHost(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getMQTTPort() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadMQTTPort(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getMQTTUsername() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadMQTTUsername(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getMQTTPassword() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadMQTTPassword(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getMQTTClientId() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadMQTTClientId(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getMQTTCleanSession() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadMQTTCleanSession(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getMQTTKeepAlive() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadMQTTKeepAlive(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getMQTTQos() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadMQTTQos(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getMQTTSubscribeTopic() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadMQTTSubscribeTopic(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getMQTTPublishTopic() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadMQTTPublishTopic(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getLWTEnable() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadLWTEnable(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getLWTQos() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadLWTQos(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getLWTRetain() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadLWTRetain(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getLWTTopic() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadLWTTopic(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getLWTPayload() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadLWTPayload(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getMQTTEncryptionType() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleReadMQTTEncryptionType(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
