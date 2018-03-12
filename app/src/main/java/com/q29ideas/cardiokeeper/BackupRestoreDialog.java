package com.q29ideas.cardiokeeper;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

//Created 9/17/2017.

public class BackupRestoreDialog extends DialogFragment implements View.OnClickListener{
    private ChoiceListener listener;
    private ViewGroup rootView;
    private String backupKey;

    static final int BACKUP_TO_PREVIOUS = 10;
    static final int BACKUP_TO_NEW = 20;
    static final int RESTORE_FROM_PREVIOUS = 30;
    static final int RESTORE_FROM_NEW = 40;

    int[] buttons = {R.id.backup_button, R.id.backup_same_location, R.id.backup_new_location,
            R.id.restore_button, R.id.restore_other_location, R.id.restore_last_location};

    interface ChoiceListener{
        void onChoiceSelected(final int choice);
    }

    @SuppressWarnings("ConstantConditions")
    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        listener = (ChoiceListener) getActivity();

        backupKey = getArguments().getString("backupKey");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        rootView = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.dialog_backup_restore, null);
        for (int button : buttons) {
            rootView.findViewById(button).setOnClickListener(this);
        }

        return builder.setView(rootView).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getDialog().dismiss();
            }
        }).create();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.backup_button:
                if (backupKey != null) {
                    rootView.findViewById(R.id.backup_mini_layout).setVisibility(View.VISIBLE);
                    final Button newLocale = rootView.findViewById(R.id.backup_new_location);
                    newLocale.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            listener.onChoiceSelected(BACKUP_TO_NEW);
                            getDialog().dismiss();
                        }
                    });
                    final Button sameLocale = rootView.findViewById(R.id.backup_same_location);
                    sameLocale.setText(backupKey.contains("google") ? "Backup to Google Drive" : "Backup to File System");
                    sameLocale.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            listener.onChoiceSelected(BACKUP_TO_PREVIOUS);
                            getDialog().dismiss();
                        }
                    });
                } else {
                    listener.onChoiceSelected(BACKUP_TO_NEW);
                    getDialog().dismiss();
                }
                break;
            case R.id.restore_button:
                if (backupKey != null) {
                    rootView.findViewById(R.id.restore_mini_layout).setVisibility(View.VISIBLE);
                    final Button otherLocale = rootView.findViewById(R.id.restore_other_location);
                    otherLocale.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            listener.onChoiceSelected(RESTORE_FROM_NEW);
                            getDialog().dismiss();
                        }
                    });
                    final Button lastLocale = rootView.findViewById(R.id.restore_last_location);
                    lastLocale.setText(backupKey.contains("google") ? "Restore from Google Drive" : "Restore from File System");
                    lastLocale.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            listener.onChoiceSelected(RESTORE_FROM_PREVIOUS);
                            getDialog().dismiss();
                        }
                    });
                } else {
                    listener.onChoiceSelected(RESTORE_FROM_NEW);
                    getDialog().dismiss();
                }
                break;
        }
    }
}