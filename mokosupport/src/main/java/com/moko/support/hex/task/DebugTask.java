package com.moko.support.hex.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.support.hex.entity.OrderCHAR;
import com.moko.support.hex.entity.ParamsKeyEnum;

public class DebugTask extends OrderTask {
    public byte[] data;

    public DebugTask() {
        super(OrderCHAR.CHAR_DEBUG_EXIT, OrderTask.RESPONSE_TYPE_WRITE);
    }

    @Override
    public byte[] assemble() {
        return data;
    }


    public void exitDebugMode() {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_DEBUG_EXIT.getParamsKey(),
                (byte) 0x01,
                (byte) 0x00
        };
    }
}
