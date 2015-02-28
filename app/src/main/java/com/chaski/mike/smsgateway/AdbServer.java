package com.chaski.mike.smsgateway;

/**
 * Created by sam and mike on 1/12/15.
 *
 * AdbServer presents a server interface to the client.py software running on the Raspberry P.
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentLinkedQueue;
import android.graphics.Color;
import android.util.Log;
import android.widget.TextView;


public class AdbServer implements Runnable {

    private final String DEBUG_TAG = getClass().getSimpleName().toString();
    public static final int TIMEOUT=10;
    ServerSocket server=null;
    DataOutputStream socketOut;
    Socket s = null;
    ConcurrentLinkedQueue<MessageBuffer> inbound = new ConcurrentLinkedQueue<MessageBuffer>();
    ConcurrentLinkedQueue<MessageBuffer> outbound = new ConcurrentLinkedQueue<MessageBuffer>();
    MainActivity myActivity;

    CommunicationThread comThread;
    Thread thread;
    transmissionThread txThread;
    Thread thread2;
    boolean adbServerRunning = false;
    //public static final String tag="OSMS-GATEWAY";

    public AdbServer(MainActivity _activity) {

       myActivity = _activity;

    }

    byte[] tempMsg = {0x41, 0x01, 0x30, 0x77, (byte) 0x94, 0x74, 0x65, 0x6d, 0x70};  // canned get temp message

    public void addInboundMessage(byte[] message) {
        MessageBuffer mb = new MessageBuffer(message);
        inbound.add(mb);
    }



    private void setServiceStatusText(final String value, final int color){
        myActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tv = (TextView) myActivity.findViewById(R.id.serviceStatus);
                tv.setText(value);
                tv.setTextColor(color);
            }
        });
    }

    private void setConnectionStatusText(final String value, final int color){
        myActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tv = (TextView) myActivity.findViewById(R.id.connectionStatus);
                tv.setText(value);
                tv.setTextColor(color);
            }
        });
    }

    public boolean smsToDevice() {
        try {
            //socketOut = new PrintWriter(s.getOutputStream(), true);
            if (inbound.isEmpty())
                return false;

            MessageBuffer message = inbound.remove();



            if (message==null) {
                Log.e(DEBUG_TAG,"Bad message");
                return false;
            }

            if (socketOut == null) {
                Log.e(DEBUG_TAG,"Socket was lost");
                return false;
            }

            myActivity.countDroidToPi();
            socketOut.write(message.message);
            //socketOut.close();
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    public void run() {
        adbServerRunning = true;
        setServiceStatusText("Service State: running", Color.GREEN);
        setConnectionStatusText("Connection State: disconnected", Color.RED);
        // initialize server socket
        try{
            server = new ServerSocket(50001);
            server.setSoTimeout(30000);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }




        while (!Thread.currentThread().isInterrupted()) {
            try {
                s = server.accept();
            } catch (Exception e) {

               continue;
            }

            Log.i(DEBUG_TAG,"accepted connection");
            if (s == null) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
                continue;
            }

            try {
                socketOut = new DataOutputStream(s.getOutputStream());
            } catch (Exception e) {

            }


            if (thread!=null)
                thread.interrupt();

            if (thread2!=null)
                thread2.interrupt();

            if (Thread.currentThread().isInterrupted()) {

                try {
                    socketOut.close();
                }
                catch (Exception e) {
                   break;
                }
                break;
            }


            comThread = new CommunicationThread(s,  myActivity);
            thread = new Thread(comThread, "comm thread");
            thread.start();

            txThread = new transmissionThread();
            thread2= new Thread(txThread, "tx thread");
            thread2.start();

        }

        if (thread!=null)
            thread.interrupt();

        if (thread2!=null)
            thread2.interrupt();


        try {
            server.close();
        }
        catch (Exception e) {
            Log.v(DEBUG_TAG, "Error closing")   ;
            }
        setServiceStatusText("Service State: off", Color.RED);
        setConnectionStatusText("Connection State: disconnected", Color.RED);
        adbServerRunning = false;
    }


    public boolean getServiceState() {
        return adbServerRunning;
    }




    class CommunicationThread implements Runnable {

        private Socket serverSocket;

        private BufferedReader input;
        InputStream inputStream;
        byte[] content = new byte [2048];
        MainActivity activity;


        public CommunicationThread(Socket serverSocket, MainActivity _activity) {

            this.activity = _activity;
            this.serverSocket = serverSocket;

            try {
                this.inputStream = serverSocket.getInputStream();
                this.serverSocket.setSoTimeout(5000);
            } catch (IOException e1) {

            }

            try {
                Log.i(DEBUG_TAG,"new bufferedreader");
                this.input = new BufferedReader(new InputStreamReader(this.serverSocket.getInputStream()));
            } catch (IOException e) {
               // e.printStackTrace();

            }
        }

        public void run() {

            setServiceStatusText("Service State: running", Color.GREEN);
            setConnectionStatusText("Connection State: connected", Color.GREEN);

            while (!Thread.currentThread().isInterrupted()) {

                Log.i(DEBUG_TAG,"Starting..");


                try {

                    //String read = input.readLine();
                    int count = inputStream.read(content);

                    if (count  <= 0)
                    {
                        Log.i(DEBUG_TAG,"End of Stream, must have lost socket");
                        break;
                    }

                    myActivity.countPiToDroidIp();
                    // wacky stuff to not send too large a buffer
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    baos.write(content,0,count);
                    byte data[] = baos.toByteArray();

                    if (count < 2048)
                        Log.i(DEBUG_TAG,HexDump.dumpHexString(data));

                    activity.sendSMS(data);


                } catch (SocketTimeoutException e0) {
                     Log.i(DEBUG_TAG, "Socket timeout");
                        continue;
                }  catch (Exception e) {

                    Log.i(DEBUG_TAG,"Socket error");
                    try {
                        serverSocket.close();
                        Thread.currentThread().interrupt();
                        break;
                    }
                    catch (Exception e1) {
                        break;
                    }
                }

                try {
                    Thread.sleep(100) ;
                } catch (Exception e) {
                }

            }
            Log.i(DEBUG_TAG,"Comm thread is done");
           // setServiceStatusText("Service State: running");
            setConnectionStatusText("Connection State: disconnected", Color.RED);
            try {
                this.serverSocket.close();
                ;
            } catch (Exception e) {}



        }



    }


    class transmissionThread implements Runnable {
        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()) {
                smsToDevice()  ;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.v(DEBUG_TAG,"interrupted exception");
                    break;
                }
            }

            Log.v(DEBUG_TAG,"communications thread done");
        }

    }

    private class MessageBuffer {
        public MessageBuffer(byte[] message2) {
            message = message2;
        }
        byte[] message;
    }

}
