package com.moko.support.hex.entity;

public class MsgCommon<T> {
    public int msg_id;
    public DeviceParams device_info;
    public T data;
    public int result_code;
    public String result_msg;
}
