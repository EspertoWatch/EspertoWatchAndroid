package espertolabs.esperto_ble_watch;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.regions.Regions;
import com.google.gson.Gson;


public class MainActivity extends AppCompatActivity {

    CognitoUserPool userPool;
    private ApiGatewayHandler handler;
    int PERMISSION_ALL = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 987;
    private boolean BLEEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
        }
        //Check whether BLE is supported
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, getString(R.string.ble_not_supported), Toast.LENGTH_LONG).show();
            finish();
        }

        //check if Bluetooth is enabled
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.enable();
            }
            BLEEnabled = true;
        } else {
            Toast.makeText(this, getString(R.string.ble_not_supported), Toast.LENGTH_LONG).show();
            finish();
        }

        String[] PERMISSIONS = {Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.PROCESS_OUTGOING_CALLS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_NETWORK_STATE};

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        userPool = new CognitoUserPool(getApplicationContext(),
                getString(R.string.cognito_userpool_id),
                getString(R.string.cognito_client_id),
                getString(R.string.cognito_client_secret),
                Regions.fromName(getString(R.string.cognito_region)));

        CognitoUser currentUser = userPool.getCurrentUser();
        if (BLEEnabled) {
            if(currentUser != null){
                currentUser.getSessionInBackground(authenticationHandler);
            } else {
                goToLogin();
            }
        }

        //TODO::check for internet permission problem -> used in offline mode
        /*
        //check if BLE is supported by the mobile device
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Warning: BLE is not supported by this device.", Toast.LENGTH_SHORT).show();
            finish();
        }*/
    }

    AuthenticationHandler authenticationHandler = new AuthenticationHandler() {
        @Override
        public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
            Log.d("userSession", userSession.getUsername());

            //save userId to local storage (will be necessary for get requests)
            final String userId = userSession.getUsername();
            SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("userId", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("USER_ID", userId);
            editor.putString("USER_TOKEN", userSession.getIdToken().getJWTToken());
            editor.commit();

            handler = new ApiGatewayHandler();

            new Thread(new Runnable(){
                @Override
                public void run(){
                    String response = handler.getUserInfo(userId);
                    Log.d("User_info_response", response);

                    if(response != ""){
                        Gson g = new Gson();
                        UserAccount user = g.fromJson(response, UserAccount.class);
                        if(user != null){
                            Log.d("built_user", user.getName());

                            Intent displaySummary = new Intent(getApplicationContext(), SummaryActivity.class);
                            displaySummary.putExtra("user_obj", user);
                            startActivity(displaySummary);
                        }
                    }
                }
            }).start();
        }
        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {
            getUserAuthentication(authenticationContinuation);
        }

        @Override
        public void getMFACode(MultiFactorAuthenticationContinuation multiFactorAuthenticationContinuation) {
            // Multi-factor authentication is required; get the verification code from user
            //multiFactorAuthenticationContinuation.setMfaCode(mfaVerificationCode);
            // Allow the sign-in process to continue
            //multiFactorAuthenticationContinuation.continueTask();
        }

        @Override
        public void onFailure(Exception exception) {
            //if current user cannot be authenticated, go to login screen
            if (BLEEnabled) {
                goToLogin();
            }
        }
        @Override
        public void authenticationChallenge(ChallengeContinuation continuation) {
            //need to define this later
        }
    };

    private void getUserAuthentication(AuthenticationContinuation continuation) {
        String usernameInput = "";
        String passwordInput = "";

        AuthenticationDetails authenticationDetails = new AuthenticationDetails(usernameInput, passwordInput, null);
        continuation.setAuthenticationDetails(authenticationDetails);
        continuation.continueTask();
    }

    private void goToLogin(){
        Intent login = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(login);
        finish();
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this, getString(R.string.location_requirec), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

}

