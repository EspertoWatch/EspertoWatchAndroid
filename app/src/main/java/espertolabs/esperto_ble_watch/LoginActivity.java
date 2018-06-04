package espertolabs.esperto_ble_watch;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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

    //TODO:: move
    // Declare a DynamoDBMapper object
    //DynamoDBMapper dynamoDBMapper; //map tables to Java classes
    //AmazonDynamoDBClient dynamoDBClient;

    CognitoUserPool userPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Button fbRedirect = (Button) findViewById(R.id.facebookLogin);
        Button googleRedirect = (Button) findViewById(R.id.googleLogin);
        Button customLogin = (Button) findViewById(R.id.customLogin);
        usernameView = (TextView) findViewById(R.id.username); //accept custom username
        passwordView = (TextView) findViewById(R.id.password); //accept custom password
        /*
        // Instantiate a AmazonDynamoDBMapperClient
        dynamoDBClient = Region.getRegion(Regions.US_EAST_1)
                .createClient(AmazonDynamoDBClient.class,
                        AWSMobileClient.getInstance().getCredentialsProvider(),
                        new ClientConfiguration());


        this.dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(dynamoDBClient)
                .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                .build();
        */
        userPool = new CognitoUserPool(getApplicationContext(), getString(R.string.cognito_userpool_id), getString(R.string.cognito_client_id), getString(R.string.cognito_client_secret), Regions.fromName(getString(R.string.cognito_region)));

        //send heart rate request - just for prototyping for now
        BasicAWSCredentials credentials = new BasicAWSCredentials("AKIAI5TPBKZTQJBU523Q", "TTRcNw6ch3OD0wIwD+rRWQAY0pudufuPIImwqUoA");
        AWSCredentialsProvider credentialsProvider = new StaticCredentialsProvider(credentials);

        AwsInterceptor awsInterceptor = new AwsInterceptor(credentialsProvider, "execute-api", "us-east-1");
        final OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(awsInterceptor)
                .build();

        new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    okhttp3.Request request2 = new okhttp3.Request.Builder()
                            .url("https://75pp5et7e7.execute-api.us-east-1.amazonaws.com/prod/heartRate/47c2f191-92b0-4a69-9050-ece261e82299/")
                            .addHeader("Content-Type", "application/x-www-form-urlencoded")
                            .build();
                    okhttp3.Response response = null;
                    response = client.newCall(request2).execute();
                    String body = response.body().string();
                    Log.d("resp_body", "response " + body);
                } catch (Exception e) {
                    Log.d("resp_error", "error " + e);
                }
            }
        }).start();
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

    // Callback handler for the sign-in process
    AuthenticationHandler authenticationHandler = new AuthenticationHandler() {
        @Override
        public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
            Log.d("userSession", userSession.toString());
            //todo: get rid of dummy vals
            Intent displaySummary = new Intent(getApplicationContext(), SummaryActivity.class);
            displaySummary.putExtra("deviceAddress", "defaultAddress");
            displaySummary.putExtra("firstName", "defaultFirstName");
            displaySummary.putExtra("lastName", "defaultLastName");
            displaySummary.putExtra("username", userSession.getUsername());
            displaySummary.putExtra("goalSetting", "default");
            startActivity(displaySummary);
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
            alertAuthenticationFailure();
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

        //Authenticate user
        user.getSessionInBackground(authenticationHandler);
        //authenticate(usernameInput, passwordInput);
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
        builder.setMessage(R.string.try_again)
                .setTitle(R.string.error_message);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
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

    /*
    //TODO:: abstract out
    //Authenticate user - confirm password and username combination
    private void authenticate(final String username, final String password){
        //query database for account
        //todo: use cognito instead

        boolean test = false;
        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                UserAccount user = new UserAccount();
                user.setUsername(username);

                Map<String, AttributeValue> expressionAttributeValues = new HashMap<String, AttributeValue>();
                expressionAttributeValues.put(":p", new AttributeValue().withS(password));

                DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression()
                        .withHashKeyValues(user)
                        .withFilterExpression("password = :p")
                        .withExpressionAttributeValues(expressionAttributeValues)
                        .withConsistentRead(false);

                //query database
               // PaginatedList<UserAccount> result = dynamoDBMapper.query(UserAccount.class, queryExpression);
                 List<UserAccount> result = dynamoDBMapper.query(UserAccount.class, queryExpression);

                Gson gson = new Gson();
                StringBuilder stringBuilder = new StringBuilder();
                Type listType = new TypeToken<List<String>>() {}.getType();
                List<String> target = new LinkedList<String>();

                UserAccount signedInUser = new UserAccount();

                // Loop through query results
                for (int i = 0; i < result.size(); i++) {
                    Log.i("result", result.get(i).toString());
                    String jsonFormOfItem = gson.toJson(result.get(i));

                    signedInUser = gson.fromJson(jsonFormOfItem, UserAccount.class);
                }

                // Add your code here to deal with the data result
                Log.d("Query result: ", stringBuilder.toString());

                if (result.isEmpty()) {
                    Log.i("Login", "User is not authenticated");
                    runOnUiThread (new Thread(new Runnable() {
                        public void run() {
                           alertAuthenticationFailure();
                        }
                    }));

                    //TODO:: move to a seperate handler
                    //alertAuthenticationFailed();

                }
                else{
                    //If user has been authenticated
                    //Transition to summary page
                    Intent displaySummary = new Intent(getApplicationContext(), SummaryActivity.class);
                    //TODO:: pass in authentication token here
                    displaySummary.putExtra("deviceAddress", signedInUser.getDeviceAddress());
                    displaySummary.putExtra("firstName", signedInUser.getFirstName());
                    displaySummary.putExtra("lastName", signedInUser.getLastName());
                    displaySummary.putExtra("username", signedInUser.getUsername());
                    displaySummary.putExtra("goalSetting", signedInUser.getGoalSetting());
                    startActivity(displaySummary);
                    finish();

                }
            }
        }).start();
    }
    */
}

