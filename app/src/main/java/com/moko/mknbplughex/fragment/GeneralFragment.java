package com.moko.mknbplughex.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.moko.mknbplughex.R;
import com.moko.mknbplughex.R2;
import com.moko.mknbplughex.base.BaseActivity;
import com.moko.mknbplughex.utils.ToastUtils;

import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;

public class GeneralFragment extends Fragment {

    private static final String TAG = GeneralFragment.class.getSimpleName();
    @BindView(R2.id.cb_clean_session)
    CheckBox cbCleanSession;
    @BindView(R2.id.rb_qos_1)
    RadioButton rbQos1;
    @BindView(R2.id.rb_qos_2)
    RadioButton rbQos2;
    @BindView(R2.id.rb_qos_3)
    RadioButton rbQos3;
    @BindView(R2.id.rg_qos)
    RadioGroup rgQos;
    @BindView(R2.id.et_keep_alive)
    EditText etKeepAlive;

    private BaseActivity activity;

    private boolean cleanSession;
    private int qos;
    private int keepAlive;

    public GeneralFragment() {
    }

    public static GeneralFragment newInstance() {
        GeneralFragment fragment = new GeneralFragment();
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
        View view = inflater.inflate(R.layout.fragment_general_app, container, false);
        ButterKnife.bind(this, view);
        activity = (BaseActivity) getActivity();
        cbCleanSession.setChecked(cleanSession);
        if (qos == 0) {
            rbQos1.setChecked(true);
        } else if (qos == 1) {
            rbQos2.setChecked(true);
        } else if (qos == 2) {
            rbQos3.setChecked(true);
        }
        etKeepAlive.setText(String.valueOf(keepAlive));
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

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
        if (cbCleanSession == null)
            return;
        cbCleanSession.setChecked(cleanSession);
    }

    public void setQos(int qos) {
        this.qos = qos;
        if (rgQos == null)
            return;
        if (qos == 0) {
            rbQos1.setChecked(true);
        } else if (qos == 1) {
            rbQos2.setChecked(true);
        } else if (qos == 2) {
            rbQos3.setChecked(true);
        }
    }

    public void setKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
        if (etKeepAlive == null)
            return;
        etKeepAlive.setText(String.valueOf(keepAlive));
    }

    public boolean isValid() {
        final String keepAliveStr = etKeepAlive.getText().toString();
        if (TextUtils.isEmpty(keepAliveStr)) {
            ToastUtils.showToast(getActivity(), "Error");
            return false;
        }
        final int keepAlive = Integer.parseInt(keepAliveStr);
        if (keepAlive < 10 || keepAlive > 120) {
            ToastUtils.showToast(getActivity(), "Keep Alive range is 10-120");
            return false;
        }
        return true;
    }

    public boolean isCleanSession() {
        return cbCleanSession.isChecked();
    }

    public int getQos() {
        int qos = 0;
        if (rbQos2.isChecked()) {
            qos = 1;
        } else if (rbQos3.isChecked()) {
            qos = 2;
        }
        return qos;
    }

    public int getKeepAlive() {
        String keepAliveStr = etKeepAlive.getText().toString();
        int keepAlive = Integer.parseInt(keepAliveStr);
        return keepAlive;
    }
}
