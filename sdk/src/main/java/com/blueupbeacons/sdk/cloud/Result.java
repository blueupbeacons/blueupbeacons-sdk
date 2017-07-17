package com.blueupbeacons.sdk.cloud;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by massimo on 06/07/17.
 */

public final class Result {
    private boolean status;
    private Object data;
    private String error;

    public static Result failure(String error, Object data) {
        return new Result(false, data, error);
    }

    public static Result failure(String error) {
        return new Result(false, null, error);
    }

    public static Result success(Object data) {
        return new Result(true, data, null);
    }

    public Result(JSONObject object) {
        try {
            this.status = object.getBoolean("status");
            this.data = object.get("data");
            this.error = object.getString("error");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Result(boolean status, Object data, String error) {
        this.status = status;
        this.data = data;
        this.error = error;
    }

    public boolean isPassed() {
        return status;
    }

    public boolean isFailed() {
        return !status;
    }

    public Object getData() throws NullPointerException {
        return data;
    }

    public String getError() throws NullPointerException {
        return error;
    }

    @Override
    public String toString() {
        try {
            JSONObject o = new JSONObject();
            o.put("status", status);
            o.put("data", data);
            o.put("error", error);

            return o.toString();
        } catch (JSONException e) {
            return super.toString();
        }

    }
}
