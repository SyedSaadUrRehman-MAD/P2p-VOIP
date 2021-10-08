package com.hawkxeye.online.discovery.modes.p2p;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;

import com.hawkxeye.online.discovery.modes.models.DeviceDetails;
import com.hawkxeye.online.preferences.PreferencesManager;
import com.hawkxeye.online.utils.Constants;
import com.hawkxeye.online.utils.ReflectionUtils;

/**
 * P2pCamsManager manages all the wifi direct specific activities, connections, disconnections, success and failures
 */
public class P2pCamsManager implements P2pCamsObserver.ObserverCallback {
    private static final String TAG = P2pCamsManager.class.getSimpleName();
    private static P2pCamsManager INSTANCE;
    public static final String ACTION_P2P_DEVICES_AVAILABLE = P2pCamsManager.class.getSimpleName() + ".ACTION_P2P_DEVICES_AVAILABLE";
    public static final String ACTION_P2P_DEVICE_AWAITING_CONNECTION = P2pCamsManager.class.getSimpleName() + ".ACTION_P2P_DEVICE_AWAITING_CONNECTION";
    public static final String ACTION_P2P_DEVICE_CONNECTED = P2pCamsManager.class.getSimpleName() + ".ACTION_P2P_DEVICE_CONNECTED";
    public static final String ACTION_P2P_DEVICE_CONNECTION_FAILED = P2pCamsManager.class.getSimpleName() + ".ACTION_P2P_DEVICE_CONNECTION_FAILED";
    public static final String ACTION_P2P_DEVICE_DISCONNECTING = P2pCamsManager.class.getSimpleName() + ".ACTION_P2P_DEVICE_DISCONNECTING";
    public static final String ACTION_P2P_DEVICE_DISCONNECT_COMPLETED = P2pCamsManager.class.getSimpleName() + ".ACTION_P2P_DEVICE_DISCONNECT_COMPLETED";

    public static final String EXTRA_P2P_DEVICE_LIST = "Extrap2pDevices";
    public static final String EXTRA_P2P_DEVICE = "Extrap2pDeviceInHand";
    public static final String EXTRA_P2P_DEVICE_DESIGNATION = "Extrap2pThisDeviceDesignation";
    public static final String EXTRA_P2P_GROUP_OWNER_ADDR = "Extrap2pGroupOwnerAddress";

    private static WifiDirectPreferences mP2pPreferences;
    private final WifiManager.WifiLock mP2pLock;

    private WifiP2pManager mP2pManager;
    private WifiP2pManager.Channel mP2pChannel;
    private WifiDirectAutoAccept mP2pAutoAccept;

    private P2pCamsObserver mP2pObserver;

    private Context mContext;
    private HandlerThread mHandlerThread;
    private boolean destroyed;

    private WifiP2pManager.ChannelListener mChannelListener = new WifiP2pManager.ChannelListener() {
        @Override
        public void onChannelDisconnected() {
            Log.d(TAG, "wifi channel Disconnected.");
//            if (mP2pObserver != null) {
//                mP2pObserver.postConnectionFailed(null);
//                mP2pObserver.destroy();
//                mP2pObserver = null;
//            }
            if (!destroyed) {
                Log.d(TAG, "p2pManager not destroyed, reinitializing channel.");
                mP2pChannel = mP2pManager.initialize(mContext, mHandlerThread.getLooper(), mChannelListener);
                if (mP2pObserver != null)
                    mP2pObserver.updateChannel(mP2pChannel);
            }
        }
    };
    private boolean isPeerConnected = false;

    private P2pCamsManager(Context context) {
        mContext = context;
        mHandlerThread = new HandlerThread("P2pHandlerThread");
        mHandlerThread.start();
        mP2pManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        mP2pChannel = mP2pManager.initialize(mContext, mHandlerThread.getLooper(), mChannelListener);
        mP2pAutoAccept = new WifiDirectAutoAccept(mP2pManager, mP2pChannel, mContext);
        mP2pAutoAccept.intercept(true);
        mP2pPreferences = new WifiDirectPreferences(mContext);
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mP2pLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "WiFiDirectLock");
    }

    public static final P2pCamsManager getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new P2pCamsManager(context);
        }
        return INSTANCE;
    }

    /**
     * Initiates the discovery on the pre-constructed instance of the P2pCamsObserver
     */
    public void initDiscovery(boolean preferAutoConnect) {
        mP2pPreferences = new WifiDirectPreferences(mContext);
        if (!preferAutoConnect) {
            mP2pPreferences.autoConnect = false;
        }
        Log.d(TAG, "initiating Discovery.");
        if (mP2pObserver == null)
            mP2pObserver = new P2pCamsObserver(mContext, mP2pManager, mP2pChannel, this);
        mP2pObserver.startDiscovery(mP2pPreferences);
    }

    /**
     * Submits the candidate to be connected on next discovery
     *
     * @param camDetails
     */
    public void connectTo(DeviceDetails camDetails) {
        mP2pLock.acquire();
        mP2pObserver.setConnectionCandidate(camDetails);
    }

    /**
     * Submits the candidate to be connected on next discovery
     *
     * @param camDetails
     */
    public void disconnectTo(DeviceDetails camDetails) {
        mP2pObserver.disconnect(camDetails);
    }


    /**
     * destroys the instance and attributes hold by the P2pCamsManager
     */
    public void destroy() {
        Log.d(TAG, "Destroying P2pCamsManager");
        try {
            if (mP2pLock != null && mP2pLock.isHeld())
                mP2pLock.release();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        destroyed = true;
        if (mHandlerThread != null)
            mHandlerThread.interrupt();
        mHandlerThread = null;
        if (mP2pObserver != null)
            mP2pObserver.destroy();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            mP2pChannel.close();
        if (isPeerConnected) {
            cancelConnection();
        }
        mP2pManager = null;
        mP2pChannel = null;
        mP2pObserver = null;
        INSTANCE = null;
    }

    private void cancelConnection() {
        if (mP2pManager != null && mP2pChannel != null) {
//            if (deviceInHand.status == WifiP2pDevice.CONNECTED)
            mP2pManager.removeGroup(mP2pChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d("removeGroup", "onSuccess");
                }

                @Override
                public void onFailure(int reason) {
                    Log.d("removeGroup", "onFailure, reason = " + String.valueOf(reason));
                }
            });
//            if (deviceInHand.status == WifiP2pDevice.INVITED)
            mP2pManager.cancelConnect(mP2pChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d("cancelConnect", "onSuccess");
                }

                @Override
                public void onFailure(int reason) {
                    Log.d("cancelConnect", "onFailure, reason = " + String.valueOf(reason));
                }
            });
        }
    }

    /**
     * This method is introduced due to the camera name is appended with last 6 characters of the mac address in case of a12 p2p mode
     *
     * @param cameraName
     * @return
     */
    public static boolean isItMyCamera(String cameraName) {
        if (mP2pPreferences == null) {
            Log.d(TAG, "Could not check camera name not initialized.");
            return false;
        }
//        String myCamName = mP2pPreferences.mDeviceName.trim();
//        if (!myCamName.isEmpty()) {
//            return cameraName.trim().contains(myCamName) || myCamName.trim().contains(cameraName);
//        }
//        else return false;
        return false;
    }

    private static String getRegexName(String deviceName) {
        StringBuilder builder = new StringBuilder();
        for (char ch : deviceName.toCharArray()
        ) {
            if (ch == '-')
                builder.append("\\-");
            else
                builder.append(ch);
        }
        return builder.toString().toLowerCase();
    }

    @Override
    public void observerDestroyed() {
        if (mP2pObserver != null) {
            mP2pObserver = null;
        }
    }

    @Override
    public void peerConnected() {
        Log.d(TAG, "Peer connected.");
        isPeerConnected = true;
    }

    @Override
    public void peerDisconnected() {
        Log.d(TAG, "Peer Disconnected.");
        isPeerConnected = false;
        if (mP2pObserver != null)
            mP2pObserver.destroy();
        mP2pObserver = null;
        Log.d(TAG, "re-initiating discovery");
        initDiscovery(mP2pPreferences.autoConnect);
    }

    /**
     * WifiDirectPreferences holds the Wifi-Direct specific user settings to be used for one INSTANCE of P2pCamsManager
     */
    protected class WifiDirectPreferences {
        private final PreferencesManager pref;
        public String mDeviceName;
        public boolean mSupportWPS;
        public String mWpsKey;
        public boolean autoConnect;

        public WifiDirectPreferences(Context context) {
            pref = new PreferencesManager(context);
            mDeviceName = pref.mDefaultPreferences.GetWifiDirectCameraName();
            mSupportWPS = pref.mDefaultPreferences.GetWifiDirectSupportsPin();
            mWpsKey = pref.mDefaultPreferences.GetWifiDirectCameraPin();
            autoConnect = pref.mDefaultPreferences.GetWifiDirectAutoConnect();
        }

        @Override
        public String toString() {
            return "mDeviceName =" + mDeviceName +
                    "\nmSupportWPS =" + mSupportWPS +
                    "\nmWpsKey =" + mWpsKey +
                    "\nautoConnect =" + autoConnect;
        }

        public void updatePinPref(String wpsKey) {
            pref.mDefaultPreferences.SetWifiDirectSupportsPin(true);
            pref.mDefaultPreferences.SetWifiDirectCameraPin(wpsKey);
            this.mWpsKey = wpsKey;
            this.mSupportWPS = true;
            sendWifiDirectBroadcast();
        }


        /**
         * Sends the broadcast to be receive by the preference Activity to update the preferences ui
         */
        private void sendWifiDirectBroadcast() {
            Intent i = new Intent(Constants.BROADCAST_PREF_CHANGE);
            mContext.sendBroadcast(i);
        }
    }
}
