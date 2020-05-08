package com.aoihosizora.tileimage

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.Toast
import io.reactivex.disposables.CompositeDisposable
import rx_activity_result2.RxActivityResult

class HelperActivity : Activity() {

    companion object {

        // Helper Type
        const val HELPER_TYPE_IMAGE = "HELPER_TYPE_IMAGE"
        const val HELPER_TYPE_OTHER = "HELPER_TYPE_OTHER"

        // Intent Extra
        const val EXTRA_IMAGE_URL = "IMAGE_URL"

        // Request Code
        const val REQUEST_PERMISSION_CODE = 0
    }

    private val disposables = CompositeDisposable()

    private val permissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, permissions[0]) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, permissions[1]) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CODE)
        } else {
            when (intent?.action) {
                HELPER_TYPE_IMAGE -> requestImage()
                HELPER_TYPE_OTHER -> {
                    Toast.makeText(applicationContext, "HELPER_TYPE_OTHER", Toast.LENGTH_SHORT).show()
                    finish()
                }
                else -> {
                    Toast.makeText(applicationContext, "Not support", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
                Toast.makeText(applicationContext, "授权失败", Toast.LENGTH_SHORT).show()
            else
                onCreate(null)
        }
    }

    /**
     * 请求相册
     */
    private fun requestImage() {
        val imageIntent = Intent()
        imageIntent.action = Intent.ACTION_PICK
        imageIntent.type = "image/*"
        imageIntent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val dis = RxActivityResult.on(this).startIntent(imageIntent)
            .map { it.data() }
            .subscribe({
                it?.data?.run {
                    val intent = Intent()
                    intent.action = OverlayService.BROADCAST_ACTION_IMAGE
                    intent.`package` = packageName
                    intent.putExtra(EXTRA_IMAGE_URL, this)
                    sendBroadcast(intent)
                }
            }, { finish() }, { finish() })

        disposables.add(dis)
    }

    override fun onDestroy() {
        disposables.dispose()
        super.onDestroy()
    }
}
