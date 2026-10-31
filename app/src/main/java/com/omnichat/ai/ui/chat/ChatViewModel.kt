package com.omnichat.ai.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnichat.ai.AppContainer
import com.omnichat.ai.domain.ChatMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(private val container: AppContainer, private val chatId: Long) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = container.chatRepository
        .observeMessages(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    private var job: Job? = null

    fun send(text: String) {
        if (text.isBlank() || job?.isActive == true) return
        job = viewModelScope.launch {
            _sending.value = true
            try {
                container.sendMessage(chatId, text.trim())
            } finally {
                _sending.value = false
            }
        }
    }

    fun stop() {
        job?.cancel()
    }
}
