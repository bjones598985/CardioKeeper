package com.bolyndevelopment.owner.runlogger2;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

public class TimerService extends Service {
    Chronometer chron;
    TextView timerFace;
    Notification.Builder notification;

    public TimerService() {
    }

    private NotificationManager mNM;

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = 1000;

    /**
      * Class for clients to access.  Because we know this service always
      * runs in the same process as its clients, we don't need to deal with
      * IPC.
    */
    public class LocalBinder extends Binder {
        TimerService getService() {
            return TimerService.this;
        }
    }

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        chron = new Chronometer(this);
        chron.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                Log.d("TimerService", "Tick tock");
                //Toast.makeText(TimerService.this, chronometer.getText(), Toast.LENGTH_SHORT).show();

                //mNM.notify(NOTIFICATION, notification.build());
            }
        });
        //timerFace = new TextView(this);

        Toast.makeText(this, "Local Service Started", Toast.LENGTH_SHORT).show();

        // Display a notification about us starting.  We put an icon in the status bar.
        //showNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        //mNM.cancel(NOTIFICATION);

        // Tell the user we stopped.
        Toast.makeText(this, "Local Service Stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
            return mBinder;
        }

        // This is the object that receives interactions from clients.  See
        // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    /**
    * Show a notification while this service is running.
    */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = "Local Service Started";

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this,
                0,new Intent(this, TimerActivity.class), 0);

        Intent i = new Intent(this, TimerActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(TimerActivity.class);
        stackBuilder.addNextIntent(i);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Set the info for the views that show in the notification panel.
        Notification.Builder notification = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.arrow_down_float)// the status icon
                .setTicker(text)// the status text
                .setWhen(System.currentTimeMillis())// the timerTime stamp
                .setContentTitle("Label")// the label of the entry
                .setContentText(text)// the contents of the entry
                .setContentIntent(contentIntent);// The intent to send when the entry is clicked



        // Send the notification.
        //mNM.notify(NOTIFICATION, notification);
        startForeground(NOTIFICATION, notification.build());
    }

    public void onStartTimer(long time) {
        Toast.makeText(TimerService.this, "onStart", Toast.LENGTH_SHORT).show();
        chron.setBase(time);

        showNotification();

        chron.start();
    }

    public long onStopTimer() {
        Toast.makeText(TimerService.this, "onStop", Toast.LENGTH_SHORT).show();
        chron.stop();
        return chron.getBase();
    }

    public long getChronBase() {
        Toast.makeText(TimerService.this, "getChronBase", Toast.LENGTH_SHORT).show();
        return chron.getBase();
    }

    public void setChronBase(long time) {

    }

    public String getChronTimeValue() {
        Toast.makeText(TimerService.this, "getChronTimeValue", Toast.LENGTH_SHORT).show();
        return chron.getText().toString();
    }

    public void setTimerFace(TextView face) {
        Toast.makeText(TimerService.this, "setTimerFace", Toast.LENGTH_SHORT).show();
        this.timerFace = face;
    }
}