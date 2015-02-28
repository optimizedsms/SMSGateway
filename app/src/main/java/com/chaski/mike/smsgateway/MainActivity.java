package com.chaski.mike.smsgateway;
/**
 * Created by sam and mike on 1/12/15.
 *
 * MainActivity presents a user interface for the optimized SMS package.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) Chaski Telecommunications, Inc.
 *
 */
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.lang.reflect.Field;
import java.lang.reflect.Method;


public class MainActivity extends Activity implements View.OnClickListener {

    Broadcast broadcast;
    Thread broadcastThread;
    AdbServer adb ;
    KeepAlive keepAlive;
    Thread thread;
    int interval=30;
    String defaultPhone = "";
    Thread keepAliveThread;
    SMSReceiver smsreceiver;
    String target;
    IetfCoAPOptimizedSMS osms;
    String lastData;
    public  final String tag = getClass().getSimpleName().toString();
    long smsIn;
    long smsOut;
    long piToDroid;
    long droidToPi;


    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.saveButton:
                saveValues();
                break;

            case R.id.statsButton:
                statsDialog();
                break;

            case R.id.startButton:
                ToggleButton tb = (ToggleButton) v;
                String currentText = tb.getText().toString();
                String offText = tb.getTextOff().toString();
                String onText = tb.getTextOn().toString();

                if (currentText.equals(tb.getTextOff())) {
                    keepAliveThread.interrupt();
                    thread.interrupt();;
                    broadcastThread.interrupt();
                } else {

                   // we are off, turn on

                    if (keepAlive != null && keepAlive.getServiceState())
                    {
                        tb.setChecked(false);
                        tb.setText("Pending Shutdown..try again");
                        return;
                    }

                    if (thread != null && adb.getServiceState())
                    {
                        tb.setChecked(false);
                        tb.setText("Pending Shutdown..try again");
                        return;
                    }

                    if (broadcastThread != null && broadcast.getServiceState())
                    {
                        tb.setChecked(false);
                        tb.setText("Pending Shutdown..try again");
                        return;
                    }


                    thread = new Thread(adb, "ADB Server");
                    thread.start();
                    Log.v(tag,"starting adb");

                    keepAliveThread = new Thread(keepAlive, "Keep Alive");
                    keepAliveThread.start();

                    broadcastThread = new Thread(broadcast, "IP Server");
                    broadcastThread.start();
                }


                break;
            default:
                break;
        }

    }





    void saveValues () {
        TextView tv = (TextView) findViewById(R.id.defaultPhone);
        defaultPhone = tv.getText().toString();
        savePreferences("DefaultPhone",defaultPhone);
    }


    private void loadSavedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String defaultPhone = sharedPreferences.getString("phone", "none");
    }

    private void savePreferences(String key, int value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    private void savePreferences(String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    private String getStringPreferences(String key) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        String retVal = sharedPreferences.getString(key, "none");
        return retVal;
    }

    private int getIntPreferences(String key) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        int retVal = sharedPreferences.getInt(key, 10);
        return retVal;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_screen);

       // PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
       // PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
       // if (wakeLock.isHeld() == false)
       //    wakeLock.acquire();

        target = "";
        String ipaddress = Utils.getIPAddress(true);
        TextView tv = (TextView) findViewById(R.id.ipaddressId);
        tv.setText(ipaddress);

        String version = getVersionInfo();
        tv = (TextView) findViewById(R.id.softwareVersion);
        tv.setText(version);

        tv = (TextView) findViewById(R.id.defaultPhone);
        tv.setText(getStringPreferences("DefaultPhone"));
        defaultPhone = tv.getText().toString();



        adb = new AdbServer(this);
        keepAlive = new KeepAlive(adb,interval);
        broadcast = new Broadcast(this);



        osms = new IetfCoAPOptimizedSMS();
        osms.loadMap();

        Button button = (Button) findViewById(R.id.saveButton);
        button.setOnClickListener(this);

        button = (Button) findViewById(R.id.statsButton);
        button.setOnClickListener(this);

        ToggleButton tb = (ToggleButton) findViewById(R.id.startButton);
        tb.setOnClickListener(this);

        smsreceiver = new SMSReceiver();
        registerReceiver(smsreceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));


        TextView serviceState = (TextView) findViewById(R.id.serviceStatus);
        serviceState.setText("Service State: off");
        serviceState.setTextColor(Color.RED);
        TextView connectionState = (TextView) findViewById(R.id.connectionStatus);
        connectionState.setText("Connection State: disconnected");
        connectionState.setTextColor(Color.RED);
        lastData="";
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String data;
        data = intent.getStringExtra("sms");

        if (data == null)
            return;

        if (data.equals(lastData)) {
            return;  // duplicate of last message
        }
        lastData = data;
        target = intent.getStringExtra("phone");

        Log.v(tag,"data was received");
        smsIn++;
        byte[] message = osms.bufferToMessage(data.getBytes());

        adb.addInboundMessage(message);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public String getTarget() {
        return target;
    }


    private void setTargetText(final String value){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView tv = (TextView) findViewById(R.id.defaultPhone);
                    tv.setText(value);
                }
            });
    }

    public void countPiToDroidIp() {
        piToDroid++;
    }

    public void countDroidToPi() {
        droidToPi++;
    }

    public void sendSMS(byte[] buffer) {
        String messageStr;
        smsOut++;
        byte message[] = osms.messageToBuffer(buffer);
        messageStr = new String(message);
        Log.v(tag,messageStr);

        if ((target.equals("")==true) || target == null) {
            Log.e(tag,"no target");
            setTargetText("NO TARGET");
            return;
        }
        setTargetText(target);
        SMSReceiver.sendSMS(target, messageStr, this);
    }

    public String getVersionInfo() {
        String strVersion = "Version:";

        PackageInfo packageInfo;
        try {
            packageInfo = getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(
                            getApplicationContext().getPackageName(),
                            0
                    );
            strVersion += packageInfo.versionName;
        } catch (Exception e) {
            strVersion += "Unknown";
        }

        return strVersion;
    }

    private void setMobileDataEnabled(Context context, boolean enabled) {
        try {
            final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final Class conmanClass = Class.forName(conman.getClass().getName());
            final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField.get(conman);
            final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
            final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);
            setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
        } catch (Exception e) {

        }
    }

    public void statsDialog() {
        // custom dialog
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.useful_info);
        dialog.setTitle("Packet Counts");

        // set the custom dialog components - text, image and button
        TextView text = (TextView) dialog.findViewById(R.id.ipCountCount);
        text.setText(""+droidToPi);

        text = (TextView) dialog.findViewById(R.id.ipPiToDroidCountCount);
        text.setText(""+piToDroid);

        text = (TextView) dialog.findViewById(R.id.smsInCount);
        text.setText(""+smsIn);


        text = (TextView) dialog.findViewById(R.id.smsOutCount);
        text.setText(""+smsOut);


        Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOk);
        // if button is clicked, close the custom dialog
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialogButton = (Button) dialog.findViewById(R.id.dialogButtonClear);
        // if button is clicked, close the custom dialog
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                smsIn=0;
                smsOut=0;
                droidToPi=0;
                piToDroid=0;
                dialog.dismiss();
            }
        });

        dialog.show();
        dialog.getWindow().setLayout(300, 400);
    }

}
