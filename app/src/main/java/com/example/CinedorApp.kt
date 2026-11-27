package com.example

import android.app.Application
import kotlinx.coroutines.launch

/** Starts Firebase (push notifications) before any screen or background message needs it. */
class CinedorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Remember crashes so the details reach the admin's Feedback list on the next start.
        com.example.ui.components.CrashReporter.install(this)
        com.example.data.remote.Server2Client.cacheDir = filesDir
        com.example.ui.components.CinedorPush.start(this)
        com.example.ui.components.PremiumState.load(this)
        // Country / phone stats for the admin, and "your problem is fixed" notices.
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching { com.example.ui.components.CrashReporter.sendPending(this@CinedorApp) }
            com.example.ui.components.CinedorCloud.reportDevice(this@CinedorApp)
            runCatching { com.example.ui.components.CinedorCloud.checkFixed(this@CinedorApp) }
            runCatching { com.example.ui.components.CinedorCloud.refreshInboxBadge(this@CinedorApp) }
            runCatching { com.example.ui.components.CinedorCloud.refreshPremium(this@CinedorApp) }
        }
    }
}
