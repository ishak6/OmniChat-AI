
package com.omnichat.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ChatConversation(
    val id: String,
    val title: String,
    val messages: List<OpenAiMessage>,
    val providerName: String,
    val profileId: String?,
    val model: String,
    val createdAt: Long,
    val updatedAt: Long
)

object ChatHistoryStore {

    private const val PREFS_NAME = "omnichat_chat_history"
    private const val CONVERSATIONS_KEY = "conversations"
    private const val MAX_CONVERSATIONS = 200
    private const val MAX_MESSAGES_PER_CHAT = 500

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun getAll(context: Context): List<ChatConversation> {
        return try {
            val raw = prefs(context).getString(
                CONVERSATIONS_KEY,
                "[]"
            ) ?: "[]"

            val array = JSONArray(raw)

            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    parseConversation(item)?.let { add(it) }
                }
            }.sortedByDescending { it.updatedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun get(
        context: Context,
        conversationId: String
    ): ChatConversation? {
        return getAll(context).firstOrNull {
            it.id == conversationId
        }
    }

    @Synchronized
    fun create(
        context: Context,
        firstMessage: String = "New conversation",
        providerName: String = "",
        profileId: String? = null,
        model: String = ""
    ): ChatConversation? {
        val now = System.currentTimeMillis()
        val title = makeTitle(firstMessage)

        val conversation = ChatConversation(
            id = UUID.randomUUID().toString(),
            title = title,
            messages = emptyList(),
            providerName = providerName,
            profileId = profileId,
            model = model,
            createdAt = now,
            updatedAt = now
        )

        return if (save(context, conversation)) conversation else null
    }

    @Synchronized
    fun save(
        context: Context,
        conversation: ChatConversation
    ): Boolean {
        return try {
            require(conversation.id.isNotBlank())
            require(conversation.title.isNotBlank())
            require(conversation.messages.size <= MAX_MESSAGES_PER_CHAT)

            val conversations = getAll(context).toMutableList()
            val index = conversations.indexOfFirst {
                it.id == conversation.id
            }

            val updated = conversation.copy(
                updatedAt = System.currentTimeMillis()
            )

            if (index >= 0) {
                conversations[index] = updated
            } else {
                conversations.add(updated)
            }

            val limited = conversations
                .sortedByDescending { it.updatedAt }
                .take(MAX_CONVERSATIONS)

            persist(context, limited)
        } catch (_: Exception) {
            false
        }
    }

    @Synchronized
    fun addMessage(
        context: Context,
        conversationId: String,
        message: OpenAiMessage
    ): Boolean {
        val conversation = get(context, conversationId) ?: return false

        if (conversation.messages.size >= MAX_MESSAGES_PER_CHAT) {
            return false
        }

        val updatedMessages = conversation.messages + message
        val updatedTitle = if (
            conversation.title == "New conversation" &&
            message.role == "user"
        ) {
            makeTitle(message.content)
        } else {
            conversation.title
        }

        return save(
            context,
            conversation.copy(
                title = updatedTitle,
                messages = updatedMessages
            )
        )
    }

    @Synchronized
    fun replaceMessages(
        context: Context,
        conversationId: String,
        messages: List<OpenAiMessage>
    ): Boolean {
        val conversation = get(context, conversationId) ?: return false
        if (messages.size > MAX_MESSAGES_PER_CHAT) return false

        return save(
            context,
            conversation.copy(messages = messages)
        )
    }

    @Synchronized
    fun updateAi(
        context: Context,
        conversationId: String,
        providerName: String,
        profileId: String?,
        model: String
    ): Boolean {
        val conversation = get(context, conversationId) ?: return false

        return save(
            context,
            conversation.copy(
                providerName = providerName,
                profileId = profileId,
                model = model
            )
        )
    }

    @Synchronized
    fun rename(
        context: Context,
        conversationId: String,
        newTitle: String
    ): Boolean {
        if (newTitle.isBlank()) return false

        val conversation = get(context, conversationId) ?: return false

        return save(
            context,
            conversation.copy(title = newTitle.trim())
        )
    }

    @Synchronized
    fun delete(
        context: Context,
        conversationId: String
    ): Boolean {
        val conversations = getAll(context).toMutableList()
        val removed = conversations.removeAll {
            it.id == conversationId
        }

        if (!removed) return false
        return persist(context, conversations)
    }

    @Synchronized
    fun clearAll(context: Context): Boolean {
        return prefs(context).edit()
            .remove(CONVERSATIONS_KEY)
            .commit()
    }

    fun buildContextForAnotherAi(
        conversation: ChatConversation,
        mode: MemoryTransferMode
    ): List<OpenAiMessage> {
        return when (mode) {
            MemoryTransferMode.FULL_HISTORY -> {
                conversation.messages
            }

            MemoryTransferMode.SUMMARY -> {
                val summary = createConversationSummary(conversation)

                listOf(
                    OpenAiMessage(
                        "system",
                        "The following is context from a previous AI conversation. " +
                            "Continue helping the user while preserving relevant context.\n\n" +
                            summary
                    )
                )
            }

            MemoryTransferMode.SAVED_MEMORY -> {
                val userMessages = conversation.messages
                    .filter { it.role == "user" }
                    .takeLast(10)

                listOf(
                    OpenAiMessage(
                        "system",
                        "Continue this conversation using the following " +
                            "recent user context. Ask for clarification if needed.\n\n" +
                            userMessages.joinToString("\n") { it.content }
                    )
                )
            }
        }
    }

    private fun createConversationSummary(
        conversation: ChatConversation
    ): String {
        val transcript = conversation.messages.joinToString("\n") {
            val speaker = if (it.role == "user") "User" else "Assistant"
            "$speaker: ${it.content}"
        }

        return buildString {
            appendLine("Conversation title: ${conversation.title}")
            appendLine("Previous AI: ${conversation.providerName}")
            appendLine("Previous model: ${conversation.model}")
            appendLine()
            appendLine("Conversation transcript:")
            append(transcript.takeLast(12000))
        }
    }

    private fun makeTitle(text: String): String {
        val cleaned = text
            .replace("\n", " ")
            .trim()
            .ifBlank { "New conversation" }

        return if (cleaned.length > 48) {
            cleaned.take(45).trimEnd() + "..."
        } else {
            cleaned
        }
    }

    private fun parseConversation(
        item: JSONObject
    ): ChatConversation? {
        return try {
            val id = item.optString("id")
            if (id.isBlank()) return null

            val messageArray = item.optJSONArray("messages") ?: JSONArray()
            val messages = buildList {
                for (i in 0 until messageArray.length()) {
                    val message = messageArray.optJSONObject(i) ?: continue
                    val role = message.optString("role")
                    val content = message.optString("content")

                    if (
                        (role == "user" || role == "assistant" || role == "system") &&
                        content.isNotBlank()
                    ) {
                        add(OpenAiMessage(role, content))
                    }
                }
            }

            ChatConversation(
                id = id,
                title = item.optString("title", "New conversation"),
                messages = messages,
                providerName = item.optString("providerName"),
                profileId = item.optString("profileId")
                    .takeIf { it.isNotBlank() },
                model = item.optString("model"),
                createdAt = item.optLong("createdAt"),
                updatedAt = item.optLong("updatedAt")
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun persist(
        context: Context,
        conversations: List<ChatConversation>
    ): Boolean {
        val array = JSONArray()

        conversations.forEach { conversation ->
            val messagesArray = JSONArray()

            conversation.messages.forEach { message ->
                messagesArray.put(
                    JSONObject().apply {
                        put("role", message.role)
                        put("content", message.content)
                    }
                )
            }

            array.put(
                JSONObject().apply {
                    put("id", conversation.id)
                    put("title", conversation.title)
                    put("messages", messagesArray)
                    put("providerName", conversation.providerName)
                    put("profileId", conversation.profileId ?: "")
                    put("model", conversation.model)
                    put("createdAt", conversation.createdAt)
                    put("updatedAt", conversation.updatedAt)
                }
            )
        }

        return prefs(context).edit()
            .putString(CONVERSATIONS_KEY, array.toString())
            .commit()
    }
}

enum class MemoryTransferMode {
    FULL_HISTORY,
    SUMMARY,
    SAVED_MEMORY
}
