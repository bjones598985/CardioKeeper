package com.bolyndevelopment.owner.runlogger2;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.app.FragmentTransaction;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import com.bolyndevelopment.owner.runlogger2.databinding.ActivityTimerBinding;

import java.util.ArrayList;

public class TimerActivity extends AppCompatActivity implements View.OnClickListener {
    static final String TAG = TimerActivity.class.getSimpleName();
    ActivityTimerBinding binder;
    long timeStopped;
    ArrayList<String> lapList;
    Typeface digital, digitalItalic;
    long lastTime = 0;
    long base;
    long timeWhenStopped = 0;
    boolean isRunning;
    NotificationCompat.Builder builder;
    NotificationManager notiMgr;
    final int NOTI_ID = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binder = DataBindingUtil.setContentView(this, R.layout.activity_timer);
        isRunning = false;
        lapList = new ArrayList<>();
        if (savedInstanceState != null) {
            lapList = savedInstanceState.getStringArrayList("list");
            timeWhenStopped = savedInstanceState.getLong("time");
            isRunning = savedInstanceState.getBoolean("isRunning");
            long runningTime = savedInstanceState.getLong("runningTime");
            lastTime = savedInstanceState.getLong("lastTime");
            if (isRunning) {
                binder.chronometer.setBase(SystemClock.elapsedRealtime() - runningTime);
            } else {
                binder.chronometer.setBase(SystemClock.elapsedRealtime() - timeWhenStopped);
            }
        }
        if (isRunning) {
            binder.chronometer.start();
            hideButtons(binder.startTimer);
            showButtons(binder.stopTimer, binder.lap);
        }
        digitalItalic = Typeface.createFromAsset(getAssets(), "fonts/digital_italic.ttf");
        digital = Typeface.createFromAsset(getAssets(), "fonts/digital_mono.ttf");
        binder.chronometer.setTypeface(digital);

        initButtons();
        initRv();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList("list", lapList);
        outState.putLong("time", timeWhenStopped);
        outState.putLong("runningTime", SystemClock.elapsedRealtime() - binder.chronometer.getBase());
        outState.putBoolean("isRunning", isRunning);
        outState.putLong("lastTime", lastTime);
        super.onSaveInstanceState(outState);
    }

    private void initButtons() {
        binder.startTimer.setOnClickListener(this);
        binder.stopTimer.setOnClickListener(this);
        binder.lap.setOnClickListener(this);
        binder.resumeTimer.setOnClickListener(this);
        binder.resetTimer.setOnClickListener(this);
    }

    private void initRv() {
        binder.list.setHasFixedSize(true);
        binder.list.setLayoutManager(new LinearLayoutManager(this));
        binder.list.setAdapter(new LapAdapter());
    }

    private void createNotification() {
        builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("Cardio Keeper - Timer")
                .setContentText("Content Text")
                .setAutoCancel(true);
        Intent i = new Intent(this, TimerActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(TimerActivity.class);
        stackBuilder.addNextIntent(i);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        notiMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notiMgr.notify(NOTI_ID, builder.build());
        binder.chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                builder.setContentText(String.format("Time elapsed: %s", chronometer.getText().toString()));
                notiMgr.notify(NOTI_ID, builder.build());
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_timer:
                binder.chronometer.setBase(SystemClock.elapsedRealtime() - timeWhenStopped);
                binder.chronometer.start();
                hideButtons(binder.startTimer);
                showButtons(binder.stopTimer, binder.lap);
                isRunning = true;
                //createNotification();
                break;
            case R.id.stop_timer:
                binder.chronometer.stop();
                timeWhenStopped = SystemClock.elapsedRealtime() - binder.chronometer.getBase();
                hideButtons(binder.stopTimer, binder.lap);
                showButtons(binder.resumeTimer, binder.resetTimer);
                isRunning = false;
                break;
            case R.id.lap:
                long time =  Utils.getTimeLongMillis(binder.chronometer.getText().toString());
                long diff = time - lastTime;
                lastTime = time;
                lapList.add(0, String.valueOf(diff));
                binder.list.getAdapter().notifyItemInserted(0);
                binder.list.scrollToPosition(0);
                break;
            case R.id.resume_timer:
                binder.chronometer.setBase(SystemClock.elapsedRealtime() - timeWhenStopped);
                binder.chronometer.start();
                hideButtons(binder.resumeTimer, binder.resetTimer);
                showButtons(binder.stopTimer, binder.lap);
                isRunning = true;
                break;
            case R.id.reset_timer:
                SaveDialog sd = new SaveDialog();
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.add(sd, "save");
                ft.commitAllowingStateLoss();
        }
    }

    private void hideButtons(Button... buttons) {
        for (Button b : buttons) {
            b.animate().alpha(0f).start();
            b.setVisibility(View.INVISIBLE);
        }
    }

    private void showButtons(Button... buttons) {
        for (Button b : buttons) {
            b.animate().alpha(1f).start();
            b.setVisibility(View.VISIBLE);
        }
    }

    private void onSavePositiveClick() {
        Intent intent = new Intent();
        intent.putExtra("totalTime", binder.chronometer.getText().toString());
        intent.putStringArrayListExtra("list", lapList);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private class LapAdapter extends RecyclerView.Adapter<LapAdapter.LapHolder> {

        @Override
        public LapHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            //final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.timer_list_item, parent, false);
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.timer_list_item_alt, parent, false);
            return new LapHolder(view);
        }

        @Override
        public void onBindViewHolder(LapHolder holder, int position) {
            //holder.order.setText(String.valueOf(lapList.size() - position));
            String time = Utils.convertMillisToHms(Long.valueOf(lapList.get(position)));
            String s = "Lap " + String.valueOf(lapList.size() - position) + " - ";
            holder.lapOrder.setText(s);
            holder.lapTime.setText(time);
        }

        @Override
        public int getItemCount() {
            return lapList.size();
        }

        class LapHolder extends RecyclerView.ViewHolder {
            TextView lapOrder, lapTime;
            public LapHolder(View itemView) {
                super(itemView);
                lapOrder = (TextView) itemView.findViewById(R.id.lap_info_text);
                lapTime = (TextView) itemView.findViewById(R.id.lap_info);
                lapTime.setTypeface(digitalItalic);
            }
        }
    }

    public static class SaveDialog extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("You want to save and exit?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ((TimerActivity)getActivity()).onSavePositiveClick();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //onSaveCancelClick();
                        }
                    });
            return builder.create();
        }
    }
}