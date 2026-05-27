package com.drawtaxi.app.logic.sms

import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class SmsFeedbackManagerTest {

    @Test
    fun testCalculateSimilarityIdenticalTexts() {
        val sim = SmsFeedbackManager.calculateSimilarity(
            "Bonjour taxi Paris Lyon 14h30",
            "Bonjour taxi Paris Lyon 14h30"
        )
        assertEquals(1.0f, sim, 0.001f)
    }

    @Test
    fun testCalculateSimilarityHalfOverlap() {
        val sim = SmsFeedbackManager.calculateSimilarity(
            "Bonjour taxi Paris Lyon 14h30",
            "Bonjour taxi Marseille Nice 15h00"
        )
        // Common: "Bonjour", "taxi" = 2, Union: 9 words total
        // 2/9 ≈ 0.22
        assertTrue(sim in 0.2f..0.25f)
    }

    @Test
    fun testCalculateSimilarityNoOverlap() {
        val sim = SmsFeedbackManager.calculateSimilarity(
            "Taxi Paris Lyon",
            "Meteo aujourdhui pluie"
        )
        assertEquals(0.0f, sim, 0.001f)
    }

    @Test
    fun testCalculateSimilarityBothEmpty() {
        val sim = SmsFeedbackManager.calculateSimilarity("", "")
        assertEquals(1.0f, sim, 0.001f)
    }

    @Test
    fun testCalculateSimilarityOneEmpty() {
        val sim = SmsFeedbackManager.calculateSimilarity("Taxi Paris", "")
        assertEquals(0.0f, sim, 0.001f)
    }

    @Test
    fun testCalculateSimilarityCaseInsensitive() {
        val sim = SmsFeedbackManager.calculateSimilarity(
            "TAXI PARIS LYON",
            "taxi paris lyon"
        )
        assertEquals(1.0f, sim, 0.001f)
    }

    @Test
    fun testCalculateSimilarityAboveThreshold() {
        val sim = SmsFeedbackManager.calculateSimilarity(
            "Bonjour taxi de Paris à Lyon pour 14h30",
            "Bonjour taxi de Paris à Lyon pour 15h00"
        )
        // Common: "Bonjour", "taxi", "de", "Paris", "à", "Lyon", "pour" = 7
        // Total unique: 9
        // 7/9 ≈ 0.78
        assertTrue("Similarity $sim should be >= 0.5", sim >= 0.5f)
    }

    @Test
    fun testCalculateSimilarityBelowThreshold() {
        val sim = SmsFeedbackManager.calculateSimilarity(
            "Taxi Paris Lyon",
            "Aller à Marseille"
        )
        // No common words
        assertEquals(0.0f, sim, 0.001f)
    }

    @Test
    fun testCalculateSimilarityWithPunctuation() {
        val sim = SmsFeedbackManager.calculateSimilarity(
            "Bonjour, taxi pour Paris !",
            "Bonjour taxi vers Lyon"
        )
        // Punctuation not stripped: "bonjour," ≠ "bonjour", "!" is a token
        // Intersection: {"taxi"} = 1, Union: {"bonjour,", "taxi", "pour", "paris", "!", "bonjour", "vers", "lyon"} = 8
        // 1/8 = 0.125
        assertTrue(sim in 0.1f..0.15f)
    }

    @Test
    fun testCalculateSimilarityHighSimilarity() {
        val sim = SmsFeedbackManager.calculateSimilarity(
            "Bonjour je voudrais un taxi de Paris à Lyon",
            "Bonjour je voudrais un taxi de Paris à Nice"
        )
        // Common: "Bonjour", "je", "voudrais", "un", "taxi", "de", "Paris", "à" = 8
        // Union: 9
        // 8/9 ≈ 0.89
        assertTrue("Similarity $sim should be >= 0.5", sim >= 0.5f)
    }
}
