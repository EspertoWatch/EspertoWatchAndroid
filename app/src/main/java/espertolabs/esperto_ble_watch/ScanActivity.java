package espertolabs.esperto_ble_watch;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.common.collect.Table;
import com.skyfishjy.library.RippleBackground;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;

import org.w3c.dom.Text;


public class ScanActivity extends AppCompatActivity implements Callback {
    BLEService mBLEService;
    boolean mServiceBound = false;
    String[] devices;
    ImageButton deviceButton;
    TextView deviceText;
    String[] userInfo;

    //TODO:: move
    // Declare a DynamoDBMapper object
    DynamoDBMapper dynamoDBMapper; //map tables to Java classes
    AmazonDynamoDBClient dynamoDBClient;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_devices);
        setSupportActionBar((Toolbar) findViewById(R.id.device_toolbar));
        deviceButton = (ImageButton)findViewById(R.id.watchDevice);
        deviceText= (TextView)findViewById(R.id.deviceName);

        //grab intent info
        Intent registerIntent = getIntent();
        //grab user data
        userInfo = registerIntent.getStringArrayExtra("Account"); //grab string array
        //userAccount = new Accounts(newUser);
        //display ripple animation
        //TODO:: set scanning to start with click of button - use to show timeout periods
        startAnimation();

        //Check whether BLE is supported
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth is not supported.", Toast.LENGTH_SHORT).show();
            finish();
        }


        // Instantiate a AmazonDynamoDBMapperClient
        dynamoDBClient = Region.getRegion(Regions.US_EAST_1)
                .createClient(AmazonDynamoDBClient.class,
                        AWSMobileClient.getInstance().getCredentialsProvider(),
                        new ClientConfiguration()
                );


        this.dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(dynamoDBClient)
                .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                .build();

        //check if Bluetooth is enabled
        Intent intent = new Intent(this, BLEService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */

    //TODO:: move into DBHandler to utilize brigde pattern
    public void createAccounts(String [] userData, String deviceAddress){
        final UserAccount newUser = new UserAccount();
        newUser.setUsername(userData[2]);
        newUser.setPassword(userData[3]);
        newUser.setDeviceAddress(deviceAddress);
        newUser.setGoalSetting(userData[4]);
        newUser.setFirstName(userData[0]);
        newUser.setLastName(userData[1]);
        //make asynchronous method call to synchronous DynamoDB
        Runnable runnable = new Runnable() {
            public void run() {
                //add account to database
                dynamoDBMapper.save(newUser);
                //move to login screen
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }


    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BLEService.LocalBinder binder = (BLEService.LocalBinder) service;
            mBLEService = binder.getService();
            mServiceBound = true;
            //initialize BLE
            boolean success = mBLEService.initialize();
            //start scanning
            mBLEService.scanForDevices(true, deviceButton, deviceText);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mServiceBound = false;
        }
    };

    private void startAnimation() {
        final RippleBackground rippleBackground = (RippleBackground) findViewById(R.id.content);
        ImageView imageView = (ImageView) findViewById(R.id.centerImage);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rippleBackground.startRippleAnimation();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        startAnimation();

        if (mServiceBound != true) {
            Intent intent = new Intent(this, BLEService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }


    // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
    // fire an intent to display a dialog asking the user to grant permission to enable it.
     /*   if (!mBluetoothAdapter.isEnabled()) { //I'm pretty sure you don't need two of these
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }*/

    /*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //User chose not to enable BLE
        if(requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode,resultCode,data);
    }*/

    @Override
    protected void onPause() {
        super.onPause();
        mBLEService.scanForDevices(false, null, null);
        //unbind service
        // mBLEService.unbindService(mConnection);
    }

    //change the content of this function
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBLEService.scanForDevices(false, null, null);
        mBLEService = null;
//        mBLEService.unbindService(mConnection);

    }

    //callback function from BLEService - if an Esperto Device is found
    //TODO:: In future move this to scan activity
    public void displayDevice(BluetoothDevice device, ImageButton btn, TextView txt){
        //display device address
        String name = device.getName();
        if(name == null) name = "unknown";
        else name = name.toString();

        //TODO:: modify for updated Esperto watch
        //update UI interface
        if(name.equals(R.string.esperto_device_name)){
            //display icon
            btn.setVisibility(View.VISIBLE);
            txt.setText(device.getAddress());
            txt.setVisibility(View.VISIBLE);
        }
    }
    //onclick for device button
    //expand to multiple users
    public void selectDevice(View v){
        //add to user account and send intent to login activity
        createAccounts(userInfo, deviceText.getText().toString()); //create user account with associated device address

        //return to login page
        Intent loginUser = new Intent(v.getContext(), LoginActivity.class);
        startActivity(loginUser);
        finish();
    }


}

interface Callback{
    void displayDevice(BluetoothDevice device, ImageButton btn, TextView txt);
}
