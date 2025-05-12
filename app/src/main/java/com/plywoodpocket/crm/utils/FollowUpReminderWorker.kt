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
        private const val NOTIFICATION_GROUP_KEY = "com.plywoodpocket.crm.FOLLOWUP_GROUP"
        private const val API_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"
        private const val DISPLAY_DATE_FORMAT = "dd-MM-yyyy HH:mm"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val tokenManager = TokenManager(applicationContext)
            // Check if user is logged in
            if (!tokenManager.isLoggedIn()) {
                return@withContext Result.success()
            }

            val api = ApiClient(tokenManager).apiService
            val response = api.getFollowUps()
            
            if (response.isSuccessful) {
                val followUps = response.body()?.followUps ?: emptyList()
                var hasNotifications = false
                
                // Sort follow-ups by date
                val sortedFollowUps = followUps.sortedBy { it.follow_up_date }
                
                for (followUp in sortedFollowUps) {
                    // Show notification for each follow-up
                    showNotification(followUp)
                    hasNotifications = true
                }

                // Show summary notification if there are multiple follow-ups
                if (hasNotifications) {
                    showSummaryNotification(followUps.size)
                }
                
                Result.success()
            } else {
                // If token is invalid, retry
                if (response.code() == 401) {
                    Result.retry()
                } else {
                    Result.success()
                }
            }
        } catch (e: Exception) {
            // Log the error and retry
            android.util.Log.e("FollowUpReminder", "Error in follow-up reminder: ${e.message}", e)
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
            applicationContext,
            followUp.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Format the follow-up date for display
        val formattedDate = try {
            val apiFormat = SimpleDateFormat(API_DATE_FORMAT, Locale.getDefault())
            val displayFormat = SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault())
            val date = apiFormat.parse(followUp.follow_up_date ?: "")
            date?.let { displayFormat.format(it) } ?: followUp.follow_up_date
        } catch (e: Exception) {
            followUp.follow_up_date
        }

        // Create notification message based on follow-up status
        val notificationMessage = when {
            followUp.is_overdue == true -> "⚠️ Overdue follow-up with ${followUp.name ?: "Lead"} on $formattedDate"
            else -> "📅 Follow up with ${followUp.name ?: "Lead"} on $formattedDate"
        }

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Lead Follow-up Reminder")
            .setContentText(notificationMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(notificationMessage)
                .setSummaryText("Company: ${followUp.company ?: "N/A"}"))

        try {
            NotificationManagerCompat.from(applicationContext).notify(followUp.id, builder.build())
        } catch (e: SecurityException) {
            android.util.Log.e("FollowUpReminder", "Failed to show notification: ${e.message}", e)
        }
    }

    private fun showSummaryNotification(count: Int) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Follow-up Reminders")
            .setContentText("You have $count follow-ups due soon")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setGroupSummary(true)

        try {
            NotificationManagerCompat.from(applicationContext).notify(0, builder.build())
        } catch (e: SecurityException) {
            android.util.Log.e("FollowUpReminder", "Failed to show summary notification: ${e.message}", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for lead follow-ups"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
} 