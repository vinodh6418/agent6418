package com.agentic.ai.core

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

    // உங்கள் புதிய Gemini API Key
    private fun getApiKey(): String {
        return "AQ.Ab8RN6IWvI_upDWL3qBcFt8bJc0z_tPq2Rely6qDhXkT89ufcQ"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. திரை அமைப்பு (UI Layout)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212")) // Dark background
            setPadding(32, 48, 32, 32)
        }

        scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f
            )
        }

        chatHistoryTextView = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 16f
            text = "AI Agent Core தயார்!\n\n"
        }
        scrollView.addView(chatHistoryTextView)

        val inputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        inputEditText = EditText(this).apply {
            hint = "கேள்வியைத் தட்டச்சு செய்யவும்..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
        }

        sendButton = Button(this).apply {
            text = "அனுப்பு"
            setBackgroundColor(Color.parseColor("#1E88E5"))
            setTextColor(Color.WHITE)
        }

        inputLayout.addView(inputEditText)
        inputLayout.addView(sendButton)

        rootLayout.addView(scrollView)
        rootLayout.addView(inputLayout)

        setContentView(rootLayout)

        // 2. அனுப்பு பொத்தானின் செயல்பாடு
        sendButton.setOnClickListener {
            val userText = inputEditText.text.toString().trim()
            if (userText.isNotEmpty()) {
                appendChat("நீங்கள்: $userText\n")
                appendChat("ஏஜென்ட்: பதில் சிந்திக்கிறது...\n")
                inputEditText.text.clear()
                callGeminiApi(userText)
            }
        }
    }

    // திரையில் உரையாடலைக் காட்டும் ஃபங்க்ஷன்
    private fun appendChat(message: String) {
        runOnUiThread {
            chatHistoryTextView.append(message)
            scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    // 3. Gemini API உடன் தொடர்பு கொள்ளும் ஃபங்க்ஷன்
    private fun callGeminiApi(prompt: String) {
        thread {
            var conn: HttpURLConnection? = null
            try {
                val apiKey = getApiKey()
                // ஆதரிக்கப்படும் தற்போதைய Flash மாடல் URL
                val urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
                val url = URL(urlString)
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                
                // AQ. சாவியை x-goog-api-key ஹெடரில் அனுப்புகிறோம்
                conn.setRequestProperty("x-goog-api-key", apiKey)
                
                // Timeout-ஐ 30 வினாடிகளாக வைத்துள்ளோம்
                conn.connectTimeout = 30000 
                conn.readTimeout = 30000
                conn.doOutput = true

                // JSON Payload
                val jsonPayload = JSONObject().apply {
                    val contents = JSONArray().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        }
                        put(JSONObject().put("parts", parts))
                    }
                    put("contents", contents)
                }

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(jsonPayload.toString())
                    writer.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val jsonResponse = JSONObject(response)
                    val candidates = jsonResponse.getJSONArray("candidates")
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    val aiReply = parts.getJSONObject(0).getString("text")

                    appendChat("ஏஜென்ட்: $aiReply\n\n")
                } else {
                    val errorResponse = conn.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
                    appendChat("பிழை ($responseCode): $errorResponse\n\n")
                }
            } catch (e: Exception) {
                appendChat("பிழை: ${e.localizedMessage ?: "இணைப்பு தோல்வி"}\n\n")
            } finally {
                conn?.disconnect()
            }
        }
    }
}
