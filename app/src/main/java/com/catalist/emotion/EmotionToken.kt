package com.catalist.emotion

import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * EmotionToken represents a quantified emotional state that can be processed by the AI system.
 * It includes primary emotions, intensity levels, and contextual metadata.
 */
data class EmotionToken(
    @SerializedName("token_id")
    val tokenId: String = UUID.randomUUID().toString(),
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @SerializedName("primary_emotion")
    val primaryEmotion: EmotionType,
    
    @SerializedName("intensity")
    val intensity: Int, // 0 to 1000
    
    @SerializedName("secondary_emotions")
    val secondaryEmotions: Map<EmotionType, Int> = emptyMap(),
    
    @SerializedName("context")
    val context: EmotionContext,
    
    @SerializedName("confidence")
    val confidence: Float, // 0.0 to 1.0
    
    @SerializedName("source")
    val source: EmotionSource,
    
    @SerializedName("metadata")
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * Combines this emotion token with another, creating a blended emotional state
     */
    fun blend(other: EmotionToken, weight: Float = 0.5f): EmotionToken {
        val blendedIntensity = (intensity * (1f - weight) + other.intensity * weight).toInt().coerceIn(0, 1000)
        val blendedSecondary = mutableMapOf<EmotionType, Int>()
        
        // Combine secondary emotions
        (secondaryEmotions.keys + other.secondaryEmotions.keys).forEach { emotion ->
            val thisValue = secondaryEmotions[emotion] ?: 0
            val otherValue = other.secondaryEmotions[emotion] ?: 0
            val blendedValue = (thisValue * (1f - weight) + otherValue * weight).toInt().coerceIn(0, 1000)
            if (blendedValue > 0) {
                blendedSecondary[emotion] = blendedValue
            }
        }
        
        return copy(
            tokenId = UUID.randomUUID().toString(),
            intensity = blendedIntensity,
            secondaryEmotions = blendedSecondary,
            confidence = (confidence + other.confidence) / 2f,
            context = context.merge(other.context),
            metadata = metadata + other.metadata
        )
    }
    
    /**
     * Returns the dominant emotion considering both primary and secondary emotions
     */
    fun getDominantEmotion(): EmotionType {
        val allEmotions = secondaryEmotions + (primaryEmotion to intensity)
        return allEmotions.maxByOrNull { it.value }?.key ?: primaryEmotion
    }
    
    /**
     * Calculates the overall emotional valence (-1.0 negative to +1.0 positive)
     */
    fun getValence(): Float {
        // Normalize intensity to 0.0-1.0 range for calculation
        val normalizedIntensity = intensity / 1000f
        val primaryValence = primaryEmotion.valence * normalizedIntensity
        val secondaryValence = secondaryEmotions.entries.sumOf { 
            val normalizedSecondary = it.value / 1000f
            (it.key.valence * normalizedSecondary).toDouble() 
        }.toFloat()
        
        val totalSecondaryWeight = secondaryEmotions.values.sum() / 1000f
        return (primaryValence + secondaryValence) / (1f + totalSecondaryWeight)
    }
    
    /**
     * Calculates the emotional arousal level (0.0 calm to 1.0 highly aroused)
     */
    fun getArousal(): Float {
        // Normalize intensity to 0.0-1.0 range for calculation
        val normalizedIntensity = intensity / 1000f
        val primaryArousal = primaryEmotion.arousal * normalizedIntensity
        val secondaryArousal = secondaryEmotions.entries.sumOf { 
            val normalizedSecondary = it.value / 1000f
            (it.key.arousal * normalizedSecondary).toDouble() 
        }.toFloat()
        
        val totalSecondaryWeight = secondaryEmotions.values.sum() / 1000f
        return (primaryArousal + secondaryArousal) / (1f + totalSecondaryWeight)
    }
    
    /**
     * Returns the intensity as a normalized 0.0-1.0 value for compatibility
     */
    fun getNormalizedIntensity(): Float = intensity / 1000f
    
    /**
     * Convenience function to get intensity on the traditional 0-100 scale
     */
    fun getIntensityPercent(): Int = (intensity / 10).coerceIn(0, 100)
    
    /**
     * Creates a copy with intensity clamped to valid range
     */
    fun withValidatedIntensity(): EmotionToken {
        return copy(intensity = intensity.coerceIn(0, 1000))
    }
}
