package com.dermochelys.simpleradio

import android.app.Application
import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.os.StrictMode
import android.util.Log
import com.google.android.gms.cast.framework.CastContext

private const val TAG = "MainApplication"

class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        CastContext.getSharedInstance(this)

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build())

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "onConfigurationChanged $newConfig")
    }

    override fun registerActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks?) {
        super.registerActivityLifecycleCallbacks(callback)
        Log.d(TAG, "registerActivityLifecycleCallbacks $callback")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.d(TAG, "onLowMemory")
    }

    override fun unregisterOnProvideAssistDataListener(callback: OnProvideAssistDataListener?) {
        super.unregisterOnProvideAssistDataListener(callback)
        Log.d(TAG, "unregisterOnProvideAssistDataListener $callback")
    }

    override fun unregisterComponentCallbacks(callback: ComponentCallbacks?) {
        super.unregisterComponentCallbacks(callback)
        Log.d(TAG, "unregisterComponentCallbacks $callback")
    }

    override fun unregisterActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks?) {
        super.unregisterActivityLifecycleCallbacks(callback)
        Log.d(TAG, "unregisterActivityLifecycleCallbacks $callback")
    }

    override fun registerComponentCallbacks(callback: ComponentCallbacks?) {
        super.registerComponentCallbacks(callback)
        Log.d(TAG, "registerComponentCallbacks $callback")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.d(TAG, "onTrimMemory $level")
    }

    override fun registerOnProvideAssistDataListener(callback: OnProvideAssistDataListener?) {
        super.registerOnProvideAssistDataListener(callback)
        Log.d(TAG, "registerOnProvideAssistDataListener $callback")
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "onTerminate")
    }
}
