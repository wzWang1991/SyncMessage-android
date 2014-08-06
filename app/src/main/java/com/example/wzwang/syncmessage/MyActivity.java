package com.example.wzwang.syncmessage;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
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
            setSmsAsReaded();
            TextView myTextView = (TextView)findViewById(R.id.test);
            myTextView.setText("New message synchronized!");

        }
    };


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

    private void setSmsAsReaded() {
        Cursor csr = getContentResolver().query(Uri.parse("content://sms"), null,
                "type = 1 and read = 0", null, null);
        if (csr != null) {
            while (csr.moveToNext()) {
                String SmsMessageId = csr.getString(csr.getColumnIndex("_id"));
                ContentValues values = new ContentValues();
                values.put("read", true);
                getApplicationContext().getContentResolver().update(Uri.parse("content://sms/inbox"), values, "_id=" + SmsMessageId, null);
            }
        }
        csr.close();
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
