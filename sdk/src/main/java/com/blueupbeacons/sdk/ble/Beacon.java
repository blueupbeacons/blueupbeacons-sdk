package com.blueupbeacons.sdk.ble;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.util.HashMap;
import java.util.Map;

/**
 * Created by massimo on 16/06/17.
 */

public final class Beacon {

    public class AdvertisingFrames {
        final static byte ADV_FRAME_FLAG_EDDYSTONE = 0x01;
        final static byte ADV_FRAME_FLAG_IBEACON = 0x02;
        final static byte ADV_FRAME_FLAG_QUUPPA = 0x04;
        final static byte ADV_FRAME_FLAG_SENSOR = 0x08;

        private boolean adv_eddystone, adv_ibeacon, adv_quuppa, adv_sensors;

        public AdvertisingFrames(byte value) {
            adv_eddystone = (ADV_FRAME_FLAG_EDDYSTONE == (value & ADV_FRAME_FLAG_EDDYSTONE));
            adv_ibeacon = (ADV_FRAME_FLAG_IBEACON == (value & ADV_FRAME_FLAG_IBEACON));
            adv_quuppa = (ADV_FRAME_FLAG_QUUPPA == (value & ADV_FRAME_FLAG_QUUPPA));
            adv_sensors = (ADV_FRAME_FLAG_SENSOR == (value & ADV_FRAME_FLAG_SENSOR));
        }

        public boolean eddystone() {

            return adv_eddystone;
        }

        public boolean ibeacon() {
            return adv_ibeacon;
        }

        public boolean quuppa() {
            return adv_quuppa;
        }

        public boolean sensors() {
            return adv_sensors;
        }

        public JSONObject toJson() {
            JSONObject object = new JSONObject();
            try {
                object.put("eddystone", adv_eddystone);
                object.put("ibeacon", adv_ibeacon);
                object.put("quuppa", adv_quuppa);
                object.put("sensors", adv_sensors);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return object;
        }

    }

    private String address;
    private int rssi;
    private int model, serial;
    private int battery;
    private byte technologies;
    private AdvertisingFrames advFrames;

    private final HashMap<String, Frame> map = new HashMap<>();

    public Beacon(String address, int model, int serial, int battery, byte services) {
        this.address = address;
        this.model = model;
        this.serial = serial;
        this.battery = battery;
        this.technologies = services;
        advFrames = new AdvertisingFrames(services);
    }

    /**
     * Get Current RSSI value
     *
     * @return current rssi measured value
     */
    public int getRssi() {
        return rssi;
    }

    public int getBattery() {
        return battery;
    }

    public int getModel() {
        return model;
    }

    public String getModel(int padding) {
        String value = String.valueOf(model);
        padding = padding - value.length();
        if (padding <= 0) {
            return value;
        }

        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < padding; i++) {
            buffer.append("0");
        }
        buffer.append(value);

        return buffer.toString();
    }

    public int getSerial() {
        return serial;
    }

    public String getSerial(int padding) {
        String value = String.valueOf(serial);
        padding = padding - value.length();
        if (padding <= 0) {
            return value;
        }

        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < padding; i++) {
            buffer.append("0");
        }
        buffer.append(value);

        return buffer.toString();
    }

    /**
     * Get Beacon fullname (BLUEUP-xx-yyyyy)
     * @return
     */
    public String getName() {
        return String.format("BLUEUP-%s-%s", getModel(2), getSerial(5));
    }

    public String getAddress() {
        return address;
    }

    public AdvertisingFrames advertise() {
        return advFrames;
    }

    public void setRSSI(int value) {
        this.rssi = value;
    }

    public void setFrame(Frame frame) {
        if (frame.isUpdatable()) {
            map.put(frame.hash(), frame);
        } else {
            if (!map.containsKey(frame.hash())) {
                map.put(frame.hash(), frame);
            }
        }
    }




    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        try {
            object.put("rssi", rssi);
            object.put("address", address);
            object.put("model", model);
            object.put("serial", serial);
            object.put("battery", battery);
            object.put("techFlag", technologies);
            object.put("technologies", advFrames.toJson());

            JSONArray frames = new JSONArray();
            for (Map.Entry<String, Frame> entry : map.entrySet()) {
                frames.put(entry.getValue().toJson());
            }
            object.put("frames", frames);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return object;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
