package com.bolyndevelopment.owner.runlogger2;

import android.Manifest;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import static com.bolyndevelopment.owner.runlogger2.MainActivityAlt.DIALOG_ABOUT;
import static com.bolyndevelopment.owner.runlogger2.MainActivityAlt.MIN_DELAY_MILLIS;
import static com.bolyndevelopment.owner.runlogger2.MainActivityAlt.SETTINGS_CODE;
import static com.bolyndevelopment.owner.runlogger2.MainActivityAlt.WRITE_REQUEST_CODE;

public class NavFragment extends Fragment implements View.OnClickListener{
    private NavFragListener mListener;
    private Handler handler = new Handler();

    public interface NavFragListener {
        void onBackUpRestoreClicked();
        void onMenuOptionClicked(View view);
    }

    public NavFragment() {
        // Required empty public constructor
    }

    public static NavFragment newInstance(String param1, String param2) {
        return new NavFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nav, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Glide.with(this).asBitmap()
                .load(R.drawable.card_keep_finish_v5)
                .into((ImageView) view.findViewById(R.id.app_icon_imageview));
        view.findViewById(R.id.nav_menu_graph).setOnClickListener(this);
        view.findViewById(R.id.nav_menu_backup).setOnClickListener(this);
        view.findViewById(R.id.nav_menu_settings).setOnClickListener(this);
        view.findViewById(R.id.nav_menu_about).setOnClickListener(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof NavFragListener) {
            mListener = (NavFragListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement ListFragListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onClick(View v) {
        mListener.onMenuOptionClicked(v);
    }
}
