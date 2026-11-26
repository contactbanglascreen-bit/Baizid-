package com.example

import android.app.Application

/** Starts Firebase (push notifications) before any screen or background message needs it. */
class CinedorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.example.ui.components.CinedorPush.start(this)
    }
}
