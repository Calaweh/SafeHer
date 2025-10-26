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

    private val systemPrompt = """
        You are SafeHer AI, a compassionate and supportive emotional assistant. 
        Your goal is to help users who may feel anxious, scared, or sad.
        1. Always respond empathetically and gently.
        2. If the message suggests stress, fear, sadness, or danger — offer emotional support and suggest relevant help resources (like mental health hotlines or SafeHer’s Resource Hub).
        3. If the user expresses distress more than once, suggest activating Instant Alert to notify emergency contacts.
        4. If the user is neutral or happy, chat normally in a friendly tone.
        5. Keep responses short and natural, like a human friend.
    """.trimIndent()

    private val chatHistory = mutableListOf<Content>()
    private var geminiModel: GenerativeModel? = null

    init {
        _messages.value = listOf(
            "Hi! 👋 I'm SafeHer AI, here to support you emotionally. How are you feeling today?" to false
        )

        chatHistory.add(Content(role = "system", parts = listOf(TextPart(systemPrompt))))

        // Fetch API key and initialize model
        viewModelScope.launch {
            val apiKey = RemoteConfigManager.getGeminiApiKey()
            if (apiKey.isNotEmpty()) {
                geminiModel = GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = apiKey
                )
            } else {
                _messages.value += "Error: Gemini API key not found in Remote Config." to false
            }
        }
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        _messages.value = _messages.value + (userMessage to true)

        viewModelScope.launch {
            if (geminiModel == null) {
                _messages.value += "⚠️ AI is still loading. Please try again in a moment." to false
                return@launch
            }

            try {
                _isTyping.value = true

                val prompt = """
                    $systemPrompt

                    User: $userMessage
                """.trimIndent()

                val response = geminiModel?.generateContent(prompt)
                val reply = response?.text ?: "Hmm, I didn’t quite understand that."

                _messages.value = _messages.value + (reply to false)

            } catch (e: Exception) {
                _messages.value = _messages.value + ("Error: ${e.message}" to false)
            } finally {
                _isTyping.value = false
            }
        }
    }
}
