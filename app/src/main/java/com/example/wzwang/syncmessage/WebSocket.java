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
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


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
    private String URI = "ws://192.168.1.107:2945";
    private LocalBroadcastManager broadcastManager;
    private LinkedBlockingQueue<Sms> reportSmsQueue = new LinkedBlockingQueue<Sms>();
    private LinkedBlockingQueue<Sms> smsQueue = new LinkedBlockingQueue<Sms>();
    private LinkedBlockingQueue<MsgPacket> sendingMsgQueue = new LinkedBlockingQueue<MsgPacket>();
    private Object sendingMsgCondition = new Object();
    private int heartInterval = 15000;
    ThreadSyncSms threadSyncSms = new ThreadSyncSms();
    Thread thread = new Thread(threadSyncSms);
    ThreadSendingSMS tsmsSender = new ThreadSendingSMS();
    Thread threadSMS = new Thread(tsmsSender);
    HeartBeat heartBeat = new HeartBeat();
    Thread threadHeartBeat = new Thread(heartBeat);
    SendingMsgPacket sendingMsgPacket = new SendingMsgPacket();
    Thread threadSendingMessage = new Thread(sendingMsgPacket);



    private final String TAG= "com.example.wzwang.syncmessage.WebSocket";



    private void initialize() {
        java.net.URI uri;
        try {
            uri = new URI(URI);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        if (!threadSendingMessage.isAlive())
            threadSendingMessage.start();
        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                synchronized (sendingMsgCondition) {
                    isConnect = true;
                    sendingMsgCondition.notify();
                }
                sendPhoneInfo();
            }

            @Override
            public void onMessage(String message) {
                Log.i("Websocket", message);
                Gson parser = new Gson();
                MsgPacket msg = parser.fromJson(message, MsgPacket.class);
                if (msg.req == 0) {
                    //Return packet
                    Intent newIntent = new Intent();
                    switch (msg.ret) {
                        case 200:
                            //Login successfully.
                            isLogin = true;
                            newIntent.setAction(".loginStatus");
                            newIntent.putExtra("login", 200);
                            newIntent.putExtra("username", msg.username);
                            broadcastManager.sendBroadcast(newIntent);
                            startSendingMsg();
                            break;
                        case 201:
                            // No such user
                            newIntent.setAction(".loginStatus");
                            newIntent.putExtra("login", 201);
                            broadcastManager.sendBroadcast(newIntent);
                            break;
                        case 202:
                            // Set user successfully.
                            newIntent.setAction(".regUserStatus");
                            newIntent.putExtra("regUser", 202);
                            broadcastManager.sendBroadcast(newIntent);
                            break;
                        case 203:
                            // No such user.
                            break;
                        default:
                            break;
                    }
                } else {
                    if (msg.req == 500) {
                        if (isLogin) {
                            smsQueue.offer(msg.sms);
                        }
                    }
                }
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                isConnect = false;
                isLogin = false;
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
            mWebSocketClient.connect();
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
        MsgPacket newMsg = MsgPacket.generateLoginMessage();
        newMsg.IMSI = IMSI;
        newMsg.IMEI = IMEI;
        addMsgPacketToQueue(newMsg);
    }

    public void addMsgPacketToQueue(MsgPacket msg) {
        try {
            sendingMsgQueue.put(msg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public boolean isConnect() {
        return isConnect;
    }

    private void send(String msg) {
        if (isConnect)
            mWebSocketClient.send(msg);
        else {
            connect(IMEI, IMSI);
            mWebSocketClient.send(msg);
        }
    }


    Gson gson = new Gson();
    private void send(MsgPacket msg) {
        String msgString = gson.toJson(msg);
        try {
            send(msgString);
        } catch (WebsocketNotConnectedException exception) {
            try {
                sendingMsgQueue.put(msg);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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


    public class ThreadSyncSms implements Runnable {
        public void run() {
            while (true) {
                Sms msg = null;
                try {
                    msg = reportSmsQueue.take();
                    if (msg != null) {
                        MsgPacket smsPacket = MsgPacket.generateReportingSmsMessage(msg);
                        sendingMsgQueue.put(smsPacket);
                    }
                } catch (InterruptedException e) {
                    if (reportSmsQueue.isEmpty())
                        break;
                }
            }
        }
    }

    private void startSendingMsg() {
        if (!thread.isAlive())
            thread.start();
        if (threadSMS.isAlive())
            threadSMS.start();
        if (threadSendingMessage.isAlive())
            threadHeartBeat.start();
    }

    public void pushReportingSms(Sms sms) {
        try {
            this.reportSmsQueue.put(sms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public class ThreadSendingSMS implements Runnable {
        public void run() {
            while (true) {
                Sms msg = null;
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

    public class HeartBeat implements Runnable {
        public void run() {
            while (true) {
                sendHeartBeat();
                try {
                    Thread.sleep(heartInterval);
                } catch (InterruptedException e) {
                    continue;
                }
            }
        }
    }

    public void sendSMS(Sms msg) {
        SmsManager smsManager = SmsManager.getDefault();
        List<String> texts =smsManager.divideMessage(msg.text);
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            if (text == null || text.length() == 0)
                continue;
            smsManager.sendTextMessage(msg.toNumber, null, text, null, null);
        }
    }

    private void sendHeartBeat() {
        if (isConnect) {
            MsgPacket heartbeat = MsgPacket.generateHeartBeatMessage();
            try {
                sendingMsgQueue.put(heartbeat);
            } catch (InterruptedException e) {
                return;
            }
        } else {
            connect(IMEI, IMSI);
        }
    }



    public void setURI(String uri) {
        this.URI = uri;
    }

    public class SendingMsgPacket implements Runnable {
        public void run() {
            while (true) {
                synchronized (sendingMsgCondition) {
                    while (!isConnect) {
                        try {
                            sendingMsgCondition.wait();
                            if (isConnect)
                                break;
                        } catch (InterruptedException e) {
                            continue;
                        }
                    }
                }
                MsgPacket msg = null;
                try {
                        msg = sendingMsgQueue.take();
                        send(msg);
                } catch (InterruptedException e) {
                    continue;
                }
            }
        }
    }

    public void sendUsername(String username) {
        MsgPacket msg = MsgPacket.generateUserRegMessage(username);
        addMsgPacketToQueue(msg);
    }

}
