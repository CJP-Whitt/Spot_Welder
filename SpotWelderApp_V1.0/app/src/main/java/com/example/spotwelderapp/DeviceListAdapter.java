package com.example.spotwelderapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {

    private static final String TAG = "DeviceListAdapter";
    private final Activity mContext;
    private ArrayList<BluetoothDevice> mDevices = new ArrayList<>();
    private int mViewResourceId;

    public DeviceListAdapter(Activity context, int tvResourceId, ArrayList<BluetoothDevice> devices){
        super(context, tvResourceId, devices);
        this.mContext = context;
        this.mViewResourceId = tvResourceId;
        this.mDevices = devices;
    }

    @NonNull
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(TAG, "getView: Inflating");
        LayoutInflater inflater = mContext.getLayoutInflater();
        @SuppressLint("ViewHolder") View rowView = inflater.inflate(mViewResourceId, null, true);

        if (mDevices.get(position) != null) {
            TextView deviceName = (TextView) rowView.findViewById(R.id.btDeviceName);
            TextView deviceAddress = (TextView) rowView.findViewById(R.id.btDeviceAddress);

            BluetoothDevice device = mDevices.get(position);
            deviceName.setText(device.getName());
            deviceAddress.setText(device.getAddress());
            Log.d(TAG, "getView: Setting text");
        }


        return rowView;
    }

}
