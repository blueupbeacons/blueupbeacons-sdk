package com.blueupbeacons.sdk.ble.frames;

import org.json.JSONException;
import org.json.JSONObject;

import com.blueupbeacons.sdk.ble.Frame;

/**
 * Created by massimo on 21/06/17.
 */

public final class Quuppa extends Frame {
    final private static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private final static int TAG_TYPE_USER_INPUT = 0x10;
    private final static int TAG_LENGTH = 6;

    private boolean hasCustomTag;
    private String customTagValue;
    private String hash;

    @Override
    public Technology technology() {
        return Technology.Quuppa;
    }

    @Override
    public Type type() {
        return Type.Quuppa;
    }

    public Quuppa(byte[] data) {
        super(data);
    }

    @Override
    protected void parse(byte[] data) {
        Frame.ByteReader reader = new ByteReader(data, true);

        // Skip Packet ID and Device Type
        reader.skipBytes(2);

        this.hasCustomTag = (reader.readUInt8() & TAG_TYPE_USER_INPUT) == TAG_TYPE_USER_INPUT;
        if (this.hasCustomTag) {
            byte[] bytes = reader.readBytes(TAG_LENGTH);
            char[] hexChars = new char[bytes.length * 2];

            for (int i = 0; i < bytes.length; i++) {
                int v = bytes[i] & 0xFF;
                hexChars[i * 2] = hexArray[v >>> 4];
                hexChars[i * 2 + 1] = hexArray[v & 0x0F];
            }

            this.customTagValue = new String(hexChars);

        } else {
            this.customTagValue = null;
        }

    }

    @Override
    protected JSONObject jsonData() {
        JSONObject object = new JSONObject();
        try {
            object.put("hasCustomTag", hasCustomTag);
            if (hasCustomTag) {
                object.put("tag", customTagValue);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return object;
    }

    @Override
    public String hash() {
        if (hash == null) {
            hash = "quuppa::" + (hasCustomTag ? customTagValue : "default");
        }
        return hash;
    }

    /**
     * @return Advertise custom tag
     */
    public boolean hasCustomTag() {
        return this.hasCustomTag;
    }

    /**
     * @return Custom tag Value if set, otherwise NULL
     */
    public String getCustomTag() {
        return this.customTagValue;
    }
}
