package com.hawkxeye.online.discovery.modes.p2p;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.hawkxeye.online.discovery.modes.models.DeviceDetails;
import com.hawkxeye.online.preferences.PreferencesManager;
import com.hawkxeye.online.R;

public class P2pCamsConnector {
    private static final String TAG = P2pCamsConnector.class.getSimpleName();
    final DeviceDetails deviceToConnect;
    private Context mContext;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    private P2pConnectionCallback connectionCallback;
    private AlertDialog mValidatingDialog;
    private State mCurrentState = State.IDLE;

    public P2pCamsConnector(Context mContext, DeviceDetails deviceToConnect, WifiP2pManager manager, WifiP2pManager.Channel channel, P2pConnectionCallback callback) {
        Log.d(TAG, "Constructing Connector.");
        this.mContext = mContext;
        this.deviceToConnect = deviceToConnect;
        this.connectionCallback = callback;
        this.mManager = manager;
        this.mChannel = channel;
        if (mManager != null && mChannel != null && !deviceToConnect.IsConnected)
            validateAndConnect();
    }

    private void validateAndConnect() {
        Log.d(TAG, "validateAndConnect");
        if (deviceToConnect.wps != -1) {
            if (deviceToConnect.wpsKey.isEmpty()) {
                Log.d(TAG, "Device is not valid, Ask Pin.");
                validateCamDetails(deviceToConnect);
                return;
            }
        }
        connect();
    }

    private void connect() {
        if (deviceToConnect != null) {
            connectionCallback.onConnectionTriggered(deviceToConnect);
            connectToCamera(deviceToConnect);
        } else {
            Log.d(TAG, "device to Connect is null.");
        }
    }

    public void connectToCamera(final DeviceDetails camDetails) {
        Log.d(TAG, "connectToCamera => " + camDetails.toString());
        mCurrentState = State.INITIATING;
        try {
            final String deviceName = camDetails.getDisplayableName();
            final WifiP2pConfig config = new WifiP2pConfig();
            config.groupOwnerIntent = 0;
            config.deviceAddress = camDetails.MAC_Address;

            if (camDetails.wps != -1) {
                config.wps.setup = WpsInfo.KEYPAD;
                config.wps.pin = camDetails.wpsKey;
            }

            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    Log.d(TAG, "Connection initiation to " + deviceName + " successful");
                    mCurrentState = State.AWAITING;
                    connectionCallback.onConnectionInitiationSucceed(camDetails);
                }

                @SuppressLint("MissingPermission")
                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "Connection initiation Failed, reason =" + String.valueOf(reason));
                    mCurrentState = State.FAILED;
                    connectionCallback.onConnectionInitiationFailed(camDetails);
                }
            });

        } catch (Exception ex) {
            Log.d(TAG, "Exception happened :" + ex);
        }
    }

    public void validateCamDetails(final DeviceDetails camDetails) {
        if (mCurrentState == State.VALIDATING)
            return;
        mCurrentState = State.VALIDATING;
        acquireKeyGuard();
        EditText input = new EditText(mContext);
        input.getBackground().mutate().setColorFilter(mContext.getResources().getColor(R.color.dialog_bg), PorterDuff.Mode.SRC_ATOP);
        input.setText(new PreferencesManager(mContext).mDefaultPreferences.GetWifiDirectCameraPin());
        input.setTextColor(ContextCompat.getColor(mContext, R.color.mview_text_color));
        input.setShowSoftInputOnFocus(true);

        FrameLayout fl = new FrameLayout(mContext);
        fl.setPadding(25, 25, 25, 25);
        fl.addView(input);

        mValidatingDialog = new AlertDialog.Builder(mContext, R.style.DialogCustomThemeDark)
                .setTitle(String.format("%s Requires Pin to Connect", camDetails.getDisplayableName()))
                .setMessage("Please feed your pin below. Or update your Camera Settings.")
                .setView(fl)
                .setCancelable(false)
                .setPositiveButton("save", (dialog, which) ->
                        {
                            validateSaveAttempt(camDetails, input.getText().toString());
                        }
                )
                .setNegativeButton("cancel", (dialog, which) -> {
                    connectionCallback.onConnectionCancelled(camDetails);
                    mCurrentState = State.IDLE;
                })
                .create();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            mValidatingDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        else
            mValidatingDialog.getWindow().setType(
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        mValidatingDialog.show();


        mValidatingDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        mValidatingDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    mValidatingDialog.dismiss();
                    validateSaveAttempt(camDetails, input.getText().toString());
                    return true;
                }
                return false;
            }
        });
    }

    private void validateSaveAttempt(final DeviceDetails camDetails, String input) {
        mCurrentState = State.IDLE;
        camDetails.wpsKey = input;
        connectionCallback.onConnectibleDeviceUpdated(camDetails);
    }

    private void acquireKeyGuard() {
        KeyguardManager km = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock kl = km.newKeyguardLock("MyKeyguardLock");
        kl.disableKeyguard();

        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "MViewMobile:MyWakeLock");
        wakeLock.acquire();
    }

    public void destroy() {
        if (mValidatingDialog != null) {
            mValidatingDialog.dismiss();
            mValidatingDialog = null;
        }
    }

    public boolean isBusy() {
        return
                mCurrentState == State.INITIATING ||
                        mCurrentState == State.AWAITING ||
                        mCurrentState == State.VALIDATING;
    }

    public boolean isConnected() {
        return mCurrentState == State.CONNECTED;
    }

    public boolean isAwaitingConnection() {
        return mCurrentState == State.AWAITING;
    }

    public void setConnected() {
        mCurrentState = State.CONNECTED;
    }

    protected enum State {
        IDLE,
        VALIDATING,
        INITIATING,
        AWAITING,
        CONNECTED,
        FAILED,
    }

    /**
     * Callback to tell the observer to flush the connector
     */
    public interface P2pConnectionCallback {
        void onConnectionTriggered(DeviceDetails camDetail);

        void onConnectionInfoRequested(DeviceDetails camDetail);

        void onConnectionInitiationSucceed(DeviceDetails camDetail);

        void onConnectionInitiationFailed(DeviceDetails camDetail);

        void onConnectionCancelled(DeviceDetails camDetail);

        void onConnectibleDeviceUpdated(DeviceDetails camDetail);

        void onConnectionSucceed(DeviceDetails camDetail,String designation);
    }
}
