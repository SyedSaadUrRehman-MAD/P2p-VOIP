package com.hawkxeye.online.comm

import android.os.Build
import android.telecom.*
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.M)
class CallConnectionService : ConnectionService(){
    companion object {
        var conn : CallConnection? = null
    }
}