package com.lazerwarfare;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.lazerwarfare.Bluetooth.BleDevicesScanner;
import com.lazerwarfare.Bluetooth.BleManager;
import com.lazerwarfare.Bluetooth.BleUtils;
import com.lazerwarfare.Bluetooth.BluetoothDeviceData;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

//Debug activity for Bluetooth LE
public class Debug extends Activity implements BleManager.BleManagerListener{
    public String TAG = "Debug Activity";

    private TextView bluetoothStatus;
    private TextView eventStatus;
    private EditText sendMessage;
    private TextView sent;
    private TextView received;

    private String totalSent;
    private String totalReceived;
    private int numSent;
    private int numReceived;

    protected BleManager mBleManager;
    protected BluetoothGattService mUartService;
    private BleDevicesScanner mScanner;

    public static final String UUID_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_RX = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_TX = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_DFU = "00001530-1212-EFDE-1523-785FEABCD123";
    public static final int kTxMaxCharacters = 20;

    private ArrayList<BluetoothDeviceData> mScannedDevices;
    public ArrayAdapter<BluetoothDeviceData> mScannedDevicesAdapter;
    private long mLastUpdateMillis;
    private BluetoothDeviceData mSelectedDeviceData;
    private final static long kMinDelayToUpdateUI = 200;    // in milliseconds

    private Handler mhandler;
    public String lastEvent;
    public String status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        bluetoothStatus = (TextView) findViewById(R.id.bluetoothStatus);
        sendMessage = (EditText) findViewById(R.id.sendEditText);
        eventStatus = (TextView) findViewById(R.id.event);
        sent = (TextView) findViewById(R.id.sent);
        received = (TextView) findViewById(R.id.received);

        mBleManager = BleManager.getInstance(this);

        mhandler = new Handler();

        status = "";
        lastEvent = "";
        totalSent = "";
        totalReceived = "";

        numSent = 0;
        numReceived = 0;

        Log.i(TAG, "Debug Started");
        lastEvent = "Scanning for devices....";
        updateUI();
    }

    public void startScan() {
        if (BleUtils.getBleStatus(this) == BleUtils.STATUS_BLE_ENABLED) {
            // If was connected, disconnect
            mBleManager.disconnect();

            // Force restart scanning
            if (mScannedDevices != null) {      // Fixed a weird bug when resuming the app (this was null on very rare occasions even if it should not be)
                mScannedDevices.clear();
            }

            try {
                scanForDevices(null, null);
            }
            catch (Exception e)
            {
                Log.e(TAG, e.toString());
            }


            mhandler.postDelayed(new Runnable() {
                public void run() {
                    selectDeviceDialog();
                }
            }, 2000);
        }
        else
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
            super.onResume();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Set listener
        mBleManager.setBleListener(this);

        // Autostart scan
        startScan();
    }

    public void scanForDevices(final UUID[] servicesToScan, final String deviceNameToScanFor) {
        Log.i(TAG, "Begin Scan....");
        lastEvent = "Start Scanning";
        updateUI();
        stopScanning();

        BluetoothAdapter bluetoothAdapter = BleUtils.getBluetoothAdapter(getApplicationContext());
        if (BleUtils.getBleStatus(this) != BleUtils.STATUS_BLE_ENABLED) {
            Log.i(TAG, "startScan: BluetoothAdapter not initialized or unspecified address.");
        } else {
            mScanner = new BleDevicesScanner(bluetoothAdapter, servicesToScan, new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    final String deviceName = device.getName();
                    //Log.d(TAG, "Discovered device: " + (deviceName != null ? deviceName : "<unknown>"));

                    BluetoothDeviceData previouslyScannedDeviceData = null;
                    if (deviceNameToScanFor == null || (deviceName != null && deviceName.equalsIgnoreCase(deviceNameToScanFor))) {       // Workaround for bug in service discovery. Discovery filtered by service uuid is not working on Android 4.3, 4.4
                        if (mScannedDevices == null) mScannedDevices = new ArrayList<>();       // Safeguard

                        // Check that the device was not previously found
                        for (BluetoothDeviceData deviceData : mScannedDevices) {
                            if (deviceData.device.getAddress().equals(device.getAddress())) {
                                previouslyScannedDeviceData = deviceData;
                                break;
                            }
                        }

                        BluetoothDeviceData deviceData;
                        if (previouslyScannedDeviceData == null) {
                            // Add it to the mScannedDevice list
                            deviceData = new BluetoothDeviceData();
                            mScannedDevices.add(deviceData);
                            lastEvent = "Device Found";
                        } else {
                            deviceData = previouslyScannedDeviceData;
                        }

                        deviceData.device = device;
                        deviceData.rssi = rssi;
                        deviceData.scanRecord = scanRecord;
                        decodeScanRecords(deviceData);

                        // Update device data
                        long currentMillis = SystemClock.uptimeMillis();
                        if (previouslyScannedDeviceData == null || currentMillis - mLastUpdateMillis > kMinDelayToUpdateUI) {          // Avoid updating when not a new device has been found and the time from the last update is really short to avoid updating UI so fast that it will become unresponsive
                            mLastUpdateMillis = currentMillis;
                        }
                    }
                }
            });

            // Start scanning
            mScanner.start();
            updateUI();
        }
        Log.i(TAG, "END SCAN-------------------");

    }

    private void decodeScanRecords(BluetoothDeviceData deviceData) {
        // based on http://stackoverflow.com/questions/24003777/read-advertisement-packet-in-android
        final byte[] scanRecord = deviceData.scanRecord;

        ArrayList<UUID> uuids = new ArrayList<>();
        byte[] advertisedData = Arrays.copyOf(scanRecord, scanRecord.length);
        int offset = 0;
        deviceData.type = BluetoothDeviceData.kType_Unknown;

        // Check if is an iBeacon ( 0x02, 0x0x1, a flag byte, 0x1A, 0xFF, manufacturer (2bytes), 0x02, 0x15)
        final boolean isBeacon = advertisedData[0] == 0x02 && advertisedData[1] == 0x01 && advertisedData[3] == 0x1A && advertisedData[4] == (byte) 0xFF && advertisedData[7] == 0x02 && advertisedData[8] == 0x15;

        // Check if is an URIBeacon
        final byte[] kUriBeaconPrefix = {0x03, 0x03, (byte) 0xD8, (byte) 0xFE};
        final boolean isUriBeacon = Arrays.equals(Arrays.copyOf(scanRecord, kUriBeaconPrefix.length), kUriBeaconPrefix) && advertisedData[5] == 0x16 && advertisedData[6] == kUriBeaconPrefix[2] && advertisedData[7] == kUriBeaconPrefix[3];

        if (isBeacon) {
            deviceData.type = BluetoothDeviceData.kType_Beacon;

            // Read uuid
            offset = 9;
            UUID uuid = BleUtils.getUuidFromByteArrayBigEndian(Arrays.copyOfRange(scanRecord, offset, offset + 16));
            uuids.add(uuid);
            offset += 16;

            // Skip major minor
            offset += 2 * 2;   // major, minor

            // Read txpower
            final int txPower = advertisedData[offset++];
            deviceData.txPower = txPower;
        } else if (isUriBeacon) {
            deviceData.type = BluetoothDeviceData.kType_UriBeacon;

            // Read txpower
            final int txPower = advertisedData[9];
            deviceData.txPower = txPower;
        } else {
            // Read standard advertising packet
            while (offset < advertisedData.length - 2) {
                // Length
                int len = advertisedData[offset++];
                if (len == 0) break;

                // Type
                int type = advertisedData[offset++];
                if (type == 0) break;

                // Data
//            Log.d(TAG, "record -> lenght: " + length + " type:" + type + " data" + data);

                switch (type) {
                    case 0x02: // Partial list of 16-bit UUIDs
                    case 0x03: {// Complete list of 16-bit UUIDs
                        while (len > 1) {
                            int uuid16 = advertisedData[offset++] & 0xFF;
                            uuid16 |= (advertisedData[offset++] << 8);
                            len -= 2;
                            uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                        }
                        break;
                    }

                    case 0x06:          // Partial list of 128-bit UUIDs
                    case 0x07: {        // Complete list of 128-bit UUIDs
                        while (len >= 16) {
                            try {
                                // Wrap the advertised bits and order them.
                                UUID uuid = BleUtils.getUuidFromByteArraLittleEndian(Arrays.copyOfRange(advertisedData, offset, offset + 16));
                                uuids.add(uuid);

                            } catch (IndexOutOfBoundsException e) {
                                Log.e(TAG, "BlueToothDeviceFilter.parseUUID: " + e.toString());
                            } finally {
                                // Move the offset to read the next uuid.
                                offset += 16;
                                len -= 16;
                            }
                        }
                        break;
                    }

                    case 0x0A: {   // TX Power
                        final int txPower = advertisedData[offset++];
                        deviceData.txPower = txPower;
                        break;
                    }

                    default: {
                        offset += (len - 1);
                        break;
                    }
                }
            }

            // Check if Uart is contained in the uuids
            boolean isUart = false;
            for (UUID uuid : uuids) {
                if (uuid.toString().equalsIgnoreCase(UUID_SERVICE)) {
                    isUart = true;
                    break;
                }
            }
            if (isUart) {
                deviceData.type = BluetoothDeviceData.kType_Uart;
            }
        }

        deviceData.uuids = uuids;
    }

    private void stopScanning() {
        // Stop scanning
        Log.i(TAG, "Stopping Scanning");
       lastEvent = "Stopping Scan";
        if (mScanner != null) {
            Log.i(TAG, "Stopping Scanner");
            mScanner.stop();
            mScanner = null;
        }
        updateUI();
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    public void sendMessage(View v)
    {
        if (sendMessage.getText().toString().length() > 0) {
            lastEvent = "Sent Data";
            numSent++;
            Log.i(TAG, "Sending Message");
            if (numSent % 5 == 0)
            {
                totalSent = "";
            }
            sendData((sendMessage.getText().toString() + "\n").getBytes());
            totalSent += sendMessage.getText().toString() + "\n";
            updateUI();
        }
    }

    public void selectDeviceDialog()
    {
        Log.i(TAG, "Select Device Dialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(Debug.this);
        if (mScannedDevices != null) {
            //make sure no null devices exist
            for (int i = 0; i < mScannedDevices.size(); i++)
            {
                if (mScannedDevices.get(i).device.getName() == null)
                {
                    mScannedDevices.remove(i);
                }
            }
            if (mScannedDevices.size() > 0) {
                Log.i(TAG, "Devices found");
                String[] items = new String[mScannedDevices.size()];
                for (int i = 0; i < mScannedDevices.size(); i++) {
                    items[i] = mScannedDevices.get(i).device.getName();
                }

                builder.setTitle("Connect to BLE Device")
                        .setItems(items, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        connect(mScannedDevices.get(which).device);
                                        dialog.dismiss();
                                    }
                                }
                        );

                // Show dialog
                AlertDialog dialog = builder.create();
                dialog.show();
            }
            else
            {
                startScan();
            }
        }
        else
        {
            Log.i(TAG, "Dialog cannot start - no devices found");
            builder.setTitle("Connect to BLE Device")
                    .setMessage("No Bluetooth Low Energy Devices Detected. Press OK to scan again")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            startScan();
                        }
                    })
                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Canceled.
                            dialog.cancel();
                        }
                    });
            // Show dialog
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    public void updateUI()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bluetoothStatus.setText("Bluetooth Status: " + status);
                eventStatus.setText("Last Event: " + lastEvent);

                sent.setText(totalSent);
                received.setText(totalReceived);
            }
        });
    }

    private void connect(BluetoothDevice device) {
        boolean isConnecting = mBleManager.connect(this, device.getAddress());
        if (isConnecting) {
            Log.i(TAG, "Attempting to connect to device: " + device.getName());
            status = "Connecting...";
        }
        updateUI();
    }

    protected void sendData(byte[] data) {
        if (mUartService != null) {
            // Split the value into chunks (UART service has a maximum number of characters that can be written )
            for (int i = 0; i < data.length; i += kTxMaxCharacters) {
                final byte[] chunk = Arrays.copyOfRange(data, i, Math.min(i + kTxMaxCharacters, data.length));
                mBleManager.writeService(mUartService, UUID_TX, chunk);
            }
        } else {
            Log.w(TAG, "Uart Service not discovered. Unable to send data");
        }
    }

    // OnStop, called right before the activity loses foreground focus.  Close the BTLE connection.
    @Override
    protected void onStop() {
        Log.i(TAG, "Stopping Bluetooth");
        mBleManager.close();
        super.onStop();
    }

    @Override
    public void onServicesDiscovered() {
        Log.i(TAG, "Services Discovered");
        //stopScanning();
        //Get the UART Service
        mUartService = mBleManager.getGattService(UUID_SERVICE);

        mBleManager.enableNotification(mUartService, UUID_RX, true);

        if (mUartService != null)
        {
            Log.i(TAG, "UART service discovered");
        }

    }

    @Override
    public void onConnected() {
        Log.i(TAG, "Connected!!");
        stopScanning();
        status = "Connected";
        updateUI();
    }

    @Override
    public void onConnecting() {
        Log.i(TAG, "Connecting...");
    }

    @Override
    public void onDisconnected() {
        status = "Disconnected";
        updateUI();
    }

    @Override
    public void onDataAvailable(BluetoothGattCharacteristic characteristic) {
        Log.i(TAG, "Data Available!!");
        lastEvent = "Data Received";

        // UART RX
        if (characteristic.getService().getUuid().toString().equalsIgnoreCase(UUID_SERVICE)) {
            if (characteristic.getUuid().toString().equalsIgnoreCase(UUID_RX)) {
                final byte[] bytes = characteristic.getValue();
                final String data = new String(bytes, Charset.forName("UTF-8"));

                numReceived++;
                if (numReceived % 5 == 0)
                {
                    totalReceived = "";
                }
                Log.i(TAG, "Received Data: " + data);
                totalReceived += data;
            }
        }
        updateUI();
    }

    @Override
    public void onDataAvailable(BluetoothGattDescriptor descriptor) {
        Log.i(TAG, "onDataAvailable for descriptor");
    }

    @Override
    public void onReadRemoteRssi(int rssi) {

    }
}
