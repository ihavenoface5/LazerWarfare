package com.lazerwarfare;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.lazerwarfare.Bluetooth.BleDevicesScanner;
import com.lazerwarfare.Bluetooth.BleManager;
import com.lazerwarfare.Bluetooth.BleUtils;
import com.lazerwarfare.Bluetooth.BluetoothDeviceData;
import com.lazerwarfare.Bluetooth.KnownUUIDs;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Game extends Activity implements OnGestureListener,BleManager.BleManagerListener{
    public String TAG = "Game";
	TextView timeLeftText;
	TextView timer;
	TextView health;
	TextView team1Score;
	TextView team2Score;
	TextView ammo;
	TextView score;
	TextView waitTime;
    TextView bluetooth;
    TextView server;
	ImageButton infinity;
	TextView waiting;
    public static Handler mhandler;
	public static Handler handler;
	public static Handler handler1;
	public static Handler handler2;

	String waitingText = "Waiting for Players...";
	String respawnText = "Recharging...";
	TextView pairedGun;
    protected BleManager mBleManager;
    protected BluetoothGattService mUartService;
    private BleDevicesScanner mScanner;
    public static final int kTxMaxCharacters = 20;
    private ArrayList<BluetoothDeviceData> mScannedDevices;
    public ArrayAdapter<BluetoothDeviceData> mScannedDevicesAdapter;
    private long mLastUpdateMillis;
    private final static long kMinDelayToUpdateUI = 200;    // in milliseconds
    public String bluetoothStatus = "Searching";
    public String serverStatus = "Offline";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);

		timeLeftText = (TextView) findViewById(R.id.timeLeftText);
		timer = (TextView) findViewById(R.id.time);
		timer.setText(Integer.toString(Player.time_limit));
		health = (TextView) findViewById(R.id.health);
		team1Score = (TextView) findViewById(R.id.team1Score);
		team2Score = (TextView) findViewById(R.id.team2Score);
		ammo = (TextView) findViewById(R.id.ammo);
		score = (TextView) findViewById(R.id.score);
		infinity = (ImageButton) findViewById(R.id.infiniteTime);
		waiting = (TextView) findViewById(R.id.waiting);
		pairedGun = (TextView) findViewById(R.id.pairedGun);
        bluetooth = (TextView) findViewById(R.id.bluetoothStatus);
        server = (TextView) findViewById(R.id.serverStatus);

		
		Typeface type = Typeface.createFromAsset(getAssets(), "fonts/PROXIMANOVA-BOLD.ttf");
		timeLeftText.setTypeface(type);

        mBleManager = BleManager.getInstance(this);
        mBleManager.setBleListener(Game.this);
        mhandler = new Handler();

        //Debugging
        Log.w(TAG, "Player name: " + Player.name);

		Player.respawn = Player.respawnTime;
		if (Player.gunId != -1)
		{
            Log.w(TAG, "This happened!");
			pairedGun.setText("Gun:" + Integer.toString(Player.gunId));
			pairedGun.setVisibility(View.VISIBLE);
		}
        else
        {
            Log.w(TAG, "Gun id: " + Player.gunId);
        }
		
		if (Player.time_limit == 999)
		{
			Player.delay = 0;
			//Hide timer, show infinity, don't have timer running
			timer.setVisibility(View.GONE);
			team1Score.setVisibility(View.GONE);
			team2Score.setVisibility(View.GONE);
			infinity.setVisibility(View.VISIBLE);
		    waiting.setVisibility(View.GONE);
			update();
            serverStatus = "Offline";
		}
		else
		{
			//Normal timing conditions
			if (Player.time_limit == 998)
			{
				timer.setVisibility(View.GONE);
				infinity.setVisibility(View.VISIBLE);
			}
			else
			{
				timer.setText(Integer.toString(Player.time_limit) + ":00");
			}

			handler1 = new Handler();
			final Runnable runnable1 = new Runnable() {
				   @Override
				   public void run() {
				      Player.sync();
				      handler1.postDelayed(this, 3000);
				   }
				};
			
			handler2 = new Handler();
			final Runnable runnable2 = new Runnable() {
				   @Override
				   public void run(){
					  if (Player.delay > 0)
					  {
						  waiting.setText(waitingText + Integer.toString(Player.delay));
						  Player.delay--;
					      handler1.postDelayed(this, 1000);
					  }
				   }
				};
			handler2.postDelayed(runnable2, 0);
			
			Runnable runnable = new Runnable() {
				   @Override
				   public void run() {
				      update();
				      waiting.setVisibility(View.GONE);
				      MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.fight); 
                  	  mp.start();
				      handler2.removeCallbacks(runnable2);
				      handler1.postDelayed(runnable1, 3000);
				   }
				};
			handler = new Handler();
			
			handler.postDelayed(runnable, Player.delay*1000);
		  
		}
	}

    @Override
    public void onResume()
    {
        super.onResume();
        startScan();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.game, menu);
		return true;
	}
	
	
	@Override
	public void onBackPressed()
	{
		Player.promptExit(this, "Press Cancel to save the world. \n Press OK to abandon it in its hour of need");
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	     if (keyCode == KeyEvent.KEYCODE_BACK) {
	     //preventing default implementation previous to android.os.Build.VERSION_CODES.ECLAIR
             Player.promptExit(this, "Press Cancel to save the world. \r\nPress OK to abandon it in its hour of need");
         }
	     return super.onKeyDown(keyCode, event);    
	}
	
	public void update(){
		
		new CountDownTimer(Player.time_limit * 60 * 1000, 1000) {
		
		     @SuppressLint("NewApi") public void onTick(long millis) {
		    	 String time = "";
		    	 long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
		    	 long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
		    	 if (minutes == 1 && seconds == 0)
		    	 {
		    		 MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.oneminutewarning); 
                 	 mp.start();
		    	 }
		    	 if (minutes < 10)
		    	 {
		    		 time = "0" + Long.toString(minutes);
		    	 }
		    	 else
		    	 {
		    		 time = Long.toString(minutes);
		    	 }
		    	 
		    	 if (seconds < 10)
		    	 {
		    		 time = time + ":0" + Long.toString(seconds);
		    	 }
		    	 else
		    	 {
		    		 time = time + ":" + Long.toString(seconds);
		    	 }
		    	 timer.setText(time);
		    	 health.setText(Integer.toString(Player.health));
		    	 ammo.setText(Integer.toString(Player.ammo));
		    	 score.setText(Integer.toString(Player.score));
		    	 team1Score.setText(Integer.toString(Player.team1Score));
		    	 team2Score.setText(Integer.toString(Player.team2Score));
                 bluetooth.setText("Bluetooth Status: " + bluetoothStatus);
                 server.setText("Server Status: " + serverStatus);
		    	 
		    	 if (Player.health == 0 || Player.ammo == 0)
		    	 {
		    		 waiting.setText(respawnText + Integer.toString(Player.respawn));
		    		 if (waiting.getVisibility() == View.GONE)
		    		 {
		    			 waiting.setVisibility(View.VISIBLE);
		    		 }
		    		 Player.respawn--;
		    		 if (Player.respawn <= 0)
		    		 {
		    			 Player.updateStatus = true;
		    			 Player.status = "111";
		    			 Player.health = 100;
		    			 Player.ammo = Player.ammoReload;
		    			 Player.respawn = Player.respawnTime;
		    			 waiting.setVisibility(View.GONE);
		    		 }
		    	 }
		     }
		
		     public void onFinish() {
		    	timer.setText("00:00");
		    	Log.i("Game", "Timer done!");
		    	try {
		    		handler1.removeCallbacksAndMessages(null);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    	finish();
		     }
		  }.start();
	}

	@Override 
    public boolean onTouchEvent(MotionEvent event){
        // Be sure to call the superclass implementation
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent event) { 
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, 
            float velocityX, float velocityY) {
    	int sensitivity = 50;
        if(event1.getX() - event2.getX() > sensitivity)	//Swipe to Left
        {
        	//startMap(null);
        }
        else if (event1.getX() - event2.getX() < sensitivity) //Swipe to Right
        {
        	//startMessages(null);
        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
            float distanceY) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
    }

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

    public void startScan() {
        Log.w(TAG, "startScan() called!!!!");
        if (BleUtils.getBleStatus(this) == BleUtils.STATUS_BLE_ENABLED) {
            // If was connected, disconnect
            mBleManager.disconnect();

            // Force restart scanning
            if (mScannedDevices != null) {      // Fixed a weird bug when resuming the app (this was null on very rare occasions even if it should not be)
                mScannedDevices.clear();
            }
            scanForDevices(null, null);

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

    private void connect(BluetoothDevice device) {
        boolean isConnecting = mBleManager.connect(this, device.getAddress());
        if (isConnecting) {
            Log.i(TAG, "Attempting to connect to device: " + device.getName());
        }
        bluetoothStatus = "Connecting...";
    }

    protected void sendData(byte[] data) {
        if (mUartService != null) {
            // Split the value into chunks (UART service has a maximum number of characters that can be written )
            for (int i = 0; i < data.length; i += kTxMaxCharacters) {
                final byte[] chunk = Arrays.copyOfRange(data, i, Math.min(i + kTxMaxCharacters, data.length));
                mBleManager.writeService(mUartService, KnownUUIDs.UUID_TX, chunk);
            }
        } else {
            Log.w(TAG, "Uart Service not discovered. Unable to send data");
        }
    }

    // OnStop, called right before the activity loses foreground focus.  Close the BTLE connection.
    @Override
    protected void onStop() {
        Log.i(TAG, "Stopping Bluetooth");
        if (mBleManager != null)
            mBleManager.close();
        super.onStop();
    }

    @Override
    public void onServicesDiscovered() {
        Log.i(TAG, "Services Discovered");
        stopScanning();
        //Get the UART Service
        mUartService = mBleManager.getGattService(KnownUUIDs.UUID_SERVICE);

        mBleManager.enableNotification(mUartService, KnownUUIDs.UUID_RX, true);

        if (mUartService != null)
        {
            Log.i(TAG, "UART service discovered");
            bluetoothStatus = "Ready";
        }
        else
        {
            startScan();
        }

    }

    @Override
    public void onConnected() {
        Log.i(TAG, "Connected!!");
        bluetoothStatus = "Connected";
        stopScanning();
    }

    @Override
    public void onConnecting() {
        Log.i(TAG, "Connecting...");
    }

    @Override
    public void onDisconnected() {
        //Prompt for reconnection
        bluetoothStatus = "Disconnected";
        startScan();
    }

    @Override
    public void onDataAvailable(BluetoothGattCharacteristic characteristic) {
        // UART RX
        if (characteristic.getService().getUuid().toString().equalsIgnoreCase(KnownUUIDs.UUID_SERVICE)) {
            if (characteristic.getUuid().toString().equalsIgnoreCase(KnownUUIDs.UUID_RX)) {
                final byte[] bytes = characteristic.getValue();
                final String data = new String(bytes, Charset.forName("UTF-8"));

                if (data.contains("H"))
                {
                    Player.receivedHit(Game.this, data);
                }
                else if (data.contains("S"))
                {
                    Player.shotFired();
                }
            }
        }
    }

    @Override
    public void onDataAvailable(BluetoothGattDescriptor descriptor) {
        Log.i(TAG, "onDataAvailable for descriptor");
    }

    @Override
    public void onReadRemoteRssi(int rssi) {

    }

    private void stopScanning() {
        // Stop scanning
        Log.i(TAG, "Stopping Scanning");
        if (mScanner != null) {
            Log.i(TAG, "Stopping Scanner");
            mScanner.stop();
            mScanner = null;
        }
    }

    public void scanForDevices(final UUID[] servicesToScan, final String deviceNameToScanFor) {
        Log.i(TAG, "Begin Scan....");
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
        }

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
                if (uuid.toString().equalsIgnoreCase(KnownUUIDs.UUID_SERVICE)) {
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

    public void selectDeviceDialog()
    {
        Log.i(TAG, "Select Device Dialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(Game.this);
        if (mScannedDevices != null) {
            if (mScannedDevices.size() > 0) {
                Log.i(TAG, "Devices found");
                String[] items = new String[mScannedDevices.size()];
                for (int i = 0; i < mScannedDevices.size(); i++) {
                    String deviceName = mScannedDevices.get(i).device.getName();
                    String deviceAddress = mScannedDevices.get(i).device.getAddress();
                    if (deviceName != null )
                    {
                        items[i] = deviceName;
                    }
                    else
                    {
                        items[i] = deviceAddress;
                    }
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

    public void sendTest(View v)
    {
        sendData("H5".getBytes());
    }

}
