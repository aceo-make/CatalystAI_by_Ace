package com.catalist.speech

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.catalist.ai.VoiceStyle
import com.catalist.emotion.*
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

/**
 * SpeechProcessor handles all speech-related functionality including:
 * - Text-to-Speech with emotional coloring
 * - Speech-to-Text recognition
 * - Voice emotion analysis
 * - Audio processing for emotion detection
 */
class SpeechProcessor(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val isInitialized = AtomicBoolean(false)
    
    // Text-to-Speech
    private var textToSpeech: TextToSpeech? = null
    private var currentVoiceStyle = VoiceStyle.NATURAL
    
    // Audio recording for speech recognition
    private var audioRecord: AudioRecord? = null
    private val isRecording = AtomicBoolean(false)
    
    // Audio processing parameters
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    
    // Emotion analysis parameters
    private val emotionAnalyzer = VoiceEmotionAnalyzer()
    
    /**
     * Initializes the speech processor
     */
    suspend fun initialize() {
        return withContext(Dispatchers.Main) {
            try {
                initializeTextToSpeech()
                initializeAudioRecord()
                isInitialized.set(true)
                Log.d("SpeechProcessor", "Speech processor initialized successfully")
            } catch (e: Exception) {
                Log.e("SpeechProcessor", "Failed to initialize speech processor", e)
                throw e
            }
        }
    }
    
    /**
     * Synthesizes speech from text with emotional coloring
     */
    suspend fun synthesizeSpeech(
        text: String,
        emotionalState: EmotionToken,
        voiceStyle: VoiceStyle
    ): ByteArray? {
        if (!isInitialized.get() || textToSpeech == null) {
            Log.w("SpeechProcessor", "TTS not initialized")
            return null
        }
        
        return withContext(Dispatchers.IO) {
            try {
                // Configure voice parameters based on emotion and style
                configureVoiceForEmotion(emotionalState, voiceStyle)
                
                // Generate speech with emotional prosody
                val processedText = applyEmotionalProsody(text, emotionalState)
                
                // Create a unique utterance ID
                val utteranceId = "catalist_${System.currentTimeMillis()}"
                
                // Synthesize to file (for now, in a real implementation you'd capture audio data)
                val result = textToSpeech?.speak(processedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                
                if (result == TextToSpeech.SUCCESS) {
                    // In a real implementation, you would capture the audio data
                    // For now, return a placeholder
                    return@withContext ByteArray(1024) // Placeholder
                } else {
                    Log.w("SpeechProcessor", "TTS failed with result: $result")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("SpeechProcessor", "Error synthesizing speech", e)
                null
            }
        }
    }
    
    /**
     * Analyzes emotion from speech audio data
     */
    suspend fun analyzeSpeechEmotion(audioData: ByteArray): List<EmotionToken> {
        return withContext(Dispatchers.Default) {
            try {
                emotionAnalyzer.analyzeAudioEmotion(audioData)
            } catch (e: Exception) {
                Log.e("SpeechProcessor", "Error analyzing speech emotion", e)
                emptyList()
            }
        }
    }
    
    /**
     * Starts voice recording for speech recognition
     */
    fun startRecording(onAudioData: (ByteArray) -> Unit) {
        if (!isInitialized.get() || isRecording.get()) return
        
        scope.launch(Dispatchers.IO) {
            try {
                audioRecord?.startRecording()
                isRecording.set(true)
                
                val buffer = ShortArray(bufferSize)
                
                while (isRecording.get()) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        // Convert to byte array and process
                        val audioData = shortArrayToByteArray(buffer, read)
                        onAudioData(audioData)
                    }
                }
            } catch (e: Exception) {
                Log.e("SpeechProcessor", "Error during recording", e)
            }
        }
    }
    
    /**
     * Stops voice recording
     */
    fun stopRecording() {
        if (isRecording.get()) {
            isRecording.set(false)
            audioRecord?.stop()
        }
    }
    
    /**
     * Configures voice style
     */
    fun configureVoice(voiceStyle: VoiceStyle) {
        currentVoiceStyle = voiceStyle
        textToSpeech?.let { tts ->
            when (voiceStyle) {
                VoiceStyle.NATURAL -> {
                    tts.setPitch(1.0f)
                    tts.setSpeechRate(1.0f)
                }
                VoiceStyle.WARM -> {
                    tts.setPitch(0.9f)
                    tts.setSpeechRate(0.9f)
                }
                VoiceStyle.PROFESSIONAL -> {
                    tts.setPitch(1.0f)
                    tts.setSpeechRate(0.95f)
                }
                VoiceStyle.ENERGETIC -> {
                    tts.setPitch(1.1f)
                    tts.setSpeechRate(1.1f)
                }
                VoiceStyle.CALM -> {
                    tts.setPitch(0.85f)
                    tts.setSpeechRate(0.85f)
                }
            }
        }
    }
    
    // Private methods
    
    private suspend fun initializeTextToSpeech() {
        return suspendCancellableCoroutine<Unit> { continuation ->
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.language = Locale.US
                    textToSpeech?.setOnUtteranceProgressListener(createUtteranceListener())
                    configureVoice(currentVoiceStyle)
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(Exception("TTS initialization failed"))
                }
            }
        }
    }
    
    private fun initializeAudioRecord() {
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                throw Exception("AudioRecord initialization failed")
            }
        } catch (e: Exception) {
            Log.e("SpeechProcessor", "Failed to initialize AudioRecord", e)
            throw e
        }
    }
    
    private fun configureVoiceForEmotion(emotion: EmotionToken, style: VoiceStyle) {
        textToSpeech?.let { tts ->
            // Base configuration from style
            configureVoice(style)
            
            // Emotional adjustments
            val pitchModifier = when (emotion.primaryEmotion) {
                EmotionType.JOY, EmotionType.EXCITEMENT -> 1.1f + (emotion.intensity * 0.2f)
                EmotionType.SADNESS, EmotionType.DISAPPOINTMENT -> 0.9f - (emotion.intensity * 0.1f)
                EmotionType.ANGER, EmotionType.FRUSTRATION -> 1.0f + (emotion.intensity * 0.15f)
                EmotionType.FEAR, EmotionType.ANXIETY -> 1.05f + (emotion.intensity * 0.25f)
                EmotionType.CONTENTMENT, EmotionType.SERENITY -> 0.95f
                else -> 1.0f
            }
            
            val rateModifier = when (emotion.primaryEmotion) {
                EmotionType.EXCITEMENT, EmotionType.ANXIETY -> 1.1f + (emotion.intensity * 0.1f)
                EmotionType.SADNESS, EmotionType.SERENITY -> 0.9f - (emotion.intensity * 0.1f)
                EmotionType.ANGER -> 1.05f + (emotion.intensity * 0.1f)
                EmotionType.CONTENTMENT -> 0.95f
                else -> 1.0f
            }
            
            // Apply emotional modulation
            val currentPitch = tts.voice?.let { 1.0f } ?: 1.0f
            val currentRate = 1.0f // Default rate
            
            tts.setPitch((currentPitch * pitchModifier).coerceIn(0.5f, 2.0f))
            tts.setSpeechRate((currentRate * rateModifier).coerceIn(0.5f, 2.0f))
        }
    }
    
    private fun applyEmotionalProsody(text: String, emotion: EmotionToken): String {
        // Add SSML-like emotional markers (simplified for demonstration)
        // In a real implementation, you would use proper SSML or prosody controls
        
        return when (emotion.primaryEmotion.category) {
            EmotionCategory.POSITIVE -> {
                if (emotion.intensity > 0.7f) {
                    "<speak><prosody rate=\"fast\" pitch=\"high\">$text</prosody></speak>"
                } else {
                    "<speak><prosody pitch=\"medium\">$text</prosody></speak>"
                }
            }
            EmotionCategory.NEGATIVE -> {
                if (emotion.primaryEmotion == EmotionType.SADNESS) {
                    "<speak><prosody rate=\"slow\" pitch=\"low\">$text</prosody></speak>"
                } else {
                    "<speak><prosody pitch=\"low\">$text</prosody></speak>"
                }
            }
            else -> text
        }
    }
    
    private fun createUtteranceListener(): UtteranceProgressListener {
        return object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d("SpeechProcessor", "TTS started: $utteranceId")
            }
            
            override fun onDone(utteranceId: String?) {
                Log.d("SpeechProcessor", "TTS completed: $utteranceId")
            }
            
            override fun onError(utteranceId: String?) {
                Log.e("SpeechProcessor", "TTS error: $utteranceId")
            }
        }
    }
    
    private fun shortArrayToByteArray(shorts: ShortArray, length: Int): ByteArray {
        val bytes = ByteArray(length * 2)
        for (i in 0 until length) {
            bytes[i * 2] = (shorts[i].toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = ((shorts[i].toInt() shr 8) and 0xFF).toByte()
        }
        return bytes
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        isRecording.set(false)
        audioRecord?.stop()
        audioRecord?.release()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}

/**
 * Voice emotion analyzer using audio signal processing
 */
private class VoiceEmotionAnalyzer {
    
    fun analyzeAudioEmotion(audioData: ByteArray): List<EmotionToken> {
        try {
            // Convert byte array to audio samples
            val samples = byteArrayToFloatArray(audioData)
            
            // Analyze various audio features
            val pitch = extractPitch(samples)
            val intensity = extractIntensity(samples)
            val spectralCentroid = extractSpectralCentroid(samples)
            val energy = extractEnergy(samples)
            
            // Map audio features to emotions
            val emotions = mutableListOf<EmotionToken>()
            
            // High pitch + high intensity = excitement/anger
            if (pitch > 200 && intensity > 0.7f) {
                val emotion = if (energy > 0.8f) EmotionType.EXCITEMENT else EmotionType.ANGER
                emotions.add(createEmotionToken(emotion, intensity * 0.8f))
            }
            
            // Low pitch + low intensity = sadness/calm
            if (pitch < 150 && intensity < 0.4f) {
                val emotion = if (energy < 0.3f) EmotionType.SADNESS else EmotionType.SERENITY
                emotions.add(createEmotionToken(emotion, (1f - intensity) * 0.7f))
            }
            
            // Medium pitch + variable intensity = neutral/curious
            if (pitch in 150f..200f) {
                emotions.add(createEmotionToken(EmotionType.CURIOSITY, intensity * 0.6f))
            }
            
            return emotions.ifEmpty { 
                listOf(createEmotionToken(EmotionType.NEUTRAL, 0.5f))
            }
            
        } catch (e: Exception) {
            Log.e("VoiceEmotionAnalyzer", "Error analyzing voice emotion", e)
            return listOf(createEmotionToken(EmotionType.NEUTRAL, 0.5f))
        }
    }
    
    private fun byteArrayToFloatArray(bytes: ByteArray): FloatArray {
        val floats = FloatArray(bytes.size / 2)
        for (i in floats.indices) {
            val low = bytes[i * 2].toInt() and 0xFF
            val high = bytes[i * 2 + 1].toInt() shl 8
            floats[i] = (high or low).toFloat() / 32768.0f
        }
        return floats
    }
    
    private fun extractPitch(samples: FloatArray): Float {
        // Simplified pitch detection using autocorrelation
        val minPeriod = 20 // ~800 Hz
        val maxPeriod = 200 // ~80 Hz
        
        var bestPeriod = minPeriod
        var bestCorrelation = 0f
        
        for (period in minPeriod..maxPeriod) {
            var correlation = 0f
            val maxLag = min(samples.size - period, 200)
            
            for (i in 0 until maxLag) {
                correlation += samples[i] * samples[i + period]
            }
            
            if (correlation > bestCorrelation) {
                bestCorrelation = correlation
                bestPeriod = period
            }
        }
        
        return 16000f / bestPeriod // Convert to Hz
    }
    
    private fun extractIntensity(samples: FloatArray): Float {
        var sum = 0f
        for (sample in samples) {
            sum += abs(sample)
        }
        return (sum / samples.size).coerceIn(0f, 1f)
    }
    
    private fun extractSpectralCentroid(samples: FloatArray): Float {
        // Simplified spectral centroid calculation
        val fft = performFFT(samples)
        var weightedSum = 0f
        var magnitudeSum = 0f
        
        for (i in fft.indices) {
            val magnitude = abs(fft[i])
            weightedSum += i * magnitude
            magnitudeSum += magnitude
        }
        
        return if (magnitudeSum > 0) weightedSum / magnitudeSum else 0f
    }
    
    private fun extractEnergy(samples: FloatArray): Float {
        var energy = 0f
        for (sample in samples) {
            energy += sample * sample
        }
        return sqrt(energy / samples.size).coerceIn(0f, 1f)
    }
    
    private fun performFFT(samples: FloatArray): FloatArray {
        // Simplified FFT implementation (in practice, use a proper FFT library)
        return samples // Placeholder
    }
    
    private fun createEmotionToken(emotion: EmotionType, intensity: Float): EmotionToken {
        return EmotionToken(
            primaryEmotion = emotion,
            intensity = intensity,
            context = EmotionContext(),
            confidence = 0.7f,
            source = EmotionSource(SourceType.VOICE_ANALYSIS, 0.8f)
        )
    }
}
