package com.timome.eggyhub

import android.app.Application
import com.timome.eggyhub.util.CrashHandler

class EggyHubApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashHandler.init(this)
    }
}