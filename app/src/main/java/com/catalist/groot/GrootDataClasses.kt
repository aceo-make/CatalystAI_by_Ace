package com.catalist.groot

import com.catalist.emotion.EmotionToken
import com.google.gson.annotations.SerializedName

/**
 * Comprehensive analysis result from Groot N1 processing
 */
data class GrootAnalysis(
    @SerializedName("original_text")
    val originalText: String,
    
    @SerializedName("language_analysis")
    val languageAnalysis: LanguageAnalysis,
    
    @SerializedName("emotions")
    val emotions: List<EmotionToken>,
    
    @SerializedName("intent")
    val intent: IntentAnalysis,
    
    @SerializedName("entities")
    val entities: List<Entity>,
    
    @SerializedName("confidence")
    val confidence: Float,
    
    @SerializedName("contextual_relevance")
    val contextualRelevance: Float,
    
    @SerializedName("processing_metadata")
    val processingMetadata: Map<String, Any>,
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun error(text: String, errorMessage: String): GrootAnalysis {
            return GrootAnalysis(
                originalText = text,
                languageAnalysis = LanguageAnalysis.empty(),
                emotions = emptyList(),
                intent = IntentAnalysis.unknown(),
                entities = emptyList(),
                confidence = 0.0f,
                contextualRelevance = 0.0f,
                processingMetadata = mapOf("error" to errorMessage)
            )
        }
    }
}

/**
 * Language analysis including sentiment, complexity, and topics
 */
data class LanguageAnalysis(
    @SerializedName("sentiment")
    val sentiment: Float, // -1.0 (negative) to +1.0 (positive)
    
    @SerializedName("complexity")
    val complexity: Float, // 0.0 (simple) to 1.0 (complex)
    
    @SerializedName("topics")
    val topics: List<String>,
    
    @SerializedName("confidence")
    val confidence: Float,
    
    @SerializedName("language")
    val language: String,
    
    @SerializedName("readability_score")
    val readabilityScore: Float = 0.0f,
    
    @SerializedName("semantic_density")
    val semanticDensity: Float = 0.0f,
    
    @SerializedName("key_phrases")
    val keyPhrases: List<String> = emptyList()
) {
    companion object {
        fun empty(): LanguageAnalysis {
            return LanguageAnalysis(
                sentiment = 0.0f,
                complexity = 0.5f,
                topics = emptyList(),
                confidence = 0.0f,
                language = "unknown"
            )
        }
    }
}

/**
 * Intent analysis including primary intent, confidence, and alternatives
 */
data class IntentAnalysis(
    @SerializedName("primary_intent")
    val primaryIntent: Intent,
    
    @SerializedName("confidence")
    val confidence: Float,
    
    @SerializedName("alternative_intents")
    val alternativeIntents: List<Intent>,
    
    @SerializedName("contextual_factors")
    val contextualFactors: Map<String, Float>,
    
    @SerializedName("intent_parameters")
    val intentParameters: Map<String, Any> = emptyMap()
) {
    companion object {
        fun unknown(): IntentAnalysis {
            return IntentAnalysis(
                primaryIntent = Intent.UNKNOWN,
                confidence = 0.0f,
                alternativeIntents = emptyList(),
                contextualFactors = emptyMap()
            )
        }
    }
}

/**
 * Named entity recognized in text
 */
data class Entity(
    @SerializedName("type")
    val type: String,
    
    @SerializedName("value")
    val value: String,
    
    @SerializedName("start_index")
    val startIndex: Int,
    
    @SerializedName("end_index")
    val endIndex: Int,
    
    @SerializedName("confidence")
    val confidence: Float = 1.0f,
    
    @SerializedName("normalized_value")
    val normalizedValue: String? = null,
    
    @SerializedName("metadata")
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * User intent categories
 */
enum class Intent {
    @SerializedName("unknown")
    UNKNOWN,
    
    @SerializedName("greeting")
    GREETING,
    
    @SerializedName("question")
    QUESTION,
    
    @SerializedName("request_information")
    REQUEST_INFORMATION,
    
    @SerializedName("request_help")
    REQUEST_HELP,
    
    @SerializedName("conversation")
    CONVERSATION,
    
    @SerializedName("command")
    COMMAND,
    
    @SerializedName("gratitude")
    GRATITUDE,
    
    @SerializedName("apology")
    APOLOGY,
    
    @SerializedName("complaint")
    COMPLAINT,
    
    @SerializedName("compliment")
    COMPLIMENT,
    
    @SerializedName("farewell")
    FAREWELL,
    
    @SerializedName("clarification")
    CLARIFICATION,
    
    @SerializedName("agreement")
    AGREEMENT,
    
    @SerializedName("disagreement")
    DISAGREEMENT,
    
    @SerializedName("emotional_expression")
    EMOTIONAL_EXPRESSION
}

/**
 * Response quality metrics
 */
data class ResponseQuality(
    @SerializedName("relevance_score")
    val relevanceScore: Float,
    
    @SerializedName("coherence_score")
    val coherenceScore: Float,
    
    @SerializedName("emotional_appropriateness")
    val emotionalAppropriateness: Float,
    
    @SerializedName("contextual_accuracy")
    val contextualAccuracy: Float,
    
    @SerializedName("overall_quality")
    val overallQuality: Float
) {
    companion object {
        fun calculate(
            relevance: Float,
            coherence: Float,
            emotional: Float,
            contextual: Float
        ): ResponseQuality {
            val overall = (relevance + coherence + emotional + contextual) / 4f
            return ResponseQuality(relevance, coherence, emotional, contextual, overall)
        }
    }
}

/**
 * Conversation topic tracking
 */
data class ConversationTopic(
    @SerializedName("topic_id")
    val topicId: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("keywords")
    val keywords: List<String>,
    
    @SerializedName("relevance_score")
    val relevanceScore: Float,
    
    @SerializedName("first_mentioned")
    val firstMentioned: Long,
    
    @SerializedName("last_mentioned")
    val lastMentioned: Long,
    
    @SerializedName("mention_count")
    val mentionCount: Int,
    
    @SerializedName("emotional_association")
    val emotionalAssociation: Map<String, Float> = emptyMap()
)

/**
 * Learning pattern for emotional responses
 */
data class EmotionalPattern(
    @SerializedName("pattern_id")
    val patternId: String,
    
    @SerializedName("trigger_text")
    val triggerText: String,
    
    @SerializedName("trigger_keywords")
    val triggerKeywords: List<String>,
    
    @SerializedName("target_emotion")
    val targetEmotion: String,
    
    @SerializedName("intensity")
    val intensity: Float,
    
    @SerializedName("confidence")
    val confidence: Float,
    
    @SerializedName("learned_from_interactions")
    val learnedFromInteractions: Int,
    
    @SerializedName("last_reinforced")
    val lastReinforced: Long,
    
    @SerializedName("success_rate")
    val successRate: Float = 0.0f
)

/**
 * Context memory for conversation continuity
 */
data class ContextMemory(
    @SerializedName("memory_id")
    val memoryId: String,
    
    @SerializedName("type")
    val type: MemoryType,
    
    @SerializedName("content")
    val content: String,
    
    @SerializedName("importance_score")
    val importanceScore: Float,
    
    @SerializedName("created_at")
    val createdAt: Long,
    
    @SerializedName("last_accessed")
    val lastAccessed: Long,
    
    @SerializedName("access_count")
    val accessCount: Int,
    
    @SerializedName("emotional_weight")
    val emotionalWeight: Float,
    
    @SerializedName("associated_topics")
    val associatedTopics: List<String> = emptyList(),
    
    @SerializedName("user_preferences")
    val userPreferences: Map<String, Any> = emptyMap()
)

/**
 * Types of memories stored in context
 */
enum class MemoryType {
    @SerializedName("user_preference")
    USER_PREFERENCE,
    
    @SerializedName("conversation_fact")
    CONVERSATION_FACT,
    
    @SerializedName("emotional_event")
    EMOTIONAL_EVENT,
    
    @SerializedName("learned_pattern")
    LEARNED_PATTERN,
    
    @SerializedName("user_goal")
    USER_GOAL,
    
    @SerializedName("contextual_hint")
    CONTEXTUAL_HINT,
    
    @SerializedName("system_insight")
    SYSTEM_INSIGHT
}

/**
 * Processing performance metrics
 */
data class ProcessingMetrics(
    @SerializedName("analysis_time_ms")
    val analysisTimeMs: Long,
    
    @SerializedName("response_generation_time_ms")
    val responseGenerationTimeMs: Long,
    
    @SerializedName("total_processing_time_ms")
    val totalProcessingTimeMs: Long,
    
    @SerializedName("model_calls")
    val modelCalls: Int,
    
    @SerializedName("cache_hits")
    val cacheHits: Int,
    
    @SerializedName("memory_usage_mb")
    val memoryUsageMb: Float,
    
    @SerializedName("confidence_scores")
    val confidenceScores: Map<String, Float> = emptyMap(),
    
    @SerializedName("error_count")
    val errorCount: Int = 0
)
