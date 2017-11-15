package com.bolyndevelopment.owner.runlogger2;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

public class TimerBroadCastService extends Service {
    public static final String TAG = TimerBroadCastService.class.getSimpleName();
    WeakReference<TextView> timerTextView;

    private Handler handler = new Handler();
    long timerTime = 0;
    long timeInstance = 0;
    private NotificationManager mNM;
    private final int NOTIF_ID = 1000;
    boolean isRunning = false;
    PendingIntent contentIntent;
    Thread timerThread;
    Timer timer;

    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            Log.d(TAG, "Time: " + timerTime);
            timerTime = timerTime + 1L;
            if (isRunning) {
                if (timerTextView.get() != null) {
                    timerTextView.get().post(new Runnable() {
                        @Override
                        public void run() {
                            timerTextView.get().setText(Utils.convertSecondsToHms(timerTime));
                        }
                    });
                }
                updateNotification();
            }
        }
    };

    public TimerBroadCastService() {

    }

    public void setTimerTextView(TextView tv) {
        timerTextView = new WeakReference<>(tv);
        timerTextView.get().setText(Utils.convertSecondsToHms(timerTime));
    }

    private void updateNotification() {
            Notification.Builder notification = new Notification.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)// the status icon
                    .setTicker("Ticker Text")// the status text
                    .setContentTitle("Timer Service")// the label of the entry
                    .setContentText("Time Elapsed - " + Utils.convertSecondsToHms(timerTime))// the contents of the entry
                    .setContentIntent(contentIntent);// The intent to send when the entry is clicked
            mNM.notify(NOTIF_ID, notification.build());
    }
    @Override
    public void onCreate() {
        Toast.makeText(this, "Local Service Started", Toast.LENGTH_SHORT).show();
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        //Intent i = new Intent(this, TimerActivity.class);
        //i.putExtra("fromNoti", true);
        contentIntent = PendingIntent.getActivity(this,
                0,new Intent(this, TimerActivity.class), 0);
        //showNotification();
        timer = new Timer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Local Service Stopped", Toast.LENGTH_SHORT).show();
        timer.cancel();
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
        contentIntent = PendingIntent.getActivity(this,
                0,new Intent(this, TimerActivity.class), 0);

        Intent i = new Intent(this, TimerActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(TimerActivity.class);
        stackBuilder.addNextIntent(i);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)// the status icon
                .setTicker("Ticker Text")// the status text
                .setContentTitle("Timer Service")// the label of the entry
                .setContentText("Where we'll post the timerTime elapsed")// the contents of the entry
                .setContentIntent(contentIntent);// The intent to send when the entry is clicked

        startForeground(NOTIF_ID, notification.build());
    }

    public void onStartTimer() {
        Toast.makeText(TimerBroadCastService.this, "onStart", Toast.LENGTH_SHORT).show();
        isRunning = true;
        showNotification();
        timer.scheduleAtFixedRate(timerTask, 1000, 1000);
        //handler.removeCallbacks(task);
        //handler.post(task);
        //timerThread.start();
    }

    public void onStopTimer() {
        Toast.makeText(TimerBroadCastService.this, "onStop", Toast.LENGTH_SHORT).show();
        timeInstance = timerTime;
        isRunning = false;
    }

    public void onResumeTimer() {
        timerTime = timeInstance;
        isRunning = true;
    }

    public boolean getIsRunning() {
        return isRunning;
    }

    public long getTimerTime() {
        return timerTime;
    }
    public class LocalBinder extends Binder {
        TimerBroadCastService getService() {
            return TimerBroadCastService.this;
        }
    }
}
