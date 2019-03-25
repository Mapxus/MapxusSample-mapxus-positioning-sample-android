package com.mapxus.positioningsample;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.AlertDialog.Builder;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapxus.map.MapViewProvider;
import com.mapxus.map.MapxusMap;
import com.mapxus.map.impl.MapboxMapViewProvider;
import com.mapxus.map.interfaces.OnMapxusMapReadyCallback;
import com.mapxus.map.model.IndoorBuilding;
import com.mapxus.map.model.Style;
import com.mapxus.positioning.config.MapxusPositioningOption;
import com.mapxus.positioning.model.info.ErrorInfo;
import com.mapxus.positioning.model.info.LocationMode;
import com.mapxus.positioning.model.info.PositioningState;
import com.mapxus.positioning.model.info.SensorAccuracy;
import com.mapxus.positioning.model.location.PositioningFloor;
import com.mapxus.positioning.model.location.PositioningLocation;
import com.mapxus.positioning.positioning.api.MapxusPositioningClient;
import com.mapxus.positioning.positioning.api.MapxusPositioningListener;
import com.mapxus.positioning.utils.Utils;
import com.mapxus.positioningsample.databinding.ActivityPositionBinding;
import com.mapxus.services.BuildingSearch;
import com.mapxus.services.model.building.BuildingDetailResult;
import com.mapxus.services.model.building.BuildingResult;
import com.mapxus.services.model.building.IndoorBuildingInfo;


public class PositionActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "PositionActivity";
    private ActivityPositionBinding mPositionBinding;

    /**
     * 添加地圖图标的参数
     */
    private static final String POSITION_MARKER_SOURCE = "position_marker_source";
    private static final String POSITION_MARKER_LAYER = "position_marker_layer";
    private static final String POSITION_MARKER_IMAGE = "position_marker_image";

    /**
     * 添加地图图标的数据
     */
    private GeoJsonSource positionMarkerSource;
    private SymbolLayer positionSymbolLayer;

    private MapboxMap mMapboxMap; //Mapbox Map
    private MapView mMapView;

    private MapxusMap mMapxusMap; //Mapxus Map
    private MapViewProvider mMapViewProvider;

    private MapxusPositioningClient mMapxusPositioningClient;//定位服务客户端

    private float mCurrentRotation; //定位角度
    private PositioningLocation mPositionLocation; //定位位置(包括floor、，buildingId, buildingId为空则定位到室外)

    private String mMapFloor; //当前地图的显示楼层
    private IndoorBuildingInfo mPositionBuildingInfo; //定位建筑详细信息
    private boolean isShowInCenter = true; //第一次显示切换地图中心点位置

    private PositionViewModel mPositionViewModel;

    private NotificationManager mNotificationManager;

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_position);
        mPositionBinding = DataBindingUtil.setContentView(this, R.layout.activity_position);
        mPositionViewModel = new PositionViewModel();
        initMap(savedInstanceState);
        initView();
    }

    /**
     * 初始化地图
     * 添加地图楼层变换监听
     *
     * @param savedInstanceState
     */
    private void initMap(Bundle savedInstanceState) {
        mMapView = mPositionBinding.map;
        mMapViewProvider = new MapboxMapViewProvider(this, mMapView);
        mMapViewProvider.setStyle(Style.MAPPYBEE); //set indoor map style
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                mMapboxMap = mapboxMap; //获取地图准备好后的MapboxMap实例
            }
        });
        mMapViewProvider.getMapxusMapAsync(new OnMapxusMapReadyCallback() {
            @Override
            public void onMapxusMapReady(MapxusMap mapxusMap) {
                mMapxusMap = mapxusMap; //获取室内地图准备好后的MapxusMap实例
                //楼层变化时marker的显示
                mMapxusMap.addOnFloorChangeListener(new MapxusMap.OnFloorChangeListener() {
                    @Override
                    public void onFloorChange(IndoorBuilding indoorBuilding, String floorCode) {
                        mMapFloor = floorCode;
                        if (null != mPositionLocation) {
                            //楼层不同则不显示定位图标
                            if (mPositionBinding.optionContent.getIsIndoor()) {
                                if (null != mPositionLocation.getFloor() &&
                                        floorCode.equals(mPositionLocation.getFloor().getCode())) {
                                    updatePositionMarker(new LatLng(mPositionLocation.getLat(),
                                            mPositionLocation.getLon()));
                                } else {
                                    Log.d(TAG, "Floor change. Map floor is different from positioning floor, remove marker");
                                    removePositionMarker();
                                }
                            }
                        }
                    }
                });
                //地图室内外切换时, 地图楼层重置为空
                mMapxusMap.addOnBuildingChangeListener(new MapxusMap.OnBuildingChangeListener() {
                    @Override
                    public void onBuildingChange(IndoorBuilding indoorBuilding) {
                        if (null == indoorBuilding) {
                            mMapFloor = null;
                            Log.d(TAG, "Map change to outdoor.");
                        }
                    }
                });
            }
        });
    }

    /**
     * 设置控件监听
     */
    private void initView() {
        mPositionBinding.optionContent.optionLayout.bringToFront();
        mPositionBinding.optionContent.start.setOnClickListener(this);
        mPositionBinding.optionContent.pause.setOnClickListener(this);
        mPositionBinding.optionContent.resume.setOnClickListener(this);
        mPositionBinding.optionContent.stop.setOnClickListener(this);
        mPositionBinding.optionContent.setIsIndoor(true);
        mPositionBinding.optionContent.setIsRunning(false);
        mPositionBinding.optionContent.version.setText("version: " + BuildConfig.VERSION_NAME);
        boolean isSupportGnss = Utils.isSupportGnss(this);
        Log.i(TAG, "Is support gnss " + isSupportGnss);
        enableStartUI();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start:
                mPositionBinding.optionContent.locationModeSpinner.setEnabled(false);
                enableRunningUI();
                startPosition();
                break;
            case R.id.pause:
                enablePauseUI();
                pausePosition();
                break;
            case R.id.resume:
                enableRunningUI();
                resumePosition();
                break;
            case R.id.stop:
                mPositionBinding.optionContent.locationModeSpinner.setEnabled(true);
                enableStartUI();
                stopPosition();
                break;
            default:
                break;
        }
    }

    /**
     * 开始定位，单次定位，第一次调用有效，其他状态中调用无效
     * 重新定位须stop后重新初始化在调用
     */
    private void startPosition() {
        showProgressDialog(R.string.get_current_location, R.string.please_wait);
        String mode = mPositionBinding.optionContent.locationModeSpinner.getSelectedItem().toString();

        //可以在初始化时传入appId 和 secret 或者与地图初始化一样在AndroidManifest.xml 中配置
        mMapxusPositioningClient = MapxusPositioningClient.getInstance(this); //定位初始化，获取实例（已在AndroidManifest.xml中配置appId与secret）
        // mMapxusPositioningClient = MapxusPositioningClient.getInstance(this, "your appId", "your secret"); //直接在初始时传入appId与secret
        mMapxusPositioningClient.setPositioningListener(mMapxusPositioningListener); //配置定位信息回调监听

        MapxusPositioningOption option = new MapxusPositioningOption();
        option.setMode(LocationMode.fromString(mode)); //配置定位模式。不配置默认为Normal

        mMapxusPositioningClient.setPositioningOption(option); //配置定位参数，不配置则会使用默认参数开始定位

        mMapxusPositioningClient.start(); //开始定位
        Log.d(TAG, "Call start()");
    }

    /**
     * 暂停定位，不销毁恢复定位不需重新初始化数据
     * 当且仅当定位服务正在运行中调用有效
     */
    private void pausePosition() {
        if (null != mMapxusPositioningClient) {
            removePositionMarker();
            mMapxusPositioningClient.pause();
            Log.d(TAG, "Call pause()");
        }
    }

    /**
     * 恢复定位
     * 当且仅当定位服务在暂停状态中调用有效
     */
    private void resumePosition() {
        if (null != mMapxusPositioningClient) {
            showProgressDialog(R.string.get_current_location, R.string.please_wait);
            mMapxusPositioningClient.resume();
            Log.d(TAG, "Call resume()");
        }
    }

    /**
     * 停止定位，销毁定位引擎需重新初始化
     */
    private void stopPosition() {
        if (null != mMapxusPositioningClient) {
            removePositionMarker();
            mMapxusPositioningClient.stop();
            Log.d(TAG, "Call stop()");
        }
    }

    /**
     * 定位状态、错误信息和定位结果返回监听
     */
    private MapxusPositioningListener mMapxusPositioningListener = new MapxusPositioningListener() {
        /**
         * 调用成功后状态返回对应
         * start() >>>>> RUNNING
         * pause() >>>>> PAUSED
         * resume()>>>>> RUNNING
         * stop()  >>>>> STOPPED
         * @param positioningState 状态
         */
        @Override
        public void onStateChange(PositioningState positioningState) {
            //定位过程中的各类状态返回
            switch (positioningState) {
                case WAITING: //等待数据加载中
                    Log.i(TAG, ">>>>> Waiting position....");
                    break;
                case RUNNING: //定位成功，定位服务运行中
                    dismissProgressDialog();
                    mPositionBinding.optionContent.setIsRunning(true);
                    Log.i(TAG, ">>>>> Start position");
                    break;
                case PAUSED: //定位服务暂停
                    mPositionBinding.optionContent.setIsRunning(false);
                    isShowInCenter = true;
                    mPositionLocation = null;
                    Log.i(TAG, ">>>>> Pause position");
                    break;
                case STOPPED:
                    mPositionBinding.optionContent.setIsRunning(false);
                    isShowInCenter = true;
                    mMapxusPositioningClient = null;
                    mPositionLocation = null;
                    mMapxusPositioningClient = null;
                    Log.i(TAG, ">>>>> Stop position");
                    break;
                default:
                    break;
            }
        }

        /**
         * WARNING: 为提示信息，出现此类信息仅为提示用户，不会销毁定位服务
         * ERROR_***: 错误信息，出现错误信息定位服务也会销魂，须重新开始定位
         * @param errorInfo 错误信息提示
         */

        @Override
        public void onError(ErrorInfo errorInfo) {
            //定位过程中错误信息返回
            Log.e(TAG, errorInfo.getErrorMessage());
            if (errorInfo.getErrorCode() == ErrorInfo.WARNING) {
                Toast.makeText(PositionActivity.this, errorInfo.getErrorMessage(), Toast.LENGTH_SHORT).show();
            } else {
                enableStartUI();
                dismissProgressDialog();
                mPositionBinding.optionContent.locationModeSpinner.setEnabled(true);
                removePositionMarker();
                if (!isFinishing()) {
                    new AlertDialog.Builder(PositionActivity.this).setTitle(R.string.error)
                            .setMessage(errorInfo.getErrorMessage()).setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //定位服务未开启或者未开启高精度模式时跳转到设置界面开启
                                    if (errorInfo.getErrorCode() == ErrorInfo.ERROR_LOCATION_SERVICE_DISABLED) {
                                        Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                        startActivity(locationIntent);
                                    }
                                }
                            }).create().show();
                }
            }
        }

        @Override
        public void onOrientationChange(float orientation, @SensorAccuracy int accuracy) {
            //手机朝向信息, 顺时针0-360
            mCurrentRotation = orientation; //保存最新的方向信息
            if (null != mMapboxMap) {
                updatePositionMarker(null);
            }

            switch (accuracy) {
                case SensorAccuracy.SENSOR_NO_CONTACT:
                case SensorAccuracy.SENSOR_UNRELIABLE:
                case SensorAccuracy.SENSOR_ACCURACY_LOW:
                case SensorAccuracy.SENSOR_ACCURACY_MEDIUM:
                    //可在低精度时添加提示用户校准的提示
                    break;
                case SensorAccuracy.SENSOR_ACCURACY_HIGH:
                    break;
                default:
                    break;
            }
        }

        /**
         *  定位位置信息返回
         *  室内：返回buildingId, floor, location
         *  室外：buildingId == null, floor == null, 返回location
         * @param positioningLocation
         */

        @Override
        public void onLocationChange(PositioningLocation positioningLocation) {
            //定位的位置返回
            LatLng latLng = new LatLng(positioningLocation.getLat(), positioningLocation.getLon());
            Log.d("Positioning result %s", new Gson().toJson(positioningLocation));
            if (null != mMapxusMap) {
                if (null != positioningLocation.getBuildingId() &&
                        (mPositionLocation == null || mPositionLocation.getBuildingId() == null
                                || !positioningLocation.getBuildingId().equals(mPositionLocation.getBuildingId()))) {//building change
                    Log.d("Building change to %s", positioningLocation.getBuildingId());
                    mMapxusMap.switchBuilding(positioningLocation.getBuildingId());
                    mPositionBinding.optionContent.setIsIndoor(true);
                    isShowInCenter = true;
                    queryBuildingInfo(positioningLocation.getBuildingId()); //query building detail info
                } else if (null == positioningLocation.getBuildingId() && mPositionBinding.optionContent.getIsIndoor()) { //change to outdoor
                    mPositionBinding.optionContent.setIsIndoor(false);
                    isShowInCenter = true;
                    Log.d(TAG, "Location change to outdoor");
                    Toast.makeText(PositionActivity.this, "Location change to outdoor", Toast.LENGTH_SHORT).show();
                }
                if (null != positioningLocation.getFloor() &&
                        (mPositionLocation == null || mPositionLocation.getFloor() == null ||
                                !positioningLocation.getFloor().getId().equals(mPositionLocation.getFloor().getId()))) {
                    mMapxusMap.switchFloor(positioningLocation.getFloor().getCode());
                    Log.d("Floor change to %s", positioningLocation.getFloor().getCode());
                }

                mPositionLocation = positioningLocation;

                //show outdoor or indoor marker
                if (!mPositionBinding.optionContent.getIsIndoor() ||
                        (mPositionBinding.optionContent.getIsIndoor() && mPositionLocation.getFloor().getCode().equals(mMapFloor))) {
                    updatePositionMarker(latLng);
                } else {
                    removePositionMarker();
                }

                if (isShowInCenter) {
                    isShowInCenter = false;
                    mMapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.5f)); //第一次显示将地图中心点移动到定位位置并放大
                }
            }
        }
    };

    /**
     * 修改maker的角度和经纬度
     *
     * @param latLng marker经纬度 latLng == null only update marker's rotation
     */
    private void updatePositionMarker(LatLng latLng) {
        if (null != latLng) {
            if (null != positionMarkerSource) {
                //更新经纬度
                positionMarkerSource.setGeoJson(Feature.fromGeometry(Point.fromLngLat(latLng.getLongitude(),
                        latLng.getLatitude())));
            } else {
                createPosisionMarker(latLng);
            }
        }
        //更新角度
        if (null != positionSymbolLayer) {
            positionSymbolLayer.setProperties(PropertyFactory.iconRotate(mCurrentRotation));
        }
    }

    private void createPosisionMarker(LatLng latLng) {
        removePositionMarker();
        //添加定位图标
        Bitmap positionImage = BitmapFactory.decodeResource(getResources(), R.drawable.arrow);
        mMapboxMap.getStyle().addImage(POSITION_MARKER_IMAGE, positionImage);
        //添加数据
        positionMarkerSource = new GeoJsonSource(POSITION_MARKER_SOURCE,
                Feature.fromGeometry(Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude())));
        mMapboxMap.getStyle().addSource(positionMarkerSource);
        //添加图标
        positionSymbolLayer = new SymbolLayer(POSITION_MARKER_LAYER, POSITION_MARKER_SOURCE)
                .withProperties(PropertyFactory.iconImage(POSITION_MARKER_IMAGE),
                        PropertyFactory.iconRotate(mCurrentRotation),
                        PropertyFactory.iconIgnorePlacement(true),
                        PropertyFactory.iconAllowOverlap(true));
        mMapboxMap.getStyle().addLayer(positionSymbolLayer);
    }

    /**
     * 移除定位marker
     */
    private void removePositionMarker() {
        mMapboxMap.getStyle().removeSource(POSITION_MARKER_SOURCE);
        mMapboxMap.getStyle().removeLayer(POSITION_MARKER_LAYER);
        positionSymbolLayer = null;
        positionMarkerSource = null;
    }

    /**
     * Method is used to interpolate the marker animation.
     */
    private static class LatLngEvaluator implements TypeEvaluator<LatLng> {

        private LatLng latLng = new LatLng();

        @Override
        public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
            latLng.setLatitude(startValue.getLatitude()
                    + ((endValue.getLatitude() - startValue.getLatitude()) * fraction));
            latLng.setLongitude(startValue.getLongitude()
                    + ((endValue.getLongitude() - startValue.getLongitude()) * fraction));
            return latLng;
        }
    }

    /**
     * 通过 buildingId 查询建筑的详细信息
     * Query indoorBuildingInfo to get all floors of positioning building
     *
     * @param buildingId
     */
    private void queryBuildingInfo(String buildingId) {
        mPositionBuildingInfo = null;
        mPositionViewModel.searchBuildingById(buildingId, new BuildingSearch.BuildingSearchResultListener() {
            @Override
            public void onGetBuildingResult(BuildingResult buildingResult) {
            }

            @Override
            public void onGetBuildingDetailResult(BuildingDetailResult buildingDetailResult) {
                if (buildingDetailResult.status != 0) {
                    String msg = "Get building info failed " + buildingDetailResult.error.toString();
                    Toast.makeText(PositionActivity.this, msg, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, msg);
                    return;
                }
                if (buildingDetailResult.getIndoorBuildingInfo() == null) {
                    String msg = "Get building info failed: Building no found.";
                    Toast.makeText(PositionActivity.this, msg, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, msg);
                    return;
                }
                mPositionBuildingInfo = buildingDetailResult.getIndoorBuildingInfo();
                Log.d("Get building info of %s", mPositionBuildingInfo.getBuildingId());
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_position, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.change_floor:
                //Change positioning floor during positioning
                if (null != mPositionLocation && null != mPositionBuildingInfo) {
                    final String[] floors = getFloors(mPositionBuildingInfo);
                    Builder builder = new Builder(PositionActivity.this);
                    builder.setTitle(getString(R.string.floors));
                    builder.setItems(floors, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (mPositionBuildingInfo != null) {
                                PositioningFloor positioningFloor = new PositioningFloor
                                        (mPositionBuildingInfo.getFloors()[i].getId(), floors[i]);
                                mMapxusPositioningClient.changeFloor(positioningFloor);
                                mMapxusMap.switchFloor(floors[i]);
                                Log.d(TAG, "Change position floor to " + floors[i]);
                            }
                        }
                    }).create().show();
                } else {
                    new Builder(PositionActivity.this).setTitle(R.string.note)
                            .setMessage(R.string.start_position_first)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).create().show();
                }
                break;
            case R.id.change_mode:
                //todo 定位过程中修改定位模式，此功能还未能使用
                break;
            case R.id.enable_background:
                //开启后台模式，防止在后台运行时定位服务被关闭
                if (null == mMapxusPositioningClient) {
                    mMapxusPositioningClient = MapxusPositioningClient.getInstance(PositionActivity.this);
                }
                if (item.getTitle().toString().equals(getString(R.string.enable_background))) {
                    item.setTitle(R.string.disable_background);
                    mMapxusPositioningClient.enableBackgroundPositioning(1, buildNotification());
                } else {
                    item.setTitle(R.string.enable_background);
                    mMapxusPositioningClient.disableBackgroundPositioning(true);
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private String[] getFloors(IndoorBuildingInfo indoorBuildingInfo) {
        String[] floors = new String[indoorBuildingInfo.getFloors().length];
        for (int i = 0; i < indoorBuildingInfo.getFloors().length; i++) {
            floors[i] = indoorBuildingInfo.getFloors()[i].getCode();
        }
        return floors;
    }

    /**
     * 未开始定位前时可点击的按钮变换
     */
    private void enableStartUI() {
        mPositionBinding.optionContent.start.setEnabled(true);
        mPositionBinding.optionContent.pause.setEnabled(false);
        mPositionBinding.optionContent.resume.setEnabled(false);
        mPositionBinding.optionContent.stop.setEnabled(false);
    }

    /**
     * pause positioning 后可点击的按钮变换
     */
    private void enablePauseUI() {
        mPositionBinding.optionContent.start.setEnabled(false);
        mPositionBinding.optionContent.pause.setEnabled(false);
        mPositionBinding.optionContent.resume.setEnabled(true);
        mPositionBinding.optionContent.stop.setEnabled(true);
    }

    /**
     * 运行中可点击的按钮
     */
    private void enableRunningUI() {
        mPositionBinding.optionContent.start.setEnabled(false);
        mPositionBinding.optionContent.pause.setEnabled(true);
        mPositionBinding.optionContent.resume.setEnabled(false);
        mPositionBinding.optionContent.stop.setEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        //退出时销毁定位
        if (null != mMapxusPositioningClient) {
            mMapxusPositioningClient.stop();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private static final String CHANNEL_NAME = "Background Positioner";

    private Notification buildNotification() {
        Notification notification;
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            //Android O上对Notification进行了修改，如果设置的targetSDKVersion>=26建议使用此种方式创建通知栏
            if (null == mNotificationManager) {
                mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            }
            NotificationChannel notificationChannel = new NotificationChannel(getPackageName(), CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.enableLights(true); //是否在桌面icon右上角展示小圆点
            notificationChannel.setLightColor(Color.GREEN);//小圆点颜色
            notificationChannel.setShowBadge(true); //是否在久按桌面图标时显示此渠道的通知
            notificationChannel.enableVibration(true); //显示通知时是否震动
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);//锁屏时通知是否显示
            mNotificationManager.createNotificationChannel(notificationChannel);
            builder = new Notification.Builder(getApplicationContext(), getPackageName());
        } else {
            builder = new Notification.Builder(getApplicationContext());
        }
        notification = builder.setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis()) // 设置该通知发生的时间
                .build();
        return notification;
    }


    public void showProgressDialog(int titleResId, int msgResId) {
        dismissProgressDialog();
        if (!isFinishing()) {
            mProgressDialog = new ProgressDialog(PositionActivity.this);
            mProgressDialog.setTitle(getString(titleResId));
            mProgressDialog.setMessage(getString(msgResId));
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
        }
    }

    public void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}
