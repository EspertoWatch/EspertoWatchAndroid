package espertolabs.esperto_ble_watch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

public class Receiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d("BootTest : ", "\nOnBootReceiver - Received a broadcast!");
        Toast.makeText(context, "OnBootReceiver Received a broadcast!!", Toast.LENGTH_LONG).show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            context.startForegroundService(new Intent(context, WatchMan.class));
        }
        else
        {
            context.startService(new Intent(context, WatchMan.class));
        }
    }
}