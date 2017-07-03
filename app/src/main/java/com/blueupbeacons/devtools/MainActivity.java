package com.blueupbeacons.devtools;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.blueupbeacons.sdk.ble.*;

public class MainActivity extends AppCompatActivity {
    private final static int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private final static int REQUEST_ENABLE_BT = 1;

    private Handler mHandler;
    private BluetoothAdapter bluetoothAdapter;

    private Button btActivity;
    private View layoutBeaconInformations;
    private TextView txtBeaconSerial, txtBeaconInfo;
    private ProgressBar pbScanning;
    private Scanner scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();

        pbScanning = (ProgressBar) findViewById(R.id.pbScanning);
        layoutBeaconInformations = findViewById(R.id.layoutReport);
        txtBeaconSerial = (TextView) findViewById(R.id.txtBeaconSerial);
        txtBeaconInfo = (TextView) findViewById(R.id.txtBeaconInfo);

        btActivity = (Button) (findViewById(R.id.btScanner));
        btActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (scanner == null) {
                    scanner = new Scanner(new Scanner.Handler() {
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

                    // Start Scanner
                    scanner.start(Scanner.Mode.MaxPerformance);
                } else {

                    // Start/Stop
                    scanner.toggle();
                }
            }
        });


        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        requestPermissions();
                    }
                });
                builder.show();
            }
        }

        // Check Bluetooth
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (this.bluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "Bluetooth device not found", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            btActivity.setEnabled(true);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermissions() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
            btActivity.setEnabled(true);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        if (scanner != null) {
            if (scanner.isScanning()) {
                scanner.stop();
            }
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (scanner != null) {
            if (scanner.isScanning()) {
                scanner.stop();
            }
        }
        super.onDestroy();
    }
}
