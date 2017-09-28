package com.bolyndevelopment.owner.runlogger2;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

/**
 * Created by Bobby Jones on 9/27/2017.
 */

public class GeneralDialog extends DialogFragment {
    private GeneralDialogListener listener;

    interface GeneralDialogListener {
        void onGeneralDialogButtonClicked(int buttonId);
    }
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        listener = (GeneralDialogListener) getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //get int dialogType to determine which dialog to show
        int type = getArguments().getInt(MainActivity.DIALOG_TYPE);

        if (type == MainActivity.DIALOG_ABOUT) {
            builder.setTitle(getResources().getString(R.string.about_dialog_title)).setView(R.layout.about_dialog_layout)
                    .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            getDialog().dismiss();
                        }
                    });
        } else {
            builder.setTitle(getResources().getString(R.string.sync_dialog_title)).setView(R.layout.sync_dialog_layout)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            listener.onGeneralDialogButtonClicked(which);
                            getDialog().dismiss();
                        }
                    })
                    .setNegativeButton("Not Now", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            listener.onGeneralDialogButtonClicked(which);
                            getDialog().dismiss();
                        }
                    });
        }
        return builder.create();
    }
}
