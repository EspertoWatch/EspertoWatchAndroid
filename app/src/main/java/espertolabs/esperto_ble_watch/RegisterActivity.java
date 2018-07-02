package espertolabs.esperto_ble_watch;

import android.app.Dialog;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.skyfishjy.library.RippleBackground;

import java.io.Serializable;

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


