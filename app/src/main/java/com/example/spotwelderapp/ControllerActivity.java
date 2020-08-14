package com.example.spotwelderapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.UUID;


public class ControllerActivity extends AppCompatActivity {

    String TAG = "ControllerActivity";
    String address = null;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    ConnectingDialog connectingDialog;
    //SPP UUID. Look for it
    private final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    Button uploadButton;
    EditText etPWeld, etPause, etWeld;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get device connection mac address from intent
        Intent newInt = getIntent();
        address = newInt.getStringExtra(MainActivity.EXTRA_ADDRESS);

        setContentView(R.layout.activity_controller);

        uploadButton = (Button)findViewById(R.id.uploadButton);
        etPWeld = (EditText)findViewById(R.id.etPreweld);
        etPause = (EditText)findViewById(R.id.etPause);
        etWeld = (EditText)findViewById(R.id.etWeld);

        connectingDialog = new ConnectingDialog(ControllerActivity.this);
        connectingDialog.startConnectingDialog();

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadSettings();
            }
        });

        // Set up edit texts and image

        new ConnectBT().execute();

    }

    private void Disconnect() {
        if (btSocket != null){
            try {
                btSocket.close();
                Log.d(TAG, "Disconnect: btSocket closed");
            } catch (IOException e) {
                Log.d(TAG, "Disconnect: btSocket not closed");
            }
        }
        finish();
    }

    private void turnOnLed(String key) {
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write(key.getBytes());
            } catch (IOException e) {
                Log.d(TAG, "turnOnLed: Could not get btSocket outStream");
            }
        }
    }
    private void turnOffLed(String key) {
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write(key.getBytes());
            } catch (IOException e) {
                Log.d(TAG, "turnOffLed: Could not get btSocket outStream");
            }
        }
    }

    private void downloadSettings() {
        if (btSocket != null) {
            // Get settings from text views, check ranges
            int tempPWeld = Integer.parseInt(etPWeld.getText().toString());
            int tempPause = Integer.parseInt(etPause.getText().toString());
            int tempWeld = Integer.parseInt(etWeld.getText().toString());

            if (!isSettingsOkay()) {
                Toast.makeText(this, "Only enter values from 0 to 999...retry", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                btSocket.getOutputStream().write("".getBytes());
            } catch (IOException e) {
                Log.d(TAG, "downloadSettings: Could not get btSocket outStream");
            }
        }
    }

    private void uploadSettings() {


    }

    private boolean isSettingsOkay() {
        Log.d(TAG, "isSettingsOkay: Checking setting values...");
        boolean functReturn = true;

        int tempPWeld = Integer.parseInt(etPWeld.getText().toString());
        int tempPause = Integer.parseInt(etPause.getText().toString());
        int tempWeld = Integer.parseInt(etWeld.getText().toString());

        if (tempPWeld < 0 || tempPWeld > 999){
            Log.d(TAG, "isSettingsOkay: PWeld value out of bounds");
            etPWeld.setBackgroundColor(123);

        }


    }


    private class ConnectBT extends AsyncTask<Void, Void, Void> {

        private boolean ConnectSuccess = true;

        @Override
        protected void onPreExecute()
        {
            Log.d(TAG, "ConnectBT/onPreExecute: Starting...");

        }

        @Override
        protected Void doInBackground(Void... voids) {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                Log.d(TAG, "ConnectBT/doInBackground: Connection failed");
                ConnectSuccess = false; //if the try failed, you can check the exception here
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            Log.d(TAG, "ConnectBT/onPostExecute: Starting...");
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                Log.d(TAG, "ConnectBT/onPostExecute: failure to connect to BT, ending");
                connectingDialog.dismissDialog();
                Toast.makeText(getApplicationContext(), "Connection failed", Toast.LENGTH_SHORT).show();
                finish();
            }
            else
            {
                Log.d(TAG, "ConnectBT/onPostExecute: Connected!");
                isBtConnected = true;
                connectingDialog.dismissDialog();
                Toast.makeText(getApplicationContext(), "Connection successful", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
