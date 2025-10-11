package com.catalist

import com.catalist.emotion.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Test suite for EmotionProcessor functionality
 */
class EmotionProcessorTest {
    
    private lateinit var emotionProcessor: EmotionProcessor
    
    @Before
    fun setup() {
        emotionProcessor = EmotionProcessor()
    }
    
    @Test
    fun `test initial emotional state is neutral`() {
        val initialState = emotionProcessor.getCurrentState()
        assertEquals(EmotionType.NEUTRAL, initialState.primaryEmotion)
        assertEquals(500, initialState.intensity) // 500 out of 1000 = neutral
    }
    
    @Test
    fun `test single emotion processing`() = runBlocking {
        val joyToken = createTestEmotionToken(EmotionType.JOY, 0.8f)
        
        emotionProcessor.processEmotion(joyToken)
        
        // Allow processing time
        kotlinx.coroutines.delay(200)
        
        val currentState = emotionProcessor.getCurrentState()
        assertEquals(EmotionType.JOY, currentState.primaryEmotion)
        assertTrue("Intensity should be positive", currentState.intensity > 500)
    }
    
    @Test
    fun `test emotion blending`() = runBlocking {
        val joyToken = createTestEmotionToken(EmotionType.JOY, 0.7f)
        val excitementToken = createTestEmotionToken(EmotionType.EXCITEMENT, 0.9f)
        
        emotionProcessor.processEmotions(listOf(joyToken, excitementToken))
        
        kotlinx.coroutines.delay(200)
        
        val currentState = emotionProcessor.getCurrentState()
        
        // Should be a blend of joy and excitement (both positive emotions)
        assertTrue("Should have high positive intensity", currentState.intensity > 600)
        assertTrue("Should be positive emotion", 
            currentState.primaryEmotion.category == EmotionCategory.POSITIVE)
    }
    
    @Test
    fun `test emotion decay over time`() = runBlocking {
        val highIntensityEmotion = createTestEmotionToken(EmotionType.ANGER, 1.0f)
        
        emotionProcessor.processEmotion(highIntensityEmotion)
        kotlinx.coroutines.delay(200)
        
        val immediateState = emotionProcessor.getCurrentState()
        val immediateIntensity = immediateState.intensity
        
        // Wait for decay
        kotlinx.coroutines.delay(1000)
        
        val decayedState = emotionProcessor.getCurrentState()
        assertTrue("Emotion should decay over time", 
            decayedState.intensity < immediateIntensity)
    }
    
    @Test
    fun `test emotional trend analysis`() = runBlocking {
        // Create sequence of escalating positive emotions
        val emotions = listOf(
            createTestEmotionToken(EmotionType.CONTENTMENT, 0.3f),
            createTestEmotionToken(EmotionType.JOY, 0.6f),
            createTestEmotionToken(EmotionType.EXCITEMENT, 0.9f)
        )
        
        emotions.forEachIndexed { index, emotion ->
            // Adjust timestamp to simulate progression
            val adjustedEmotion = emotion.copy(timestamp = System.currentTimeMillis() + index * 1000)
            emotionProcessor.processEmotion(adjustedEmotion)
            kotlinx.coroutines.delay(100)
        }
        
        val trend = emotionProcessor.getEmotionalTrend()
        assertEquals("Should detect escalating positive trend", 
            EmotionalTrend.ESCALATING_POSITIVE, trend)
    }
    
    @Test
    fun `test personality configuration effects`() = runBlocking {
        // Test with high sensitivity
        emotionProcessor.configurePersonality(
            decayRate = 0.9f,
            sensitivity = 2.0f,
            blendWeight = 0.5f
        )
        
        val testEmotion = createTestEmotionToken(EmotionType.JOY, 0.5f)
        emotionProcessor.processEmotion(testEmotion)
        kotlinx.coroutines.delay(200)
        
        val sensitiveState = emotionProcessor.getCurrentState()
        
        // Reset to normal sensitivity
        emotionProcessor.configurePersonality(
            decayRate = 0.9f,
            sensitivity = 1.0f,
            blendWeight = 0.5f
        )
        
        emotionProcessor.processEmotion(testEmotion)
        kotlinx.coroutines.delay(200)
        
        val normalState = emotionProcessor.getCurrentState()
        
        // High sensitivity should result in higher intensity
        assertTrue("High sensitivity should amplify emotions",
            sensitiveState.intensity >= normalState.intensity)
    }
    
    @Test
    fun `test contextual emotion weighting`() {
        val homeContext = EmotionContext(
            environment = EnvironmentType.HOME,
            socialContext = SocialContext.ALONE
        )
        
        val workContext = EmotionContext(
            environment = EnvironmentType.WORK,
            socialContext = SocialContext.PROFESSIONAL
        )
        
        val homeWeight = homeContext.getContextualWeight()
        val workWeight = workContext.getContextualWeight()
        
        assertTrue("Home context should have higher emotional weight than work",
            homeWeight > workWeight)
    }
    
    @Test
    fun `test emotion compatibility calculation`() {
        val joyCompatible = EmotionType.JOY.getCompatibleEmotions()
        
        assertTrue("Joy should be compatible with other positive emotions",
            joyCompatible.contains(EmotionType.EXCITEMENT))
        assertFalse("Joy should not be compatible with sadness",
            joyCompatible.contains(EmotionType.SADNESS))
    }
    
    @Test
    fun `test emotion distance calculation`() {
        val joyToExcitement = EmotionType.JOY.distanceTo(EmotionType.EXCITEMENT)
        val joyToSadness = EmotionType.JOY.distanceTo(EmotionType.SADNESS)
        
        assertTrue("Joy should be closer to excitement than sadness",
            joyToExcitement < joyToSadness)
    }
    
    @Test
    fun `test emotion valence calculation`() {
        val joyToken = createTestEmotionToken(EmotionType.JOY, 0.8f)
        val sadnessToken = createTestEmotionToken(EmotionType.SADNESS, 0.8f)
        
        assertTrue("Joy should have positive valence", joyToken.getValence() > 0)
        assertTrue("Sadness should have negative valence", sadnessToken.getValence() < 0)
    }
    
    @Test
    fun `test emotion arousal calculation`() {
        val excitementToken = createTestEmotionToken(EmotionType.EXCITEMENT, 0.8f)
        val serenityToken = createTestEmotionToken(EmotionType.SERENITY, 0.8f)
        
        assertTrue("Excitement should have high arousal",
            excitementToken.getArousal() > serenityToken.getArousal())
    }
    
    private fun createTestEmotionToken(
        emotion: EmotionType, 
        normalizedIntensity: Float, // 0.0-1.0 range, will be converted to 0-1000
        context: EmotionContext = EmotionContext()
    ): EmotionToken {
        return EmotionToken(
            primaryEmotion = emotion,
            intensity = (normalizedIntensity * 1000).toInt().coerceIn(0, 1000),
            context = context,
            confidence = 0.9f,
            source = EmotionSource(SourceType.AI_GENERATED, 0.9f)
        )
    }
    
    // Cleanup after tests
    @org.junit.After
    fun cleanup() {
        emotionProcessor.cleanup()
    }
}
