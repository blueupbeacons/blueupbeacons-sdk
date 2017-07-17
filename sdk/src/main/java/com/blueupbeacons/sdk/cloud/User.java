package com.blueupbeacons.sdk.cloud;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by massimo on 12/07/17.
 */

public final class User {
    private long id;
    private String username;
    private String firstName, lastName;


    public User(JSONObject data) {
        try {
            this.id = data.getLong("id");
            this.username = data.getString("username");
            this.firstName = data.getString("firstName");
            this.lastName = data.getString("lastName");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return (firstName + " " + lastName).trim();
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
