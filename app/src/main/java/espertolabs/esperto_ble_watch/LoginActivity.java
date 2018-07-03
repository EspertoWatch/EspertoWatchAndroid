package espertolabs.esperto_ble_watch;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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

//all cognito imports here

/**
 * Created by m2macmah on 3/17/2018.
 * Activity to sign-in or login-user
 * TODO:: Access SQLlite to sign in users who have signed in before
 * Now just have standard sign-in page
 */

public class LoginActivity extends AppCompatActivity {
    TextView usernameView;
    TextView passwordView;
    CognitoUserPool userPool;
    private ApiGatewayHandler handler;
    Boolean hasAttemptedLogin = false;
    int PERMISSION_ALL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Button fbRedirect = (Button) findViewById(R.id.facebookLogin);
        Button googleRedirect = (Button) findViewById(R.id.googleLogin);
        Button customLogin = (Button) findViewById(R.id.customLogin);
        usernameView = (TextView) findViewById(R.id.username); //accept custom username
        passwordView = (TextView) findViewById(R.id.password); //accept custom password

        userPool = new CognitoUserPool(getApplicationContext(),
                getString(R.string.cognito_userpool_id),
                getString(R.string.cognito_client_id), 
                getString(R.string.cognito_client_secret),
                Regions.fromName(getString(R.string.cognito_region)));

        CognitoUser currentUser = userPool.getCurrentUser();
        if(currentUser != null){
            currentUser.getSessionInBackground(authenticationHandler);
        }

        //check if Bluetooth is enabled
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.enable();
            }
        } else {
            Toast.makeText(this, "Bluetooth LE is not supported.", Toast.LENGTH_SHORT).show();
        }

        String[] PERMISSIONS = {Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.READ_CALL_LOG,
                                Manifest.permission.PROCESS_OUTGOING_CALLS,
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.READ_SMS,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_NETWORK_STATE};

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
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
    protected void onResume(){
        super.onResume();

    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    //TODO: MOVE COGNITO STUFF TO ITS OWN CLASS
    // Callback handler for the sign-in process
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
            getUserAuthentication(authenticationContinuation, userId);
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
            if(hasAttemptedLogin == true){
                alertAuthenticationFailure();
            }
            // Sign-in failed, check exception for the cause
        }
        @Override
        public void authenticationChallenge(ChallengeContinuation continuation) {
            //need to define this later
        }
    };

    public void checkCredentials(View v){
        //TODO:: modularize into handler

        boolean authenticated = false; //TODO:: update
        //read text input
        String usernameInput = usernameView.getText().toString();
        String passwordInput = passwordView.getText().toString();
        Log.d("username",usernameInput);
        Log.i("password",passwordInput);
        CognitoUser user = userPool.getUser(usernameInput);
        Log.d("cognitoUser", user.toString());

        hasAttemptedLogin = true;

        //Authenticate user
        user.getSessionInBackground(authenticationHandler);
    }

    private void getUserAuthentication(AuthenticationContinuation continuation, String username) {
        String usernameInput = usernameView.getText().toString();
        String passwordInput = passwordView.getText().toString();
        Log.d("username",usernameInput);
        Log.i("password",passwordInput);
        AuthenticationDetails authenticationDetails = new AuthenticationDetails(usernameInput, passwordInput, null);
        continuation.setAuthenticationDetails(authenticationDetails);
        continuation.continueTask();
    }

    //alert user of failed login attempt
    public void alertAuthenticationFailure(){
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

    public void createAccount(View v){
        //TODO:: send intent to register activity
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (isConnected) {
            Intent registerUser = new Intent(v.getContext(), RegisterActivity.class);
            startActivity(registerUser);
            finish();
        } else {
            Toast.makeText(this, "Connect to WiFi or data then please try again :)", Toast.LENGTH_SHORT).show();
        }
    }
}
