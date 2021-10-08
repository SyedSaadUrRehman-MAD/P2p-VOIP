package com.hawkxeye.online.ui.main.videmodels

import android.os.Handler
import android.os.Message
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.hawkxeye.online.discovery.ConnDiscoveryService
import com.hawkxeye.online.discovery.enums.SessionStatus
import com.hawkxeye.online.discovery.modes.models.DeviceDetails
import com.hawkxeye.online.utils.Constants

class MainViewModel : ViewModel() {
    val TAG = "MainViewModel"
    private val _index = MutableLiveData<Int>()
    val text: LiveData<String> = Transformations.map(_index) {
        "Hello world from section: $it"
    }

    fun setIndex(index: Int) {
        _index.value = index
    }

    private val _deviceList = MutableLiveData<List<DeviceDetails>>()
    val devicesList: LiveData<List<DeviceDetails>> = _deviceList

    private val _discoveryService = MutableLiveData<ConnDiscoveryService>()
    fun setService(service: ConnDiscoveryService) {
        service.boundHandler = m_Handler
        _discoveryService.value = service
    }

    fun connectDeviceAtIndex(position: Int) {
        _discoveryService.value?.connectTo(position)
    }

    fun disconnectDeviceAtIndex(position: Int) {
        _discoveryService.value?.disconnectTo(position)
    }

    fun triggerPeerDiscovery() {
        if (_discoveryService.value != null) {
            _discoveryService.value!!.startWifiPeerDiscovery();
        }
    }

    private val _sessionStatus = MutableLiveData<SessionStatus>()
    val sessionStatus: LiveData<SessionStatus> = _sessionStatus

    private val _awaiting = MutableLiveData<Boolean>()
    val awaiting: LiveData<Boolean> = Transformations.map(_discoveryService)
    {
        it.IsSessionInprogress()
    }

    private val _connectedDevice = MutableLiveData<DeviceDetails>()
    val connectedDevice: LiveData<DeviceDetails> = Transformations.map(_discoveryService) {
        it.connectedDeviceDetails
    }

    private val m_Handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                Constants.UPDATE_DEVICES -> {
                    Log.d(
                        TAG,
                        " Update camera list " + msg.obj
                    )
                    if (msg.obj != null)
                        _deviceList.value = msg.obj as List<DeviceDetails>?

                    _awaiting.value = false
                }
                Constants.HIDE_LOADER -> {
                    Log.d(TAG, "show loader")
                    if (msg.obj != null)
                        _awaiting.value = false
                }
                Constants.SHOW_LOADER -> {
                    Log.d(TAG, "show loader")
                    if (msg.obj != null)
                        _awaiting.value = true
                }
                Constants.CONNECTING -> {
                    Log.d(TAG, "connecting")
                    if (msg.obj != null)
                        _awaiting.value = true
                }
                Constants.CONNECTED -> {
                    Log.d(TAG, "connected")
                    if (msg.obj != null)
                        _connectedDevice.value = msg.obj as DeviceDetails?
                    _awaiting.value = false
                }
                Constants.CONNECTED -> {
                    Log.d(TAG, "connected")
                    if (msg.obj != null)
                        _connectedDevice.value = msg.obj as DeviceDetails?
                }
            }
        }
    }

}