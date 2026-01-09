package uk.co.zlurgg.mybookshelf.core.util

import kotlin.math.max

/**
 * Generic text processing utilities for string manipulation and similarity calculations.
 */
object TextUtils {

    /**
     * Calculates the Levenshtein distance between two strings.
     * This represents the minimum number of single-character edits (insertions, deletions, or substitutions)
     * required to change one string into another.
     *
     * @param s1 First string
     * @param s2 Second string
     * @return Levenshtein distance as an integer
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1, // deletion
                    dp[i][j - 1] + 1, // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[s1.length][s2.length]
    }

    /**
     * Calculates string similarity as a percentage (0.0 to 1.0) based on Levenshtein distance.
     * Higher values indicate more similar strings.
     *
     * @param s1 First string
     * @param s2 Second string
     * @return Similarity score from 0.0 (completely different) to 1.0 (identical)
     */
    fun calculateStringSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val distance = levenshteinDistance(s1, s2)
        val maxLength = max(s1.length, s2.length)
        return (maxLength - distance).toDouble() / maxLength
    }
}
