package com.catalist.emotion

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.*

/**
 * EmotionProcessor is the central engine for processing, blending, and managing emotion tokens.
 * It maintains the AI's emotional state and provides real-time emotion processing.
 */
class EmotionProcessor {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Current emotional state
    private val currentState = AtomicReference(
        EmotionToken(
            primaryEmotion = EmotionType.NEUTRAL,
            intensity = 500, // 0-1000 scale, 500 = neutral/middle
            context = EmotionContext(),
            confidence = 1.0f,
            source = EmotionSource(SourceType.AI_GENERATED, 1.0f)
        )
    )
    
    // Emotion history for temporal analysis
    private val emotionHistory = ConcurrentLinkedQueue<EmotionToken>()
    private val maxHistorySize = 100
    
    // Active emotion blending queue
    private val processingQueue = ConcurrentLinkedQueue<EmotionToken>()
    
    // Emotion state flow for observers
    private val _emotionState = MutableStateFlow(currentState.get())
    val emotionState: StateFlow<EmotionToken> = _emotionState.asStateFlow()
    
    // Configuration parameters
    private var emotionDecayRate = 0.95f
    private var blendingWeight = 0.3f
    private var minimumIntensityThreshold = 100 // 0-1000 scale, 100 = 10%
    private var contextSensitivity = 1.0f
    
    init {
        startProcessingLoop()
    }
    
    /**
     * Processes a new emotion token and updates the AI's emotional state
     */
    suspend fun processEmotion(token: EmotionToken) {
        processingQueue.offer(token)
        addToHistory(token)
    }
    
    /**
     * Processes multiple emotions simultaneously for complex emotional states
     */
    suspend fun processEmotions(tokens: List<EmotionToken>) {
        tokens.forEach { processingQueue.offer(it) }
        tokens.forEach { addToHistory(it) }
    }
    
    /**
     * Gets the current emotional state with context awareness
     */
    fun getCurrentState(): EmotionToken = currentState.get()
    
    /**
     * Gets emotional state with specific context filtering
     */
    fun getStateInContext(context: EmotionContext): EmotionToken {
        val state = getCurrentState()
        val contextWeight = context.getContextualWeight()
        
        return state.copy(
            intensity = (state.intensity * contextWeight).toInt().coerceIn(0, 1000),
            context = state.context.merge(context)
        )
    }
    
    /**
     * Analyzes emotional trend over recent history
     */
    fun getEmotionalTrend(timeWindow: Long = 60000): EmotionalTrend {
        val now = System.currentTimeMillis()
        val recentEmotions = emotionHistory
            .filter { now - it.timestamp <= timeWindow }
            .toList()
        
        if (recentEmotions.isEmpty()) {
            return EmotionalTrend.STABLE
        }
        
        val valenceValues = recentEmotions.map { it.getValence() }
        val arousalValues = recentEmotions.map { it.getArousal() }
        
        val valenceSlope = calculateSlope(valenceValues)
        val arousalSlope = calculateSlope(arousalValues)
        val averageIntensity = recentEmotions.map { it.intensity }.average().toFloat()
        
        return when {
            valenceSlope > 0.1f && arousalSlope > 0.1f -> EmotionalTrend.ESCALATING_POSITIVE
            valenceSlope > 0.1f && arousalSlope < -0.1f -> EmotionalTrend.CALMING_POSITIVE
            valenceSlope < -0.1f && arousalSlope > 0.1f -> EmotionalTrend.ESCALATING_NEGATIVE
            valenceSlope < -0.1f && arousalSlope < -0.1f -> EmotionalTrend.CALMING_NEGATIVE
            averageIntensity > 700f -> EmotionalTrend.INTENSE // 70% of 1000
            averageIntensity < 300f -> EmotionalTrend.SUBDUED // 30% of 1000
            else -> EmotionalTrend.STABLE
        }
    }
    
    /**
     * Predicts likely next emotional state based on history and current context
     */
    fun predictNextEmotionalState(context: EmotionContext): EmotionToken {
        val current = getCurrentState()
        val trend = getEmotionalTrend()
        val recentPattern = getRecentEmotionalPattern()
        
        val predictedEmotion = when {
            trend == EmotionalTrend.ESCALATING_POSITIVE -> {
                current.primaryEmotion.getCompatibleEmotions()
                    .filter { it.valence > current.primaryEmotion.valence }
                    .randomOrNull() ?: EmotionType.CONTENTMENT
            }
            trend == EmotionalTrend.ESCALATING_NEGATIVE -> {
                current.primaryEmotion.getCompatibleEmotions()
                    .filter { it.valence < current.primaryEmotion.valence }
                    .randomOrNull() ?: EmotionType.SADNESS
            }
            recentPattern.isNotEmpty() -> {
                // Use pattern-based prediction
                recentPattern.groupingBy { it }.eachCount()
                    .maxByOrNull { it.value }?.key ?: current.primaryEmotion
            }
            else -> current.primaryEmotion
        }
        
        return EmotionToken(
            primaryEmotion = predictedEmotion,
            intensity = (current.intensity * 0.8f).toInt().coerceIn(100, 900), // 10-90% of 1000
            context = context,
            confidence = 0.6f,
            source = EmotionSource(SourceType.AI_GENERATED, 0.8f),
            metadata = mapOf("prediction_method" to "pattern_based")
        )
    }
    
    /**
     * Adjusts processing parameters for different AI personalities
     */
    fun configurePersonality(
        decayRate: Float = 0.95f,
        sensitivity: Float = 1.0f,
        blendWeight: Float = 0.3f
    ) {
        emotionDecayRate = decayRate.coerceIn(0.8f, 0.99f)
        contextSensitivity = sensitivity.coerceIn(0.5f, 2.0f)
        blendingWeight = blendWeight.coerceIn(0.1f, 0.7f)
    }
    
    // Private methods
    
    private fun startProcessingLoop() {
        scope.launch {
            while (isActive) {
                try {
                    processQueuedEmotions()
                    applyEmotionalDecay()
                    delay(100) // Process every 100ms
                } catch (e: Exception) {
                    // Log error in production
                }
            }
        }
    }
    
    private suspend fun processQueuedEmotions() {
        val emotionsToProcess = mutableListOf<EmotionToken>()
        while (processingQueue.isNotEmpty()) {
            processingQueue.poll()?.let { emotionsToProcess.add(it) }
        }
        
        if (emotionsToProcess.isNotEmpty()) {
            val blendedEmotion = blendEmotions(emotionsToProcess)
            updateCurrentState(blendedEmotion)
        }
    }
    
    private fun blendEmotions(emotions: List<EmotionToken>): EmotionToken {
        if (emotions.isEmpty()) return getCurrentState()
        if (emotions.size == 1) return emotions.first()
        
        // Start with the highest confidence emotion
        var result = emotions.maxByOrNull { it.confidence } ?: emotions.first()
        
        // Blend with remaining emotions
        emotions.filter { it != result }.forEach { emotion ->
            val weight = calculateBlendWeight(result, emotion)
            result = result.blend(emotion, weight)
        }
        
        // Apply context sensitivity
        result = result.copy(
            intensity = (result.intensity * contextSensitivity).toInt().coerceIn(0, 1000)
        )
        
        return result
    }
    
    private fun calculateBlendWeight(primary: EmotionToken, secondary: EmotionToken): Float {
        val confidenceRatio = secondary.confidence / (primary.confidence + secondary.confidence)
        val intensityRatio = secondary.intensity / (primary.intensity + secondary.intensity)
        val compatibilityScore = 1f - primary.primaryEmotion.distanceTo(secondary.primaryEmotion) / 2f
        
        return (blendingWeight * confidenceRatio * intensityRatio * compatibilityScore)
            .coerceIn(0.1f, 0.6f)
    }
    
    private fun updateCurrentState(newState: EmotionToken) {
        if (newState.intensity >= minimumIntensityThreshold) {
            val previous = currentState.getAndSet(newState)
            _emotionState.tryEmit(newState)
        }
    }
    
    private fun applyEmotionalDecay() {
        val current = getCurrentState()
        if (current.intensity > minimumIntensityThreshold) {
            val decayedState = current.copy(
                intensity = (current.intensity * emotionDecayRate).toInt().coerceIn(0, 1000),
                timestamp = System.currentTimeMillis()
            )
            updateCurrentState(decayedState)
        }
    }
    
    private fun addToHistory(token: EmotionToken) {
        emotionHistory.offer(token)
        while (emotionHistory.size > maxHistorySize) {
            emotionHistory.poll()
        }
    }
    
    private fun getRecentEmotionalPattern(): List<EmotionType> {
        return emotionHistory.takeLast(10).map { it.primaryEmotion }
    }
    
    private fun calculateSlope(values: List<Float>): Float {
        if (values.size < 2) return 0f
        val n = values.size
        val xSum = (0 until n).sum()
        val ySum = values.sum()
        val xySum = values.mapIndexed { index, value -> index * value }.sum()
        val x2Sum = (0 until n).sumOf { it * it }
        
        val denominator = n * x2Sum - xSum * xSum
        return if (denominator != 0) {
            (n * xySum - xSum * ySum) / denominator
        } else 0f
    }
    
    fun cleanup() {
        scope.cancel()
    }
}

enum class EmotionalTrend {
    ESCALATING_POSITIVE,
    ESCALATING_NEGATIVE,
    CALMING_POSITIVE,
    CALMING_NEGATIVE,
    INTENSE,
    SUBDUED,
    STABLE
}
