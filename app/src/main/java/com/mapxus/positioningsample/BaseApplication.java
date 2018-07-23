package com.mapxus.positioningsample;

import android.app.Application;

import com.mapxus.beemap.BeeMapContext;

public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        BeeMapContext.init(getApplicationContext()); //地图初始
    }
}
