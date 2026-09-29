package com.zlat1991.agentcontroller

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class MainActivity : Activity() {

    private var runner: AgentRunner? = null

    private var screenCaptureEngine: ScreenCaptureEngine? = null

    private lateinit var status: TextView

    private lateinit var goalInput: EditText
    private lateinit var apiKeyInput: EditText

    private val screenCaptureManager by lazy {
        ScreenCaptureManager(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        goalInput =
            findViewById(R.id.goalInput)

        apiKeyInput =
            findViewById(R.id.apiKeyInput)

        status =
            findViewById(R.id.status)

        val accessibilityButton =
            findViewById<Button>(
                R.id.accessibilityButton
            )

        val startButton =
            findViewById<Button>(
                R.id.startButton
            )

        val stopButton =
            findViewById<Button>(
                R.id.stopButton
            )

        accessibilityButton.setOnClickListener {

            startActivity(
                Intent(
                    Settings.ACTION_ACCESSIBILITY_SETTINGS
                )
            )
        }

        startButton.setOnClickListener {

            startAgent()
        }

        stopButton.setOnClickListener {

            runner?.stop()

            screenCaptureEngine?.stop()
            screenCaptureEngine = null

            status.text =
                "Статус: остановлено"
        }
    }

    private fun startAgent() {

        val goal =
            goalInput.text
                .toString()
                .trim()

        val apiKey =
            apiKeyInput.text
                .toString()
                .trim()

        if (goal.isEmpty()) {

            status.text =
                "Введите задачу"

            return
        }

        if (apiKey.isEmpty()) {

            status.text =
                "Введите DeepSeek API key"

            return
        }

        if (
            AgentAccessibilityService.instance == null
        ) {

            status.text =
                "Сначала включите управление телефоном"

            return
        }

        if (screenCaptureEngine == null) {

            status.text =
                "Запрашиваем доступ к экрану..."

            startActivityForResult(
                screenCaptureManager.createRequestIntent(),
                ScreenCaptureManager.REQUEST_CODE
            )

            return
        }

        startRunner(
            goal,
            apiKey
        )
    }

    private fun startRunner(
        goal: String,
        apiKey: String
    ) {

        runner?.stop()

        runner =
            AgentRunner(
                apiKey = apiKey,
                goal = goal
            ) { message ->

                runOnUiThread {

                    status.text =
                        "Статус: $message"
                }
            }

        status.text =
            "Статус: запуск агента..."

        runner?.start()
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode !=
            ScreenCaptureManager.REQUEST_CODE
        ) {
            return
        }

        if (
            resultCode != RESULT_OK ||
            data == null
        ) {

            status.text =
                "Доступ к экрану не предоставлен"

            return
        }

        try {

            val projection =
                screenCaptureManager.createProjection(
                    resultCode,
                    data
                )

            if (projection == null) {

                status.text =
                    "Не удалось получить доступ к экрану"

                return
            }

            val metrics =
                DisplayMetrics()

            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
                .getMetrics(metrics)

            val width =
                metrics.widthPixels

            val height =
                metrics.heightPixels

            val density =
                metrics.densityDpi

            screenCaptureEngine =
                ScreenCaptureEngine(
                    projection = projection,
                    width = width,
                    height = height,
                    density = density
                )

            screenCaptureEngine?.start()

            status.text =
                "Экран доступен AI"

            val goal =
                goalInput.text
                    .toString()
                    .trim()

            val apiKey =
                apiKeyInput.text
                    .toString()
                    .trim()

            if (
                goal.isNotEmpty() &&
                apiKey.isNotEmpty()
            ) {

                startRunner(
                    goal,
                    apiKey
                )
            }

        } catch (e: Exception) {

            status.text =
                "Ошибка захвата экрана: ${e.message}"
        }
    }

    override fun onDestroy() {

        runner?.stop()

        screenCaptureEngine?.stop()
        screenCaptureEngine = null

        super.onDestroy()
    }
}
