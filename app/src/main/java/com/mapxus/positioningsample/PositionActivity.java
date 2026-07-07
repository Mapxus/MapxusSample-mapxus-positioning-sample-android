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
import com.mapxus.map.mapxusmap.api.map.MapViewProvider;
import com.mapxus.map.mapxusmap.api.map.MapxusMap;
import com.mapxus.map.mapxusmap.api.map.MapxusMapZoomMode;
import com.mapxus.map.mapxusmap.api.services.BuildingSearch;
import com.mapxus.map.mapxusmap.api.services.model.building.BuildingDetailResult;
import com.mapxus.map.mapxusmap.api.services.model.building.FloorInfo;
import com.mapxus.map.mapxusmap.api.services.model.building.IndoorBuildingInfo;
import com.mapxus.map.mapxusmap.impl.MapLibreMapViewProvider;
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

import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.layers.PropertyFactory;
import org.maplibre.android.style.layers.SymbolLayer;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.Point;


public class PositionActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "PositionActivity";
    private ActivityPositionBinding mPositionBinding;

    /**
     * Parameters for adding the map marker icon
     */
    private static final String POSITION_MARKER_SOURCE = "position_marker_source";
    private static final String POSITION_MARKER_LAYER = "position_marker_layer";
    private static final String POSITION_MARKER_IMAGE = "position_marker_image";

    /**
     * Data for adding the map marker icon
     */
    private GeoJsonSource positionMarkerSource;
    private SymbolLayer positionSymbolLayer;

    private MapLibreMap mapLibreMap;
    private MapView mMapView;

    private MapxusMap mMapxusMap;

    private MapxusPositioningClient mMapxusPositioningClient;//Positioning service client

    private float mCurrentRotation; //Positioning orientation angle
    private MapxusLocation mapxusLocation; //Positioning location (including floor and buildingId; if buildingId is empty, the location is outdoors)

    private String mMapFloor; //The floor currently displayed on the map
    private IndoorBuildingInfo mPositionBuildingInfo; //Detailed information of the positioning building
    private boolean isShowInCenter = true; //Whether to move the map center to the location on first display

    private PositionViewModel mPositionViewModel;


    private ProgressDialog mProgressDialog;
    /**
     * Listener for positioning state, error information and positioning result
     */
    private final MapxusPositioningListener mMapxusPositioningListener = new MapxusPositioningListener() {
        /**
         * After a successful call, the state changes accordingly
         * start() >>>>> RUNNING
         * pause() >>>>> PAUSED
         * resume()>>>>> RUNNING
         * stop()  >>>>> STOPPED
         * @param positioningState state
         */
        @Override
        public void onStateChange(PositioningState positioningState) {
            //Various states returned during positioning
            switch (positioningState) {
                case RUNNING: //Positioning succeeded, positioning service running
                    dismissProgressDialog();
                    mPositionBinding.optionContent.setIsRunning(true);
                    Log.i(TAG, ">>>>> Start position");
                    break;
                case PAUSED: //Positioning service paused
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
         * WARNING: informational message. Such messages only notify the user and will not destroy the positioning service
         * ERROR_***: error message. When an error message occurs, the positioning service is also destroyed and positioning must be restarted
         * @param errorInfo error information
         */

        @Override
        public void onError(ErrorInfo errorInfo) {
            //Error information returned during positioning
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
                                        //When the location service is disabled or high-accuracy mode is off, jump to settings to enable it
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
            //Phone orientation information, clockwise 0-360
            mCurrentRotation = orientation; //Save the latest orientation information
            if (null != mapLibreMap) {
                updatePositioningMarkerDirection(orientation);
            }

            switch (accuracy) {
                case SensorAccuracy.SENSOR_NO_CONTACT:
                case SensorAccuracy.SENSOR_UNRELIABLE:
                case SensorAccuracy.SENSOR_ACCURACY_LOW:
                case SensorAccuracy.SENSOR_ACCURACY_MEDIUM:
                case SensorAccuracy.SENSOR_ACCURACY_HIGH:
                    //A calibration prompt can be shown to the user when accuracy is low
                    break;
                default:
                    break;
            }
        }

        /**
         *  Positioning location information returned
         *  Indoor: returns buildingId, floor, location
         *  Outdoor: buildingId == null, floor == null, returns location
         * @param mapxusLocation location
         */

        @Override
        public void onLocationChange(MapxusLocation mapxusLocation) {
            //The positioning location returned
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
                    mapLibreMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.5f)); //On first display, move the map center to the location and zoom in
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
        //You can pass appId and secret during initialization, or configure them in AndroidManifest.xml like the map initialization
        mMapxusPositioningClient = MapxusPositioningClient.getInstance(this, getApplicationContext()); //Positioning initialization, get instance (appId and secret already configured in AndroidManifest.xml)
        initView();
    }

    /**
     * Set control listeners
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
     * Start positioning. Single positioning; only the first call is effective, calls in other states are ignored.
     * To reposition, you must stop and re-initialize before calling again.
     */
    private void startPosition() {
        showProgressDialog(R.string.get_current_location, R.string.please_wait);
        String mode = mPositionBinding.optionContent.locationModeSpinner.getSelectedItem().toString();
        mMapxusPositioningClient.addPositioningListener(mMapxusPositioningListener); //Configure the positioning callback listener

        MapxusPositioningOption option = new MapxusPositioningOption();
        option.setPositioningMode(PositioningMode.fromString(mode)); //Configure the positioning mode. Defaults to Normal if not configured

        mMapxusPositioningClient.setPositioningOption(option); //Configure positioning parameters; if not configured, default parameters are used to start positioning

        mMapxusPositioningClient.start(); //Start positioning
        Log.d(TAG, "Call start()");
    }

    /**
     * Pause positioning without destroying it. Resuming does not require re-initializing data.
     * Only effective when the positioning service is running.
     */
    private void pausePosition() {
        if (null != mMapxusPositioningClient) {
            removePositionMarker();
            mMapxusPositioningClient.pause();
            Log.d(TAG, "Call pause()");
        }
    }

    /**
     * Resume positioning.
     * Only effective when the positioning service is in the paused state.
     */
    private void resumePosition() {
        if (null != mMapxusPositioningClient) {
            showProgressDialog(R.string.get_current_location, R.string.please_wait);
            mMapxusPositioningClient.resume();
            Log.d(TAG, "Call resume()");
        }
    }

    /**
     * Stop positioning. The positioning engine is destroyed and must be re-initialized.
     */
    private void stopPosition() {
        if (null != mMapxusPositioningClient) {
            removePositionMarker();
            mMapxusPositioningClient.stop();
            Log.d(TAG, "Call stop()");
        }
    }

    /**
     * Initialize the map
     * Add a map floor change listener
     *
     * @param savedInstanceState savedInstanceState
     */
    private void initMap(Bundle savedInstanceState) {
        mMapView = mPositionBinding.map;
        MapViewProvider mMapViewProvider = new MapLibreMapViewProvider(this, mMapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(mapboxMap -> {
            mapLibreMap = mapboxMap; //Get the MapboxMap instance after the map is ready
        });
        mMapViewProvider.getMapxusMapAsync(mapxusMap -> {
            mMapxusMap = mapxusMap; //Get the MapxusMap instance after the indoor map is ready
            //Display of the marker when the floor changes
            mMapxusMap.addOnFloorChangedListener((venue, indoorBuilding, floorInfo) -> {
                mMapFloor = floorInfo == null ? null : floorInfo.getCode();
                if (null != mapxusLocation && floorInfo != null) {
                    //Do not show the positioning icon if the floors are different
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
            //When the map switches between indoor and outdoor, reset the map floor to null
            mMapxusMap.addOnBuildingChangeListener(indoorBuilding -> {
                if (null == indoorBuilding) {
                    mMapFloor = null;
                    Log.d(TAG, "Map change to outdoor.");
                }
            });
        });
    }


    private void updatePositioningMarkerDirection(Float rotation) {
        //Update the angle
        if (positionSymbolLayer != null) {
            float finalRotation = (float) (rotation + (360 - mapLibreMap.getCameraPosition().bearing));
            positionSymbolLayer.setProperties(PropertyFactory.iconRotate(finalRotation));
        }
    }

    /**
     * Update the marker's angle and coordinates
     *
     * @param latLng marker coordinates; latLng == null only update marker's rotation
     */
    private void updatePositionMarker(LatLng latLng) {
        if (null != latLng) {
            if (null != positionMarkerSource) {
                //Update coordinates
                positionMarkerSource.setGeoJson(Feature.fromGeometry(Point.fromLngLat(latLng.getLongitude(),
                        latLng.getLatitude())));
            } else {
                createPositionMarker(latLng);
            }
        }
    }

    private void createPositionMarker(LatLng latLng) {
        Style style = mapLibreMap.getStyle();
        if (style != null) {
            removePositionMarker();
            //Add the positioning icon
            Bitmap positionImage = BitmapFactory.decodeResource(getResources(), R.drawable.arrow);
            style.addImage(POSITION_MARKER_IMAGE, positionImage);
            //Add data
            positionMarkerSource = new GeoJsonSource(POSITION_MARKER_SOURCE,
                    Feature.fromGeometry(Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude())));
            style.addSource(positionMarkerSource);
            //Add the icon
            positionSymbolLayer = new SymbolLayer(POSITION_MARKER_LAYER, POSITION_MARKER_SOURCE)
                    .withProperties(PropertyFactory.iconImage(POSITION_MARKER_IMAGE),
                            PropertyFactory.iconRotate(mCurrentRotation),
                            PropertyFactory.iconIgnorePlacement(true),
                            PropertyFactory.iconAllowOverlap(true));
            style.addLayer(positionSymbolLayer);
        }
    }

    /**
     * Remove the positioning marker
     */
    private void removePositionMarker() {
        if (mapLibreMap.getStyle() != null) {
            mapLibreMap.getStyle().removeSource(POSITION_MARKER_SOURCE);
            mapLibreMap.getStyle().removeLayer(POSITION_MARKER_LAYER);
        }
        positionSymbolLayer = null;
        positionMarkerSource = null;
    }

    /**
     * Query the detailed information of a building by buildingId
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
                //todo Changing the positioning mode during positioning; this feature is not yet available
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
     * Button state before positioning starts (clickable buttons)
     */
    private void enableStartUI() {
        mPositionBinding.optionContent.start.setEnabled(true);
        mPositionBinding.optionContent.pause.setEnabled(false);
        mPositionBinding.optionContent.resume.setEnabled(false);
        mPositionBinding.optionContent.stop.setEnabled(false);
    }

    /**
     * Clickable buttons after pausing positioning
     */
    private void enablePauseUI() {
        mPositionBinding.optionContent.start.setEnabled(false);
        mPositionBinding.optionContent.pause.setEnabled(false);
        mPositionBinding.optionContent.resume.setEnabled(true);
        mPositionBinding.optionContent.stop.setEnabled(true);
    }

    /**
     * Clickable buttons while running
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
