package com.bolyndevelopment.owner.runlogger2;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

public class TimerBroadCastService extends Service {
    WeakReference<TextView> timerTextView;

    private long timerTime = 0, timeInstance = 0;
    private final int NOTIF_ID = 1000;
    private boolean isRunning = false;

    private NotificationManager mNM;
    private PendingIntent contentIntent;
    private Timer timer;

    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
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

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        contentIntent = PendingIntent.getActivity(this,
                0,new Intent(this, TimerActivity.class), 0);
        timer = new Timer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        timer.cancel();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new LocalBinder();

    private void showNotification() {
        contentIntent = PendingIntent.getActivity(this,
                0,new Intent(this, TimerActivity.class), 0);
        String title = getResources().getString(R.string.ticker);

        Notification.Builder notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)// the status icon
                .setTicker(title)// the status text
                .setContentTitle(title)// the label of the entry
                .setContentText("")// the contents of the entry
                .setContentIntent(contentIntent);// The intent to send when the entry is clicked

        startForeground(NOTIF_ID, notification.build());
    }

    public void onStartTimer() {
        showNotification();
        timer.scheduleAtFixedRate(timerTask, 1000, 1000);
        isRunning = true;
    }

    public void onStopTimer() {
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

    class LocalBinder extends Binder {
        TimerBroadCastService getService() {
            return TimerBroadCastService.this;
        }
    }
}