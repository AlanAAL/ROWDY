package com.RowdyAvocado

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class BfliXPlugin : Plugin() {
    var activity: AppCompatActivity? = null

    override fun load(context: Context) {
        activity = context as AppCompatActivity
        registerMainAPI(BfliX(this))
    }
}
