package com.blueupbeacons.sdk.cloud;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.blueupbeacons.sdk.ble.Beacon;
import com.scottyab.aescrypt.AESCrypt;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import javax.crypto.spec.SecretKeySpec;

/**
 * Created by massimo on 05/07/17.
 */

public class Client {
    private final String TAG = Client.class.getSimpleName();
    private final String USER_AGENT = "Blueup Cloud Android Client";
    private static final String HEADER = "BlueUp-Api";
    private static final String ENDPOINT = "https://api.blueupbeacons.com/v1";
    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    public enum ConnectionState {Idle, Connecting, Connected}

    private final String apiCode;
    private final DataHandler dataHandler;
    private final OkHttpClient httpClient;

    private InitializeCallback initializeCallback;
    private TokenResumeCallback tokenResumeCallback;
    private int keyShift;
    private String tokenId;
    private ConnectionState state = ConnectionState.Idle;

    public interface InitializeCallback {
        void onStart();

        void onComplete(String token, int keyShift);

        void onError(String error, JSONObject data);
    }

    public interface TokenResumeCallback {
        void onComplete(boolean isResumed, Integer keyShift);
    }

    public interface ResultCallback {
        void onResult(Result result);
    }


    public Client(String apiCode, String ApiKey) {
        this.apiCode = apiCode;
        this.httpClient = new OkHttpClient();
        this.httpClient.setReadTimeout(15, TimeUnit.SECONDS);    // socket timeout
        this.dataHandler = new DataHandler(Base64.decode(ApiKey, Base64.NO_WRAP));
    }

    public boolean hasState(ConnectionState state) {
        return state.equals(this.state);
    }

    public void connect(InitializeCallback callback) {
        this.initializeCallback = callback;
        // Update status
        state = ConnectionState.Connecting;
        new Authorization().execute();
    }

    public void connect(String token, int keyShift, TokenResumeCallback callback) {
        this.tokenResumeCallback = callback;
        this.tokenId = token;
        this.keyShift = keyShift;

        new TokenResume(this.tokenId, this.keyShift).execute();
    }

    /**
     *
     * @param callback Async callback Interface @see {@link ResultCallback}
     */
    public void getBeaconList(ResultCallback callback) {
        new Call("sdk_beacon_list", callback).execute();
    }

    /**
     *
     * @param beacons List of Beacons
     * @param callback Async callback Interface @see {@link ResultCallback}
     */
    public void updateBeacons(List<Beacon> beacons, ResultCallback callback) {
        try {
            JSONObject params = new JSONObject();
            JSONArray list = new JSONArray();

            for (Beacon beacon : beacons) {
                list.put(beacon.toJson());
            }

            params.put("beacons", list);

            new Call("sdk_beacon_update", params, callback).execute();

        } catch (JSONException e) {
            callback.onResult(Result.failure(e.getMessage(), e));
        }
    }

    /**
     *
     * @param beacon Beacon
     * @param callback Async callback Interface @see {@link ResultCallback}
     */
    public void updateBeacon(Beacon beacon, ResultCallback callback) {
        try {
            JSONObject params = new JSONObject();
            JSONArray list = new JSONArray();
            list.put(beacon.toJson());


            params.put("beacons", list);

            new Call("sdk_beacon_update", params, callback).execute();

        } catch (JSONException e) {
            callback.onResult(Result.failure(e.getMessage(), e));
        }
    }

    private void on(String method, JSONObject params, ResultCallback callback) {
        new Call(method, params, callback).execute();
    }

    private void on(String method, ResultCallback callback) {
        new Call(method, callback).execute();
    }

    private class DataHandler {
        private final int API_LENGTH = 48;
        private final int KEY_SIZE = 32;
        private final int KEY_IV = 16;

        private byte[] api;
        private byte[] key;
        private byte[] iv;

        public DataHandler(byte[] api) {
            this.api = api;
        }

        public void setShift(int value) {
            int shift = value;
            this.key = new byte[KEY_SIZE];
            for (int i = 0; i < KEY_SIZE; i++) {
                this.key[i] = this.api[shift];
                shift = (shift + 1) % API_LENGTH;
            }

            this.iv = new byte[KEY_IV];
            for (int i = 0; i < KEY_IV; i++) {
                this.iv[i] = this.api[shift];
                shift = (shift + 1) % API_LENGTH;
            }
        }

        public byte[] encrypt(byte[] data) {
            if (key == null || iv == null) setShift(0);
            try {
                SecretKeySpec skeySpec = new SecretKeySpec(this.key, "AES");
                return AESCrypt.encrypt(skeySpec, this.iv, data);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public byte[] decrypt(byte[] data) {
            if (key == null || iv == null) setShift(0);
            try {
                SecretKeySpec skeySpec = new SecretKeySpec(this.key, "AES");
                return AESCrypt.decrypt(skeySpec, this.iv, data);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public String jsonEncrypt(JSONObject object) {
            byte[] data = object.toString().getBytes(StandardCharsets.US_ASCII);

            return Base64.encodeToString(this.encrypt(data), Base64.NO_WRAP);
        }

        public JSONObject jsonDecrypt(String base64String) {
            try {
                byte[] data = this.decrypt(Base64.decode(base64String, Base64.NO_WRAP));
                return new JSONObject(new String(data, StandardCharsets.US_ASCII));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private class Authorization extends AsyncTask<Void, Void, Void> {
        private JSONObject challenge;

        public Authorization() {
            this.challenge = null;
        }

        private Authorization(long authId, String challenge) {
            this.challenge = new JSONObject();
            try {
                this.challenge.put("id", authId);
                this.challenge.put("challenge", challenge);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            Request.Builder requestBuilder = new Request.Builder()
                    .header(HEADER, apiCode)
                    .header("User-Agent", USER_AGENT)
                    .url(ENDPOINT + "/auth");

            Request request;

            dataHandler.setShift(0);
            if (challenge != null) {
                String authData = Base64.encodeToString(dataHandler.encrypt(challenge.toString().getBytes(StandardCharsets.US_ASCII)), Base64.NO_WRAP);

                requestBuilder.post(RequestBody.create(MEDIA_TYPE_JSON, authData));
                request = requestBuilder.build();
                httpClient.newCall(request).enqueue(new Callback() {
                    private final Handler handler = new Handler(Looper.getMainLooper());

                    @Override
                    public void onFailure(Request request, final IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                initializeCallback.onError(e.getMessage(), new JSONObject());
                            }
                        });
                    }

                    @Override
                    public void onResponse(final Response response) throws IOException {
                        final String body = response.body().string();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Result r = new Result(new JSONObject(body));
                                    if (r.isFailed()) {
                                        initializeCallback.onError(r.getError(), (JSONObject) (r.getData()));
                                    } else {
                                        JSONObject authInfo = dataHandler.jsonDecrypt((String) r.getData());

                                        // Save keyShift
                                        keyShift = authInfo.getInt("shift");
                                        dataHandler.setShift(keyShift);

                                        // Save token
                                        tokenId = authInfo.getString("token");

                                        // Update status
                                        state = ConnectionState.Connected;

                                        // Complete
                                        initializeCallback.onComplete(tokenId, keyShift);
                                    }
                                } catch (JSONException jsonError) {
                                    initializeCallback.onError(jsonError.getMessage(), null);
                                }
                            }
                        });
                    }
                });
            } else {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        initializeCallback.onStart();
                    }
                });

                request = requestBuilder.build();
                httpClient.newCall(request).enqueue(new Callback() {
                    private final Handler handler = new Handler(Looper.getMainLooper());

                    @Override
                    public void onFailure(Request request, final IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                initializeCallback.onError(e.getMessage(), null);
                            }
                        });
                    }

                    @Override
                    public void onResponse(final Response response) throws IOException {
                        final String body = response.body().string();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Result r = new Result(new JSONObject(body));
                                    if (r.isFailed()) {
                                        initializeCallback.onError(r.getError(), (JSONObject) (r.getData()));
                                    } else {
                                        JSONObject authData = dataHandler.jsonDecrypt((String) r.getData());
                                        long authId = authData.getLong("id");
                                        int shift = authData.getInt("shift");
                                        String challenge = authData.getString("challenge");

                                        // Apply Key Shift
                                        dataHandler.setShift(shift);

                                        // Calculate challenge
                                        byte[] chData = dataHandler.encrypt(Base64.decode(challenge, Base64.NO_WRAP));
                                        challenge = Base64.encodeToString(chData, Base64.NO_WRAP);


                                        new Authorization(authId, challenge).execute();
                                    }
                                } catch (JSONException jsonError) {
                                    initializeCallback.onError(jsonError.getMessage(), null);
                                }
                            }
                        });
                    }
                });
            }


            return null;
        }
    }

    private class TokenResume extends AsyncTask<Void, Void, Void> {
        private final int originalKeyShift;
        private JSONObject data;

        public TokenResume(String token, int keyShift) {

            originalKeyShift = keyShift;

            try {
                data = new JSONObject();
                data.put("token", token);
                data.put("shift", keyShift);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        protected Void doInBackground(Void... params) {
            Request.Builder requestBuilder = new Request.Builder()
                    .header(HEADER, apiCode)
                    .header("User-Agent", USER_AGENT)
                    .url(ENDPOINT + "/auth");

            dataHandler.setShift(0);
            String authData = Base64.encodeToString(dataHandler.encrypt(data.toString().getBytes(StandardCharsets.US_ASCII)), Base64.NO_WRAP);
            dataHandler.setShift(originalKeyShift);

            requestBuilder.post(RequestBody.create(MEDIA_TYPE_JSON, authData));
            Request request = requestBuilder.build();
            httpClient.newCall(request).enqueue(new Callback() {
                private final Handler handler = new Handler(Looper.getMainLooper());

                @Override
                public void onFailure(Request request, final IOException e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG, "[TokenResume]", e);
                            tokenResumeCallback.onComplete(false, null);
                        }
                    });
                }

                @Override
                public void onResponse(final Response response) throws IOException {
                    final String body = response.body().string();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Result r = new Result(new JSONObject(body));
                                if (r.isFailed()) {
                                    tokenResumeCallback.onComplete(false, null);
                                } else {
                                    JSONObject authInfo = dataHandler.jsonDecrypt((String) r.getData());
                                    if (authInfo.getBoolean("valid")) {

                                        // Save keyShift
                                        keyShift = authInfo.getInt("shift");
                                        dataHandler.setShift(keyShift);

                                        tokenResumeCallback.onComplete(true, Integer.valueOf(keyShift));
                                    } else {
                                        tokenResumeCallback.onComplete(false, null);
                                    }
                                }
                            } catch (JSONException jsonError) {
                                Log.e(TAG, "[TokenResume]", jsonError);
                                tokenResumeCallback.onComplete(false, null);
                            }
                        }
                    });
                }
            });

            return null;
        }
    }

    private class Call extends AsyncTask<Void, Void, Void> {
        private String method;
        private JSONObject params;
        private ResultCallback callback;

        public Call(String method, ResultCallback callback) {
            this.method = method;
            this.callback = callback;
        }

        public Call(String method, JSONObject params, ResultCallback callback) {
            this.method = method;
            this.params = params;
            this.callback = callback;
        }

        @Override
        protected Void doInBackground(Void... params) {
            String mMethod = method.toLowerCase();
            Request.Builder requestBuilder = new Request.Builder()
                    .header(HEADER, tokenId)
                    .header("User-Agent", USER_AGENT)
                    .url(ENDPOINT + (mMethod.startsWith("/") ? mMethod : "/" + mMethod));

            if (this.params != null) {
                String postData = Base64.encodeToString(dataHandler.encrypt(this.params.toString().getBytes(StandardCharsets.US_ASCII)), Base64.NO_WRAP);
                requestBuilder.post(RequestBody.create(MEDIA_TYPE_JSON, postData));
            }

            Request request = requestBuilder.build();
            httpClient.newCall(request).enqueue(new Callback() {
                private final Handler handler = new Handler(Looper.getMainLooper());

                @Override
                public void onFailure(Request request, final IOException e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onResult(Result.failure(e.getMessage(), e));
                        }
                    });
                }

                @Override
                public void onResponse(final Response response) throws IOException {
                    final String body = response.body().string();
                    try {
                        final Result r = new Result(new JSONObject(body));
                        if (r.isFailed()) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onResult(r);
                                }
                            });

                        } else {
                            final JSONObject data = dataHandler.jsonDecrypt((String) r.getData());

                            if (data.has("shift")) {
                                dataHandler.setShift(data.getInt("shift"));
                            }

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        callback.onResult(new Result(data.getJSONObject("result")));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });


                        }
                    } catch (final JSONException jsonError) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onResult(Result.failure(jsonError.getMessage(), jsonError));
                            }
                        });
                    }


                }
            });
            return null;
        }
    }

}
