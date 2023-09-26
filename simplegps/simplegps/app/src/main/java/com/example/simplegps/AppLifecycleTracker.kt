package com.example.simplegps

import android.app.Activity
import android.app.Application
import android.os.Bundle

class AppLifecycleTracker(private val listener: Listener) : Application.ActivityLifecycleCallbacks {

    private var foregroundActivities = 0

    interface Listener {
        fun onAppForeground()
        fun onAppBackground()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (foregroundActivities == 0) {
            listener.onAppForeground()
        }
        foregroundActivities++
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        foregroundActivities--
        if (foregroundActivities == 0) {
            listener.onAppBackground()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
