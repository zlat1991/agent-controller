package com.zlat1991.agentcontroller

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper

class ScreenCaptureEngine(
    private val projection: MediaProjection,
    private val width: Int,
    private val height: Int,
    private val density: Int
) {

    private val imageReader =
        ImageReader.newInstance(
            width,
            height,
            PixelFormat.RGBA_8888,
            2
        )

    private var virtualDisplay: VirtualDisplay? = null

    private val handler =
        Handler(Looper.getMainLooper())

    fun start() {

        virtualDisplay =
            projection.createVirtualDisplay(
                "AgentControllerScreen",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                null,
                handler
            )
    }

    fun capture(): Bitmap? {

        val image =
            imageReader.acquireLatestImage()
                ?: return null

        try {

            val plane =
                image.planes[0]

            val buffer =
                plane.buffer

            val pixelStride =
                plane.pixelStride

            val rowStride =
                plane.rowStride

            val rowPadding =
                rowStride -
                    pixelStride * width

            val bitmapWidth =
                width +
                    rowPadding / pixelStride

            val bitmap =
                Bitmap.createBitmap(
                    bitmapWidth,
                    height,
                    Bitmap.Config.ARGB_8888
                )

            bitmap.copyPixelsFromBuffer(buffer)

            return Bitmap.createBitmap(
                bitmap,
                0,
                0,
                width,
                height
            )

        } finally {

            image.close()
        }
    }

    fun stop() {

        virtualDisplay?.release()
        virtualDisplay = null

        imageReader.close()

        projection.stop()
    }
}
