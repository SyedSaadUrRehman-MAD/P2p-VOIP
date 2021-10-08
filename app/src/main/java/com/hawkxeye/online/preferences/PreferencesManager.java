package com.hawkxeye.online.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

public class PreferencesManager {
    private static String PREF_NAME = "OnlineAppSettings";
    private final SharedPreferences mPref;
    private final SharedPreferences DefaultmPref;

    ////////////////////////////

    public static final String IS_MATRIX = "wificam_is_matrix_pref";
    public static final String USER_LOCAL_SETTINGS = "wificam_settings_use_local";
    public static final String IS_AUDIO_ENABLED = "wificam_enable_audio_pref";
    public static final String IS_AUDIO_MASTER_STREAM = "wificam_master_stream_audio_pref";
    public static final String SKIP_TETHER = "wificam_skip_tether_pref";
    public static final String MANUAL_TETHER = "wificam_manual_tether_setup_pref";
    public static final String WIFI_CAMERA_AS_AP = "wificam_as_access_point_pref";
    public static final String WIFI_DIRECT = "wificam_wifi_direct_pref";
    public static final String WIFI_DIRECT_CAMERA_NAME = "wificam_wifi_direct_camera_name_pref";
    public static final String WIFI_DIRECT_AUTO_CONNECT = "wificam_wifi_direct_autoconnect_pref";
    public static final String WIFI_DIRECT_SUPPORT_PIN = "wificam_support_pin_pref";
    public static final String WIFI_DIRECT_CAMERA_PIN = "wificam_wifi_direct_camera_pin_pref";
    public static final String SKIP_TETHER_START_RANGE = "wificam_skip_tether_start_range_ref";
    public static final String SKIP_TETHER_STOP_RANGE = "wificam_skip_tether_end_range_ref";
    public static final String SKIP_TETHER_START_RANGE_DEFAULT = "10.94.101.50";
    public static final String SKIP_TETHER_STOP_RANGE_DEFAULT = "10.94.101.100";
    public static final String SSID = "wificam_tether_ssid_pref";
    public static final String PASSWORD = "wificam_tether_password_pref";
    public static final String SERVER_IS_MATRIX = "server_wificam_is_matrix_pref";
    public static final String SERVER_CAMERA_PROVIDER = "server_wificam_camera_provider";
    public static final String SERVER_WIFI_CONNECTION = "server_wificam_wifi_connection_mode";
    public static final String SERVER_SKIP_TETHER = "server_wificam_skip_tether_pref";
    public static final String SERVER_MANUAL_TETHER = "server_wificam_manual_tether_setup_pref";
    public static final String SERVER_WIFI_DIRECT = "server_wificam_wifi_direct_pref";
    public static final String SERVER_WIFI_DIRECT_CAMERA_NAME = "server_wificam_wifi_direct_camera_name_pref";
    public static final String SERVER_WIFI_DIRECT_CAMERA_PIN = "server_wificam_wifi_direct_camera_pin_pref";
    public static final String SERVER_SSID = "server_wificam_tether_ssid_pref";
    public static final String SERVER_PASSWORD = "server_wificam_tether_password_pref";
    public static final String SERVER_AP_SSID = "server_wificam_apssid_pref";
    public static final String SERVER_AP_PASSWORD = "server_wificam_appassword_pref";
    public static final String USER_NAME = "username_pref";
    public static final String STORE_PASSWORD = "save_user_details_pref";

    public static final String SHOWALL_CAM = "wificam_showallcam_pref";
    public static final String BACKGROUND_STREAMING = "wificam_can_background_stream_pref";
    public static final String GPS_BROADCAST = "wificam_gps_broadcast_pref";
    public static final String ENABLE_FR_PREF = "face_recognition_enabled";
    public static final String AUTO_RECORD_BROADCAST = "wificam_autorec_broadcast_pref";
    public static final String AUTO_LIVE_STREAM = "wificam_auto_stream_pref";
    public static final String AUTO_LOCAL_RECORD_BROADCAST_MATRIX = "wificam_auto_local_rec_broadcast_matrix_pref";
    public static final String AUTO_LOCAL_RECORD_BROADCAST_ONCALL = "wificam_auto_local_rec_broadcast_oncall_pref";

    // should create instance of the preference class to add it inside the hierarchy
    public DefaultPreferences mDefaultPreferences;
    public DefaultServerPreferences mDefaultServerPreferences;


    public PreferencesManager(Context context) {
        mPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        mDefaultPreferences = new DefaultPreferences();
        mDefaultServerPreferences = new DefaultServerPreferences();
        DefaultmPref = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setValue(String key, String value) {
        mPref.edit()
                .putString(key, value)
                .commit();
    }

    public void setValue(String key, int value) {
        mPref.edit()
                .putInt(key, value)
                .commit();
    }

    public void setValue(String key, float value) {
        mPref.edit()
                .putFloat(key, value)
                .commit();
    }

    public void setValue(String key, long value) {
        mPref.edit()
                .putLong(key, value)
                .commit();
    }

    public void setValue(String key, boolean value) {
        mPref.edit()
                .putBoolean(key, value)
                .commit();
    }

    public void remove(String key) {
        mPref.edit()
                .remove(key)
                .commit();
    }

    public boolean clear() {
        return mPref.edit()
                .clear()
                .commit();
    }

    public boolean contains(String key) {
        return mPref.contains(key);
    }


    public class DefaultPreferences {

        public boolean GetFREnabled() {

            return DefaultmPref.getBoolean(ENABLE_FR_PREF, false);
        }


        public boolean GetIsMatrix() {

            return DefaultmPref.getBoolean(IS_MATRIX, false);
        }

        public boolean GetIsAudioEnabled() {
            return DefaultmPref.getBoolean(IS_AUDIO_ENABLED, false);
        }

        public boolean GetWifiDirect() {

            return DefaultmPref.getBoolean(WIFI_DIRECT, false);
        }

        public boolean GetSkipTether() {

            return DefaultmPref.getBoolean(SKIP_TETHER, false);
        }

        public boolean GetManualTether() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                return DefaultmPref.getBoolean(MANUAL_TETHER, true);//Toda default value made true for oreo and above
            else
                return DefaultmPref.getBoolean(MANUAL_TETHER, false);//no change to previous versions
        }

        public boolean GetIsCameraAsAccessPoint() {
            return DefaultmPref.getBoolean(WIFI_CAMERA_AS_AP, false);
        }

        public String GetWifiDirectCameraName() {

            return DefaultmPref.getString(WIFI_DIRECT_CAMERA_NAME, "");
        }

        public String GetWifiDirectCameraPin() {

            return DefaultmPref.getString(WIFI_DIRECT_CAMERA_PIN, "");
        }

        public Boolean GetWifiDirectAutoConnect() {

            return DefaultmPref.getBoolean(WIFI_DIRECT_AUTO_CONNECT, false);
        }

        public Boolean GetWifiDirectSupportsPin() {

            return DefaultmPref.getBoolean(WIFI_DIRECT_SUPPORT_PIN, false);
        }

        public String GetUserName() {
            return DefaultmPref.getString(USER_NAME, "");
        }

        public Boolean GetStorePassword() {
            return DefaultmPref.getBoolean(STORE_PASSWORD, true);
        }

        public String GetSkipTetherStartRange() {

            return DefaultmPref.getString(SKIP_TETHER_START_RANGE, SKIP_TETHER_START_RANGE_DEFAULT);
        }

        public String GetSkipTetherStopRange() {

            return DefaultmPref.getString(SKIP_TETHER_STOP_RANGE, SKIP_TETHER_STOP_RANGE_DEFAULT);
        }

        public String GetSSID() {

            return DefaultmPref.getString(SSID, "");
        }

        public String GetPassword() {

            return DefaultmPref.getString(PASSWORD, "");
        }

        public boolean GetShowAllCameras() {

            return DefaultmPref.getBoolean(SHOWALL_CAM, true);
        }

        public boolean GetCanStreamInBackground() {

            return DefaultmPref.getBoolean(BACKGROUND_STREAMING, false);
        }

        public boolean GetGpsBroadcast() {
            return DefaultmPref.getBoolean(GPS_BROADCAST, false);
        }

        public boolean GetAutoRecordBroadcast() {
            return DefaultmPref.getBoolean(AUTO_RECORD_BROADCAST, false);
        }

        public boolean GetAutoLiveStream() {
            return DefaultmPref.getBoolean(AUTO_LIVE_STREAM, false);
        }

        public boolean GetAutoLocalRecordBroadcastMatrix() {
            return DefaultmPref.getBoolean(AUTO_LOCAL_RECORD_BROADCAST_MATRIX, false);
        }

        public boolean GetAutoLocalRecordBroadcastOncall() {
            return DefaultmPref.getBoolean(AUTO_LOCAL_RECORD_BROADCAST_ONCALL, false);
        }

        public void SetUseLocalSettings(boolean value) {
            DefaultmPref.edit().putBoolean(USER_LOCAL_SETTINGS, value).commit();
        }

        public boolean GetUseLocalSettings() {
            return DefaultmPref.getBoolean(USER_LOCAL_SETTINGS, false);
        }

        public void SetIsMatrix(boolean value) {
            DefaultmPref.edit().putBoolean(IS_MATRIX, value).commit();
        }

        public void SetIsAudioEnabled(boolean value) {
            DefaultmPref.edit().putBoolean(IS_AUDIO_ENABLED, value).commit();
        }

        public void SetIsAudioMasterStream(boolean value) {
            DefaultmPref.edit().putBoolean(IS_AUDIO_MASTER_STREAM, value).commit();
        }

        public void SetWifiDirect(boolean value) {

            DefaultmPref.edit().putBoolean(WIFI_DIRECT, value).commit();
        }

        public void SetSkipTether(boolean value) {

            DefaultmPref.edit().putBoolean(SKIP_TETHER, value).commit();
        }

        public void SetIsCameraAsAccessPoint(Boolean value) {
            DefaultmPref.edit().putBoolean(WIFI_CAMERA_AS_AP, value).commit();
        }

        public void SetManualTether(boolean value) {
            //TODA user cannot change the manual tether preference if it is oreo or greater
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DefaultmPref.edit().putBoolean(MANUAL_TETHER, true).commit();
            } else
                DefaultmPref.edit().putBoolean(MANUAL_TETHER, value).commit();
        }

        public void SetWifiDirectCameraName(String value) {

            DefaultmPref.edit().putString(WIFI_DIRECT_CAMERA_NAME, value).commit();
        }

        public void SetWifiDirectCameraPin(String pin) {

            DefaultmPref.edit().putString(WIFI_DIRECT_CAMERA_PIN, pin).commit();
        }

        public void SetWifiDirectAutoConnect(boolean status) {

            DefaultmPref.edit().putBoolean(WIFI_DIRECT_AUTO_CONNECT, status).commit();
        }

        public void SetWifiDirectSupportsPin(boolean status) {

            DefaultmPref.edit().putBoolean(WIFI_DIRECT_SUPPORT_PIN, status).commit();
        }

        public void SetSkipTetherStartRange(String value) {

            DefaultmPref.edit().putString(SKIP_TETHER_START_RANGE, value).commit();
        }

        public void SetSkipTetherStopRange(String value) {

            DefaultmPref.edit().putString(SKIP_TETHER_STOP_RANGE, value).commit();
        }

        public void SetSSID(String value) {

            DefaultmPref.edit().putString(SSID, value).commit();
        }

        public void SetPassword(String value) {

            DefaultmPref.edit().putString(PASSWORD, value).commit();
        }

        public void SetShowAllCameras(boolean value) {

            DefaultmPref.edit().putBoolean(SHOWALL_CAM, value).commit();
        }

        public void SetCanStreamInBackground(boolean value) {

            DefaultmPref.edit().putBoolean(BACKGROUND_STREAMING, value).commit();
        }

        public void SetGpsBroadcast(boolean value) {
            DefaultmPref.edit().putBoolean(GPS_BROADCAST, value).commit();
        }

        public void SetAutoRecordBroadcast(boolean value) {
            DefaultmPref.edit().putBoolean(AUTO_RECORD_BROADCAST, value).commit();
        }

        public void SetAutoLiveStream(boolean value) {
            DefaultmPref.edit().putBoolean(AUTO_LIVE_STREAM, value).commit();
        }

        public void SetAutoLocalRecordBroadcastMatrix(boolean value) {
            DefaultmPref.edit().putBoolean(AUTO_LOCAL_RECORD_BROADCAST_MATRIX, value).commit();
        }

        public void SetAutoLocalRecordBroadcastOncall(boolean value) {
            DefaultmPref.edit().putBoolean(AUTO_LOCAL_RECORD_BROADCAST_ONCALL, value).commit();
        }

        public void SetUsername(String value) {
            DefaultmPref.edit().putString(USER_NAME,value).commit();
        }

        public void remove(String key) {
            DefaultmPref.edit()
                    .remove(key)
                    .commit();
        }

        public boolean clear() {
            return DefaultmPref.edit()
                    .clear()
                    .commit();
        }

        public boolean contains(String key) {
            return DefaultmPref.contains(key);
        }


    }

    public class DefaultServerPreferences {
        public boolean GetServerIsMatrix() {

            return DefaultmPref.getBoolean(SERVER_IS_MATRIX, false);
        }

        public int GetServerCameraProvider() {

            return DefaultmPref.getInt(SERVER_CAMERA_PROVIDER, 0);
        }

        public int GetServerWifiConnectionMode() {

            return DefaultmPref.getInt(SERVER_WIFI_CONNECTION, 0);
        }

        public boolean GetServerWifiDirect() {

            return DefaultmPref.getBoolean(SERVER_WIFI_DIRECT, false);
        }

        public boolean GetServerSkipTether() {

            return DefaultmPref.getBoolean(SERVER_SKIP_TETHER, false);
        }

        public boolean GetServerManualTether() {

            return DefaultmPref.getBoolean(SERVER_MANUAL_TETHER, false);
        }

        public String GetServerWifiDirectCameraName() {

            return DefaultmPref.getString(SERVER_WIFI_DIRECT_CAMERA_NAME, "");
        }

        public String GetServerWifiDirectCameraPin() {

            return DefaultmPref.getString(SERVER_WIFI_DIRECT_CAMERA_PIN, "");
        }

        public String GetServerSSID() {

            return DefaultmPref.getString(SERVER_SSID, "");
        }

        public String GetServerPassword() {

            return DefaultmPref.getString(SERVER_PASSWORD, "");
        }

        public String GetServerApSSID() {

            return DefaultmPref.getString(SERVER_AP_SSID, "");
        }

        public String GetServerAPPassword() {

            return DefaultmPref.getString(SERVER_AP_PASSWORD, "");
        }


        public void SetServerIsMatrix(boolean value) {

            DefaultmPref.edit().putBoolean(SERVER_IS_MATRIX, value).commit();
        }

        public void SetServerCameraProvider(int value) {

            DefaultmPref.edit().putInt(SERVER_CAMERA_PROVIDER, value).commit();
        }

        public void SetServerWifiConnectionMode(int value) {

            DefaultmPref.edit().putInt(SERVER_WIFI_CONNECTION, value).commit();
        }

        public void SetServerWifiDirect(boolean value) {

            DefaultmPref.edit().putBoolean(SERVER_WIFI_DIRECT, value).commit();
        }

        public void SetServerSkipTether(boolean value) {

            DefaultmPref.edit().putBoolean(SERVER_SKIP_TETHER, value).commit();
        }

        public void SetServerManualTether(boolean value) {

            DefaultmPref.edit().putBoolean(SERVER_MANUAL_TETHER, value).commit();
        }

        public void SetServerWifiDirectCameraName(String value) {

            DefaultmPref.edit().putString(SERVER_WIFI_DIRECT_CAMERA_NAME, value).commit();
        }

        public void SetServerWifiDirectCameraPin(String value) {

            DefaultmPref.edit().putString(SERVER_WIFI_DIRECT_CAMERA_PIN, value).commit();
        }

        public void SetServerSSID(String value) {

            DefaultmPref.edit().putString(SERVER_SSID, value).commit();
        }

        public void SetServerPassword(String value) {

            DefaultmPref.edit().putString(SERVER_PASSWORD, value).commit();
        }

        public void SetServerAPSsid(String value) {

            DefaultmPref.edit().putString(SERVER_AP_SSID, value).commit();
        }

        public void SetServerAPPassword(String value) {

            DefaultmPref.edit().putString(SERVER_AP_PASSWORD, value).commit();
        }

        public void remove(String key) {
            DefaultmPref.edit()
                    .remove(key)
                    .commit();
        }

        public boolean clear() {
            return DefaultmPref.edit()
                    .clear()
                    .commit();
        }

        public boolean contains(String key) {
            return DefaultmPref.contains(key);
        }


    }
}
