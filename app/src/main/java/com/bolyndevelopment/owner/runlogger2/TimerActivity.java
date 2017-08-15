package com.bolyndevelopment.owner.runlogger2;

import android.databinding.DataBindingUtil;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.bolyndevelopment.owner.runlogger2.databinding.ActivityTimerBinding;

public class TimerActivity extends AppCompatActivity {
    static final String TAG = TimerActivity.class.getSimpleName();
    ActivityTimerBinding binder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_timer);
        binder = DataBindingUtil.setContentView(this, R.layout.activity_timer);
        binder.chronometer.setBase(SystemClock.elapsedRealtime());

        initButtons();
    }

    private void initButtons() {
        binder.startTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binder.chronometer.start();
            }
        });
        binder.stopTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binder.chronometer.stop();
            }
        });
        binder.lap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Time: " + binder.chronometer.getText());
            }
        });
    }

    private class LapAdapter extends RecyclerView.Adapter<LapAdapter.LapHolder> {

        @Override
        public LapHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return null;
        }

        @Override
        public void onBindViewHolder(LapHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 0;
        }

        class LapHolder extends RecyclerView.ViewHolder {
            public LapHolder(View itemView) {
                super(itemView);
            }
        }
    }
}
