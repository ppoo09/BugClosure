package com.keyolla.bugclosure.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.keyolla.bugclosure.BugClosure


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        BugClosure.i("Test Log Message 1")
        BugClosure.d("Test Log Message 2")


    }
}