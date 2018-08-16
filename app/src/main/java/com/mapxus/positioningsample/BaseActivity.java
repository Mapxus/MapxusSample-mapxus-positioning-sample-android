package com.mapxus.positioningsample;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * 基础Activity,运行权限检测
 * Android6.0及以上在获取危险权限时需要运行权限检测
 */

public class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";

    private static final int PERMISSION_REQUEST_CODE = 0; // 系统权限管理页面的参数

    /**
     * 所有需要检测的权限
     */
    private String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= 23 && getApplicationInfo().targetSdkVersion >= 23) {
            if (lacksPermissions(permissions)) { //检测是否缺少权限
                requestPermissions(permissions);
            } else {
                Log.d(TAG, "All Permissions Granted");
            }
        }
    }

    /**
     * 判断权限集合所有权限是否授予
     */
    public boolean lacksPermissions(String... permissions) {
        for (String permission : permissions) {
            if (lacksPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否缺少某个权限
     */
    private boolean lacksPermission(String permission) {
        return ContextCompat.checkSelfPermission(BaseActivity.this, permission) ==
                PackageManager.PERMISSION_DENIED;
    }

    /**
     * 权限检测
     */
    private void requestPermissions(String... permissions) {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (!allPermissionsGranted(grantResults)) {
                showMissingPermissionDialog();
            } else {
                Log.d(TAG, "All Permissions Granted");
            }
        }
    }

    /**
     * 判断是否所有的权限都已授予
     *
     * @param grantResults
     * @return
     */
    private boolean allPermissionsGranted(@NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }


    /**
     * 提示缺失的权限信息
     */
    private void showMissingPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this);
        builder.setTitle(R.string.help);
        builder.setMessage(R.string.lacks_permission);

        // 拒绝, 退出应用
        builder.setNegativeButton(R.string.quit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        builder.setPositiveButton(R.string.setting, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startAppSettings();
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    /**
     * 启动应用的设置,手动设置权限
     */
    private void startAppSettings() {
        Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

}
