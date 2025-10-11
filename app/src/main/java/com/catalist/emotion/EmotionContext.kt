package com.catalist.emotion

import com.google.gson.annotations.SerializedName

/**
 * EmotionContext provides situational and environmental context for emotion tokens
 */
data class EmotionContext(
    @SerializedName("situation")
    val situation: String = "",
    
    @SerializedName("environment")
    val environment: EnvironmentType = EnvironmentType.UNKNOWN,
    
    @SerializedName("social_context")
    val socialContext: SocialContext = SocialContext.ALONE,
    
    @SerializedName("time_of_day")
    val timeOfDay: TimeContext = TimeContext.fromTimestamp(System.currentTimeMillis()),
    
    @SerializedName("activity")
    val activity: String = "",
    
    @SerializedName("conversation_turn")
    val conversationTurn: Int = 0,
    
    @SerializedName("user_mood_history")
    val userMoodHistory: List<EmotionType> = emptyList(),
    
    @SerializedName("context_tags")
    val contextTags: Set<String> = emptySet()
) {
    /**
     * Merges this context with another, prioritizing non-empty values
     */
    fun merge(other: EmotionContext): EmotionContext {
        return EmotionContext(
            situation = if (other.situation.isNotEmpty()) other.situation else situation,
            environment = if (other.environment != EnvironmentType.UNKNOWN) other.environment else environment,
            socialContext = other.socialContext,
            timeOfDay = other.timeOfDay,
            activity = if (other.activity.isNotEmpty()) other.activity else activity,
            conversationTurn = maxOf(conversationTurn, other.conversationTurn),
            userMoodHistory = (userMoodHistory + other.userMoodHistory).distinct().takeLast(5),
            contextTags = contextTags + other.contextTags
        )
    }
    
    /**
     * Returns contextual weight for emotion intensity adjustment
     */
    fun getContextualWeight(): Float {
        var weight = 1.0f
        
        // Adjust based on social context
        weight *= when (socialContext) {
            SocialContext.GROUP_CONVERSATION -> 1.2f
            SocialContext.INTIMATE_CONVERSATION -> 1.3f
            SocialContext.PUBLIC -> 0.8f
            SocialContext.ALONE -> 1.0f
            SocialContext.PROFESSIONAL -> 0.7f
        }
        
        // Adjust based on environment
        weight *= when (environment) {
            EnvironmentType.HOME -> 1.1f
            EnvironmentType.WORK -> 0.9f
            EnvironmentType.SOCIAL -> 1.2f
            EnvironmentType.TRANSPORT -> 0.8f
            EnvironmentType.OUTDOOR -> 1.0f
            EnvironmentType.UNKNOWN -> 1.0f
        }
        
        return weight.coerceIn(0.5f, 1.5f)
    }
}

/**
 * EmotionSource indicates where the emotion was detected from
 */
data class EmotionSource(
    @SerializedName("source_type")
    val sourceType: SourceType,
    
    @SerializedName("confidence_score")
    val confidenceScore: Float,
    
    @SerializedName("processing_method")
    val processingMethod: String = "",
    
    @SerializedName("sensor_data")
    val sensorData: Map<String, Float> = emptyMap()
)

enum class EnvironmentType {
    @SerializedName("home")
    HOME,
    
    @SerializedName("work")
    WORK,
    
    @SerializedName("social")
    SOCIAL,
    
    @SerializedName("transport")
    TRANSPORT,
    
    @SerializedName("outdoor")
    OUTDOOR,
    
    @SerializedName("unknown")
    UNKNOWN
}

enum class SocialContext {
    @SerializedName("alone")
    ALONE,
    
    @SerializedName("group_conversation")
    GROUP_CONVERSATION,
    
    @SerializedName("intimate_conversation")
    INTIMATE_CONVERSATION,
    
    @SerializedName("public")
    PUBLIC,
    
    @SerializedName("professional")
    PROFESSIONAL
}

enum class TimeContext(val displayName: String) {
    @SerializedName("early_morning")
    EARLY_MORNING("Early Morning"),
    
    @SerializedName("morning")
    MORNING("Morning"),
    
    @SerializedName("midday")
    MIDDAY("Midday"),
    
    @SerializedName("afternoon")
    AFTERNOON("Afternoon"),
    
    @SerializedName("evening")
    EVENING("Evening"),
    
    @SerializedName("night")
    NIGHT("Night"),
    
    @SerializedName("late_night")
    LATE_NIGHT("Late Night");
    
    companion object {
        fun fromTimestamp(timestamp: Long): TimeContext {
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = timestamp
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            
            return when (hour) {
                in 5..7 -> EARLY_MORNING
                in 8..11 -> MORNING
                in 12..13 -> MIDDAY
                in 14..17 -> AFTERNOON
                in 18..20 -> EVENING
                in 21..23 -> NIGHT
                else -> LATE_NIGHT
            }
        }
    }
}

enum class SourceType {
    @SerializedName("text_analysis")
    TEXT_ANALYSIS,
    
    @SerializedName("voice_analysis")
    VOICE_ANALYSIS,
    
    @SerializedName("facial_recognition")
    FACIAL_RECOGNITION,
    
    @SerializedName("gesture_analysis")
    GESTURE_ANALYSIS,
    
    @SerializedName("physiological_sensors")
    PHYSIOLOGICAL_SENSORS,
    
    @SerializedName("context_inference")
    CONTEXT_INFERENCE,
    
    @SerializedName("user_explicit")
    USER_EXPLICIT,
    
    @SerializedName("ai_generated")
    AI_GENERATED
}
