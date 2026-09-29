package com.zlat1991.agentcontroller

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class MainActivity : Activity() {

    private var runner: AgentRunner? = null

    private lateinit var status: TextView
    private lateinit var goalInput: EditText
    private lateinit var apiKeyInput: EditText

    private val screenCaptureManager by lazy {
        ScreenCaptureManager(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        goalInput = findViewById(R.id.goalInput)
        apiKeyInput = findViewById(R.id.apiKeyInput)
        status = findViewById(R.id.status)

        val accessibilityButton =
            findViewById<Button>(R.id.accessibilityButton)

        val startButton =
            findViewById<Button>(R.id.startButton)

        val stopButton =
            findViewById<Button>(R.id.stopButton)

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
            runner = null

            stopScreenCaptureService()

            status.text = "Статус: остановлено"
        }
    }

    private fun startAgent() {

        val goal =
            goalInput.text.toString().trim()

        val apiKey =
            apiKeyInput.text.toString().trim()

        if (goal.isEmpty()) {
            status.text = "Введите задачу"
            return
        }

        if (apiKey.isEmpty()) {
            status.text = "Введите DeepSeek API key"
            return
        }

        if (screenCaptureEngineIsRunning()) {
            startRunner(goal, apiKey)
            return
        }

        status.text =
            "Запрашиваем доступ к экрану..."

        startActivityForResult(
            screenCaptureManager.createRequestIntent(),
            ScreenCaptureManager.REQUEST_CODE
        )
    }

    private fun startRunner(
        goal: String,
        apiKey: String
    ) {

        if (
            AgentAccessibilityService.instance == null
        ) {
            status.text =
                "Сначала включите управление телефоном"
            return
        }

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

    private fun screenCaptureEngineIsRunning(): Boolean {
        return ScreenCaptureService.projection != null
    }

    private fun startScreenCaptureService(
        resultCode: Int,
        data: Intent
    ) {

        val serviceIntent =
            Intent(
                this,
                ScreenCaptureService::class.java
            ).apply {

                action =
                    ScreenCaptureService.ACTION_START

                putExtra(
                    ScreenCaptureService.EXTRA_RESULT_CODE,
                    resultCode
                )

                putExtra(
                    ScreenCaptureService.EXTRA_DATA,
                    data
                )
            }

        startForegroundService(
            serviceIntent
        )
    }

    private fun stopScreenCaptureService() {

        val intent =
            Intent(
                this,
                ScreenCaptureService::class.java
            ).apply {
                action =
                    ScreenCaptureService.ACTION_STOP
            }

        startService(intent)
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

            startScreenCaptureService(
                resultCode,
                data
            )

            status.text =
                "Запускаем захват экрана..."

            val goal =
                goalInput.text
                    .toString()
                    .trim()

            val apiKey =
                apiKeyInput.text
                    .toString()
                    .trim()

            window.decorView.postDelayed({

                if (
                    ScreenCaptureService.projection != null
                ) {

                    status.text =
                        "Экран доступен AI"

                    if (
                        goal.isNotEmpty() &&
                        apiKey.isNotEmpty()
                    ) {

                        startRunner(
                            goal,
                            apiKey
                        )
                    }

                } else {

                    status.text =
                        "Не удалось запустить захват экрана"
                }

            }, 1000)

        } catch (e: Exception) {

            status.text =
                "Ошибка захвата экрана: ${e.message}"
        }
    }

    override fun onDestroy() {

        runner?.stop()
        runner = null

        stopScreenCaptureService()

        super.onDestroy()
    }
}
