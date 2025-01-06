package com.keyolla.bugclosure.demo

import android.app.Application
import com.keyolla.bugclosure.BugClosure


class Application : Application() {

    override fun onCreate() {
        super.onCreate()
        BugClosure.init(this)
            .setLogPrint(true)
            .setSaveLogFile(true)
            .setLogFileInformation("fileLog", "logFile", 2)
    }
}