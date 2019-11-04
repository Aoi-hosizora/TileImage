package com.aoihosizora.tileimage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class AppTileService : TileService() {

    companion object {
        const val ACTION_INACTIVE_TILE = "ACTION_INACTIVE_TILE"
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            Toast.makeText(applicationContext, intent?.action, Toast.LENGTH_SHORT).show()
            intent?.action?.run {
                if (this == ACTION_INACTIVE_TILE)
                    inactiveTile()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        inactiveTile()

        val filter = IntentFilter()
        filter.addAction(ACTION_INACTIVE_TILE)
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

        if (qsTile?.state == Tile.STATE_INACTIVE) {
            activeTile()
            startService(Intent(this, OverlayService::class.java))
        }
        else {
            inactiveTile()
            stopService(Intent(this, OverlayService::class.java))
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
}