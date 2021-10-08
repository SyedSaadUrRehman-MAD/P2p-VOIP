package com.hawkxeye.online;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.hawkxeye.online.discovery.ConnDiscoveryService;
import com.hawkxeye.online.preferences.PreferencesManager;
import com.hawkxeye.online.utils.Utils;

import static com.hawkxeye.online.discovery.ConnDiscoveryService.INTENT_EXTRA_SHOULD_DESTROY;

public class OnlineApp extends MultiDexApplication implements LifecycleObserver {
    public static final String FLAG_CONNECTION_CHANGE = "dji_drone_connection_change";
    public static final String OBSERVER_SERVICE_RESTART = "shouldStartObserver";
    public static final String OBSERVER_SERVICE_RESTART_ACTION = "shouldStartObserverAction";
    public static final String REMOTE_SERVICE_RESTART_ACTION = "shouldStartRemoteAction";
    public static final String FR_SERVICE_RESTART_ACTION = "shouldStartFRAction";
    public static final String OBSERVER_SERVICE_UNBOUND_ACTION = "observerServiceUnboundAction";
    public static final String REMOTE_RECEIVER_SERVICE_NAME = "com.mview.mobileapp.remotecontrol.remoteservice.RemoteControlService";
    public static final String REMOTE_RECEIVER_SERVICE_START_ACTION = REMOTE_RECEIVER_SERVICE_NAME + ".START";
    public static final String FR_RECEIVER_SERVICE_NAME = "com.mview.mobileapp.facerecognizer.FaceRecognizerService";
    public static final String FR_RECEIVER_SERVICE_START_ACTION = FR_RECEIVER_SERVICE_NAME + ".START";
    public static final String DASHBOARD_ACTIVITY = "com.mview.mobileapp.remotecontrol.dashboard.Dashboard";
    public static final String DASHBOARD_ACTIVITY_START_ACTION = DASHBOARD_ACTIVITY + ".START";
    private static final String TAG = OnlineApp.class.getName();
    private static String ravenAccessToken;
    private static Context context;
    private Handler mHandler;
    private static boolean mShowSplash = true;
    private static Activity mCurrentActivity = null;
    private static boolean mRestartingObserver = false;
    private static boolean mRestartingRemote = false;
    private static boolean mRestartingFR = false;
    public static boolean mIsOnForeground = false;

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        //App in background
        mIsOnForeground = false;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onAppForegrounded() {
        // App in foreground
        mIsOnForeground = true;
    }

    private BroadcastReceiver mRestartObserverReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mRestartingObserver)
                        StartObserverService(getApplicationContext());
                    mRestartingObserver = false;
                }
            }, 2000);
        }
    };


    public static void registerActivity(Activity activity) {
        mCurrentActivity = activity;
    }

    public static void unregisterActivity(Activity activity) {
        if (activity == mCurrentActivity)
            mCurrentActivity = null;
    }

    public static Activity getCurrentActivity() {
        return mCurrentActivity;
    }

    public static boolean getShowSplash() {
        return mShowSplash;
    }

    public static boolean isMatrixCamera(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("wificam_is_matrix_pref", false);
    }

    public static void setShowSplash(boolean showSplash) {
        mShowSplash = showSplash;
    }

    private Runnable updateRunnable = new Runnable() {

        @Override
        public void run() {
            Log.d(TAG, FLAG_CONNECTION_CHANGE);
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };

    @Override
    public void onCreate() {

        if (BuildConfig.DEBUG && false) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
//        StartObserverService(getApplicationContext());
        context = getApplicationContext();
        mHandler = new Handler(Looper.getMainLooper());
//        DJISDKManager.getInstance().initSDKManager(this, mDJISDKManagerCallback);
        PreferencesManager m_pref = new PreferencesManager(context);
        boolean m_bStorePassPref = m_pref.mDefaultPreferences.GetStorePassword();

        if (!m_bStorePassPref) {
            m_pref.mDefaultPreferences.remove("password_pref");
        }
        registerReceiver(mRestartObserverReceiver, new IntentFilter(OBSERVER_SERVICE_RESTART_ACTION));
        //DJISDKManager.getInstance().setCallbackRunInUIThread(false);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
//        mViewDFM = MViewDFM.getInstance(OnlineApp.this);
//        mViewDFM.init(OnlineApp.this);
    }

    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

    public static final void RestartObserverService(Context context) {
        if (mRestartingObserver)
            return;

        if (Utils.isServiceRunning(context, ConnDiscoveryService.class.getName())) {
            mRestartingObserver = true;
            StopObserverService(context);
        } else {
            StartObserverService(context);
        }
    }


    public static final void StopObserverService(Context context) {
        Intent intent = new Intent(context, ConnDiscoveryService.class);
        intent.setAction(ConnDiscoveryService.INTENT_ACTION_CLEANUP_OBSERVER_SESSION);
        intent.putExtra(INTENT_EXTRA_SHOULD_DESTROY, true);
        if (!OnlineApp.mIsOnForeground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static final void StartObserverService(Context context) {
        if (!Utils.isServiceRunning(context, ConnDiscoveryService.class.getName())) {
            Intent intent = new Intent(context, ConnDiscoveryService.class);
            intent.setAction(ConnDiscoveryService.INTENT_ACTION_DISCOVER_PEERS);
            if (!OnlineApp.mIsOnForeground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        //unregistering receiver
        try {
            StopObserverService(context);
            unregisterReceiver(mRestartObserverReceiver);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

    }
}
