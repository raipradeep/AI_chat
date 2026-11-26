package com.example.aichat

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val modelAI = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel(
            modelName = "gemini-3.5-flash-lite",
            systemInstruction = content {
                text("You are codeLabX Bot, a friendly assistent. Keep Answer short and simple. Use Plain Text with no markdown")
            }

        )

    private var chat = modelAI.startChat()
    val message = mutableStateListOf<ChatMessage>()
    var isLoading by mutableStateOf(false)
        private set

    fun newChat() {
        if (isLoading) return
        chat = modelAI.startChat()
        message.clear()
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || userText.isEmpty() || isLoading) return
        message.add(ChatMessage(text = userText, isUser = true))
        message.add(ChatMessage(text = "", isUser = false))
        val botIndex = message.lastIndex
        isLoading = true
        sendMessageStream(userText, botIndex)

    }

    fun sendMessageStream(userText: String, botIndex: Int) {
        viewModelScope.launch {
            try {
                chat.sendMessageStream(userText).collect { chunk ->
                    val current = message[botIndex]
                    message[botIndex] = current.copy(
                        text = current.text + (chunk.text ?: "")
                    )
                }
            } catch (error: Error) {
                Log.e("VM-ERROR", "sendMessageStream: ${error.message}")
                message[botIndex]= ChatMessage(
                    text = "Oops: ${error.message} something went wrong",
                    isUser = false
                )
            } finally {
                isLoading = false
            }
        }
    }
}
