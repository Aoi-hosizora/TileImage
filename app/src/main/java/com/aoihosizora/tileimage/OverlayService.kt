package com.aoihosizora.tileimage

import android.app.AlertDialog
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.support.constraint.ConstraintLayout
import android.view.*
import kotlinx.android.synthetic.main.overlay.view.*

class OverlayService : Service() {

    private val windowManager by lazy {
        application.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val overlayLayout by lazy {
        val inflater = LayoutInflater.from(application)
        inflater.inflate(R.layout.overlay, null) as ConstraintLayout
    }

    private val overlayWindowType =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

    private var relationX = 0f
    private var relationY = 0f

    override fun onCreate() {
        super.onCreate()

        // https://www.jianshu.com/p/ac63c57d2555

        val params = WindowManager.LayoutParams()
        params.run {
            type = overlayWindowType
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            format = PixelFormat.RGBA_8888
            gravity = Gravity.START or Gravity.TOP
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            windowAnimations = R.style.WindowAnimation
        }

        windowManager.addView(overlayLayout, params)

        overlayLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val resourceId: Int = resources.getIdentifier("status_bar_height","dimen","android")
        val statusBarHeight =
            if (resourceId > 0)
                resources.getDimensionPixelSize(resourceId)
            else -1

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
                    windowManager.updateViewLayout(overlayLayout, params)
                }
                MotionEvent.ACTION_UP -> {
                    relationX = 0f
                    relationY = 0f
                }
            }
            true
        } }

        overlayLayout.close_btn.setOnClickListener {
            val alertDialog = AlertDialog.Builder(applicationContext)
                .setTitle("提醒")
                .setMessage("是否关闭？")
                .setPositiveButton("关闭") { _, _ -> stopSelf() }
                .setNegativeButton("取消", null)
                .create()
            alertDialog.window?.setType(overlayWindowType)
            alertDialog.show()
        }
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onDestroy() {
        windowManager.removeView(overlayLayout.image)
        windowManager.removeView(overlayLayout.close_btn)
        super.onDestroy()
    }
}