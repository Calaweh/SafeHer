package com.example.safeher.ui.resource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIChatViewModel @Inject constructor() : ViewModel() {

    private val _messages = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
    val messages: StateFlow<List<Pair<String, Boolean>>> = _messages

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping

    private var helpOfferedCount = 0

    private val systemPrompt = """
        You are SafeHer AI, a compassionate and supportive emotional assistant designed to help women in Malaysia feel safe and supported.

        Guidelines:
        1. Be warm, empathetic, and human-like in your responses.
        2. Listen actively and validate their feelings.
        3. Keep responses conversational and concise (2–3 sentences max).
        4. Always respond in English only.
        5. Never be robotic or formal.
        6. Don't mention hotline numbers or resources explicitly — the app shows buttons.
        7. Detect the user’s emotion (positive, neutral, negative) from tone and content.

        Response tone rules:
        - Positive emotion → reply kindly, softly, and encouragingly.
        - Negative emotion → offer emotional support and gently ask if they need SafeHer’s help or alert options.
    """.trimIndent()

    private val chatHistory = mutableListOf<Content>()
    private var geminiModel: GenerativeModel? = null

    init {
        _messages.value = listOf(
            "Hi there! 👋 I'm SafeHer AI, always here for you. How are you feeling today?" to false
        )

        chatHistory.add(Content(role = "system", parts = listOf(TextPart(systemPrompt))))

        viewModelScope.launch {
            val apiKey = RemoteConfigManager.getGeminiApiKey()
            if (apiKey.isNotEmpty()) {
                geminiModel = GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = apiKey
                )
            } else {
                _messages.value += "⚠️ I'm having trouble connecting right now. Please check back in a moment." to false
            }
        }
    }

    /** Detect emotional tone of user message */
    private fun detectEmotion(message: String): EmotionType {
        val lower = message.lowercase()

        val positiveKeywords = listOf("happy", "good", "great", "excited", "thank", "love", "fine", "okay", "better")
        val negativeKeywords = listOf(
            "sad", "scared", "afraid", "panic", "anxious", "depressed",
            "stress", "angry", "hurt", "alone", "bad", "worried", "unsafe",
            "danger", "attack", "followed", "violence", "cry", "down"
        )

        return when {
            positiveKeywords.any { it in lower } -> EmotionType.POSITIVE
            negativeKeywords.any { it in lower } -> EmotionType.NEGATIVE
            else -> EmotionType.NEUTRAL
        }
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return
        _messages.value = _messages.value + (userMessage to true)

        viewModelScope.launch {
            if (geminiModel == null) {
                _messages.value += "I'm still getting ready. Give me just a moment! ✨" to false
                return@launch
            }

            try {
                _isTyping.value = true

                val emotion = detectEmotion(userMessage)
                val contextualPrompt = buildString {
                    append(systemPrompt)
                    append("\n\nUser message: $userMessage\n")

                    when (emotion) {
                        EmotionType.POSITIVE -> append(
                            "User seems positive. Reply softly, kindly, and with uplifting tone."
                        )
                        EmotionType.NEGATIVE -> append(
                            "User seems upset or distressed. Offer emotional support, validate feelings, " +
                                    "and ask gently: 'Do you need support from SafeHer's Resource Hub or alert your family and friends anytime you are in danger or emergency?' " +
                                    "Then stop. Do not suggest resources directly."
                        )
                        EmotionType.NEUTRAL -> append(
                            "User tone is neutral. Respond naturally and friendly in English."
                        )
                    }
                }

                val response = geminiModel?.generateContent(contextualPrompt)
                var reply = response?.text?.trim()
                    ?: "I'm here for you. Could you tell me more about how you’re feeling?"

                reply = reply.replace("**", "").replace("*", "")

                _messages.value = _messages.value + (reply to false)

                // Button logic
                when (emotion) {
                    EmotionType.NEGATIVE -> {
                        // Offer both support options
                        _messages.value += ("__HOTLINE_BUTTON__" to false)
                        _messages.value += ("__INSTANT_ALERT_BUTTON__" to false)
                        helpOfferedCount++
                    }

                    EmotionType.NEUTRAL -> {
                        // Detect direct requests for help
                        if (userMessage.contains("need resource", true)
                            || userMessage.contains("want help", true)
                            || userMessage.contains("alert", true)
                            || userMessage.contains("family", true)
                            || userMessage.contains("danger", true)
                        ) {
                            _messages.value += (
                                    "SafeHer has provided hotline resources for you, or you can trigger Instant Alert if you want." to false
                                    )
                            _messages.value += ("__HOTLINE_BUTTON__" to false)
                            _messages.value += ("__INSTANT_ALERT_BUTTON__" to false)
                            helpOfferedCount++
                        }
                    }

                    EmotionType.POSITIVE -> {
                        // No support buttons, just friendly talk
                    }
                }

            } catch (e: Exception) {
                _messages.value += ("I'm having trouble responding right now. Please try again! 🔄" to false)
            } finally {
                _isTyping.value = false
            }
        }
    }

    enum class EmotionType {
        POSITIVE, NEGATIVE, NEUTRAL
    }
}
