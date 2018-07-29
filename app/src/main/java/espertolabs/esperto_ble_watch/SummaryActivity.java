package espertolabs.esperto_ble_watch;

import android.Manifest;
import android.arch.persistence.room.Room;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
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
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.regions.Regions;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.Utils;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;


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
    TextView hr_delta;
    TextView steps_delta;

    //instantiate api gateway handler
    final ApiGatewayHandler handler = new ApiGatewayHandler();
    String userId;

    HeartRateDB hr_db;
    StepCountDB steps_db;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_profile:
                    flipper.setDisplayedChild(3);
                    summaryDisplay = true;
                    detailedHeart = false;
                    detailedStep = false;
                   // TODO updateUI("Summary");
                    return true;
                case R.id.navigation_heart:
                    displayHeart(false);
                    detailedHeart = true;
                    summaryDisplay = false;
                    detailedStep = false;
                    // TODO updateUI("Heart Rate");
                    return true;
                case R.id.navigation_steps:
                    displaySteps(false);
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
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
//        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(),R.drawable.settings_ic);

        messageUser = findViewById(R.id.messageUser);
        bleConnection = findViewById(R.id.bleConnection);
        heartChart = findViewById(R.id.heartChart); //used to display daily HR
        stepChart = findViewById(R.id.stepChart); //used to display daily stepCount
        flipper = findViewById(R.id.vf);
        hr_current = findViewById(R.id.heartRateNum);
        steps_current = findViewById(R.id.stepCount);
        hr_delta = findViewById(R.id.heartRateDelta);
        steps_delta = findViewById(R.id.stepsDelta);

        mTextMessage = findViewById(R.id.message);
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setItemBackgroundResource(R.drawable.navigationbackground);

        //Check whether BLE is supported
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, getString(R.string.ble_not_supported), Toast.LENGTH_SHORT).show();
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
        registerReceiver(mSMSReceiver, new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));

        // Storage init
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("userId", Context.MODE_PRIVATE);
        userId = sharedPref.getString("USER_ID", "");

        hr_db = Room.databaseBuilder(getApplicationContext(),
                HeartRateDB.class, "HeartRate").build();

        steps_db = Room.databaseBuilder(getApplicationContext(),
                StepCountDB.class, "StepCount").build();

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

        userHR.setUserId(user.getUsername());
        userSteps.setUserId(user.getUsername());

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
                    Toast.makeText(this, getString(R.string.location_requirec), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    public void greetUser(){
        String fullName = user.getName();
        String[] splitName = fullName.trim().split("\\s+");
        messageUser.append(" "+splitName[0]);
    }

    //TODO:: add a loading screen to keep user occupied before data display

    private void displayHeart(boolean updateOnly){
        if (!updateOnly) {
            flipper.setDisplayedChild(1);
        }
        if (userHR.getAvgHourlyHR().size() != 0){
            List<Entry> entries = retrieveHeartRateData();
            LineDataSet dataSet = new LineDataSet(entries, "Heart Rate"); // add entries to dataset

            dataSet.setDrawFilled(true);
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
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
        HashMap<String, Float> avgHourlyHR = userHR.getAvgHourlyHR();
        int timeCounter = 8; //start at 8am TODO:: add actual times once I have the esperto watch (24 h clock)
        for(Map.Entry<String, Float> entry : avgHourlyHR.entrySet()){
            // turn your data into Entry objects
            entries.add(new Entry(timeCounter, entry.getValue())); //wrap each data point into Entry objects
            timeCounter++;
        }
        return entries;
    }

    //TODO:: adjust color of graph if goal achieved
    private void displaySteps(boolean updateOnly){
        if (!updateOnly) {
            flipper.setDisplayedChild(2);
        }
        if (userSteps.getTotalDailySteps().size() != 0) {
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
    }

    //call database to retrieve heart data
    //TODO:: store in a local data class so I don't have to keep requesting the data
    private BarDataSet retrieveStepData(){
        //retrieve step data and format output
        ArrayList xVals = new ArrayList();
        ArrayList<BarEntry> yVals = new ArrayList<BarEntry>();
        float counter = 0;
        float timeCounter = 8; //start at 8am TODO:: add actual times once I have the esperto watch (24 h clock)
        HashMap<String, Integer> totalDailySteps = userSteps.getTotalDailySteps();
        for(Map.Entry<String, Integer> entry : totalDailySteps.entrySet()){
            // turn your data into Entry objects
            int hours = (int)timeCounter;
            int min = (int)(timeCounter - hours) *60;
            String time = String.format("%d:%02d",hours, min).toString();

            yVals.add(new BarEntry(counter, entry.getValue())); //wrap each data point into Entry objects
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
                mBLEService.scanFromSummary(true);
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
            boolean connectedOnce = false;
            if (BLEService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.i("Update", "Device connected");
                mConnected = true;
                connectedOnce = true;
                invalidateOptionsMenu();
                bleConnection.setText("Watch connected");
                bleConnection.setTextColor(Color.GREEN);
            } else if (BLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.i("Update", "Device disconnected");
                mConnected = false;
                Toast.makeText(getApplicationContext(), getString(R.string.watch_disconnected), Toast.LENGTH_SHORT).show();
                invalidateOptionsMenu();

                if (connectedOnce == true) {
                    Date now = new Date();
                    SimpleDateFormat ft = new SimpleDateFormat ("hh:mm:ss a dd/MM/YYYY");
                    String lastConnectedTime = ft.format(now);

                    bleConnection.setText("Watch disconnected\nLast seen at " + lastConnectedTime);
                    bleConnection.setTextColor(Color.RED);
                }

                final Handler connectHandler = new Handler();
                connectHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBLEService.scanFromSummary(true);
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

                Log.i("Update", "onReceive: " + gattServices.toString());

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

                SimpleDateFormat ft = new SimpleDateFormat ("HH:mm:ss", Locale.US);
                String timeString = ft.format(now);
                byte[] send = timeString.getBytes(StandardCharsets.UTF_8);
                mBLEService.writeRXCharacteristic(send);

                ft = new SimpleDateFormat ("dd/MM/YYYY", Locale.US);
                timeString = ft.format(now);
                send = timeString.getBytes(StandardCharsets.UTF_8);
                mBLEService.writeRXCharacteristic(send);
            } else if (BLEService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] rcv = intent.getByteArrayExtra(BLEService.EXTRA_DATA);
                if (rcv.length % 4 == 0) {
                    Log.i("BLE data rcv'd", Arrays.toString(rcv));
                    for (int i = 0; i < rcv.length; i += 4) {
                        if (rcv[i+3] == 0) {
                            int heartRate = rcv[i];
                            int stepCount = ( ( rcv[i+1] & 0xFFFF ) << 8 ) | ( rcv[i+2] );

                            Log.i("Heart rate", Integer.toString(heartRate));
                            Log.i("Step count", Integer.toString(stepCount));

                            onBLEReceive(heartRate, stepCount);
                        } else {
                            Log.e("BLE data bad check byte", Integer.toString((int) rcv[i+3]));
                        }
                    }
                } else {
                    Log.e("BLE data bad length", Arrays.toString(rcv));
                }
            }
        }
    };

    private void onBLEReceive(int heartRate, int stepCount) {
        updateHeartUI(heartRate);
        updateStepsUI(stepCount);

        // Store after receive because UI updates depend on last value of HR and Step Count
        storeHeartRate(heartRate);
        storeStepCount(stepCount);
    }

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
                if (number != null) {
                   String callNumber = "C" + number.substring(number.length()-10);
                    byte[] send = callNumber.getBytes(StandardCharsets.UTF_8);
                    mBLEService.writeRXCharacteristic(send);
                } else {
                    Log.d("incoming call", "NULL number");
                }

            } else
                Log.i("tag", "none");
        }
    };

    private final BroadcastReceiver mSMSReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String TAG = "Summary";
            final String tag = TAG + "mSMSReceiver";
            Bundle bundle = intent.getExtras();
            if (bundle == null) {
                Log.w(tag, "BroadcastReceiver failed, no intent data to process.");
                return;
            }
            Log.d(tag, "SMS_RECEIVED");

            String smsOriginatingAddress;

            for (SmsMessage message : Telephony.Sms.Intents.
                    getMessagesFromIntent(intent)) {
                if (message == null) {
                    Log.e(tag, "SMS message is null -- ABORT");
                    break;
                }
                smsOriginatingAddress = message.getDisplayOriginatingAddress();
                Log.d("originating address", smsOriginatingAddress);
                String msgNumber = "M" + smsOriginatingAddress.substring( smsOriginatingAddress.length()-10);
                byte[] send = msgNumber.getBytes(StandardCharsets.UTF_8);
                mBLEService.writeRXCharacteristic(send);
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
        mBLEService.disconnect();
        unregisterReceiver(mGattUpdateReceiver);
        unregisterReceiver(mCallReceiver);
        unregisterReceiver(mSMSReceiver);
        unbindService(mConnection);
        hr_db.close();
        // steps_db.close();
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

    public void storeHeartRate(int heartRate){
        Date now = new Date();
        SimpleDateFormat ft = new SimpleDateFormat ("dd-MM-YYYY kk", Locale.US);
        String timeString = ft.format(now);
        userHR.addAvgHourlyHR(timeString, heartRate);
        new Thread(new Runnable(){
            @Override
            public void run(){
                JSONObject userJsonObject = new JSONObject();
                JSONObject userJsonMap = new JSONObject(userHR.getAvgHourlyHR());
                try {
                    userJsonObject.put("userId", userId);
                    userJsonObject.put("currentHR", heartRate);
                    userJsonObject.put("avgHourlyHR", userJsonMap);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String userJson = userJsonObject.toString();
                String response = handler.postHeartRate(userJson);
                Log.d("HR_post_response", response);

                if(response != ""){
                    Log.i("HR store", "success");
                    hr_db.HeartRateDAO().insertHeartRate(userHR);
                }
            }
        }).start();
    }

    public void storeStepCount(int stepCount) {
        Date now = new Date();
        SimpleDateFormat ft = new SimpleDateFormat ("dd-MM-YYYY", Locale.US);
        String timeString = ft.format(now);
        userSteps.addTotalDailySteps(timeString, stepCount);
        new Thread(new Runnable(){
            @Override
            public void run(){
                JSONObject userJsonObject = new JSONObject();
                JSONObject userJsonMap = new JSONObject(userSteps.getTotalDailySteps());
                try {
                    userJsonObject.put("userId", userId);
                    userJsonObject.put("currentSteps", stepCount);
                    userJsonObject.put("totalDailySteps", userJsonMap);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String userJson = userJsonObject.toString();
                String response = handler.postStepCount(userJson);
                Log.d("StepCount_post_response", response);

                if(response != ""){
                    Log.i("Step count store", "success");
                   steps_db.StepCountDAO().insertStepCount(userSteps);
                }
            }
        }).start();
    }

    private void getHRDB(){
        //query database for heart data
        new Thread(new Runnable() {
            @Override
            public void run() {
                HeartRate userHRTemp = hr_db.HeartRateDAO().getUserHeartRate(user.getUsername());
                if (userHRTemp == null) {
                    hr_db.HeartRateDAO().insertHeartRate(userHR);
                } else {
                    userHR = userHRTemp;
                }
                //send request via apigateway
                String response = handler.getHeartRate(userId);
                Log.i("HR obj response", response);
                Gson g = new Gson();
                if(response != ""){
                    HeartRate hr = g.fromJson(response, HeartRate.class);
                    // This also sets the currentHR of userHR
                    updateHeartUI(hr.getCurrentHR());
                    if (hr.getAvgHourlyHR().size() != 0) {
                        userHR.setAvgHourlyHR(hr.getAvgHourlyHR());
                    }
                    hr_db.HeartRateDAO().insertHeartRate(userHR);
                } else {
                    updateHeartUI(0);
                }
            }
        }).start();
    }

    private void updateHeartUI(Integer currentHr){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (currentHr > 0) {
                    displayHeart(true);
                    String dynamicPart;
                    int last = userHR.getCurrentHR();
                    int delta = currentHr - last;
                    userHR.setCurrentHR(currentHr);
                    if (delta == 0) {
                        dynamicPart = "No change";
                    } else {
                        dynamicPart = Integer.toString(Math.abs(delta));
                        dynamicPart += delta > 0 ?  " Up" : " Down";
                    }
                    hr_delta.setText(dynamicPart + " from last");
                    hr_current.setText(currentHr != 0 ? Integer.toString(userHR.getCurrentHR()) + " BPM" : "No Data");
                }
            }
        });
    }

    private void updateStepsUI(Integer currentSteps){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (currentSteps > 0) {
                    displaySteps(true);
                    String dynamicPart;
                    int last = userSteps.getCurrentSteps();
                    int delta = currentSteps - last;
                    userSteps.setCurrentSteps(currentSteps);
                    if (delta == 0) {
                        dynamicPart = "No change";
                    } else {
                        dynamicPart = Integer.toString(Math.abs(delta));
                        dynamicPart += delta > 0 ?  " Up" : " Down";
                    }
                    steps_delta.setText(dynamicPart + " from last");
                    steps_current.setText(currentSteps != 0 ? Integer.toString(userSteps.getCurrentSteps()) + " steps" : "No Data");
                }
            }
        });
    }

    private void getStepDB(){
        //query database for heart data
        new Thread(new Runnable() {
            @Override
            public void run() {
                StepCount userStepsTemp = steps_db.StepCountDAO().getUserStepCount(user.getUsername());
                if (userStepsTemp == null) {
                    steps_db.StepCountDAO().insertStepCount(userSteps);
                } else {
                    userSteps = userStepsTemp;
                }
                //send request via apigateway
                String response = handler.getStepCount(userId);
                Log.i("Steps obj response", response);
                Gson g = new Gson();
                if(response != ""){
                    StepCount sc = g.fromJson(response, StepCount.class);
                    updateStepsUI(sc.getCurrentSteps());
                    if (sc.getTotalDailySteps().size() != 0) {
                        userSteps.setTotalDailySteps(sc.getTotalDailySteps());
                    }
                    steps_db.StepCountDAO().insertStepCount(userSteps);
                } else {
                    updateStepsUI(0);
                }
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
        finish();
        Intent splashScreen = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(splashScreen);
    }
}
