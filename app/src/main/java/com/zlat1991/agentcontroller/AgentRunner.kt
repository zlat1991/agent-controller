package com.zlat1991.agentcontroller

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class AgentRunner(
    private val apiKey: String,
    private val goal: String,
    private val onStatus: (String) -> Unit
) {

    @Volatile
    private var running = false

    fun start() {
        if (running) return

        running = true

        thread(name = "agent-runner") {
            runAgent()
        }
    }

    fun stop() {
        running = false
    }

    private fun runAgent() {

        val service = AgentAccessibilityService.instance

        if (service == null) {
            onStatus("Сначала включите управление телефоном")
            running = false
            return
        }

        onStatus("Агент запущен")

        for (step in 1..50) {

            if (!running) {
                onStatus("Остановлено")
                break
            }

            try {

                val screen = service.getScreenText()

                onStatus("Шаг $step/50: анализ экрана")

                val action = requestAction(screen)

                if (action == null) {
                    onStatus("Не удалось получить действие")
                    break
                }

                val actionName = action.optString("action")

                onStatus("Действие: $actionName")

                if (actionName == "done") {
                    onStatus(
                        action.optString(
                            "reason",
                            "Задача завершена"
                        )
                    )
                    break
                }

                val success = executeAction(
                    action,
                    service
                )

                if (!success) {
                    onStatus(
                        "Не удалось выполнить: $actionName"
                    )
                    break
                }

                Thread.sleep(800)

            } catch (e: Exception) {

                onStatus(
                    "Ошибка: ${e.message ?: "неизвестная ошибка"}"
                )

                break
            }
        }

        running = false

        onStatus("Агент остановлен")
    }

    private fun requestAction(
        screen: String
    ): JSONObject? {

        val prompt = """
            Ты — осторожный AI-оператор Android телефона.

            ЦЕЛЬ ПОЛЬЗОВАТЕЛЯ:
            $goal

            ТЕКУЩИЙ ТЕКСТ ЭКРАНА:
            $screen

            Твоя задача — выбрать ОДНО следующее действие.

            Разрешённые действия:

            {"action":"tap_text","text":"Текст кнопки"}

            {"action":"tap","x":500,"y":1000}

            {"action":"swipe","x1":500,"y1":1500,"x2":500,"y2":500,"duration":500}

            {"action":"type_text","text":"текст"}

            {"action":"back"}

            {"action":"home"}

            {"action":"wait","ms":1000}

            {"action":"done","reason":"почему задача завершена"}

            ВАЖНО:

            - Возвращай ТОЛЬКО JSON.
            - Не используй markdown.
            - Не объясняй решение.
            - Выполняй только действия, необходимые для достижения цели.
            - Не совершай покупки.
            - Не переводи деньги.
            - Не вводи пароли.
            - Не меняй пароли.
            - Не удаляй важные данные.
            - Не обходи защиту или авторизацию.
            - Если для продолжения требуется опасное действие, остановись через done.
        """.trimIndent()

        val body = JSONObject()

        body.put("model", "deepseek-chat")

        val messages = org.json.JSONArray()

        val system = JSONObject()
        system.put(
            "role",
            "system"
        )
        system.put(
            "content",
            "Ты управляешь Android интерфейсом. Возвращай только JSON."
        )

        val user = JSONObject()
        user.put(
            "role",
            "user"
        )
        user.put(
            "content",
            prompt
        )

        messages.put(system)
        messages.put(user)

        body.put(
            "messages",
            messages
        )

        body.put(
            "temperature",
            0.1
        )

        body.put(
            "max_tokens",
            500
        )

        val connection =
            URL(
                "https://api.deepseek.com/chat/completions"
            ).openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 30000

        connection.setRequestProperty(
            "Content-Type",
            "application/json"
        )

        connection.setRequestProperty(
            "Authorization",
            "Bearer $apiKey"
        )

        connection.doOutput = true

        connection.outputStream.use { output ->
            output.write(
                body.toString().toByteArray(Charsets.UTF_8)
            )
        }

        val responseCode = connection.responseCode

        val stream =
            if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

        val response = BufferedReader(
            InputStreamReader(stream)
        ).use {
            it.readText()
        }

        connection.disconnect()

        if (responseCode !in 200..299) {
            throw Exception(
                "DeepSeek HTTP $responseCode: $response"
            )
        }

        val root = JSONObject(response)

        val content =
            root
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

        return parseAction(content)
    }

    private fun parseAction(
        content: String
    ): JSONObject? {

        var text = content.trim()

        if (text.startsWith("```")) {

            text = text
                .removePrefix("```json")
                .removePrefix("```")
                .trim()

            if (text.endsWith("```")) {
                text = text
                    .removeSuffix("```")
                    .trim()
            }
        }

        val start = text.indexOf("{")
        val end = text.lastIndexOf("}")

        if (start < 0 || end <= start) {
            return null
        }

        text = text.substring(
            start,
            end + 1
        )

        return try {
            JSONObject(text)
        } catch (e: Exception) {
            null
        }
    }

    private fun executeAction(
        action: JSONObject,
        service: AgentAccessibilityService
    ): Boolean {

        return when (
            action.optString("action")
        ) {

            "tap_text" -> {

                val text =
                    action.optString("text")

                if (text.isBlank()) {
                    false
                } else {
                    service.tapText(text)
                }
            }

            "tap" -> {

                val x =
                    action.optDouble("x", -1.0)

                val y =
                    action.optDouble("y", -1.0)

                if (x < 0 || y < 0) {
                    false
                } else {
                    service.tap(
                        x.toFloat(),
                        y.toFloat()
                    )
                }
            }

            "swipe" -> {

                val x1 =
                    action.optDouble("x1", -1.0)

                val y1 =
                    action.optDouble("y1", -1.0)

                val x2 =
                    action.optDouble("x2", -1.0)

                val y2 =
                    action.optDouble("y2", -1.0)

                val duration =
                    action.optLong(
                        "duration",
                        500
                    )

                if (
                    x1 < 0 ||
                    y1 < 0 ||
                    x2 < 0 ||
                    y2 < 0
                ) {
                    false
                } else {
                    service.swipe(
                        x1.toFloat(),
                        y1.toFloat(),
                        x2.toFloat(),
                        y2.toFloat(),
                        duration
                    )
                }
            }

            "type_text" -> {

                val text =
                    action.optString("text")

                service.typeText(text)
            }

            "back" -> {
                service.back()
            }

            "home" -> {
                service.home()
            }

            "wait" -> {

                val ms =
                    action.optLong(
                        "ms",
                        1000
                    )

                Thread.sleep(
                    ms.coerceIn(100, 5000)
                )

                true
            }

            else -> false
        }
    }
}
