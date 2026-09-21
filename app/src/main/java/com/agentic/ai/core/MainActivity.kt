package com.agentic.ai.core

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var chatHistoryTextView: TextView
    private lateinit var inputEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var scrollView: ScrollView

    // உங்கள் API Key
    private val part1 = "AQ.Ab8RN6KNSyMPIpQdycVn4B"
    private val part2 = "_GYREZI4sCmJyCWPycHkAByskM4A"

    private fun getApiKey(): String {
        return part1 + part2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET
        )
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 101)
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.parseColor("#121212"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val titleView = TextView(this).apply {
            text = "AI Agent Core"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        rootLayout.addView(titleView)

        scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        chatHistoryTextView = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.parseColor("#E0E0E0"))
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            text = "வணக்கம்! என்ன உதவி வேண்டும்?\n\n"
        }
        scrollView.addView(chatHistoryTextView)
        rootLayout.addView(scrollView)

        val inputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0)
            }
        }

        inputEditText = EditText(this).apply {
            hint = "கேள்வியைத் தட்டச்சு செய்யவும்..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2A2A2A"))
            setPadding(24, 24, 24, 24)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        inputLayout.addView(inputEditText)

        sendButton = Button(this).apply {
            text = "அனுப்பு"
            setBackgroundColor(Color.parseColor("#007AFF"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 16
            }
        }
        inputLayout.addView(sendButton)

        rootLayout.addView(inputLayout)
        setContentView(rootLayout)

        sendButton.setOnClickListener {
            val userText = inputEditText.text.toString().trim()
            if (userText.isNotEmpty()) {
                appendChat("நீங்கள்", userText)
                inputEditText.text.clear()
                appendChat("ஏஜென்ட்", "பதில் சிந்திக்கிறது...")
                callGeminiApiWithRetry(userText)
            }
        }
    }

    private fun appendChat(sender: String, message: String) {
        runOnUiThread {
            chatHistoryTextView.append("$sender: $message\n\n")
            scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private fun callGeminiApiWithRetry(prompt: String) {
        thread {
            val maxRetries = 3
            var attempt = 0
            var success = false

            while (attempt < maxRetries && !success) {
                attempt++
                try {
                    val apiKey = getApiKey()
                    val urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=$apiKey"
                    val url = URL(urlString)

                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        setRequestProperty("X-goog-api-key", apiKey)
                        doOutput = true
                        connectTimeout = 20000
                        readTimeout = 20000
                    }

                    val jsonBody = JSONObject().apply {
                        val contents = JSONArray().apply {
                            val partObject = JSONObject().apply {
                                val parts = JSONArray().apply {
                                    put(JSONObject().put("text", prompt))
                                }
                                put("parts", parts)
                            }
                            put(partObject)
                        }
                        put("contents", contents)
                    }

                    val os = OutputStreamWriter(conn.outputStream, "UTF-8")
                    os.write(jsonBody.toString())
                    os.flush()
                    os.close()

                    val responseCode = conn.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(conn.inputStream))
                        val response = reader.use { it.readText() }

                        val responseJson = JSONObject(response)
                        val candidates = responseJson.getJSONArray("candidates")
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        val answer = parts.getJSONObject(0).getString("text")

                        appendChat("ஏஜென்ட்", answer.trim())
                        success = true
                    } else if (responseCode == 503 && attempt < maxRetries) {
                        // 503 சர்வர் சுமை வந்தால் 1.5 வினாடிகள் காத்திருந்து மீண்டும் முயற்சிக்கும்
                        Thread.sleep(1500)
                    } else {
                        val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                        appendChat("பிழை ($responseCode)", errorStream)
                        break
                    }
                } catch (e: Exception) {
                    if (attempt >= maxRetries) {
                        appendChat("பிழை", e.localizedMessage ?: "இணைப்பில் சிக்கல் ஏற்பட்டது")
                    } else {
                        Thread.sleep(1500)
                    }
                }
            }
        }
    }
}
