package com.keyolla.bugclosure

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        BugClosure.i("Test Log Message 1")
        BugClosure.d("Test Log Message 2")


    }
}