package com.example.notificationdetector

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class NotificationListener : NotificationListenerService() {

    private val serverUrl = "http://10.0.2.2:5000/api/check"
    private val channelId = "traceit_alerts"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val packageName = it.packageName
            // Ignore our own TraceIt warnings to prevent endless loops
            if (packageName == this.packageName) return
            
            val notification = it.notification
            val extras = notification.extras
            
            val title = extras.getString("android.title") ?: "Unknown"
            val text = extras.getCharSequence("android.text")?.toString()
            
            val appName = getAppName(packageName)

            if (text != null && text.isNotEmpty()) {
                Log.d("NotificationListener", "Received: $text")
                checkMessageWithBackend(packageName, appName, title, text)
            }
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun checkMessageWithBackend(packageName: String, appName: String, title: String, text: String) {
        val item = NotificationItem(
            id = text.hashCode(),
            packageName = packageName,
            appName = appName,
            title = title,
            text = text
        )

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
                    
                    val responseJson = JSONObject(responseStr)
                    val sensationalism = responseJson.optString("sensationalism_score", "Low")
                    val forwardCount = responseJson.optInt("forward_count", 0)
                    
                    item.sensationalism = sensationalism
                    item.forwardCount = forwardCount

                    // Broadcast finding to MainActivity adapter in real-time
                    val intent = Intent("com.example.notificationdetector.NEW_NOTIFICATION")
                    intent.putExtra("notification_item", item)
                    sendBroadcast(intent)

                    // Emit an Android user notification if it looks like serious spam/scam
                    if (sensationalism == "High" || forwardCount > 5) {
                        showWarningNotification(appName, forwardCount, sensationalism)
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationListener", "Failed connection", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "TraceIt Alerts"
            val descriptionText = "Warnings for sensationalized or highly forwarded messages"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showWarningNotification(appName: String, count: Int, sensationalism: String) {
        val intent = Intent() 
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle("⚠️ TraceIt Alert Triggered")
            .setContentText("A viral message in $appName has been forwarded $count times (Score: $sensationalism).")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
