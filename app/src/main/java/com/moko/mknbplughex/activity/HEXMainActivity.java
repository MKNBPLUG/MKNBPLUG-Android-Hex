package com.moko.mknbplughex.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplughex.AppConstants;
import com.moko.mknbplughex.BuildConfig;
import com.moko.mknbplughex.R;
import com.moko.mknbplughex.R2;
import com.moko.mknbplughex.adapter.DeviceAdapter;
import com.moko.mknbplughex.base.BaseActivity;
import com.moko.mknbplughex.db.DBTools;
import com.moko.mknbplughex.dialog.AlertMessageDialog;
import com.moko.mknbplughex.entity.MQTTConfig;
import com.moko.mknbplughex.entity.MokoDevice;
import com.moko.mknbplughex.utils.SPUtiles;
import com.moko.mknbplughex.utils.ToastUtils;
import com.moko.mknbplughex.utils.Utils;
import com.moko.support.hex.MQTTConstants;
import com.moko.support.hex.MQTTMessageAssembler;
import com.moko.support.hex.MQTTSupport;
import com.moko.support.hex.MokoSupport;
import com.moko.support.hex.event.DeviceDeletedEvent;
import com.moko.support.hex.event.DeviceModifyNameEvent;
import com.moko.support.hex.event.DeviceOnlineEvent;
import com.moko.support.hex.event.DeviceUpdateEvent;
import com.moko.support.hex.event.MQTTConnectionCompleteEvent;
import com.moko.support.hex.event.MQTTConnectionFailureEvent;
import com.moko.support.hex.event.MQTTConnectionLostEvent;
import com.moko.support.hex.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class HEXMainActivity extends BaseActivity implements BaseQuickAdapter.OnItemChildClickListener,
        BaseQuickAdapter.OnItemClickListener,
        BaseQuickAdapter.OnItemLongClickListener {

    @BindView(R2.id.rl_empty)
    RelativeLayout rlEmpty;
    @BindView(R2.id.rv_device_list)
    RecyclerView rvDeviceList;
    @BindView(R2.id.tv_title)
    TextView tvTitle;
    private ArrayList<MokoDevice> devices;
    private DeviceAdapter adapter;
    public Handler mHandler;
    public String MQTTAppConfigStr;
    private MQTTConfig appMqttConfig;

    public static String PATH_LOGCAT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_hex);
        ButterKnife.bind(this);

        // 初始化Xlog
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // 优先保存到SD卡中
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                PATH_LOGCAT = getExternalFilesDir(null).getAbsolutePath() + File.separator + (BuildConfig.IS_LIBRARY ? "MKNBPLUG" : "MKNBPLUGHEX");
            } else {
                PATH_LOGCAT = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + (BuildConfig.IS_LIBRARY ? "MKNBPLUG" : "MKNBPLUGHEX");
            }
        } else {
            // 如果SD卡不存在，就保存到本应用的目录下
            PATH_LOGCAT = getFilesDir().getAbsolutePath() + File.separator + (BuildConfig.IS_LIBRARY ? "MKNBPLUG" : "MKNBPLUGHEX");
        }
        MokoSupport.getInstance().init(getApplicationContext());
        MQTTSupport.getInstance().init(getApplicationContext());

        devices = DBTools.getInstance(this).selectAllDevice();
        adapter = new DeviceAdapter();
        adapter.openLoadAnimation();
        adapter.replaceData(devices);
        adapter.setOnItemClickListener(this);
        adapter.setOnItemLongClickListener(this);
        adapter.setOnItemChildClickListener(this);
        rvDeviceList.setLayoutManager(new LinearLayoutManager(this));
        rvDeviceList.setAdapter(adapter);
        if (devices.isEmpty()) {
            rlEmpty.setVisibility(View.VISIBLE);
            rvDeviceList.setVisibility(View.GONE);
        } else {
            rvDeviceList.setVisibility(View.VISIBLE);
            rlEmpty.setVisibility(View.GONE);
        }
        mHandler = new Handler(Looper.getMainLooper());
        MQTTAppConfigStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        if (!TextUtils.isEmpty(MQTTAppConfigStr)) {
            appMqttConfig = new Gson().fromJson(MQTTAppConfigStr, MQTTConfig.class);
            tvTitle.setText(getString(R.string.mqtt_connecting));
        }
        StringBuffer buffer = new StringBuffer();
        // 记录机型
        buffer.append("机型：");
        buffer.append(android.os.Build.MODEL);
        buffer.append("=====");
        // 记录版本号
        buffer.append("手机系统版本：");
        buffer.append(android.os.Build.VERSION.RELEASE);
        buffer.append("=====");
        // 记录APP版本
        buffer.append("APP版本：");
        buffer.append(Utils.getVersionInfo(this));
        XLog.d(buffer.toString());
        try {
            MQTTSupport.getInstance().connectMqtt(MQTTAppConfigStr);
        } catch (FileNotFoundException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ToastUtils.showToast(this, "Please select your SSL certificates again, otherwise the APP can't use normally.");
                startActivityForResult(new Intent(this, SetAppMQTTActivity.class), AppConstants.REQUEST_CODE_MQTT_CONFIG_APP);
            }
            // 读取stacktrace信息
            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            e.printStackTrace(printWriter);
            StringBuffer errorReport = new StringBuffer();
            errorReport.append(result.toString());
            XLog.e(errorReport.toString());
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // connect event
    ///////////////////////////////////////////////////////////////////////////
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionCompleteEvent(MQTTConnectionCompleteEvent event) {
        tvTitle.setText(getString(R.string.app_name));
        // 订阅所有设备的Topic
        subscribeAllDevices();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionLostEvent(MQTTConnectionLostEvent event) {
        tvTitle.setText(getString(R.string.mqtt_connecting));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionFailureEvent(MQTTConnectionFailureEvent event) {
        tvTitle.setText(getString(R.string.mqtt_connect_failed));
    }

    ///////////////////////////////////////////////////////////////////////////
    // topic message event
    ///////////////////////////////////////////////////////////////////////////
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        // 更新所有设备的网络状态
        updateDeviceNetworkStatus(event);
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onMQTTUnSubscribeSuccessEvent(MQTTUnSubscribeSuccessEvent event) {
//        dismissLoadingProgressDialog();
//    }
//
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onMQTTUnSubscribeFailureEvent(MQTTUnSubscribeFailureEvent event) {
//        dismissLoadingProgressDialog();
//    }

    ///////////////////////////////////////////////////////////////////////////
    // device event
    ///////////////////////////////////////////////////////////////////////////

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceModifyNameEvent(DeviceModifyNameEvent event) {
        // 修改了设备名称
        if (!devices.isEmpty()) {
            for (MokoDevice device : devices) {
                if (device.deviceId.equals(event.getDeviceId())) {
                    device.name = event.getName();
                    break;
                }
            }
        }
        adapter.replaceData(devices);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceDeletedEvent(DeviceDeletedEvent event) {
        // 删除了设备
        int id = event.getId();
        Iterator<MokoDevice> iterator = devices.iterator();
        while (iterator.hasNext()) {
            MokoDevice device = iterator.next();
            if (id == device.id) {
                iterator.remove();
                break;
            }
        }
        adapter.replaceData(devices);
        if (devices.isEmpty()) {
            rlEmpty.setVisibility(View.VISIBLE);
            rvDeviceList.setVisibility(View.GONE);
        }
        if (id > 0 && mHandler.hasMessages(id)) {
            mHandler.removeMessages(id);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceUpdateEvent(DeviceUpdateEvent event) {
        String deviceId = event.getDeviceId();
        if (TextUtils.isEmpty(deviceId))
            return;
        MokoDevice mokoDevice = DBTools.getInstance(this).selectDevice(deviceId);
        if (devices.isEmpty()) {
            devices.add(mokoDevice);
        } else {
            Iterator<MokoDevice> iterator = devices.iterator();
            while (iterator.hasNext()) {
                MokoDevice device = iterator.next();
                if (deviceId.equals(device.deviceId)) {
                    iterator.remove();
                    break;
                }
            }
            devices.add(mokoDevice);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        XLog.i("onNewIntent...");
        setIntent(intent);
        if (getIntent().getExtras() != null) {
            String from = getIntent().getStringExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY);
            String deviceId = getIntent().getStringExtra(AppConstants.EXTRA_KEY_DEVICE_ID);
            if (ModifyNameActivity.TAG.equals(from)
                    || PlugSettingActivity.TAG.equals(from)) {
                devices.clear();
                devices.addAll(DBTools.getInstance(this).selectAllDevice());
                if (!TextUtils.isEmpty(deviceId)) {
                    MokoDevice mokoDevice = DBTools.getInstance(this).selectDevice(deviceId);
                    if (mokoDevice == null)
                        return;
                    for (final MokoDevice device : devices) {
                        if (deviceId.equals(device.deviceId)) {
                            device.isOnline = true;
                            if (mHandler.hasMessages(device.id)) {
                                mHandler.removeMessages(device.id);
                            }
                            Message message = Message.obtain(mHandler, () -> {
                                device.isOnline = false;
                                XLog.i(device.deviceId + "离线");
                                adapter.replaceData(devices);
                            });
                            message.what = device.id;
                            mHandler.sendMessageDelayed(message, 90 * 1000);
                            break;
                        }
                    }
                }
                adapter.replaceData(devices);
                if (!devices.isEmpty()) {
                    rvDeviceList.setVisibility(View.VISIBLE);
                    rlEmpty.setVisibility(View.GONE);
                } else {
                    rvDeviceList.setVisibility(View.GONE);
                    rlEmpty.setVisibility(View.VISIBLE);
                }
            }
            if (ModifyMQTTSettingsActivity.TAG.equals(from)) {
                if (!TextUtils.isEmpty(deviceId)) {
                    MokoDevice mokoDevice = DBTools.getInstance(this).selectDevice(deviceId);
                    for (final MokoDevice device : devices) {
                        if (deviceId.equals(device.deviceId)) {
                            if (!device.topicPublish.equals(mokoDevice.topicPublish)) {
                                // 取消订阅
                                try {
                                    MQTTSupport.getInstance().unSubscribe(device.topicPublish);
                                } catch (MqttException e) {
                                    e.printStackTrace();
                                }
                            }
                            device.mqttInfo = mokoDevice.mqttInfo;
                            device.topicPublish = mokoDevice.topicPublish;
                            device.topicSubscribe = mokoDevice.topicSubscribe;
                            break;
                        }
                    }
                }
                adapter.replaceData(devices);
            }
        }
    }

    public void setAppMQTTConfig(View view) {
        if (isWindowLocked())
            return;
        startActivityForResult(new Intent(this, SetAppMQTTActivity.class), AppConstants.REQUEST_CODE_MQTT_CONFIG_APP);
    }

    public void mainAddDevices(View view) {
        if (isWindowLocked())
            return;
        if (TextUtils.isEmpty(MQTTAppConfigStr)) {
            startActivityForResult(new Intent(this, SetAppMQTTActivity.class), AppConstants.REQUEST_CODE_MQTT_CONFIG_APP);
            return;
        }
        if (Utils.isNetworkAvailable(this)) {
            MQTTConfig MQTTAppConfig = new Gson().fromJson(MQTTAppConfigStr, MQTTConfig.class);
            if (TextUtils.isEmpty(MQTTAppConfig.host)) {
                startActivityForResult(new Intent(this, SetAppMQTTActivity.class), AppConstants.REQUEST_CODE_MQTT_CONFIG_APP);
                return;
            }
            startActivity(new Intent(this, DeviceScannerActivity.class));
        } else {
            String ssid = Utils.getWifiSSID(this);
            ToastUtils.showToast(this, String.format("SSID:%s, the network cannot available,please check", ssid));
            XLog.i(String.format("SSID:%s, the network cannot available,please check", ssid));
        }
    }

    @Override
    public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
        MokoDevice device = (MokoDevice) adapter.getItem(position);
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (device.isOverload) {
            ToastUtils.showToast(this, "Socket is overload, please check it!");
            return;
        }
        if (device.isOverCurrent) {
            ToastUtils.showToast(this, "Socket is overcurrent, please check it!");
            return;
        }
        if (device.isOverVoltage) {
            ToastUtils.showToast(this, "Socket is overvoltage, please check it!");
            return;
        }
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        changeSwitch(device);
    }

    private void changeSwitch(MokoDevice device) {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = device.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        device.on_off = !device.on_off;
        byte[] message = MQTTMessageAssembler.assembleWriteSwitchInfo(device.deviceId, device.on_off ? 1 : 0);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        MokoDevice mokoDevice = (MokoDevice) adapter.getItem(position);
        if (mokoDevice == null)
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(HEXMainActivity.this, PlugActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mokoDevice);
        startActivity(i);
    }

    @Override
    public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
        MokoDevice mokoDevice = (MokoDevice) adapter.getItem(position);
        if (mokoDevice == null)
            return true;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Remove Device");
        dialog.setMessage("Please confirm again whether to \n remove the device,the device \n will be deleted from the device list.");
        dialog.setOnAlertConfirmListener(() -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(HEXMainActivity.this, R.string.network_error);
                return;
            }
//            showLoadingProgressDialog();
            // 取消订阅
            try {
                MQTTSupport.getInstance().unSubscribe(mokoDevice.topicPublish);
            } catch (MqttException e) {
                e.printStackTrace();
            }
            XLog.i(String.format("删除设备:%s", mokoDevice.name));
            DBTools.getInstance(HEXMainActivity.this).deleteDevice(mokoDevice);
            EventBus.getDefault().post(new DeviceDeletedEvent(mokoDevice.id));
        });
        dialog.show(getSupportFragmentManager());
        return true;
    }

    private void subscribeAllDevices() {
        if (!TextUtils.isEmpty(appMqttConfig.topicSubscribe)) {
            try {
                MQTTSupport.getInstance().subscribe(appMqttConfig.topicSubscribe, appMqttConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            if (devices.isEmpty()) {
                return;
            }
            for (MokoDevice device : devices) {
                try {
                    MQTTSupport.getInstance().subscribe(device.topicPublish, appMqttConfig.qos);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateDeviceNetworkStatus(MQTTMessageArrivedEvent event) {
        if (devices.isEmpty()) {
            return;
        }
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
        for (final MokoDevice device : devices) {
            if (device.deviceId.equals(deviceId)) {
                device.isOnline = true;
                if (mHandler.hasMessages(device.id)) {
                    mHandler.removeMessages(device.id);
                }
                Message offline = Message.obtain(mHandler, () -> {
                    device.isOnline = false;
                    device.on_off = false;
                    XLog.i(device.deviceId + "离线");
                    adapter.replaceData(devices);
                    EventBus.getDefault().post(new DeviceOnlineEvent(device.deviceId, false));
                });
                offline.what = device.id;
                mHandler.sendMessageDelayed(offline, 90 * 1000);
                if (cmd == MQTTConstants.NOTIFY_MSG_ID_SWITCH_STATE && flag == 2) {
                    if (dataLength != 11)
                        return;
                    // 启动设备定时离线，90s收不到应答则认为离线
                    device.on_off = data[5] == 1;
                    device.isOverload = data[7] == 1;
                    device.isOverCurrent = data[8] == 1;
                    device.isOverVoltage = data[9] == 1;
                    device.isUnderVoltage = data[10] == 1;
                    break;
                }
                if (cmd == MQTTConstants.READ_MSG_ID_SWITCH_INFO) {
                    if (dataLength != 6)
                        return;
                    // 启动设备定时离线，90s收不到应答则认为离线
                    device.on_off = data[0] == 1;
                    device.isOverload = data[2] == 1;
                    device.isOverCurrent = data[3] == 1;
                    device.isOverVoltage = data[4] == 1;
                    device.isUnderVoltage = data[5] == 1;
                    break;
                }
//                if (cmd == MQTTConstants.NOTIFY_MSG_ID_OVERLOAD_OCCUR) {
//                    Type infoType = new TypeToken<OverloadInfo>() {
//                    }.getType();
//                    OverloadInfo overLoadInfo = new Gson().fromJson(msgCommon.data, infoType);
//                    device.isOverload = overLoadInfo.overload_state == 1;
//                    device.overloadValue = overLoadInfo.overload_value;
//                }
                if (cmd == MQTTConstants.NOTIFY_MSG_ID_OVERLOAD_OCCUR && flag == 2) {
                    if (dataLength != 6)
                        return;
                    device.isOverload = data[5] == 1;
                    break;
                }
                if (cmd == MQTTConstants.NOTIFY_MSG_ID_OVER_VOLTAGE_OCCUR && flag == 2) {
                    if (dataLength != 6)
                        return;
                    device.isOverVoltage = data[5] == 1;
                    break;
                }
                if (cmd == MQTTConstants.NOTIFY_MSG_ID_OVER_CURRENT_OCCUR && flag == 2) {
                    if (dataLength != 6)
                        return;
                    device.isOverCurrent = data[5] == 1;
                    break;
                }
                if (cmd == MQTTConstants.NOTIFY_MSG_ID_UNDER_VOLTAGE_OCCUR && flag == 2) {
                    if (dataLength != 6)
                        return;
                    device.isUnderVoltage = data[5] == 1;
                    break;
                }
                if (cmd == MQTTConstants.CONFIG_MSG_ID_SWITCH_STATE && flag == 1) {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    if (dataLength != 1)
                        return;
                    if (data[0] == 0) {
                        ToastUtils.showToast(this, "Set up failed");
                    }
                    break;
                }
                break;
            }
        }
        adapter.replaceData(devices);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.REQUEST_CODE_MQTT_CONFIG_APP && resultCode == RESULT_OK) {
            MQTTAppConfigStr = data.getStringExtra(AppConstants.EXTRA_KEY_MQTT_CONFIG_APP);
            appMqttConfig = new Gson().fromJson(MQTTAppConfigStr, MQTTConfig.class);
            tvTitle.setText(getString(R.string.app_name));
            // 订阅所有设备的Topic
            subscribeAllDevices();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MQTTSupport.getInstance().disconnectMqtt();
        if (!devices.isEmpty()) {
            for (final MokoDevice device : devices) {
                if (mHandler.hasMessages(device.id)) {
                    mHandler.removeMessages(device.id);
                }
            }
        }
    }

    // 记录上次收到信息的时间,屏蔽无效事件
//    protected long mLastMessageTime = 0;
//
//    public boolean isDurationVoid() {
//        long current = SystemClock.elapsedRealtime();
//        if (current - mLastMessageTime > 500) {
//            mLastMessageTime = current;
//            return false;
//        } else {
//            return true;
//        }
//    }

    public void onBack(View view) {
        back();
    }

    private void back() {
        if (BuildConfig.IS_LIBRARY) {
            finish();
        } else {
            AlertMessageDialog dialog = new AlertMessageDialog();
            dialog.setMessage(R.string.main_exit_tips);
            dialog.setOnAlertConfirmListener(() -> HEXMainActivity.this.finish());
            dialog.show(getSupportFragmentManager());
        }
    }

    @Override
    public void onBackPressed() {
        back();
    }
}
