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
import android.icu.util.TimeUnit;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeMap;


public class SummaryActivity extends AppCompatActivity implements Observer {

    private TextView mTextMessage;
    private UserAccount user = new UserAccount();
    private HeartRate userHR = new HeartRate();
    private StepCount userSteps = new StepCount();

    private BLEService mBLEService;
    private boolean mServiceBound;
    private boolean mConnected = false;
    private String dataCharacteristicUUID = "00002a05-0000-1000-8000-00805f9b34fb";
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
    Spinner hrChartSpinner;
    Spinner stepsChartSpinner;
    Integer hrChartDisplayDays = 0;
    Integer stepsChartDisplayDays = 7;

    //Summary values
    TextView hr_current;
    TextView steps_current;
    TextView hr_delta;
    TextView steps_delta;
    ImageView hr_arrow;
    ImageView steps_arrow;
    ImageView logoImage;
    ConstraintLayout hr_delta_layout;
    ConstraintLayout steps_delta_layout;

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
                    return true;
                case R.id.navigation_heart:
                    flipper.setDisplayedChild(1);
                    detailedHeart = true;
                    summaryDisplay = false;
                    detailedStep = false;
                    return true;
                case R.id.navigation_steps:
                    flipper.setDisplayedChild(2);
                    detailedHeart = false;
                    detailedStep = true;
                    summaryDisplay = false;
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
        hrChartSpinner = findViewById(R.id.heartRateGraphSpinner);
        stepsChartSpinner = findViewById(R.id.stepGraphSpinner);
        flipper = findViewById(R.id.vf);
        logoImage = findViewById(R.id.logo);
        hr_current = findViewById(R.id.heartRateNum);
        steps_current = findViewById(R.id.stepCount);
        hr_delta = findViewById(R.id.heartRateDelta);
        steps_delta = findViewById(R.id.stepsDelta);
        hr_arrow = findViewById(R.id.heartRateArrow);
        steps_arrow = findViewById(R.id.stepsArrow);
        hr_delta_layout = findViewById(R.id.heartRateDeltaLayout);
        steps_delta_layout = findViewById(R.id.stepsDeltaLayout);

        mTextMessage = findViewById(R.id.message);
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setItemBackgroundResource(R.drawable.navigationbackground);

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

        TextView logoutButton = findViewById(R.id.text_logout);

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
            }
        });

        logoImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                syncWatch();
            }
        });

        ArrayAdapter<CharSequence> stepsGraphSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.steps_graph_options_array, android.R.layout.simple_spinner_item);
        stepsGraphSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stepsChartSpinner.setAdapter(stepsGraphSpinnerAdapter);

        stepsChartSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: stepsChartDisplayDays = 7;
                            break;
                    case 1: stepsChartDisplayDays = 31;
                            break;
                    case 2: stepsChartDisplayDays = 365;
                            break;
                    default: stepsChartDisplayDays = 7;
                }

                drawStepsGraph();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        ArrayAdapter<CharSequence> hrGraphSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.hr_graph_options_array, android.R.layout.simple_spinner_item);
        hrGraphSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        hrChartSpinner.setAdapter(hrGraphSpinnerAdapter);

        hrChartSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: hrChartDisplayDays = 0;
                            break;
                    case 1: hrChartDisplayDays = -1;
                            break;
                    case 2: hrChartDisplayDays = 7;
                            break;
                    case 3: hrChartDisplayDays = 31;
                            break;
                    case 4: hrChartDisplayDays = 365;
                            break;
                    default: hrChartDisplayDays = 0;
                }

                drawHeartRateGraph();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
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

    public void greetUser(){
        String fullName = user.getName();
        String[] splitName = fullName.trim().split("\\s+");
        messageUser.append(" "+splitName[0]);
    }

    //TODO:: add a loading screen to keep user occupied before data display

    private void drawHeartRateGraph() {
        if (userHR.getAvgHourlyHR().size() != 0){
            List<Entry> hr_graph_entries = retrieveHeartRateData(hrChartDisplayDays);
            LineDataSet hr_dataSet = new LineDataSet(hr_graph_entries, "Heart Rate");

            hr_dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

            hr_dataSet.setColors(getResources().getColor(android.R.color.holo_red_dark));
            hr_dataSet.setLineWidth(3f);
            hr_dataSet.setDrawCircleHole(true);
            hr_dataSet.setDrawCircles(true);
            hr_dataSet.setCircleColor(getResources().getColor(android.R.color.holo_red_dark));
            hr_dataSet.setCircleHoleRadius(2f);
            hr_dataSet.setCircleRadius(5f);
            hr_dataSet.setDrawValues(false);

            LineData lineData = new LineData(hr_dataSet);
            YAxis leftAxis = heartChart.getAxisLeft();
            XAxis bottomAxis = heartChart.getXAxis();
            YAxis rightAxis = heartChart.getAxisRight();

            leftAxis.setDrawAxisLine(false);
            leftAxis.setDrawGridLines(true);
            leftAxis.setDrawZeroLine(false);
            leftAxis.setAxisMinimum(0.01f);
            leftAxis.setGranularity(1f);
            leftAxis.setTypeface(ResourcesCompat.getFont(this, R.font.lato));
            leftAxis.setTextColor(R.color.EspertoTextGrey);

            bottomAxis.setDrawGridLines(false);
            bottomAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            bottomAxis.setTypeface(ResourcesCompat.getFont(this, R.font.lato));
            bottomAxis.setTextColor(R.color.EspertoTextGrey);
//            if (hrChartDisplayDays > 0) {
//                bottomAxis.setValueFormatter(new DayAxisValueFormatter(heartChart));
//            }
            bottomAxis.setLabelCount(7);
            bottomAxis.setAxisMinimum(0f);
            bottomAxis.setGranularity(1f);



            rightAxis.setEnabled(false);

            heartChart.getDescription().setEnabled(false);
            heartChart.getLegend().setEnabled(false);

            heartChart.setData(lineData);
            heartChart.invalidate(); // Refresh
        }
    }

    //TODO:: adjust color of graph if goal achieved
    private void drawStepsGraph() {
        if (userSteps.getTotalDailySteps().size() != 0) {
            List<BarEntry> steps_graph_entries = retrieveStepData(stepsChartDisplayDays);
            BarDataSet steps_dataSet = new BarDataSet(steps_graph_entries, "Step Count");

            steps_dataSet.setColors(getResources().getColor(R.color.steps));
            steps_dataSet.setDrawValues(false);

            BarData barData = new BarData(steps_dataSet);
            YAxis leftAxis = stepChart.getAxisLeft();
            XAxis bottomAxis = stepChart.getXAxis();
            YAxis rightAxis = stepChart.getAxisRight();

            leftAxis.setDrawAxisLine(false);
            leftAxis.setDrawGridLines(true);
            leftAxis.setDrawZeroLine(false);
            leftAxis.setAxisMinimum(0.01f);
//            leftAxis.setGranularity(1f);
            leftAxis.setTypeface(ResourcesCompat.getFont(this, R.font.lato));
            leftAxis.setTextColor(R.color.EspertoTextGrey);

            bottomAxis.setDrawGridLines(false);
            bottomAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            bottomAxis.setTypeface(ResourcesCompat.getFont(this, R.font.lato));
            bottomAxis.setTextColor(R.color.EspertoTextGrey);
            bottomAxis.setLabelCount(7);
            bottomAxis.setValueFormatter(new DayAxisValueFormatter(stepChart));
            bottomAxis.setGranularity(1f);
            bottomAxis.setLabelRotationAngle(45f);

            rightAxis.setEnabled(false);

            stepChart.getDescription().setEnabled(false);
            stepChart.getLegend().setEnabled(false);
            stepChart.setFitBars(true);
            stepChart.setExtraOffsets(0,0,0,8f);

            if (stepsChartDisplayDays > 7) {
                stepChart.setScaleXEnabled(true);
            } else {
                stepChart.setScaleXEnabled(false);
            }

            stepChart.setData(barData);
            stepChart.invalidate(); // Refresh
        }
    }

    private List<Entry> retrieveHeartRateData(int displayDays) {
        // Retrieve data here
        List<Entry> entries = new ArrayList<>();
        HashMap<String, Float> avgHourlyHR = userHR.getAvgHourlyHR();
//        Map<String, Float> sortedAvgHourlyHR = new TreeMap<>(avgHourlyHR);

//        float counter = 0;

        Long now = new Date().getTime();
        Long deltaTime;
        Date targetTime;
        SimpleDateFormat ftKey = new SimpleDateFormat ("YYYY-MM-dd kk", Locale.US);
        SimpleDateFormat ftYear = new SimpleDateFormat ("YYYY", Locale.US);
        SimpleDateFormat ftDayofYear = new SimpleDateFormat ("D", Locale.US);
        String targetTimeString;
        Integer targetDayofYear;
        Integer targetYear;
        Float targetHR;

        if (displayDays > 0) {
            for (int i = 0; i < displayDays; i++) {
                deltaTime = (1000 * 60 * 60 * 24) * (long)i;
                targetTime = new Date(now - deltaTime);
                targetTimeString = ftKey.format(targetTime);
                targetDayofYear = Integer.parseInt(ftDayofYear.format(targetTime)) + 1;
                targetYear = Integer.parseInt(ftYear.format(targetTime));
                targetDayofYear += (int) ((targetYear - 2016) * 365.25f);

                targetHR = avgHourlyHR.get(targetTimeString);

                if (targetHR != null) {
                    entries.add(new Entry(targetDayofYear, targetHR));
                } else {
                    entries.add(new Entry(targetDayofYear, -1));
                }
            }
        } else {
            for (int i = 0; i < 24; i++) {
                deltaTime = (1000 * 60 * 60) * (long)i;
                targetTime = new Date(now - deltaTime);
                targetTimeString = ftKey.format(targetTime);
                targetHR = avgHourlyHR.get(targetTimeString);

                if (targetHR != null) {
                    entries.add(new Entry(i, targetHR));
                } else {
                    entries.add(new Entry(i, -1));
                }
            }
        }



//        for (Map.Entry<String, Float> entry : sortedAvgHourlyHR.entrySet()) {
//            // Turn your data into Entry objects
//            entries.add(new Entry(counter, entry.getValue()));
//            counter++;
//        }

        return entries;
    }

    private List<BarEntry> retrieveStepData(int displayDays) {
        // Retrieve data here
        List<BarEntry> entries = new ArrayList<>();
        HashMap<String, Integer> totalDailySteps = userSteps.getTotalDailySteps();
//        Map<String, Integer> sortedTotalDailySteps = new TreeMap<>(totalDailySteps);

        Long now = new Date().getTime();
        Long deltaTime;
        Date targetTime;
        SimpleDateFormat ftKey = new SimpleDateFormat ("YYYY-MM-dd", Locale.US);
        SimpleDateFormat ftYear = new SimpleDateFormat ("YYYY", Locale.US);
        SimpleDateFormat ftDayofYear = new SimpleDateFormat ("D", Locale.US);
        String targetTimeString;
        Integer targetDayofYear;
        Integer targetYear;
        Integer targetSteps;

        for (int i = 0; i < displayDays; i++) {
            deltaTime = (1000 * 60 * 60 * 24) * (long)i;
            targetTime = new Date(now - deltaTime);
            targetTimeString = ftKey.format(targetTime);
            targetDayofYear = Integer.parseInt(ftDayofYear.format(targetTime)) + 1;
            targetYear = Integer.parseInt(ftYear.format(targetTime));
            targetDayofYear += (int) ((targetYear - 2016) * 365.25f);
            targetSteps = totalDailySteps.get(targetTimeString);

            if (targetSteps != null) {
                entries.add(new BarEntry(targetDayofYear, targetSteps));
            } else {
                entries.add(new BarEntry(targetDayofYear, 0));
            }
        }

//        for (Map.Entry<String, Integer> entry : sortedTotalDailySteps.entrySet()) {
//            // Turn your data into Entry objects
//            entries.add(new BarEntry(counter, entry.getValue()));
//            if (counter == displayDays) {
//                return entries;
//            } else {
//                counter += 1;
//            }
//        }

        return entries;
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
                scanAndConnect();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mServiceBound = false;
        }
    };

    void syncWatch() {
        if (mConnected) {
            // Send updated time and date every time summary is opened
            Date now = new Date();

            SimpleDateFormat ft = new SimpleDateFormat ("HH:mm:ss", Locale.US);
            String timeString = ft.format(now);
            byte[] sendTime = timeString.getBytes(StandardCharsets.UTF_8);

            ft = new SimpleDateFormat ("dd/MM/YYYY", Locale.US);
            timeString = ft.format(now);
            byte[] sendDate = timeString.getBytes(StandardCharsets.UTF_8);

            Runnable retrySendTimeDate = () -> {
                int retries = 10;
                boolean statusTime;
                boolean statusDate;
                do {
                    statusTime = mBLEService.writeRXCharacteristic(sendTime);
                    statusDate = mBLEService.writeRXCharacteristic(sendDate);
                    try {
                        Thread.sleep(50);
                    } catch(InterruptedException e) {
                    }
                } while (((!statusTime) || (!statusDate)) && (retries-- > 0));
            };

            Runnable retrySendDate = () -> {
                int retries = 10;
                while (!mBLEService.writeRXCharacteristic(sendDate) && (retries-- > 0)) {
                    try {
                        Thread.sleep(50);
                    } catch(InterruptedException e) {
                    }
                }
            };

            if (!mBLEService.writeRXCharacteristic(sendTime)) {
                new Thread(retrySendTimeDate).start();
            } else {
                if (!mBLEService.writeRXCharacteristic(sendDate)) {
                    new Thread(retrySendDate).start();
                }
            }

        } else {
            scanAndConnect();
        }
    }

    void scanAndConnect() {
        mBLEService.scanFromSummary(true);
        mBLEService.connect(user.getDeviceAddress());
    }

    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device. This can be a
    // result of read or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
//            boolean connectedOnce = false;
            if (BLEService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.i("Update", "Device connected");
                mConnected = true;
//                connectedOnce = true;
                invalidateOptionsMenu();
                bleConnection.setVisibility(View.INVISIBLE);
//                bleConnection.setText("Watch connected");
//                bleConnection.setTextColor(Color.GREEN);
            } else if (BLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.i("Update", "Device disconnected");
                mConnected = false;
                Toast.makeText(getApplicationContext(), getString(R.string.watch_disconnected), Toast.LENGTH_SHORT).show();
                invalidateOptionsMenu();

//                if (connectedOnce == true) {
//                    Date now = new Date();
//                    SimpleDateFormat ft = new SimpleDateFormat ("hh:mm:ss a dd/MM/YYYY");
//                    String lastConnectedTime = ft.format(now);
//
//                    bleConnection.setText("Watch disconnected\nLast seen at " + lastConnectedTime);
                    bleConnection.setVisibility(View.VISIBLE);
                    bleConnection.setTextColor(Color.RED);
//                }

                final Handler connectHandler = new Handler();
                connectHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanAndConnect();
                    }
                }, 500);
                //clearUI();
            } else if (BLEService.
                    ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.i("Update", "Services discovered");
                mBLEService.enableTXNotification();

                List<BluetoothGattService> gattServices = mBLEService.getSupportedGattServices();

                if (gattServices == null) {
                    return;
                }

                syncWatch();
            } else if (BLEService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] rcv = intent.getByteArrayExtra(BLEService.EXTRA_DATA);
                if (rcv.length % 4 == 0) {
                    Log.i("BLE data rcv'd", Arrays.toString(rcv));
                    for (int i = 0; i < rcv.length; i += 4) {
                        if ((rcv[i+3] & 0x80) == 0) {
                            int heartRate = rcv[i];
                            int stepCount = ( ( rcv[i+1] & 0xFFFF ) << 8 ) | ( rcv[i+2] );
                            int spo2 = rcv[i+3];

                            Log.i("Heart rate", Integer.toString(heartRate));
                            Log.i("Step count", Integer.toString(stepCount));
                            Log.i("SpO2", Integer.toString(spo2));
                            if (heartRate >= 40 && heartRate <= 200 && stepCount > 0 && spo2 >= 0 && spo2 <= 100) {
                                onBLEReceive(heartRate, stepCount);
                            } else {
                                Log.e("BLE data", "out of range");
                            }
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
        steps_db.close();
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
        SimpleDateFormat ft = new SimpleDateFormat ("YYYY-MM-dd kk", Locale.US);
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
        SimpleDateFormat ft = new SimpleDateFormat ("YYYY-MM-dd", Locale.US);
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
                    drawHeartRateGraph();
                    String dynamicPart = "---";
                    int last = userHR.getCurrentHR();
                    int delta = currentHr - last;
                    userHR.setCurrentHR(currentHr);
                    if (delta == 0) {
                        hr_delta_layout.setVisibility(View.INVISIBLE);
                    } else {
                        dynamicPart = Integer.toString(Math.abs(delta));
                        hr_arrow.setBackgroundResource(delta > 0 ?  R.drawable.ic_up_triangle : R.drawable.ic_down_triangle);
                        hr_delta_layout.setVisibility(View.VISIBLE);
                    }
                    hr_delta.setText(dynamicPart + " BPM");
                    hr_current.setText(Integer.toString(userHR.getCurrentHR()) + " BPM");
                }
            }
        });
    }

    private void updateStepsUI(Integer currentSteps){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (currentSteps > 0) {
                    drawStepsGraph();
                    String dynamicPart = "---";
                    int last = userSteps.getCurrentSteps();
                    int delta = currentSteps - last;
                    userSteps.setCurrentSteps(currentSteps);
                    if (delta == 0) {
                        steps_delta_layout.setVisibility(View.INVISIBLE);
                    } else {
                        dynamicPart = Integer.toString(Math.abs(delta));
                        steps_arrow.setBackgroundResource(delta > 0 ?  R.drawable.ic_up_triangle : R.drawable.ic_down_triangle);
                        steps_delta_layout.setVisibility(View.VISIBLE);
                    }
                    steps_delta.setText(dynamicPart + " Steps");
                    steps_current.setText(Integer.toString(userSteps.getCurrentSteps()) + " Steps");
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
        Intent loginScreen = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(loginScreen);
    }
    @Override
    public void onBackPressed() {
        //don't allow users to press back
    }
}
