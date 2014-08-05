package com.example.wzwang.syncmessage;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;
import android.support.v4.content.LocalBroadcastManager;

import com.google.gson.Gson;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Handler;

/**
 * Created by wzwang on 14-7-26.
 */
public enum WebSocket {
    INSTANCE;
    private WebSocketClient mWebSocketClient;
    private boolean isConnect = false;
    private boolean isLogin = false;
    private String IMEI = null;
    private String IMSI = null;
    private SendingMessage msg;
    private String URI = "ws://www.lifeincode.net:1337";
    private LocalBroadcastManager broadcastManager;
    private LinkedBlockingQueue<Message> msgQueue = new LinkedBlockingQueue<Message>(10);
    private LinkedBlockingQueue<RequestMessage> smsQueue = new LinkedBlockingQueue<RequestMessage>();
    ThreadSendingMsg tmsg = new ThreadSendingMsg();
    Thread thread = new Thread(tmsg);
    ThreadSendingSMS tsmsSender = new ThreadSendingSMS();
    Thread threadSMS = new Thread(tsmsSender);


    private final String TAG= "com.example.wzwang.syncmessage.WebSocket";



    private void initialize() {
        java.net.URI uri;
        try {
            uri = new URI(URI);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                sendPhoneInfo();
            }

            @Override
            public void onMessage(String message) {
                Log.i("Websocket", message);
                Gson parser = new Gson();
                if (!isLogin) {
                    ReceivingMessage loginMsg = parser.fromJson(message, ReceivingMessage.class);
                    int loginStatus = loginMsg.login;

                    Intent newIntent = new Intent();
                    newIntent.putExtra("login", loginStatus);
                    newIntent.setAction(".loginStatus");
                    if (loginStatus == 200) {
                        isLogin = true;
                        newIntent.putExtra("username", loginMsg.username);
                        startSendingMsg();
                    } else
                        isLogin = false;
                    if (loginStatus == 405) {
                        newIntent.setAction(".regUserStatus");
                        newIntent.putExtra("regUser", loginMsg.setUser);
                    }
                    broadcastManager.sendBroadcast(newIntent);
                } else {
                    RequestMessage req = parser.fromJson(message, RequestMessage.class);
                    if (req.req == 600) {
                        smsQueue.offer(req);
                    }
                }
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                isConnect = false;
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };
    }

    public boolean connect(String IMEI, String IMSI) {
        if (isConnect) {
            Log.i(TAG, "Already connected.");
            return true;
        }
        if (mWebSocketClient == null)
            initialize();
        try {
            this.IMEI = IMEI;
            this.IMSI = IMSI;
            this.msg = new SendingMessage();
            msg.IMSI = IMSI;
            msg.IMEI = IMEI;
            mWebSocketClient.connect();
            isConnect = true;

            return true;
        } catch (IllegalStateException e) {
            //e.printStackTrace();
            Log.i(TAG, "Connect failed. Reconnecting...");
            initialize();
            if (connect(IMEI, IMSI))
                return true;
            return false;
        }
    }

    private void sendPhoneInfo() {
        SendingMessage message = new SendingMessage();
        message.IMEI = this.IMEI;
        message.IMSI = this.IMSI;
        message.login = true;
        Gson gson = new Gson();
        send(gson.toJson(message));
    }


    public boolean isConnect() {
        return isConnect;
    }

    public void send(String msg) {
        if (isConnect)
            mWebSocketClient.send(msg);
    }

    public void send(SendingMessage msg) {
        Gson gson = new Gson();
        send(gson.toJson(msg));
    }

    public void setBroadcastManager(LocalBroadcastManager broadcastManager) {
        this.broadcastManager = broadcastManager;
    }

    public WebSocketClient getSocket() {
        if (mWebSocketClient == null)
            initialize();
        return mWebSocketClient;
    }

    public void setIMEI(String IMEI) {
        this.IMEI = IMEI;
    }

    public void setIMSI(String IMSI) {
        this.IMSI = IMSI;
    }

    public void sendUsername(String username) {
        msg.login = true;
        msg.setUser = true;
        msg.user = username;
        send(msg);
    }

    public class ThreadSendingMsg implements Runnable {
        public void run() {
            while (true) {
                Message msg = null;
                try {
                    msg = msgQueue.take();
                    SendingMessage syncMessage = new SendingMessage();
                    syncMessage.msg = msg;
                    send(syncMessage);
                } catch (InterruptedException e) {
                    if (msgQueue.isEmpty())
                        break;
                }

                System.out.println(msg.content);
            }
        }
    }

    private void startSendingMsg() {
        thread.start();
        threadSMS.start();
    }

    public void pushMsg(Message msg) {
        try {
            this.msgQueue.put(msg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public class ThreadSendingSMS implements Runnable {
        public void run() {
            while (true) {
                RequestMessage msg = null;
                try {
                    msg = smsQueue.take();
                    sendSMS(msg);
                } catch (InterruptedException e) {
                    if (smsQueue.isEmpty())
                        break;
                }
            }
        }
    }

    public void sendSMS(RequestMessage msg) {
        SmsManager smsManager = SmsManager.getDefault();
        List<String> texts =smsManager.divideMessage(msg.text);
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            if (text == null || text.length() == 0)
                continue;
            smsManager.sendTextMessage(msg.number, null, text, null, null);
        }
    }

}
