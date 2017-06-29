package com.blueupbeacons.sdk.ble.frames;

import org.json.JSONException;
import org.json.JSONObject;

import com.blueupbeacons.sdk.ble.Frame;

/**
 * Created by massimo on 22/06/17.
 */

public final class EddystoneEid extends Frame {
    private static final int ID_LEN = 8;
    private int txPower;
    private String id;
    private String hash;

    @Override
    public Technology technology() {
        return Technology.Eddystone;
    }

    @Override
    public Type type() {
        return Type.Eid;
    }

    public EddystoneEid(byte[] data) {
        super(data);
    }

    @Override
    protected void parse(byte[] data) {
        Frame.ByteReader reader = new ByteReader(data, false);

        // Skip Type 0x30
        reader.skipBytes(1);


        // Get "Calibrated Tx power at 0m"
        txPower = reader.readInt8();

        // Get "Namespace"
        id = reader.readHexString(ID_LEN);
    }

    @Override
    protected JSONObject jsonData() {
        JSONObject object = new JSONObject();
        try {
            object.put("id", id);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return object;
    }

    @Override
    public String hash() {
        if (hash == null) {
            hash = "eid::" + id;
        }
        return hash;
    }

    public int getTxPower() {
        return txPower;
    }

    public String getId() {
        return id;
    }
}
