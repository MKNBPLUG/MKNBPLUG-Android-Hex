package com.moko.mknbplughex.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioGroup;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.gson.Gson;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplughex.AppConstants;
import com.moko.mknbplughex.R;
import com.moko.mknbplughex.adapter.MQTTFragmentAdapter;
import com.moko.mknbplughex.base.BaseActivity;
import com.moko.mknbplughex.databinding.ActivityMqttDeviceModifyBinding;
import com.moko.mknbplughex.db.DBTools;
import com.moko.mknbplughex.dialog.AlertMessageDialog;
import com.moko.mknbplughex.dialog.BottomDialog;
import com.moko.mknbplughex.entity.MokoDevice;
import com.moko.mknbplughex.fragment.GeneralDeviceFragment;
import com.moko.mknbplughex.fragment.LWTFragment;
import com.moko.mknbplughex.fragment.SSLDevicePathFragment;
import com.moko.mknbplughex.fragment.UserDeviceFragment;
import com.moko.mknbplughex.utils.SPUtils;
import com.moko.mknbplughex.utils.ToastUtils;
import com.moko.support.hex.MQTTConstants;
import com.moko.support.hex.MQTTMessageAssembler;
import com.moko.support.hex.MQTTSupport;
import com.moko.support.hex.entity.APNSettings;
import com.moko.support.hex.entity.LWTSettings;
import com.moko.support.hex.entity.MQTTConfig;
import com.moko.support.hex.entity.MQTTSettings;
import com.moko.support.hex.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;

public class ModifyMQTTSettingsActivity extends BaseActivity<ActivityMqttDeviceModifyBinding> implements RadioGroup.OnCheckedChangeListener {
    public static String TAG = ModifyMQTTSettingsActivity.class.getSimpleName();
    private final String FILTER_ASCII = "[ -~]*";
    private GeneralDeviceFragment generalFragment;
    private UserDeviceFragment userFragment;
    private SSLDevicePathFragment sslFragment;
    private LWTFragment lwtFragment;
    private ArrayList<Fragment> fragments;
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private MQTTSettings mMQTTSettings;
    private LWTSettings mLWTSettings;
    private APNSettings mAPNSettings;
    private ArrayList<String> mNetworkPriority;
    private int mSelectedNetworkPriority;
    public Handler mHandler;

    @Override
    protected void onCreate() {
        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMQTTSettings = new MQTTSettings();
        mLWTSettings = new LWTSettings();
        mAPNSettings = new APNSettings();
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }
            return null;
        };
        mBind.etMqttHost.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        mBind.etMqttClientId.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        mBind.etMqttSubscribeTopic.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        mBind.etMqttPublishTopic.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        mBind.etApn.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100), filter});
        mBind.etApnUsername.setFilters(new InputFilter[]{new InputFilter.LengthFilter(127), filter});
        mBind.etApnPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(127), filter});
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
        MQTTFragmentAdapter adapter = new MQTTFragmentAdapter(this);
        adapter.setFragmentList(fragments);
        mBind.vpMqtt.setAdapter(adapter);
        mBind.vpMqtt.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    mBind.rbGeneral.setChecked(true);
                } else if (position == 1) {
                    mBind.rbUser.setChecked(true);
                } else if (position == 2) {
                    mBind.rbSsl.setChecked(true);
                } else if (position == 3) {
                    mBind.rbLwt.setChecked(true);
                }
            }
        });
        mBind.vpMqtt.setOffscreenPageLimit(4);
        mBind.rgMqtt.setOnCheckedChangeListener(this);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected ActivityMqttDeviceModifyBinding getViewBinding() {
        return ActivityMqttDeviceModifyBinding.inflate(getLayoutInflater());
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
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        readDeviceStatus();
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
        String deviceId = new String(Arrays.copyOfRange(message, 4, 4 + deviceIdLength));
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
            } else {
                //读取设备参数
                readHost();
                readPort();
                readMqttUsername();
                readMqttPassword();
                readMqttClientId();
                readMqttCleanSession();
                readMqttKeepAlive();
                readMqttQos();
                readMqttSubscribe();
                readMqttPublish();
                readMqttLwtEnable();
                readMqttLwtQos();
                readMqttLwtRetainEnable();
                readMqttLwtTopic();
                readMqttLwtMsg();
                readMqttEntryType();
                readApn();
                readApnUsername();
                readApnPassword();
                readNetworkPriority();
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
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }, 60 * 1000);
            setMQTTHost();
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_HOST) {
            if (flag == 1) {
                if (dataLength == 1 && data[0] == 1)
                    setMQTTPort();
                else {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }
            } else {
                if (dataLength > 0) {
                    mBind.etMqttHost.setText(new String(data));
                    mBind.etMqttHost.setSelection(mBind.etMqttHost.getText().length());
                }
            }
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_PORT) {
            if (flag == 1) {
                if (dataLength == 1 && data[0] == 1)
                    setMQTTUsername();
                else {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }
            } else {
                if (dataLength == 2) {
                    mBind.etMqttPort.setText(String.valueOf(MokoUtils.toInt(data)));
                    mBind.etMqttPort.setSelection(mBind.etMqttPort.getText().length());
                }
            }
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_USERNAME) {
            if (flag == 1) {
                if (dataLength == 1 && data[0] == 1)
                    setMQTTPassword();
                else {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }
            } else {
                if (dataLength > 0) {
                    userFragment.setUserName(new String(data));
                }
            }
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_PASSWORD) {
            if (flag == 1) {
                if (dataLength == 1 && data[0] == 1)
                    setMQTTClientId();
                else {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }
            } else {
                if (dataLength > 0) {
                    userFragment.setPassword(new String(data));
                }
            }
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_CLIENT_ID) {
            if (flag == 1) {
                if (dataLength == 1 && data[0] == 1)
                    setMQTTCleanSession();
                else {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }
            } else {
                if (dataLength > 0) {
                    mBind.etMqttClientId.setText(new String(data));
                    mBind.etMqttClientId.setSelection(mBind.etMqttClientId.getText().length());
                }
            }
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_CLEAN_SESSION) {
            if (flag == 1) {
                if (dataLength == 1 && data[0] == 1)
                    setMQTTKeepAlive();
                else {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }
            } else {
                if (dataLength == 1) {
                    generalFragment.setCleanSession(data[0] == 1);
                }
            }
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_KEEP_ALIVE) {
            if (flag == 1) {
                if (dataLength == 1 && data[0] == 1)
                    setMQTTQos();
                else {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }
            } else {
                if (dataLength == 1) {
                    generalFragment.setKeepAlive(data[0] & 0xff);
                }
            }
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_QOS) {
            if (flag == 1) {
                if (dataLength == 1 && data[0] == 1)
                    setMQTTSubscribeTopic();
                else {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }
            } else {
                if (dataLength == 1) {
                    generalFragment.setQos(data[0]);
                }
            }
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_SUBSCRIBE_TOPIC) {
            if (flag == 1) {
                if (dataLength == 1 && data[0] == 1)
                    setMQTTPublishTopic();
                else {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }
            } else {
                if (dataLength > 0) {
                    mBind.etMqttSubscribeTopic.setText(new String(data));
                    mBind.etMqttSubscribeTopic.setSelection(mBind.etMqttSubscribeTopic.getText().length());
                }
            }
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_PUBLISH_TOPIC) {
            if (flag == 1) {
                if (dataLength == 1 && data[0] == 1)
                    setLWTEnable();
                else {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }
            } else {
                if (dataLength > 0) {
                    mBind.etMqttPublishTopic.setText(new String(data));
                    mBind.etMqttPublishTopic.setSelection(mBind.etMqttSubscribeTopic.getText().length());
                }
            }
        }
        if (cmd == MQTTConstants.MSG_ID_LWT_ENABLE) {
            if (flag == 1) {
                if (dataLength == 1 && data[0] == 1)
                    setLWTRetain();
                else {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }
            } else {
                if (dataLength == 1) {
                    lwtFragment.setLwtEnable(data[0] == 1);
                }
            }
        }
        if (cmd == MQTTConstants.MSG_ID_LWT_RETAIN) {
            if (flag == 1) {
                if (dataLength == 1 && data[0] == 1)
                    setLWTQos();
                else {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }
            } else {
                if (dataLength == 1) {
                    lwtFragment.setLwtRetain(data[0] == 1);
                }
            }
        }
        if (cmd == MQTTConstants.MSG_ID_LWT_QOS) {
            if (flag == 1) {
                if (dataLength == 1 && data[0] == 1)
                    setLWTTopic();
                else {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }
            } else {
                if (dataLength == 1) {
                    lwtFragment.setQos(data[0]);
                }
            }
        }
        if (cmd == MQTTConstants.MSG_ID_LWT_TOPIC) {
            if (flag == 1) {
                if (dataLength == 1 && data[0] == 1)
                    setLWTPayload();
                else {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }
            } else {
                if (dataLength > 0) {
                    lwtFragment.setTopic(new String(data));
                }
            }
        }
        if (cmd == MQTTConstants.MSG_ID_LWT_MESSAGE) {
            if (flag == 1) {
                if (dataLength == 1 && data[0] == 1)
                    setAPN();
                else {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }
            } else {
                if (dataLength > 0) {
                    lwtFragment.setPayload(new String(data));
                }
            }
        }
        if (cmd == MQTTConstants.MSG_ID_APN) {
            if (flag == 1) {
                if (dataLength == 1 && data[0] == 1)
                    setAPNUsername();
                else {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }
            } else {
                if (dataLength > 0) {
                    mBind.etApn.setText(new String(data));
                    mBind.etApn.setSelection(mBind.etApn.getText().length());
                }
            }
        }
        if (cmd == MQTTConstants.MSG_ID_APN_USERNAME) {
            if (flag == 1) {
                if (dataLength == 1 && data[0] == 1)
                    setAPNPassword();
                else {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }
            } else {
                if (dataLength > 0) {
                    mBind.etApnUsername.setText(new String(data));
                    mBind.etApnUsername.setSelection(mBind.etApnUsername.getText().length());
                }
            }
        }
        if (cmd == MQTTConstants.MSG_ID_APN_PASSWORD) {
            if (flag == 1) {
                if (dataLength == 1 && data[0] == 1)
                    setNetworkPriority();
                else {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }
            } else {
                if (dataLength > 0) {
                    mBind.etApnPassword.setText(new String(data));
                    mBind.etApnPassword.setSelection(mBind.etApnPassword.getText().length());
                }
            }
        }
        if (cmd == MQTTConstants.MSG_ID_NETWORK_PRIORITY) {
            if (flag == 1) {
                if (dataLength == 1 && data[0] == 1)
                    setMQTTEncryptionType();
                else {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    ToastUtils.showToast(this, "Setup failed, please try it again!");
                }
            } else {
                if (dataLength == 1) {
                    mSelectedNetworkPriority = data[0];
                    mBind.tvNetworkPriority.setText(mNetworkPriority.get(mSelectedNetworkPriority));
                }
            }
        }
        if (cmd == MQTTConstants.MSG_ID_MQTT_SSL) {
            if (flag == 1) {
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
            } else {
                if (dataLength == 1) {
                    sslFragment.setConnectMode(data[0]);
                }
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
            if (dataLength != 1) return;
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
            if (dataLength != 1) return;
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
            intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_MAC, mMokoDevice.mac);
            startActivity(intent);
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

    public void onSave(View view) {
        if (isWindowLocked()) return;
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
        if (isWindowLocked()) return;
        sslFragment.selectCertificate();
    }

    private void saveParams() {
        final String host = mBind.etMqttHost.getText().toString().trim();
        final String port = mBind.etMqttPort.getText().toString().trim();
        final String clientId = mBind.etMqttClientId.getText().toString().trim();
        String topicSubscribe = mBind.etMqttSubscribeTopic.getText().toString().trim();
        String topicPublish = mBind.etMqttPublishTopic.getText().toString().trim();
        String apn = mBind.etApn.getText().toString().trim();
        String apnUsername = mBind.etApnUsername.getText().toString().trim();
        String apnPassword = mBind.etApnPassword.getText().toString().trim();

        mMQTTSettings.host = host;
        mMQTTSettings.port = Integer.parseInt(port);
        mMQTTSettings.client_id = clientId;
        mMQTTSettings.subscribe_topic = topicSubscribe;
        mMQTTSettings.publish_topic = topicPublish;

        mMQTTSettings.clean_session = generalFragment.isCleanSession() ? 1 : 0;
        mMQTTSettings.qos = generalFragment.getQos();
        mMQTTSettings.keepalive = generalFragment.getKeepAlive();
        mMQTTSettings.username = userFragment.getUsername();
        mMQTTSettings.password = userFragment.getPassword();
        mMQTTSettings.encryption_type = sslFragment.getConnectMode();
        if (mMQTTSettings.encryption_type == 1) {
            mMQTTSettings.ca_cert_url = sslFragment.getCAPath();
        }
        if (mMQTTSettings.encryption_type == 2) {
            mMQTTSettings.ca_cert_url = sslFragment.getCAPath();
            mMQTTSettings.client_cert_url = sslFragment.getClientCerPath();
            mMQTTSettings.client_key_url = sslFragment.getClientKeyPath();
        }
        mLWTSettings.lwt_enable = lwtFragment.getLwtEnable() ? 1 : 0;
        mLWTSettings.lwt_retain = lwtFragment.getLwtRetain() ? 1 : 0;
        mLWTSettings.lwt_qos = lwtFragment.getQos();
        mLWTSettings.lwt_topic = lwtFragment.getTopic();
        mLWTSettings.lwt_message = lwtFragment.getPayload();
        mAPNSettings.apn = apn;
        mAPNSettings.apn_username = apnUsername;
        mAPNSettings.apn_password = apnPassword;

        byte[] message = MQTTMessageAssembler.assembleReadDeviceStatus(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTHost() {
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTHost(mMokoDevice.mac, mMQTTSettings.host);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTPort() {
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTPort(mMokoDevice.mac, mMQTTSettings.port);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTUsername() {
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTUsername(mMokoDevice.mac, mMQTTSettings.username);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTPassword() {
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTPassword(mMokoDevice.mac, mMQTTSettings.password);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTClientId() {
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTClientId(mMokoDevice.mac, mMQTTSettings.client_id);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTCleanSession() {
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTCleanSession(mMokoDevice.mac, mMQTTSettings.clean_session);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTKeepAlive() {
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTKeepAlive(mMokoDevice.mac, mMQTTSettings.keepalive);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTQos() {
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTQos(mMokoDevice.mac, mMQTTSettings.qos);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTSubscribeTopic() {
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTSubscribeTopic(mMokoDevice.mac, mMQTTSettings.subscribe_topic);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTPublishTopic() {
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTPublishTopic(mMokoDevice.mac, mMQTTSettings.publish_topic);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setLWTEnable() {
        byte[] message = MQTTMessageAssembler.assembleWriteLWTEnable(mMokoDevice.mac, mLWTSettings.lwt_enable);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setLWTQos() {
        byte[] message = MQTTMessageAssembler.assembleWriteLWTQos(mMokoDevice.mac, mLWTSettings.lwt_qos);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setLWTRetain() {
        byte[] message = MQTTMessageAssembler.assembleWriteLWTRetain(mMokoDevice.mac, mLWTSettings.lwt_retain);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setLWTTopic() {
        byte[] message = MQTTMessageAssembler.assembleWriteLWTTopic(mMokoDevice.mac, mLWTSettings.lwt_topic);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setLWTPayload() {
        byte[] message = MQTTMessageAssembler.assembleWriteLWTPayload(mMokoDevice.mac, mLWTSettings.lwt_message);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setAPN() {
        byte[] message = MQTTMessageAssembler.assembleWriteAPN(mMokoDevice.mac, mAPNSettings.apn);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setAPNUsername() {
        byte[] message = MQTTMessageAssembler.assembleWriteAPNUsername(mMokoDevice.mac, mAPNSettings.apn_username);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setAPNPassword() {
        byte[] message = MQTTMessageAssembler.assembleWriteAPNPassword(mMokoDevice.mac, mAPNSettings.apn_password);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setNetworkPriority() {
        byte[] message = MQTTMessageAssembler.assembleWriteNetworkPriority(mMokoDevice.mac, mSelectedNetworkPriority);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTEncryptionType() {
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTEncryptionType(mMokoDevice.mac, mMQTTSettings.encryption_type);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setCACertFile() {
        byte[] message = MQTTMessageAssembler.assembleWriteCACertFile(mMokoDevice.mac, mMQTTSettings.ca_cert_url);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setSelfSingleServerCertificates() {
        byte[] message = MQTTMessageAssembler.assembleWriteSelfSingleServerCertificates(mMokoDevice.mac, mMQTTSettings.ca_cert_url
                , mMQTTSettings.client_cert_url, mMQTTSettings.client_key_url);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setDeviceReconnect() {
        byte[] message = MQTTMessageAssembler.assembleWriteDeviceReconnect(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setConfigFinish() {
        byte[] message = MQTTMessageAssembler.assembleWriteMQTTConfigFinish(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readHost() {
        byte[] message = MQTTMessageAssembler.assembleReadMQTTHost(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readPort() {
        byte[] message = MQTTMessageAssembler.assembleReadMQTTPort(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readMqttUsername() {
        byte[] message = MQTTMessageAssembler.assembleReadMQTTUsername(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readMqttPassword() {
        byte[] message = MQTTMessageAssembler.assembleReadMQTTPassword(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readMqttClientId() {
        byte[] message = MQTTMessageAssembler.assembleReadMQTTClientId(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readMqttCleanSession() {
        byte[] message = MQTTMessageAssembler.assembleReadMQTTCleanSession(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readMqttKeepAlive() {
        byte[] message = MQTTMessageAssembler.assembleReadMQTTKeepAlive(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readMqttQos() {
        byte[] message = MQTTMessageAssembler.assembleReadMQTTQos(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readMqttSubscribe() {
        byte[] message = MQTTMessageAssembler.assembleReadMQTTSubscribeTopic(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readMqttPublish() {
        byte[] message = MQTTMessageAssembler.assembleReadMQTTPublishTopic(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readMqttLwtEnable() {
        byte[] message = MQTTMessageAssembler.assembleReadLWTEnable(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readMqttLwtQos() {
        byte[] message = MQTTMessageAssembler.assembleReadLWTQos(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readMqttLwtRetainEnable() {
        byte[] message = MQTTMessageAssembler.assembleReadLWTRetain(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readMqttLwtTopic() {
        byte[] message = MQTTMessageAssembler.assembleReadLWTTopic(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readMqttLwtMsg() {
        byte[] message = MQTTMessageAssembler.assembleReadLWTPayload(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readMqttEntryType() {
        byte[] message = MQTTMessageAssembler.assembleReadMQTTEncryptionType(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readApn() {
        byte[] message = MQTTMessageAssembler.assembleReadApn(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readApnUsername() {
        byte[] message = MQTTMessageAssembler.assembleReadApnUsername(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readApnPassword() {
        byte[] message = MQTTMessageAssembler.assembleReadApnPassword(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readNetworkPriority() {
        byte[] message = MQTTMessageAssembler.assembleReadNetworkPriority(mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(getAppTopTic(), message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private boolean isValid() {
        String host = mBind.etMqttHost.getText().toString().trim();
        String port = mBind.etMqttPort.getText().toString().trim();
        String clientId = mBind.etMqttClientId.getText().toString().trim();
        String topicSubscribe = mBind.etMqttSubscribeTopic.getText().toString().trim();
        String topicPublish = mBind.etMqttPublishTopic.getText().toString().trim();
        if (TextUtils.isEmpty(host)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_host));
            return false;
        }
        if (TextUtils.isEmpty(port)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_port_empty));
            return false;
        }
        if (Integer.parseInt(port) < 1 || Integer.parseInt(port) > 65535) {
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
        return generalFragment.isValid() && sslFragment.isValid() && lwtFragment.isValid();
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
        if (checkedId == R.id.rb_general)
            mBind.vpMqtt.setCurrentItem(0);
        else if (checkedId == R.id.rb_user)
            mBind.vpMqtt.setCurrentItem(1);
        else if (checkedId == R.id.rb_ssl)
            mBind.vpMqtt.setCurrentItem(2);
        else if (checkedId == R.id.rb_lwt)
            mBind.vpMqtt.setCurrentItem(3);
    }

    public void selectNetworkPriority(View view) {
        if (isWindowLocked()) return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mNetworkPriority, mSelectedNetworkPriority);
        dialog.setListener(value -> {
            mSelectedNetworkPriority = value;
            mBind.tvNetworkPriority.setText(mNetworkPriority.get(value));
        });
        dialog.show(getSupportFragmentManager());
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
