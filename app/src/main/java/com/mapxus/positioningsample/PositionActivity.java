package com.mapxus.positioningsample;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.mapxus.beemap.BeeMap;
import com.mapxus.beemap.BeeMapView;
import com.mapxus.beemap.CameraUpdateFactory;
import com.mapxus.beemap.interfaces.OnMapReadyCallback;
import com.mapxus.beemap.model.CameraPosition;
import com.mapxus.beemap.model.IndoorBuilding;
import com.mapxus.beemap.model.LatLng;
import com.mapxus.beemap.model.Style;
import com.mapxus.beemap.model.overlay.BitmapDescriptor;
import com.mapxus.beemap.model.overlay.BitmapDescriptorFactory;
import com.mapxus.beemap.model.overlay.Marker;
import com.mapxus.beemap.model.overlay.MarkerOptions;
import com.mapxus.positioning.config.MapxusPositioningOption;
import com.mapxus.positioning.model.info.ErrorInfo;
import com.mapxus.positioning.model.info.LocationMode;
import com.mapxus.positioning.model.info.PositioningState;
import com.mapxus.positioning.model.location.PositioningBuilding;
import com.mapxus.positioning.model.location.PositioningFloor;
import com.mapxus.positioning.model.location.PositioningLocation;
import com.mapxus.positioning.positioning.api.MapxusPositioningClient;
import com.mapxus.positioning.positioning.api.MapxusPositioningListener;

import com.mapxus.positioningsample.databinding.ActivityPositionBinding;

public class PositionActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "PositionActivity";
    private ActivityPositionBinding mPositionBinding;
    private BeeMap mBeeMap;
    private BeeMapView mBeeMapView;

    private MapxusPositioningClient mMapxusPositioningClient;

    private Marker mPositionMarker; //定位图标
    private float mCurrentRotation; //定位角度
    private PositioningBuilding mPositionBuilding; //定位建筑
    private PositioningFloor mPositionFloor; //定位楼层
    private PositioningLocation mPositionLocation; //定位位置
    private String mMapFloor;
    private boolean isFirstShow = true;

    private NotificationManager mNotificationManager;

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_position);
        mPositionBinding = DataBindingUtil.setContentView(this, R.layout.activity_position);

        initMap(savedInstanceState);
        initView();
    }

    /**
     * 初始化地图
     *
     * @param savedInstanceState
     */
    private void initMap(Bundle savedInstanceState) {
        mBeeMapView = mPositionBinding.map;
        mBeeMapView.onCreate(savedInstanceState);
        mBeeMapView.setStyle(Style.MAPPYBEE);
        mBeeMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(BeeMap beeMap) {
                mBeeMap = beeMap;
                mBeeMap.addOnCameraChangeListener(new BeeMap.OnCameraChangeListener() {
                    @Override
                    public void onCameraChange(CameraPosition cameraPosition) {
                        //每次地图旋转图标角度须跟着一起旋转
                        if (null != mPositionMarker) {
                            mPositionMarker.setRotateAngle(mCurrentRotation -
                                    mBeeMap.getCameraPosition().bearing);
                        }
                    }
                });
                //楼层变化时marker的显示
                mBeeMap.addOnFloorChangeListener(new BeeMap.OnFloorChangeListener() {
                    @Override
                    public void onFloorChange(IndoorBuilding indoorBuilding, String floor) {
                        boolean isFirst = (null == mMapFloor); //室内地图首次显示，切换建筑和楼层，未显示前切换建筑和楼层是无效的
                        mMapFloor = floor;
                        if (isFirst) {
                            if (null != mPositionBuilding) {
                                mBeeMap.switchBuilding(mPositionBuilding.getId());
                            }
                            if (null != mPositionFloor) {
                                mBeeMap.switchFloor(mPositionFloor.getCode());
                            }
                        }
                        if (null != mPositionLocation && null != mPositionFloor) {
                            if (floor.equals(mPositionFloor.getCode())) {
                                updatePositionMarker(new LatLng(mPositionLocation.getLat(),
                                        mPositionLocation.getLon())); //楼层切到定位位置时显示marker
                            } else {
                                removePositionMarker(); //楼层切换到非定位楼层移除marker
                            }
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
        }
    }

    /**
     * 开始定位，单次定位，第一次调用有效，其他状态中调用无效
     * 重新定位须stop后重新初始化在调用
     */
    private void startPosition() {
        showProgressDialog(R.string.get_current_location, R.string.please_wait);
        String mode = mPositionBinding.optionContent.locationModeSpinner.getSelectedItem().toString();

        mMapxusPositioningClient = MapxusPositioningClient.getInstance(this, "977b5b54", "F6d7666@"); //定位初始化，获取实例
        mMapxusPositioningClient.setPositioningListener(indoorPositionerListener); //配置定位信息回调监听

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
    private MapxusPositioningListener indoorPositionerListener = new MapxusPositioningListener() {
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
                    Log.i(TAG, ">>>>> Start position");
                    break;
                case PAUSED: //定位服务暂停
                    isFirstShow = true;
                    mPositionLocation = null;
                    mPositionFloor = null;
                    mPositionBuilding = null;
                    Log.i(TAG, ">>>>> Pause position");
                    break;
                case STOPPED:
                    isFirstShow = true;
                    mMapxusPositioningClient = null;
                    mPositionLocation = null;
                    mPositionFloor = null;
                    mPositionBuilding = null;
                    mMapxusPositioningClient = null;
                    Log.i(TAG, ">>>>> Stop position");
                    break;
            }
        }

        @Override
        public void onError(ErrorInfo errorInfo) {
            //定位过程中错误信息返回
            Log.e(TAG, errorInfo.getErrorMessage());
            if (errorInfo.getErrorCode() == ErrorInfo.ERROR_WARNING) {
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
                                }
                            }).create().show();
                }
            }
        }

        @Override
        public void onOrientationChange(float v) {
            //手机朝向信息, 顺时针0-360
            mCurrentRotation = v; //保存最新的方向信息
            if (null != mBeeMap) {
                updatePositionMarker(null);
            }
        }

        @Override
        public void onLocationChange(PositioningLocation beeTrackLocation) {
            dismissProgressDialog();
            //定位的位置返回
            mPositionLocation = beeTrackLocation;
            LatLng latLng = new LatLng(beeTrackLocation.getLat(), beeTrackLocation.getLon());
            if (isFirstShow && mBeeMap != null) {
                isFirstShow = false;
                mBeeMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f)); //第一次显示将地图中心点移动到定位位置并放大
            }
            if (null != mPositionFloor && mPositionFloor.getCode().equals(mMapFloor)) {//当前楼层在定位楼层时显示marker
                updatePositionMarker(latLng);
            } else {
                removePositionMarker();
            }
        }

        @Override
        public void onBuildingChange(PositioningBuilding beeTrackBuilding) {
            //定位的建筑变化
            isFirstShow = true; //切换建筑也须移动放大地图
            mPositionBuilding = beeTrackBuilding;
            if (null != mBeeMap) {
                mBeeMap.switchBuilding(beeTrackBuilding.getId());//切换地图建筑
            }
        }

        @Override
        public void onFloorChange(PositioningFloor beeTrackFloor) {
            //定位的楼层
            mPositionFloor = beeTrackFloor;
            if (null != mBeeMap) {
                mBeeMap.switchFloor(beeTrackFloor.getCode()); //切换地图显示楼层到定位楼层
            }
        }
    };

    /**
     * 修改maker的角度和经纬度
     *
     * @param latLng marker经纬度
     */
    private void updatePositionMarker(LatLng latLng) {
        if (null != latLng) { //改变marker经纬度
            if (null == mPositionMarker) { //第一次创建marker
                BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.arrow);
                mPositionMarker = mBeeMap.addMarker(new MarkerOptions()
                        .icon(icon).position(latLng).anchor(0.5f, 0.5f));
                mBeeMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latLng.latitude + 0.000001, latLng.longitude + 0.0000001)));
            } else {
                mPositionMarker.setPosition(latLng);
            }
        }
        if (null != mPositionMarker) { //只改变角度，角度的变化会很频繁
            mPositionMarker.setRotateAngle(mCurrentRotation - mBeeMap.getCameraPosition().bearing);
        }
    }

    /**
     * 移除定位marker
     */
    private void removePositionMarker() {
        if (null != mPositionMarker) {
            mPositionMarker.remove();
            mPositionMarker = null;
        }
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
                //定位过程中改变定位楼层
                if (null != mPositionBuilding) {
                    final String[] floors = getFloors(mPositionBuilding);
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(PositionActivity.this);
                    builder.setTitle(getString(R.string.floors));
                    builder.setItems(floors, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mMapxusPositioningClient.changeFloor(mPositionBuilding.getFloors().get(i));
                            mBeeMap.switchFloor(floors[i]);
                            Log.d(TAG, "Change floor to " + floors[i]);
                        }
                    }).create().show();
                } else {
                    new android.app.AlertDialog.Builder(PositionActivity.this).setTitle(R.string.note).setMessage(R.string.start_position_first)
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
        }
        return super.onOptionsItemSelected(item);
    }

    private String[] getFloors(PositioningBuilding positioningBuilding) {
        String[] floors = new String[positioningBuilding.getFloors().size()];
        for (int i = 0; i < positioningBuilding.getFloors().size(); i++) {
            floors[i] = positioningBuilding.getFloors().get(i).getCode();
        }
        return floors;
    }

    private void enableStartUI() {
        mPositionBinding.optionContent.start.setEnabled(true);
        mPositionBinding.optionContent.pause.setEnabled(false);
        mPositionBinding.optionContent.resume.setEnabled(false);
        mPositionBinding.optionContent.stop.setEnabled(false);
    }

    private void enablePauseUI() {
        mPositionBinding.optionContent.start.setEnabled(false);
        mPositionBinding.optionContent.pause.setEnabled(false);
        mPositionBinding.optionContent.resume.setEnabled(true);
        mPositionBinding.optionContent.stop.setEnabled(true);
    }

    private void enableRunningUI() {
        mPositionBinding.optionContent.start.setEnabled(false);
        mPositionBinding.optionContent.pause.setEnabled(true);
        mPositionBinding.optionContent.resume.setEnabled(false);
        mPositionBinding.optionContent.stop.setEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mBeeMapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBeeMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBeeMapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mBeeMapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBeeMapView.onDestroy();
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
