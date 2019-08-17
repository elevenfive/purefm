package com.dermochelys.simpleradio

import android.content.Context
import com.google.android.gms.cast.framework.media.NotificationAction
import com.google.android.gms.cast.framework.media.NotificationActionsProvider

class NotificationActionsProvider(context: Context): NotificationActionsProvider(context) {
    override fun getNotificationActions(): MutableList<NotificationAction> {
        return mutableListOf()
    }

    override fun getCompactViewActionIndices(): IntArray {
        return intArrayOf()
    }
}
