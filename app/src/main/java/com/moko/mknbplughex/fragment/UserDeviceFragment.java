package com.moko.mknbplughex.fragment;

import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.moko.mknbplughex.R;
import com.moko.mknbplughex.R2;
import com.moko.mknbplughex.base.BaseActivity;

import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;

public class UserDeviceFragment extends Fragment {
    private final String FILTER_ASCII = "[ -~]*";
    private static final String TAG = UserDeviceFragment.class.getSimpleName();
    @BindView(R2.id.et_mqtt_username)
    EditText etMqttUsername;
    @BindView(R2.id.et_mqtt_password)
    EditText etMqttPassword;


    private BaseActivity activity;
    private String username;
    private String password;

    public UserDeviceFragment() {
    }

    public static UserDeviceFragment newInstance() {
        UserDeviceFragment fragment = new UserDeviceFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_user_device, container, false);
        ButterKnife.bind(this, view);
        activity = (BaseActivity) getActivity();
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }

            return null;
        };
        etMqttUsername.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        etMqttPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        etMqttUsername.setText(username);
        etMqttPassword.setText(password);
        return view;
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume: ");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause: ");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        super.onDestroy();
    }

    public void setUserName(String username) {
        this.username = username;
        if (etMqttUsername == null)
            return;
        etMqttUsername.setText(username);
    }

    public void setPassword(String password) {
        this.password = password;
        if (etMqttPassword == null)
            return;
        etMqttPassword.setText(password);
    }

    public String getUsername() {
        String username = etMqttUsername.getText().toString();
        return username;
    }

    public String getPassword() {
        String password = etMqttPassword.getText().toString();
        return password;
    }
}
