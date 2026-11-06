package com.example.lovecounter.utils

import android.util.Log
import com.example.lovecounter.BuildConfig

object Logger {
    private const val TAG = "LoveCounterApp"

    fun d(message: String, tag: String = TAG) {
        if (BuildConfig.build_debug) {
            Log.d(tag, message)
        }
    }

    fun e(message: String, tag: String = TAG, throwable: Throwable? = null) {
        if (BuildConfig.build_debug) {
            Log.e(tag, message, throwable)
        }
    }

    fun i(message: String, tag: String = TAG) {
        if (BuildConfig.build_debug) {
            Log.i(tag, message)
        }
    }
}
