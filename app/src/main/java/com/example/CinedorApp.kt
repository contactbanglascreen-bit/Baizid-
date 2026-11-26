package com.example

import android.app.Application
import kotlinx.coroutines.launch

/** Starts Firebase (push notifications) before any screen or background message needs it. */
class CinedorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.example.ui.components.CinedorPush.start(this)
        // Country / phone stats for the admin, and "your problem is fixed" notices.
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO).launch {
            com.example.ui.components.CinedorCloud.reportDevice(this@CinedorApp)
            runCatching { com.example.ui.components.CinedorCloud.checkFixed(this@CinedorApp) }
            runCatching { com.example.ui.components.CinedorCloud.refreshInboxBadge(this@CinedorApp) }
        }
    }
}
