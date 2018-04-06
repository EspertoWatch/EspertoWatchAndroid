package espertolabs.esperto_ble_watch;

import android.bluetooth.le.ScanRecord;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Test;

import static org.junit.Assert.*;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.any;

//Utilize integration tests to test BLE operation
@RunWith(AndroidJUnit4.class)
public class BLEServiceTest {

    @Rule
    public final ServiceTestRule mServiceRule = new ServiceTestRule();

    @Test
    public void testInitialize() throws TimeoutException {
        // Create the BLE service Intent.
        Intent serviceIntent =
                new Intent(InstrumentationRegistry.getTargetContext(),
                        BLEService.class);

        // Bind the service and grab a reference to the binder.
        IBinder binder = mServiceRule.bindService(serviceIntent);


        // Get the reference to the service, or you can call
        // public methods on the binder directly.
        BLEService service =
                ((BLEService.LocalBinder) binder).getService();
        Looper.prepare();
        assertTrue(service.initialize());

        assertFalse(service.isEspertoWatch("NotEsperto"));
        assertTrue(service.isEspertoWatch(Integer.toString(R.string.esperto_device_name)));
        //attempt
        // to connect to device
        assertTrue(service.connect("20:FA:BB:04:9D:C9"));
        service.disconnect();
        service.close();

        //test device disconnect
        try
        {
            service.disconnect(); //check that it actually disconnects - shouldn't throw an exception
        }
        catch(Exception e)
        {
            Assert.fail("Should not have thrown exception");
        }

    }


}
