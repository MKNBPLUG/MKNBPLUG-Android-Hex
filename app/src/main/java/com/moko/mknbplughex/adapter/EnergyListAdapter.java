package com.moko.mknbplughex.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mknbplughex.R;
import com.moko.support.hex.entity.EnergyInfo;

public class EnergyListAdapter extends BaseQuickAdapter<EnergyInfo, BaseViewHolder> {
    public EnergyListAdapter() {
        super(R.layout.item_energy_pro);
    }

    @Override
    protected void convert(BaseViewHolder helper, EnergyInfo item) {
        helper.setText(R.id.tv_time, item.time);
        helper.setText(R.id.tv_value, item.value);
    }
}
