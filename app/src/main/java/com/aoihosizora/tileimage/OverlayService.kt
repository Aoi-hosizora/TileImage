package com.aoihosizora.tileimage

import android.app.AlertDialog
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.support.constraint.ConstraintLayout
import android.util.DisplayMetrics
import android.view.*
import kotlinx.android.synthetic.main.overlay.view.*

class OverlayService : Service() {

    companion object {
        private const val DEF_SIZE: Int = 300
    }

    private val windowManager by lazy {
        application.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val overlayLayout by lazy {
        val nullParent: ViewGroup? = null
        val inflater = LayoutInflater.from(application)
        inflater.inflate(R.layout.overlay, nullParent) as ConstraintLayout
    }

    private val overlayWindowType =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

    private var relationX = 0f
    private var relationY = 0f

    private var motoHeight = 0
    private var motoWidth = 0

    override fun onCreate() {
        super.onCreate()

        // https://www.jianshu.com/p/ac63c57d2555

        val params = WindowManager.LayoutParams()
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

        // overlayLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val resourceId: Int = resources.getIdentifier("status_bar_height","dimen","android")
        val statusBarHeight =
            if (resourceId > 0)
                resources.getDimensionPixelSize(resourceId)
            else -1

        val outMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(outMetrics)
        outMetrics.heightPixels -= statusBarHeight

        // Touch movable

        // https://blog.csdn.net/vicwudi/article/details/82084965
        overlayLayout.image.setOnTouchListener { _, motionEvent -> run {
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
        } }

        overlayLayout.close_btn.setOnClickListener {
            val alertDialog = AlertDialog.Builder(applicationContext)
                .setTitle("提醒")
                .setMessage("是否关闭本弹窗？")
                .setPositiveButton("关闭") { _, _ -> run {
                    // sendBroadcast()
                    stopSelf()
                } }
                .setNegativeButton("取消", null)
                .create()
            alertDialog.window?.setType(overlayWindowType)
            alertDialog.show()
        }

        overlayLayout.zoom_btn.setOnTouchListener { _, motionEvent -> run {
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    // moto size
                    motoWidth = params.width
                    motoHeight = params.height
                }
                MotionEvent.ACTION_MOVE -> {

                    // newSize / motoSize
                    val rate = kotlin.math.hypot(motionEvent.rawX - params.x,  motionEvent.rawY - params.y) /
                            kotlin.math.hypot(motoWidth.toDouble(), motoHeight.toDouble())

                    // new size
                    val newWidth = motoWidth * rate
                    val newHeight = motoHeight * rate

                    if (params.x + newWidth <= outMetrics.widthPixels // right border
                        && params.y + newHeight <= outMetrics.heightPixels // bottom border
                        && newWidth >= overlayLayout.minWidth
                        && newHeight >= overlayLayout.minHeight) {

                        params.width = newWidth.toInt()
                        params.height = newHeight.toInt()
                        windowManager.updateViewLayout(overlayLayout, params)
                    }
                }
            }
            true
        } }
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onDestroy() {
        sendBroadcast()
        if (overlayLayout.image != null)
            windowManager.removeView(overlayLayout)

        super.onDestroy()
    }

    /**
     * 发送广播 取消激活 Tile
     */
    private fun sendBroadcast() {
        val intent = Intent()
        intent.action = AppTileService.ACTION_INACTIVE_TILE
        intent.`package` = packageName
        sendBroadcast(intent)
    }
}