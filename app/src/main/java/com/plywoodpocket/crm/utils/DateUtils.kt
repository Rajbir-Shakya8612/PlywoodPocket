package com.plywoodpocket.crm.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    fun formatDate(date: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val d = inputFormat.parse(date)
            d?.let { outputFormat.format(it) } ?: date
        } catch (e: Exception) {
            date
        }
    }

    fun getCurrentMonth(): Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    fun getCurrentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    fun formatIsoToDate(iso: String?): String? {
        return iso?.let {
            try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = parser.parse(it)
                SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                }.format(date ?: return null)
            } catch (e: Exception) {
                null
            }
        }
    }
} 