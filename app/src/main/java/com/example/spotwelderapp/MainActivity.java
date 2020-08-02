package com.example.spotwelderapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.Tag;
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
    private final static int REQUEST_ENABLE_COARSE_LOCATION= 3;
    private final static int REQUEST_ENABLE_FINE_LOCATION = 4;
    private Button scan;
    private Button connect;
    private ListView lvBTDevices;
    private String TAG = "MainActivity";
    private String connectMACAddress = "";
    private BluetoothAdapter mBTAdapter;
    private ArrayList<Device> mBTDevices = new ArrayList<>();

    //private DeviceListAdapter mDeviceListAdapter;

//    final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                // Discovery has found a device. Get the BluetoothDevice
//                // object and its info from the Intent.
//                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                Log.i("***INFO***","onRecieve: " + device.getName() + ": " + device.getAddress());
//                mBTDevices.add(device);
//                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
//                deviceList.setAdapter((mDeviceListAdapter));
//
//            }
//        }
//    };

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

        final DeviceListAdapter adapter = new DeviceListAdapter(this, R.layout.device_adapter_view, mBTDevices);
        lvBTDevices.setAdapter(adapter);
        Log.d(TAG, "onCreate: Created list adapter");


        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClickListener: SCAN button clicked");
                permissionCheck();

                Set<BluetoothDevice> all_devices = mBTAdapter.getBondedDevices();
                if (all_devices.size()>0){
                    for (BluetoothDevice currentDevice : all_devices) {
                        Device device = new Device(currentDevice.getName(), currentDevice.getAddress());
                        Log.d(TAG, "onClickListener: SCAN - appended *" + device.getName() + "*");
                        mBTDevices.add(device);
                    }
                    Log.d(TAG, "onClickListener: SCAN - Devices appended");
                }
                Log.d(TAG, "onClickListener: SCAN - Adapter notified");
                adapter.notifyDataSetChanged();


            }
        });
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectMACAddress == ""){
                    Log.d(TAG, "onClickListener: CONNECT button clicked");
                    Toast.makeText(getApplicationContext(), "Select device first",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                else{

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

    }
    @Override
    public  void onStart() {
        super.onStart();
        // Check if BT is enabled, if not request it
        if (!mBTAdapter.isEnabled()){
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
            Log.d(TAG, "onStart: BT adapter is disabled, asking for BT to be enabled");

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        //unregisterReceiver(mBroadcastReceiver);
    }

    // Fill in the list View for available BT devices
//    protected void populateBTList(){
//        // Enable discovery
//        if (!mBTAdapter.isDiscovering()) {
//            mBTAdapter.startDiscovery();
//        }
//
//        Intent discoverableIntent =
//                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//        startActivity(discoverableIntent);
//
//        // Get paired devices
//        Set<BluetoothDevice> pairedDevices = mBTAdapter.getBondedDevices();
//        if (pairedDevices.size() > 0){
//            mBTDevices.addAll(pairedDevices);
//        }
//
//        // Get unknown devices
//        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//        registerReceiver(mBroadcastReceiver, filter);
//
//        // Populate List
//
//    }


    //BT exist and permissions check
    protected void permissionCheck() {
        // Check if BT adapter created
        if (mBTAdapter == null) {
            Toast.makeText(getApplicationContext(), "BT Not Available", Toast.LENGTH_LONG).show();
            Log.d(TAG, "permissionsCheck: BT not available on device");

        }
        if (mBTAdapter.isEnabled()) { // Check for BT permissions
            Toast.makeText(getApplicationContext(), "BT Available", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "permissionsCheck: BT adapter is enabled");
        }
        else{
            Log.d(TAG, "permissionsCheck: BT adapter is disabled, asking for BT to be enabled");
        }

        // Check BT and Location permissions for app
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED){
            Log.d(TAG, "permissionsCheck: COARSE_LOCATION not granted...Requesting");
            requestPermissions(new String[] { Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_ENABLE_COARSE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED){
            Log.d(TAG, "permissionsCheck: FINE_LOCATION not granted...Requesting");
            requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ENABLE_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) !=
                PackageManager.PERMISSION_GRANTED){
            Log.d(TAG, "permissionsCheck: BLUETOOTH not granted...Requesting");
            requestPermissions(new String[] { Manifest.permission.BLUETOOTH},
                    REQUEST_ENABLE_BT);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) !=
                PackageManager.PERMISSION_GRANTED){
            Log.d(TAG, "permissionsCheck: BLUETOOTH_ADMIN not granted...Requesting");
            requestPermissions(new String[] { Manifest.permission.BLUETOOTH_ADMIN},
                    REQUEST_ENABLE_BT_ADMIN);
        }
    }
}