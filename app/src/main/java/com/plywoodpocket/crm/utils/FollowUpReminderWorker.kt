package com.plywoodpocket.crm.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.plywoodpocket.crm.MainActivity
import com.plywoodpocket.crm.R
import com.plywoodpocket.crm.api.ApiClient
import com.plywoodpocket.crm.models.FollowUpsResponse
import com.plywoodpocket.crm.models.LeadFollowUp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class FollowUpReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    companion object {
        const val CHANNEL_ID = "followup_reminder_channel"
        const val CHANNEL_NAME = "Follow Up Reminders"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val tokenManager = TokenManager(applicationContext)
            val api = ApiClient(tokenManager).apiService
            val response = api.getFollowUps()
            if (response.isSuccessful) {
                val followUps = response.body()?.followUps ?: emptyList()
                val now = System.currentTimeMillis()
                for (followUp in followUps) {
                    val followUpTime = parseDateToMillis(followUp.follow_up_date)
                    val diff = followUpTime - now
                    if (diff <= 2 * 24 * 60 * 60 * 1000L) { // within 2 days or overdue
                        showNotification(followUp)
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(followUp: LeadFollowUp) {
        createNotificationChannel()
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("navigate_to_followup_detail", true)
            putExtra("lead_id", followUp.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, followUp.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Lead Follow-up Reminder")
            .setContentText("Follow up with ${followUp.name ?: "Lead"} on ${followUp.follow_up_date ?: "-"}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        NotificationManagerCompat.from(applicationContext).notify(followUp.id, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun parseDateToMillis(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return 0L
        // Try both possible formats
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd HH:mm",
            "dd-MM-yyyy HH:mm",
            "dd-MM-yyyy"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                return sdf.parse(dateStr)?.time ?: 0L
            } catch (_: Exception) {}
        }
        return 0L
    }
} 