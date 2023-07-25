package com.moko.mknbplughex.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplughex.AppConstants;
import com.moko.mknbplughex.base.BaseActivity;
import com.moko.mknbplughex.databinding.ActivityAddDeviceSuccessBinding;
import com.moko.mknbplughex.db.DBTools;
import com.moko.mknbplughex.entity.MokoDevice;
import com.moko.mknbplughex.utils.SPUtils;
import com.moko.mknbplughex.utils.ToastUtils;
import com.moko.support.hex.MQTTConstants;
import com.moko.support.hex.MQTTMessageAssembler;
import com.moko.support.hex.MQTTSupport;
import com.moko.support.hex.entity.MQTTConfig;
import com.moko.support.hex.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;

/**
 * @author: jun.liu
 * @date: 2023/7/5 20:37
 * @des: add device success
 */
public class AddDeviceSuccessActivity extends BaseActivity<ActivityAddDeviceSuccessBinding> {
    private String subscribe;
    private String publish;
    private String mac;
    private Handler mHandler;
    public static final String TAG = AddDeviceSuccessActivity.class.getSimpleName();

    @Override
    protected void onCreate() {
        mac = getIntent().getStringExtra("mac");
        subscribe = getIntent().getStringExtra("subscribe");
        publish = getIntent().getStringExtra("publish");
        mBind.etDeviceName.setText("MK117NB-" + mac.substring(mac.length() - 4).toUpperCase());
        mBind.etDeviceName.setSelection(mBind.etDeviceName.getText().length());
        mHandler = new Handler(Looper.getMainLooper());
        mBind.btnDone.setOnClickListener(v -> {
            if (TextUtils.isEmpty(mBind.etDeviceName.getText())) {
                ToastUtils.showToast(this, "device name can not be null");
                return;
            }
            //查询设备信息
            showLoadingProgressDialog();
            byte[] message = MQTTMessageAssembler.assembleReadDeviceType(mac);
            String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
            MQTTConfig appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
            try {
                MQTTSupport.getInstance().publish(subscribe, message, appMqttConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "add fail!");
            }, 90 * 1000);
        });
    }

    @Override
    protected ActivityAddDeviceSuccessBinding getViewBinding() {
        return ActivityAddDeviceSuccessBinding.inflate(getLayoutInflater());
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
        if (!mac.equalsIgnoreCase(deviceId)) return;
        if (cmd == MQTTConstants.MSG_ID_READ_DEVICE_TYPE) {
            if (mHandler.hasMessages(0)) {
                mHandler.removeMessages(0);
            }
            if (dataLength != 1) return;
            insertDeviceToLocal(data[0]);
        }
    }

    @Override
    public void onBackPressed() {
    }

    private void insertDeviceToLocal(int deviceType) {
        //保存设备信息
        String MQTTConfigStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        MQTTConfig mqttDeviceConfig;
        if (TextUtils.isEmpty(MQTTConfigStr)) {
            mqttDeviceConfig = new MQTTConfig();
        } else {
            Gson gson = new Gson();
            mqttDeviceConfig = gson.fromJson(MQTTConfigStr, MQTTConfig.class);
            mqttDeviceConfig.connectMode = 0;
            mqttDeviceConfig.cleanSession = true;
            mqttDeviceConfig.qos = 1;
            mqttDeviceConfig.keepAlive = 60;
            mqttDeviceConfig.clientId = "";
            mqttDeviceConfig.username = "";
            mqttDeviceConfig.password = "";
            mqttDeviceConfig.caPath = "";
            mqttDeviceConfig.clientKeyPath = "";
            mqttDeviceConfig.clientCertPath = "";
            mqttDeviceConfig.lwtTopic = "{device_name}/{device_id}/device_to_app";
            mqttDeviceConfig.lwtPayload = "Offline";
            mqttDeviceConfig.apn = "";
            mqttDeviceConfig.apnUsername = "";
            mqttDeviceConfig.apnPassword = "";
            mqttDeviceConfig.topicPublish = publish;
            mqttDeviceConfig.topicSubscribe = subscribe;
            mqttDeviceConfig.timeZone = 0;
        }

        String mqttConfigStr = new Gson().toJson(mqttDeviceConfig, MQTTConfig.class);
        MokoDevice mokoDevice = new MokoDevice();
        mokoDevice.name = mBind.etDeviceName.getText().toString();
        mokoDevice.mac = mac.toLowerCase();
        mokoDevice.mqttInfo = mqttConfigStr;
        mokoDevice.topicSubscribe = subscribe;
        mokoDevice.topicPublish = publish;
        mokoDevice.deviceType = deviceType;
        DBTools.getInstance(getApplicationContext()).insertDevice(mokoDevice);
        dismissLoadingProgressDialog();
        // 跳转首页，刷新数据
        Intent intent = new Intent(this, HEXMainActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY, TAG);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_MAC, mokoDevice.mac);
        startActivity(intent);
    }
}
