package com.hawkxeye.online.utils

sealed class Constants {
    companion object {
        const val BROADCAST_PREF_CHANGE  = "com.hawkxeye.online.broadcast_pref_change"
        const val RECORDING_FILE = "/sdcard/mviewRecording/recording.dat"
        const val RECORDING_DIR = "/sdcard/mviewRecording/"
        const val ONPHONE_RECORDING_DIR = "/sdcard/mviewRecording/OnPhoneCam/"

        const val MESSAGE_READ =1
        const val SHOW_LOADER = 601
        const val HIDE_LOADER = 602
        const val UPDATE_DEVICES = 603
        const val SESSION_STATUS = 604
        const val CONNECTING = 605
        const val CONNECTED = 606
        const val DISCONNECTED = 607
    }
}
