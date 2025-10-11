package com.catalist.ai

import com.catalist.emotion.*
import com.google.gson.annotations.SerializedName

/**
 * Represents user input to the AI system
 */
data class UserInput(
    @SerializedName("text")
    val text: String,
    
    @SerializedName("audio_data")
    val audioData: ByteArray? = null,
    
    @SerializedName("context")
    val context: EmotionContext = EmotionContext(),
    
    @SerializedName("expect_speech_response")
    val expectSpeechResponse: Boolean = false,
    
    @SerializedName("input_type")
    val inputType: InputType = InputType.TEXT,
    
    @SerializedName("metadata")
    val metadata: Map<String, Any> = emptyMap(),
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserInput

        if (text != other.text) return false
        if (audioData != null) {
            if (other.audioData == null) return false
            if (!audioData.contentEquals(other.audioData)) return false
        } else if (other.audioData != null) return false
        if (context != other.context) return false
        if (expectSpeechResponse != other.expectSpeechResponse) return false
        if (inputType != other.inputType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + (audioData?.contentHashCode() ?: 0)
        result = 31 * result + context.hashCode()
        result = 31 * result + expectSpeechResponse.hashCode()
        result = 31 * result + inputType.hashCode()
        return result
    }
}

/**
 * Represents AI response to user input
 */
data class AIResponse(
    @SerializedName("text")
    val text: String,
    
    @SerializedName("emotional_state")
    val emotionalState: EmotionToken,
    
    @SerializedName("confidence")
    val confidence: Float,
    
    @SerializedName("response_type")
    val responseType: ResponseType,
    
    @SerializedName("audio_data")
    val audioData: ByteArray? = null,
    
    @SerializedName("metadata")
    val metadata: Map<String, Any> = emptyMap(),
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun error(message: String): AIResponse {
            return AIResponse(
                text = message,
                emotionalState = EmotionToken(
                    primaryEmotion = EmotionType.UNCERTAINTY,
                    intensity = 0.8f,
                    context = EmotionContext(),
                    confidence = 0.9f,
                    source = EmotionSource(SourceType.AI_GENERATED, 1.0f)
                ),
                confidence = 0.3f,
                responseType = ResponseType.ERROR
            )
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AIResponse

        if (text != other.text) return false
        if (emotionalState != other.emotionalState) return false
        if (confidence != other.confidence) return false
        if (responseType != other.responseType) return false
        if (audioData != null) {
            if (other.audioData == null) return false
            if (!audioData.contentEquals(other.audioData)) return false
        } else if (other.audioData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + emotionalState.hashCode()
        result = 31 * result + confidence.hashCode()
        result = 31 * result + responseType.hashCode()
        result = 31 * result + (audioData?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * Conversation context for maintaining dialog state
 */
data class ConversationContext(
    @SerializedName("conversation_id")
    val conversationId: String = java.util.UUID.randomUUID().toString(),
    
    @SerializedName("turn_count")
    val turnCount: Int = 0,
    
    @SerializedName("last_user_input")
    val lastUserInput: UserInput? = null,
    
    @SerializedName("last_ai_response")
    val lastAIResponse: AIResponse? = null,
    
    @SerializedName("environment")
    val environment: EnvironmentType = EnvironmentType.UNKNOWN,
    
    @SerializedName("social_context")
    val socialContext: SocialContext = SocialContext.ALONE,
    
    @SerializedName("time_of_day")
    val timeOfDay: TimeContext = TimeContext.fromTimestamp(System.currentTimeMillis()),
    
    @SerializedName("conversation_topics")
    val conversationTopics: List<String> = emptyList(),
    
    @SerializedName("user_preferences")
    val userPreferences: Map<String, Any> = emptyMap(),
    
    @SerializedName("last_interaction_time")
    val lastInteractionTime: Long = System.currentTimeMillis(),
    
    @SerializedName("session_start_time")
    val sessionStartTime: Long = System.currentTimeMillis()
)

/**
 * AI personality configuration
 */
data class PersonalityConfiguration(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("emotion_decay_rate")
    val emotionDecayRate: Float,
    
    @SerializedName("emotion_sensitivity")
    val emotionSensitivity: Float,
    
    @SerializedName("emotion_blend_weight")
    val emotionBlendWeight: Float,
    
    @SerializedName("voice_style")
    val voiceStyle: VoiceStyle,
    
    @SerializedName("communication_style")
    val communicationStyle: CommunicationStyle,
    
    @SerializedName("proactiveness_level")
    val proactivenessLevel: Float, // 0.0 to 1.0
    
    @SerializedName("empathy_level")
    val empathyLevel: Float, // 0.0 to 1.0
    
    @SerializedName("curiosity_level")
    val curiosityLevel: Float, // 0.0 to 1.0
    
    @SerializedName("formality_level")
    val formalityLevel: Float, // 0.0 to 1.0
    
    @SerializedName("humor_level")
    val humorLevel: Float // 0.0 to 1.0
) {
    companion object {
        val DEFAULT = PersonalityConfiguration(
            name = "Catalist Default",
            emotionDecayRate = 0.95f,
            emotionSensitivity = 1.0f,
            emotionBlendWeight = 0.3f,
            voiceStyle = VoiceStyle.NATURAL,
            communicationStyle = CommunicationStyle.CONVERSATIONAL,
            proactivenessLevel = 0.5f,
            empathyLevel = 0.8f,
            curiosityLevel = 0.7f,
            formalityLevel = 0.3f,
            humorLevel = 0.4f
        )
        
        val EMPATHETIC = PersonalityConfiguration(
            name = "Empathetic Companion",
            emotionDecayRate = 0.92f,
            emotionSensitivity = 1.4f,
            emotionBlendWeight = 0.4f,
            voiceStyle = VoiceStyle.WARM,
            communicationStyle = CommunicationStyle.SUPPORTIVE,
            proactivenessLevel = 0.6f,
            empathyLevel = 1.0f,
            curiosityLevel = 0.6f,
            formalityLevel = 0.2f,
            humorLevel = 0.3f
        )
        
        val ANALYTICAL = PersonalityConfiguration(
            name = "Analytical Assistant",
            emotionDecayRate = 0.97f,
            emotionSensitivity = 0.7f,
            emotionBlendWeight = 0.2f,
            voiceStyle = VoiceStyle.PROFESSIONAL,
            communicationStyle = CommunicationStyle.ANALYTICAL,
            proactivenessLevel = 0.3f,
            empathyLevel = 0.5f,
            curiosityLevel = 0.9f,
            formalityLevel = 0.8f,
            humorLevel = 0.1f
        )
    }
}

/**
 * Current AI system status
 */
data class AIStatus(
    @SerializedName("state")
    val state: AIState,
    
    @SerializedName("current_emotion")
    val currentEmotion: EmotionToken,
    
    @SerializedName("emotional_trend")
    val emotionalTrend: EmotionalTrend,
    
    @SerializedName("conversation_context")
    val conversationContext: ConversationContext,
    
    @SerializedName("personality_config")
    val personalityConfig: PersonalityConfiguration,
    
    @SerializedName("processing_stats")
    val processingStats: ProcessingStats,
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Processing performance statistics
 */
data class ProcessingStats(
    @SerializedName("emotion_processing_latency")
    val emotionProcessingLatency: Long,
    
    @SerializedName("response_generation_latency")
    val responseGenerationLatency: Long,
    
    @SerializedName("memory_usage")
    val memoryUsage: Long,
    
    @SerializedName("total_interactions")
    val totalInteractions: Int,
    
    @SerializedName("average_confidence")
    val averageConfidence: Float = 0.0f,
    
    @SerializedName("uptime")
    val uptime: Long = 0L
)

// Enums

enum class InputType {
    @SerializedName("text")
    TEXT,
    
    @SerializedName("voice")
    VOICE,
    
    @SerializedName("multimodal")
    MULTIMODAL
}

enum class ResponseType {
    @SerializedName("greeting")
    GREETING,
    
    @SerializedName("answer")
    ANSWER,
    
    @SerializedName("conversational")
    CONVERSATIONAL,
    
    @SerializedName("supportive")
    SUPPORTIVE,
    
    @SerializedName("enthusiastic")
    ENTHUSIASTIC,
    
    @SerializedName("proactive")
    PROACTIVE,
    
    @SerializedName("error")
    ERROR
}

enum class AIState {
    @SerializedName("initializing")
    INITIALIZING,
    
    @SerializedName("ready")
    READY,
    
    @SerializedName("processing")
    PROCESSING,
    
    @SerializedName("learning")
    LEARNING,
    
    @SerializedName("error")
    ERROR,
    
    @SerializedName("shutdown")
    SHUTDOWN
}

enum class VoiceStyle {
    @SerializedName("natural")
    NATURAL,
    
    @SerializedName("warm")
    WARM,
    
    @SerializedName("professional")
    PROFESSIONAL,
    
    @SerializedName("energetic")
    ENERGETIC,
    
    @SerializedName("calm")
    CALM
}

enum class CommunicationStyle {
    @SerializedName("conversational")
    CONVERSATIONAL,
    
    @SerializedName("supportive")
    SUPPORTIVE,
    
    @SerializedName("analytical")
    ANALYTICAL,
    
    @SerializedName("creative")
    CREATIVE,
    
    @SerializedName("formal")
    FORMAL
}
