package com.omnichat.ai.domain

import com.omnichat.ai.data.secure.SecureProviderStore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class SendMessageUseCase(
    private val repo: ChatRepository,
    private val client: LlmClient,
    private val providers: SecureProviderStore,
) {
    suspend operator fun invoke(chatId: Long, text: String) {
        val history = repo.getMessages(chatId)
        val userMsg = ChatMessage(chatId = chatId, role = Role.USER, content = text)
        repo.addMessage(chatId, Role.USER, text)
        if (history.isEmpty()) repo.renameChat(chatId, text.take(40))

        val replyId = repo.addMessage(chatId, Role.ASSISTANT, "")
        val provider = providers.active()
        if (provider == null) {
            repo.updateMessage(replyId, "⚠ আগে Providers থেকে একটি API provider যোগ করো।")
            return
        }

        val sb = StringBuilder()
        try {
            client.stream(provider, history + userMsg).collect { chunk ->
                sb.append(chunk)
                repo.updateMessage(replyId, sb.toString())
            }
        } catch (e: CancellationException) {
            throw e // user pressed Stop: partial text stays saved
        } catch (e: Exception) {
            sb.append("\n\n⚠ ${e.message ?: "Unknown error"}")
            withContext(NonCancellable) { repo.updateMessage(replyId, sb.toString()) }
        }
    }
}
