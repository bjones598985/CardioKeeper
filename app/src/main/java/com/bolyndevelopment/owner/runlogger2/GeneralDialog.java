package com.bolyndevelopment.owner.runlogger2;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

//Created 9/27/2017.

public class GeneralDialog extends DialogFragment {
    View root;

    private GeneralDialogListener listener;

    private DialogInterface.OnClickListener allowPermClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
            getDialog().dismiss();
        }
    };

    interface GeneralDialogListener {
        void onGeneralDialogButtonClicked(int buttonId);
    }

    @SuppressLint("InflateParams")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments().getInt(MainActivity.DIALOG_TYPE) == MainActivity.DIALOG_ABOUT) {
            root = LayoutInflater.from(getActivity()).inflate(R.layout.about_dialog_layout, null);
            ImageView icon = (ImageView) root.findViewById(R.id.toasty_icon);
            Glide.with(this)
                    .load("https://raw.githubusercontent.com/GrenderG/Toasty/master/art/web_hi_res_512.png")
                    .into(icon);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        listener = (GeneralDialogListener) getActivity();

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //get int dialogType to determine which dialog to show
        int type = getArguments().getInt(MainActivity.DIALOG_TYPE);

        switch (type) {
            case MainActivity.DIALOG_ABOUT:
                builder.setTitle(null).setView(root)
                        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                                getDialog().dismiss();
                        }
                    });
                break;

            case MainActivity.DIALOG_ENABLE_BACKUP:
                builder.setView(R.layout.sync_dialog_layout)
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
                break;
            case MainActivity.DIALOG_PERMISSION:
                builder.setView(R.layout.general_dialog_textview)
                        .setPositiveButton(getString(R.string.allow_permission_msg), allowPermClickListener)
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                            }
                        });
                break;

        }
        AlertDialog ad = builder.create();
        //ad.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return ad;
    }
}