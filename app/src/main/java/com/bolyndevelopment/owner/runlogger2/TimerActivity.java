package com.bolyndevelopment.owner.runlogger2;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binder = DataBindingUtil.setContentView(this, R.layout.activity_timer);
        if (savedInstanceState == null) {
            //binder.chronometer.setBase(SystemClock.elapsedRealtime());
            lapList = new ArrayList<>();
            timeStopped = 0;
        } else {
            timeStopped = savedInstanceState.getLong("time");
            //binder.chronometer.setBase(SystemClock.elapsedRealtime() + timeStopped);
            this.onClick(binder.startTimer);
            lapList = savedInstanceState.getStringArrayList("list");
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
        outState.putLong("time", binder.chronometer.getBase() - SystemClock.elapsedRealtime());
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_timer:
                binder.chronometer.setBase(SystemClock.elapsedRealtime() + timeStopped);
                binder.chronometer.start();
                binder.startTimer.animate().alpha(0).start();
                binder.startTimer.setVisibility(View.INVISIBLE);
                binder.stopTimer.animate().alpha(1f).start();
                binder.stopTimer.setVisibility(View.VISIBLE);
                binder.lap.animate().alpha(1f).start();
                binder.lap.setVisibility(View.VISIBLE);
                break;

            case R.id.stop_timer:
                timeStopped = binder.chronometer.getBase() - SystemClock.elapsedRealtime();
                binder.chronometer.stop();
                binder.stopTimer.animate().alpha(0f).start();
                binder.stopTimer.setVisibility(View.INVISIBLE);
                binder.lap.animate().alpha(0f).start();
                binder.lap.setVisibility(View.INVISIBLE);
                binder.resumeTimer.animate().alpha(1f).start();
                binder.resumeTimer.setVisibility(View.VISIBLE);
                binder.resetTimer.animate().alpha(1f).start();
                binder.resetTimer.setVisibility(View.VISIBLE);
                break;
            case R.id.lap:
                Log.d(TAG, "lastTime: " + lastTime);
                long time =  Utils.getTimeLongMillis(binder.chronometer.getText().toString());
                Log.d(TAG, "time: " + time);
                long diff = time - lastTime;
                Log.d(TAG, "diff: " + diff);
                lastTime = time;
                Log.d(TAG, "lastTime: " + lastTime);
                lapList.add(0, String.valueOf(diff));
                binder.list.getAdapter().notifyItemInserted(0);
                binder.list.scrollToPosition(0);
                break;
            case R.id.resume_timer:
                binder.chronometer.setBase(SystemClock.elapsedRealtime() + timeStopped);
                binder.chronometer.start();
                binder.resumeTimer.animate().alpha(0f).start();
                binder.resumeTimer.setVisibility(View.INVISIBLE);
                binder.resetTimer.animate().alpha(0f).start();
                binder.resetTimer.setVisibility(View.INVISIBLE);
                binder.stopTimer.animate().alpha(1f).start();
                binder.stopTimer.setVisibility(View.VISIBLE);
                binder.lap.animate().alpha(1f).start();
                binder.lap.setVisibility(View.VISIBLE);
                break;
            case R.id.reset_timer:
                SaveDialog sd = new SaveDialog();
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.add(sd, "save");
                ft.commitAllowingStateLoss();
                /*
                binder.chronometer.setBase(SystemClock.elapsedRealtime());
                timeStopped = 0;
                binder.resumeTimer.animate().alpha(0f).start();
                binder.resumeTimer.setVisibility(View.INVISIBLE);
                binder.resetTimer.animate().alpha(0f).start();
                binder.resetTimer.setVisibility(View.INVISIBLE);
                binder.chronometer.setBase(SystemClock.elapsedRealtime());
                binder.startTimer.animate().alpha(1f).start();
                binder.startTimer.setVisibility(View.VISIBLE);
                lapList.clear();
                binder.list.getAdapter().notifyDataSetChanged();
                */
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
