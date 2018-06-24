package espertolabs.esperto_ble_watch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.DefaultRequest;
import com.amazonaws.Request;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;

//all cognito imports here
import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChooseMfaContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.NewPasswordContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedList;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.ghedeon.AwsInterceptor;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.OkHttpClient;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Button fbRedirect = (Button) findViewById(R.id.facebookLogin);
        Button googleRedirect = (Button) findViewById(R.id.googleLogin);
        Button customLogin = (Button) findViewById(R.id.customLogin);
        usernameView = (TextView) findViewById(R.id.username); //accept custom username
        passwordView = (TextView) findViewById(R.id.password); //accept custom password

        userPool = new CognitoUserPool(getApplicationContext(), getString(R.string.cognito_userpool_id), getString(R.string.cognito_client_id), getString(R.string.cognito_client_secret), Regions.fromName(getString(R.string.cognito_region)));

        CognitoUser currentUser = userPool.getCurrentUser();
        if(currentUser != null){
            currentUser.getSessionInBackground(authenticationHandler);
        }
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
                        Log.d("built_user", user.getName());

                        Intent displaySummary = new Intent(getApplicationContext(), SummaryActivity.class);
                        displaySummary.putExtra("deviceAddress", "D4:49:8C:44:48:82");
                        displaySummary.putExtra("user_obj", user);
                        startActivity(displaySummary);
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
        Intent registerUser = new Intent(v.getContext(), RegisterActivity.class);
        startActivity(registerUser);
        finish();
    }
}

