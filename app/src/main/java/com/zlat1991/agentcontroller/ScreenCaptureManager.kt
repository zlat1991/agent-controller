package com.zlat1991.agentcontroller

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager

class ScreenCaptureManager(
    private val context: Context
) {

    companion object {
        const val REQUEST_CODE = 1001
    }

    fun createRequestIntent(): Intent {
        val manager =
            context.getSystemService(
                Context.MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        return manager.createScreenCaptureIntent()
    }

    fun createProjection(
        resultCode: Int,
        data: Intent
    ): MediaProjection? {

        val manager =
            context.getSystemService(
                Context.MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        return manager.getMediaProjection(
            resultCode,
            data
        )
    }
}
