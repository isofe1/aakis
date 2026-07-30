package com.example.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.data.UserPreferences

@RequiresApi(Build.VERSION_CODES.N)
class BubbleTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val context = applicationContext
        val userPrefs = UserPreferences(context)

        // Ensure Overlay Permission is granted
        if (!Settings.canDrawOverlays(context)) {
            Toast.makeText(
                context,
                "الرجاء منح إذن الظهور فوق التطبيقات أولاً من إعدادات التطبيق",
                Toast.LENGTH_LONG
            ).show()

            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    startActivityAndCollapse(pendingIntent)
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        val isRunning = userPrefs.isFloatingServiceEnabled
        if (isRunning) {
            userPrefs.isFloatingServiceEnabled = false
            FloatingBubbleService.stopService(context)
            Toast.makeText(context, "تم إيقاف الفقاعة العائمة", Toast.LENGTH_SHORT).show()
        } else {
            userPrefs.isFloatingServiceEnabled = true
            FloatingBubbleService.startService(context)
            Toast.makeText(context, "تم تشغيل الفقاعة العائمة", Toast.LENGTH_SHORT).show()
        }

        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val userPrefs = UserPreferences(applicationContext)
        val hasOverlay = Settings.canDrawOverlays(applicationContext)

        tile.label = "فقاعة عاكس"

        if (!hasOverlay) {
            tile.state = Tile.STATE_UNAVAILABLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "بحاجة لإذن"
            }
        } else if (userPrefs.isFloatingServiceEnabled) {
            tile.state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "مفعلة"
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "معطلة"
            }
        }
        tile.updateTile()
    }
}
