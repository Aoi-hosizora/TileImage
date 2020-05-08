package com.aoihosizora.tileimage

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.ColorUtils
import android.support.v7.widget.CardView
import android.util.DisplayMetrics
import android.view.*
import android.widget.Toast
import kotlinx.android.synthetic.main.overlay.view.*

class OverlayService : Service() {

    companion object {
        private const val DEF_SIZE: Int = 300
        const val BROADCAST_ACTION_IMAGE: String = "com.aoihosizora.tileImage.ACTION_IMAGE"
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.run {
                // Toast.makeText(applicationContext, "OverlayService: $this", Toast.LENGTH_SHORT).show()
                when (this) {
                    BROADCAST_ACTION_IMAGE -> onReturnImageUri(intent)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val filter = IntentFilter()
        filter.addAction(BROADCAST_ACTION_IMAGE)
        filter.addCategory(Intent.CATEGORY_DEFAULT)
        registerReceiver(receiver, filter)
        showOverlay()
    }

    override fun onDestroy() {
        sendBroadcast()
        if (overlayLayout.image != null) {
            windowManager.removeView(overlayLayout)
        }
        unregisterReceiver(receiver)

        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? = null

    private fun sendBroadcast() {
        val intent = Intent()
        intent.action = AppTileService.BROADCAST_ACTION_INACTIVE_TILE
        intent.`package` = packageName
        sendBroadcast(intent)
    }

    /**
     * WINDOW_SERVICE
     */
    private val windowManager by lazy {
        application.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    /**
     * params.type
     */
    @Suppress("DEPRECATION")
    private val overlayWindowType =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

    private val statusBarHeight by lazy {
        val resourceId: Int = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

    /**
     * 主布局: ConstraintLayout
     */
    private val overlayLayout by lazy {
        val nullParent: ViewGroup? = null
        val inflater = LayoutInflater.from(application)
        inflater.inflate(R.layout.overlay, nullParent) as CardView
    }

    /**
     * 主布局的 LayoutParams
     */
    private val params = WindowManager.LayoutParams()

    private var relationX = 0f
    private var relationY = 0f

    private var motoHeight = 0
    private var motoWidth = 0

    /**
     * 显示弹出窗口
     */
    private fun showOverlay() {
        params.run {
            type = overlayWindowType
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            format = PixelFormat.RGBA_8888
            gravity = Gravity.START or Gravity.TOP
            width = DEF_SIZE
            height = DEF_SIZE
            windowAnimations = R.style.WindowAnimation
        }
        windowManager.addView(overlayLayout, params)

        val outMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(outMetrics)
        outMetrics.heightPixels -= statusBarHeight

        // Touch movable
        overlayLayout.image.setOnTouchListener { _, motionEvent ->
            let {
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        relationX = motionEvent.x
                        relationY = motionEvent.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = (motionEvent.rawX - relationX).toInt()
                        params.y = (motionEvent.rawY - relationY - statusBarHeight).toInt()

                        // in screen
                        params.x = kotlin.math.min(kotlin.math.max(params.x, 0), outMetrics.widthPixels - params.width)
                        params.y = kotlin.math.min(kotlin.math.max(params.y, 0), outMetrics.heightPixels - params.height)
                        windowManager.updateViewLayout(overlayLayout, params)
                    }
                }
                true
            }
        }

        // zoom_btn
        overlayLayout.zoom_btn.setOnTouchListener { _, motionEvent ->
            run {
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // moto size
                        motoWidth = params.width
                        motoHeight = params.height
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // newSize / motoSize
                        val rate = kotlin.math.hypot(motionEvent.rawX - params.x, motionEvent.rawY - params.y) /
                            kotlin.math.hypot(motoWidth.toDouble(), motoHeight.toDouble())

                        // new size
                        val newWidth = motoWidth * rate
                        val newHeight = motoHeight * rate

                        if (params.x + newWidth <= outMetrics.widthPixels // right border
                            && params.y + newHeight <= outMetrics.heightPixels // bottom border
                            && newWidth >= overlayLayout.minimumWidth
                            && newHeight >= overlayLayout.minimumHeight
                        ) {
                            params.width = newWidth.toInt()
                            params.height = newHeight.toInt()
                            windowManager.updateViewLayout(overlayLayout, params)
                        }
                    }
                }
                true
            }
        }

        // close_btn
        overlayLayout.close_btn.setOnClickListener {
            onExit()
        }

        // image_btn
        overlayLayout.image_btn.setOnClickListener {
            onChoose()
        }
    }

    private fun onExit() {
        val alertDialog = AlertDialog.Builder(applicationContext)
            .setTitle("提醒")
            .setMessage("是否关闭本弹窗？")
            .setPositiveButton("关闭") { _, _ ->
                run {
                    sendBroadcast()
                    stopSelf()
                }
            }
            .setNegativeButton("取消", null)
            .create()
        alertDialog.window?.setType(overlayWindowType)
        alertDialog.show()
    }

    private fun onChoose() {
        val intent = Intent(this, HelperActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.action = HelperActivity.HELPER_TYPE_IMAGE

        val tagNotification = "NOTIFICATION_MESSAGE"
        val channelId = "1111"
        val notificationId = 1111
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val mBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("")
            .setContentText("")
            .setColor(getColor(R.color.colorPrimaryDark))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_VIBRATE)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel(channelId, "Activity Opening Notification", NotificationManager.IMPORTANCE_HIGH)
            mChannel.enableLights(true)
            mChannel.enableVibration(true)
            mChannel.description = "Activity opening notification"
            mNotificationManager?.createNotificationChannel(mChannel)
        }

        mNotificationManager?.notify(tagNotification, notificationId, mBuilder.build())

        // val intent = Intent(this, HelperActivity::class.java)
        // intent.action = HelperActivity.HELPER_TYPE_IMAGE
        // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // startActivity(intent)
    }

    /**
     * 相册广播返回
     */
    @Suppress("DEPRECATION")
    private fun onReturnImageUri(intent: Intent) {

        fun isLightColor(color: Int): Boolean {
            return ColorUtils.calculateLuminance(color) >= 0.5
        }

        intent.getParcelableExtra<Uri>(HelperActivity.EXTRA_IMAGE_URL)?.run {
            // content://media/external/images/media/388342

            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, this)
                val outMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(outMetrics)
                outMetrics.heightPixels -= statusBarHeight

                val heightRate = outMetrics.heightPixels.toDouble() / bitmap.height
                val widthRate = outMetrics.widthPixels.toDouble() / bitmap.width

                val rate: Double = if (heightRate >= 1 && widthRate >= 1) 1.0 else kotlin.math.min(heightRate, widthRate)

                params.height = (bitmap.height * rate).toInt()
                params.width = (bitmap.width * rate).toInt()
                windowManager.updateViewLayout(overlayLayout, params)

                overlayLayout.image.setImageBitmap(bitmap)

                overlayLayout.zoom_btn.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_zoom_white_24dp))
                overlayLayout.close_btn.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_close_white_24dp))
                overlayLayout.image_btn.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_photo_white_24dp))
                if (isLightColor(bitmap.getPixel(bitmap.width - 1, bitmap.height - 1))) {
                    overlayLayout.zoom_btn.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_zoom_dark_24dp))
                }
                if (isLightColor(bitmap.getPixel(0, 0))) {
                    overlayLayout.close_btn.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_close_dark_24dp))
                }
                if (isLightColor(bitmap.getPixel(bitmap.width - 1, 0))) {
                    overlayLayout.image_btn.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_photo_dark_24dp))
                }
            } catch (ex: Exception) {
                Toast.makeText(applicationContext, ex.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
