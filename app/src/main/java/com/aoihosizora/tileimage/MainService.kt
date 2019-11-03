package com.aoihosizora.tileimage

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class MainService : TileService() {

    override fun onCreate() {
        super.onCreate()
        Toast.makeText(applicationContext, "Create Tile Service", Toast.LENGTH_SHORT).show()
        qsTile?.state = Tile.STATE_INACTIVE
        onStartListening()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        Toast.makeText(applicationContext, "Added Tile", Toast.LENGTH_SHORT).show()
        qsTile?.state = Tile.STATE_INACTIVE
        onStartListening()
    }

    override fun onClick() {
        super.onClick()
        Toast.makeText(applicationContext, "Click Tile", Toast.LENGTH_SHORT).show()

        if (qsTile?.state == Tile.STATE_ACTIVE) {
            qsTile.label = "Start Tile"
            qsTile.state = Tile.STATE_INACTIVE
            stopService(Intent(this, OverlayService::class.java))
        } else {
            qsTile.label = "Running"
            qsTile.state = Tile.STATE_ACTIVE
            startService(Intent(this, OverlayService::class.java))
        }
        qsTile?.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()

        qsTile?.label = when (qsTile.state) {
            Tile.STATE_ACTIVE -> "Running"
            Tile.STATE_INACTIVE -> "Start Tile"
            else -> "Unavailable"
        }
        qsTile?.updateTile()
    }

    override fun onDestroy() {
        Toast.makeText(applicationContext, "Destroy Tile Service", Toast.LENGTH_SHORT).show()
        super.onDestroy()
    }
}