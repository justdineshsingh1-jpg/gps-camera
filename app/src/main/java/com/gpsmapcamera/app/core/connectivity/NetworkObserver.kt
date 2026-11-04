package com.gpsmapcamera.app.core.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkObserver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun observe(): Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                trySend(NetworkStatus.ONLINE)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(NetworkStatus.OFFLINE)
            }

            override fun onUnavailable() {
                super.onUnavailable()
                trySend(NetworkStatus.OFFLINE)
            }
        }

        // Send current immediate status
        val currentCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val hasInternet = currentCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                currentCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        trySend(if (hasInternet) NetworkStatus.ONLINE else NetworkStatus.OFFLINE)

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}
