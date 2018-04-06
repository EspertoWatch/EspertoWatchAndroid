package espertolabs.esperto_ble_watch;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import com.amazonaws.mobile.client.AWSMobileClient;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton startButton = (ImageButton) findViewById(R.id.startButton);
        startButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startScan = new Intent(v.getContext(), LoginActivity.class);
                startActivity(startScan);
                finish();
            }

        });

        //Initialize AWS web services
        AWSMobileClient.getInstance().initialize(this).execute();


        //TODO::checkk for internet permission problem -> used in offline mode
        /*
        //check if BLE is supported by the mobile device
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Warning: BLE is not supported by this device.", Toast.LENGTH_SHORT).show();
            finish();
        }*/
    }
}

