package it.blueupbeacons.blueupclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.regex.Pattern;

/**
 * Created by massimo on 14/06/17.
 */

public final class Detector {
    private final static String NAME = "BLUEUP";
    private final static String TAG = Detector.class.getSimpleName();

    private static final ParcelUuid BATTERY_SERVICE_UUID = ParcelUuid.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private Listener mListener;
    private boolean mScanning;
    private BluetoothAdapter mAdapter;
    private BluetoothLeScanner mScanner;
    private ScanCallback mScanCallback;



    public interface Listener {
        Integer rssiThreshold();

        boolean onBeaconDetected(int model, int serial, String macAddress, int rssiCalibratedPower, int batteryLevel);
    }


    private Detector() {
        mScanning = false;
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        if (Build.VERSION.SDK_INT >= 21) {
            mScanner = mAdapter.getBluetoothLeScanner();
            setupScanCallbackV21();
        } else {
            Log.e(TAG, "Android version not supported");
        }
    }

    @RequiresApi(21)
    private void setupScanCallbackV21() {
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {

                /**
                 *  1. Check RSSI ThresHold
                 */
                if (null != mListener.rssiThreshold()) {
                    if (result.getRssi() < mListener.rssiThreshold()) {
                        return;
                    }
                }

                final ScanRecord record = result.getScanRecord();

                /**
                 *  2. Get data from Service
                 */
                byte[] serviceData = record.getServiceData(BATTERY_SERVICE_UUID);
                if (serviceData == null) {
                    return;
                }

                // Check Name
                final String name = record.getDeviceName();
                if (name == null) {
                    return;
                }

                if (!name.toUpperCase().startsWith(NAME)) {
                    return;
                }


                // Get Model and Serial Number
                int model;
                int serial;
                try {
                    String[] deviceComponents = name.split(Pattern.quote("-"));
                    model = Integer.parseInt(deviceComponents[1]);
                    serial = Integer.parseInt(deviceComponents[2]);
                } catch (Exception e) {
                    Log.e("BLUEUP", "Recognize: ", e);
                    return;
                }

                // Mac Address
                String macAddress = result.getDevice().getAddress();

                // Rssi
                int calibratedRssi = result.getRssi();

                // Livello batteria
                int batteryLevel = serviceData[0];

                // Stop Scan
                stop();

                /*
                   Notify the detected beacon data.
                   The callback must return:
                   TRUE if the scanning has to be resumed,
                   FALSE if the beacon has been validated and the scan as to be terminated
                 */
                if (mListener.onBeaconDetected(model, serial, macAddress, calibratedRssi, batteryLevel)) {
                    start();
                }
            }
        };
    }

    public static Detector create(Listener listener) {
        Detector beaconDetector = new Detector();
        beaconDetector.mListener = listener;

        return beaconDetector;
    }

    public boolean isScanning() {
        return mScanning;
    }

    public void start() {
        if (mScanning) {
            return;
        }

        mScanning = true;
        if (Build.VERSION.SDK_INT >= 21) {
            mScanner.startScan(mScanCallback);
        } else {
            Log.e(TAG, "Android version not supported");
        }
    }

    public void stop() {
        if (!mScanning) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            mScanner.stopScan(mScanCallback);
        } else {
            Log.e(TAG, "Android version not supported");
        }
        mScanning = false;
    }

}
