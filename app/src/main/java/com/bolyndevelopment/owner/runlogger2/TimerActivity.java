package com.bolyndevelopment.owner.runlogger2;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bolyndevelopment.owner.runlogger2.databinding.ActivityTimerBinding;

import java.util.ArrayList;

public class TimerActivity extends AppCompatActivity implements View.OnClickListener {
    static final int DIALOG_SAVE_AND_EXIT = 1;
    static final int DIALOG_CANCEL_TIMER = 2;
    static final String DIALOG_TYPE = "dialogType";

    long lastTime = 0;

    ActivityTimerBinding binder;
    ArrayList<String> lapList;
    Typeface digital, digitalItalic;

    TimerBroadCastService timerService;
    boolean isBound;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TimerBroadCastService.LocalBinder localBinder = (TimerBroadCastService.LocalBinder) service;
            timerService = localBinder.getService();
            timerService.setTimerTextView(binder.timeText);
            if (timerService.getIsRunning()) {
                hideButtons(binder.startTimer);
                showButtons(binder.stopTimer, binder.lap);
            }
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    public void onBackPressed() {
        decideHowToFinish();
    }

    private void decideHowToFinish() {
        if (timerService.getIsRunning()) {
            SaveDialog sd = new SaveDialog();
            Bundle b = new Bundle();
            b.putInt(DIALOG_TYPE, DIALOG_CANCEL_TIMER);
            b.putBoolean("isRunning", timerService.getIsRunning());
            sd.setArguments(b);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(sd, "save");
            ft.commitAllowingStateLoss();
        } else {
            clearServiceConnection();
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            decideHowToFinish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binder = DataBindingUtil.setContentView(this, R.layout.activity_timer);

        Intent intent = new Intent(this, TimerBroadCastService.class);
        startService(intent);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState != null) {
            lapList = savedInstanceState.getStringArrayList("list");
        }

        lapList = new ArrayList<>();
        digitalItalic = Typeface.createFromAsset(getAssets(), "fonts/digital_italic.ttf");
        digital = Typeface.createFromAsset(getAssets(), "fonts/digital_mono.ttf");
        binder.timeText.setTypeface(digital);

        initButtons();
        initRv();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList("list", lapList);
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

    private void clearServiceConnection() {
        unbindService(connection);
        stopService(new Intent(this, TimerBroadCastService.class));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_timer:
                timerService.onStartTimer();
                hideButtons(binder.startTimer);
                showButtons(binder.stopTimer, binder.lap);
                break;
            case R.id.stop_timer:
                timerService.onStopTimer();
                hideButtons(binder.stopTimer, binder.lap);
                showButtons(binder.resumeTimer, binder.resetTimer);
                break;
            case R.id.lap:
                long time = timerService.getTimerTime() * 1000;
                long diff = time - lastTime;
                lastTime = time;
                lapList.add(0, String.valueOf(diff));
                binder.list.getAdapter().notifyItemInserted(0);
                binder.list.scrollToPosition(0);
                break;
            case R.id.resume_timer:
                timerService.onResumeTimer();
                hideButtons(binder.resumeTimer, binder.resetTimer);
                showButtons(binder.stopTimer, binder.lap);
                break;
            case R.id.reset_timer:
                SaveDialog sd = new SaveDialog();
                Bundle b = new Bundle();
                b.putInt(DIALOG_TYPE, DIALOG_SAVE_AND_EXIT);
                sd.setArguments(b);
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
        intent.putExtra("totalTime", binder.timeText.getText().toString());
        intent.putStringArrayListExtra("list", lapList);
        setResult(Activity.RESULT_OK, intent);
        clearServiceConnection();
        finish();
    }

    private class LapAdapter extends RecyclerView.Adapter<LapAdapter.LapHolder> {

        @Override
        public LapHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.timer_list_item_alt, parent, false);
            return new LapHolder(view);
        }

        @Override
        public void onBindViewHolder(LapHolder holder, int position) {
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
            LapHolder(View itemView) {
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
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.DialogStyle);
            int dialogType = getArguments().getInt(DIALOG_TYPE);
            TextView root;
            if (dialogType == DIALOG_SAVE_AND_EXIT) {
                root = (TextView) getActivity().getLayoutInflater().inflate(R.layout.general_dialog_textview, null);
                root.setText(getString(R.string.dialog_save_exit_msg));
                builder.setView(root)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                ((TimerActivity) getActivity()).onSavePositiveClick();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
            } else {
                final boolean isRunning = getArguments().getBoolean("isRunning");
                String msg = isRunning ? getResources().getString(R.string.dialog_back_press_exit_running_msg) :
                        getResources().getString(R.string.dialog_save_exit_msg);
                root = (TextView) getActivity().getLayoutInflater().inflate(R.layout.general_dialog_textview, null);
                root.setText(msg);
                builder.setView(root)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (isRunning) {
                                    ((TimerActivity) getActivity()).clearServiceConnection();
                                    getActivity().finish();
                                } else {
                                    ((TimerActivity) getActivity()).onSavePositiveClick();
                                }
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                getDialog().dismiss();
                            }
                        });
            }
            AlertDialog ad = builder.create();
            ad.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            return ad;
        }
    }
}
