package com.hawkxeye.online.ui.main.uihelpers;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hawkxeye.online.R;
import com.hawkxeye.online.discovery.modes.models.DeviceDetails;

import java.util.List;

public class DevicesListAdapter extends RecyclerView.Adapter<DevicesListAdapter.DeviceViewHolder> {
    private static final String TAG = "DevicesListAdapter";
    Context m_context;
    List<DeviceDetails> devices;
    private DeviceActionsListener _listener;

    public DevicesListAdapter(Context context, List<DeviceDetails> deviceList, DeviceActionsListener listener) {
        super();
        m_context = context;
        devices = deviceList;
        this._listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater m_LayoutInflater = (LayoutInflater) m_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = m_LayoutInflater.inflate(R.layout.discovered_item, null);
        DeviceViewHolder vh = new DeviceViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder deviceViewHolder, int pos) {
        if (pos < devices.size()) {
            try {
                final int position = pos;
                final DeviceDetails wificam = devices.get(position);
                deviceViewHolder.displayName.setText(wificam.getDisplayableName());
                deviceViewHolder.deviceStatus.setText(wificam.getStatus());
                deviceViewHolder.connectedButton.setVisibility(wificam.IsConnected ? View.VISIBLE : View.INVISIBLE);
                deviceViewHolder.disconnectButton.setVisibility((wificam.IsConnected) ? View.VISIBLE : View.INVISIBLE);

                Log.d(TAG, "getView Called! Camera Index " + position + " Camera Name " + wificam.Name);

                deviceViewHolder.displayName.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
//                            if (BuildConfig.FLAVOR.contains("nswpf"))
//                                startConnectionToCamera(position);
//                            else
//                                showCameraDetails(position);
                    }
                });

                deviceViewHolder.connectButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        _listener.onConnect(pos);
//                            startConnectionToCamera(position);
                    }
                });

                deviceViewHolder.fileButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
//                            startQueryFileList(getItem(position));
                    }
                });
                deviceViewHolder.disconnectButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        _listener.onDisconnect(pos);
//                            disconnectCamera();
                    }
                });
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, " Exception in GetView Error : " + e.getMessage() + " Stack Trace " + e.getStackTrace());
            }
        }
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    //class for caching the views in a row
    protected class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView displayName;
        TextView deviceStatus;
        ImageButton connectButton;
        ImageButton fileButton;
        ImageButton connectedButton;
        ImageButton disconnectButton;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            displayName = (TextView) itemView.findViewById(R.id.DeviceDisplayName);
            deviceStatus = (TextView) itemView.findViewById(R.id.deviceStatus);
            connectButton = (ImageButton) itemView.findViewById(R.id.LiveCamButton);
            fileButton = (ImageButton) itemView.findViewById(R.id.FileListButton);
            connectedButton = (ImageButton) itemView.findViewById(R.id.ConnectedButton);
            disconnectButton = (ImageButton) itemView.findViewById(R.id.disconnectButton);
        }
    }

    public interface DeviceActionsListener {
        void onConnect(int position);

        void onDisconnect(int position);
    }
}