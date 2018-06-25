package espertolabs.esperto_ble_watch;

import android.Manifest;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneStateListener;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.Utils;
import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataBufferObserver;
import com.google.common.base.Utf8;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.nio.charset.StandardCharsets;
import java.util.Set;


//TODO:: pass in internet information to adjust views
//TODO:: connect to BLE device here
public class SummaryActivity extends AppCompatActivity implements Observer {


    private TextView mTextMessage;
    private UserAccount user = new UserAccount();
    private HeartRate userHR = new HeartRate();
    private StepCount userSteps = new StepCount();

    private BLEService mBLEService;
    private boolean mServiceBound;
    private boolean mConnected = false;
    private String dataCharacteristicUUID = "00002a05-0000-1000-8000-00805f9b34fb";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 987;
    private BluetoothGattCharacteristic dataCharacteristic;
    //DynamoDBMapper dynamoDBMapper; //map tables to Java classes
    //AmazonDynamoDBClient dynamoDBClient;
    private boolean summaryDisplay = false;
    private boolean detailedHeart = false;
    private boolean detailedStep = false;
    TextView messageUser;
    TextView bleConnection;

    //Graph UI objects
    LineChart heartChart;
    BarChart stepChart;
    ViewFlipper flipper;

    //Summary values
    TextView hr_current;
    TextView steps_current;

    //instantiate api gateway handler
    final ApiGatewayHandler handler = new ApiGatewayHandler();
    String userId;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_profile:
                    displaySummary();
                    summaryDisplay = true;
                    detailedHeart = false;
                    detailedStep = false;
                   // TODO updateUI("Summary");
                    return true;
                case R.id.navigation_heart:
                    displayHeart();
                    detailedHeart = true;
                    summaryDisplay = false;
                    detailedStep = false;
                    // TODO updateUI("Heart Rate");
                    return true;
                case R.id.navigation_steps:
                    displaySteps();
                    detailedHeart = false;
                    detailedStep = true;
                    summaryDisplay = false;
                    // TODO updateUI("Step Count");
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(),R.drawable.settings_ic);

        messageUser = (TextView) findViewById(R.id.messageUser);
        bleConnection = (TextView) findViewById(R.id.bleConnection);
        heartChart = (LineChart)findViewById(R.id.heartChart); //used to display daily HR
        stepChart = (BarChart) findViewById(R.id.stepChart); //used to display daily stepCount
        flipper = (ViewFlipper)findViewById(R.id.vf);
        hr_current = (TextView) findViewById(R.id.heartRateNum);
        steps_current = (TextView) findViewById(R.id.stepCount);

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        //Check whether BLE is supported
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth LE is not supported.", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
        }

        //check if Bluetooth is enabled
        Intent intent = new Intent(this, BLEService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        registerReceiver(mCallReceiver, new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));
        registerReceiver(mSMSReceiver, new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("userId", Context.MODE_PRIVATE);
        userId = sharedPref.getString("USER_ID", "");

        //retrieve intent
        Intent userIntent = getIntent();
        user = (UserAccount) getIntent().getSerializableExtra("user_obj");

        TextView logoutButton = (TextView) findViewById(R.id.text_logout);

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
            }
        });

        //retrieve data
        getHRDB();
        getStepDB();
        greetUser();
        //TODO: for now we are just setting step/hr to 0 if no data
        //TODO: need to replace with some user friendly msg
    }

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

    public void greetUser(){
        String greetText = "Welcome " + user.getName() + "!";
        messageUser.setText(greetText);
    }

    //TODO:: add a loading screen to keep user occupied before data display
    private void displaySummary(){
        flipper.setDisplayedChild(3);
        //using default goals for now

        //NOT SURE WHY WE ARE SETTING STUFF TO ZERO IF WE HAVE NO LT DATA
        /*
        if(userHR.getUsername() == null || userSteps.getUsername() == null){
            Log.i("Fail", "user has no long term data");
            hr_current.setText("0");
            steps_current.setText("0");
            return; //just displaying random stand in data if not long term user
        }
        */

        hr_current.setText(Integer.toString(userHR.getCurrentHR()));
        steps_current.setText(Integer.toString(userSteps.getCurrentSteps()));
    }

    private void displayHeart(){
        flipper.setDisplayedChild(1);
        if(userHR.getUsername() != null){

            List<Entry> entries = retrieveHeartRateData();
            LineDataSet dataSet = new LineDataSet(entries, "Heart Rate"); // add entries to dataset

            dataSet.setDrawFilled(true);
            if (Utils.getSDKInt() >= 18) {
                // fill drawable only supported on api level 18 and above
                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);
                dataSet.setFillDrawable(drawable);
            } else {
                dataSet.setFillColor(Color.BLACK);
            }
            dataSet.setColors(getResources().getColor(R.color.colorPrimary));
            dataSet.setValueTextColor(getResources().getColor(R.color.colorPrimary)); // styling
            LineData lineData = new LineData(dataSet);
            YAxis leftAxis = heartChart.getAxisLeft();
            leftAxis.setDrawAxisLine(false);
            leftAxis.setDrawGridLines(false);
            leftAxis.setDrawZeroLine(false);
            heartChart.getXAxis().setDrawGridLines(false);
            heartChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            heartChart.getAxisRight().setEnabled(false);
            YAxis rightAxis = heartChart.getAxisLeft();
            leftAxis.setDrawAxisLine(false);
            leftAxis.setDrawGridLines(false);
            heartChart.getDescription().setEnabled(false);
            heartChart.getLegend().setEnabled(false);

            heartChart.setData(lineData);
            heartChart.invalidate(); // refresh


        }
    }

    //Retrieve data from AWS services
    //If no data - state "no data available"
    private List<Entry> retrieveHeartRateData(){
       //Retrieve data here
        List<Entry> entries = new ArrayList<Entry>();
        // TODO: change from Set since it does not allow for duplicate entries
        Set<Integer> dailyHR = userHR.getDailyHR();
        int timeCounter = 8; //start at 8am TODO:: add actual times once I have the esperto watch (24 h clock)
        for(Integer i: dailyHR){
            // turn your data into Entry objects
            entries.add(new Entry(timeCounter, i)); //wrap each data point into Entry objects
            timeCounter++;
        }
        return entries;
    }

    //TODO:: adjust color of graph if goal achieved
    private void displaySteps(){
        flipper.setDisplayedChild(2);
//        if(!user.getUsername().equals("mmacmahon")){return;}
        BarDataSet dataSet = retrieveStepData();
        ArrayList<IBarDataSet> dataSets = new ArrayList<IBarDataSet>();
        dataSet.setColors(getResources().getColor(R.color.navbar));
        dataSet.setValueTextColor(getResources().getColor(R.color.colorPrimary)); // styling
        BarData barData = new BarData(dataSet);
        YAxis leftAxis = stepChart.getAxisLeft();
        leftAxis.setDrawAxisLine(false);
        leftAxis.setDrawGridLines(false);
        leftAxis.setDrawZeroLine(false);
        stepChart.getXAxis().setDrawGridLines(false);
        stepChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        stepChart.getAxisRight().setEnabled(false);
        YAxis rightAxis = stepChart.getAxisLeft();
        leftAxis.setDrawAxisLine(false);
        leftAxis.setDrawGridLines(false);
        dataSet.setDrawValues(false);

        stepChart.getDescription().setEnabled(false);
        stepChart.getLegend().setEnabled(false);
        dataSets.add(dataSet);
        stepChart.setData(barData);
        stepChart.invalidate(); // refresh
    }

    //call database to retrieve heart data
    //TODO:: store in a local data class so I don't have to keep requesting the data
    private BarDataSet retrieveStepData(){
        //retrieve step data and format output
        ArrayList xVals = new ArrayList();
        ArrayList<BarEntry> yVals = new ArrayList<BarEntry>();
        float counter = 0;
        float timeCounter = 8; //start at 8am TODO:: add actual times once I have the esperto watch (24 h clock)
        // TODO: change from Set since it does not allow for duplicate entries
        Set<Integer> dailySteps = userSteps.getDailySteps();
        for(Integer i:dailySteps){
            // turn your data into Entry objects
            int hours = (int)timeCounter;
            int min = (int)(timeCounter - hours) *60;
            String time = String.format("%d:%02d",hours, min).toString();

            yVals.add(new BarEntry(counter, i)); //wrap each data point into Entry objects
            xVals.add(time);
            timeCounter = timeCounter + (float)0.5;
            counter++;
        }

        BarDataSet dataSet = new BarDataSet(yVals, "Step Count");
        return dataSet;
     //   BarData data = new BarData(xVals.toString(), dataSet);
    }

    //check if user is new - TODO
    private boolean checkNewUser(){
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    //handle action bar clicks here
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        //TODO:: add settings page
        if (id == R.id.settings_icon) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BLEService.LocalBinder binder = (BLEService.LocalBinder) service;
            mBLEService = binder.getService();
            mServiceBound = true;
            if (mBLEService.initialize()) {
                mBLEService.connect(user.getDeviceAddress());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mServiceBound = false;
        }
    };

    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device. This can be a
    // result of read or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BLEService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.i("Update", "Device connected");
                mConnected = true;
                invalidateOptionsMenu();
                bleConnection.setText("Watch connected");
                bleConnection.setTextColor(Color.GREEN);
            } else if (BLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.i("Update", "Device disconnected");
                mConnected = false;
                Toast.makeText(getApplicationContext(), "Esperto Watch disconnected. ", Toast.LENGTH_SHORT).show();
                invalidateOptionsMenu();

                Date now = new Date();

                SimpleDateFormat ft = new SimpleDateFormat ("hh:mm:ss a dd/MM/YYYY");
                String timeString = ft.format(now);

                bleConnection.setText("Watch disconnected\nLast connected at " + timeString);
                bleConnection.setTextColor(Color.RED);
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBLEService.connect(user.getDeviceAddress());
                    }
                }, 5000);
                //clearUI();
            } else if (BLEService.
                    ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.i("Update", "Services discovered");
                mBLEService.enableTXNotification();

                List<BluetoothGattService> gattServices = mBLEService.getSupportedGattServices();

                if (gattServices == null) {
                    return;
                }
                Log.e("Update", "onReceive: " + gattServices.toString());

                for (BluetoothGattService gattService : gattServices) {

                    List<BluetoothGattCharacteristic> gattCharacteristics =
                            gattService.getCharacteristics();
                    // Loops through available Characteristics.
                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                        String uuid = gattCharacteristic.getUuid().toString();
                        //if(uuid.equals(dataCharacteristicUUID)){
                        dataCharacteristic = gattCharacteristic;
                        mBLEService.readCharacteristic(gattCharacteristic);
                        //}
                        Log.d("DEBUG", "PRINT UUIDS: " + uuid);
                    }
                }

                // Show all the supported services and characteristics on the
                // user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());

                // Send updated time and date every time summary is opened
                Date now = new Date();

                SimpleDateFormat ft = new SimpleDateFormat ("hh:mm:ssa");
                String timeString = ft.format(now);
                byte[] send = timeString.getBytes(StandardCharsets.UTF_8);
                mBLEService.writeRXCharacteristic(send);

                ft = new SimpleDateFormat ("dd/MM/YYYY");
                timeString = ft.format(now);
                timeString = "D" + timeString;
                send = timeString.getBytes(StandardCharsets.UTF_8);
                mBLEService.writeRXCharacteristic(send);
            } else if (BLEService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] rcv = intent.getByteArrayExtra(BLEService.EXTRA_DATA);
                Log.i("DATA","AVAIL");
                Log.i("DATA RCV'D",Arrays.toString(rcv));
                int heartRate = rcv[0];
                int stepCount = ( ( rcv[1] & 0xFF ) << 8 ) | ( rcv[0] );

                Log.i("Heart rate", Integer.toString(heartRate));
                Log.i("Step count", Integer.toString(stepCount));

                storeHeartRate(heartRate);
                storeStepCount(stepCount);

//                bleConnection.setText(new String(rcv));

                //TODO:: uncomment code with functional BLE module

                //Log.i("data",intent.getResources().getStringExtra(BLEService.EXTRA_DATA));
                //String characteristic = intent.getResources().getStringExtra("characteristic");
                //if(characteristic.equals(dataCharacteristicUUID)){
                   // mBLEService.readCharacteristic(dataCharacteristic);
                //}

                /*
                int heartRate = data & 0x0000FF;
                int stepCount = (data >> 8) 0x00FFFF;
                storeHeartRate(heartRate); //update
                storeStepCount(stepCount); //update in database and in UI display
                updateHeartRateUI(heartRate);
                updateStepCount(UI); //store in global variables - depends on what is being displayed in the UI
                 */
            }
        }
    };

    private final BroadcastReceiver mCallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (state == null) {

                //Outgoing call
                String number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                Log.i("tag", "Outgoing number : " + number);

            } else if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {

                Log.i("tag", "EXTRA_STATE_OFFHOOK");

            } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {

                Log.i("tag", "EXTRA_STATE_IDLE");

            } else if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {

                //Incoming call
                String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                String callNumber = "C" + number;
                byte[] send = callNumber.getBytes(StandardCharsets.UTF_8);
                mBLEService.writeRXCharacteristic(send);

            } else
                Log.i("tag", "none");
        }
    };

    private final BroadcastReceiver mSMSReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String TAG = "Summary";
            final String tag = TAG + ".onReceive";
            Bundle bundle = intent.getExtras();
            if (bundle == null) {
                Log.w(tag, "BroadcastReceiver failed, no intent data to process.");
                return;
            }
            if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                Log.d(tag, "SMS_RECEIVED");

                String smsOriginatingAddress;

                // You have to CHOOSE which code snippet to use NEW (KitKat+), or legacy
                // Please comment out the for{} you don't want to use.

                // API level 19 (KitKat 4.4) getMessagesFromIntent
                for (SmsMessage message : Telephony.Sms.Intents.
                        getMessagesFromIntent(intent)) {
                    Log.d(tag, "KitKat or newer");
                    if (message == null) {
                        Log.e(tag, "SMS message is null -- ABORT");
                        break;
                    }
                    smsOriginatingAddress = message.getDisplayOriginatingAddress();
                    Log.d("originating address", smsOriginatingAddress);
                    String msgNumber = "M" + smsOriginatingAddress;
                    byte[] send = msgNumber.getBytes(StandardCharsets.UTF_8);
                    mBLEService.writeRXCharacteristic(send);

                }
            }
        }
    };

    @Override
    protected void onPause(){
        super.onPause();
//        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(mGattUpdateReceiver);
        unregisterReceiver(mCallReceiver);
        unregisterReceiver(mSMSReceiver);
        unbindService(mConnection);
    }
    @Override
    protected void onResume(){
        super.onResume();
//        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
//        if(mBLEService != null) mBLEService.connect(user.getDeviceAddress());
    }
    //filter intents
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BLEService.EXTRA_DATA);
        return intentFilter;
    }

    //TODO:: add in with actual BLE watch
    public void storeHeartRate(int heartRate){
        //store recent heart rate data into database
    }

    //TODO:: add in with actual BLE watch
    public void storeStepCount(int stepCount)
    {
        //store step count into database
    }

    /*
    @Override
    public void onBackPressed() {
        Intent login = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(login);
        finish();
    }
    */

    private void getHRDB(){
        //query database for heart data
        new Thread(new Runnable() {
            @Override
            public void run() {
                //send request via apigateway
                String response = handler.getHeartRate(userId);
                Gson g = new Gson();
                if(response != ""){
                    HeartRate hr = g.fromJson(response, HeartRate.class);
                    userHR.setCurrentHR(hr.getCurrentHR());
                }
                else{
                    userHR.setCurrentHR(0);
                }

                //insert fake dailyHR vals for now
                //insert some fake vals for now
                Set<Integer> dailyHR = new HashSet<>(Arrays.asList(70, 60, 80, 100, 130, 61, 51, 62, 84, 102, 138, 65, 52, 60, 85, 111, 139, 62, 51, 67, 84, 120, 131, 68, 54));
                userHR.setDailyHR(dailyHR);

            }
        }).start();
    }

    private void getStepDB(){
        //query database for heart data
        new Thread(new Runnable() {
            @Override
            public void run() {
                //send request via apigateway
                String response = handler.getStepCount(userId);
                Gson g = new Gson();
                if(response != ""){
                    //StepCount sc = g.fromJson(response, StepCount.class);
                    //TODO: ISSUE WITH GSON INVESTIGATE LATER
                    userSteps.setCurrentSteps(10000);
                }
                else{
                    userSteps.setCurrentSteps(0);
                }

                //insert some fake vals for now
                Set<Integer> dailySteps = new HashSet<>(Arrays.asList(8000, 9000, 10000, 9000, 8200, 9005, 10500, 9580, 8100, 9600, 10250, 9890, 8012));
                userSteps.setDailySteps(dailySteps);
                userSteps.setUsername(user.getUsername());
            }
        }).start();
    }

    //utilizing observer design pattern to notify changes from database
    //TODO:: add for future improvement
    public void update(Observable obs, Object obj){
        if(obj == userHR){
            //notify data changed
            Log.i("State", "Heart rate state has changed");
        }
        else if(obj == userSteps){
            Log.i("State", "Step count state has changed");
        }
    }

    public void signOut(){
        //TODO: MOVE ALL COGNITO STUFF TO A HANDLER
        CognitoUserPool userPool = new CognitoUserPool(getApplicationContext(),
                getString(R.string.cognito_userpool_id),
                getString(R.string.cognito_client_id),
                getString(R.string.cognito_client_secret),
                Regions.fromName(getString(R.string.cognito_region)));
        userPool.getCurrentUser().signOut();
        Intent splashScreen = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(splashScreen);
        finish();
    }
}
