package com.hawkxeye.online.discovery.modes.models;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Parcel;
import android.os.Parcelable;

public class DeviceDetails implements Parcelable {
    public String Name = "";
    public String MAC_Address = "";
    public String IP_Address = "";
    public String Version = "";
    public String Original_Name = "";
    public int Type = -1;
    public boolean IsDefault = false;
    public boolean IsConnected = false;
    public int wps = -1;
    public String wpsKey = "";
    public boolean isAPCam;
    public String SSID;
    public int status = WifiP2pDevice.AVAILABLE;

    public DeviceDetails() {

    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(Name);
        out.writeString(MAC_Address);
        out.writeString(IP_Address);
        out.writeString(Version);
        out.writeString(Original_Name);
        out.writeInt(Type);
        out.writeInt(IsDefault ? 1 : 0);
        out.writeInt(IsConnected ? 1 : 0);
        out.writeInt(wps);
        out.writeInt(isAPCam ? 1 : 0);
        out.writeString(SSID);
        out.writeString(wpsKey);
        out.writeInt(status);
    }

    public static final Parcelable.Creator<DeviceDetails> CREATOR
            = new Parcelable.Creator<DeviceDetails>() {
        public DeviceDetails createFromParcel(Parcel in) {
            return new DeviceDetails(in);
        }

        public DeviceDetails[] newArray(int size) {
            return new DeviceDetails[size];
        }
    };

    private DeviceDetails(Parcel in) {
        Name = in.readString();
        MAC_Address = in.readString();
        IP_Address = in.readString();
        Version = in.readString();
        Original_Name = in.readString();
        Type = in.readInt();
        IsDefault = in.readInt() == 1;
        IsConnected = in.readInt() == 1;
        wps = in.readInt();
        isAPCam = in.readInt() == 1;
        SSID = in.readString();
        wpsKey = in.readString();
        status = in.readInt();
    }

    public boolean matches(DeviceDetails sub) {
        return this.IP_Address.equalsIgnoreCase(sub.IP_Address) &&
                this.MAC_Address.equalsIgnoreCase(sub.MAC_Address);

    }

    public String getStatus() {
        switch (status) {
            case WifiP2pDevice.AVAILABLE:
                return "Online";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.INVITED:
                return "Calling..";
            default:
                return "Available";
        }

    }

    public String getDisplayableName() {
        if (!Name.isEmpty() && wps != -1) {
            if (Name.toLowerCase().matches("^.+[-][a-fA-F0-9]{6}$")) // to match suffix
            {
                return Name.substring(0, Name.lastIndexOf("-"));
            }
        }
        return Name;

    }
}