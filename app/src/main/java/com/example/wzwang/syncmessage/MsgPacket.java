package com.example.wzwang.syncmessage;

/**
 * Created by wzwang on 14-8-6.
 */
public class MsgPacket {
    int req;
    int ret;
    String IMEI;
    String IMSI;
    String username;
    Sms sms;

    public MsgPacket() {
        this.req = 0;
    }

    public MsgPacket(int req) {
        this.req = req;
    }

    public static MsgPacket generateHeartBeatMessage() {
        MsgPacket msg = new MsgPacket(700);
        return msg;
    }

    public static MsgPacket generateReportingSmsMessage(Sms sms) {
        MsgPacket msg = new MsgPacket(300);
        msg.sms = sms;
        return msg;
    }

    public static MsgPacket generateLoginMessage() {
        MsgPacket msg = new MsgPacket(100);
        return msg;
    }

    public static MsgPacket generateUserRegMessage(String username) {
        MsgPacket msg = new MsgPacket(101);
        msg.username = username;
        return msg;
    }

}
