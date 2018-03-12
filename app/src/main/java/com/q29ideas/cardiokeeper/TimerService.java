package com.q29ideas.cardiokeeper;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

public class TimerService extends Service {
    WeakReference<TextView> timerTextView, lapTimeTextView;

    private long timerTime = 0, timeInstance = 0, lapTime = 0;
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

    private TimerTask lapTimerTask = new TimerTask() {
        @Override
        public void run() {
            lapTime = lapTime + 1L;
            if (isRunning) {
                if (lapTimeTextView.get() != null) {
                    lapTimeTextView.get().post(new Runnable() {
                        @Override
                        public void run() {
                            lapTimeTextView.get().setText(Utils.convertSecondsToHms(lapTime));
                        }
                    });
                }
            }
        }
    };

    public TimerService() {

    }

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
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
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        Intent i = new Intent(this, MainActivityAlt.class);
        i.putExtra("recreate", true);
        stackBuilder.addParentStack(MainActivityAlt.class);
        stackBuilder.addNextIntent(i);
        contentIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)// the status icon
                .setContentIntent(contentIntent);// The intent to send when the entry is clicked

        startForeground(NOTIF_ID, notification.build());
    }

    public void onStartTimer() {
        showNotification();
        timer.scheduleAtFixedRate(timerTask, 1000, 1000);
        timer.scheduleAtFixedRate(lapTimerTask, 1000, 1000);
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
        lapTime = -1L;
        return timerTime;
    }

    public void setTimerTextViews(TextView tv, TextView lapTv) {
        timerTextView = new WeakReference<>(tv);
        timerTextView.get().setText(Utils.convertSecondsToHms(timerTime));
        lapTimeTextView = new WeakReference<>(lapTv);
        lapTimeTextView.get().setText(Utils.convertSecondsToHms(lapTime));
    }

    private void updateNotification() {
        String mainText = "Time Elapsed: " + Utils.convertSecondsToHms(timerTime);
        String secondartyText = "Lap Time: " + Utils.convertSecondsToHms(lapTime);
        Notification.Builder notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)// the status icon
                .setTicker(mainText)// the status text
                .setContentTitle(mainText)// the label of the entry
                .setContentText(secondartyText)// the contents of the entry
                .setContentIntent(contentIntent);// The intent to send when the entry is clicked
        mNM.notify(NOTIF_ID, notification.build());
    }

    class LocalBinder extends Binder {
        TimerService getService() {
            return TimerService.this;
        }
    }
}