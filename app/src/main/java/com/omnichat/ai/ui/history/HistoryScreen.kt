package com.omnichat.ai.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.omnichat.ai.domain.Chat
import com.omnichat.ai.ui.rememberContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onOpen: (Long) -> Unit, onProviders: () -> Unit) {
    val repo = rememberContainer().chatRepository
    val scope = rememberCoroutineScope()
    val chatsFlow = remember { repo.observeChats() }
    val chats by chatsFlow.collectAsState(initial = emptyList<Chat>())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OmniChat AI") },
                actions = { TextButton(onClick = onProviders) { Text("Providers") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scope.launch { onOpen(repo.createChat("New chat")) }
            }) { Text("+") }
        },
    ) { pad ->
        LazyColumn(modifier = Modifier.padding(pad)) {
            items(chats, key = { it.id }) { chat ->
                ListItem(
                    headlineContent = { Text(chat.title, maxLines = 1) },
                    trailingContent = {
                        TextButton(onClick = { scope.launch { repo.deleteChat(chat.id) } }) { Text("Delete") }
                    },
                    modifier = Modifier.clickable { onOpen(chat.id) },
                )
            }
        }
    }
}
