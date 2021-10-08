package com.hawkxeye.online.discovery;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.hawkxeye.online.BuildConfig;
import com.hawkxeye.online.comm.ClientClass;
import com.hawkxeye.online.comm.SendReceive;
import com.hawkxeye.online.comm.ServerClass;
import com.hawkxeye.online.utils.Constants;
import com.hawkxeye.online.OnlineApp;
import com.hawkxeye.online.discovery.enums.CAMERA_CONNECTION_MODE;
import com.hawkxeye.online.discovery.enums.SessionStatus;
import com.hawkxeye.online.discovery.modes.models.DeviceDetails;
import com.hawkxeye.online.discovery.modes.p2p.P2pCamsManager;
import com.hawkxeye.online.notifications.OnlineNotificationManager;
import com.hawkxeye.online.preferences.PreferencesManager;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import kotlinx.coroutines.Dispatchers;

import static com.hawkxeye.online.discovery.modes.p2p.P2pCamsManager.ACTION_P2P_DEVICES_AVAILABLE;
import static com.hawkxeye.online.discovery.modes.p2p.P2pCamsManager.ACTION_P2P_DEVICE_AWAITING_CONNECTION;
import static com.hawkxeye.online.discovery.modes.p2p.P2pCamsManager.ACTION_P2P_DEVICE_CONNECTED;
import static com.hawkxeye.online.discovery.modes.p2p.P2pCamsManager.ACTION_P2P_DEVICE_CONNECTION_FAILED;
import static com.hawkxeye.online.discovery.modes.p2p.P2pCamsManager.ACTION_P2P_DEVICE_DISCONNECTING;
import static com.hawkxeye.online.discovery.modes.p2p.P2pCamsManager.ACTION_P2P_DEVICE_DISCONNECT_COMPLETED;
import static kotlinx.coroutines.CoroutineScopeKt.CoroutineScope;

public class ConnDiscoveryService extends Service {
    static final String TAG = ConnDiscoveryService.class.getSimpleName();
    private static final String INTENT_ACTION_SERVICE_STARTED = "action_discovery_service_started";
    public static final String INTENT_ACTION_CLEANUP_OBSERVER_SESSION = "action_cleanup_observer_session";
    public static final String INTENT_ACTION_DISCOVER_PEERS = "action_discover_peers";
    public static final String INTENT_EXTRA_SHOULD_DESTROY = "action_stop";

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    private final IBinder mBinder = new OnlineDiscoveryBinder();
    private Handler mBoundHandler;
    private List<DeviceDetails> currentDeviceList;
    private boolean shouldDestroy;

    public DeviceDetails getConnectedDeviceDetails() {
        return connectedDeviceDetails;
    }

    private DeviceDetails connectedDeviceDetails;
    private String currentConnectionType = "";//server/client
    private InetAddress groupOwnerAddress;
    private Context mServiceContext;
    private PreferencesManager pref;
    private OnlineNotificationManager mNotificationManager;
    private SessionStatus sessionStatus = SessionStatus.Ready;
    private CAMERA_CONNECTION_MODE mCameraConnectionMode;

    private ServerClass serverClass;
    private ClientClass clientClass;

    private Handler connHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_READ:
                    byte[] readBuff = (byte[]) msg.obj;
                    String tempMsg = new String(readBuff, 0, msg.arg1);
                    Log.d(TAG, "message received =" + tempMsg);
                    SendReceive sendReceive = SendReceive.getInstance();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), tempMsg, Toast.LENGTH_LONG).show();
                        }
                    });
                    break;
            }
            return true;
        }
    });

    public void connectTo(int position) {
        synchronized (currentDeviceList) {
            if (position < currentDeviceList.size())
                P2pCamsManager.getInstance(getApplicationContext()).connectTo(currentDeviceList.get(position));
        }
    }

    public void disconnectTo(int position) {
        synchronized (currentDeviceList) {
            if (connectedDeviceDetails != null && position < currentDeviceList.size())
                P2pCamsManager.getInstance(getApplicationContext()).disconnectTo(currentDeviceList.get(position));
        }
    }

    public class OnlineDiscoveryBinder extends Binder {
        public ConnDiscoveryService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ConnDiscoveryService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");

        if (connectedDeviceDetails == null && intent.getExtras() != null) {
            Bundle b = intent.getExtras();
            connectedDeviceDetails = new DeviceDetails();
            connectedDeviceDetails.IP_Address = b.getString("CamIPAddress");
            connectedDeviceDetails.MAC_Address = b.getString("CamMACAddress");
            connectedDeviceDetails.Name = b.getString("CamName");
            connectedDeviceDetails.Original_Name = b.getString("CamName");

//            initCameraConnector(connectedDeviceDetails.IP_Address,
//                    connectedDeviceDetails.MAC_Address,
//                    connectedDeviceDetails.getDisplayableName());
//            ReportCameraSessionInProgressStatus(true);
        } else {
            Log.d(TAG, "camera connector is not triggered.");
            Log.d(TAG, "connectedDeviceDetails =" + connectedDeviceDetails);
            Log.d(TAG, "intent.getExtras() =" + intent.getExtras());
        }
        return mBinder;
    }

    public Handler getBoundHandler() {
        return mBoundHandler;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        mBoundHandler = null;

        try {
            Intent unbindBroadcast = new Intent();
            unbindBroadcast.setAction(OnlineApp.OBSERVER_SERVICE_UNBOUND_ACTION);
            sendBroadcast(unbindBroadcast);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        startWifiDirect();
        return super.onUnbind(intent);
    }

    public void setBoundHandler(Handler handler) {
        this.mBoundHandler = handler;
        if (handler == null) return;
        updateDeviceList(currentDeviceList);

        //bind handler and update states
        if (IsSessionInprogress()) {
            if (connectedDeviceDetails != null)
                showLoaderOnBoundActivity(connectedDeviceDetails);
            else
                Log.d(TAG, "connected device details are null");
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        mServiceContext = getApplicationContext();
        pref = new PreferencesManager(mServiceContext);
        mNotificationManager = OnlineNotificationManager.getInstance(getApplicationContext());
        startWifiDirect();
//        mIsConnectionModeWifiDirect = pref.mDefaultPreferences.GetWifiDirect();
//        mIsConnectionModeCameraAsAccessPoint = pref.mDefaultPreferences.GetIsCameraAsAccessPoint();
//
//        boolean skipTether = pref.mDefaultPreferences.GetSkipTether();
//        boolean manualTether = pref.mDefaultPreferences.GetManualTether();

//        if (mIsConnectionModeWifiDirect) {
//            mCameraConnectionMode = WIFI_DIRECT;
//        } else if (skipTether) {
//            mCameraConnectionMode = MviewConstant.CAMERA_CONNECTION_MODE.WIFI;
//        } else if (manualTether) {
//            mCameraConnectionMode = MviewConstant.CAMERA_CONNECTION_MODE.AP_MANUAL;
//        } else {
//            mCameraConnectionMode = AP;
//        }
//        registerReceiver(foregroundingReceiver, new IntentFilter(Intent_Action_Device_Session_Established));
//        registerReceiver(sessionDisconnectReceiver, new IntentFilter(Intent_Action_Destroyed));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        //sending service started braodcast
        shouldDestroy = false;
        if (intent != null && intent.getAction() != null) {
            takeAction(intent);
        }
        sendServiceStarted();
        if (BuildConfig.DEBUG)
            Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        if (!OnlineApp.mIsOnForeground)
            startForegroundCompat();
        if (sessionStatus == SessionStatus.Ready) {
            connectedDeviceDetails = null;
        }
        super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }

    private void takeAction(Intent intent) {
        switch (intent.getAction()) {
            case INTENT_ACTION_CLEANUP_OBSERVER_SESSION:
                Log.d(TAG, "INTENT_ACTION_CLEANUP_OBSERVER_SESSION");
                //clean up calling will trigger @link sessionDisconnectReceiver then the handshake will call-off
                if (intent != null && intent.getExtras() != null)
                    shouldDestroy = intent.getBooleanExtra(INTENT_EXTRA_SHOULD_DESTROY, false);

                abandonCamComplete();
                break;
            case INTENT_ACTION_DISCOVER_PEERS:
                Log.d(TAG, "INTENT_ACTION_DISCOVER_PEERS");
                startWifiPeerDiscovery();
                break;
        }
    }

    private void abandonCamComplete() {
        stopWifiDirect();
        if (shouldDestroy)
            stopSelf();
//        else
//            OnlineApp.RestartObserverService(getApplicationContext());
    }

    private void sendServiceStarted() {
        Intent serviceStartedIntent = new Intent();
        serviceStartedIntent.setAction(INTENT_ACTION_SERVICE_STARTED);
        sendBroadcast(serviceStartedIntent);
    }

    private void reset() {
        connectedDeviceDetails = null;
        sessionStatus = SessionStatus.Ready;
    }

    private void startWifiDirect() {
        Log.d(TAG, "Registering WiFiDirect discovery Receiver. ");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_P2P_DEVICES_AVAILABLE);
        intentFilter.addAction(ACTION_P2P_DEVICE_AWAITING_CONNECTION);
        intentFilter.addAction(ACTION_P2P_DEVICE_CONNECTED);
        intentFilter.addAction(ACTION_P2P_DEVICE_CONNECTION_FAILED);
        intentFilter.addAction(ACTION_P2P_DEVICE_DISCONNECTING);
        intentFilter.addAction(ACTION_P2P_DEVICE_DISCONNECT_COMPLETED);
//            intentFilter.addAction(INTENT_ACTION_P2P_DEVICE_OFFLINE);
        try {
            unregisterReceiver(mWifiP2pCamsStateReceiver);
        } catch (Exception e) {
            Log.d(TAG, "Receiver was not already registered");
        } finally {
            registerReceiver(mWifiP2pCamsStateReceiver, intentFilter);
        }
        startWifiPeerDiscovery();
    }

    /**
     * invokes the foregrounding
     */
    void startForegroundCompat() {
        if (null == mNotificationManager) return;
        Log.d(TAG, "Start Foregrounding DiscoveryService");
        startForeground(Integer.valueOf(mNotificationManager.MVIEW_NOTIFICATION_ID),
                mNotificationManager.getLiveNotification());
    }

    public boolean isSessionConnected() {
        return (sessionStatus == SessionStatus.Established);
    }

    public boolean IsSessionInprogress() {
        return (sessionStatus == SessionStatus.InProgress);
    }

    /**
     * prefAutoConnect should be false to make a difference otherwise it will use default(lastupdated) value of preference
     */
    public void startWifiPeerDiscovery() {
        if (!isSessionConnected())
            P2pCamsManager.getInstance(getApplicationContext()).initDiscovery(false);
    }

    private void stopWifiDirect() {
        Log.d(TAG, "Stopping Wifi-Direct");
        try {
            if (mWifiP2pCamsStateReceiver != null) {
                Log.d(TAG, "UnRegistering mWifiP2pCamsStateReceiver");
                unregisterReceiver(mWifiP2pCamsStateReceiver);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        } finally {
            Log.d(TAG, "Finally destroying P2pCamsManager");
            P2pCamsManager.getInstance(getApplicationContext()).destroy();
        }
    }

    private BroadcastReceiver mWifiP2pCamsStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentaction = intent.getAction();

            if (intentaction.equals(ACTION_P2P_DEVICES_AVAILABLE)) {
                List<DeviceDetails> devices = intent.getParcelableArrayListExtra(P2pCamsManager.EXTRA_P2P_DEVICE_LIST);
                updateDeviceList(devices);
            } else if (intentaction.equals(ACTION_P2P_DEVICE_AWAITING_CONNECTION)) {
                updateConnectingStatus();
            } else if (intentaction.equals(ACTION_P2P_DEVICE_CONNECTED)) {
                connectedDeviceDetails = intent.getParcelableExtra(P2pCamsManager.EXTRA_P2P_DEVICE);
                currentConnectionType = intent.getStringExtra(P2pCamsManager.EXTRA_P2P_DEVICE_DESIGNATION);
                groupOwnerAddress = (InetAddress) intent.getSerializableExtra(P2pCamsManager.EXTRA_P2P_GROUP_OWNER_ADDR);
                updateConnectedStatus(connectedDeviceDetails, currentConnectionType);
                if (currentConnectionType != null) {

                    if (clientClass != null) {
                        clientClass.interrupt();
                        clientClass =null;
                    }
                    if (serverClass != null) {
                        serverClass.interrupt();
                        serverClass = null;
                    }
                    if (currentConnectionType.toLowerCase().contains("server")) {
                        Log.d(TAG, "Create Server thread");
                        serverClass = new ServerClass(connHandler);
                        serverClass.start();
                    } else {
                        Log.d(TAG, "Create Client thread");
                        //todo start calling screen
                        clientClass = new ClientClass(groupOwnerAddress, connHandler);
                        clientClass.start();
                    }
                } else
                    Log.d(TAG, "conn type is null");
            } else if (intentaction.equals(ACTION_P2P_DEVICE_CONNECTION_FAILED) ||
                    intentaction.equals(ACTION_P2P_DEVICE_DISCONNECTING) ||
                    intentaction.equals(ACTION_P2P_DEVICE_DISCONNECT_COMPLETED)) {
//                mSessionHandler.removeCallbacks(mSessionTimeOut);
                reset();
                updateDisConnectedStatus(connectedDeviceDetails);

            }
//            else if (intentaction.equals(INTENT_ACTION_P2P_DEVICE_OFFLINE)) {
//                if (mBoundHandler == null) {
//                    abandonCamComplete();
//                    OnlineApp.RestartObserverService(getApplicationContext());
//                }
//            }

        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopWifiDirect();
        Toast.makeText(this, "stopping service", Toast.LENGTH_SHORT).show();
    }

    public void restartWifiDirectDiscovery() {
        stopWifiDirect();
        startWifiDirect();
    }

    public void showLoaderOnBoundActivity(DeviceDetails camDetails) {
        if (mBoundHandler != null) {
            Message msg = mBoundHandler.obtainMessage(Constants.SHOW_LOADER);
            msg.obj = camDetails;
            mBoundHandler.sendMessage(msg);
        }
    }

    public void updateDeviceList(List<DeviceDetails> deviceDetailsList) {
        currentDeviceList = deviceDetailsList;
        if (mBoundHandler != null) {
            Message msg = mBoundHandler.obtainMessage(Constants.UPDATE_DEVICES);
            msg.obj = deviceDetailsList;
            mBoundHandler.sendMessage(msg);
        }
    }

    public void updateStatus(SessionStatus status) {
        if (mBoundHandler != null) {
            Message msg = mBoundHandler.obtainMessage(Constants.SESSION_STATUS);
            msg.obj = status;
            mBoundHandler.sendMessage(msg);
        }
    }

    public void updateConnectingStatus() {
        if (mBoundHandler != null) {
            Message msg = mBoundHandler.obtainMessage(Constants.CONNECTING);
            mBoundHandler.sendMessage(msg);
        }
    }

    public void updateConnectedStatus(DeviceDetails deviceDetails, String currentConnectionType) {
        if (mBoundHandler != null) {
            Message msg = mBoundHandler.obtainMessage(Constants.CONNECTED);
            msg.obj = deviceDetails;
            if (currentConnectionType != null)
                msg.arg1 = currentConnectionType.toLowerCase().contains("server") ? 1 : 0;//0 is client 1 is server
            mBoundHandler.sendMessage(msg);
        }
    }

    public void updateDisConnectedStatus(DeviceDetails deviceDetails) {
        if (mBoundHandler != null) {
            Message msg = mBoundHandler.obtainMessage(Constants.DISCONNECTED);
            msg.obj = deviceDetails;
            mBoundHandler.sendMessage(msg);
        }
    }

    public void dismissLoaderOnBoundActivity() {
        if (mBoundHandler != null) {
            Message msg = mBoundHandler.obtainMessage(Constants.HIDE_LOADER);
            mBoundHandler.sendMessage(msg);
        }
    }
}