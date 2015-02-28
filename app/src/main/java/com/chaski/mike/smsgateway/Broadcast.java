package com.chaski.mike.smsgateway;

/**
 * Created by sam and mike on 1/12/15.
 * However, inspiration from:
 * https://code.google.com/p/boxeeremote/source/browse/trunk/Boxee+Remote/src/com/andrewchatham/Discoverer.java?spec=svn28&r=28
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


import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Broadcast implements Runnable {

    DatagramSocket socket;
    MainActivity mContext;
    private static final int DISCOVERY_PORT = 50004;
    WifiManager wifi;
    WifiManager.MulticastLock lock;

    private final String tag = getClass().getSimpleName().toString();

    boolean broadcastServiceState = false;

    public Broadcast(MainActivity _context) {

        mContext = _context;
        String ipaddress = Utils.getIPAddress(true);
        setMiIpText(ipaddress);
    }

    @TargetApi(4)
    public void setMulticastLock()
    {
       try {
            WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            if (wifi != null) {
                lock = wifi.createMulticastLock("Log_Tag");
                lock.acquire();
                lock = lock;
            }
        }
        catch (Exception e) {
         Log.v(tag, "Unable to acquire lock");
        }
      }

    @TargetApi(4)
    public void freeMulticastLock()
    {
       try {
            lock.release();
        } catch (Exception e) {
            Log.v(tag, "unable to release lock");
        }

    }

    InetAddress getBroadcastAddress() throws Exception {
        WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    private void setMiIpText(final String ipaddress) {

        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EditText et = (EditText) mContext.findViewById(R.id.ipaddressId);
                et.setText(ipaddress);
            }
        });

    }

    private void setPiIpText(final String value){
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EditText et = (EditText) mContext.findViewById(R.id.piIpaddressId);
                et.setText(value);
            }
        });
    }

    private void setWifiStatus(final String value, final int color){
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tv = (TextView) mContext.findViewById(R.id.wifiStatus);
                tv.setText(value);
                tv.setTextColor(color);
            }
        });
    }

    public void whatever(String data) throws Exception {

        socket = new DatagramSocket(DISCOVERY_PORT);
        socket.setBroadcast(true);
        socket.setSoTimeout(5000);

        DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(),
                getBroadcastAddress(), 50005);
        socket.send(packet);

        byte[] buf = new byte[1024];


        DatagramPacket newPacket = new DatagramPacket(buf, buf.length);
        socket.receive(newPacket);

        //newPacket.setData(data.getBytes());
        Log.v(tag, "Port:"+newPacket.getPort());
        Log.v(tag, "IP:"+newPacket.getAddress().toString());


        socket.send(packet);
        String piIpaddress = newPacket.getAddress().toString().replace("/","");
        setPiIpText(piIpaddress);
        socket.close();

    }

    @Override
    public void run() {
        String ipaddress;
        broadcastServiceState = false;
        if (socket != null)
            return;

        setMulticastLock();

        broadcastServiceState = true;

        while(Thread.currentThread().isInterrupted() == false) {

        try {
            ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (mWifi.isConnected()) {
                ipaddress = Utils.getIPAddress(true);
                setWifiStatus("Wifi Status: Connected", Color.GREEN);
                setMiIpText(ipaddress);
            }
            else {
                ipaddress = "127.0.0.1";
                setWifiStatus("Wifi Status: Disconnected", Color.RED);
            }

            whatever(ipaddress);
            Thread.sleep(1000);
        }
        catch (Exception e) {
            if (socket != null)
             socket.close();
            Log.v(tag," broadcast:"+e);

        }
        }
        Log.v(tag,"broadcast thread done");

        // really should fix this.
        // this is a concurancy/synchronization problem that occasionally pops up.
        //
        try {
            socket.disconnect();
            socket.close();
            socket = null;
        } catch (Exception e) {
            socket=null;
        }

        freeMulticastLock();
        broadcastServiceState = false;
    }

    public boolean getServiceState() {
        return broadcastServiceState;
    }
}
