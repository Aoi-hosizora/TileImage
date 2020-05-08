package com.aoihosizora.tileimage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class AppTileService : TileService() {

    companion object {
        const val BROADCAST_ACTION_INACTIVE_TILE = "com.aoihosizora.tileImage.ACTION_INACTIVE_TILE"
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.run {
                // Toast.makeText(applicationContext, "AppTileService: $this", Toast.LENGTH_SHORT).show()
                when (this) {
                    BROADCAST_ACTION_INACTIVE_TILE -> inactiveTile()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        inactiveTile()

        val filter = IntentFilter()
        filter.addAction(BROADCAST_ACTION_INACTIVE_TILE)
        filter.addCategory(Intent.CATEGORY_DEFAULT)
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

    /**
     * 点击标签
     */
    override fun onClick() {
        super.onClick()
        if (qsTile?.state == Tile.STATE_ACTIVE) {
            inactiveTile()
            stopService(Intent(this, OverlayService::class.java))
        } else {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            } else {
                activeTile()
                startService(Intent(this, OverlayService::class.java))
            }
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