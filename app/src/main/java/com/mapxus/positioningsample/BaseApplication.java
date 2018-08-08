package com.mapxus.positioningsample;

import android.app.Application;

import com.mapxus.map.MapxusMapContext;

import timber.log.Timber;


public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MapxusMapContext.init(getApplicationContext()); //地图初始, 在AndroidManifest.xml中已配置appId跟secret的初始化方法
        //MapxusMapContext.init(getApplicationContext(), "your appId", "your secret"); //不配置xml文件，直接在代码中传入appId跟secret的初始化方法
        Timber.plant(new Timber.DebugTree());
    }
}
