package com.example.wzwang.syncmessage;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import android.support.v4.content.LocalBroadcastManager;


public class MyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        registerObserver();
        IntentFilter iff= new IntentFilter(".loginStatus");
        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, iff);

    }


    protected void onDestroy() {
        super.onDestroy();
        this.stopService(new Intent(this, BackgroundService.class));
    }


    private ContentObserver newMmsContentObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            int mNewSmsCount = getNewSmsCount() + getNewMmsCount();

            TextView myTextView = (TextView)findViewById(R.id.test);
            myTextView.setText(String.valueOf(mNewSmsCount));

        }
    };


    public String getSmsInPhone()
    {
        final String SMS_URI_ALL   = "content://sms/";
        final String SMS_URI_INBOX = "content://sms/inbox";
        final String SMS_URI_SEND  = "content://sms/sent";
        final String SMS_URI_DRAFT = "content://sms/draft";

        StringBuilder smsBuilder = new StringBuilder();

        try{
            ContentResolver cr = getContentResolver();
            String[] projection = new String[]{"_id", "address", "person",
                    "body", "date", "type"};
            Uri uri = Uri.parse(SMS_URI_ALL);
            Cursor cur = cr.query(uri, projection, null, null, "date desc");

            if (cur.moveToFirst()) {
                String name;
                String phoneNumber;
                String smsbody;
                String date;
                String type;
                String read;

                int nameColumn = cur.getColumnIndex("person");
                int phoneNumberColumn = cur.getColumnIndex("address");
                int smsbodyColumn = cur.getColumnIndex("body");
                int dateColumn = cur.getColumnIndex("date");
                int typeColumn = cur.getColumnIndex("type");
                int readColumn = cur.getColumnIndex("read");

                do{
                    read = cur.getString(readColumn);
                    if (read.equals("0")) {
                        name = cur.getString(nameColumn);
                        phoneNumber = cur.getString(phoneNumberColumn);
                        smsbody = cur.getString(smsbodyColumn);

                        SimpleDateFormat dateFormat = new SimpleDateFormat(
                                "yyyy-MM-dd hh:mm:ss");
                        Date d = new Date(Long.parseLong(cur.getString(dateColumn)));
                        date = dateFormat.format(d);

                        int typeId = cur.getInt(typeColumn);
                        if(typeId == 1){
                            type = "接收";
                        } else if(typeId == 2){
                            type = "发送";
                        } else {
                            type = "";
                        }

                        smsBuilder.append("[");
                        smsBuilder.append(name+",");
                        smsBuilder.append(phoneNumber+",");
                        smsBuilder.append(smsbody+",");
                        smsBuilder.append(date+",");
                        smsBuilder.append(type);
                        smsBuilder.append("] ");

                        if(smsbody == null) smsbody = "";
                    }

                } while(cur.moveToNext());
            } else {
                smsBuilder.append("no result!");
            }

            smsBuilder.append("getSmsInPhone has executed!");
        } catch(SQLiteException ex) {
            Log.d("SQLiteException in getSmsInPhone", ex.getMessage());
        }
        return smsBuilder.toString();
    }

    private void registerObserver() {
        unregisterObserver();
        getContentResolver().registerContentObserver(Uri.parse("content://sms"), true,
                newMmsContentObserver);
        getContentResolver().registerContentObserver(Uri.parse("content://mms-sms"), true,
                newMmsContentObserver);
    }

    private synchronized void unregisterObserver() {
        try {
            if (newMmsContentObserver != null) {
                getContentResolver().unregisterContentObserver(newMmsContentObserver);
            }
            if (newMmsContentObserver != null) {
                getContentResolver().unregisterContentObserver(newMmsContentObserver);
            }
        } catch (Exception e) {
            Log.e("Fail", "unregisterObserver fail");
        }
    }

    private int getNewSmsCount() {
        int result = 0;
        Cursor csr = getContentResolver().query(Uri.parse("content://sms"), null,
                "type = 1 and read = 0", null, null);
        if (csr != null) {
            result = csr.getCount();
            csr.close();
        }
        return result;
    }
    private int getNewMmsCount() {
        int result = 0;
        Cursor csr = getContentResolver().query(Uri.parse("content://mms/inbox"),
                null, "read = 0", null, null);
        if (csr != null) {
            result = csr.getCount();
            csr.close();
        }

        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, Settings.class);
            startActivity(intent);
            return false;
        }
        return super.onOptionsItemSelected(item);
    }

    private BroadcastReceiver onNotice= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int login = 0;
            if(intent.getAction().equals(".loginStatus")){
                login = intent.getIntExtra("login", 0);
                if (login == 201) {
                    Intent it= new Intent(getApplicationContext(), SubmitUsername.class);
                    startActivityForResult(it, 0);
                } else if (login == 200) {
                    TextView myTextView = (TextView)findViewById(R.id.test);
                    String username = intent.getStringExtra("username");
                    myTextView.setText("Connected. This phone belongs to " + username + ".");
                    startService(new Intent(getApplicationContext(), BackgroundService.class));
                }
            }

        }
    };

    public void connectToServer(View view) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean testMode = preferences.getBoolean("test_mode", false);
        if (!testMode) {
            String address = preferences.getString("ws_server", "");
            WebSocket.INSTANCE.setURI(address);
        }
        WebSocket.INSTANCE.setBroadcastManager(LocalBroadcastManager.getInstance(getApplicationContext()));
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String imei = tm.getDeviceId();
        String imsi =tm.getSubscriberId();
        WebSocket.INSTANCE.connect(imei, imsi);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        TextView myTextView = (TextView)findViewById(R.id.test);
        if (requestCode == 0 && resultCode == 0) {
            int regStatus = data.getIntExtra("regStatus", -1);
            if (regStatus == 0) {
                WebSocket.INSTANCE.getSocket().close();
                myTextView.setText("Register successfully. Please reconnect..");
            }
        }

    }



}
