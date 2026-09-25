package com.chatglobal.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatglobal.app.data.model.Message
import com.chatglobal.app.data.model.OnlineUser
import com.chatglobal.app.data.model.User
import com.chatglobal.app.data.repository.AuthRepository
import com.chatglobal.app.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val loading: Boolean = true,
    val connected: Boolean = false,
    val reconnecting: Boolean = false,
    val currentUser: User? = null,
    val messages: List<Message> = emptyList(),
    val onlineUsers: List<OnlineUser> = emptyList(),
    val onlineCount: Int = 0,
    val typingUsers: List<String> = emptyList(),
    val inputText: String = "",
    val canSend: Boolean = false,
    val error: String? = null,
    val showScrollToBottom: Boolean = false
)

class ChatViewModel(
    private val authRepo: AuthRepository,
    private val chatRepo: ChatRepository
) : ViewModel() {

    private val _input = MutableStateFlow("")
    private val _showScrollToBottom = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _typingUsers = MutableStateFlow<List<String>>(emptyList())
    private val _onlineCount = MutableStateFlow(0)

    @Volatile
    private var cachedUser: User? = null

    private var typingJob: Job? = null
    private var typingSent = false
    private var isAtBottom = true

    init {
        viewModelScope.launch {
            cachedUser = authRepo.getCurrentUser()
            val token = authRepo.getToken()
            if (token != null) chatRepo.connect(token)
        }

        viewModelScope.launch {
            authRepo.userFlow.collect { user ->
                cachedUser = user
            }
        }

        observeSocketExtras()
    }

    private fun observeSocketExtras() {
        viewModelScope.launch {
            chatRepo.welcomeEvents.collect { event ->
                _onlineCount.value = event.onlineCount
            }
        }

        viewModelScope.launch {
            chatRepo.onlineUsersEvents.collect { event ->
                _onlineCount.value = event.count
            }
        }

        viewModelScope.launch {
            chatRepo.typingEvents.collect { event ->
                val myName = cachedUser?.name
                _typingUsers.value = event.users.filter { it != myName }
            }
        }

        viewModelScope.launch {
            chatRepo.errorEvents.collect { event ->
                _error.value = event.message
            }
        }

        viewModelScope.launch {
            chatRepo.newMessages.collect {
                if (!isAtBottom) _showScrollToBottom.value = true
            }
        }
    }

    val state: StateFlow<ChatUiState> = combine(
        authRepo.userFlow,
        _input,
        chatRepo.messagesFlow,
        chatRepo.onlineUsersFlow,
        _onlineCount,
        _typingUsers,
        _showScrollToBottom,
        _error,
        chatRepo.connected
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val user = values[0] as User?
        val input = values[1] as String
        val messages = values[2] as List<Message>
        val online = values[3] as List<OnlineUser>
        val count = values[4] as Int
        val typing = values[5] as List<String>
        val scrollBtn = values[6] as Boolean
        val err = values[7] as String?
        val isConnected = values[8] as Boolean

        ChatUiState(
            loading = false,
            connected = isConnected,
            reconnecting = !isConnected && user != null,
            currentUser = user,
            messages = messages,
            onlineUsers = online,
            onlineCount = count,
            typingUsers = typing,
            inputText = input,
            canSend = input.isNotBlank() && isConnected,
            error = err,
            showScrollToBottom = scrollBtn
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState()
    )

    fun onInputChange(text: String) {
        if (text.length > 500) return
        _input.value = text

        if (text.isNotBlank() && !typingSent) {
            chatRepo.startTyping()
            typingSent = true
        }

        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(2000)
            if (typingSent) {
                chatRepo.stopTyping()
                typingSent = false
            }
        }
    }

    fun sendMessage() {
        val text = _input.value.trim()
        if (text.isBlank()) return

        val user = cachedUser ?: return

        chatRepo.sendMessage(
            content = text,
            currentUserId = user.id,
            currentUserName = user.name,
            currentUsername = user.username
        )

        _input.value = ""

        if (typingSent) {
            chatRepo.stopTyping()
            typingSent = false
        }
    }

    fun onScrollChanged(atBottom: Boolean) {
        isAtBottom = atBottom
        if (atBottom) _showScrollToBottom.value = false
    }

    fun onScrollToBottomClicked() {
        _showScrollToBottom.value = false
    }

    fun clearError() {
        _error.value = null
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            chatRepo.disconnect()
            chatRepo.clearLocalCache()
            authRepo.logout()
            onDone()
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatRepo.disconnect()
    }
}
