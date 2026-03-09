package com.example.notificationdetector

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class NotificationListener : NotificationListenerService() {

    // 10.0.2.2 is the localhost loopback for Android Emulator. 
    // If testing on a real device, change this to your computer's local IP (e.g., 192.168.11.135:5000)
    private val serverUrl = "http://10.0.2.2:5000/api/check"

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val packageName = it.packageName
            val notification = it.notification
            val extras = notification.extras
            
            val title = extras.getString("android.title")
            val text = extras.getCharSequence("android.text")?.toString()
            
            if (text != null && text.isNotEmpty()) {
                Log.d("NotificationListener", "Received notification from $packageName: $text")
                checkMessageWithBackend(text)
            }
        }
    }

    private fun checkMessageWithBackend(text: String) {
        thread {
            try {
                val url = URL(serverUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true

                val json = JSONObject()
                json.put("text", text)

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(json.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("NotificationListener", "Server response: $responseStr")
                    
                    val responseJson = JSONObject(responseStr)
                    val sensationalism = responseJson.optString("sensationalism_score", "Low")
                    val forwardCount = responseJson.optInt("forward_count", 0)
                    
                    if (sensationalism == "High" || forwardCount > 5) {
                        Log.w("NotificationListener", "Warning: High sensationalism or forward count detected!")
                        // You could trigger a local warning notification here if needed
                    }
                } else {
                    Log.e("NotificationListener", "Server Error: $responseCode")
                }
            } catch (e: Exception) {
                Log.e("NotificationListener", "Failed to connect to backend", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
