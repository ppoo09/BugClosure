package com.keyolla.bugclosure

import android.app.Application



class Application : Application() {

    override fun onCreate() {
        super.onCreate()

        BugClosure.init(applicationContext)
            .setLogPrint(true)
            .setSaveLogFile(true)
            .setLogFileInformation("fileLog", "TEMPFILE",  2)
    }
}