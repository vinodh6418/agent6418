package com.agentic.ai.core

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ஆடியோ ரெக்கார்டிங் அனுமதி சரிபார்த்தல்
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        // Accessibility Service அமைப்பிற்கு வழிகாட்டுதல்
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)

        // பின்னணி ஏஜென்ட் சேவையைத் தொடங்குதல்
        val serviceIntent = Intent(this, AgentForegroundService::class.java)
        startForegroundService(serviceIntent)
    }
}
