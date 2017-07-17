package com.blueupbeacons.sdk.ble;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 *  Frame
 */
public abstract class Frame {
    public enum Technology {
        Eddystone, iBeacon, Quuppa, Sensors
    }

    public enum Type {
        Uid, Url, Eid, Tlm, iBeacon, Quuppa, Sensors
    }

    public final class ByteReader {

        private boolean isLittleEndian;
        private byte[] data;
        private int length;
        private int cursor = 0;

        public ByteReader(byte[] data, boolean isLittleEndian) {
            this.isLittleEndian = isLittleEndian;
            this.data = data;
            this.length = data.length;
        }

        public ByteReader(byte[] data) {
            this.isLittleEndian = true;
            this.data = data;
        }


        private byte[] getBuffer(int length) {
            byte[] b = new byte[length];

            for (int i = 0; i < length; i++) {
                b[i] = data[cursor++];
            }

            return b;
        }

    /*
        public Methods
     */

        public void skipBytes(int amount) {
            cursor += amount;
        }

        public byte[] getUnreadBytes() {

            if (cursor == 0) {
                return data;
            }


            final int bufferLength = this.length - cursor;
            byte[] buffer = new byte[bufferLength];
            for (int i = 0; i < bufferLength; i++) {
                buffer[i] = data[i + cursor];
            }

            return buffer;
        }

        public String readHexString(int length) {
            char[] hexArray = "0123456789ABCDEF".toCharArray();
            byte[] buffer = this.getBuffer(length);
            char[] hexChars = new char[buffer.length * 2];

            for (int i = 0; i < buffer.length; i++) {
                int v = buffer[i] & 0xFF;
                hexChars[i * 2] = hexArray[v >>> 4];
                hexChars[i * 2 + 1] = hexArray[v & 0x0F];
            }

            return new String(hexChars);
        }

        public byte readByte() {
            return data[cursor++];
        }

        public byte[] readBytes(int amount) {
            byte[] bytes = new byte[amount];

            System.arraycopy(data, cursor, bytes, 0, amount);

            cursor += amount;

            return bytes;
        }

        public int readInt8() {
            return (int) (data[cursor++]);
        }

        public int readUInt8() {
            return ((int) (data[cursor++])) & 0xFF;
        }

        public short readInt16() {
            final ByteOrder bo = isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
            return ByteBuffer.wrap(this.getBuffer(2)).order(bo).getShort();
        }

        public int readUInt16() {
            return readInt16() & 0xFFFF;
        }

        public int readInt32() {
            final ByteOrder bo = isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
            return ByteBuffer.wrap(this.getBuffer(4)).order(bo).getInt();
        }

        public long readUInt32() {
            return readInt32() & 0xFFFFFFFF;
        }

    }

    public abstract Technology technology();

    public abstract Type type();

    public String hash(){
        return type().toString();
    }

    public boolean isUpdatable() {
        return false;
    }

    public Frame(byte[] data) {
        parse(data);
    }

    protected abstract void parse(byte[] data);

    protected abstract JSONObject jsonData();

    public final JSONObject toJson() {
        JSONObject object = new JSONObject();

        try {
            object.put("type", this.type().toString());

            JSONObject data = this.jsonData();
            if (data != null) {
                object.put("data", data);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return object;
    }

    @Override
    public final String toString() {
        return this.toJson().toString();
    }
}
