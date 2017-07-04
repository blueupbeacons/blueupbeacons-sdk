# blueupbeacons-sdk for Android

Blueup SDK will make you able to develop Android applications that receive BlueUp beacons frames.


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

### `public boolean isScanning()`

 * **Returns:** whether the scanner is running

### `public void start(Mode mode)`

Start the scanner with the specified scan mode. <b>LowPower</b> Perform Bluetooth LE scan in low power mode. This is the default scan mode as it consumes the least power. <b>Balanced</b> Perform Bluetooth LE scan in balanced power mode. Scan results are returned at a rate that provides a good trade-off between scan frequency and power consumption. <b>MaxPerformance</b>Scan using highest duty cycle. It's recommended to only use this mode when the application is running in the foreground.

 * **See also:** Mode

     <p>
 * **Parameters:** `mode` — scan mode

### `public void start()`

Start the scanner with the default scan mode <b>Balanced</b>

 * **See also:** Mode

### `public void stop()`

Stop the scanner

### `public void toggle()`

Start or stop the scanner.



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

## Frames

## EddystoneUID
### `public String getNamespace()`

 * **Returns:** Namespace ID

### `public String getInstance()`

 * **Returns:** Instance ID
 
 
## EddystoneUrl
### `public String getUrl()`

 * **Returns:** Url;
 
 
## EddystoneTlm
### `public int getVersion()`

 * **Returns:** version

### `public boolean isEncrypted()`

 * **Returns:** is content encrypted

### `public int getBatteryVoltage()`

 * **Returns:** battery voltage

### `public double getTemperature()`

 * **Returns:** temperature

### `public long getPackets()`

 * **Returns:** number of packets advertised since boot

### `public long getTimeSincePowerOn()`

 * **Returns:** milliseconds since last reboot

### `public Date getRebootDate()`

 * **Returns:** Date of last reboot
## EddystoneEid
### `public String getId()`

 * **Returns:** Identifier
## IBeacon
### `public String getUuid()`

 * **Returns:** UUID

### `public int getMajor()`

 * **Returns:** Major

### `public int getMinor()`

 * **Returns:** Minor
## Quuppa
### `public boolean hasCustomTag()`

 * **Returns:** Advertise custom tag

### `public String getCustomTag()`

 * **Returns:** Custom tag Value if set, otherwise NULL
## Sensors
### `public double getTemperature()`

 * **Returns:** Temperature

### `public double getHumidity()`

 * **Returns:** Humidity

### `public double getPressure()`

 * **Returns:** Pressure
