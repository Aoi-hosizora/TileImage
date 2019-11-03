package com.aoihosizora.tileimage

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class MainService : TileService() {

    override fun onClick() {
        super.onClick()
        Toast.makeText(applicationContext, "Start Tile Service", Toast.LENGTH_SHORT).show()

        if (qsTile.state == Tile.STATE_ACTIVE) {
            qsTile.label = "Start Tile"
            qsTile.state = Tile.STATE_INACTIVE
        } else {
            qsTile.label = "Running"
            qsTile.state = Tile.STATE_ACTIVE
        }
        qsTile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()

        qsTile.label = when (qsTile.state) {
            Tile.STATE_ACTIVE -> "Running"
            Tile.STATE_INACTIVE -> "Start Tile"
            else -> "Available"
        }
        qsTile.updateTile()
    }

    override fun onDestroy() {
        Toast.makeText(applicationContext, "Stop Tile Service", Toast.LENGTH_SHORT).show()
        super.onDestroy()
    }
}