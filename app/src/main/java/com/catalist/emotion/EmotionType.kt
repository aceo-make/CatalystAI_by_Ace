package com.catalist.emotion

/**
 * EmotionType defines all supported emotions with their psychological properties.
 * Based on Russell's Circumplex Model of Affect (valence and arousal dimensions).
 */
enum class EmotionType(
    val displayName: String,
    val valence: Float, // -1.0 (negative) to +1.0 (positive)
    val arousal: Float, // 0.0 (calm) to 1.0 (highly aroused)
    val category: EmotionCategory
) {
    // Basic Emotions (Ekman's model)
    JOY("Joy", 0.8f, 0.7f, EmotionCategory.POSITIVE),
    SADNESS("Sadness", -0.7f, 0.3f, EmotionCategory.NEGATIVE),
    ANGER("Anger", -0.6f, 0.9f, EmotionCategory.NEGATIVE),
    FEAR("Fear", -0.8f, 0.8f, EmotionCategory.NEGATIVE),
    SURPRISE("Surprise", 0.1f, 0.9f, EmotionCategory.NEUTRAL),
    DISGUST("Disgust", -0.7f, 0.5f, EmotionCategory.NEGATIVE),
    
    // Extended Emotions
    LOVE("Love", 0.9f, 0.6f, EmotionCategory.POSITIVE),
    EXCITEMENT("Excitement", 0.8f, 0.9f, EmotionCategory.POSITIVE),
    CONTENTMENT("Contentment", 0.6f, 0.2f, EmotionCategory.POSITIVE),
    SERENITY("Serenity", 0.5f, 0.1f, EmotionCategory.POSITIVE),
    CURIOSITY("Curiosity", 0.3f, 0.6f, EmotionCategory.POSITIVE),
    
    ANXIETY("Anxiety", -0.5f, 0.8f, EmotionCategory.NEGATIVE),
    CONCERN("Concern", -0.3f, 0.6f, EmotionCategory.NEGATIVE),
    FRUSTRATION("Frustration", -0.6f, 0.7f, EmotionCategory.NEGATIVE),
    DISAPPOINTMENT("Disappointment", -0.5f, 0.4f, EmotionCategory.NEGATIVE),
    GUILT("Guilt", -0.7f, 0.6f, EmotionCategory.NEGATIVE),
    SHAME("Shame", -0.8f, 0.5f, EmotionCategory.NEGATIVE),
    ENVY("Envy", -0.5f, 0.6f, EmotionCategory.NEGATIVE),
    
    CONFUSION("Confusion", -0.1f, 0.5f, EmotionCategory.NEUTRAL),
    BOREDOM("Boredom", -0.2f, 0.1f, EmotionCategory.NEUTRAL),
    RELIEF("Relief", 0.4f, 0.3f, EmotionCategory.POSITIVE),
    HOPE("Hope", 0.6f, 0.5f, EmotionCategory.POSITIVE),
    PRIDE("Pride", 0.7f, 0.6f, EmotionCategory.POSITIVE),
    
    // AI-specific emotions for system states
    CONFIDENCE("Confidence", 0.5f, 0.4f, EmotionCategory.SYSTEM),
    UNCERTAINTY("Uncertainty", -0.2f, 0.6f, EmotionCategory.SYSTEM),
    PROCESSING("Processing", 0.0f, 0.7f, EmotionCategory.SYSTEM),
    LEARNING("Learning", 0.4f, 0.6f, EmotionCategory.SYSTEM),
    
    // Default state
    NEUTRAL("Neutral", 0.0f, 0.0f, EmotionCategory.NEUTRAL);
    
    /**
     * Returns emotions that are psychologically compatible for blending
     */
    fun getCompatibleEmotions(): List<EmotionType> {
        return values().filter { other ->
            when {
                this == other -> false
                category == EmotionCategory.SYSTEM || other.category == EmotionCategory.SYSTEM -> false
                // Similar valence emotions are more compatible
                kotlin.math.abs(valence - other.valence) < 0.5f -> true
                // Opposite emotions with similar arousal can create interesting blends
                kotlin.math.abs(arousal - other.arousal) < 0.3f -> true
                else -> false
            }
        }
    }
    
    /**
     * Calculates emotional distance to another emotion
     */
    fun distanceTo(other: EmotionType): Float {
        val valenceDistance = kotlin.math.abs(valence - other.valence)
        val arousalDistance = kotlin.math.abs(arousal - other.arousal)
        return kotlin.math.sqrt((valenceDistance * valenceDistance + arousalDistance * arousalDistance).toDouble()).toFloat()
    }
    
    /**
     * Returns the complementary emotion (opposite on the circumplex)
     */
    fun getComplement(): EmotionType {
        val targetValence = -valence
        val targetArousal = arousal
        
        return values().minByOrNull { emotion ->
            val valenceDiff = kotlin.math.abs(emotion.valence - targetValence)
            val arousalDiff = kotlin.math.abs(emotion.arousal - targetArousal)
            kotlin.math.sqrt((valenceDiff * valenceDiff + arousalDiff * arousalDiff).toDouble())
        } ?: NEUTRAL
    }
}

enum class EmotionCategory {
    POSITIVE,
    NEGATIVE,
    NEUTRAL,
    SYSTEM // AI system states
}
