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

fun Context.isCharging(): Boolean {
    val battery = registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
    val status = battery?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
        ?: return true
    return status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
        status == android.os.BatteryManager.BATTERY_STATUS_FULL
}

fun Context.mobileDataBytes(): Long {
    val uid = android.os.Process.myUid()
    val received = android.net.TrafficStats.getUidRxBytes(uid)
    val sent = android.net.TrafficStats.getUidTxBytes(uid)
    if (received < 0 && sent < 0) return 0
    return received.coerceAtLeast(0) + sent.coerceAtLeast(0)
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
