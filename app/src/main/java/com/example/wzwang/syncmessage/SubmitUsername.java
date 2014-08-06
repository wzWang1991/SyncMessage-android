package com.example.wzwang.syncmessage;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.wzwang.syncmessage.R;

public class SubmitUsername extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_username);
        IntentFilter iff = new IntentFilter(".regUserStatus");
        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, iff);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.submit_username, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void sendUsername(View view) {
        EditText usernameText = (EditText) findViewById(R.id.username);
        String username = usernameText.getText().toString();
        if (username.length() == 0) {
            return;
        }
        WebSocket.INSTANCE.sendUsername(username);
    }

    private BroadcastReceiver onNotice = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int regUser = 0;
            if (intent.getAction().equals(".regUserStatus")) {
                regUser = intent.getIntExtra("regUser", 0);
                if (regUser == 202) {
                    Intent it = new Intent();
                    intent.putExtra("regStatus", 0);
                    SubmitUsername.this.setResult(0, intent);
                    SubmitUsername.this.finish();
                }
            }

        }
    };

}
