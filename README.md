[API reference](https://mapxussample.github.io/mapxus-positioning-sdk-android/)

# Mapxus Positioning Android SDK使用说明

## 1. 创建工程

### 1.1 创建Android项目

首先创建一个新的 Android 项目（[Android创建项目指南](https://developer.android.com/studio/projects/create-project)），然后按照以下步骤手动集成。

### 1.2 配置项目

#### 1.2.1 配置项目根目录下的build.gradle文件

在build.gradle中添加maven库地址及申请的账号密码

~~~groovy
allprojects {
	repositories {
		maven {
			credentials {
				username PARTNER_USER
				password PARTNER_PASSWORD
			}
			url PARTNER_REPOSITORY_URL
		}
	}
}
~~~
#### 1.2.2 在**gradle.properties**文件中, 配置仓库地址及账号密码
在Global或Project的**gradle.properties**文件配置都可以，不过建议在 Global Properties 中配置这样其他项目使用时就不用重复配置

~~~
PARTNER_USER=mapxus
PARTNER_PASSWORD=f(Qz3%JjGcg9
PARTNER_REPOSITORY_URL=http://nexus.maphive.io:8081/nexus/content/repositories/partner/
~~~

如果没有**gradle.properties**文件创建方式：

* Project Properties：项目根目录直接新建**gradle.properties**文件
* Global Properties：在系统**.gradle**文件下创建新**gradle.properties**文件，Mac默认路径：**/Users/用户名/.gradle**，执行 **touch gradle.properties**
，Windows默认路径：**C:\Users\用户名\.gradle**

#### 1.2.3 添加授权的 app id 和 secret

传入 app id 和 secret 有以下两种方式，可以按照自己的需要选择其中一种：

(1). 在 **AndroidManifest.xml** 文件的 *application* 节点中添加 *meta-data*

```xml
<application ...
	<meta-data
	      android:name="com.mapxus.api.v1.appid"
	      android:value="{appid}" />
	<meta-data
	      android:name="com.mapxus.api.v1.secret"
	      android:value="{secret}" />
</application>
```

(2). 在获取定位服务客户端MapxusPositioningClient的实例时传入 app id 和 secret

~~~java
MapxusPositioningClient mMapxusPositioningClient = 
MapxusPositioningClient.getInstance(this, "your appid", "your secret");
~~~


#### 1.2.4 在需要使用到SDK的module的build.gradle文件中添加SDK依赖

	implementation 'com.mapxus.positioning:positioning:0.2.19'

***注意***：Mapxus Positioning SDK 使用限制minSdkVersion 19，所以请运行在Android4.4或以上

#### 1.2.5 在项目清单文件AndroidMainfest.xml里添加定位需要使用的所有权限
~~~xml
<!-- 获取运营商信息，用于支持提供运营商信息相关的接口-->
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<!--用于进行网络定位-->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<!--用于访问GPS定位-->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<!-- 用于访问wifi网络信息，wifi信息会用于进行网络定位-->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<!-- 这个权限用于获取wifi的获取权限，wifi信息会用来进行网络定位 -->
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
<!-- 访问网络，定位需要上网-->
<uses-permission android:name="android.permission.INTERNET" />
<!-- 用于读取手机当前的状态-->
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<!--读写外部存储的权限，隐式赋予了读外部存储的权限-->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
~~~
#### 1.2.6 Android 6.0及以上运行时检查和请求权限，确认应用被授予必要的权限

运行在Android6.0及以上设备如果项目配置targetSdkVersion小于23则会默认授予了所有申请的权限不需要检查和请求权限， 如果 targetSdkVersion >= 23 则需要。[Android在运行时请求权限指南](https://developer.android.com/training/permissions/requesting)

Mapxus Positioning SDK 需要检测获取的权限有：

*  `Manifest.permission.ACCESS_FINE_LOCATION`
*  `Manifest.permission.ACCESS_COARSE_LOCATION`
*  `Manifest.permission.READ_PHONE_STATE`
*  `Manifest.permission.WRITE_EXTERNAL_STORAGE`

PS：关于如何动态请求权限详细可参考Sample APP

### 1.3 设备状态的开启

1. 请在整个定位过程中确保网络可用，定位过程需要不断进行网络请求获取最新位置信息

2. 请开启WIFI，定位过程需要不断获取WIFI信息进行定位

3. Android6.0及以上设备出现在原生系统和部分第三方、厂商定制系统因没有开启GPS导致无法获取WIFI信息而不能进行定位的问题，所以Mapxus Positioning SDK在Android6.0及以上设备运行时必须开启GPS以防止无法使用

4. 确保使用Mapxus Positioning SDK的设备具有气压传感器，否则无法使用

## 2.获取室内定位

### 2.1 获取实例

以下为获取IndoorPositionerClient的示例代码：

~~~java
//在主线程中声明MapxusPositioningClient对象
public MapxusPositioningClient mMapxusPositioningClient;

//获取MapxusPositioningClient定位服务客户端的实例
mMapxusPositioningClient = MapxusPositioningClient.getInstance(getApplicationContext());

//设置定位回调监听
mMapxusPositioningClient.setPositioningListener(listener);
~~~

### 2.2 配置定位参数

~~~java
//不设置此option则定位时会使用默认值
MapxusPositioningOption option = new MapxusPositioningOption();

//配置定位模式，NORMAL BACKGROUND GAME可选默认NORMAL（BACKFROUND、GAME模式暂时不可用）
positionerOption.setMode(LocationMode.NORMAL);

//设置定位参数，不配置则会使用默认参数开始定位
mIndoorPositionerClient.setPositionerOption(option);
~~~

### 2.3 获取定位

实现MapxusPositioningListener接口，获取定位结果

~~~java
//实现MapxusPositioningListener监听接口
private MapxusPositioningListener listener = new MapxusPositioningListener() {
	@Override
	public void onStateChange(PositioningState state) {
		//定位过程中状态变化回调
		//WAITING >>>> 调用start()到成功获取定位位置前的等待状态
		//RUNNING >>>> 定位成功后的运行状态
		//PAUSED  >>>> 调用pause()后成功暂停返回暂停状态
		//STOPPED >>>> 调用stop()后成功停止返回停止状态
	}

	@Override
	public void onError(ErrorInfo errorInfo) {
		//错误信息回调，各类错误信息解释请查看API文档
		//开始定位后出现的错误信息的返回 errorCode == ERROR_WARNING 时定位引擎会抛出此错误信息后仍继续运行
		//其余错误返回后会自动销毁定位引擎，再使用须重新实例化IndoorLocatorClient 定位成功前初始化可能出现多个错误返回
	}

	@Override
	public void onOrientationChange(float orientation) {
		//当前角度信息回调，取值范围在[0, 360]间，0代表正北方，90代表正东，180代表正南，270代表正西
	}

	@Override
	public void onLocationChange(PositioningLocation location) {
		//定位位置信息回调
	}

	@Override
	public void onBuildingChange(PositioningBuilding building) {
		//定位建筑变化回调
	}

	@Override
	public void onFloorChange(PositioningFloor floor) {
		//定位楼层变化回调
	}
};

~~~
### 2.4 开始定位

~~~java
//调用start()之后只需要等待定位结果自动返回即可, 定位成功后会不断地请求定位更新位置
//只在第一次调用有效，其他状态下调用无效。重新start()请stop()后重新获取MapxusPositioningClient实例执行
//开始成功返回PositioningState.RUNNING
mMapxusPositioningClient.start(); //开始定位
~~~

### 2.5 暂停定位
~~~java
//暂停运行状态（其他状态下调用无效）的定位引擎，不再请求定位但不销毁定位服务
//暂停成功返回PositioningState.PAUSED
mMapxusPositioningClient.pause();
~~~
### 2.6 恢复定位
~~~java
//恢复暂停状态（其他状态下调用无效）的定位引擎，继续不断地请求定位更新位置
//恢复成功返回PositioningState.RUNNING
mMapxusPositioningClient.resume();
~~~
### 2.7 切换定位模式
~~~java
//在定位成功后的运行状态（其他状态下调用无效）切换定位模式（此方法暂时不可用）
mMapxusPositioningClient.changeMode(newMode);
~~~
### 2.8 切换定位楼层
~~~java
//在定位成功后的运行状态（其他状态下调用无效）切换定位定位楼层
mMapxusPositioningClient.changeFloor(floor);
~~~
### 2.9 停止定位
~~~java
//在开始后的任意状态调用均有效，可直接停止等待、暂停、运行状态下的定位服务，停止成功返回PositionerState.STOPED
//停止定位，销毁定位引擎，再使用需要重新获取MapxusPositioningClient实例
 mMapxusPositioningClient.stop();
~~~
### 2.10 开启后台定位服务
~~~java
//开启后台定位服务。
//Android6.0 以上为了节能优化，app退到后台一段时间没有使用系统会判断它处于空闲状态
//停用网络访问以及暂停同步和作业导致无法正常使用定位服务。开启后能防止出现这种情况
mMapxusPositioningClient.enableBackgroundPositioning(1, notification);
~~~
### 2.11 关闭后台定位服务
~~~java
//app返回前台后可关闭后台定位服务
mMapxusPositioningClient.disableBackgroundPositioning(true);
~~~



