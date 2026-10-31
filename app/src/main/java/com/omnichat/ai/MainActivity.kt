
package com.omnichat.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OmniChatApp()
        }
    }
}

private data class AppTab(
    val title: String,
    val icon: ImageVector
)

@Composable
fun OmniChatApp() {
    var selectedTab by remember { mutableStateOf(0) }
    var darkMode by remember { mutableStateOf(false) }

    val tabs = listOf(
        AppTab("Home", Icons.Default.Home),
        AppTab("Chat", Icons.Default.Chat),
        AppTab("Agents", Icons.Default.AutoAwesome),
        AppTab("Tools", Icons.Default.Build),
        AppTab("Settings", Icons.Default.Settings)
    )

    MaterialTheme(
        colorScheme = if (darkMode) darkColorScheme() else lightColorScheme()
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(tab.icon, contentDescription = tab.title)
                            },
                            label = { Text(tab.title) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
            ) {
                AppHeader()

                when (selectedTab) {
                    0 -> HomeScreen(
                        onOpenChat = { selectedTab = 1 }
                    )
                    1 -> ChatScreen()
                    2 -> AgentsScreen()
                    3 -> ToolsScreen()
                    4 -> SettingsScreen(
                        darkMode = darkMode,
                        onThemeChange = { darkMode = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(30.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = "OmniChat",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "AI • One interface, many minds",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = {}) {
            Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
        }
    }
}

@Composable
private fun HomeScreen(onOpenChat: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Your AI workspace",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "What would you like to do today?",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Start a conversation",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Talk to your AI models from one place.",
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    Button(
                        onClick = onOpenChat,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Open Chat")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null
                        )
                    }
                }
            }
        }

        item {
            Text(
                "Explore OmniChat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            FeatureCard(
                icon = Icons.Default.Psychology,
                title = "AI Agents",
                description = "Create assistants for different tasks."
            )
        }

        item {
            FeatureCard(
                icon = Icons.Default.Code,
                title = "Coding Studio",
                description = "Your future workspace for coding with AI."
            )
        }

        item {
            FeatureCard(
                icon = Icons.Default.Search,
                title = "Deep Research",
                description = "Research and organize information with AI."
            )
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChatScreen() {
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            "AI Chat",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Connect an AI provider to start chatting.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("No AI provider connected yet.")
                Text(
                    "Provider setup and live AI responses will be added in a later step.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Message OmniChat...") },
            shape = RoundedCornerShape(18.dp),
            trailingIcon = {
                IconButton(
                    onClick = { },
                    enabled = message.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send"
                    )
                }
            },
            maxLines = 4
        )
    }
}

@Composable
private fun AgentsScreen() {
    SimpleScreen(
        title = "AI Agents",
        description = "Your personal AI assistants will live here.",
        icon = Icons.Default.Psychology
    )
}

@Composable
private fun ToolsScreen() {
    SimpleScreen(
        title = "AI Tools",
        description = "Coding, research, image and data tools will be added here.",
        icon = Icons.Default.Build
    )
}

@Composable
private fun SimpleScreen(
    title: String,
    description: String,
    icon: ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            description,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsScreen(
    darkMode: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DarkMode,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    "Dark mode",
                    modifier = Modifier.weight(1f)
                )

                Switch(
                    checked = darkMode,
                    onCheckedChange = onThemeChange
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "OmniChat AI • Version 0.1.0",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
