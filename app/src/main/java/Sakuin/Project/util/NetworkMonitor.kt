package Sakuin.Project.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Whether the device has a network with internet access; only the receipt scan needs it. */
class NetworkMonitor(context: Context) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val online = MutableStateFlow(currentlyOnline())
    val isOnline: StateFlow<Boolean> = online.asStateFlow()

    init {
        // Registered once for the whole process; the monitor is only created when a scan starts.
        connectivity.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    online.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }

                override fun onLost(network: Network) {
                    online.value = false
                }
            },
        )
    }

    /** Re-reads the current state, for an explicit "Try again". */
    fun refresh() {
        online.value = currentlyOnline()
    }

    private fun currentlyOnline(): Boolean {
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
