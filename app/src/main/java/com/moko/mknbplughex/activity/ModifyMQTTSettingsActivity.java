package com.moko.mknbplughex.activity;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplughex.AppConstants;
import com.moko.mknbplughex.R;
import com.moko.mknbplughex.R2;
import com.moko.mknbplughex.adapter.MQTTFragmentAdapter;
import com.moko.mknbplughex.base.BaseActivity;
import com.moko.mknbplughex.db.DBTools;
import com.moko.mknbplughex.dialog.BottomDialog;
import com.moko.mknbplughex.entity.MokoDevice;
import com.moko.mknbplughex.fragment.GeneralDeviceFragment;
import com.moko.mknbplughex.fragment.LWTFragment;
import com.moko.mknbplughex.fragment.SSLDevicePathFragment;
import com.moko.mknbplughex.fragment.UserDeviceFragment;
import com.moko.mknbplughex.utils.SPUtiles;
import com.moko.mknbplughex.utils.ToastUtils;
import com.moko.support.hex.MQTTConstants;
import com.moko.support.hex.MQTTMessageAssembler;
import com.moko.support.hex.MQTTSupport;
import com.moko.support.hex.entity.APNSettings;
import com.moko.support.hex.entity.LWTSettings;
import com.moko.support.hex.entity.MQTTConfig;
import com.moko.support.hex.entity.MQTTSettings;
import com.moko.support.hex.event.DeviceOnlineEvent;
import com.moko.support.hex.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import butterknife.BindView;
import butterknife.ButterKnife;

public class ModifyMQTTSettingsActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener {
    public static String TAG = ModifyMQTTSettingsActivity.class.getSimpleName();
    private final String FILTER_ASCII = "[ -~]*";
    @BindView(R2.id.et_mqtt_host)
    EditText etMqttHost;
    @BindView(R2.id.et_mqtt_port)
    EditText etMqttPort;
    @BindView(R2.id.et_mqtt_client_id)
    EditText etMqttClientId;
    @BindView(R2.id.et_mqtt_subscribe_topic)
    EditText etMqttSubscribeTopic;
    @BindView(R2.id.et_mqtt_publish_topic)
    EditText etMqttPublishTopic;
    @BindView(R2.id.rb_general)
    RadioButton rbGeneral;
    @BindView(R2.id.rb_user)
    RadioButton rbUser;
    @BindView(R2.id.rb_ssl)
    RadioButton rbSsl;
    @BindView(R2.id.rb_lwt)
    RadioButton rbLwt;
    @BindView(R2.id.vp_mqtt)
    ViewPager2 vpMqtt;
    @BindView(R2.id.rg_mqtt)
    RadioGroup rgMqtt;
    @BindView(R2.id.et_apn)
    EditText etApn;
    @BindView(R2.id.et_apn_username)
    EditText etApnUsername;
    @BindView(R2.id.et_apn_password)
    EditText etApnPassword;
    @BindView(R2.id.tv_network_priority)
    TextView tvNetworkPriority;

    private GeneralDeviceFragment generalFragment;
    private UserDeviceFragment userFragment;
    private SSLDevicePathFragment sslFragment;
    private LWTFragment lwtFragment;
    private MQTTFragmentAdapter adapter;
    private ArrayList<Fragment> fragments;
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private MQTTSettings mMQTTSettings;
    private LWTSettings mLWTSettings;
    private APNSettings mAPNSettings;
    private ArrayList<String> mNetworkPriority;
    private int mSelectedNetworkPriority;

    public Handler mHandler;

    private InputFilter filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mqtt_device_modify);
        ButterKnife.bind(this);

        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMQTTSettings = new MQTTSettings();
        mLWTSettings = new LWTSettings();
        mAPNSettings = new APNSettings();
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }

            return null;
        };
        etMqttHost.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        etMqttClientId.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        etMqttSubscribeTopic.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        etMqttPublishTopic.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        etApn.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100), filter});
        etApnUsername.setFilters(new InputFilter[]{new InputFilter.LengthFilter(127), filter});
        etApnPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(127), filter});
        mNetworkPriority = new ArrayList<>();
        mNetworkPriority.add("eMTC->NB-IOT->GSM");
        mNetworkPriority.add("eMTC-> GSM -> NB-IOT");
        mNetworkPriority.add("NB-IOT->GSM-> eMTC");
        mNetworkPriority.add("NB-IOT-> eMTC-> GSM");
        mNetworkPriority.add("GSM -> NB-IOT-> eMTC");
        mNetworkPriority.add("GSM -> eMTC->NB-IOT");
        mNetworkPriority.add("eMTC->NB-IOT");
        mNetworkPriority.add("NB-IOT-> eMTC");
        mNetworkPriority.add("GSM");
        mNetworkPriority.add("NB-IOT");
        mNetworkPriority.add("eMTC");
        createFragment();
        initData();
        adapter = new MQTTFragmentAdapter(this);
        adapter.setFragmentList(fragments);
        vpMqtt.setAdapter(adapter);
        vpMqtt.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    rbGeneral.setChecked(true);
                } else if (position == 1) {
                    rbUser.setChecked(true);
                } else if (position == 2) {
                    rbSsl.setChecked(true);
                } else if (position == 3) {
                    rbLwt.setChecked(true);
                }
            }
        });
        vpMqtt.setOffscreenPageLimit(4);
        rgMqtt.setOnCheckedChangeListener(this);
        mHandler = new Handler(Looper.getMainLooper());
    }

    private void createFragment() {
        fragments = new ArrayList<>();
        generalFragment = GeneralDeviceFragment.newInstance();
        userFragment = UserDeviceFragment.newInstance();
        sslFragment = SSLDevicePathFragment.newInstance();
        lwtFragment = LWTFragment.newInstance();
        fragments.add(generalFragment);
        fragments.add(userFragment);
        fragments.add(sslFragment);
        fragments.add(lwtFragment);
    }

    private void initData() {
        generalFragment.setCleanSession(mMQTTSettings.clean_session == 1);
        generalFragment.setQos(mMQTTSettings.qos);
        generalFragment.setKeepAlive(mMQTTSettings.keepalive);
        lwtFragment.setQos(mLWTSettings.lwt_qos);
        lwtFragment.setTopic(mLWTSettings.lwt_topic);
        lwtFragment.setPayload(mLWTSettings.lwt_message);
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
            if (dataLength != 1)
                return;
            if (data[0] != 0) {
                ToastUtils.showToast(this, "Device is OTA, please wait");
                return;
            }
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }, 60 * 1000);
            setMQTTHost();
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_HOST && flag == 1) {
            if (dataLength == 1 && data[0] == 1)
                setMQTTPort();
            else {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_PORT && flag == 1) {
            if (dataLength == 1 && data[0] == 1)
                setMQTTUsername();
            else {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_USERNAME && flag == 1) {
            if (dataLength == 1 && data[0] == 1)
                setMQTTPassword();
            else {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_PASSWORD && flag == 1) {
            if (dataLength == 1 && data[0] == 1)
                setMQTTClientId();
            else {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_CLIENT_ID && flag == 1) {
            if (dataLength == 1 && data[0] == 1)
                setMQTTCleanSession();
            else {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_CLEAN_SESSION && flag == 1) {
            if (dataLength == 1 && data[0] == 1)
                setMQTTKeepAlive();
            else {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_KEEP_ALIVE && flag == 1) {
            if (dataLength == 1 && data[0] == 1)
                setMQTTQos();
            else {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_QOS && flag == 1) {
            if (dataLength == 1 && data[0] == 1)
                setMQTTSubscribeTopic();
            else {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_SUBSCRIBE_TOPIC && flag == 1) {
            if (dataLength == 1 && data[0] == 1)
                setMQTTPublishTopic();
            else {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_PUBLISH_TOPIC && flag == 1) {
            if (dataLength == 1 && data[0] == 1)
                setLWTEnable();
            else {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.MSG_ID_LWT_ENABLE && flag == 1) {
            if (dataLength == 1 && data[0] == 1)
                setLWTRetain();
            else {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.MSG_ID_LWT_RETAIN && flag == 1) {
            if (dataLength == 1 && data[0] == 1)
                setLWTQos();
            else {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.MSG_ID_LWT_QOS && flag == 1) {
            if (dataLength == 1 && data[0] == 1)
                setLWTTopic();
            else {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.MSG_ID_LWT_TOPIC && flag == 1) {
            if (dataLength == 1 && data[0] == 1)
                setLWTPayload();
            else {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.MSG_ID_LWT_MESSAGE && flag == 1) {
            if (dataLength == 1 && data[0] == 1)
                setAPN();
            else {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.MSG_ID_APN && flag == 1) {
            if (dataLength == 1 && data[0] == 1)
                setAPNUsername();
            else {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.MSG_ID_APN_USERNAME && flag == 1) {
            if (dataLength == 1 && data[0] == 1)
                setAPNPassword();
            else {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.MSG_ID_APN_PASSWORD && flag == 1) {
            if (dataLength == 1 && data[0] == 1)
                setNetworkPriority();
            else {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.MSG_ID_NETWORK_PRIORITY && flag == 1) {
            if (dataLength == 1 && data[0] == 1)
                setMQTTEncryptionType();
            else {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_SSL && flag == 1) {
            if (dataLength == 1 && data[0] == 1) {
                if (mMQTTSettings.encryption_type == 0) {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    showLoadingProgressDialog();
                    mHandler.postDelayed(() -> {
                        dismissLoadingProgressDialog();
                        ToastUtils.showToast(this, "Setup failed, please try it again!");
                    }, 30 * 1000);
                    setConfigFinish();
                } else if (mMQTTSettings.encryption_type == 1) {
                    setCACertFile();
                } else {
                    setSelfSingleServerCertificates();
                }
            } else {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.CONFIG_MSG_ID_CA_CERTIFICATE) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength == 1 && data[0] == 1) {
                showLoadingProgressDialog();
                mHandler.postDelayed(() -> {
                    dismissLoadingProgressDialog();
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }, 30 * 1000);
                setConfigFinish();
            } else {
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.CONFIG_MSG_ID_SELF_SIGNED_SERVER_CERTIFICATES) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength == 1 && data[0] == 1) {
                showLoadingProgressDialog();
                mHandler.postDelayed(() -> {
                    dismissLoadingProgressDialog();
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }, 30 * 1000);
                setConfigFinish();
            } else {
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }
        }
        if (cmd == MQTTConstants.CONFIG_MSG_ID_MQTT_CONFIG_FINISH) {
            if (dataLength != 1)
                return;
            if (data[0] == 0) {
                ToastUtils.showToast(this, "Set up failed");
                return;
            }
        }
        if (cmd == MQTTConstants.NOTIFY_MSG_ID_RECONNECT_READY_RESULT) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (data[5] == 0) {
                ToastUtils.showToast(this, "Setup failed, please try it again!");
                return;
            }
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }, 30 * 1000);
            // 切换服务器
            setDeviceReconnect();
        }
        if (cmd == MQTTConstants.CONFIG_MSG_ID_MQTT_RECONNECT) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (dataLength != 1)
                return;
            if (data[0] == 0) {
                ToastUtils.showToast(this, "Setup failed, please try it again!");
                return;
            }
            MQTTConfig mqttConfig = new Gson().fromJson(mMokoDevice.mqttInfo, MQTTConfig.class);
            mqttConfig.topicPublish = mMQTTSettings.publish_topic;
            mqttConfig.topicSubscribe = mMQTTSettings.subscribe_topic;
            mMokoDevice.topicPublish = mMQTTSettings.publish_topic;
            mMokoDevice.topicSubscribe = mMQTTSettings.subscribe_topic;
            mMokoDevice.mqttInfo = new Gson().toJson(mqttConfig, MQTTConfig.class);
            DBTools.getInstance(this).updateDevice(mMokoDevice);
            // 跳转首页，刷新数据
            Intent intent = new Intent(this, HEXMainActivity.class);
            intent.putExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY, TAG);
            intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_ID, mMokoDevice.deviceId);
            startActivity(intent);
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

    public void onSave(View view) {
        if (isWindowLocked())
            return;
        if (isValid()) {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            saveParams();
        }
    }


    public void onSelectCertificate(View view) {
        if (isWindowLocked())
            return;
        sslFragment.selectCertificate();
    }


    private void saveParams() {
        final String host = etMqttHost.getText().toString().trim();
        final String port = etMqttPort.getText().toString().trim();
        final String clientId = etMqttClientId.getText().toString().trim();
        String topicSubscribe = etMqttSubscribeTopic.getText().toString().trim();
        String topicPublish = etMqttPublishTopic.getText().toString().trim();
        String apn = etApn.getText().toString().trim();
        String apnUsername = etApnUsername.getText().toString().trim();
        String apnPassword = etApnPassword.getText().toString().trim();

        mMQTTSettings.host = host;
        mMQTTSettings.port = Integer.parseInt(port);
        mMQTTSettings.client_id = clientId;
        if ("{device_name}/{device_id}/app_to_device".equals(topicSubscribe)) {
            topicSubscribe = String.format("%s/%s/app_to_device", mMokoDevice.name, mMokoDevice.deviceId);
        }
        if ("{device_name}/{device_id}/device_to_app".equals(topicPublish)) {
            topicPublish = String.format("%s/%s/device_to_app", mMokoDevice.name, mMokoDevice.deviceId);
        }
        mMQTTSettings.subscribe_topic = topicSubscribe;
        mMQTTSettings.publish_topic = topicPublish;

        mMQTTSettings.clean_session = generalFragment.isCleanSession() ? 1 : 0;
        mMQTTSettings.qos = generalFragment.getQos();
        mMQTTSettings.keepalive = generalFragment.getKeepAlive();
        mMQTTSettings.username = userFragment.getUsername();
        mMQTTSettings.password = userFragment.getPassword();
        mMQTTSettings.encryption_type = sslFragment.getConnectMode();
        if (mMQTTSettings.encryption_type > 0) {
            mMQTTSettings.cert_host = sslFragment.getSSLHost();
            mMQTTSettings.cert_port = sslFragment.getSSLPort();
        }
        if (mMQTTSettings.encryption_type == 1) {
            mMQTTSettings.ca_cert_path = sslFragment.getCAPath();
        }
        if (mMQTTSettings.encryption_type == 2) {
            mMQTTSettings.ca_cert_path = sslFragment.getCAPath();
            mMQTTSettings.client_cert_path = sslFragment.getClientCerPath();
            mMQTTSettings.client_key_path = sslFragment.getClientKeyPath();
        }
        mLWTSettings.lwt_enable = lwtFragment.getLwtEnable() ? 1 : 0;
        mLWTSettings.lwt_retain = lwtFragment.getLwtRetain() ? 1 : 0;
        mLWTSettings.lwt_qos = lwtFragment.getQos();
        String lwtTopic = lwtFragment.getTopic();
        if ("{device_name}/{device_id}/device_to_app".equals(lwtTopic)) {
            lwtTopic = String.format("%s/%s/device_to_app", mMokoDevice.name, mMokoDevice.deviceId);
        }
        mLWTSettings.lwt_topic = lwtTopic;
        mLWTSettings.lwt_message = lwtFragment.getPayload();
        mAPNSettings.apn = apn;
        mAPNSettings.apn_username = apnUsername;
        mAPNSettings.apn_password = apnPassword;

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


    private void setMQTTHost() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTHost(mMokoDevice.deviceId, mMQTTSettings.host);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTPort() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTPort(mMokoDevice.deviceId, mMQTTSettings.port);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTUsername() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTUsername(mMokoDevice.deviceId, mMQTTSettings.username);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTPassword() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTPassword(mMokoDevice.deviceId, mMQTTSettings.password);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTClientId() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTClientId(mMokoDevice.deviceId, mMQTTSettings.client_id);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTCleanSession() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTCleanSession(mMokoDevice.deviceId, mMQTTSettings.clean_session);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTKeepAlive() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTKeepAlive(mMokoDevice.deviceId, mMQTTSettings.keepalive);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private void setMQTTQos() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTQos(mMokoDevice.deviceId, mMQTTSettings.qos);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTSubscribeTopic() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTSubscribeTopic(mMokoDevice.deviceId, mMQTTSettings.subscribe_topic);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTPublishTopic() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTPublishTopic(mMokoDevice.deviceId, mMQTTSettings.publish_topic);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setLWTEnable() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteLWTEnable(mMokoDevice.deviceId, mLWTSettings.lwt_enable);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setLWTQos() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteLWTQos(mMokoDevice.deviceId, mLWTSettings.lwt_qos);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setLWTRetain() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteLWTRetain(mMokoDevice.deviceId, mLWTSettings.lwt_retain);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setLWTTopic() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteLWTTopic(mMokoDevice.deviceId, mLWTSettings.lwt_topic);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setLWTPayload() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteLWTPayload(mMokoDevice.deviceId, mLWTSettings.lwt_message);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setAPN() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteAPN(mMokoDevice.deviceId, mAPNSettings.apn);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setAPNUsername() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteAPNUsername(mMokoDevice.deviceId, mAPNSettings.apn_username);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setAPNPassword() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteAPNPassword(mMokoDevice.deviceId, mAPNSettings.apn_password);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setNetworkPriority() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteNetworkPriority(mMokoDevice.deviceId, mSelectedNetworkPriority);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTEncryptionType() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTEncryptionType(mMokoDevice.deviceId, mMQTTSettings.encryption_type);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setCACertFile() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteCACertFile(mMokoDevice.deviceId
                , mMQTTSettings.cert_host
                , mMQTTSettings.cert_port
                , mMQTTSettings.ca_cert_path);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setSelfSingleServerCertificates() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteSelfSingleServerCertificates(mMokoDevice.deviceId
                , mMQTTSettings.cert_host
                , mMQTTSettings.cert_port
                , mMQTTSettings.ca_cert_path
                , mMQTTSettings.client_cert_path
                , mMQTTSettings.client_key_path);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setDeviceReconnect() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteDeviceReconnect(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setConfigFinish() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTConfigFinish(mMokoDevice.deviceId);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private boolean isValid() {
        String host = etMqttHost.getText().toString().trim();
        String port = etMqttPort.getText().toString().trim();
        String clientId = etMqttClientId.getText().toString().trim();
        String topicSubscribe = etMqttSubscribeTopic.getText().toString().trim();
        String topicPublish = etMqttPublishTopic.getText().toString().trim();
        if (TextUtils.isEmpty(host)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_host));
            return false;
        }
        if (TextUtils.isEmpty(port)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_port_empty));
            return false;
        }
        if (Integer.parseInt(port) > 65535) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_port));
            return false;
        }
        if (TextUtils.isEmpty(clientId)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_client_id_empty));
            return false;
        }
        if (TextUtils.isEmpty(topicSubscribe)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_topic_subscribe));
            return false;
        }
        if (TextUtils.isEmpty(topicPublish)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_topic_publish));
            return false;
        }
        if (topicPublish.equals(topicSubscribe)) {
            ToastUtils.showToast(this, "Subscribed and published topic can't be same !");
            return false;
        }
        if (!generalFragment.isValid() || !sslFragment.isValid() || !lwtFragment.isValid())
            return false;
        return true;
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
        if (checkedId == R.id.rb_general)
            vpMqtt.setCurrentItem(0);
        else if (checkedId == R.id.rb_user)
            vpMqtt.setCurrentItem(1);
        else if (checkedId == R.id.rb_ssl)
            vpMqtt.setCurrentItem(2);
        else if (checkedId == R.id.rb_lwt)
            vpMqtt.setCurrentItem(3);
    }

    public void selectNetworkPriority(View view) {
        if (isWindowLocked())
            return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mNetworkPriority, mSelectedNetworkPriority);
        dialog.setListener(value -> {
            mSelectedNetworkPriority = value;
            tvNetworkPriority.setText(mNetworkPriority.get(value));
        });
        dialog.show(getSupportFragmentManager());
    }
}
