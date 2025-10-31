package com.example.safeher.ui.resource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timeMillis: Long = System.currentTimeMillis()
)

@HiltViewModel
class AIChatViewModel @Inject constructor() : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping

    private var helpOfferedCount = 0
    private var geminiModel: GenerativeModel? = null

    // Firebase setup
    private val firestore = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest_user"
    private val chatCollection = firestore.collection("safeher_chats")

    private val recentMessagesLimit = 6
    private val chatHistory = mutableListOf<Content>()

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
        8. Cannot directly tell user whether their emotion is Positive, Negative or Neutral.
    """.trimIndent()

    init {
        _messages.value = listOf(
            ChatMessage("Hi there! 👋 I'm SafeHer AI, always here for you. How are you feeling today?", false)
        )

        chatHistory.add(Content(role = "system", parts = listOf(TextPart(systemPrompt))))

        viewModelScope.launch {
            loadChatFromFirebase()
            val apiKey = RemoteConfigManager.getGeminiApiKey()
            if (apiKey.isNotEmpty()) {
                geminiModel = GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = apiKey
                )
            } else {
                _messages.value += ChatMessage("⚠️ I'm having trouble connecting right now. Please check back in a moment.", false)
            }
        }
    }

    enum class EmotionType { POSITIVE, NEGATIVE, NEUTRAL }
    enum class IntentType { CHAT, QUESTION, HELP_OR_REASONING }

    private fun detectEmotionAndIntent(message: String): Pair<EmotionType, IntentType> {
        val lower = message.lowercase()
        val positive = listOf("happy", "good", "great", "excited", "thank", "love", "fine", "okay", "better", "relaxed")
        val negative = listOf("sad", "scared", "afraid", "panic", "anxious", "depressed", "stress", "angry", "hurt", "alone", "bad", "worried", "unsafe", "danger", "attack", "followed", "violence", "cry", "down")
        val help = listOf("help", "problem", "issue", "question", "why", "how", "should", "what", "fix", "solve", "can you", "advice")

        val emotion = when {
            positive.any { it in lower } -> EmotionType.POSITIVE
            negative.any { it in lower } -> EmotionType.NEGATIVE
            else -> EmotionType.NEUTRAL
        }

        val intent = when {
            help.any { it in lower } -> IntentType.HELP_OR_REASONING
            lower.contains("?") -> IntentType.QUESTION
            else -> IntentType.CHAT
        }

        return emotion to intent
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return
        val userMsg = ChatMessage(userMessage, true)
        _messages.value += userMsg
        chatHistory.add(Content(role = "user", parts = listOf(TextPart(userMessage))))
        trimHistory()
        saveChatToFirebase(userMsg)

        viewModelScope.launch {
            if (geminiModel == null) {
                _messages.value += ChatMessage("I'm still getting ready. Give me just a moment! ✨", false)
                return@launch
            }

            try {
                _isTyping.value = true
                val (emotion, intent) = detectEmotionAndIntent(userMessage)

                val recentContext = chatHistory.takeLast(recentMessagesLimit).joinToString("\n") {
                    val role = it.role
                    val text = it.parts.joinToString { part -> (part as? TextPart)?.text ?: "" }
                    "$role: $text"
                }

                val contextualPrompt = buildString {
                    append(systemPrompt)
                    append("\n\nRecent conversation:\n$recentContext\n")
                    append("\nUser latest message: $userMessage\n")

                    when {
                        emotion == EmotionType.NEGATIVE -> append(
                            "Respond warmly, provide emotional support, and suggest helpful actions if appropriate."
                        )
                        emotion == EmotionType.POSITIVE -> append(
                            "Reply in a friendly, encouraging, and uplifting tone."
                        )
                        intent == IntentType.HELP_OR_REASONING -> append(
                            "Provide clear, empathetic advice in a supportive manner."
                        )
                        intent == IntentType.QUESTION -> append(
                            "Answer the user’s question accurately and warmly."
                        )
                        else -> append("Respond naturally, kindly, and conversationally.")
                    }
                }

                val response = geminiModel?.generateContent(contextualPrompt)
                var reply = response?.text?.trim() ?: "I'm here for you. Could you tell me more about how you’re feeling?"

                reply = reply.replace("**", "").replace("*", "")
                val aiMsg = ChatMessage(reply, false)
                _messages.value += aiMsg
                chatHistory.add(Content(role = "model", parts = listOf(TextPart(reply))))
                trimHistory()
                saveChatToFirebase(aiMsg)

                if (emotion == EmotionType.NEGATIVE || intent == IntentType.HELP_OR_REASONING) {
                    _messages.value += ChatMessage("__HOTLINE_BUTTON__", false)
                    _messages.value += ChatMessage("__INSTANT_ALERT_BUTTON__", false)
                }

            } catch (e: Exception) {
                _messages.value += ChatMessage("I'm having trouble responding right now. Please try again! 🔄", false)
            } finally {
                _isTyping.value = false
            }
        }
    }

    private fun trimHistory() {
        if (chatHistory.size > recentMessagesLimit * 2)
            chatHistory.subList(0, chatHistory.size - recentMessagesLimit * 2).clear()
    }

    private fun saveChatToFirebase(msg: ChatMessage) {
        val data = hashMapOf(
            "message" to msg.text,
            "isUser" to msg.isUser,
            "timestamp" to msg.timeMillis
        )
        chatCollection.document(userId).collection("messages").add(data)
    }

    private suspend fun loadChatFromFirebase() {
        try {
            val snapshot = chatCollection.document(userId)
                .collection("messages")
                .orderBy("timestamp")
                .limitToLast(20)
                .get()
                .await()

            val loaded = snapshot.documents.mapNotNull { doc ->
                val msg = doc.getString("message") ?: return@mapNotNull null
                val isUser = doc.getBoolean("isUser") ?: false
                val time = doc.getLong("timestamp") ?: System.currentTimeMillis()
                ChatMessage(msg, isUser, time)
            }

            if (loaded.isNotEmpty()) {
                _messages.value = loaded
                loaded.forEach { chatHistory.add(Content(role = if (it.isUser) "user" else "model", parts = listOf(TextPart(it.text)))) }
            }
        } catch (e: Exception) {
            _messages.value += ChatMessage("⚠️ Couldn't load previous chat. Starting fresh!", false)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            // Clear local state
            _messages.value = emptyList()
            chatHistory.clear()

            // Reset initial greeting
            val initialMsg = ChatMessage(
                "Hi there! 👋 I'm SafeHer AI, always here for you. How are you feeling today?",
                false
            )
            _messages.value = listOf(initialMsg)
            chatHistory.add(Content(role = "system", parts = listOf(TextPart(systemPrompt))))

            // Delete chat history from Firebase
            try {
                val messagesSnapshot = chatCollection.document(userId)
                    .collection("messages")
                    .get()
                    .await()
                for (doc in messagesSnapshot.documents) {
                    chatCollection.document(userId)
                        .collection("messages")
                        .document(doc.id)
                        .delete()
                        .await()
                }
            } catch (e: Exception) {
                _messages.value += ChatMessage("⚠️ Couldn't clear chat from server.", false)
            }
        }
    }

}
