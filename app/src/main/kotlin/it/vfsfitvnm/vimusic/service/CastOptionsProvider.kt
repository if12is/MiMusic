package it.vfsfitvnm.vimusic.service

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

/**
 * Read only when the listener taps Cast. The manifest entry alone does not
 * start the Cast framework, so phones without Play services still open.
 */
class CastOptionsProvider : OptionsProvider {
    companion object {
        const val DEFAULT_RECEIVER_ID = "CC1AD845"
    }

    override fun getCastOptions(context: Context): CastOptions {
        return CastOptions.Builder()
            .setReceiverApplicationId(DEFAULT_RECEIVER_ID)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider> = emptyList()
}
