package com.example.wzwang.syncmessage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

/**
 * Created by wzwang on 14-7-25.
 */
public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //---get the SMS message passed in---
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs = null;
        String str = "";
        if (bundle != null) {
            //---retrieve the SMS message received---
            Object[] pdus = (Object[]) bundle.get("pdus");
            msgs = new SmsMessage[pdus.length];
            for (int i = 0; i < msgs.length; i++) {
                msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                Message newMessage = new Message();
                newMessage.fromNumber = msgs[i].getOriginatingAddress();
                newMessage.content = msgs[i].getMessageBody().toString();
                newMessage.time = String.valueOf(msgs[i].getTimestampMillis());
                //---display the new SMS message---
                Gson gson = new Gson();
                Log.d("SMSreceiver.java", gson.toJson(newMessage));
                WebSocket.INSTANCE.pushMsg(newMessage);
            }
            Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
        }
    }
}