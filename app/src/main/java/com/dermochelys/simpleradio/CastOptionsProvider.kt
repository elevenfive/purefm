package com.dermochelys.simpleradio

import android.content.Context
import android.util.Log
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions

class CastOptionsProvider: OptionsProvider {
    override fun getCastOptions(p0: Context?): CastOptions {
        Log.d(CAST_TAG, "getCastOptions")

        val castMediaOptionsBuilder = CastMediaOptions.Builder()
        castMediaOptionsBuilder.setMediaIntentReceiverClassName("com.dermochelys.simpleradio.MediaIntentReceiver")
        castMediaOptionsBuilder.setExpandedControllerActivityClassName("com.dermochelys.simpleradio.MainActivity")
        val castMediaOptions = castMediaOptionsBuilder.build()

        val castOptionsBuilder = CastOptions.Builder()
        castOptionsBuilder.setResumeSavedSession(true)
        castOptionsBuilder.setEnableReconnectionService(true)
        castOptionsBuilder.setStopReceiverApplicationWhenEndingSession(true)
        castOptionsBuilder.setCastMediaOptions(castMediaOptions)

        p0?.getString(R.string.styled_receiver_app_id)?.let { castOptionsBuilder.setReceiverApplicationId(it) } ?:
            Log.e(CAST_TAG, "failed to setReceiverApplicationId")

        return castOptionsBuilder.build()
    }

    override fun getAdditionalSessionProviders(p0: Context?): MutableList<SessionProvider> {
        Log.d(CAST_TAG, "getAdditionalSessionProviders")

        return mutableListOf()
    }
}