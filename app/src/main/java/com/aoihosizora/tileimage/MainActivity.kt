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
            val intent =
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQ_PERMISSION)
        } else
            startService()
    }

    private fun startService() {
        val intent = Intent(this, AppTileService::class.java)
        startService(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQ_PERMISSION -> {
                startService()
            }
        }
    }

    override fun onDestroy() {
        // var isRun = false
        // val manager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        // manager?.run {
        //     val className =
        //         OverlayService::class.java.toString().substringBefore("$").substringAfter("class ")
        //     for (service in getRunningServices(Integer.MAX_VALUE)) {
        //         if (service?.service?.className == className) {
        //             isRun = true
        //             break
        //         }
        //     }
        // }

        // if (!isRun) {
        //     val intent = Intent(this@MainActivity, AppTileService::class.java)
        //     intent.action = AppTileService.ACTION_INACTIVE_TILE
        //     intent.`package` = packageName
        //     sendBroadcast(intent)
        // }

        super.onDestroy()
    }
}