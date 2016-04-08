package com.lazerwarfare.Bluetooth;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.UUID;

public class BluetoothDeviceData {
    public BluetoothDevice device;
    public int rssi;
    public byte[] scanRecord;

    // Decoded scan record (update R.array.scan_devicetypes if this list is modified)
    public static final int kType_Unknown = 0;
    public static final int kType_Uart = 1;
    public static final int kType_Beacon = 2;
    public static final int kType_UriBeacon = 3;

    public int type;
    public int txPower;
    public ArrayList<UUID> uuids;
}
