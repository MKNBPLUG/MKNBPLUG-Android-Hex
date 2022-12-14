package com.moko.mknbplughex.dialog;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.moko.mknbplughex.R;
import com.moko.mknbplughex.R2;
import com.moko.mknbplughex.utils.ToastUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PasswordDialog extends MokoBaseDialog {
    public static final String TAG = PasswordDialog.class.getSimpleName();

    @BindView(R2.id.et_password)
    EditText etPassword;
    @BindView(R2.id.tv_password_ensure)
    TextView tvPasswordEnsure;
    private final String FILTER_ASCII = "[ -~]*";

    private String password;

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_password;
    }

    @Override
    public void bindView(View v) {
        ButterKnife.bind(this, v);
        InputFilter filter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (!(source + "").matches(FILTER_ASCII)) {
                    return "";
                }

                return null;
            }
        };
        etPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8), filter});
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                tvPasswordEnsure.setEnabled(s.toString().length() == 8);
            }
        });
        if (!TextUtils.isEmpty(password)) {
            etPassword.setText(password);
            etPassword.setSelection(password.length());
        }
        etPassword.postDelayed(new Runnable() {
            @Override
            public void run() {
                //?????????????????????
                etPassword.setFocusable(true);
                etPassword.setFocusableInTouchMode(true);
                //??????????????????
                etPassword.requestFocus();
                //?????????????????????
                InputMethodManager inputManager = (InputMethodManager) etPassword
                        .getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.showSoftInput(etPassword, 0);
            }
        }, 200);
    }

    @OnClick(R2.id.tv_password_cancel)
    public void onCancel(View view) {
        dismiss();
        if (passwordClickListener != null) {
            passwordClickListener.onDismiss();
        }
    }

    @OnClick(R2.id.tv_password_ensure)
    public void onEnsure(View view) {
        dismiss();
        if (TextUtils.isEmpty(etPassword.getText().toString())) {
            ToastUtils.showToast(getContext(), getContext().getString(R.string.password_null));
            return;
        }
        if (passwordClickListener != null)
            passwordClickListener.onEnsureClicked(etPassword.getText().toString());
    }

    @Override
    public int getDialogStyle() {
        return R.style.CenterDialog;
    }

    @Override
    public int getGravity() {
        return Gravity.CENTER;
    }

    @Override
    public String getFragmentTag() {
        return TAG;
    }

    @Override
    public float getDimAmount() {
        return 0.7f;
    }

    @Override
    public boolean getCancelOutside() {
        return false;
    }

    @Override
    public boolean getCancellable() {
        return true;
    }

    private PasswordClickListener passwordClickListener;

    public void setOnPasswordClicked(PasswordClickListener passwordClickListener) {
        this.passwordClickListener = passwordClickListener;
    }

    public interface PasswordClickListener {

        void onEnsureClicked(String password);

        void onDismiss();
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
