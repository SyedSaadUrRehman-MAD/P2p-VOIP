package com.hawkxeye.online.ui.main.activities

import android.media.MediaRecorder
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.system.Os.socket
import androidx.viewpager.widget.ViewPager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.hawkxeye.online.OnlineApp
import com.hawkxeye.online.R
import com.hawkxeye.online.comm.SendReceive
import com.hawkxeye.online.ui.main.base.BasePermissibleActivity
import com.hawkxeye.online.ui.main.uihelpers.SectionsPagerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileDescriptor
import java.io.IOException
import java.lang.Exception


class MainActivity : BasePermissibleActivity() {

    private var recorder: MediaRecorder = MediaRecorder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OnlineApp.StartObserverService(applicationContext)
        setContentView(R.layout.activity_main)
//        OnlineApp.StartObserverService(this)
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)
        val fab: FloatingActionButton = findViewById(R.id.call)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Make a call to peer", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
            CoroutineScope(Dispatchers.IO).launch {
                var sendReceive = SendReceive.getInstance()
//                if (sendReceive != null) {
//                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
//                    recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
//                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
//                    recorder.setOutputFile(sendReceive.fileDescriptor)
//
//                    var start: Boolean = false
//                    try {
//                        recorder.prepare()
//                        start = true
//                    } catch (e: IllegalStateException) {
//                        e.printStackTrace()
//                        start = false
//                    } catch (e: IOException) {
//                        e.printStackTrace()
//                        start = false
//                    } finally {
//                        try {
//                            if (start)
//                                recorder.start()
//                        }catch (e:Exception)
//                        {
//                            e.printStackTrace()
//                        }finally {
//                            recorder = MediaRecorder()
//                        }
//                    }
//                }
                SendReceive.getInstance()?.write("new MEssage is sent".toByteArray())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

//        OnlineApp.StopObserverService(this)
    }
}