package com.chaski.mike.smsgateway;
/**
 * Created by sam and mike on 1/12/15.
 *
 * KeepAlive attempts to maintain the connection to the Raspberry P.
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

public class KeepAlive implements Runnable {
    byte[] tempMsg = {0x41, 0x01, 0x30, 0x77, (byte) 0x94, 0x74, 0x65, 0x6d, 0x70};  // canned get temp message
    byte[] uptimeMsg = {0x41, 0x01, 0x00 ,0x00, (byte)0x96, 0x75, 0x70, 0x74, 0x69,  (byte)0x6D,  (byte)0x65};
    byte[] counterMsg = {0x41 ,0x01 ,0x00 ,0x00 ,(byte)0x97, 0x63, 0x6F, 0x75, 0x6E, 0x74, 0x65 ,0x72};
    AdbServer adbs;

    int interval = 1;

    KeepAlive(AdbServer _adbs, int _interval) {
        adbs = _adbs;
        interval = _interval;
    }

    public void oneMessage() {
        if (adbs == null) return;
        adbs.addInboundMessage(counterMsg);
    }


    boolean keepAliveState = false;

    @Override
    public void run() {
        keepAliveState = true;
        while (true) {
            try {
                Thread.sleep(interval*1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                break;
            }

            if (Thread.currentThread().isInterrupted())
                break;

            adbs.addInboundMessage(counterMsg);

        }
        keepAliveState = false;
    }

    public boolean getServiceState() {
        return keepAliveState;
    }

}
