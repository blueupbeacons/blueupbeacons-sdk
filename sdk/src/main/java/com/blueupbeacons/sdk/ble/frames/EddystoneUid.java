package com.blueupbeacons.sdk.ble.frames;

import org.json.JSONException;
import org.json.JSONObject;

import com.blueupbeacons.sdk.ble.Frame;

public final class EddystoneUid extends Frame {
    private static final int NAMESPACE_LEN = 10;
    private static final int INSTANCE_LEN = 6;

    private int txPower;
    private String namespace, instance;
    private String hash;

    @Override
    public Technology technology() {
        return Technology.Eddystone;
    }

    @Override
    public Type type() {
        return Type.Uid;
    }

    public EddystoneUid(byte[] data) {
        super(data);
    }

    @Override
    protected void parse(byte[] data) {
        final Frame.ByteReader reader = new ByteReader(data, false);

        // Skip Type 0x00
        reader.skipBytes(1);

        // Get "Calibrated Tx power at 0m"
        txPower = reader.readInt8();

        // Get "Namespace"
        namespace = reader.readHexString(NAMESPACE_LEN);

        // Get "Instance ID"
        instance = reader.readHexString(INSTANCE_LEN);
    }

    @Override
    protected JSONObject jsonData() {
        JSONObject object = new JSONObject();
        try {
            object.put("namespace", namespace);
            object.put("instance", instance);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return object;
    }

    @Override
    public String hash() {
        if (hash == null) {
            hash = "uid::" + namespace + "/" + instance;
        }

        return hash;
    }

    /**
     *
     * @return Namespace ID
     */
    public String getNamespace(){
        return namespace;
    }

    /**
     *
     * @return Instance ID
     */
    public String getInstance(){
        return instance;
    }
}
