package com.aoihosizora.tileimage

import android.app.AlertDialog
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.ColorUtils
import android.support.v7.widget.CardView
import android.util.DisplayMetrics
import android.view.*
import android.widget.ImageButton
import kotlinx.android.synthetic.main.overlay.view.*

class OverlayService : Service() {

    companion object {
        private const val DEF_SIZE: Int = 300
        const val BROADCAST_ACTION_IMAGE = "com.aoihosizora.tileImage.ACTION_IMAGE"
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.run {
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
        (application as? MyApplication)?.let { it.state = true }
    }

    override fun onDestroy() {
        (application as? MyApplication)?.let { it.state = false }
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
        overlayLayout.close_btn.setOnClickListener { onExit() }
        // image_btn
        overlayLayout.image_btn.setOnClickListener { onChoose() }
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
        intent.action = HelperActivity.HELPER_TYPE_IMAGE
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // <<<
        startActivity(intent)
    }

    /**
     * 相册广播返回
     */
    @Suppress("DEPRECATION")
    private fun onReturnImageUri(intent: Intent) {
        val url = intent.getParcelableExtra<Uri>(HelperActivity.EXTRA_IMAGE_URL)
        url?.let {
            // content://media/external/images/media/388342
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)

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
            setIcon(overlayLayout.zoom_btn, R.drawable.ic_zoom_white_24dp, R.drawable.ic_zoom_dark_24dp, bitmap.getPixel(bitmap.width - 1, bitmap.height - 1))
            setIcon(overlayLayout.close_btn, R.drawable.ic_close_white_24dp, R.drawable.ic_close_dark_24dp, bitmap.getPixel(0, 0))
            setIcon(overlayLayout.image_btn, R.drawable.ic_photo_white_24dp, R.drawable.ic_photo_dark_24dp, bitmap.getPixel(bitmap.width - 1, 0))
        }
    }

    private fun setIcon(btn: ImageButton, lightId: Int, darkId: Int, pixel: Int) {
        fun isLightColor(color: Int): Boolean {
            return ColorUtils.calculateLuminance(color) >= 0.5
        }

        btn.setImageDrawable(ContextCompat.getDrawable(applicationContext, lightId))
        if (isLightColor(pixel)) {
            btn.setImageDrawable(ContextCompat.getDrawable(applicationContext, darkId))
        }
    }
}
