package com.blueupbeacons.sdk.ble.frames;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import com.blueupbeacons.sdk.ble.Frame;

/**
 * Created by massimo on 21/06/17.
 */

public final class IBeacon extends Frame {
    private String uuid;
    private int major, minor, txPower;
    private String hash = null;

    @Override
    public Technology technology() {
        return Technology.iBeacon;
    }

    @Override
    public Type type() {
        return Type.iBeacon;
    }

    public IBeacon(byte[] data) {
        super(data);
    }

    @Override
    protected void parse(byte[] data) {
        Frame.ByteReader reader = new ByteReader(data, false);
        // Skip manufacturer constant preamble;
        reader.skipBytes(2);

        // Set UUID
        ArrayList<String> uuidParts = new ArrayList<>();
        uuidParts.add(reader.readHexString(4));
        uuidParts.add(reader.readHexString(2));
        uuidParts.add(reader.readHexString(2));
        uuidParts.add(reader.readHexString(8));

        this.uuid = TextUtils.join("-", uuidParts);

        // get Major
        this.major = reader.readUInt16();

        // get Minor
        this.minor = reader.readUInt16();

        this.txPower = reader.readInt8();
    }

    @Override
    protected JSONObject jsonData() {
        JSONObject object = new JSONObject();
        try {
            object.put("uuid", uuid);
            object.put("major", major);
            object.put("minor", minor);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

    @Override
    public String hash() {
        if (hash == null) {
            hash = "ibeacon::" + uuid + "/" + String.valueOf(major) + "/" + String.valueOf(minor);
        }
        return hash;
    }

    public String getUuid() {
        return uuid;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }
}
