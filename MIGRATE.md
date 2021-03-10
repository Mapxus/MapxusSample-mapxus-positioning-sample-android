# Mapxus Positioning Android SDK Migration Guidance

Mapxus positioning android sdk has a big major update since 2.0.0, 1.0.0 and less version sdk users are highly recommended to do this upgrade. Please follow the detailed guidance.  
If you have any problem migrating to 2.x.x, please contact us <support@mapxus.com>. For more information of mapxus indoor location service, please check our website: [https://www.mapxus.com/](https://www.mapxus.com/).

## Introduction

This document is about how to migrate to latest Mapxus Positioning Android SDK.

## Guidance

### Step1: Set AndroidX Support

Please refer to Google about Migrating to AndroidX documentation and set AndroidX Support. [https://developer.android.google.cn/jetpack/androidx/migrate](https://developer.android.google.cn/jetpack/androidx/migrate)

### Step2: Change version of positioning dependency declaration

Notice:


Old version dependencies declaration:
```
//positioning
"com.mapxus.positioning:positioning:1.0.0" and below

```

New version dependencies declaration:
```
//mapxus
"com.mapxus.positioning:positioning:2.0.8"

```

### Step3: Class name change

Here is the Mapping table between old class name and new class name

Old class name  |  New class name
----- | ------
com.mapxus.signal.place.Floor | com.mapxus.positioning.positioning.api.MapxusFloor
com.mapxus.positioning.model.location.PositioningLocation | com.mapxus.positioning.positioning.api.MapxusLocation
com.mapxus.positioning.config.MapxusPositioningOption | com.mapxus.positioning.positioning.api.MapxusPositioningOption
om.mapxus.positioning.model.info.LocationMode | com.mapxus.positioning.positioning.api.PositioningMode
com.mapxus.positioning.model.info.PositioningState | com.mapxus.positioning.positioning.api.PositioningState
com.mapxus.positioning.model.info.SensorAccuracy | com.mapxus.positioning.positioning.api.SensorAccuracy
com.mapxus.positioning.model.info.ErrorInfo | com.mapxus.positioning.positioning.api.ErrorInfo

### Step4: Method name change
Old method name  |  New method name
----- | ------
com.mapxus.positioning.model.location.PositioningLocation.getLat() | com.mapxus.positioning.positioning.api.MapxusLocation.getLatitude()
com.mapxus.positioning.model.location.PositioningLocation.getLon() | com.mapxus.positioning.positioning.api.MapxusLocation.getLongitude()
com.mapxus.positioning.model.location.PositioningLocation.getFloor() | com.mapxus.positioning.positioning.api.MapxusLocation.getMapxusFloor()
com.mapxus.positioning.positioning.api.MapxusPositioningClient.setPositioningListener() | com.mapxus.positioning.positioning.api.MapxusPositioningClient.addPositioningListener()
com.mapxus.positioning.positioning.api.MapxusPositioningClient.getInstance(getApplicationContext()) | com.mapxus.positioning.positioning.api.MapxusPositioningClient.getInstance(? extends androidx.lifecycle.LifecycleOwner, getApplicationContext())



## More

For more instructions , please check in the README.md.