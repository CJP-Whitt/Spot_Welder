package com.example.spotwelderapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.UUID;


public class ControllerActivity extends AppCompatActivity {

    String TAG = "ControllerActivity";
    String address = null;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    private final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get device connectiona mac address from intent
        Intent newInt = getIntent();
        address = newInt.getStringExtra(MainActivity.EXTRA_ADDRESS);

        setContentView(R.layout.activity_controller);

        Button btOn = (Button)findViewById(R.id.onButton);
        Button btOff = (Button)findViewById(R.id.offButton);



        btOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOnLed("1");
            }
        });
        btOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOffLed("0");
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
                finish();
            }
            else
            {
                Log.d(TAG, "ConnectBT/onPostExecute: Connected!");

                isBtConnected = true;
            }
        }
    }
}
