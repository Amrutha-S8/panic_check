package com.example.notificationdetector

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
