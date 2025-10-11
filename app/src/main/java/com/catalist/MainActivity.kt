package com.catalist

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.catalist.ai.*
import com.catalist.emotion.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * MainActivity - Main activity for the Catalist AI Android application
 * Provides the user interface and manages the AI service connection
 */
class MainActivity : ComponentActivity() {
    
    private lateinit var aiService: CatalistAIService
    private var isServiceBound = false
    
    // Permission request launcher
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            initializeAI()
        } else {
            showPermissionError()
        }
    }
    
    // Required permissions for AI functionality
    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.MODIFY_AUDIO_SETTINGS,
        Manifest.permission.WAKE_LOCK
    )
    
    // Service connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as CatalistAIService.LocalBinder
            aiService = binder.getService()
            isServiceBound = true
            Log.d("MainActivity", "AI Service connected")
            observeAIResponses()
        }
        
        override fun onServiceDisconnected(className: ComponentName) {
            isServiceBound = false
            Log.d("MainActivity", "AI Service disconnected")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        Log.d("MainActivity", "Catalist AI MainActivity created")
        
        // Check and request permissions
        if (hasAllPermissions()) {
            initializeAI()
        } else {
            requestPermissions()
        }
    }
    
    override fun onStart() {
        super.onStart()
        if (hasAllPermissions()) {
            bindAIService()
        }
    }
    
    override fun onStop() {
        super.onStop()
        unbindAIService()
    }
    
    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestPermissions() {
        requestPermissionsLauncher.launch(requiredPermissions)
    }
    
    private fun showPermissionError() {
        Toast.makeText(
            this,
            "Catalist AI requires microphone and network permissions to function properly",
            Toast.LENGTH_LONG
        ).show()
    }
    
    private fun initializeAI() {
        Log.d("MainActivity", "Initializing Catalist AI...")
        bindAIService()
    }
    
    private fun bindAIService() {
        val intent = Intent(this, CatalistAIService::class.java)
        startService(intent) // Start the service first
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun unbindAIService() {
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }
    
    private fun observeAIResponses() {
        lifecycleScope.launch {
            if (isServiceBound) {
                aiService.getAIResponses().collect { response ->
                    handleAIResponse(response)
                }
            }
        }
        
        lifecycleScope.launch {
            if (isServiceBound) {
                aiService.getAIState().collect { state ->
                    handleAIStateChange(state)
                }
            }
        }
    }
    
    private fun handleAIResponse(response: AIResponse) {
        Log.d("MainActivity", "Received AI response: ${response.text}")
        
        // Update UI with response
        runOnUiThread {
            // TODO: Update UI elements to display the response
            Toast.makeText(this, response.text, Toast.LENGTH_SHORT).show()
            
            // If there's audio data, play it
            response.audioData?.let { audioData ->
                // TODO: Play audio response
                Log.d("MainActivity", "Playing audio response")
            }
        }
    }
    
    private fun handleAIStateChange(state: AIState) {
        Log.d("MainActivity", "AI State changed to: $state")
        
        runOnUiThread {
            when (state) {
                AIState.INITIALIZING -> {
                    showStatus("Initializing Catalist AI...")
                }
                AIState.READY -> {
                    showStatus("Catalist AI ready!")
                    enableUserInteraction()
                }
                AIState.PROCESSING -> {
                    showStatus("Processing...")
                }
                AIState.ERROR -> {
                    showStatus("Error occurred in AI system")
                    showErrorDialog()
                }
                else -> {
                    showStatus(state.toString())
                }
            }
        }
    }
    
    private fun showStatus(message: String) {
        // TODO: Update status UI element
        Log.d("MainActivity", "Status: $message")
    }
    
    private fun enableUserInteraction() {
        // TODO: Enable input UI elements
        Log.d("MainActivity", "User interaction enabled")
    }
    
    private fun showErrorDialog() {
        // TODO: Show error dialog to user
        Toast.makeText(this, "AI system encountered an error", Toast.LENGTH_LONG).show()
    }
    
    /**
     * Send text input to AI
     */
    private fun sendTextInput(text: String) {
        if (isServiceBound) {
            lifecycleScope.launch {
                val userInput = UserInput(
                    text = text,
                    context = getCurrentContext(),
                    inputType = InputType.TEXT,
                    expectSpeechResponse = false
                )
                
                aiService.processUserInput(userInput)
            }
        }
    }
    
    /**
     * Send voice input to AI
     */
    private fun sendVoiceInput(audioData: ByteArray) {
        if (isServiceBound) {
            lifecycleScope.launch {
                val userInput = UserInput(
                    text = "", // Will be transcribed by the AI
                    audioData = audioData,
                    context = getCurrentContext(),
                    inputType = InputType.VOICE,
                    expectSpeechResponse = true
                )
                
                aiService.processUserInput(userInput)
            }
        }
    }
    
    /**
     * Get current context for AI processing
     */
    private fun getCurrentContext(): EmotionContext {
        return EmotionContext(
            environment = determineEnvironment(),
            socialContext = SocialContext.ALONE, // Assuming single user for now
            timeOfDay = TimeContext.fromTimestamp(System.currentTimeMillis()),
            situation = "mobile_app_interaction"
        )
    }
    
    /**
     * Determine current environment context
     */
    private fun determineEnvironment(): EnvironmentType {
        // In a real implementation, you could use sensors, location, etc.
        return EnvironmentType.HOME // Default assumption
    }
    
    /**
     * Configure AI personality
     */
    private fun configureAIPersonality(personality: PersonalityConfiguration) {
        if (isServiceBound) {
            aiService.configurePersonality(personality)
        }
    }
    
    /**
     * Get current AI status for debugging/monitoring
     */
    private fun getAIStatus(): AIStatus? {
        return if (isServiceBound) {
            aiService.getCurrentAIStatus()
        } else null
    }
    
    /**
     * Train AI on emotional responses
     */
    private fun trainEmotionalResponse(trigger: String, emotion: EmotionType, intensity: Float) {
        if (isServiceBound) {
            lifecycleScope.launch {
                aiService.trainEmotionalResponse(trigger, emotion, intensity)
            }
        }
    }
    
    /**
     * Example interaction methods that would be called by UI elements
     */
    
    fun onTextInputSubmitted(text: String) {
        sendTextInput(text)
    }
    
    fun onVoiceRecordingComplete(audioData: ByteArray) {
        sendVoiceInput(audioData)
    }
    
    fun onPersonalityChanged(personality: PersonalityConfiguration) {
        configureAIPersonality(personality)
    }
    
    fun onEmotionalTrainingRequested(trigger: String, emotion: EmotionType, intensity: Float) {
        trainEmotionalResponse(trigger, emotion, intensity)
    }
}
