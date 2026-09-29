package com.zlat1991.agentcontroller

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.IBinder

class ScreenCaptureService : Service() {

    companion object {
        const val ACTION_START = "START_CAPTURE"
        const val ACTION_STOP = "STOP_CAPTURE"

        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"

        private const val CHANNEL_ID = "screen_capture"
        private const val NOTIFICATION_ID = 1001

        var projection: MediaProjection? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent?.action == ACTION_STOP) {
            projection?.stop()
            projection = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_START) {

            val notification =
                createNotification()

            startForeground(
                NOTIFICATION_ID,
                notification
            )

            val resultCode =
                intent.getIntExtra(
                    EXTRA_RESULT_CODE,
                    0
                )

            val data =
                intent.getParcelableExtra<Intent>(
                    EXTRA_DATA
                )

            if (data != null) {

                val manager =
                    getSystemService(
                        MEDIA_PROJECTION_SERVICE
                    ) as android.media.projection.MediaProjectionManager

                projection =
                    manager.getMediaProjection(
                        resultCode,
                        data
                    )
            }
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Захват экрана",
                NotificationManager.IMPORTANCE_LOW
            )

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        manager.createNotificationChannel(
            channel
        )
    }

    private fun createNotification(): Notification {

        return Notification.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle(
                "Agent Controller"
            )
            .setContentText(
                "AI получает изображение экрана"
            )
            .setSmallIcon(
                android.R.drawable.ic_menu_view
            )
            .build()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }

    override fun onDestroy() {

        projection?.stop()
        projection = null

        super.onDestroy()
    }
}
