package com.blueupbeacons.sdk.ble.frames;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import com.blueupbeacons.sdk.ble.Frame;

/**
 * Created by massimo on 21/06/17.
 */

public final class EddystoneUrl extends Frame {
    private static final String[] URL_SCHEMES = new String[]{
            "http://www.",
            "https://www.",
            "http://",
            "https://"
    };

    private static final String[] URL_EXPANSIONS = new String[]{
            ".com/",
            ".org/",
            ".edu/",
            ".net/",
            ".info/",
            ".biz/",
            ".gov/",
            ".com",
            ".org",
            ".edu",
            ".net",
            ".info",
            ".biz",
            ".gov"
    };

    private int txPower;
    private String url;
    private String hash;

    @Override
    public Technology technology() {
        return Technology.Eddystone;
    }

    @Override
    public Type type() {
        return Type.Url;
    }

    public EddystoneUrl(byte[] data) {
        super(data);
    }

    @Override
    protected void parse(byte[] data) {
        Frame.ByteReader reader = new ByteReader(data, false);

        StringBuilder sb = new StringBuilder();

        // Skip Type 0x10
        reader.skipBytes(1);

        // Get "Calibrated Tx power at 0m"
        txPower = reader.readInt8();

        // Get "Url Scheme Prefix"
        int schemePrefix = reader.readUInt8();
        sb.append(URL_SCHEMES[schemePrefix]);

        // Get "Url"
        byte[] chars = reader.getUnreadBytes();

        for (byte c : chars) {
            if ((c >= 0x20) && (c < 0x7F)) {
                sb.append(new String(new byte[]{c}, StandardCharsets.US_ASCII));
            } else {
                sb.append(URL_EXPANSIONS[c]);
            }
        }

        url = sb.toString();
    }

    @Override
    protected JSONObject jsonData() {
        JSONObject object = new JSONObject();
        try {
            object.put("url", url);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

    @Override
    public String hash() {
        if (hash == null) {
            hash = "url::" + url;
        }

        return hash;
    }


    /**
     * @return Url;
     */
    public String getUrl() {
        return url;
    }
}
