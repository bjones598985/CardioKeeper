package com.q29ideas.cardiokeeper;

import android.app.Application;
import android.content.Context;

//Created 7/23/2017.

public class MyApplication extends Application {
    public static Context appContext = null;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }
}
