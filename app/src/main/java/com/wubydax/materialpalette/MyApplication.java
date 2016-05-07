package com.wubydax.materialpalette;

import android.app.Application;
import android.content.Context;

/**
 * Created by Anna Berkovitch on 26/03/2016.
 * For static context purposes
 */
public class MyApplication extends Application {
    private static Context CONTEXT;

    @Override
    public void onCreate() {
        super.onCreate();
        CONTEXT = this;
    }

    public static Context getContext(){
        return CONTEXT;
    }

    @Override
    public void onLowMemory() {
        Runtime.getRuntime().gc();
        super.onLowMemory();
    }
}
