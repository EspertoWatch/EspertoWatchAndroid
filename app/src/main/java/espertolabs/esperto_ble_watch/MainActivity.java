package espertolabs.esperto_ble_watch;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        ImageButton startButton = (ImageButton) findViewById(R.id.startButton);
        startButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startScan = new Intent(v.getContext(), LoginActivity.class);
                startActivity(startScan);
                finish();
            }

        });
        */

        userPool = new CognitoUserPool(getApplicationContext(),
                getString(R.string.cognito_userpool_id),
                getString(R.string.cognito_client_id),
                getString(R.string.cognito_client_secret),
                Regions.fromName(getString(R.string.cognito_region)));

        CognitoUser currentUser = userPool.getCurrentUser();
        if(currentUser != null){
            Log.d("CURRENTUSER", currentUser.toString());
            currentUser.getSessionInBackground(authenticationHandler);
        }
        else{
            authenticationFailure();
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
            authenticationFailure();
            // Sign-in failed, check exception for the cause
        }
        @Override
        public void authenticationChallenge(ChallengeContinuation continuation) {
            //need to define this later
        }
    };

    private void getUserAuthentication(AuthenticationContinuation continuation, String username) {
        //String usernameInput = usernameView.getText().toString();
        //String passwordInput = passwordView.getText().toString();
        //Log.d("username",usernameInput);
        //Log.i("password",passwordInput);

        String usernameInput = "";
        String passwordInput = "";

        AuthenticationDetails authenticationDetails = new AuthenticationDetails(usernameInput, passwordInput, null);
        continuation.setAuthenticationDetails(authenticationDetails);
        continuation.continueTask();

        /*
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (isConnected) {
            AuthenticationDetails authenticationDetails = new AuthenticationDetails(usernameInput, passwordInput, null);
            continuation.setAuthenticationDetails(authenticationDetails);
            continuation.continueTask();
        } else {
            Toast.makeText(this, getString(R.string.internet_required), Toast.LENGTH_SHORT).show();
        }
        */
    }

    private void authenticationFailure(){
        Log.d("GOTOLOGIN", "Not logged in");
        Intent startScan = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(startScan);
        finish();
    }

}

