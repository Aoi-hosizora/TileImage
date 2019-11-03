package com.aoihosizora.tileimage

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQ_PERMISSION = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQ_PERMISSION)
        } else
            startService()
    }

    private fun startService() {
        val intent = Intent(this, MainService::class.java)
        startService(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQ_PERMISSION -> {
                startService()
            }
        }
    }
}
