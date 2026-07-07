package com.mapxus.positioningsample;

import android.app.Application;

import com.mapxus.map.mapxusmap.api.map.MapxusMapContext;


public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //Map initialization. This init method is used when appId and secret are already configured in AndroidManifest.xml
        MapxusMapContext.init(getApplicationContext());
        //Without configuring the xml file, pass appId and secret directly in code
        //MapxusMapContext.init(getApplicationContext(), "your appId", "your secret");
    }
}
