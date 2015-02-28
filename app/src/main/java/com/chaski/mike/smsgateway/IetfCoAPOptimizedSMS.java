package com.chaski.mike.smsgateway;

/**
 *
 * IeftCoAPOptimizedSMS.java
 * Created by sam and mike on 1/12/15.
 *
 * The heart of the application.
 * Translates SMS messages into optimized SMS messages and back to text.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.ByteBuffer;


public class IetfCoAPOptimizedSMS {

    // used for unit testing
    public static byte testmessage1[] = {(byte) 'b', (byte) 'E', (byte) 'B',
            (byte) 0x89, (byte) 0x11, (byte) '(', (byte) 0xa7, (byte) 0x73,
            (byte) 0x6f, (byte) 0x6d, (byte) 0x65, (byte) 0x74, (byte) 0x6f,
            (byte) 0x6b, (byte) 0x3c, (byte) 0x2f, (byte) 0x3e, (byte) 0x3b,
            (byte) 0x74, (byte) 0x69, (byte) 0x74, (byte) 0x6c, (byte) 0x65,
            (byte) 0x3d, (byte) 0x22, (byte) 0x47, (byte) 0x65, (byte) 0x6e,
            (byte) 0x65, (byte) 0x72, (byte) 0x61, (byte) 0x6c, (byte) 0x20,
            (byte) 0x49, (byte) 0x6e, (byte) 0x66, (byte) 0x6f, (byte) 0x22,
            (byte) 0x3b, (byte) 0x63, (byte) 0x74, (byte) 0x3d, (byte) 0x30,
            (byte) 0x2c, (byte) 0x3c, (byte) 0x2f, (byte) 0x74, (byte) 0x69,
            (byte) 0x6d, (byte) 0x65, (byte) 0x3e, (byte) 0x3b, (byte) 0x69,
            (byte) 0x66, (byte) 0x3d, (byte) 0x22, (byte) 0x63, (byte) 0x6c,
            (byte) 0x6f, (byte) 0x63, (byte) 0x6b, (byte) 0x22, (byte) 0x3b,
            (byte) 0x72, (byte) 0x74, (byte) 0x3d, (byte) 0x22, (byte) 0x54,
            (byte) 0x69, (byte) 0x63, (byte) 0x6b, (byte) 0x73, (byte) 0x22,
            (byte) 0x3b, (byte) 0x74, (byte) 0x69, (byte) 0x74, (byte) 0x6c,
            (byte) 0x65, (byte) 0x3d, (byte) 0x22, (byte) 0x49, (byte) 0x6e,
            (byte) 0x74, (byte) 0x65, (byte) 0x72, (byte) 0x6e, (byte) 0x61,
            (byte) 0x6c, (byte) 0x20, (byte) 0x43, (byte) 0x6c, (byte) 0x6f,
            (byte) 0x63, (byte) 0x6b, (byte) 0x22, (byte) 0x3b, (byte) 0x63,
            (byte) 0x74, (byte) 0x3d, (byte) 0x30, (byte) 0x2c, (byte) 0x3c,
            (byte) 0x2f, (byte) 0x61, (byte) 0x73, (byte) 0x79, (byte) 0x6e,
            (byte) 0x63, (byte) 0x3e, (byte) 0x3b, (byte) 0x63, (byte) 0x74,
            (byte) 0x3d, (byte) 0x30,

    };


    Map<Integer, Byte> map = new HashMap<Integer, Byte>();
    Map<Byte, Integer> decodeMap = new HashMap<Byte, Integer>();



    public void loadMap() {

        map.put(100, (byte) 0x0b);
        map.put(101, (byte) 0x0c);
        map.put(102, (byte) 0x0e);
        map.put(110, (byte) 0x0f);
        map.put(111, (byte) 0x10);
        map.put(112, (byte) 0x11);
        map.put(120, (byte) 0x12);
        map.put(121, (byte) 0x13);
        map.put(122, (byte) 0x14);
        map.put(200, (byte) 0x15);
        map.put(201, (byte) 0x16);
        map.put(202, (byte) 0x17);
        map.put(210, (byte) 0x18);
        map.put(211, (byte) 0x19);
        map.put(212, (byte) 0x1a);
        map.put(220, (byte) 0x1c);
        map.put(221, (byte) 0x1d);
        map.put(222, (byte) 0x1e);

        decodeMap.put((byte) 0x0b, 0x100);
        decodeMap.put((byte) 0x0c, 0x101);
        decodeMap.put((byte) 0x0e, 0x102);
        decodeMap.put((byte) 0x0f, 0x110);
        decodeMap.put((byte) 0x10, 0x111);
        decodeMap.put((byte) 0x11, 0x112);
        decodeMap.put((byte) 0x12, 0x120);
        decodeMap.put((byte) 0x13, 0x121);
        decodeMap.put((byte) 0x14, 0x122);
        decodeMap.put((byte) 0x15, 0x200);
        decodeMap.put((byte) 0x16, 0x201);
        decodeMap.put((byte) 0x17, 0x202);
        decodeMap.put((byte) 0x18, 0x210);
        decodeMap.put((byte) 0x19, 0x211);
        decodeMap.put((byte) 0x1a, 0x212);
        decodeMap.put((byte) 0x1c, 0x220);
        decodeMap.put((byte) 0x1d, 0x221);
        decodeMap.put((byte) 0x1e, 0x222);

    }

    public SMSbyteacter translate(int jeb) {
        SMSbyteacter retVal = new SMSbyteacter();
        // 8 bytes characters
        // 0x00 to 0x07 are represented as 0x01 to 0x08
        if (jeb >= 0 && jeb <= 7) {
            retVal.byteacter = (byte) (jeb + 1);
            retVal.prefix = 0;
            return retVal;
        }


        // This leaves 0x08 to 0x1F and 0x80 to 0xFF.
        // Of these, the bytes 0x80
        // to 0x87 and 0xA0 to 0xFF are represented as the bytes 0x00 to
        // 0x07
        // (represented by bytes 0x01 to 0x08) and 0x20 to 0x7F, with a
        // prefix of 1 (see below).
        if (jeb >= 0x80 && jeb <= 0x87) {
            retVal.byteacter = (byte) (jeb - 0x80);
            retVal.prefix = 1;
            return retVal;

        }
        if (jeb >= 0xa0 && jeb <= 0xff) {
            retVal.byteacter = (byte) (jeb - 0x80);
            retVal.prefix = 1;
            return retVal;
        }

        // The byteacters 0x08 to 0x1F are represented
        // as the byteacters 0x28 to 0x3F with a prefix of 2 (see below).
        if (jeb >= 0x08 && jeb <= 0x1f) {
            retVal.byteacter = (byte) (jeb + 0x20);
            retVal.prefix = 2;
            return retVal;
        }

        // The
        // byteacters 0x88 to 0x9F are represented as the byteacters 0x48 to
        // 0x5F with a prefix of 2 (see below).

        if (jeb >= 0x88 && jeb <= 0x9f) {
            retVal.byteacter = (byte) (jeb - 0x40);
            retVal.prefix = 2;
            return retVal;
        }

        // In other words, bytes 0x20 to 0x7F are encoded into the same code
        // positions in the 7-bit byteacter set.
        //
        if (jeb >= 0x20 && jeb <= 0x7f) {
            retVal.byteacter = (byte) (jeb);
            retVal.prefix = 0;
            return retVal;
        }

        return retVal;
    }

    public List<Byte> convertThreeBytes(ByteBuffer bb, int index) {
        List<Byte> retVal = new ArrayList<Byte>();
        int j = 1;
        int prefix = 0;
        for (int i = index; (i < index + 3) && (i < bb.capacity()); i++) {
            int inVal = (bb.get(i) & 0xff);
            SMSbyteacter outVal = translate(inVal);
            retVal.add(outVal.byteacter);
            prefix += outVal.prefix * Math.pow(10, (3 - j));
            j++;
        }
        byte escape = map.get(prefix);
        retVal.add(0, (byte) escape);
        // retVal[0] = (byte) escape;
        return retVal;
    }

    public byte[] messageToBuffer(String inbuf) {
        byte[] inValues = inbuf.getBytes();
        return messageToBuffer(inValues);
    }

    public byte[] turnToBuffer(SMSbyteacter[] outbuf) {

        return null;
    }

    public byte[] getAsci(String unicode) {
        return unicode.getBytes();
    }

    public byte[] messageToBuffer(byte[] inValues) {
        int length = inValues.length;
        List<Byte> newbuffer = new ArrayList<Byte>();
        int j = 0;
        int outIndex = 0;
        int inIndex = 0;

        ByteBuffer bb = ByteBuffer.wrap(inValues);

        for (inIndex = 0; inIndex < length; inIndex++) {
            int jeb = (bb.get(inIndex) & 0xff);
            SMSbyteacter temp = translate(jeb);
            if (temp.prefix != 0) {
                List<Byte> out = convertThreeBytes(bb, inIndex);
                if (out == null)
                    continue;
                int k = 0;

                for (j = 0; (j < 4) && (j < out.size()); j++) {
                    newbuffer.add(out.get(j));
                    // retVal[outIndex++] = out[j];
                }
                inIndex = inIndex + 2;
            } else {
                newbuffer.add(temp.byteacter);
                // retVal[outIndex++] = temp.byteacter;
            }
        }
        byte[] retVal = new byte[newbuffer.size()];
        for (int i = 0; i < newbuffer.size(); i++)
            retVal[i] = newbuffer.get(i);

        return retVal;
    }

    byte byteDecode(int jeb, int prefix) {
        byte retVal = -1;

        if (prefix == 0) {
            // 0x00 to 0x07 are represented as 0x01 to 0x08
            if (jeb >= 1 && jeb <= 0x8) {
                retVal = (byte) (jeb - 1);
                return retVal;
            }

            // bytes 0x20 to 0x7F are encoded into the same code positions in
            // the 7-bit character set
            if (jeb >= 0x20 && jeb <= 0x7f) {
                retVal = (byte) jeb;
                return retVal;
            }
        }

        if (prefix == 1) {
            /*
			 * Of these, the bytes 0x80 to 0x87 and 0xA0 to 0xFF are represented
			 * as the bytes 0x00 to 0x07 (represented by characters 0x01 to
			 * 0x08) and 0x20 to 0x7F, with a prefix of 1 (see below)
			 */
            if (jeb >= 0x0 && jeb <= 0x7) {
                retVal = (byte) (jeb + 0x80);
                return retVal;
            }

            if (jeb >= 0x20 && jeb <= 0x7f) {
                retVal = (byte) (jeb + 0x80);
                return retVal;
            }
        }

        if (prefix == 2) {
			/*
			 * The characters 0x08 to 0x1F are represented as the characters
			 * 0x28 to 0x3F with a prefix of 2
			 */
            if (jeb >= 0x28 && jeb <= 0x3f) {
                retVal = (byte) (jeb - 0x20);
                return retVal;
            }

			/*
			 * The characters 0x88 to 0x9F are represented as the characters
			 * 0x48 to 0x5F with a prefix of 2 (see below).
			 */

            if (jeb >= 0x48 && jeb <= 0x5f) {
                retVal = (byte) (jeb + 0x40);
                return retVal;
            }

        }

        return retVal;
    }

    public List<Byte> decodeThreeBytes(byte[] bb, int index, int esc) {
        List<Byte> retVal = new ArrayList<Byte>();
        int prefix = 0;
        int mask = 0xf00;
        int shift = 8;
        index++;

        for (int i = index; (i < index + 3) && (i < bb.length); i++) {
            prefix = (esc & mask);
            mask = mask >> 4;
            prefix = (prefix >> shift);
            shift = shift - 4;
            int inVal = (bb[i] & 0xff);
            byte outVal = byteDecode(inVal, prefix);
            retVal.add(outVal);
        }

        return retVal;
    }

    public byte[] bufferToMessage(byte[] inbuff) {
        List<Byte> newMessage = new ArrayList<Byte>();
        // int outIndex=0;
        int inIndex = 0;

        while (inIndex < inbuff.length) {
            Byte jeb = (byte) (inbuff[inIndex] & 0x0ff);
            Integer esc = decodeMap.get(jeb);
            if (esc != null) {
                List<Byte> out = decodeThreeBytes(inbuff, inIndex, esc.intValue());
                for (int j = 0; (j < 3) && (j < out.size()); j++) {
                    newMessage.add(out.get(j));
                }
                inIndex = inIndex += 4;
            } else {

                newMessage.add(byteDecode(jeb, 0));
                inIndex++;
            }
        }

        byte[] retVal = new byte[newMessage.size()];
        for (int i = 0; i < retVal.length; i++)
            retVal[i] = newMessage.get(i);
        return retVal;
    }

    boolean buffCompare(byte[] buff1, byte[] buff2) {
        if (buff1.length != buff2.length) return false;
        for (int i = 0; i < buff1.length; i++) {
            if (buff1[i] != buff2[i]) {
                System.out.println("Buffer compare fail at:" + i);
                return false;
            }
        }
        return true;
    }

    private class SMSbyteacter {
        public byte byteacter;
        public byte prefix;
    }
}
