package com.agentic.ai.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.*

class AgentForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var voiceEngine: VoiceCommandEngine? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = Notification.Builder(this, "AgentChannel")
            .setContentTitle("Autonomous AI Agent")
            .setContentText("ஏஜென்ட் தன்னாட்சியாக இயங்குகிறது...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)

        // குரல் கட்டளை இன்ஜினை இயக்குதல்
        voiceEngine = VoiceCommandEngine(this) { command ->
            processVoiceCommand(command)
        }
        voiceEngine?.startListening()

        return START_STICKY
    }

    private fun processVoiceCommand(command: String) {
        serviceScope.launch {
            // குரல் கட்டளையை திரையில் செயல்படுத்தும் லாஜிக்
            if (command.contains("open", ignoreCase = true) || command.contains("திற")) {
                DeviceAutomationService.instance?.clickByText("WhatsApp")
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "AgentChannel",
            "Agent Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        voiceEngine?.stopListening()
        serviceScope.cancel()
    }
}
