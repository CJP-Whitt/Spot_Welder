package com.example.spotwelderapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class DeviceListAdapter extends ArrayAdapter<Device> {

    private static final String TAG = "DeviceListAdapter";
    private final Activity mContext;
    private ArrayList<Device> mDevices = new ArrayList<>();
    private int mViewResourceId;

    public DeviceListAdapter(Activity context, int tvResourceId, ArrayList<Device> devices){
        super(context, tvResourceId, devices);
        this.mContext = context;
        this.mViewResourceId = tvResourceId;
        this.mDevices = devices;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(TAG, "getView: Inflating");
        LayoutInflater inflater = mContext.getLayoutInflater();
        View rowView = inflater.inflate(mViewResourceId, null, true);

        if (mDevices.get(position) != null) {
            TextView deviceName = (TextView) rowView.findViewById(R.id.btDeviceName);
            TextView deviceAddress = (TextView) rowView.findViewById(R.id.btDeviceAddress);

            Device device = mDevices.get(position);
            deviceName.setText(device.getName());
            deviceAddress.setText(device.getAddress());
            Log.d(TAG, "getView: Setting text");
        }


        return rowView;
    }

    public set
}
