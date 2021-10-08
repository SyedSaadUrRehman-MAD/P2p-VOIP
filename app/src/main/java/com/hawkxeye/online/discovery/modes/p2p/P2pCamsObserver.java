package com.hawkxeye.online.discovery.modes.p2p;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.InetAddresses;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.hawkxeye.online.discovery.modes.models.DeviceDetails;
import com.hawkxeye.online.utils.ReflectionUtils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;

import static com.hawkxeye.online.discovery.modes.p2p.P2pCamsManager.isItMyCamera;

public class P2pCamsObserver {
    private static final String TAG = P2pCamsObserver.class.getSimpleName();
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private Context mContext;
    private WifiP2pInfo connectedWifiP2pInfo;

    private boolean isDiscoveryInitiated;//to avoid unwanted discover calls
    private boolean peersRequested;//to avoid unwanted peers requests
    private boolean connectionInfoRequested;//to avoid duplicated info requests
    private P2pCamsManager.WifiDirectPreferences p2pPreference;
    private P2pCamsConnector p2pConnector = null;
    //    private boolean autoConnecting;
    private DeviceDetails connectibleCandidate;
    private ObserverCallback mObserverCallback;

    private Handler observerHandler = new Handler();
    private Runnable observerRunnable = new Runnable() {
        @Override
        public void run() {
            observerHandler.removeCallbacks(observerRunnable);
            initDiscovery(p2pPreference);
            observerHandler.postDelayed(observerRunnable, 120000);
        }
    };

    public P2pCamsObserver(Context context, WifiP2pManager mP2pManager, WifiP2pManager.Channel mP2pChannel, ObserverCallback callback) {
        Log.d(TAG, "constructing P2pCamsObserver");
        this.mContext = context;
        this.mManager = mP2pManager;
        this.mChannel = mP2pChannel;
        this.mObserverCallback = callback;

        //registering p2p actions receiver, these actions are to be triggered by Context.WIFI_P2P_SERVICE
        IntentFilter p2pIntentFilter = new IntentFilter();
        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mContext.registerReceiver(p2pBroadcastReceiver, p2pIntentFilter);
    }

    /**
     * Initiates the p2p peer's Discovery
     *
     * @param mP2pPreferences
     */
    private void initDiscovery(P2pCamsManager.WifiDirectPreferences mP2pPreferences) {
        this.p2pPreference = mP2pPreferences;
        Log.d(TAG, "Initating P2p Discovery, preferences =" + p2pPreference.toString());
        if (p2pConnector == null || !p2pConnector.isBusy() && !p2pConnector.isConnected()) {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    isDiscoveryInitiated = true;
                    Log.d(TAG, "discoverPeers:OnSuccess");
                }

                @Override
                public void onFailure(int reason) {
                    isDiscoveryInitiated = false;
                    Log.d(TAG, "discoverPeers:OnFailure, reason =" + String.valueOf(reason));

                }
            });
        } else if (!peersRequested) {
            Log.d(TAG, "Could not initiate Discovery as Connector is Busy. Simply requesting peers.");
            mManager.requestPeers(mChannel, peersListener);
            peersRequested = true;
        }
    }

    /**
     * connectionInfoListener's OnConnectionInfoAvailable is Triggered after calling the WifiP2pManager.requestConnectionInfo
     */
    private WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            Log.d(TAG, "wifiP2pInfo :" + new Gson().toJson(wifiP2pInfo));
            connectionInfoRequested = false;//as the request is entertained here
            Log.d(TAG, "wifiP2pInfo : Address = " + wifiP2pInfo.groupOwnerAddress);
            connectedWifiP2pInfo = wifiP2pInfo;
            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                //todo broadcast status as server
                Log.d(TAG, "This is server");
                if (mConnectionsCallback != null)
                    mConnectionsCallback.onConnectionSucceed(connectibleCandidate, "Server");
            } else if (wifiP2pInfo.groupFormed) {

                //todo broadcast status as server
                Log.e(TAG, "This is Client");
                if (mConnectionsCallback != null)
                    mConnectionsCallback.onConnectionSucceed(connectibleCandidate, "Client");
            }
        }
    };

    /**
     * peersListener's onPeersAvailable is called when the request peers is called on WifiP2pManager
     */
    private WifiP2pManager.PeerListListener peersListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peers) {
            Log.d(TAG, "peers Available" + peers.getDeviceList().toString());
            peersRequested = false;
            if (connectedWifiP2pInfo == null && (p2pConnector == null || !p2pConnector.isBusy())) {
                if (connectibleCandidate != null) {
                    findAndConnectTo(connectibleCandidate, peers.getDeviceList());
                } else
                    Log.d(TAG, "no candidate for connection");
            }
            if (connectibleCandidate == null) {
                for (WifiP2pDevice device : peers.getDeviceList()) {
                    if (device.status == WifiP2pDevice.CONNECTED) {
                        Log.d(TAG, device.deviceName + " is found connected");
                        connectibleCandidate = getDeviceDetails(device);
                        p2pConnector = new P2pCamsConnector(mContext, connectibleCandidate, mManager, mChannel, mConnectionsCallback);
                    } else
                        Log.d(TAG, device.deviceName + " is not connected");
                }
            }

            filterAndPostPeers(peers.getDeviceList());
        }

        private void findAndConnectTo(DeviceDetails connectibleCandidate, Collection<WifiP2pDevice> deviceList) {
            Log.d(TAG, "Finding Connectible candidate in discovered List.");
            boolean found = false;
            for (WifiP2pDevice device : deviceList) {
                if (device.deviceName.equalsIgnoreCase(connectibleCandidate.Name)) {
                    Log.d(TAG, "Found Connectible Candidate in Discovery");
                    if (p2pConnector == null)
                        p2pConnector = new P2pCamsConnector(mContext, connectibleCandidate, mManager, mChannel, mConnectionsCallback);
                    found = true;
                    break;
                }
            }

            if (!found) {
                Log.d(TAG, "Connectible Candidate not found, Reinitiating Discovery.");
                setConnectionCandidate(connectibleCandidate);
            }
        }

        private boolean isAnyCamConnectedOrInvited(Collection<WifiP2pDevice> deviceList) {
            boolean found = false;
            for (WifiP2pDevice deviceInHand : deviceList) {
                if (deviceInHand.status == WifiP2pDevice.CONNECTED || deviceInHand.status == WifiP2pDevice.INVITED) {
                    if (deviceInHand.status == WifiP2pDevice.CONNECTED && isItMyCamera(deviceInHand.deviceName)) {
                        if (mManager != null && mChannel != null) {
                            Log.d(TAG, "Found Default camera pre-connected, requesting connection info");
                            connectibleCandidate = getDeviceDetails(deviceInHand);
                            p2pConnector = new P2pCamsConnector(mContext, connectibleCandidate, null, null, mConnectionsCallback);
                            postAwaitingConnection(connectibleCandidate);
                            mManager.requestConnectionInfo(mChannel, connectionInfoListener);
                        }
                        break;
                    } else
                        found = true;
                    break;
                }
            }
            return found;
        }

        private void filterAndPostPeers(Collection<WifiP2pDevice> deviceList) {
            ArrayList<DeviceDetails> filteredList = new ArrayList<>();
            for (WifiP2pDevice device : deviceList) {
                filteredList.add(getDeviceDetails(device));
            }

            Intent availableDevicesIntent = new Intent();
            availableDevicesIntent.setAction(P2pCamsManager.ACTION_P2P_DEVICES_AVAILABLE);
            availableDevicesIntent.putParcelableArrayListExtra(P2pCamsManager.EXTRA_P2P_DEVICE_LIST, filteredList);
            mContext.sendBroadcast(availableDevicesIntent);
        }
    };

    @NonNull
    private DeviceDetails getDeviceDetails(WifiP2pDevice deviceInHand) {
        DeviceDetails camdetails = new DeviceDetails();
        camdetails.IP_Address = "";
        camdetails.MAC_Address = deviceInHand.deviceAddress;
        camdetails.Name = deviceInHand.deviceName;
        camdetails.Original_Name = deviceInHand.deviceName;
        camdetails.IsConnected = deviceInHand.status == WifiP2pDevice.CONNECTED;
        camdetails.status = deviceInHand.status;
        if (!deviceInHand.wpsPbcSupported())//a12 is not pbc supported
        {
            camdetails.wps = WpsInfo.KEYPAD;
        } else {
            camdetails.wps = -1;
        }
        return camdetails;
    }

    private void cancelConnection() {
        connectedWifiP2pInfo = null;
        connectibleCandidate = null;
        if (mManager != null && mChannel != null) {
            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d("removeGroup", "onSuccess");
                    initDiscovery(p2pPreference);
                }

                @Override
                public void onFailure(int reason) {
                    Log.d("removeGroup", "onFailure, reason = " + String.valueOf(reason));
                    initDiscovery(p2pPreference);
                }
            });
            mManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d("cancelConnect", "onSuccess");
                    initDiscovery(p2pPreference);
                }

                @Override
                public void onFailure(int reason) {
                    Log.d("cancelConnect", "onFailure, reason = " + String.valueOf(reason));
                    initDiscovery(p2pPreference);
                }
            });
        }
    }

    /**
     * p2pBroadcastReceiver receives the broadcasts triggered by WifiP2pService which is a System's Service
     */
    private BroadcastReceiver p2pBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Action Received  = " + action);
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                Log.d(TAG, "WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION");
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {
                    Log.d(TAG, "WifiP2pManager.WIFI_P2P_STATE_DISABLED");
                } else {
                    Log.d(TAG, "WifiP2pManager.WIFI_P2P_STATE_ENABLED");
//                    postConnectionFailed(connectibleCandidate);
                }
                if (mManager != null && !isDiscoveryInitiated) {
                    Log.d(TAG, "Requesting peers");
                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mManager.requestPeers(mChannel, peersListener);
                    peersRequested = true;
                }
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                Log.d(TAG, "WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION");
                if (mManager != null && isDiscoveryInitiated) {
                    Log.d(TAG, "Requesting peers");
                    mManager.requestPeers(mChannel, peersListener);
                    peersRequested = true;
                }
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                Log.d(TAG, "WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION");
                connectionInfoRequested = false;
                if (mManager == null) {
                    Log.d(TAG, "P2p manager is null!");
                    return;
                }
                isDiscoveryInitiated = false;
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                Log.d(TAG, "networkInfo =" + networkInfo.toString());
                if (networkInfo.isConnected()) {
                    // we are connected with the other device, request connection info to find group owner IP
                    if (!connectionInfoRequested) {
                        connectionInfoRequested = true;
                        mManager.requestConnectionInfo(mChannel, connectionInfoListener);
                        if (p2pConnector != null && mConnectionsCallback != null)
                            mConnectionsCallback.onConnectionInfoRequested(p2pConnector.deviceToConnect);
                    } else
                        Log.d(TAG, "Connection info already requested");
                } else {
                    Log.d(TAG, "network connected =" + networkInfo.isConnected() + ", connectedWifiP2pinfo =" + connectedWifiP2pInfo);
                    destroyConnector();
                }
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                Log.d(TAG, "WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
//                WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
//                if (!device.deviceName.contains("Tel:"))
//                    changeMyP2pName(device.deviceName);
            }
        }
    };

    /**
     * P2pCamsConnector.P2pConnectionCallback receives the callbacks from the P2pCamsConnector
     */
    private P2pCamsConnector.P2pConnectionCallback mConnectionsCallback = new P2pCamsConnector.P2pConnectionCallback() {
        @Override
        public void onConnectionTriggered(DeviceDetails camDetail) {
            Log.d("mP2pConnectionsCallback", "onConnectionTriggered for => " + camDetail.toString());
            postAwaitingConnection(camDetail);
        }

        @Override
        public void onConnectionInfoRequested(DeviceDetails camDetail) {
            Log.d("mP2pConnectionsCallback", "onConnectionInfoRequested for => " + camDetail.toString());
        }

        @Override
        public void onConnectionInitiationSucceed(DeviceDetails camDetail) {
            Log.d("mP2pConnectionsCallback", "onConnectionInitiationSucceed for => " + camDetail.toString());
            postAwaitingConnection(camDetail);
            if (camDetail.wps == -1) {
                connectionInfoRequested = true;
                mManager.requestConnectionInfo(mChannel, connectionInfoListener);
            }
        }

        @Override
        public void onConnectionInitiationFailed(DeviceDetails camDetail) {
            Log.d("mP2pConnectionsCallback", "onConnectionInitiationFailed for => " + camDetail.toString());
            postConnectionFailed(camDetail);
            destroyConnector();

        }

        @Override
        public void onConnectionCancelled(DeviceDetails camDetail) {
            Log.d("mP2pConnectionsCallback", "onConnectionCancelled for => " + camDetail.toString());
            postConnectionFailed(camDetail);
            destroyConnector();
        }

        @Override
        public void onConnectibleDeviceUpdated(DeviceDetails camDetail) {
            Log.d("mP2pConnectionsCallback", "onConnectibleDeviceUpdated for => " + camDetail.toString());
            if (isItMyCamera(camDetail.Name))
                p2pPreference.updatePinPref(camDetail.wpsKey);
            setConnectionCandidate(camDetail);
        }

        @Override
        public void onConnectionSucceed(DeviceDetails camDetail, String designation) {
            Log.d("mP2pConnectionsCallback", "onConnectionSucceed for => " + camDetail.toString());
            if (p2pConnector != null)
                p2pConnector.setConnected();
//            camDetail.IP_Address = connectedWifiP2pInfo.groupOwnerAddress.;
            Intent successIntent = new Intent();
            successIntent.putExtra(P2pCamsManager.EXTRA_P2P_DEVICE, camDetail);
            if (connectedWifiP2pInfo != null) {
                successIntent.putExtra(P2pCamsManager.EXTRA_P2P_DEVICE_DESIGNATION, designation);
                successIntent.putExtra(P2pCamsManager.EXTRA_P2P_GROUP_OWNER_ADDR, connectedWifiP2pInfo.groupOwnerAddress);
            }
            successIntent.setAction(P2pCamsManager.ACTION_P2P_DEVICE_CONNECTED);
            if (mContext != null) {
                Log.d("mP2pConnectionsCallback", "sending connection success broadcast. ");
                mContext.sendBroadcast(successIntent);
            } else {
                Log.d(TAG, "Unable to send Successful Connection broadcast.");
            }
        }

    };

    /**
     * Sends Connection Failed Broadcast
     *
     * @param camDetail
     */
    protected void postConnectionFailed(DeviceDetails camDetail) {
        Intent failedIntent = new Intent();
        failedIntent.putExtra(P2pCamsManager.EXTRA_P2P_DEVICE, camDetail);
        failedIntent.setAction(P2pCamsManager.ACTION_P2P_DEVICE_CONNECTION_FAILED);
        if (mContext != null) {
            Log.d("mP2pConnectionsCallback", "sending connection failed broadcast. ");
            mContext.sendBroadcast(failedIntent);
        } else {
            Log.d(TAG, "Unable to send connection failed broadcast.");
        }
    }

    /**
     * Sends Awaiting Connection broadcast
     *
     * @param camDetail
     */
    private void postAwaitingConnection(DeviceDetails camDetail) {
        Intent failedIntent = new Intent();
        failedIntent.putExtra(P2pCamsManager.EXTRA_P2P_DEVICE, camDetail);
        failedIntent.setAction(P2pCamsManager.ACTION_P2P_DEVICE_AWAITING_CONNECTION);
        if (mContext != null) {
            Log.d("mP2pConnectionsCallback", "sending awaiting connection broadcast. ");
            mContext.sendBroadcast(failedIntent);
        } else {
            Log.d(TAG, "Unable to send awaiting connection broadcast.");
        }
    }

    /**
     * Destroys the connector if not null
     */
    private void destroyConnector() {
        Log.d(TAG, "Destroying Connector");
        if (p2pConnector != null) {
            if (p2pConnector.isConnected() || p2pConnector.isAwaitingConnection()) {
                cancelConnection();
            }
            p2pConnector.destroy();
            p2pConnector = null;
        }
    }

    /**
     * Destroys the observer
     */
    public void destroy() {
        Log.d(TAG, "Destroying");
        connectionInfoRequested = false;
        isDiscoveryInitiated = false;
        peersRequested = false;
        try {
            mContext.unregisterReceiver(p2pBroadcastReceiver);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        cancelConnection();
        if (mManager != null)
            mManager.stopPeerDiscovery(mChannel, null);
        peersListener = null;
        if (mObserverCallback != null) {
            mObserverCallback.observerDestroyed();
            mObserverCallback = null;
        }
    }

    /**
     * Sets the connectibleCandidate that will be connected on intiating new discovery
     *
     * @param camDetails
     */
    public void setConnectionCandidate(final DeviceDetails camDetails) {
        Log.d(TAG, "setting connectible candidate =" + camDetails.toString());
        destroyConnector();
        postAwaitingConnection(camDetails);
        this.connectibleCandidate = camDetails;
        initDiscovery(p2pPreference);
    }

    /**
     * Updates the Framework Channel
     *
     * @param mP2pChannel
     */
    public void updateChannel(WifiP2pManager.Channel mP2pChannel) {
        Log.d(TAG, "updating Channel");
        this.mChannel = mP2pChannel;
        isDiscoveryInitiated = peersRequested = false;
        if (p2pConnector == null || !p2pConnector.isConnected())
            initDiscovery(p2pPreference);
    }

    public void startDiscovery(P2pCamsManager.WifiDirectPreferences mP2pPreferences) {
        this.p2pPreference = mP2pPreferences;
        observerHandler.removeCallbacks(observerRunnable);
        observerHandler.postDelayed(observerRunnable, 0);
    }

    public void changeMyP2pName(String myNewDeviceName) {

        //
        //  Setup for using the reflection API to actually call the methods we want
        //
        if (myNewDeviceName.contains("Tel:"))
            return;
        int numberOfParams = 3;
        Class[] methodParameters = new Class[numberOfParams];
        methodParameters[0] = WifiP2pManager.Channel.class;
        methodParameters[1] = String.class;
        methodParameters[2] = WifiP2pManager.ActionListener.class;

        Object arglist[] = new Object[numberOfParams];
        arglist[0] = mChannel;
        arglist[1] = myNewDeviceName;
        String finalMyNewDeviceName = "Tel:" + myNewDeviceName.split(" ")[0].split("'")[0];

        arglist[2] = new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                String resultString = "Changed to " + finalMyNewDeviceName;
                Log.e("SECRETAPI", resultString);
//                Toast.makeText(mContext, resultString, Toast.LENGTH_LONG).show();
            }

            public void onFailure(int reason) {
                String resultString = "Fail reason: " + String.valueOf(reason);
                Log.e("SECRETAPI", resultString);
//                Toast.makeText(mContext, resultString, Toast.LENGTH_LONG).show();
            }
        };

        //
        //  Now we use the reflection API to call a method we normally wouldnt have access to.
        //

        ReflectionUtils.executePrivateMethod(mManager, WifiP2pManager.class, "setDeviceName", methodParameters, arglist);

    }

    public void disconnect(DeviceDetails camDetails) {
        cancelConnection();
    }

    interface ObserverCallback {
        void observerDestroyed();

        void peerConnected();

        void peerDisconnected();
    }
}
