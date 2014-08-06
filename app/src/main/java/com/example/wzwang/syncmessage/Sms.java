package com.example.wzwang.syncmessage;

import java.util.UUID;

/**
 * Created by wzwang on 14-8-6.
 */
public class Sms {
    String smsId;
    String fromNumber;
    String toNumber;
    String text;
    String time;

    public Sms() {
        this.smsId = generateGuid();
    }

    public Sms(String smsId) {
        this.smsId = smsId;
    }

    private String generateGuid() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

}
