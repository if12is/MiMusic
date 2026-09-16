package it.vfsfitvnm.vimusic.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.getSystemService

fun Context.isOnUnmeteredNetwork(): Boolean {
    val manager = getSystemService<ConnectivityManager>() ?: return true
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    } else {
        @Suppress("DEPRECATION")
        manager.activeNetworkInfo?.type == ConnectivityManager.TYPE_WIFI
    }
}

fun Context.hasNetwork(): Boolean {
    val manager = getSystemService<ConnectivityManager>() ?: return true
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } else {
        @Suppress("DEPRECATION")
        manager.activeNetworkInfo?.isConnected == true
    }
}
