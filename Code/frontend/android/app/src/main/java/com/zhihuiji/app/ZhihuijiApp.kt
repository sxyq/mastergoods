package com.zhihuiji.app

import android.app.Application
import com.zhihuiji.data.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ZhihuijiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Immediate sync waits for the authenticated session in AppNavGraph. Keep the
        // periodic safety net registered at process start without racing that worker.
        SyncScheduler.enqueuePeriodicSync(this)
    }
}
