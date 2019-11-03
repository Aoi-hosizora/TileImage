package com.aoihosizora.tileimage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val serviceIntent = Intent(context, MainService::class.java)
        context?.startService(serviceIntent)
    }
}