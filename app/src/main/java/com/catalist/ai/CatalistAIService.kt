package com.catalist.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.catalist.emotion.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * CatalistAIService - Android service that manages the Catalist AI system
 * Runs as a foreground service to maintain AI functionality in the background
 */
class CatalistAIService : Service() {
    
    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private lateinit var catalistAI: CatalistAI
    private var isInitialized = false
    
    // Notification constants
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "CATALIST_AI_CHANNEL"
    
    /**
     * Local binder for service connection
     */
    inner class LocalBinder : Binder() {
        fun getService(): CatalistAIService = this@CatalistAIService
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d("CatalistAIService", "Service created")
        
        createNotificationChannel()
        startForegroundService()
        initializeCatalistAI()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("CatalistAIService", "Service started")
        return START_STICKY // Restart if killed
    }
    
    override fun onBind(intent: Intent): IBinder {
        Log.d("CatalistAIService", "Service bound")
        return binder
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d("CatalistAIService", "Service destroyed")
        
        if (isInitialized) {
            catalistAI.shutdown()
        }
        scope.cancel()
    }
    
    /**
     * Initialize the Catalist AI system
     */
    private fun initializeCatalistAI() {
        scope.launch {
            try {
                Log.d("CatalistAIService", "Initializing Catalist AI system...")
                catalistAI = CatalistAI(applicationContext)
                
                // Wait for initialization to complete
                catalistAI.aiState.first { it == AIState.READY }
                
                isInitialized = true
                updateNotification("Catalist AI Ready")
                
                Log.d("CatalistAIService", "Catalist AI initialization complete")
                
            } catch (e: Exception) {
                Log.e("CatalistAIService", "Failed to initialize Catalist AI", e)
                updateNotification("AI Initialization Failed")
            }
        }
    }
    
    /**
     * Process user input through the AI system
     */
    suspend fun processUserInput(input: UserInput): AIResponse {
        return if (isInitialized) {
            try {
                updateNotification("Processing...")
                val response = catalistAI.processUserInput(input)
                updateNotification("Catalist AI Ready")
                response
            } catch (e: Exception) {
                Log.e("CatalistAIService", "Error processing user input", e)
                AIResponse.error("I encountered an error processing your request")
            }
        } else {
            AIResponse.error("AI system is not ready yet")
        }
    }
    
    /**
     * Configure AI personality
     */
    fun configurePersonality(config: PersonalityConfiguration) {
        if (isInitialized) {
            catalistAI.configurePersonality(config)
        }
    }
    
    /**
     * Get current AI status
     */
    fun getCurrentAIStatus(): AIStatus? {
        return if (isInitialized) {
            catalistAI.getCurrentAIStatus()
        } else null
    }
    
    /**
     * Update conversation context
     */
    fun updateContext(context: EmotionContext) {
        if (isInitialized) {
            catalistAI.updateContext(context)
        }
    }
    
    /**
     * Train AI on emotional responses
     */
    suspend fun trainEmotionalResponse(
        trigger: String,
        targetEmotion: EmotionType,
        intensity: Float
    ) {
        if (isInitialized) {
            catalistAI.trainEmotionalResponse(trigger, targetEmotion, intensity)
        }
    }
    
    /**
     * Get AI responses flow for observation
     */
    fun getAIResponses(): SharedFlow<AIResponse> {
        return if (isInitialized) {
            catalistAI.responses
        } else {
            emptyFlow<AIResponse>().asSharedFlow()
        }
    }
    
    /**
     * Get AI state flow for observation
     */
    fun getAIState(): StateFlow<AIState> {
        return if (isInitialized) {
            catalistAI.aiState
        } else {
            MutableStateFlow(AIState.INITIALIZING).asStateFlow()
        }
    }
    
    // Private helper methods
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Catalist AI Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Catalist AI running in the background"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun startForegroundService() {
        val notification = createNotification("Initializing Catalist AI...")
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotification(message: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Catalist AI")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use a proper icon in production
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
