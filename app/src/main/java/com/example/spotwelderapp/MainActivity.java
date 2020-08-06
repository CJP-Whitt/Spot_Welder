package com.example.spotwelderapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.AdapterView;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_ENABLE_BT_ADMIN = 2;
    private final static int REQUEST_ENABLE_FINE_LOCATION = 3;
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static String EXTRA_ADDRESS = "device_address";
    private String mServiceName = "Spot Welder Phone App";
    private int mBufferSize = 50000; //Default
    private Button scan;
    private Button connect;
    private ListView lvBTDevices;
    private String TAG = "MainActivity";
    private BluetoothDevice connectDevice;
    private BluetoothAdapter mBTAdapter;
    private DeviceListAdapter adapter;
    private ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: Creating window");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: Window created");

        final Context context = this;

        // Link views and btns, set onClick listeners
        scan = (Button) findViewById(R.id.scanButton);
        connect = (Button) findViewById(R.id.connectButton);
        lvBTDevices = (ListView) findViewById(R.id.deviceList);

        adapter = new DeviceListAdapter(this, R.layout.device_adapter_view, mBTDevices);
        lvBTDevices.setAdapter(adapter);
        lvBTDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                connectDevice =  (BluetoothDevice)parent.getItemAtPosition(position);
                Log.d(TAG, "ListView: onItemClick - Clicked *" + connectDevice.getName() + "*");
            }
        });
        Log.d(TAG, "onCreate: Created list adapter");

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClickListener: SCAN button clicked");


            }
        });
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClickListener: CONNECT button clicked");

                if (connectDevice == null){
                    Toast.makeText(getApplicationContext(), "Select device first",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                else{
                    Toast.makeText(getApplicationContext(), "Connecting to " + connectDevice.getName(),
                            Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(MainActivity.this, ControllerActivity.class);
                    i.putExtra(EXTRA_ADDRESS, connectDevice.getAddress());
                    startActivity(i);


                }
            }
        });
        Log.d(TAG, "onCreate: Linked view/bts/listeners");


        // Create BT adapter
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBTAdapter == null) { // If BT not available, send error and end
            Log.d(TAG, "onCreate: No BT available on device");
            Toast.makeText(this, "Bluetooth not supported...",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        else{
            Log.d(TAG, "onCreate: Created BT adapter");
        }

        // Check permissions and Get already bonded devices
        checkFineLocationPermission();
        checkBluetoothPermissions();


    }

    // Get BT boned devices
    protected void listBondedDevices(){
        Log.d(TAG, "listBondedDevices: Getting bonded devices...");
        Set<BluetoothDevice> all_devices = mBTAdapter.getBondedDevices();
        if (all_devices.size() > 0) {
            for (BluetoothDevice currentDevice : all_devices) {
                Log.d(TAG, "listBondedDevices: appended *" + currentDevice.getName() + "*");
                mBTDevices.add(currentDevice);
            }
            Log.d(TAG, "listBondedDevices: Devices appended");
        }
        adapter.notifyDataSetChanged();
        Log.d(TAG, "listBondedDevices: Adapter notified...DONE");
    }

    //BT exist and permissions check
    private class ListBondedDevices extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "ListBondedDevices: doInBackground");

            boolean bluetoothEnabledState = false;

            while (!bluetoothEnabledState){ // Wait for BT to be enabled
                if (mBTAdapter.isEnabled()){
                    bluetoothEnabledState = true;
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void params) {
            Log.d(TAG, "ListBondedDevices: onPostExecute");
            listBondedDevices();
            Log.d(TAG, "ListBondedDevices: onPostExecute...DONE");

        }
    }

    public void checkFineLocationPermission() {
        Log.d(TAG, "checkFineLocationPermission: Checking ACCESS_FINE_LOCAITON permission...");

        // Check if the fine location permission is already available
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // ACCESS_FINE_LOCATION permissions already available
            Log.d(TAG, "checkFineLocationPermission: ACCESS_FINE_LOCAITON already granted...DONE");
        } else {
            // ACCESS_FINE_LOCATION permission not granted
            // Provide additional rational to user
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                Toast.makeText(this, "Location permission is needed for bluetooth " +
                        "scanning.", Toast.LENGTH_SHORT).show();
            }
            Log.d(TAG, "checkFineLocationPermission: ACCESS_FINE_LOCAITON not granted...requesting");
            // Request ACCESS_FINE_LOCATION Permission
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ENABLE_FINE_LOCATION);
        }
    }

    public void checkBluetoothPermissions() {
        Log.d(TAG, "checkBluetoothPermissions: Checking BLUETOOTH permissions...");

        // Check if the bluetooth permission is already available
        if (checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
            // BLUETOOTH permissions already available
            Log.d(TAG, "checkBluetoothPermissions: BLUETOOTH already granted.");
        } else {
            // BLUETOOTH permission not granted
            // Provide additional rational to user
            if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH)){
                Toast.makeText(this, "Bluetooth permission is needed for connecting to bluetooth " +
                        "devices.", Toast.LENGTH_SHORT).show();
            }
            Log.d(TAG, "checkBluetoothPermissions: BLUETOOTH not granted...requesting");

            // Request BLUETOOTH Permission
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH},
                    REQUEST_ENABLE_BT);
        }

        // Check if the bluetooth admin permission is already available
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED) {
            // BLUETOOTH_ADMIN permissions already available
            Log.d(TAG, "checkBluetoothPermissions: BLUETOOTH_ADMIN already granted...DONE");
            mBTAdapter.enable();
            new ListBondedDevices().execute();
        } else {
            // BLUETOOTH ADMIN permission not granted
            // Provide additional rational to user
            if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_ADMIN)){
                Toast.makeText(this, "Bluetooth permission is needed for connecting to bluetooth " +
                        "devices.", Toast.LENGTH_SHORT).show();
            }
            Log.d(TAG, "checkBluetoothPermissions: BLUETOOTH_ADMIN not granted...requesting");

            // Request BLUETOOTH_ADMIN Permission
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADMIN},
                    REQUEST_ENABLE_BT_ADMIN);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){

        switch (requestCode){
            case REQUEST_ENABLE_FINE_LOCATION:
                if (grantResults.length > 0) {
                    Log.d(TAG, "onRequestPermissionsResult: Case-REQUEST_ENABLE_FINE_LOCATION");
                    boolean fineLocationPermission = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
                    if (!fineLocationPermission){ // Request denied, re request
                        Toast.makeText(this, "Location permission not granted", Toast.LENGTH_LONG).show();
                    }
                    break;
                }
            case REQUEST_ENABLE_BT:
                if (grantResults.length > 0) {
                    Log.d(TAG, "onRequestPermissionsResult: Case-REQUEST_ENABLE_BT");
                    boolean bluetoothPermission = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
                    if (bluetoothPermission){
                    } else {
                        Toast.makeText(this, "Bluetooth permission not granted", Toast.LENGTH_LONG).show();
                    }
                    break;
                }
            case REQUEST_ENABLE_BT_ADMIN:
                if (grantResults.length > 0) {
                    Log.d(TAG, "onRequestPermissionsResult: Case-REQUEST_REQUEST_ENABLE_BT_ADMIN");
                    boolean bluetoothAdminPermission = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
                    if (bluetoothAdminPermission){
                        mBTAdapter.enable();
                        new ListBondedDevices().execute();
                        Toast.makeText(this, "Bluetooth admin permission not granted", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Bluetooth permission not granted", Toast.LENGTH_LONG).show();
                    }
                    break;
                }
        }
    }


    @Override
    public  void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Resumed");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}