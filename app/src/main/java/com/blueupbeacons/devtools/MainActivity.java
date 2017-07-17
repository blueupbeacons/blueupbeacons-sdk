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
import com.blueupbeacons.sdk.cloud.Client;
import com.blueupbeacons.sdk.cloud.Result;
import com.blueupbeacons.sdk.cloud.Session;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private final static int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private final static int REQUEST_ENABLE_BT = 1;
    final String CLIENT_API_CODE = "FK1gcjLnQr8VPNbQfMhW61P8VlVtKc+R7jC+c3voGwOcvoVwk9P8z+PDpCkTPJ4Z";
    final String CLIENT_API_KEY = "ziAbvKkDChfK/069yOWn+2Q2/25R2mH+fKVBfPeIjG9jNMX6brp07l4tgYou9R1y";

    private Handler mHandler;
    private BluetoothAdapter bluetoothAdapter;

    private Button btActivity, btCloud;
    private View layoutBeaconInformations;
    private TextView txtBeaconSerial, txtBeaconInfo;
    private ProgressBar pbScanning;
    private Scanner scanner;
    final Client client = new Client(CLIENT_API_CODE, CLIENT_API_KEY);

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
                            return -50;
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
                            return beacon.matches(1, 13494);
                        }

                        @Override
                        public void onBeaconDetected(final Scanner scanner, final Beacon beacon) {
                            Log.d(TAG, beacon.toString());


                            txtBeaconInfo.post(new Runnable() {
                                @Override
                                public void run() {

                                    // Stop the scanner
                                    scanner.stop();

                                    // Display the beacon
                                    pbScanning.setVisibility(View.GONE);
                                    layoutBeaconInformations.setVisibility(View.VISIBLE);
                                    txtBeaconSerial.setText(beacon.getName());
                                    txtBeaconSerial.setVisibility(View.VISIBLE);
                                    txtBeaconInfo.setText(beacon.toString());
                                    txtBeaconInfo.setVisibility(View.VISIBLE);


                                    // Connect to cloud
                                    client.connect(new Client.InitializeCallback() {
                                        @Override
                                        public void onStart() {
                                            txtBeaconInfo.append("\n\nCONNECT TO CLOUD\n");
                                        }

                                        @Override
                                        public void onComplete(String token, int keyShift) {
                                            txtBeaconInfo.append(String.format("Token: %s\n", token));
                                            txtBeaconInfo.append(String.format("Shift: %d\n", keyShift));

                                            txtBeaconInfo.append("\n\nUPLOAD BEACON DATA\n");
                                            client.updateBeacon(beacon, new Client.ResultCallback() {
                                                @Override
                                                public void onResult(Result result) {
                                                    if (result.isFailed()) {
                                                        txtBeaconInfo.append("[ERROR] " + result.getError());
                                                    } else {
                                                        txtBeaconInfo.append("[OK]\n" + result.toString());
                                                    }
                                                }
                                            });
                                        }

                                        @Override
                                        public void onError(String error, JSONObject data) {
                                            txtBeaconInfo.append("[ERROR] " + error);
                                        }
                                    });

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
        btCloud = (Button) (findViewById(R.id.btCloud));
        btCloud.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Init
                if (client.hasState(Client.ConnectionState.Idle)) {
                    txtBeaconInfo.setText("");

                    client.connect(new Client.InitializeCallback() {
                        @Override
                        public void onStart() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    txtBeaconInfo.append("Connecting...\n");
                                }
                            });
                        }

                        @Override
                        public void onComplete(String token, int keyShift) {
                            txtBeaconInfo.append("Connection Complete...\n");
                            txtBeaconInfo.append(String.format("Token: %s\n", token));
                            txtBeaconInfo.append(String.format("Shift: %d\n", keyShift));


                            client.getBeaconList(new Client.ResultCallback() {
                                @Override
                                public void onResult(final Result result) {
                                    txtBeaconInfo.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            txtBeaconInfo.append("\nBEACON LIST\n");
                                            txtBeaconInfo.append(result.toString());
                                        }
                                    });
                                }
                            });
                        }

                        @Override
                        public void onError(final String error, JSONObject data) {
                            txtBeaconSerial.post(new Runnable() {
                                @Override
                                public void run() {
                                    txtBeaconInfo.append("[ERROR] " + error);
                                }
                            });
                        }
                    });
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

    private void testClient() {
        /********
         [Creating Api]
         Enter description: Test Api for Android client

         Api created:
         CODE :: FK1gcjLnQr8VPNbQfMhW61P8VlVtKc+R7jC+c3voGwOcvoVwk9P8z+PDpCkTPJ4Z
         KEY  :: ziAbvKkDChfK/069yOWn+2Q2/25R2mH+fKVBfPeIjG9jNMX6brp07l4tgYou9R1y

         *******/


        // Resume
        client.connect("", 0, new Client.TokenResumeCallback() {
            @Override
            public void onComplete(boolean isResumed, Integer keyShift) {

            }
        });

        // Make Call
        /*client.on("uploadMeasurements", new Client.ResultCallback() {
            @Override
            public void onResult(Result result) {

            }
        });*/

        // Make Call
        /*client.on("downloadMeasurements", new Client.ResultCallback() {
            @Override
            public void onResult(Result result) {

            }
        });*/
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
