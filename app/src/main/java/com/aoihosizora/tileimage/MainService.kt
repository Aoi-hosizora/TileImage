package com.aoihosizora.tileimage

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.support.constraint.ConstraintLayout
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.overlay.view.*

class MainService : TileService() {

    companion object {
        const val ACTION_CLOSE_TILE = "ACTION_CLOSE_TILE"
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.run {
                if (this == ACTION_CLOSE_TILE)
                    inactiveTile()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        inactiveTile()

        val filter = IntentFilter()
        filter.addAction(ACTION_CLOSE_TILE)
        registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        inactiveTile()
    }

    override fun onClick() {
        super.onClick()

        if (qsTile?.state == Tile.STATE_ACTIVE) {
            inactiveTile()
            stopService(Intent(this, OverlayService::class.java))
        }
        else {
            activeTile()
            startService(Intent(this, OverlayService::class.java))
        }
    }

    /**
     * 激活 启动服务
     */
    private fun activeTile() {
        qsTile?.run {
            label = "Running"
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }

    /**
     * 取消激活 停止服务
     */
    private fun inactiveTile() {
        qsTile?.run {
            label = "Start Tile"
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onStartListening() {
        // var isRun = false
        // val manager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        // manager?.run {
        //     val className = OverlayService::class.java.toString().substringBefore("$").substringAfter("class ")
        //     Toast.makeText(baseContext, className, Toast.LENGTH_SHORT).show()
        //     for (service in getRunningServices(Integer.MAX_VALUE)) {
        //         if (service?.service?.className == className) {
        //             Toast.makeText(baseContext, "${service.service.className}, 2", Toast.LENGTH_SHORT).show()
        //             isRun = true
        //             break
        //         }
        //     }
        //

        // val nullParent: ViewGroup? = null
        // val inflater = LayoutInflater.from(application)
        // val overlayLayout = inflater.inflate(R.layout.overlay, nullParent) as? ConstraintLayout

        // Toast.makeText(baseContext, "${overlayLayout?.windowToken == null}", Toast.LENGTH_SHORT).show()

        // if (overlayLayout?.windowToken == null)
        //     inactiveTile()
        // else
        //     activeTile()

        qsTile?.label = when (qsTile.state) {
            Tile.STATE_ACTIVE -> "Running"
            Tile.STATE_INACTIVE -> "Start Tile"
            else -> "Unavailable"
        }
        qsTile?.updateTile()

        super.onStartListening()
    }
}