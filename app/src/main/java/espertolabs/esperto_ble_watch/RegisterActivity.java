package espertolabs.esperto_ble_watch;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * A screen for user registration
 */
public class RegisterActivity extends AppCompatActivity {
    EditText fNameView;
    EditText lNameView;
    EditText passwordView;
    EditText passwordDupView;
    EditText usernameView;
//    String[] userInfo = new String[5]; //{firstname, lastname, username, password, goalPreference}
    String[] userInfo = new String[4]; //{firstname, lastname, username, password}
    Button goalButton;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 987;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        setSupportActionBar((Toolbar) findViewById(R.id.my_toolbar));
        //redirect to login after registration
        // Set up the login form.
        //TODO:: google AutoCompleteTextView
//        goalButton = (Button) findViewById(R.id.enterGoalSetting);
        fNameView = (EditText) findViewById(R.id.first_name);
        lNameView = (EditText) findViewById(R.id.last_name);
        usernameView = (EditText) findViewById(R.id.username);
        passwordView = (EditText) findViewById(R.id.password);
        passwordDupView = (EditText) findViewById(R.id.password_dup);

         //TODO:: instantiate question mark
/*        ImageView checkMark = (ImageView) findViewById(R.id.check);
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);*/
        //checkMark.setVisibility(View.VISIBLE);
        //checkMark.animate().setDuration(shortAnimTime);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            // need location enabled for BLE scanning
            locationStatusCheck();
        }

    }

    public void locationStatusCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();

        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your location is disabled, do you want to enable it in order to search for an Esperto watch?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this, "Location permissions are required for Bluetooth scanning", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    //public function call for unit testing
    public boolean checkPassword(String p1, String p2){
        return isPasswordValid(p1,p2);
    }

    private boolean isPasswordValid(String password, String passwordDup) {
        //TODO:: add more specifications regarding password requirements
        //TODO:: add security and encyrption

        if (password.equals(passwordDup)) return true;
        else return false;
    }

    //when register button is clicked
    public void registerUser(View v) {
        //read inputs
        boolean completed = validateTextInputs();
        if(completed == false) Log.i("test", "Failed");
        //else redirect to login page
        //TODO:: remove the passage of data to login page - have login passage retrieve data
        if (!completed) return;
        else {
            Intent scanDevices = new Intent(v.getContext(), ScanActivity.class);
            scanDevices.putExtra("Account", userInfo);
            startActivity(scanDevices);
            finish();
        }
    }

    private boolean validateTextInputs() {
        boolean filled = true;

        //set first name
        String fName = fNameView.getText().toString();
        if (TextUtils.isEmpty(fName)) {
            filled = false;
            fNameView.setError("First name required.");
        } else userInfo[0] = fName;

        //set last name
        String lName = lNameView.getText().toString();
        if (TextUtils.isEmpty(lName)) {
            filled = false;
            lNameView.setError("Last name required.");
        } else userInfo[1] = lName;

        //set username
        String strUsername = usernameView.getText().toString();
        if (TextUtils.isEmpty(strUsername)) {
            filled = false;
            usernameView.setError("Username required.");
        } else userInfo[2] = strUsername;

        //set password
        String password = passwordView.getText().toString();
        if (TextUtils.isEmpty(password)) {
            filled = false;
            passwordView.setError("Password required.");
        } else {
            if (isPasswordValid(password, passwordDupView.getText().toString()))
                userInfo[3] = password;
            else {
                passwordDupView.setError("Passwords don't match.");
                filled = false;
            }
        }

        //check if goal settings have been set
//        String goalTxt = goalButton.getText().toString();
//        if(goalTxt.equals(getResources().getString(R.string.goal_setting))) {
//            filled = false;
//        }
//        //TODO:: add custom goal settings here - set initially to default
//        else userInfo[4] = "DEFAULT";

        return filled;
    }


    //update goal setting
    public void updateGoalSetting(View v) {
        Button b = (Button)v;
        //TODO:: alternate between goal preferences
        //TODO:: use to determine whether to prompt the user with a survey
        b.setText(getResources().getString(R.string.default_setting));
        b.setBackgroundColor(getResources().getColor(R.color.accent));
    }

    @Override
    public void onBackPressed() {
        Intent login = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(login);
        finish();
    }


}


