package com.dermochelys.simpleradio

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.cast.framework.Session
import com.google.android.gms.cast.framework.media.MediaIntentReceiver

class MediaIntentReceiver: MediaIntentReceiver() {
    override fun onReceiveActionRewind(p0: Session?,
                                       p1: Long) {
        super.onReceiveActionRewind(p0, p1)
        Log.d(CAST_TAG, "onReceiveActionRewind: $p0, $p1")
    }


    override fun onReceiveOtherAction(p0: Context?,
                                      p1: String?,
                                      p2: Intent?) {
        super.onReceiveOtherAction(p0, p1, p2)
        Log.d(CAST_TAG, "onReceiveOtherAction: $p0, $p1, ${p2?.extras}")
    }

    override fun onReceiveActionTogglePlayback(p0: Session?) {
        super.onReceiveActionTogglePlayback(p0)
        Log.d(CAST_TAG, "onReceiveActionTogglePlayback: $p0")
    }

    override fun onReceiveActionSkipNext(p0: Session?) {
        super.onReceiveActionSkipNext(p0)
        Log.d(CAST_TAG, "onReceiveActionTogglePlayback: $p0")
    }

    override fun onReceive(p0: Context?,
                           p1: Intent?) {
        super.onReceive(p0, p1)
        Log.d(CAST_TAG, "onReceive: $p0, ${p1?.extras}")
    }

    override fun onReceiveActionSkipPrev(p0: Session?) {
        super.onReceiveActionSkipPrev(p0)
        Log.d(CAST_TAG, "onReceiveActionSkipPrev: $p0")
    }

    override fun onReceiveActionMediaButton(p0: Session?,
                                            p1: Intent?) {
        super.onReceiveActionMediaButton(p0, p1)
        Log.d(CAST_TAG, "onReceiveActionMediaButton: $p0, $p1")
    }

    override fun onReceiveActionForward(p0: Session?,
                                        p1: Long) {
        super.onReceiveActionForward(p0, p1)
        Log.d(CAST_TAG, "onReceiveActionForward: $p0, $p1")
    }
}