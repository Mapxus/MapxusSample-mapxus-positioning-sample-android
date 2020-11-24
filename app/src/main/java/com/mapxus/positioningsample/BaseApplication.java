package com.mapxus.positioningsample;

import android.app.Application;

import com.mapxus.map.mapxusmap.api.map.MapxusMapContext;
import com.mapxus.map.mapxusmap.api.map.model.Style;


public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //地图初始, 在AndroidManifest.xml中已配置appId跟secret的初始化方法
        MapxusMapContext.init(getApplicationContext());
        MapxusMapContext.getMapServiceProvider().setDefaultStyle(Style.MAPXUS_V2);

        //不配置xml文件，直接在代码中传入appId跟secret的初始化方法
        //MapxusMapContext.init(getApplicationContext(), "your appId", "your secret");
    }
}
