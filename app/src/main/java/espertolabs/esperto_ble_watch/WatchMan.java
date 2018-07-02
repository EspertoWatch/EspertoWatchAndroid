package espertolabs.esperto_ble_watch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

public class WatchMan extends Service
{
    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;
    NotificationChannel notificationChannel;
    String NOTIFICATION_CHANNEL_ID = "17";

    private BroadcastReceiver mCallBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String PhoneNumber = "UNKNOWN";
            Log.d("RECEIVER :  ","IS UP AGAIN....");

            try
            {
                String action = intent.getAction();
                if(action.equalsIgnoreCase("android.intent.action.PHONE_STATE"))
                {
                    if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING))
                    {
                        PhoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                        Log.d("RECEIVER : ","Incoming number : "+PhoneNumber);

                        // update in database and goto catchnumber to sms

//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//                        {
//                            context.startForegroundService(new Intent(context, CatchNumbers.class));
//                        }
//                        else
//                        {
//                            context.startService(new Intent(context, CatchNumbers.class));
//                        }
                    }
                    if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_IDLE))
                    {
                        PhoneNumber = "UNKNOWN";
                    }
                    if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_OFFHOOK))
                    {
                        Log.d("RECEIVER :  ","OUTGOING CALL RECEIVED....");

                        // UPDATED in database and JUST GOTO catchnumber to sms

//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//                        {
//                            context.startForegroundService(new Intent(context, CatchNumbers.class));
//                        }
//                        else
//                        {
//                            context.startService(new Intent(context, CatchNumbers.class));
//                        }
                    }
                }
                if(action.equalsIgnoreCase("android.intent.action.NEW_OUTGOING_CALL"))
                {
                    PhoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                    Log.d("RECEIVER : ","Outgoing number : "+PhoneNumber);

                    // update in database and BUT DO NOT GOTO catchnumber to sms
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.e("RECEIVER : ", "Exception is : ", e);
            }
        }
    };

    public WatchMan() { }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d("WatchMan : ", "\nOnCreate...");

        IntentFilter CallFilter = new IntentFilter();
        CallFilter.addAction("android.intent.action.NEW_OUTGOING_CALL");
        CallFilter.addAction("android.intent.action.PHONE_STATE");
        this.registerReceiver(mCallBroadcastReceiver, CallFilter);

        Log.d("WatchMan : ", "\nmCallBroadcastReceiver Created....");

        mNotifyManager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this, null);
        mBuilder.setContentTitle("Test")
                .setContentText("test")
                .setTicker("test")
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_HIGH);

            // Configure the notification channel.
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            mNotifyManager.createNotificationChannel(notificationChannel);

            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
            startForeground(17, mBuilder.build());
        }
        else
        {
            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
            //startForeground(17, mBuilder.build());
            mNotifyManager.notify(17, mBuilder.build());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d("WatchMan : ", "\nmCallBroadcastReceiver Listening....");

        //return super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        this.unregisterReceiver(mCallBroadcastReceiver);
        Log.d("WatchMan : ", "\nDestroyed....");
        Log.d("WatchMan : ", "\nWill be created again....");
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}