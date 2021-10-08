package com.hawkxeye.online.ui.main.base;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Toast;

import com.hawkxeye.online.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

//region "statics definitions"
import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;//Location group
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;//storage group
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_PHONE_STATE;//Phone group
import static android.Manifest.permission.CAMERA;//camera group
import static android.Manifest.permission.RECORD_AUDIO;//microphone group
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.CHANGE_WIFI_STATE;
import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.CHANGE_NETWORK_STATE;
import static android.Manifest.permission.VIBRATE;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.WAKE_LOCK;
import static android.Manifest.permission.RECEIVE_BOOT_COMPLETED;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_ADMIN;
import static android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;
import static android.Manifest.permission.FOREGROUND_SERVICE;

//endregion
public class BasePermissibleActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    //region "Function references"
    private ArrayList<Callable<Void>> FUNC_TO_PERFORM_AFTER_LOCATION_PERMISSION_GRANTS;
    private ArrayList<Callable<Void>> FUNC_TO_PERFORM_AFTER_STORAGE_PERMISSION_GRANTS;
    private ArrayList<Callable<Void>> FUNC_TO_PERFORM_AFTER_PHONE_STATE_PERMISSION_GRANTS;
    private ArrayList<Callable<Void>> FUNC_TO_PERFORM_AFTER_GET_ACCOUNT_PERMISSION_GRANTS;
    private ArrayList<Callable<Void>> FUNC_TO_PERFORM_AFTER_CAMERA_PERMISSION_GRANTS;
    private ArrayList<Callable<Void>> FUNC_TO_PERFORM_AFTER_CALENDAR_PERMISSION_GRANTS;
    private ArrayList<Callable<Void>> FUNC_TO_PERFORM_AFTER_MIC_PERMISSION_GRANTS;
    private ArrayList<Callable<Void>> FUNC_TO_PERFORM_AFTER_ALL_PERMISSION_GRANTS;
    private ArrayList<Callable<Void>> FUNC_TO_PERFORM_AFTER_PRIMARY_PERMISSION_GRANTS;
    private ArrayList<Callable<Void>> FUNC_TO_PERFORM_AFTER_SECONDARY_PERMISSION_GRANTS;
    private ArrayList<Callable<Void>> FUNC_TO_PERFORM_AFTER_DJI_PERMISSION_GRANTS;
    private ArrayList<Callable<Void>> FUNC_TO_PERFORM_AFTER_NETWORK_PERMISSION_GRANTS;
    //endregion

    //region "Permissions Request Codes"
    private static final int LOCATION_PERMISSIONS_REQUEST_CODE = 300;
    private static final int STORAGE_PERMISSIONS_REQUEST_CODE = 301;
    private static final int PHONE_STATE_PERMISSIONS_REQUEST_CODE = 302;
    private static final int GET_ACCOUNTS_PERMISSIONS_REQUEST_CODE = 303;
    private static final int CAMERA_PERMISSIONS_REQUEST_CODE = 304;
    private static final int CALENDAR_PERMISSIONS_REQUEST_CODE = 305;
    private static final int MIC_PERMISSIONS_REQUEST_CODE = 306;
    private static final int ALL_PERMISSIONS_REQUEST_CODE = 1111;
    private static final int PRIMARY_PERMISSIONS_REQUEST_CODE = 1112;
    private static final int SECONDARY_PERMISSIONS_REQUEST_CODE = 1113;
    private static final int DJI_PERMISSION_REQUEST_CODE = 1114;
    private static final int NETWORK_PERMISSION_REQUEST_CODE = 1115;

    //endregion

    //region "Class Specific Variables"
    private static String TAG = "com.hawkxeye.online.application.BasePermissibleActivity";
    private androidx.appcompat.app.AlertDialog dialog;
    //endregion

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FUNC_TO_PERFORM_AFTER_LOCATION_PERMISSION_GRANTS = new ArrayList();
        FUNC_TO_PERFORM_AFTER_STORAGE_PERMISSION_GRANTS = new ArrayList();
        FUNC_TO_PERFORM_AFTER_PHONE_STATE_PERMISSION_GRANTS = new ArrayList();
        FUNC_TO_PERFORM_AFTER_GET_ACCOUNT_PERMISSION_GRANTS = new ArrayList();
        FUNC_TO_PERFORM_AFTER_CAMERA_PERMISSION_GRANTS = new ArrayList();
        FUNC_TO_PERFORM_AFTER_CALENDAR_PERMISSION_GRANTS = new ArrayList();
        FUNC_TO_PERFORM_AFTER_MIC_PERMISSION_GRANTS = new ArrayList();
        FUNC_TO_PERFORM_AFTER_ALL_PERMISSION_GRANTS = new ArrayList();
        FUNC_TO_PERFORM_AFTER_PRIMARY_PERMISSION_GRANTS = new ArrayList();
        FUNC_TO_PERFORM_AFTER_SECONDARY_PERMISSION_GRANTS = new ArrayList();
        FUNC_TO_PERFORM_AFTER_DJI_PERMISSION_GRANTS = new ArrayList();
        FUNC_TO_PERFORM_AFTER_NETWORK_PERMISSION_GRANTS = new ArrayList();
        requestPrimaryPermissions(null);
        requestSecondaryPermissions(null,false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, BasePermissibleActivity.this);
    }

    //callback method of the interface EasyPermissions.PermissionsCallback
    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (getPermsForRequestCode(requestCode).length == perms.size())
            PerformRespectiveFunction(requestCode);
    }

    //This will call the respective Function reference from request code
    public void PerformRespectiveFunction(int requestCode) {
        try {
            callFuncsAndClear(getFuncsForRequestCode(requestCode));
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }

    private void callFuncsAndClear(ArrayList<Callable<Void>> list) {
        if (list != null) {
            for (Callable<Void> fun : list) {
                try {
                    fun.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            list.clear();
        }
    }

    //callback method of the interface EasyPermissions.PermissionsCallback
    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        Log.d(TAG, "Permissions denied:" + perms.toString());
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
        //again throwing the request back to user
        reboundDeniedRequests(requestCode);
    }

    //this method will check from the request code of denied request that which suitable request should now be called
    private void reboundDeniedRequests(int requestCode) {
        switch (requestCode) {
            case PHONE_STATE_PERMISSIONS_REQUEST_CODE:
                requestPhoneStatePermission();
                break;
            case LOCATION_PERMISSIONS_REQUEST_CODE:
                requestLocationPermission();
                break;
            case STORAGE_PERMISSIONS_REQUEST_CODE:
                requestStoragePermission();
                break;
            case CAMERA_PERMISSIONS_REQUEST_CODE:
                requestCameraPermission();
                break;
            case MIC_PERMISSIONS_REQUEST_CODE:
                requestMicPermission();
                break;
            case ALL_PERMISSIONS_REQUEST_CODE:
                requestAllPermissions();
                break;
            case PRIMARY_PERMISSIONS_REQUEST_CODE:
                requestPrimaryPermissions(null);
                break;
            case SECONDARY_PERMISSIONS_REQUEST_CODE:
                requestSecondaryPermissions(null, false);
                break;
            case DJI_PERMISSION_REQUEST_CODE:
                requestDJIPermissions(null, false);
                break;
            case NETWORK_PERMISSION_REQUEST_CODE:
                requestNetworkPermissions(null, false);
                break;
        }
    }

    private ArrayList<Callable<Void>> getFuncsForRequestCode(int requestCode) {
        switch (requestCode) {
            case LOCATION_PERMISSIONS_REQUEST_CODE:
                return FUNC_TO_PERFORM_AFTER_LOCATION_PERMISSION_GRANTS;
            case STORAGE_PERMISSIONS_REQUEST_CODE:
                return FUNC_TO_PERFORM_AFTER_STORAGE_PERMISSION_GRANTS;
            case PHONE_STATE_PERMISSIONS_REQUEST_CODE:
                return FUNC_TO_PERFORM_AFTER_PHONE_STATE_PERMISSION_GRANTS;
            case GET_ACCOUNTS_PERMISSIONS_REQUEST_CODE:
                return FUNC_TO_PERFORM_AFTER_GET_ACCOUNT_PERMISSION_GRANTS;
            case CAMERA_PERMISSIONS_REQUEST_CODE:
                return FUNC_TO_PERFORM_AFTER_CAMERA_PERMISSION_GRANTS;
            case CALENDAR_PERMISSIONS_REQUEST_CODE:
                return FUNC_TO_PERFORM_AFTER_CALENDAR_PERMISSION_GRANTS;
            case MIC_PERMISSIONS_REQUEST_CODE:
                return FUNC_TO_PERFORM_AFTER_MIC_PERMISSION_GRANTS;
            case PRIMARY_PERMISSIONS_REQUEST_CODE:
                return FUNC_TO_PERFORM_AFTER_PRIMARY_PERMISSION_GRANTS;
            case SECONDARY_PERMISSIONS_REQUEST_CODE:
                return FUNC_TO_PERFORM_AFTER_SECONDARY_PERMISSION_GRANTS;
            case ALL_PERMISSIONS_REQUEST_CODE:
                return FUNC_TO_PERFORM_AFTER_ALL_PERMISSION_GRANTS;
            case DJI_PERMISSION_REQUEST_CODE:
                return FUNC_TO_PERFORM_AFTER_DJI_PERMISSION_GRANTS;
            case NETWORK_PERMISSION_REQUEST_CODE:
                return FUNC_TO_PERFORM_AFTER_NETWORK_PERMISSION_GRANTS;
        }
        return null;
    }

    //region "Checking Location Permission"
    public void checkLocationPermissionsAndPerform(Callable<Void> function) {
        if (function != null && !FUNC_TO_PERFORM_AFTER_LOCATION_PERMISSION_GRANTS.contains(function)) {
            FUNC_TO_PERFORM_AFTER_LOCATION_PERMISSION_GRANTS.add(function);
            requestLocationPermission();
        }
    }

    public void requestLocationPermission() {
        String[] perms = getPermsForRequestCode(LOCATION_PERMISSIONS_REQUEST_CODE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || EasyPermissions.hasPermissions(this, perms)) {
            PerformRespectiveFunction(LOCATION_PERMISSIONS_REQUEST_CODE);
        } else {
            EasyPermissions.requestPermissions(this, "Please grant the location permission", LOCATION_PERMISSIONS_REQUEST_CODE, perms);
        }
    }
    //endregion

    //region "Storage Permissions Check"
    public void checkStoragePermissionsAndPerform(Callable<Void> function) {

        if (function != null && !FUNC_TO_PERFORM_AFTER_STORAGE_PERMISSION_GRANTS.contains(function)) {
            FUNC_TO_PERFORM_AFTER_STORAGE_PERMISSION_GRANTS.add(function);
            requestStoragePermission();
        }
    }

    public void requestStoragePermission() {
        String[] perms = getPermsForRequestCode(STORAGE_PERMISSIONS_REQUEST_CODE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || EasyPermissions.hasPermissions(this, perms)) {
            PerformRespectiveFunction(STORAGE_PERMISSIONS_REQUEST_CODE);
        } else {
            EasyPermissions.requestPermissions(this, "Please grant the Write permissions to save recordings.", STORAGE_PERMISSIONS_REQUEST_CODE, perms);
        }
    }
    //endregion

    //region "Phone State Permission"
    public void checkPhoneStatePermissionsAndPerform(Callable<Void> function) {
        if (function != null && !FUNC_TO_PERFORM_AFTER_PHONE_STATE_PERMISSION_GRANTS.contains(function)) {
            FUNC_TO_PERFORM_AFTER_PHONE_STATE_PERMISSION_GRANTS.add(function);
            requestPhoneStatePermission();
        }
    }

    public void requestPhoneStatePermission() {
        String[] perms = getPermsForRequestCode(PHONE_STATE_PERMISSIONS_REQUEST_CODE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || EasyPermissions.hasPermissions(this, perms)) {
            PerformRespectiveFunction(PHONE_STATE_PERMISSIONS_REQUEST_CODE);
        } else {
            EasyPermissions.requestPermissions(this, "Please grant the read phone state permissions.", PHONE_STATE_PERMISSIONS_REQUEST_CODE, perms);
        }
    }
    //endregion

    //region "Get Camera Permission Check"
    public void checkCameraPermissionsAndPerform(Callable<Void> function) {
        if (function != null && !FUNC_TO_PERFORM_AFTER_CAMERA_PERMISSION_GRANTS.contains(function)) {
            FUNC_TO_PERFORM_AFTER_CAMERA_PERMISSION_GRANTS.add(function);
            requestCameraPermission();
        }
    }

    public void requestCameraPermission() {
        String[] perms = getPermsForRequestCode(CAMERA_PERMISSIONS_REQUEST_CODE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || EasyPermissions.hasPermissions(this, perms)) {
            PerformRespectiveFunction(CAMERA_PERMISSIONS_REQUEST_CODE);
        } else {
            EasyPermissions.requestPermissions(this, "Please grant the Camera permissions.", CAMERA_PERMISSIONS_REQUEST_CODE, perms);
        }
    }
    //endregion

    //region "Calendar Permission Check"
    public void checkCalendarPermissionsAndPerform(Callable<Void> function) {
        if (function != null && !FUNC_TO_PERFORM_AFTER_CALENDAR_PERMISSION_GRANTS.contains(function)) {
            FUNC_TO_PERFORM_AFTER_CALENDAR_PERMISSION_GRANTS.add(function);
            requestCalendarPermission();
        }
    }

    public void requestCalendarPermission() {
//        String[] perms = {READ_CALENDAR};
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || EasyPermissions.hasPermissions(this, perms)) {
//            PerformRespectiveFunction(CALENDAR_PERMISSIONS_REQUEST_CODE);
//        } else {
//            EasyPermissions.requestPermissions(this, "Please grant the read Calendar permissions.", CALENDAR_PERMISSIONS_REQUEST_CODE, perms);
//        }
    }
    //endregion

    //region "MIC Permission Check"
    public void checkMicPermissionsAndPerform(Callable<Void> function) {
        if (function != null && !FUNC_TO_PERFORM_AFTER_MIC_PERMISSION_GRANTS.contains(function)) {
            FUNC_TO_PERFORM_AFTER_MIC_PERMISSION_GRANTS.add(function);
            requestMicPermission();
        }
    }

    public void requestMicPermission() {
        String[] perms = getPermsForRequestCode(MIC_PERMISSIONS_REQUEST_CODE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || EasyPermissions.hasPermissions(this, perms)) {
            PerformRespectiveFunction(MIC_PERMISSIONS_REQUEST_CODE);
        } else {
            EasyPermissions.requestPermissions(this, "Please grant the microphone permissions.", MIC_PERMISSIONS_REQUEST_CODE, perms);
        }
    }

    //endregion

    //region "Bulk requesting"
    public void requestAllPermissions() {
        requestAllPermissions(null);
    }

    public void requestAllPermissions(Callable<Void> function) {
        if (function != null && !FUNC_TO_PERFORM_AFTER_ALL_PERMISSION_GRANTS.contains(function))
            FUNC_TO_PERFORM_AFTER_ALL_PERMISSION_GRANTS.add(function);

        String[] perms = getPermsForRequestCode(ALL_PERMISSIONS_REQUEST_CODE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || EasyPermissions.hasPermissions(this, perms)) {
            PerformRespectiveFunction(ALL_PERMISSIONS_REQUEST_CODE);
        } else {
            EasyPermissions.requestPermissions(this, "Please grant the permissions, required for the app to work properly.", ALL_PERMISSIONS_REQUEST_CODE, perms);
        }

    }

    public void requestPrimaryPermissions(Callable<Void> function) {
        if (function != null && !FUNC_TO_PERFORM_AFTER_PRIMARY_PERMISSION_GRANTS.contains(function))
            FUNC_TO_PERFORM_AFTER_PRIMARY_PERMISSION_GRANTS.add(function);

        String[] perms = getPermsForRequestCode(PRIMARY_PERMISSIONS_REQUEST_CODE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || EasyPermissions.hasPermissions(this, perms)) {
            PerformRespectiveFunction(PRIMARY_PERMISSIONS_REQUEST_CODE);
        } else {
            EasyPermissions.requestPermissions(this,
                    "Please grant the permissions, required for the app to work properly.",
                    PRIMARY_PERMISSIONS_REQUEST_CODE,
                    perms);
        }
    }

    public void requestSecondaryPermissions(Callable<Void> function, boolean isTrigger) {
        if (isTrigger)
            FUNC_TO_PERFORM_AFTER_SECONDARY_PERMISSION_GRANTS.clear();
        if (function != null && !FUNC_TO_PERFORM_AFTER_SECONDARY_PERMISSION_GRANTS.contains(function))
            FUNC_TO_PERFORM_AFTER_SECONDARY_PERMISSION_GRANTS.add(function);
        String[] perms = getPermsForRequestCode(SECONDARY_PERMISSIONS_REQUEST_CODE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || EasyPermissions.hasPermissions(this, perms)) {
            PerformRespectiveFunction(SECONDARY_PERMISSIONS_REQUEST_CODE);
        } else {
            EasyPermissions.requestPermissions(this,
                    "Please grant the permissions, required for the app to work properly.",
                    SECONDARY_PERMISSIONS_REQUEST_CODE,
                    perms);
        }
    }

    //dji sdk
    public void requestDJIPermissions(Callable<Void> function, boolean isTrigger) {
        if (isTrigger)
            FUNC_TO_PERFORM_AFTER_DJI_PERMISSION_GRANTS.clear();
        if (function != null && !FUNC_TO_PERFORM_AFTER_DJI_PERMISSION_GRANTS.contains(function))
            FUNC_TO_PERFORM_AFTER_DJI_PERMISSION_GRANTS.add(function);
        String[] perms = getPermsForRequestCode(DJI_PERMISSION_REQUEST_CODE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || EasyPermissions.hasPermissions(this, perms)) {
            PerformRespectiveFunction(DJI_PERMISSION_REQUEST_CODE);
        } else {
            EasyPermissions.requestPermissions(this,
                    "Please grant the permissions, required for the DJI to work properly.",
                    DJI_PERMISSION_REQUEST_CODE,
                    perms);
        }
    }
    //endregion

    //NETWORK perms request
    public void requestNetworkPermissions(Callable<Void> function, boolean isTrigger) {
        if (isTrigger)
            FUNC_TO_PERFORM_AFTER_NETWORK_PERMISSION_GRANTS.clear();
        if (function != null && !FUNC_TO_PERFORM_AFTER_NETWORK_PERMISSION_GRANTS.contains(function))
            FUNC_TO_PERFORM_AFTER_NETWORK_PERMISSION_GRANTS.add(function);
        String[] perms = getPermsForRequestCode(NETWORK_PERMISSION_REQUEST_CODE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || EasyPermissions.hasPermissions(this, perms)) {
            PerformRespectiveFunction(NETWORK_PERMISSION_REQUEST_CODE);
        } else {
            EasyPermissions.requestPermissions(this,
                    "Please grant the network permissions, for the app to work properly.",
                    NETWORK_PERMISSION_REQUEST_CODE,
                    perms);
        }
    }
    //endregion

    public String[] getPermsForRequestCode(int requestCode) {
        String[] perms = null;
        switch (requestCode) {
            case LOCATION_PERMISSIONS_REQUEST_CODE:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                    perms = new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION};
                else
                    perms = new String[]{ACCESS_FINE_LOCATION,
                            ACCESS_BACKGROUND_LOCATION, ACCESS_COARSE_LOCATION};
                break;
            case STORAGE_PERMISSIONS_REQUEST_CODE:
                perms = new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE};
                break;
            case PHONE_STATE_PERMISSIONS_REQUEST_CODE:
                perms = new String[]{READ_PHONE_STATE};
                break;
            case GET_ACCOUNTS_PERMISSIONS_REQUEST_CODE:
                perms = new String[]{};
                break;//not required
            case CAMERA_PERMISSIONS_REQUEST_CODE:
                perms = new String[]{CAMERA};
                break;
            case CALENDAR_PERMISSIONS_REQUEST_CODE:
                perms = new String[]{};
                break;//not required
            case MIC_PERMISSIONS_REQUEST_CODE:
                perms = new String[]{RECORD_AUDIO};
                break;
            case PRIMARY_PERMISSIONS_REQUEST_CODE:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
                    perms = new String[]{
                            READ_PHONE_STATE,
                            ACCESS_WIFI_STATE,
                            CHANGE_WIFI_STATE,
                            ACCESS_NETWORK_STATE,
                            CHANGE_NETWORK_STATE,
                            VIBRATE,
                            INTERNET,
                            WAKE_LOCK,
                            RECEIVE_BOOT_COMPLETED,
                            BLUETOOTH,
                            BLUETOOTH_ADMIN,
                            REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    };
                else
                    perms = new String[]{
                            READ_PHONE_STATE,
                            ACCESS_WIFI_STATE,
                            CHANGE_WIFI_STATE,
                            ACCESS_NETWORK_STATE,
                            CHANGE_NETWORK_STATE,
                            VIBRATE,
                            INTERNET,
                            WAKE_LOCK,
                            RECEIVE_BOOT_COMPLETED,
                            BLUETOOTH,
                            BLUETOOTH_ADMIN,
                            REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            FOREGROUND_SERVICE
                    };
                break;
            case SECONDARY_PERMISSIONS_REQUEST_CODE:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                    perms = new String[]{
                            ACCESS_FINE_LOCATION,
                            ACCESS_COARSE_LOCATION,
                            WRITE_EXTERNAL_STORAGE,
                            READ_EXTERNAL_STORAGE,
                            READ_PHONE_STATE,
                            CAMERA,
                            RECORD_AUDIO
                    };
                else
                    perms = new String[]{
                            ACCESS_FINE_LOCATION,
                            ACCESS_COARSE_LOCATION,
                            ACCESS_BACKGROUND_LOCATION,
                            WRITE_EXTERNAL_STORAGE,
                            READ_EXTERNAL_STORAGE,
                            READ_PHONE_STATE,
                            CAMERA,
                            RECORD_AUDIO,
                            FOREGROUND_SERVICE
                    };
                break;
            case ALL_PERMISSIONS_REQUEST_CODE:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
                        perms = new String[]{
                                ACCESS_FINE_LOCATION,
                                ACCESS_COARSE_LOCATION,
                                WRITE_EXTERNAL_STORAGE,
                                READ_EXTERNAL_STORAGE,
                                READ_PHONE_STATE,
                                CAMERA,
                                RECORD_AUDIO,
                                ACCESS_WIFI_STATE,
                                CHANGE_WIFI_STATE,
                                ACCESS_NETWORK_STATE,
                                CHANGE_NETWORK_STATE,
                                VIBRATE,
                                INTERNET,
                                WAKE_LOCK,
                                RECEIVE_BOOT_COMPLETED,
                                BLUETOOTH,
                                BLUETOOTH_ADMIN,
                                REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        };
                    else
                        perms = new String[]{
                                ACCESS_FINE_LOCATION,
                                ACCESS_COARSE_LOCATION,
                                WRITE_EXTERNAL_STORAGE,
                                READ_EXTERNAL_STORAGE,
                                READ_PHONE_STATE,
                                CAMERA,
                                RECORD_AUDIO,
                                ACCESS_WIFI_STATE,
                                CHANGE_WIFI_STATE,
                                ACCESS_NETWORK_STATE,
                                CHANGE_NETWORK_STATE,
                                VIBRATE,
                                INTERNET,
                                WAKE_LOCK,
                                RECEIVE_BOOT_COMPLETED,
                                BLUETOOTH,
                                BLUETOOTH_ADMIN,
                                REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                FOREGROUND_SERVICE
                        };
                } else {
                    perms = new String[]{
                            ACCESS_FINE_LOCATION,
                            ACCESS_COARSE_LOCATION,
                            ACCESS_BACKGROUND_LOCATION,
                            WRITE_EXTERNAL_STORAGE,
                            READ_EXTERNAL_STORAGE,
                            READ_PHONE_STATE,
                            CAMERA,
                            RECORD_AUDIO,
                            ACCESS_WIFI_STATE,
                            CHANGE_WIFI_STATE,
                            ACCESS_NETWORK_STATE,
                            CHANGE_NETWORK_STATE,
                            VIBRATE,
                            INTERNET,
                            WAKE_LOCK,
                            RECEIVE_BOOT_COMPLETED,
                            BLUETOOTH,
                            BLUETOOTH_ADMIN,
                            REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            FOREGROUND_SERVICE
                    };
                }
                break;
            case DJI_PERMISSION_REQUEST_CODE:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                    perms = new String[]{
                            VIBRATE,
                            INTERNET,
                            ACCESS_WIFI_STATE,
                            WAKE_LOCK,
                            ACCESS_COARSE_LOCATION,
                            ACCESS_NETWORK_STATE,
                            ACCESS_FINE_LOCATION,
                            CHANGE_WIFI_STATE,
                            WRITE_EXTERNAL_STORAGE,
                            BLUETOOTH,
                            BLUETOOTH_ADMIN,
                            READ_EXTERNAL_STORAGE,
                            READ_PHONE_STATE
                    };
                else
                    perms = new String[]{
                            VIBRATE,
                            INTERNET,
                            ACCESS_WIFI_STATE,
                            WAKE_LOCK,
                            ACCESS_COARSE_LOCATION,
                            ACCESS_BACKGROUND_LOCATION,
                            ACCESS_NETWORK_STATE,
                            ACCESS_FINE_LOCATION,
                            CHANGE_WIFI_STATE,
                            WRITE_EXTERNAL_STORAGE,
                            BLUETOOTH,
                            BLUETOOTH_ADMIN,
                            READ_EXTERNAL_STORAGE,
                            READ_PHONE_STATE
                    };
                break;
            case NETWORK_PERMISSION_REQUEST_CODE:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                    perms = new String[]{
                            INTERNET,
                            ACCESS_WIFI_STATE,
                            ACCESS_COARSE_LOCATION,
                            ACCESS_NETWORK_STATE,
                            ACCESS_FINE_LOCATION,
                            CHANGE_WIFI_STATE
                    };
                else
                    perms = new String[]{
                            INTERNET,
                            ACCESS_WIFI_STATE,
                            ACCESS_COARSE_LOCATION,
                            ACCESS_BACKGROUND_LOCATION,
                            ACCESS_NETWORK_STATE,
                            ACCESS_FINE_LOCATION,
                            CHANGE_WIFI_STATE
                    };
                break;
        }
        return perms;
    }

    public void showToast(String text) {
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public void showDialog(String msg) {
        dialog = new AlertDialog.Builder(BasePermissibleActivity.this, R.style.DialogCustomThemeDark).create();
        dialog.setMessage(msg);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.setCancelable(true);
        dialog.show();
    }

    public void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public void onLoggedout() {
        //implementation to be provided by the subclass
    }

    public boolean isLoggedIn() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean("check_credential_pref", false);
    }

    boolean animationStarted;

//    protected void shineViewIfAvailable(final View img) {
//        if (img == null) return;
//        final ImageView shine = findViewById(R.id.shine);
//        img.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                if (!animationStarted) {
//                    animationStarted = true;
//                    startShineAnimation(shine, img.getWidth());
//                }
//            }
//        });
//    }

    private void startShineAnimation(View shine, int width) {
        final Handler animHandler = new Handler();
        final Runnable animator = new Runnable() {
            @Override
            public void run() {
                if (shine != null) {
                    runOnUiThread(() -> {
                        Animation animation = new TranslateAnimation(-10, width + 10, 0, 0);
                        animation.setDuration(1000);
                        animation.setFillAfter(false);
                        animation.setInterpolator(new AccelerateDecelerateInterpolator());
                        animation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                shine.setVisibility(View.GONE);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                                shine.setVisibility(View.VISIBLE);
                            }
                        });
                        shine.setVisibility(View.VISIBLE);
                        shine.startAnimation(animation);
                    });
                    animHandler.postDelayed(this, 5000);
                }
            }
        };
        animHandler.removeCallbacks(animator);
        animHandler.postDelayed(animator, 2000);
    }

    /**
     * This is called from the xml layout
     *
     * @param v
     */
    public void onBack(View v) {
        onBackPressed();
    }
}
