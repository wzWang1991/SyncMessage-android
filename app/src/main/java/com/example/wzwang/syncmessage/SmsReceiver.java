package com.example.wzwang.syncmessage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wzwang on 14-7-25.
 */
public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Map<String, MsgTextTime> msg = RetrieveMessages(intent);

        if (msg == null) {
            // unable to retrieve SMS
        } else {
            // send all SMS via XMPP by sender
            for (String sender : msg.keySet()) {
                Message newMessage = new Message();
                newMessage.fromNumber = sender;
                newMessage.content = msg.get(sender).getText();
                newMessage.time = msg.get(sender).getTime();
                Gson gson = new Gson();
                Log.d("SMSreceiver.java", gson.toJson(newMessage));
                WebSocket.INSTANCE.pushMsg(newMessage);
            }
        }
    }

    private Map<String, MsgTextTime> RetrieveMessages(Intent intent) {
        Map<String, MsgTextTime> msg = null;
        SmsMessage[] msgs;
        Bundle bundle = intent.getExtras();

        if (bundle != null && bundle.containsKey("pdus")) {
            Object[] pdus = (Object[]) bundle.get("pdus");

            if (pdus != null) {
                int numberOfPdus = pdus.length;
                msg = new HashMap<String, MsgTextTime>(numberOfPdus);
                msgs = new SmsMessage[numberOfPdus];

                // There can be multiple SMS from multiple senders, there can be a maximum of nbrOfpdus different senders
                // However, send long SMS of same sender in one message
                for (int i = 0; i < numberOfPdus; i++) {
                    msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);

                    String originatinAddress = msgs[i].getOriginatingAddress();

                    // Check if index with number exists
                    if (!msg.containsKey(originatinAddress)) {
                        // Index with number doesn't exist
                        // Save string into associative array with sender number as index
                        MsgTextTime newMessage = new MsgTextTime(String.valueOf(msgs[i].getTimestampMillis()));
                        newMessage.addText(msgs[i].getMessageBody());
                        msg.put(msgs[i].getOriginatingAddress(), newMessage);

                    } else {
                        // Number has been there, add content but consider that
                        // msg.get(originatinAddress) already contains sms:sndrNbr:previousparts of SMS,
                        // so just add the part of the current PDU
                        msg.get(originatinAddress).addText(msgs[i].getMessageBody());
                    }
                }
            }
        }

        return msg;
    }

    private class MsgTextTime{
        String time;
        StringBuilder text;

        public MsgTextTime(String time) {
            this.time = time;
            text = new StringBuilder();
        }

        public void addText(String s) {
            text.append(s);
        }

        public String getText() {
            return text.toString();
        }

        public String getTime() {
            return time;
        }

    }

}