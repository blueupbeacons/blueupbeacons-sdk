package com.blueupbeacons.sdk.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.HashMap;
import java.util.regex.Pattern;

import com.blueupbeacons.sdk.ble.frames.EddystoneEid;
import com.blueupbeacons.sdk.ble.frames.EddystoneTlm;
import com.blueupbeacons.sdk.ble.frames.EddystoneUid;
import com.blueupbeacons.sdk.ble.frames.EddystoneUrl;
import com.blueupbeacons.sdk.ble.frames.IBeacon;
import com.blueupbeacons.sdk.ble.frames.Quuppa;
import com.blueupbeacons.sdk.ble.frames.Sensors;

/**
 * Created by massimo on 21/06/17.
 */

public final class Scanner {
    private final static String TAG = Scanner.class.getSimpleName();
    private final static String NAME = "BLUEUP";
    private static final ParcelUuid BATTERY_SERVICE_UUID = ParcelUuid.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private static final ParcelUuid CUSTOM_SERVICE_UUID = ParcelUuid.fromString("00008800-0000-1000-8000-00805f9b34fb");
    private static final ParcelUuid EDDYSTONE_SERVICE_UUID = ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");
    private static final int APPLE_MANUFACTURER_UUID = 0x4C;
    private static final int QUUPPA_MANUFATURER_UUID = 0xC7;
    private static final ParcelUuid ENVIROMENTAL_SENSING = ParcelUuid.fromString("0000181a-0000-1000-8000-00805f9b34fb");
    public static final int UID = 0x00;
    public static final int URL = 0x10;
    public static final int TLM = 0x20;
    public static final int EID = 0x30;


    private final HashMap<String, Beacon> beacons = new HashMap<>();

    private Handler handler;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback bluetoothLeScannerCallback;
    private boolean bluetoothLeScannerRunning = false;

    public interface Handler {
        Integer rssiThreshold();

        void onError(Scanner scanner, int error);

        boolean accept(Beacon beacon);

        void onBeaconDetected(Scanner scanner, Beacon beacon);
    }


    public Scanner(Handler handler) {
        this.handler = handler;
        this.initInternalScanner();
    }

    private void initInternalScanner() {
        bluetoothLeScannerCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {

                /**
                 *  1. Check RSSI ThresHold
                 */
                if (null != handler.rssiThreshold()) {
                    if (result.getRssi() < handler.rssiThreshold()) {
                        return;
                    }
                }

                /**
                 *  2. Detect Beacon
                 */
                final Beacon detectedBeacon = detect(result);
                if (detectedBeacon == null) {
                    return;
                }

                /**
                 *  3. Filter
                 */
                if (handler.accept(detectedBeacon))
                    handler.onBeaconDetected(Scanner.this, detectedBeacon);
            }

            @Override
            public void onScanFailed(int errorCode) {
                handler.onError(Scanner.this, errorCode);
            }
        };
    }

    @Nullable
    private synchronized Beacon detect(ScanResult result) {
        final BluetoothDevice device = result.getDevice();
        final ScanRecord record = result.getScanRecord();

        if (beacons.containsKey(device.getAddress())) {
            return compose(beacons.get(device.getAddress()), result);
        }

        if (device == null || record == null) {
            return null;
        }

        // Check Name
        final String name = record.getDeviceName();
        if (name == null) {
            return null;
        }

        if (!name.toUpperCase().startsWith(NAME)) {
            return null;
        }

        // Get Model and Serial Number
        int devModel;
        int devSerialNumber;
        try {
            String[] deviceComponents = device.getName().split(Pattern.quote("-"));
            devModel = Integer.parseInt(deviceComponents[1]);
            devSerialNumber = Integer.parseInt(deviceComponents[2]);
        } catch (Exception e) {
            return null;
        }

        // Set battery Status
        int battery = -1;
        byte[] data = record.getServiceData(BATTERY_SERVICE_UUID);
        if (data != null) {
            battery = Integer.valueOf(data[0]);
        }

        // Get custom Service data
        byte services = 0;
        byte[] csdata = record.getServiceData(CUSTOM_SERVICE_UUID);
        if (csdata != null && csdata.length > 0) {
            Log.d(TAG, "technologies:" + csdata[0]);
            services = csdata[0];
        }

        Beacon beacon = new Beacon(device.getAddress(), devModel, devSerialNumber, battery, services);
        beacon.setRSSI(result.getRssi());

        return beacons.put(beacon.getAddress(), beacon);
    }

    private Beacon compose(Beacon beacon, ScanResult result) {

        beacon.setRSSI(result.getRssi());

        if (beacon.advertise().eddystone()) {
            byte[] data = result.getScanRecord().getServiceData(EDDYSTONE_SERVICE_UUID);
            if (data != null) {
                switch (data[0]) {
                    case UID:
                        beacon.setFrame(new EddystoneUid(data));
                        break;
                    case URL:
                        beacon.setFrame(new EddystoneUrl(data));
                        break;
                    case TLM:
                        beacon.setFrame(new EddystoneTlm(data));
                        break;
                    case EID:
                        beacon.setFrame(new EddystoneEid(data));
                        break;
                }
            }
        }

        if (beacon.advertise().ibeacon()) {
            byte[] appleManufacturerData = result.getScanRecord().getManufacturerSpecificData(APPLE_MANUFACTURER_UUID);
            if (appleManufacturerData != null) {
                beacon.setFrame(new IBeacon(appleManufacturerData));
            }
        }

        if (beacon.advertise().quuppa()) {
            byte[] quuppaManufacturerData = result.getScanRecord().getManufacturerSpecificData(QUUPPA_MANUFATURER_UUID);

            if (quuppaManufacturerData != null) {
                beacon.setFrame(new Quuppa(quuppaManufacturerData));
            }
        }

        if (beacon.advertise().sensors()) {
            byte[] data = result.getScanRecord().getServiceData(ENVIROMENTAL_SENSING);
            if (data != null) {
                beacon.setFrame(new Sensors(data));
            }
        }

        return beacon;
    }

    public boolean isScanning(){
        return bluetoothLeScannerRunning;
    }

    public void start() {
        if (bluetoothLeScannerRunning) {
            return;
        }
        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (bluetoothLeScanner == null) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }

        bluetoothLeScannerRunning = true;
        bluetoothLeScanner.startScan(bluetoothLeScannerCallback);
    }

    public void stop() {
        if (!bluetoothLeScannerRunning) {
            return;
        }
        if (bluetoothLeScanner == null) {
            return;
        }
        bluetoothLeScanner.stopScan(bluetoothLeScannerCallback);
        bluetoothLeScannerRunning = false;
        beacons.clear();
    }
}
