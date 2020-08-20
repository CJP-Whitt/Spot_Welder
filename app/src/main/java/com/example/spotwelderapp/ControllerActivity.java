package com.example.spotwelderapp;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.UUID;


public class ControllerActivity extends AppCompatActivity {

    String TAG = "ControllerActivity";
    String address = null;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    ConnectingDialog connectingDialog;
    Context mContext;
    //SPP UUID. Look for it
    private final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public Button uploadBtn, readBtn;
    public EditText etPWeld, etPause, etWeld;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        // Get device connection mac address from intent
        Intent newInt = getIntent();
        address = newInt.getStringExtra(MainActivity.EXTRA_ADDRESS);

        setContentView(R.layout.activity_controller);

        uploadBtn = (Button)findViewById(R.id.uploadButton);
        readBtn = (Button)findViewById(R.id.readButton);
        etPWeld = (EditText)findViewById(R.id.etPreweld);
        etPause = (EditText)findViewById(R.id.etPause);
        etWeld = (EditText)findViewById(R.id.etWeld);
        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "uploadBtn/onClick: Upload button clicked");
                uploadSettings();
            }
        });
        readBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "readBtn/onClick: Read button clicked");
                new DownloadSettings().execute();
            }
        });

        connectingDialog = new ConnectingDialog(ControllerActivity.this);
        connectingDialog.startConnectingDialog();

        // Set up edit texts and image

        new ConnectBT().execute();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Disconnect();
    }
//    @Override
//    protected void onPause() {
//        super.onPause();
//        Disconnect();
//    }
//    @Override
//    protected void onResume() {
//        super.onResume();
//        // new ConnectBT().execute();
//    }

    private void Disconnect() {
        if (btSocket != null){
            try {
                btSocket.close();
                Log.d(TAG, "Disconnect: btSocket closed");
            } catch (IOException e) {
                Log.d(TAG, "Disconnect: btSocket not closed");
            }
        }
    }


    private void uploadSettings() {       // Uploading welding parameters to arduino
        Log.d(TAG, "uploadSettings: Uploading welding settings");

        if (btSocket != null) {
            // Check bluetooth device still connected
            if (!btSocket.isConnected()){
                Toast.makeText(this, "Upload failed...device not connected", Toast.LENGTH_SHORT).show();
                finish();
            }

            // Get settings from text views, check ranges
            String tempPWeld = etPWeld.getText().toString();
            String tempPause = etPause.getText().toString();
            String tempWeld = etWeld.getText().toString();

            if (!isSettingsOkay()) {
                Toast.makeText(this, "Only enter values from 0 to 999...retry", Toast.LENGTH_LONG).show();
                return;
            }

            // Upload code = 200, then array string
            String paramsArrayStr = "200:[" + tempPWeld + "," + tempPause + "," + tempWeld + "]";

            // Send settings array
            try {
                btSocket.getOutputStream().write(paramsArrayStr.getBytes());
                Log.d(TAG, "uploadSettings: BT data sent");
                Toast.makeText(this, "Upload successful!", Toast.LENGTH_SHORT).show();


            } catch (IOException e) {
                Log.d(TAG, "uploadSettings: Could not get btSocket outStream");
                Toast.makeText(this, "Upload failed...", Toast.LENGTH_SHORT).show();

            }
        }
    }



    private boolean isSettingsOkay() {      // Checking welding parameters for upload
        Log.d(TAG, "isSettingsOkay: Checking setting values...");
        boolean functReturn = true;
        String tempPWeldStr = etPWeld.getText().toString();
        String tempPauseStr = etPause.getText().toString();
        String tempWeldStr = etWeld.getText().toString();

        // Check for empty editTexts
        if (tempPWeldStr.isEmpty()){
            Log.d(TAG, "isSettingsOkay: PWeld value empty");
            etPWeld.setError("Field cannot be empty");
            functReturn = false;
        }
        if (tempPauseStr.isEmpty()){
            Log.d(TAG, "isSettingsOkay: Pause value empty");
            etPause.setError("Field cannot be empty");
            functReturn = false;
        }
        if (tempWeldStr.isEmpty()){
            Log.d(TAG, "isSettingsOkay: Weld value empty");
            etWeld.setError("Field cannot be empty");
            functReturn = false;
        }
        if (!functReturn) {
            return false;
        }

        int tempPWeld = Integer.parseInt(etPWeld.getText().toString());
        int tempPause = Integer.parseInt(etPause.getText().toString());
        int tempWeld = Integer.parseInt(etWeld.getText().toString());

        // Check values
        if (tempPWeld < 0 || tempPWeld > 999){
            Log.d(TAG, "isSettingsOkay: PWeld value out of bounds");
            etPWeld.setError("Value out of bounds");
            functReturn = false;
        }
        if (tempPause < 0 || tempPause > 999){
            Log.d(TAG, "isSettingsOkay: Pause value out of bounds");
            etPause.setError("Value out of bounds");
            functReturn = false;
        }
        if (tempWeld < 0 || tempWeld > 999){
            Log.d(TAG, "isSettingsOkay: Weld value out of bounds");
            etWeld.setError("Value out of bounds");
            functReturn = false;
        }

        return functReturn;
    }

    private class DownloadSettings extends AsyncTask<Void, Void, Void> {

        private String[] paramsArr = new String[3];

        @Override
        protected void onPreExecute()
        {
            Log.d(TAG, "DownloadSetting/onPreExecute: Starting...");
        }

        @Override
        protected Void doInBackground(Void... voids) {      // Request settings from welder and set edit text views
            Log.d(TAG, "DownloadSettings/doInBackground: Downloading welding settings...");
            InputStream socketInStream = null;
            byte[] buffer = new byte[256];
            int bytes;
            String readMessage = "";

            if (btSocket != null) {
                // Check that
                if (!btSocket.isConnected()){
                    Toast.makeText(mContext, "Read failed...device not connected", Toast.LENGTH_SHORT).show();
                    finish();
                }

                // Send code 100, then receive string
                try {
                    btSocket.getOutputStream().write("100".getBytes());
                    Log.d(TAG, "DownloadSettings/doInBackground: BT read code sent");
                } catch (IOException e) {
                    Log.d(TAG, "DownloadSettings/doInBackground: Could not get btSocket outStream");
                    cancel(true);
                }

                try {
                    socketInStream = btSocket.getInputStream();
                    Log.d(TAG, "downloadSettings: Got inStream");
                } catch (IOException e) {
                    Log.d(TAG, "downloadSettings: Could not get btSocket inStream");
                    cancel(true);
                }

                if (socketInStream != null) {
                    while (true) {
                        try {
                            Log.d(TAG, "downloadSettings: Reading incoming data");
                            bytes = socketInStream.read(buffer);
                            String chunk = new String(buffer, 0 ,bytes);
                            readMessage += chunk;
                            if (readMessage.contains("]")){
                                break;
                            }
                        }
                        catch (IOException e) {
                            Log.d(TAG, "downloadSettings: Couldn't read data");
                            Toast.makeText(mContext, "Couldn't read welder settings...", Toast.LENGTH_SHORT).show();
                            cancel(true);
                        }
                    }
                    Log.d(TAG, "downloadSettings: Read message: " + readMessage);
                }
                else {cancel(true);}

                int charIndex = 0;
                int paramIndex = 0;
                String tempNumString = "";

                while (charIndex < readMessage.length()){
                    if (Character.isDigit(readMessage.charAt(charIndex))){
                        tempNumString += readMessage.charAt(charIndex);

                        if (!Character.isDigit(readMessage.charAt(charIndex + 1))){
                            paramsArr[paramIndex] = tempNumString;
                            Log.d(TAG, "downloadSettings:  Found int: " + tempNumString);
                            tempNumString = "";
                            paramIndex++;
                        }
                    }

                    charIndex++;
                }
            } else {cancel(true);}

            Log.d(TAG, "downloadSettings: Downloading welding settings...DONE");

            return null;
        }

        @Override
        protected void onCancelled() {
            Log.d(TAG, "DownloadSettings: Cancelled...DONE");

        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, log
        {
            etPWeld.setText(paramsArr[0]);
            etPause.setText(paramsArr[1]);
            etWeld.setText(paramsArr[2]);
            Toast.makeText(mContext, "Download successful!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "DownloadSettings: DONE");
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
                new DownloadSettings().execute();
            }
        }
    }
}
