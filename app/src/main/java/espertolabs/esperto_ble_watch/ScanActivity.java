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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest.permission;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.common.collect.Table;
import com.skyfishjy.library.RippleBackground;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;

import org.w3c.dom.Text;

import java.util.UUID;


public class ScanActivity extends AppCompatActivity implements Callback {
    BLEService mBLEService;
    boolean mServiceBound = false;
    String[] devices;
    ImageButton deviceButton;
    TextView deviceText;
    TextView enterCode;
    EditText confirmationCode;
    Button submitButton;
    String[] userInfo;
    String watch_name = "Esperto";
    String device_addr;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 987;
    String uniqueId;

    CognitoUserPool userPool;
    CognitoUser newUser;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_devices);
        watch_name = getString(R.string.esperto_device_name);
        deviceButton = findViewById(R.id.watchDevice);
        deviceText = findViewById(R.id.deviceName);
        enterCode = findViewById(R.id.enter_code);
        confirmationCode = findViewById(R.id.confirmation_code);
        submitButton = findViewById(R.id.submit_button);
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
            Toast.makeText(this, "Bluetooth LE is not supported.", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
        }

        userPool = new CognitoUserPool(getApplicationContext(), getString(R.string.cognito_userpool_id), getString(R.string.cognito_client_id), getString(R.string.cognito_client_secret), Regions.fromName(getString(R.string.cognito_region)));

        //check if Bluetooth is enabled
        Intent intent = new Intent(this, BLEService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    SignUpHandler signUpCallback = new SignUpHandler() {

        @Override
        public void onSuccess(CognitoUser cognitoUser, boolean userConfirmed, CognitoUserCodeDeliveryDetails cognitoUserCodeDeliveryDetails) {
            // Sign-up was successful
            // Check if this user (cognitoUser) needs to be confirmed
            if(!userConfirmed) {
                // This user must be confirmed and a confirmation code was sent to the user
                // cognitoUserCodeDeliveryDetails will indicate where the confirmation code was sent
                // Get the confirmation code from user
                newUser = cognitoUser;
                enterCode.setVisibility(View.VISIBLE);
                confirmationCode.setVisibility(View.VISIBLE);
                submitButton.setVisibility(View.VISIBLE);
            }
            else {
                // The user has already been confirmed
                CreateUserRecord();
            }
        }

        @Override
        public void onFailure(Exception exception) {
            alertSignUpFailure();
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this, "Location permissions are required for Bluetooth scanning.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */

    //TODO:: move into DBHandler to utilize brigde pattern
    public void createAccounts(){
        uniqueId = UUID.randomUUID().toString();
        // Create a CognitoUserAttributes object and add user attributes
        CognitoUserAttributes userAttributes = new CognitoUserAttributes();

        userPool.signUpInBackground(userInfo[2], userInfo[3], userAttributes, null, signUpCallback);
    }

    public void CreateUserRecord(){
        //TODO: POST TO USER TABLE VIA APIGATEWAY
        /*
        //return to login page
        Intent loginUser = new Intent(v.getContext(), LoginActivity.class);
        startActivity(loginUser);
        finish();
        */
    }

    public void onSubmitCode(){
        //TODO: on click method for submitButton
        //needs to send confirmation code to amazon
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
            if (mBLEService.initialize()) {
                //start scanning
                mBLEService.scanForDevices(true, deviceButton, deviceText);
            }
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

    //alert user of failed login attempt
    public void alertSignUpFailure(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert);
        builder.setMessage(getResources().getString(R.string.try_again))
                .setTitle(getResources().getString(R.string.error_message));

        builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        mBLEService.scanForDevices(false, null, null);
        //unbind service
        // mBLEService.unbindService(mConnection);
    }

    //change the content of this function
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConnection!= null) {
            unbindService(mConnection);
        }
        mBLEService.scanForDevices(false, null, null);
        mBLEService = null;
//        mBLEService.unbindService(mConnection);

    }

    //callback function from BLEService - if an Esperto Device is found
    //TODO:: In future move this to scan activity
    public void displayDevice(BluetoothDevice device, ImageButton btn, TextView txt){
        //display device address
        String name = device.getName();
        device_addr = device.getAddress();
        if(name == null) name = "unknown";

        //TODO:: modify for updated Esperto watch
        //update UI interface
        if(name.equals(watch_name)){
            //display icon
            btn.setVisibility(View.VISIBLE);
            txt.setText("Esperto watch found at:\n" + device_addr);
            txt.setVisibility(View.VISIBLE);
        }
    }
    //onclick for device button
    //expand to multiple users
    public void selectDevice(View v){
        //add to user account and send intent to login activity
        createAccounts(); //create user account with associated device address
    }


}

interface Callback{
    void displayDevice(BluetoothDevice device, ImageButton btn, TextView txt);
}
