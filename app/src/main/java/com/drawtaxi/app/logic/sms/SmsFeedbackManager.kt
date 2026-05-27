package com.drawtaxi.app.logic.sms

import android.content.Context
import com.drawtaxi.app.data.RideRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Manages user feedback on SMS parsing to improve AI accuracy over time.
 * Stores corrected parses and uses them to adapt future parsing.
 */
object SmsFeedbackManager {

    private const val PREFS_NAME = "SmsFeedbackPrefs"
    private const val KEY_FEEDBACK_LIST = "feedback_list"
    private const val MAX_FEEDBACK_STORED = 100

    private val gson = Gson()

    /**
     * Stores a corrected parse as feedback for future learning.
     * 
     * @param context Android context
     * @param originalSmsBody The original SMS body that was parsed
     * @param correctedRide The corrected RideRequest as provided by user
     */
    fun storeFeedback(context: Context, originalSmsBody: String, correctedRide: RideRequest) {
        val feedbackList = getFeedbackList(context)
        
        // Create feedback entry
        val feedbackEntry = FeedbackEntry(
            originalSms = originalSmsBody,
            correctedRide = correctedRide,
            timestamp = System.currentTimeMillis()
        )
        
        // Add to list, maintaining size limit
        feedbackList.add(0, feedbackEntry) // Add to beginning (most recent first)
        if (feedbackList.size > MAX_FEEDBACK_STORED) {
            feedbackList.removeAt(feedbackList.size - 1) // Remove oldest
        }
        
        // Save back to preferences
        val json = gson.toJson(feedbackList)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FEEDBACK_LIST, json).apply()
    }

    /**
     * Retrieves stored feedback entries.
     * 
     * @param context Android context
     * @return List of feedback entries, most recent first
     */
    private fun getFeedbackList(context: Context): MutableList<FeedbackEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_FEEDBACK_LIST, null)
        return if (json != null) {
            val type = object : TypeToken<ArrayList<FeedbackEntry>>() {}.type
            gson.fromJson(json, type) ?: mutableListOf()
        } else {
            mutableListOf()
        }
    }

    /**
     * Finds similar past corrections that could help parse the current SMS.
     * 
     * @param context Android context
     * @param smsBody The SMS body to find similar corrections for
     * @return Optional corrected RideRequest if a good match is found
     */
    fun getSimilarFeedbackCorrection(context: Context, smsBody: String): RideRequest? {
        val feedbackList = getFeedbackList(context)
        val bestMatch = feedbackList
            .mapNotNull { entry ->
                val similarity = calculateSimilarity(smsBody, entry.originalSms)
                if (similarity > 0.5) Pair(entry, similarity) else null
            }
            .maxByOrNull { it.second } // Get highest similarity
            ?.first // Get the entry
        
        return bestMatch?.correctedRide
    }

    /**
     * Calculates similarity between two SMS bodies using simple Jaccard similarity on words.
     * 
     * @param s1 First string
     * @param s2 Second string
     * @return Similarity score between 0 and 1
     */
    internal fun calculateSimilarity(s1: String, s2: String): Float {
        val words1 = s1.lowercase().split(Regex("\\s+")).toSet()
        val words2 = s2.lowercase().split(Regex("\\s+")).toSet()
        
        if (words1.isEmpty() && words2.isEmpty()) return 1.0f
        if (words1.isEmpty() || words2.isEmpty()) return 0.0f
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return intersection.toFloat() / union
    }

    /**
     * Data class to store feedback entries.
     */
    private data class FeedbackEntry(
        val originalSms: String,
        val correctedRide: RideRequest,
        val timestamp: Long
    )
}