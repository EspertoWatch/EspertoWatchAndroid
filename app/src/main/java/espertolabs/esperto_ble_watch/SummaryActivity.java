package espertolabs.esperto_ble_watch;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.mobile.client.AWSMobileClient;
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
import com.google.android.gms.common.data.DataBufferObserver;
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
    private BluetoothGattCharacteristic dataCharacteristic;
    //DynamoDBMapper dynamoDBMapper; //map tables to Java classes
    //AmazonDynamoDBClient dynamoDBClient;
    private boolean summaryDisplay = false;
    private boolean detailedHeart = false;
    private boolean detailedStep = false;
    TextView messageUser;

    //Graph UI objects
    LineChart heartChart;
    BarChart stepChart;
    ViewFlipper flipper;

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
                   // TODO updateUI("Summary
                    // ");
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
        toolbar.setOverflowIcon(drawable);

        messageUser = (TextView) findViewById(R.id.messageUser);
        heartChart = (LineChart)findViewById(R.id.heartChart); //used to display daily HR
        stepChart = (BarChart) findViewById(R.id.stepChart); //used to display daily stepCount
        flipper = (ViewFlipper)findViewById(R.id.vf);


        //retrieve intent
        Intent userIntent = getIntent();
        user.setDeviceAddress(userIntent.getStringExtra("deviceAddress"));
        user.setFirstName(userIntent.getStringExtra("firstName"));
        user.setLastName(userIntent.getStringExtra("lastName"));
        user.setUsername(userIntent.getStringExtra("username"));
        user.setGoalSetting(userIntent.getStringExtra("goalSetting"));

        greetUser();

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        //check if Bluetooth is enabled
        Intent intent = new Intent(this, BLEService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        // Instantiate a AmazonDynamoDBMapperClient
        //Commenting out because we're eventually replacing with ApiGateway!
        //Todo: connect to ApiGateway
        /*
        dynamoDBClient = Region.getRegion(Regions.US_EAST_1)
                .createClient(AmazonDynamoDBClient.class,
                        AWSMobileClient.getInstance().getCredentialsProvider(),
                        new ClientConfiguration()
                );


        this.dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(dynamoDBClient)
                .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                .build();
        */

        //retrieve heart rate data
        getHRDB();
        getStepDB();

    }

    public void greetUser(){
        String greetText = "Welcome " + user.getFirstName() + "!";
        messageUser.setText(greetText);
    }

    //TODO:: add a loading screen to keep user occupied before data display
    private void displaySummary(){
        flipper.setDisplayedChild(3);
        TextView hr = (TextView) findViewById(R.id.heartRateNum);
        TextView step = (TextView) findViewById(R.id.stepCount);
        //using default goals for now
        if(userHR.getUsername() == null || userSteps.getUsername() == null){
            Log.i("Fail", "user has no long term data");
            hr.setText("0");
            step.setText("0");
            return; //just displaying random stand in data if not long term user
        }
        hr.setText(Integer.toString(userHR.getCurrentHR()));
        step.setText(Integer.toString(userSteps.getCurrentSteps()));
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
        else return;
    }

    //Retrieve data from AWS services
    //If no data - state "no data available"
    private List<Entry> retrieveHeartRateData(){
       //Retrieve data here
        List<Entry> entries = new ArrayList<Entry>();
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
        if(!user.getUsername().equals("mmacmahon")){return;}
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
        Set<Integer> dailySteps = userSteps.getDailySteps();
        for(Integer i:dailySteps){
            // turn your data into Entry objects
            int hours = (int)timeCounter;
            int min = (int)(timeCounter - hours) *60;
            //String time = String.format("%d:%02d",hours, min).toString();

            yVals.add(new BarEntry(counter, i)); //wrap each data point into Entry objects
            //xVals.add(time);
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
            mServiceBound = true;
            mBLEService = binder.getService();
            if (mBLEService.initialize()) {
                user.setDeviceAddress("D4:49:8C:44:48:82");
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
            } else if (BLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.i("Update", "Device disconnected");
                mConnected = false;
                Toast.makeText(getApplicationContext(), "Esperto Watch disconnected. ", Toast.LENGTH_SHORT).show();
                invalidateOptionsMenu();
                finish();
                //TODO::attempt to reconnect
                //else close application and display toast message
                //clearUI();
            } else if (BLEService.
                    ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.i("Update", "Services discovered");

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
                        //Log.d("DEBUG", "PRINT UUIDS: " + uuid);
                    }
                }

                // Show all the supported services and characteristics on the
                // user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BLEService.ACTION_DATA_AVAILABLE.equals(action)) {
                //TODO:: uncomment code with functional BLE module

                //Log.i("data",intent.getStringExtra(BLEService.EXTRA_DATA));
                //String characteristic = intent.getStringExtra("characteristic");
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

    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

    }
    @Override
    protected void onResume(){
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if(mBLEService != null) mBLEService.connect(user.getDeviceAddress());
    }
    //filter intents
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEService.ACTION_DATA_AVAILABLE);
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

    @Override
    public void onBackPressed() {
        Intent login = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(login);
        finish();
    }

    private void getHRDB(){
        //query database for heart data
        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                //insert some fake vals for now
                Set<Integer> dailyHR = new HashSet<>(Arrays.asList(80, 90, 100, 90, 80));
                Integer currentHR = 85;
                userHR.setCurrentHR(currentHR);
                userHR.setDailyHR(dailyHR);
                userHR.setUsername(user.getUsername());

                //query database
                //todo: replace with apigateway
                /*
                DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression()
                        .withHashKeyValues(hr)
                        .withConsistentRead(false);

                // PaginatedList<UserAccount> result = dynamoDBMapper.query(UserAccount.class, queryExpression);
                List<HeartRate> result = dynamoDBMapper.query(HeartRate.class, queryExpression);

                Gson gson = new Gson();
                StringBuilder stringBuilder = new StringBuilder();
                Type listType = new TypeToken<List<String>>() {}.getType();
                List<String> target = new LinkedList<String>();


                // Loop through query results
                for (int i = 0; i < result.size(); i++) {
                    Log.i("result", result.get(i).toString());
                    String jsonFormOfItem = gson.toJson(result.get(i));

                    userHR = gson.fromJson(jsonFormOfItem, HeartRate.class);
                }

                // Add your code here to deal with the data result
                Log.d("Query result: ", Integer.toString(userHR.getCurrentHR()));

                if (result.isEmpty()) {
                    Log.i("Data", "User does not have long term data");

                }
                */
            }
        }).start();


    }

    private void getStepDB(){
        //query database for heart data
        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                //insert some fake vals for now
                Set<Integer> dailySteps = new HashSet<>(Arrays.asList(8000, 9000, 10000, 9000, 8000));
                Integer currentSteps = 8500;
                userSteps.setCurrentSteps(currentSteps);
                userSteps.setDailySteps(dailySteps);
                userSteps.setUsername(user.getUsername());

                //query database
                //todo: replace with apigateway
                /*
                DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression()
                        .withHashKeyValues(sc)
                        .withConsistentRead(false);

                // PaginatedList<UserAccount> result = dynamoDBMapper.query(UserAccount.class, queryExpression);
                List<StepCount> result = dynamoDBMapper.query(StepCount.class, queryExpression);

                Gson gson = new Gson();
                StringBuilder stringBuilder = new StringBuilder();
                Type listType = new TypeToken<List<String>>() {}.getType();
                List<String> target = new LinkedList<String>();


                // Loop through query results
                for (int i = 0; i < result.size(); i++) {
                    Log.i("result", result.get(i).toString());
                    String jsonFormOfItem = gson.toJson(result.get(i));

                    userSteps = gson.fromJson(jsonFormOfItem, StepCount.class);
                }

                // Add your code here to deal with the data result
                Log.d("Query result: ", Integer.toString(userSteps.getCurrentSteps()));

                if (result.isEmpty()) {
                    Log.i("Data", "User does not have long term data");

                }
                */
            }
        }).start();


    }

    //utilizing observer design pattern to notify changes from database call
    public void update(Observable obs, Object obj){
        if(obj == userHR){
            //notify data changed
            Log.i("State", "Heart rate state has changed");

            //call summary data(assuming thats the first screen)
        }
        else if(obj == userSteps){
            Log.i("State", "Step count state has changed");

        }
    }


}
