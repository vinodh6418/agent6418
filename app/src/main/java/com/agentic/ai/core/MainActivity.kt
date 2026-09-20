package com.agentic.ai.core

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
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

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. ஆடியோ ரெக்கார்டிங் அனுமதி சரிபார்த்தல்
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        // 2. பின்னணி ஏஜென்ட் சேவையைத் தொடங்குதல் (இருந்தால் மட்டும்)
        try {
            val serviceIntent = Intent(this, AgentForegroundService::class.java)
            startForegroundService(serviceIntent)
        } catch (e: Exception) {
            // Service இல்லை என்றாலும் ஆப் கிராஷ் ஆகாமல் தடுக்க
        }

        // 3. முழு வெண்மை நிறத் திரை அமைப்பு (Clean White UI Layout)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FFFFFF"))
            setPadding(24, 40, 24, 24)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // தலைப்பு (Header)
        val header = TextView(this).apply {
            text = "AI Agent"
            textSize = 20f
            setTextColor(Color.parseColor("#111111"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        rootLayout.addView(header)

        // சாட் பகுதி (Chat History Area)
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        val chatBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(chatBox)
        rootLayout.addView(scrollView)

        // ஆரம்ப வரவேற்புச் செய்தி
        val welcomeMsg = TextView(this).apply {
            text = "வணக்கம்! நான் உங்கள் ஏஜென்ட். கீழே உள்ள பெட்டியில் கட்டளையிடலாம்."
            textSize = 15f
            setTextColor(Color.parseColor("#222222"))
            setPadding(20, 16, 20, 16)
            setBackgroundColor(Color.parseColor("#F1F3F4"))
        }
        chatBox.addView(welcomeMsg)

        // உள்ளீட்டு பகுதி (Bottom Input Bar)
        val inputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }

        // தட்டச்சு செய்யும் பெட்டி (Input Box)
        val inputField = EditText(this).apply {
            hint = "இங்கு தட்டச்சு செய்யவும்..."
            setHintTextColor(Color.parseColor("#888888"))
            setTextColor(Color.parseColor("#000000"))
            setBackgroundColor(Color.parseColor("#F8F9FA"))
            setPadding(24, 20, 24, 20)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        // அனுப்பு பட்டன் (Send Button)
        val sendBtn = Button(this).apply {
            text = "அனுப்பு"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1A73E8"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 12
            }
        }

        // பட்டன் கிளிக் செயல்பாடு
        sendBtn.setOnClickListener {
            val userText = inputField.text.toString().trim()
            if (userText.isNotEmpty()) {
                // பயனர் உரையைச் சேர்த்தல்
                val userMsg = TextView(this).apply {
                    text = userText
                    textSize = 15f
                    setTextColor(Color.BLACK)
                    gravity = Gravity.END
                    setPadding(20, 16, 20, 16)
                }
                chatBox.addView(userMsg)
                inputField.text.clear()
            }
        }

        inputLayout.addView(inputField)
        inputLayout.addView(sendBtn)
        rootLayout.addView(inputLayout)

        // திரையைக் காட்டுதல்
        setContentView(rootLayout)
    }
}
