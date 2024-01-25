package com.moko.mknbplughex.dialog;

import android.text.TextUtils;
import android.view.View;

import com.moko.mknbplughex.R;
import com.moko.mknbplughex.view.WheelView;

import java.util.ArrayList;

public class BottomDialog extends MokoBaseDialog {
    private WheelView wvBottom;
    private ArrayList<String> mDatas;
    private int mIndex;

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_bottom_hex;
    }

    @Override
    public void bindView(View v) {
        wvBottom = v.findViewById(R.id.wv_bottom);
        v.findViewById(R.id.tv_cancel).setOnClickListener(v1 -> dismiss());
        v.findViewById(R.id.tv_confirm).setOnClickListener(v1 -> {
            if (TextUtils.isEmpty(wvBottom.getSelectedText())) {
                return;
            }
            dismiss();
            final int selected = wvBottom.getSelected();
            if (listener != null) {
                listener.onValueSelected(selected);
            }
        });
        wvBottom.setData(mDatas);
        wvBottom.setDefault(mIndex);
    }

    @Override
    public float getDimAmount() {
        return 0.7f;
    }

    public void setDatas(ArrayList<String> datas, int index) {
        this.mDatas = datas;
        this.mIndex = index;
    }

    private OnBottomListener listener;

    public void setListener(OnBottomListener listener) {
        this.listener = listener;
    }

    public interface OnBottomListener {
        void onValueSelected(int value);
    }
}
