package espertolabs.esperto_ble_watch;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

///service to handle connection to BLE device, and maintain that connection across desired activities

/*Modified from Android's BluetoothLeService*/

public class BLEService extends Service {
    public BLEService(){

    }

    private final static String TAG = BLEService.class.getSimpleName();

    private static final long SCAN_PERIOD = 30000;
    private Handler mHandler;
    private String filter; //TODO add
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress; //store device address
    private BluetoothGatt mBluetoothGatt; //device gatt database
    private int mConnectionState = STATE_DISCONNECTED;
    private ImageButton deviceButton;
    private TextView deviceText;
    private String [] userData;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    //TODO:: update with actual UUIDS


 /*
    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);*/

    public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID RX_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        intentAction = ACTION_GATT_CONNECTED;
                        mConnectionState = STATE_CONNECTED;
                        broadcastUpdate(intentAction);
                        Log.i(TAG, "Connected to GATT server.");
                        Log.i(TAG, "Attempting to start service discovery:" +
                                mBluetoothGatt.discoverServices());

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        intentAction = ACTION_GATT_DISCONNECTED;
                        mConnectionState = STATE_DISCONNECTED;
                        Log.i(TAG, "Disconnected from GATT server.");
                        broadcastUpdate(intentAction);
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                    } else {
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt,
                                                    BluetoothGattCharacteristic characteristic) {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                }

            };
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();

        intent.putExtra(EXTRA_DATA, data);

//        try{
//            String str = new String(data, "UTF-8"); // for UTF-8 encoding
//            Log.i("test",str);
//        }
//        catch(Exception e){
//            Log.w("Failed","Wrong encoding");
//        }
//
//
//
//        if (data != null && data.length > 0) {
//            final StringBuilder stringBuilder = new StringBuilder(data.length);
//            for(byte byteChar : data)
//                stringBuilder.append(String.format("%02X ", byteChar));
//                intent.putExtra(EXTRA_DATA, new String(data) + "\n" +
//                    stringBuilder.toString());
//        }
//        if(data == null) intent.putExtra(EXTRA_DATA, "null");
//        intent.putExtra("characteristic", characteristic.getUuid().toString());

        sendBroadcast(intent);
    }

    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE); /*access getSystemService to retrieve a reference to the Bluetooth Manager, which can be used to access adapter @M*/
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter(); /*accessing a function call specific to bluetooth manager class @M*/
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Device does not support Bluetooth.", Toast.LENGTH_SHORT).show();
            return false;
        }
        else if(!mBluetoothAdapter.isEnabled()){
            Toast.makeText(getApplicationContext(),"Bluetooth is not enabled.", Toast.LENGTH_SHORT).show();

        }
        mHandler = new Handler();


        return true;
    }

    //connect to Gatt server
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            return false;
        }

        if (address == null) {
            Log.w(TAG, "Unspecified BLE address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void writeRXCharacteristic(byte[] value)
    {
        BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
        BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
        RxChar.setValue(value);
        boolean status = mBluetoothGatt.writeCharacteristic(RxChar);

        while (status == false) {
            status = mBluetoothGatt.writeCharacteristic(RxChar);
        }

        Log.d(TAG, "write TXchar - status=" + status);
    }

    public void enableTXNotification()
    {
        BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
        BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(TX_CHAR_UUID);

        mBluetoothGatt.setCharacteristicNotification(TxChar,true);

        BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);

    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void scanFromSummary (boolean start) {
        if (start == true) {
            if (mBluetoothLeScanner == null) {
                mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeScanner.stopScan(mSummaryScanCallback);
                }
            }, 5000);

            mBluetoothLeScanner.startScan(mSummaryScanCallback);
        } else {
            mBluetoothLeScanner.stopScan(mSummaryScanCallback);

        }

    }

    //returns device addresses, else returns null
    public void scanForDevices(boolean start, ImageButton btn, TextView txt) {
        if(start == true) {
            if (mBluetoothLeScanner == null) {
                mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            }
            scanLeDevice(true, btn, txt);
        }
        //else disable scanning
        else{
            scanLeDevice(false, null, null);

        }
    }

    /*Scan for available BLE devices*/
    private void scanLeDevice(final boolean enable, ImageButton button, TextView text) {
        if (enable) {
            deviceButton = button;
            deviceText = text;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);

            mBluetoothLeScanner.startScan(mScanCallback);

        } else {
            mBluetoothLeScanner.stopScan(mScanCallback);
        }
    }


        // Device scan callback.
        //runs when a device makes it past the filters - in this case that is all BLE devices
        //result should contain advertisment data
        private ScanCallback mScanCallback = new ScanCallback() {

            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                ScanRecord record = result.getScanRecord();
                String name;
                if(record == null) return;
                else{
                    name = record.getDeviceName();
                }

                if (name == null) {
                    Log.i("onScanResult", "scanned device name null");
                } else {
                    if(isEspertoWatch(name)){
                        ImageButton device = deviceButton;
                        TextView txt = deviceText;
                        BLEService caller = new BLEService();
                        Callback callBack = new ScanActivity();
                        //because of the interface, the type is Callback even thought the new instance is the CallBackImpl class. This alows to pass different types of classes that have the implementation of CallBack interface
                        caller.register(callBack, result.getDevice(), device, txt);

                        Log.d("DEBUG:", "Address:" + result.getDevice().getAddress());
                        scanLeDevice(false, null,null);
                    }
                }
            }
        };

    private ScanCallback mSummaryScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            ScanRecord record = result.getScanRecord();
            String name;
            if(record == null) return;
            else{
                name = record.getDeviceName();
            }

            if (name == null) {
                Log.i("onScanResult", "scanned device name null");
            } else {
                if(isEspertoWatch(name)){
                    Log.i("onScanResult", "Esperto watch found");
                    scanFromSummary(false);
                }
            }
        }
    };

    //function to filter out force sensors
    public boolean isEspertoWatch(String name)
    {
        //TODO::update with new BLE chip
        String watchName = getString(R.string.esperto_device_name);
        return name.equals(watchName);
    }

    public class LocalBinder extends Binder {
        BLEService getService() {
            return BLEService.this;
         }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();


    //disconnect from device
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) {
            Log.d("DEBUG", "YOUR GATT REF IS NULL");
            return null;
        }
        return mBluetoothGatt.getServices();
    }


    //register to callback function
    public void register(Callback callback, BluetoothDevice device, ImageButton btn, TextView txt) {
        callback.displayDevice(device, btn, txt);
    }

}
