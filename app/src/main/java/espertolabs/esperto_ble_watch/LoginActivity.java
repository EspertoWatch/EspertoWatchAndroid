package espertolabs.esperto_ble_watch;

import android.content.DialogInterface;
import android.content.Intent;
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

import com.amazonaws.ClientConfiguration;
import com.amazonaws.mobile.client.AWSMobileClient;
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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
    DynamoDBMapper dynamoDBMapper; //map tables to Java classes
    AmazonDynamoDBClient dynamoDBClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Button fbRedirect= (Button)findViewById(R.id.facebookLogin);
        Button googleRedirect = (Button) findViewById(R.id.googleLogin);
        Button customLogin = (Button) findViewById(R.id.customLogin);
        usernameView = (TextView) findViewById(R.id.username); //accept custom username
        passwordView = (TextView) findViewById(R.id.password); //accept custom password

        // Instantiate a AmazonDynamoDBMapperClient
        dynamoDBClient = Region.getRegion(Regions.US_EAST_1)
                .createClient(AmazonDynamoDBClient.class,
                        AWSMobileClient.getInstance().getCredentialsProvider(),
                        new ClientConfiguration());


        this.dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(dynamoDBClient)
                .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                .build();

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
    //authenticate user with stored password


    public void checkCredentials(View v){
        //TODO:: modularize into handler

        boolean authenticated = false; //TODO:: update
        //read text input
        String usernameInput = usernameView.getText().toString();
        String passwordInput = passwordView.getText().toString();
        Log.d("username",usernameInput);
        Log.i("password",passwordInput);

        //Authenticate user
        authenticate(usernameInput, passwordInput);
    }

    //TODO:: abstract out
    //Authenticate user - confirm password and username combination
    private void authenticate(final String username, final String password){
        //query database for account

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
}

