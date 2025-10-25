package com.example.safeher.ui.resource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIChatViewModel @Inject constructor(
    private val geminiModel: GenerativeModel
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
    val messages: StateFlow<List<Pair<String, Boolean>>> = _messages

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping

    init {
        // initial welcome message
        _messages.value = listOf("Hi! 👋 I'm your AI assistant. How can I help you today?" to false)
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        // Add user message
        _messages.value = _messages.value + (userMessage to true)

        // Launch Gemini request
        viewModelScope.launch {
            try {
                _isTyping.value = true

                // Gemini generates content
                val response = geminiModel.generateContent(userMessage)
                val reply = response.text ?: "Hmm, I didn’t quite understand that."

                // Add AI reply
                _messages.value = _messages.value + (reply to false)

            } catch (e: Exception) {
                _messages.value = _messages.value + ("Error: ${e.message}" to false)
            } finally {
                _isTyping.value = false
            }
        }
    }
}
