package com.hawkxeye.online.ui.main.fragments

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.hawkxeye.online.R
import com.hawkxeye.online.discovery.ConnDiscoveryService
import com.hawkxeye.online.discovery.modes.models.DeviceDetails
import com.hawkxeye.online.ui.main.uihelpers.DevicesListAdapter
import com.hawkxeye.online.ui.main.videmodels.MainViewModel

/**
 * ScanDevicesFragment displaying the list of the devices scanned
 */
class ScanDevicesFragment : Fragment(),DevicesListAdapter.DeviceActionsListener,SwipeRefreshLayout.OnRefreshListener {

    private lateinit var devicesListView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private val TAG = "ScanDevicesFragment"
    private lateinit var mainViewModel: MainViewModel
    private var devicesList = ArrayList<DeviceDetails>();
    private lateinit var devicesAdapter:DevicesListAdapter
    var mBound = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.discovery_list, container, false)
        val textView: TextView = root.findViewById(R.id.section_label)
        mainViewModel.text.observe(viewLifecycleOwner, Observer<String> {
            textView.text = it
        })
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
        swipeRefresh.setOnRefreshListener(this)
        devicesListView = view.findViewById<RecyclerView>(R.id.devicesList)
        devicesListView = view.findViewById<RecyclerView>(R.id.devicesList)
        devicesListView.setLayoutManager(LinearLayoutManager(activity))
        devicesAdapter = DevicesListAdapter(activity, devicesList,this)
        devicesListView.adapter = devicesAdapter

        mainViewModel.devicesList.observe(viewLifecycleOwner, Observer {
            devicesList.clear()
            devicesList.addAll(it)
            devicesAdapter.notifyDataSetChanged()
        })

        mainViewModel.awaiting.observe(viewLifecycleOwner, Observer {
            swipeRefresh.isRefreshing = it
            if(it)
            {
                progressBar.visibility = View.VISIBLE
            }else
                progressBar.visibility = View.INVISIBLE
        })
        activity?.let { bindWifiService(it) }
    }

    private fun bindWifiService(context: Context) {
        if (!mBound) {
            val serviceIntent = Intent(
                context,
                ConnDiscoveryService::class.java
            )

            context.bindService(serviceIntent, mGenericConnection, Context.BIND_AUTO_CREATE)
            mBound = true
        }
    }

    private val mGenericConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            Log.d(TAG, "Service connected.")
            val binder: ConnDiscoveryService.OnlineDiscoveryBinder =
                service as ConnDiscoveryService.OnlineDiscoveryBinder
            mainViewModel.setService(binder.getService())
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d(
                TAG,
                "Service disconnected generic connection."
            )
        }
    }

    private fun unBindWifiService() {
        if (mBound) {
            activity?.unbindService(mGenericConnection)
            mBound = false
        }
//        OnlineApp.StopObserverService(activity)
    }

    override fun onDestroy() {
        unBindWifiService()
        super.onDestroy()
    }
    companion object {
        private const val ARG_SECTION_NUMBER = "section_number"

        @JvmStatic
        fun newInstance(sectionNumber: Int): ScanDevicesFragment {
            return ScanDevicesFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }

    override fun onConnect(position: Int) {
        mainViewModel.connectDeviceAtIndex(position);
    }

    override fun onDisconnect(position: Int) {
        mainViewModel.disconnectDeviceAtIndex(position);
    }

    override fun onRefresh() {
        mainViewModel.triggerPeerDiscovery()
    }
}