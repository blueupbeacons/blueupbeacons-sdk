package com.blueupbeacons.sdk.ble.frames;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import com.blueupbeacons.sdk.ble.Frame;

/**
 * Created by massimo on 21/06/17.
 */

public final class EddystoneTlm extends Frame {
    private int version, batteryVoltage;
    private double temperature;
    private long packets, timeSincePowerOn;
    private Date rebootDate;

    @Override
    public Technology technology() {
        return Technology.Eddystone;
    }

    @Override
    public Type type() {
        return Type.Tlm;
    }

    @Override
    public boolean isUpdatable() {
        return true;
    }

    public EddystoneTlm(byte[] data) {
        super(data);
    }

    @Override
    protected void parse(byte[] data) {
        final Frame.ByteReader reader = new ByteReader(data, false);

        // Skip Type 0x20
        reader.skipBytes(1);

        // Get Version
        this.version = reader.readUInt8();

        // Get Battery Voltage
        this.batteryVoltage = reader.readUInt16();

        // Get Temperature
        final int tempFixed = reader.readInt8();
        final int tempFloat = reader.readUInt8();

        this.temperature = Double.valueOf(tempFixed) + Double.valueOf(tempFloat / 256.0);

        // Get sent Packets count
        this.packets = reader.readUInt32();

        // Get Time since Power on
        this.timeSincePowerOn = 100 * reader.readUInt32();

        // Calculate Reboot date
        this.rebootDate = new Date(System.currentTimeMillis() - timeSincePowerOn);
    }

    @Override
    protected JSONObject jsonData() {
        JSONObject object = new JSONObject();
        try {
            object.put("version", version);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

    public int getVersion() {
        return version;
    }

    public int getBatteryVoltage() {
        return batteryVoltage;
    }

    public double getTemperature() {
        return temperature;
    }

    public long getPackets() {
        return packets;
    }

    public long getTimeSincePowerOn() {
        return timeSincePowerOn;
    }

    public Date getRebootDate() {
        return rebootDate;
    }

}
