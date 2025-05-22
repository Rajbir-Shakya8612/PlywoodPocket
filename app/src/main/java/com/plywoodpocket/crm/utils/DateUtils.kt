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
        if (iso == null) return null
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd"
        )
        for (pattern in formats) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = parser.parse(iso)
                if (date != null) {
                    return SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                    }.format(date)
                }
            } catch (_: Exception) {}
        }
        return iso
    }
} 