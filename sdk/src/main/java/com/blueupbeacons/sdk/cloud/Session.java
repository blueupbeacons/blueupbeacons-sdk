package com.blueupbeacons.sdk.cloud;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by massimo on 12/07/17.
 */

public final class Session {
    private Client client;
    private String token;
    private User user;

    public Session(Client client, JSONObject data) {
        try {
            this.token = data.getString("token");
            this.user = new User(data.getJSONObject("user"));
        } catch (JSONException e) {
            e.printStackTrace();

        }
    }

    public User getUser() {
        return this.user;
    }

    public void on(String method, JSONObject params, Client.ResultCallback callback) {

    }



}
