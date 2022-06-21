package com.hossein.compass;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

public class App extends Application {

    @SuppressLint("StaticFieldLeak")
    private static Context context = null;

    @Override
    public void onCreate() {
        super.onCreate();
        if (context == null) {
            context = getApplicationContext();
        }
    }

    public static Context getContext() {
        return context;
    }
}
