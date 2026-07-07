package com.mapxus.positioningsample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Base Activity that performs runtime permission checks.
 * On Android 6.0 and above, runtime permission checks are required when requesting dangerous permissions.
 */

public class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";

    private static final int PERMISSION_REQUEST_CODE = 0; // Parameter for the system permission management page

    /**
     * All permissions that need to be checked
     */
    private final String[] permissions = {
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
        if (lacksPermissions(permissions)) { //Check whether any permission is missing
            requestPermissions(permissions);
        } else {
            Log.d(TAG, "All Permissions Granted");
        }
    }

    /**
     * Determine whether all permissions in the set are granted
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
     * Determine whether a specific permission is missing
     */
    private boolean lacksPermission(String permission) {
        return ContextCompat.checkSelfPermission(BaseActivity.this, permission) ==
                PackageManager.PERMISSION_DENIED;
    }

    /**
     * Request permissions
     */
    private void requestPermissions(String... permissions) {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (!allPermissionsGranted(grantResults)) {
                showMissingPermissionDialog();
            } else {
                Log.d(TAG, "All Permissions Granted");
            }
        }
    }

    /**
     * Determine whether all permissions have been granted
     *
     * @param grantResults result
     * @return boolean
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
     * Prompt information about the missing permissions
     */
    private void showMissingPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this);
        builder.setTitle(R.string.help);
        builder.setMessage(R.string.lacks_permission);

        // Deny and quit the application
        builder.setNegativeButton(R.string.quit, (dialog, which) -> finish());

        builder.setPositiveButton(R.string.setting, (dialog, which) -> startAppSettings());

        builder.setCancelable(false);
        builder.show();
    }

    /**
     * Launch the application settings to grant permissions manually
     */
    private void startAppSettings() {
        Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

}
