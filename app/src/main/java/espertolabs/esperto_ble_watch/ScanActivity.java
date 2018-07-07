package espertolabs.esperto_ble_watch;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler;
import com.amazonaws.regions.Regions;
import com.google.gson.Gson;
import com.skyfishjy.library.RippleBackground;

import org.json.JSONException;
import org.json.JSONObject;

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
    String watchName;
    String uniqueId;

    CognitoUserPool userPool;
    CognitoUser newUser;

    private ApiGatewayHandler handler;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_devices);
        watchName = getString(R.string.esperto_device_name);
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
            Toast.makeText(this, getString(R.string.ble_not_supported), Toast.LENGTH_SHORT).show();
            finish();
        }

        userPool = new CognitoUserPool(getApplicationContext(),
                                       getString(R.string.cognito_userpool_id),
                                       getString(R.string.cognito_client_id),
                                       getString(R.string.cognito_client_secret),
                                       Regions.fromName(getString(R.string.cognito_region)));

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
            } else {
                // The user has already been confirmed
                //CreateUserRecord();
            }
        }
        @Override
        public void onFailure(Exception exception) {
            Log.e("Registration error:",exception.toString());
            alertFailure(getResources().getString(R.string.sign_up_error));
        }
    };

    GenericHandler confirmationCallback = new GenericHandler() {
        @Override
        public void onSuccess() {
            newUser.getSessionInBackground(authenticationHandler);
        }

        @Override
        public void onFailure(Exception exception) {
            alertFailure(getResources().getString(R.string.wrong_confirmation_code));
        }
    };

    //TODO: MOVE COGNITO STUFF TO ITS OWN CLASS
    // Callback handler for the sign-in process
    AuthenticationHandler authenticationHandler = new AuthenticationHandler() {
        @Override
        public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
            Log.d("userSession", userSession.getUsername());
            final String userId = userSession.getUsername();
            CreateUserRecord(userId);
        }
        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {
            getUserAuthentication(authenticationContinuation, userId);
        }

        @Override
        public void getMFACode(MultiFactorAuthenticationContinuation multiFactorAuthenticationContinuation) {
        }

        @Override
        public void onFailure(Exception exception) {
            Log.e("Registration error:",exception.toString());
            alertFailure(getResources().getString(R.string.sign_up_error));
        }
        @Override
        public void authenticationChallenge(ChallengeContinuation continuation) {
        }
    };

    private void getUserAuthentication(AuthenticationContinuation continuation, String username) {
        AuthenticationDetails authenticationDetails = new AuthenticationDetails(userInfo[2], userInfo[3], null);
        continuation.setAuthenticationDetails(authenticationDetails);
        continuation.continueTask();
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

    public void CreateUserRecord(String userId){
        handler = new ApiGatewayHandler();
        new Thread(new Runnable(){
            @Override
            public void run(){
                JSONObject userJsonObject = new JSONObject();
                try {
                    userJsonObject.put("userId", userId);
                    userJsonObject.put("name", userInfo[0] + " " + userInfo[1]);
                    //NOTE: NEED TO MODIFY FUNCTION ON BACKEND TO ENSURE THAT DEVICE ADDR GETS POSTED
                    SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("deviceInfo", Context.MODE_PRIVATE);
                    String deviceAddr = mBLEService.deviceAddress;
                    if (deviceAddr != null) {
                        Log.i("DEVICE ADDR TO POST", deviceAddr);
                        userJsonObject.put("deviceAddress", deviceAddr);
                    } else {
                        Log.e("Account creation error:", "no device address");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String userJson = userJsonObject.toString();
                String response = handler.postUserInfo(userJson);
                Log.d("User_info_response", response);

                if(response != ""){
                    Gson g = new Gson();
                    UserAccount user = g.fromJson(response, UserAccount.class);
                    if(user != null){
                        //return to login page
                        Intent loginUser = new Intent(getApplicationContext(), LoginActivity.class);
                        startActivity(loginUser);
                        finish();
                    }
                }
            }
        }).start();
    }

    public void onSubmitCode(View v){
        //needs to send code to amazon
        newUser.confirmSignUpInBackground(confirmationCode.getText().toString(), false, confirmationCallback);
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

    //alert user of failed attempt
    public void alertFailure(String errorMessage){
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert);
        builder.setMessage(errorMessage).setTitle(getResources().getString(R.string.error_message));
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
        if(name == null) name = "unknown";

        //TODO:: modify for updated Esperto watch
        //update UI interface
        if(name.equals(watchName)){
            String deviceAddr = device.getAddress();
            //display icon
            btn.setVisibility(View.VISIBLE);
            txt.setText("Esperto watch found at:\n" + deviceAddr);
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
