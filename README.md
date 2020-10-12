# Mapxus Positioning Android SDK Sample App

This is a sample project to demonstrate how to use mapxus positioning android sdk.

# Table of Contents
- [Installation](#installation)
- [Mapxus Postioning Android SDK Instruction](#Mapxus-Positioning-Android-SDK-Instruction)
  - [About Mapxus Positioning Android SDK](#About-Mapxus-Positioning-Android-SDK)
  - [Install Mapxus Positioning Android SDK](#Installation)
    - [Create a Project](#Create-a-Project)
    - [Device Hardware Requirement](#Device-Hardware-Requirement)
  - [Access Location](#Access-Location)

# Installation

We highly recommend using the latest stable version of Android Studio (Current version: 4.0) to open this project.

Before running this project, please create the **secret.properties** file in the project root directory and fill in the application appid and secret in the following format:

	appid=
	secret=

Please contact us  <support@mapxus.com> to get appid and secret if you do not have them.

# Mapxus Postioning Android SDK Instruction

## About Mapxus Positioning Android SDK

Mapxus Positioning Android SDK allows you to do city-based seemless indoor-outdoor positioning.

[ ![Download](https://api.bintray.com/packages/mapxus/maven/positioning/images/download.svg) ](https://bintray.com/mapxus/maven/positioning/_latestVersion)

##  Install Mapxus Positioning Android SDK

### Create a Project

First of all, create a new Android project ([Instruction for creating an Android project](https://developer.android.com/studio/projects/create-project)). If you have already had an existing project, we highly recommend update Android Gradle Plugin.


If you are already using older version of mapxus positioning android SDK, please check the migration guide here.

Then, configure your project according to the following steps.


#### Step1: Add jcenter repository

Add jcenter repository to your root project's **build.gradle** file:

```grovvy

allprojects {
    repositories {
        jcenter()
        ...
    }
}

```

#### Step2: Add Positioning SDK Dependency
Add dependency in `build.gradle` file of necessary modules:

~~~groovy
dependencies {
	implementation 'com.mapxus.positioning:positioning:2.0.4'
}
~~~

***Note***：Please make sure that your project's minimum SDK Version is at 21 or above.

#### Step3: Set Java 8 Support

Please refer to Google about Java 8 language documentation and set Java 8 Support.
[https://developer.android.com/studio/write/java8-support](https://developer.android.com/studio/write/java8-support)

#### Step4: Set AndroidX Support

Please refer to Google about Migrating to AndroidX documentation and set AndroidX Support.
[https://developer.android.google.cn/jetpack/androidx/migrate](https://developer.android.google.cn/jetpack/androidx/migrate)


#### Step5: Prevent Obfuscation

Please configurate these in ProGuard to avoid obfuscation:

```
-keep class com.mapxus.positioning.** {*;}
-dontwarn com.mapxus.positioning.**
-keep class com.mapxus.map.auth.** {*;}
-dontwarn com.mapxus.map.auth.**

```

#### Step6: Set Key and Secret

There are two ways of adding app id and secret. You can choose one as needed.

##### Option 1: Configure the following codes in AndroidManifest.xml:

``` xml
	<meta-data
	        android:name="com.mapxus.api.v1.appid"
	        android:value="acquiredkey" />
	<meta-data
	    android:name="com.mapxus.api.v1.secret"
	    android:value="acquiredsecret" />
```

##### Option 2: Add app id and secret when getting MapxusPositioningClient instance


~~~java
MapxusPositioningClient mMapxusPositioningClient = 
MapxusPositioningClient.getInstance(this, "your appid", "your secret");
~~~


#### Step7: Set up Permissions in AndroidMainfest.xml in Your Application Manifest Files
~~~xml
<!--access information about networks to support relevant interface-->
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<!--to access approximate location through Internet-->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<!--to access precise location-->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<!--to access information about Wifi networks for positioning-->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<!--to change Wifi connectivity state -->
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
<!--to open network socket-->
<uses-permission android:name="android.permission.INTERNET" />
~~~

If your device is in Android 6.0 or above, when configuring your project, you need to grant all permissions as below.

For devices using Android 6.0 and above, if targetSdkVersion is less than 23, all permissions will be granted without asking. However, if targetSdkVersion is no more than 23, permissions will be asked. [Referrence: Guidebook of permissions requesting in Android](https://developer.android.com/training/permissions/requesting)


**Mapxus Positioning SDK** will ask these permissions：

*  `Manifest.permission.ACCESS_FINE_LOCATION`
*  `Manifest.permission.ACCESS_COARSE_LOCATION`


Remarks: You can refer to Positioning Sample APP about how to ask permissions dynamically.

### Device Hardware Requirement

1. Location is always updated through asking network during the process of positioning. Please make sure your **network** is available;

2. Location is always updated through asking Wifi network. Please **activate Wifi** during positioning;

3. In terms of devices (Android 6.0 or above) with native system or some third-parties, manufacturers customized system, Wifi information is not accessible because GPS is not on. In this case, it is impossible to access location. Therefore, when using postioning sdk in devices (Android 6.0 or above), **Location Service** should be available during positioning.

4. The positioning performance will be better if the device has pressure sensor.

5. Positioning SDK can automatically switch between indoor positioning and outdoor positioning. This feature only works for smart phones with raw GNSS measurements (mostly producing after 2016 and equipped with Android 7.0 and above). Click [here] (https://developer.android.com/guide/topics/sensors/gnss#supported-devices) to see details of supporting devices. If your devices are not able to support switch between indoor positioning and outdoor positioning, it will position indoor by default. You may use `Utils.isSupportGnss(Context context)` to determine.

6. When using **Mapxus Positioning SDK 2.0.0** and above version, you should activate precise **location service**. Otherwise, it will return `ERROR_LOCATION_SERVICE_DISABLED` error. By the following codes, you can set location precision:

~~~java
Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
startActivity(locationIntent);
~~~

# Access Location

## Examples

Example of MapxusPositioningClient (must be called at main thread):

~~~java
//declaire MapxusPositioningClient object in main thread
public MapxusPositioningClient mMapxusPositioningClient;

//access MapxusPositioningClient example
//Note: Because MapxusPositioningClient has implemented LifecycleObserver, the standard way to get the instance of a client is to call this method in LifecycleOwner.
mMapxusPositioningClient = MapxusPositioningClient.getInstance(this, getApplicationContext());

//set positioning listener
mMapxusPositioningClient.addPositioningListener(listener);
~~~

### Configure Parameters

Configure the positioning options. There are two available options: `NORMAL` and `NORMAL_WITH_WIFI_THROTTLING_DISABLED`. `NORMAL` mode is the default mode, NORMAL_WITH_WIFI_THROTTLING_DISABLED mode can be used to improve Android 10+ device's positioning performance, For more information, please check the Android 9+ device positioning section.

~~~java
//skip this option and positioning will take default value
MapxusPositioningOption option = new MapxusPositioningOption();

//configure positioning mode; NORMAL(default value)
positionerOption.setMode(LocationMode.NORMAL);

//configure positioning parameters; otherwise, default parameter will be taken when start positioning
mIndoorPositionerClient.setPositionerOption(option);
~~~

### Add Listener

Implement MapxusPositioningListener interface and access location 

~~~java
//implement MapxusPositioningListener
private MapxusPositioningListener listener = new MapxusPositioningListener() {
	@Override
	public void onStateChange(PositioningState state) {
		//callback of state change during positioning
		//RUNNING >>>> callback after starting/resuming positioner successfully
		//PAUSED  >>>> callback after pausing positioner successfully
		//STOPPED >>>> callback after stopping positioner successfully	}

	@Override
	public void onError(ErrorInfo errorInfo) {
		//callback of error information; check API documentation for detailed error information;
		//return of error information after positioning; if errorCode == ERROR_WARNING, position engine will keep running;
		//if other errors happen, engine will be destroyed after error return; then you should re-instantiate IndoorLocatorClient; initialize before location successfully may lead to error returns;	}

	@Override
	public void onOrientationChange(float orientation, @SensorAccuracy int accuracy) {
		//orientation change callback, value ranging [0, 360]; 0 due north, 90 east, 180 south, and 270 west;
		//The accuracy parameter is the accuracy level of the current orientation. 
		//You can tilt and move your phone like a 8-shape when the accuracy is not high
	}

	@Override
	public void onLocationChange(MapxusLocation location) {
		//location information callback, including building ID, floor, longitude&latitude, positioning accuracy
	}
	
};

~~~
### Start Positioning

Start positioning client from scratch (including acceptable initialization time).
~~~java
mMapxusPositioningClient.start(); //start positioning
~~~

### Pause Positioning

Automatically called when the corresponding activity is not in the foreground. This method can be called manually if you want fine grained control.
~~~java
mMapxusPositioningClient.pause();
~~~
### Resume Positioning

Automatically called when the corresponding activity is back to the foreground.
This method can be called manually if you want fine grained control.
~~~java
mMapxusPositioningClient.resume();
~~~
### Change Positioning Mode

Change positioning mode. This method can be called when the client is stopped.
~~~java
mMapxusPositioningClient.changeMode(newMode);
~~~
### Change Floor

Change floor in running state (invalid for the other states);
~~~java
mMapxusPositioningClient.changeFloor(floor);
~~~

### Stop Positioning

Stop positioning and destroy the corresponding resources.
~~~java
 mMapxusPositioningClient.stop();
~~~
### About Backgroung Positioning
For user privacy protection reason, we canceled the background positioning feature since 2.0.0.



