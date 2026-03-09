package com.example.notificationdetector

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(private val items: List<NotificationItem>) : 
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textApp: TextView = view.findViewById(R.id.tvAppName)
        val textTitle: TextView = view.findViewById(R.id.tvTitle)
        val textContent: TextView = view.findViewById(R.id.tvContent)
        val textScore: TextView = view.findViewById(R.id.tvScore)
        val textTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.textApp.text = item.appName
        holder.textTitle.text = item.title ?: "No Title"
        holder.textContent.text = item.text ?: ""
        
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        holder.textTime.text = timeFormat.format(Date(item.timestamp))

        val countStr = item.forwardCount?.toString() ?: "?"
        val sensStr = item.sensationalism ?: "Unknown"
        
        holder.textScore.text = "Forwards: $countStr | Score: $sensStr"
        
        if (sensStr == "High" || (item.forwardCount ?: 0) > 5) {
            holder.textScore.setTextColor(Color.parseColor("#B91C1C")) // Red
        } else if (sensStr == "Medium") {
            holder.textScore.setTextColor(Color.parseColor("#C2410C")) // Orange
        } else {
            holder.textScore.setTextColor(Color.parseColor("#047857")) // Green
        }
    }

    override fun getItemCount() = items.size
}
