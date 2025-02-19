package com.mapxus.positioningsample;


import android.annotation.SuppressLint;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;

import com.google.gson.Gson;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapxus.map.mapxusmap.api.map.MapViewProvider;
import com.mapxus.map.mapxusmap.api.map.MapxusMap;
import com.mapxus.map.mapxusmap.api.map.MapxusMapZoomMode;
import com.mapxus.map.mapxusmap.api.services.BuildingSearch;
import com.mapxus.map.mapxusmap.api.services.model.building.BuildingDetailResult;
import com.mapxus.map.mapxusmap.api.services.model.building.FloorInfo;
import com.mapxus.map.mapxusmap.api.services.model.building.IndoorBuildingInfo;
import com.mapxus.map.mapxusmap.impl.MapboxMapViewProvider;
import com.mapxus.positioning.positioning.api.ErrorInfo;
import com.mapxus.positioning.positioning.api.FloorType;
import com.mapxus.positioning.positioning.api.MapxusFloor;
import com.mapxus.positioning.positioning.api.MapxusLocation;
import com.mapxus.positioning.positioning.api.MapxusPositioningClient;
import com.mapxus.positioning.positioning.api.MapxusPositioningListener;
import com.mapxus.positioning.positioning.api.MapxusPositioningOption;
import com.mapxus.positioning.positioning.api.PositioningMode;
import com.mapxus.positioning.positioning.api.PositioningState;
import com.mapxus.positioning.positioning.api.SensorAccuracy;
import com.mapxus.positioningsample.databinding.ActivityPositionBinding;


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

    private MapxusPositioningClient mMapxusPositioningClient;//定位服务客户端

    private float mCurrentRotation; //定位角度
    private MapxusLocation mapxusLocation; //定位位置(包括floor、，buildingId, buildingId为空则定位到室外)

    private String mMapFloor; //当前地图的显示楼层
    private IndoorBuildingInfo mPositionBuildingInfo; //定位建筑详细信息
    private boolean isShowInCenter = true; //第一次显示切换地图中心点位置

    private PositionViewModel mPositionViewModel;


    private ProgressDialog mProgressDialog;
    /**
     * 定位状态、错误信息和定位结果返回监听
     */
    private final MapxusPositioningListener mMapxusPositioningListener = new MapxusPositioningListener() {
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
                case RUNNING: //定位成功，定位服务运行中
                    dismissProgressDialog();
                    mPositionBinding.optionContent.setIsRunning(true);
                    Log.i(TAG, ">>>>> Start position");
                    break;
                case PAUSED: //定位服务暂停
                    mPositionBinding.optionContent.setIsRunning(false);
                    isShowInCenter = true;
                    mapxusLocation = null;
                    Log.i(TAG, ">>>>> Pause position");
                    break;
                case STOPPED:
                    mPositionBinding.optionContent.setIsRunning(false);
                    isShowInCenter = true;
                    mapxusLocation = null;
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
                                    (dialogInterface, i) -> {
                                        //定位服务未开启或者未开启高精度模式时跳转到设置界面开启
                                        if (errorInfo.getErrorCode() == ErrorInfo.ERROR_LOCATION_SERVICE_DISABLED) {
                                            Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            startActivity(locationIntent);
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
                updatePositioningMarkerDirection(orientation);
            }

            switch (accuracy) {
                case SensorAccuracy.SENSOR_NO_CONTACT:
                case SensorAccuracy.SENSOR_UNRELIABLE:
                case SensorAccuracy.SENSOR_ACCURACY_LOW:
                case SensorAccuracy.SENSOR_ACCURACY_MEDIUM:
                case SensorAccuracy.SENSOR_ACCURACY_HIGH:
                    //可在低精度时添加提示用户校准的提示
                    break;
                default:
                    break;
            }
        }

        /**
         *  定位位置信息返回
         *  室内：返回buildingId, floor, location
         *  室外：buildingId == null, floor == null, 返回location
         * @param mapxusLocation location
         */

        @Override
        public void onLocationChange(MapxusLocation mapxusLocation) {
            //定位的位置返回
            if (mapxusLocation.getVenueId() == null) {
                mPositionBinding.optionContent.locationDetail.setText(
                        "MapxusLocation(\n latitude=" + mapxusLocation.getLatitude() + ", longitude=" + mapxusLocation.getLongitude() +
                                "\n accuracy=" + mapxusLocation.getAccuracy() + ", time=" + mapxusLocation.getTime() + "\n)"
                );
            } else {
                mPositionBinding.optionContent.locationDetail.setText(
                        "MapxusLocation(\n venueId=" + mapxusLocation.getVenueId() +
                                "\n buildingId=" + mapxusLocation.getBuildingId() +
                                "\n MapxusFloor(id = " + mapxusLocation.getMapxusFloor().getCode() + ", ordinal = " + mapxusLocation.getMapxusFloor().getOrdinal() + ", code = " + mapxusLocation.getMapxusFloor().getCode() + ")" +
                                "\n latitude=" + mapxusLocation.getLatitude() + ", longitude=" + mapxusLocation.getLongitude() +
                                "\n accuracy=" + mapxusLocation.getAccuracy() + ", time=" + mapxusLocation.getTime() + "\n)"
                );
            }

            LatLng latLng = new LatLng(mapxusLocation.getLatitude(), mapxusLocation.getLongitude());
            Log.d("Positioning result %s", new Gson().toJson(mapxusLocation));
            if (null != mMapxusMap) {
                if (null != mapxusLocation.getVenueId() &&
                        (PositionActivity.this.mapxusLocation == null || PositionActivity.this.mapxusLocation.getVenueId() == null
                                || !mapxusLocation.getVenueId().equals(PositionActivity.this.mapxusLocation.getVenueId()))) {//building change
                    Log.d("Venue change to %s", mapxusLocation.getVenueId());
                    Toast.makeText(PositionActivity.this, String.format("Venue change to %s , building id %s", mapxusLocation.getVenueId(), mapxusLocation.getBuildingId()), Toast.LENGTH_SHORT).show();
                    mMapxusMap.selectFloorById(mapxusLocation.getMapxusFloor().getId(), MapxusMapZoomMode.ZoomDisable, null);
                    mPositionBinding.optionContent.setIsIndoor(true);
                    isShowInCenter = true;
                    if (mapxusLocation.getBuildingId() != null) {
                        queryBuildingInfo(mapxusLocation.getBuildingId()); //query building detail info
                    }
                } else if (null == mapxusLocation.getVenueId() && mPositionBinding.optionContent.getIsIndoor()) { //change to outdoor
                    mPositionBinding.optionContent.setIsIndoor(false);
                    isShowInCenter = true;
                    Log.d(TAG, "Location change to outdoor");
                    Toast.makeText(PositionActivity.this, "Location change to outdoor", Toast.LENGTH_SHORT).show();
                }
                if (null != mapxusLocation.getMapxusFloor() &&
                        (PositionActivity.this.mapxusLocation == null || PositionActivity.this.mapxusLocation.getMapxusFloor() == null ||
                                !mapxusLocation.getMapxusFloor().getId().equals(PositionActivity.this.mapxusLocation.getMapxusFloor().getId()))) {
                    mMapxusMap.selectFloorById(mapxusLocation.getMapxusFloor().getId(), MapxusMapZoomMode.ZoomDisable, null);
                    Log.d("Floor change to %s", mapxusLocation.getMapxusFloor().getCode());
                }

                PositionActivity.this.mapxusLocation = mapxusLocation;

                //show outdoor or indoor marker
                if (mPositionBinding.optionContent.getIsIndoor() && PositionActivity.this.mapxusLocation.getMapxusFloor().getCode().equals(mMapFloor)) {
                    updatePositionMarker(latLng);
                } else if (!mPositionBinding.optionContent.getIsIndoor()) {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_position);
        mPositionBinding = DataBindingUtil.setContentView(this, R.layout.activity_position);
        mPositionViewModel = new PositionViewModel();
        initMap(savedInstanceState);
        //可以在初始化时传入appId 和 secret 或者与地图初始化一样在AndroidManifest.xml 中配置
        mMapxusPositioningClient = MapxusPositioningClient.getInstance(this, getApplicationContext()); //定位初始化，获取实例（已在AndroidManifest.xml中配置appId与secret）
        initView();
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
        mPositionBinding.optionContent.setIsIndoor(false);
        mPositionBinding.optionContent.setIsRunning(false);
        mPositionBinding.optionContent.version.setText("version: " + BuildConfig.VERSION_NAME);
        enableStartUI();
    }

    @SuppressLint("NonConstantResourceId")
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
                mPositionBinding.optionContent.setIsIndoor(false);
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
        mMapxusPositioningClient.addPositioningListener(mMapxusPositioningListener); //配置定位信息回调监听

        MapxusPositioningOption option = new MapxusPositioningOption();
        option.setPositioningMode(PositioningMode.fromString(mode)); //配置定位模式。不配置默认为Normal

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
     * 初始化地图
     * 添加地图楼层变换监听
     *
     * @param savedInstanceState savedInstanceState
     */
    private void initMap(Bundle savedInstanceState) {
        mMapView = mPositionBinding.map;
        MapViewProvider mMapViewProvider = new MapboxMapViewProvider(this, mMapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(mapboxMap -> {
            mMapboxMap = mapboxMap; //获取地图准备好后的MapboxMap实例
        });
        mMapViewProvider.getMapxusMapAsync(mapxusMap -> {
            mMapxusMap = mapxusMap; //获取室内地图准备好后的MapxusMap实例
            //楼层变化时marker的显示
            mMapxusMap.addOnFloorChangedListener((venue, indoorBuilding, floorInfo) -> {
                mMapFloor = floorInfo == null ? null : floorInfo.getCode();
                if (null != mapxusLocation && floorInfo != null) {
                    //楼层不同则不显示定位图标
                    if (mPositionBinding.optionContent.getIsIndoor()) {
                        if (null != mapxusLocation.getMapxusFloor() &&
                                mMapFloor.equals(mapxusLocation.getMapxusFloor().getCode())) {
                            updatePositionMarker(new LatLng(mapxusLocation.getLatitude(),
                                    mapxusLocation.getLongitude()));
                        } else {
                            Log.d(TAG, "Floor change. Map floor is different from positioning floor, remove marker");
                            removePositionMarker();
                        }
                    }
                }
            });
            //地图室内外切换时, 地图楼层重置为空
            mMapxusMap.addOnBuildingChangeListener(indoorBuilding -> {
                if (null == indoorBuilding) {
                    mMapFloor = null;
                    Log.d(TAG, "Map change to outdoor.");
                }
            });
        });
    }


    private void updatePositioningMarkerDirection(Float rotation) {
        //更新角度
        if (positionSymbolLayer != null) {
            float finalRotation = (float) (rotation + (360 - mMapboxMap.getCameraPosition().bearing));
            positionSymbolLayer.setProperties(PropertyFactory.iconRotate(finalRotation));
        }
    }

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
                createPositionMarker(latLng);
            }
        }
    }

    private void createPositionMarker(LatLng latLng) {
        Style style = mMapboxMap.getStyle();
        if (style != null) {
            removePositionMarker();
            //添加定位图标
            Bitmap positionImage = BitmapFactory.decodeResource(getResources(), R.drawable.arrow);
            style.addImage(POSITION_MARKER_IMAGE, positionImage);
            //添加数据
            positionMarkerSource = new GeoJsonSource(POSITION_MARKER_SOURCE,
                    Feature.fromGeometry(Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude())));
            style.addSource(positionMarkerSource);
            //添加图标
            positionSymbolLayer = new SymbolLayer(POSITION_MARKER_LAYER, POSITION_MARKER_SOURCE)
                    .withProperties(PropertyFactory.iconImage(POSITION_MARKER_IMAGE),
                            PropertyFactory.iconRotate(mCurrentRotation),
                            PropertyFactory.iconIgnorePlacement(true),
                            PropertyFactory.iconAllowOverlap(true));
            style.addLayer(positionSymbolLayer);
        }
    }

    /**
     * 移除定位marker
     */
    private void removePositionMarker() {
        if (mMapboxMap.getStyle() != null) {
            mMapboxMap.getStyle().removeSource(POSITION_MARKER_SOURCE);
            mMapboxMap.getStyle().removeLayer(POSITION_MARKER_LAYER);
        }
        positionSymbolLayer = null;
        positionMarkerSource = null;
    }

    /**
     * 通过 buildingId 查询建筑的详细信息
     * Query indoorBuildingInfo to get all floors of positioning building
     *
     * @param buildingId id
     */
    private void queryBuildingInfo(String buildingId) {
        mPositionBuildingInfo = null;
        mPositionViewModel.searchBuildingById(buildingId, new BuildingSearch.BuildingSearchResultListenerAdapter() {
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

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.change_floor:
                //Change positioning floor during positioning
                if (null != mapxusLocation && null != mPositionBuildingInfo) {
                    final String[] floors = getFloors(mPositionBuildingInfo);
                    Builder builder = new Builder(PositionActivity.this);
                    builder.setTitle(getString(R.string.floors));
                    builder.setItems(floors, (dialogInterface, i) -> {
                        if (mPositionBuildingInfo != null) {
                            FloorInfo floorInfo = mPositionBuildingInfo.getFloors().get(i);
                            MapxusFloor floor = new MapxusFloor
                                    (floorInfo.getId(), floorInfo.getOrdinal(), floorInfo.getCode(), FloorType.FLOOR);
                            mMapxusPositioningClient.changeFloor(floor);
                            mMapxusMap.selectFloorById(floorInfo.getId(), MapxusMapZoomMode.ZoomDisable, null);
                            Log.d(TAG, "Change position floor to " + floors[i]);
                        }
                    }).create().show();
                } else {
                    new Builder(PositionActivity.this).setTitle(R.string.note)
                            .setMessage(R.string.start_position_first)
                            .setPositiveButton(R.string.ok, (dialog, which) -> {
                            }).create().show();
                }
                break;
            case R.id.change_mode:
                //todo 定位过程中修改定位模式，此功能还未能使用
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private String[] getFloors(IndoorBuildingInfo indoorBuildingInfo) {
        String[] floors = new String[indoorBuildingInfo.getFloors().size()];
        for (int i = 0; i < indoorBuildingInfo.getFloors().size(); i++) {
            floors[i] = indoorBuildingInfo.getFloors().get(i).getCode();
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
