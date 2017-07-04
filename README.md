# blueupbeacons-sdk for Android

Blueup SDK will make you able to develop Android application that interate with blueup beacons.


## Getting Started

Reference in your Gradle file the library

```
compile 'com.blueupbeacons.sdk:sdk:1.0.0'
```


## Objects

## Beacon

### `public int getRssi()`

Get Current RSSI value

 * **Returns:** current rssi measured value

### `public int getBattery()`

 * **Returns:** battery level (0-100)

### `public int getModel()`

 * **Returns:** model

### `public String getModel(int padding)`

 * **Parameters:** `padding` — padding length
 * **Returns:** model

### `public int getSerial()`

 * **Returns:** serial number

### `public String getSerial(int padding)`

 * **Parameters:** `padding` — padding length
 * **Returns:** 

### `public String getName()`

Get Beacon fullname (BLUEUP-xx-yyyyy)

 * **Returns:** 

### `public String getAddress()`

 * **Returns:** Device MAC Address

### `public AdvertisingFrames advertise()`

 * **Returns:** Advertise flags

### `public JSONObject toJson()`

 * **Returns:** Beacon json serialization


## Scanner Handler Interface

### `Integer rssiThreshold()`

Threshold limit. return <b>null</b> to accept all beacons return a valid value (e.g. -50 to -10) to filter beacons within a RSSI value

 * **Returns:** Threshold limit

### `void onError(Scanner scanner, int error)`

Notify an error during scan

 * **Parameters:**
   * `scanner` — scanner instance
   * `error` — error code

### `void onStart(Scanner scanner)`

Callback when the scanner has been started

 * **Parameters:** `scanner` — scanner instance

### `void onStop(Scanner scanner)`

Callback when the scanner has been stopped

 * **Parameters:** `scanner` — scanner instance

### `boolean accept(Beacon beacon)`

Filter to accept/refuse a Beacon found

 * **Parameters:** `beacon` — found beacon

     <p>
 * **Returns:** true when the found beacon have to be notified

### `void onBeaconDetected(Scanner scanner, Beacon beacon)`

 * **Parameters:**
   * `scanner` — scanner instance
   * `beacon` — Beacon found

## Scanner

The scanner



### Usage
```java
Scanner scanner = new Scanner(new Scanner.Handler() {
                        private final String TAG = "Scanner.Handler";

                        @Override
                        public Integer rssiThreshold() {
                            
                            return null;
                        }

                        @Override
                        public void onError(Scanner scanner, int error) {
                            Log.d(TAG, "onError: " + String.valueOf(error));
                        }

                        @Override
                        public void onStart(Scanner scanner) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    layoutBeaconInformations.setVisibility(View.GONE);
                                    pbScanning.setVisibility(View.VISIBLE);
                                    btActivity.setText("Stop Scan");
                                }
                            });
                        }

                        @Override
                        public void onStop(Scanner scanner) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pbScanning.setVisibility(View.GONE);
                                    btActivity.setText("Start Scan");
                                }
                            });
                        }

                        @Override
                        public boolean accept(Beacon beacon) {
                            return true;
                        }

                        @Override
                        public void onBeaconDetected(Scanner scanner, final Beacon beacon) {
                            Log.d(TAG, beacon.toString());
                            txtBeaconInfo.post(new Runnable() {
                                @Override
                                public void run() {
                                    pbScanning.setVisibility(View.GONE);
                                    layoutBeaconInformations.setVisibility(View.VISIBLE);
                                    txtBeaconSerial.setText(beacon.getName());
                                    txtBeaconSerial.setVisibility(View.VISIBLE);
                                    txtBeaconInfo.setText(beacon.toString());
                                    txtBeaconInfo.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    });
```

