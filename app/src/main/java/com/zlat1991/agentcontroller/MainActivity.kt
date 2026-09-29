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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val goalInput =
            findViewById<EditText>(R.id.goalInput)

        val apiKeyInput =
            findViewById<EditText>(R.id.apiKeyInput)

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

                return@setOnClickListener
            }

            if (apiKey.isEmpty()) {

                status.text =
                    "Введите DeepSeek API key"

                return@setOnClickListener
            }

            if (
                AgentAccessibilityService.instance == null
            ) {

                status.text =
                    "Сначала включите управление телефоном"

                return@setOnClickListener
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

        stopButton.setOnClickListener {

            runner?.stop()

            status.text =
                "Статус: остановлено"
        }
    }

    override fun onDestroy() {

        runner?.stop()

        super.onDestroy()
    }
}
