package com.example.notificationdetector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.Serializable

data class NotificationItem(
    val id: Int,
    val packageName: String,
    val appName: String,
    val title: String?,
    val text: String?,
    val timestamp: Long = System.currentTimeMillis(),
    var forwardCount: Int? = null,
    var sensationalism: String? = null
) : Serializable

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationItem>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.notificationdetector.NEW_NOTIFICATION") {
                val item = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra("notification_item", NotificationItem::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra("notification_item") as? NotificationItem
                }
                
                if (item != null) {
                    runOnUiThread {
                        notificationList.add(0, item)
                        adapter.notifyItemInserted(0)
                        findViewById<RecyclerView>(R.id.recyclerView).scrollToPosition(0)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnEnable).setOnClickListener {
            if (!isNotificationServiceEnabled()) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            } else {
                Toast.makeText(this, "Notification access granted! Waiting for scans...", Toast.LENGTH_SHORT).show()
            }
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(notificationList)
        recyclerView.adapter = adapter

        // Register receiver to get updates from the active service
        val filter = IntentFilter("com.example.notificationdetector.NEW_NOTIFICATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(packageName) == true
    }
}
