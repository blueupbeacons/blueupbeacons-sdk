# blueupbeacons-sdk for Android

Blueup SDK will make you able to develop Android application that interate with blueup beacons.


## Getting Started

Reference in your Gradle file the library

```
compile 'com.blueupbeacons.sdk:sdk:1.0.0'
```


## Objects

### Scanner

The scanner

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

