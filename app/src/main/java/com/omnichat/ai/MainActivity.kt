package com.omnichat.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

private data class AIProvider(
    val id: String,
    val name: String
)

private val aiProviders = listOf(
    AIProvider("openai", "OpenAI"),
    AIProvider("gemini", "Google Gemini"),
    AIProvider("claude", "Anthropic Claude"),
    AIProvider("deepseek", "DeepSeek"),
    AIProvider("grok", "xAI Grok"),
    AIProvider("mistral", "Mistral AI"),
    AIProvider("openrouter", "OpenRouter"),
    AIProvider("groq", "Groq"),
    AIProvider("together", "Together AI"),
    AIProvider("custom", "Custom Provider")
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
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(30.dp)
        )

        Spacer(Modifier.width(10.dp))

        Column {
            Text(
                "OmniChat",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "AI • One interface, many minds",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.weight(1f))

        IconButton(onClick = {}) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = "Profile"
            )
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
            Spacer(Modifier.height(8.dp))

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
                Column(Modifier.padding(22.dp)) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(12.dp))

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
                        Spacer(Modifier.width(8.dp))
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
                Icons.Default.Psychology,
                "AI Agents",
                "Create assistants for different tasks."
            )
        }

        item {
            FeatureCard(
                Icons.Default.Code,
                "Coding Studio",
                "Your future workspace for coding with AI."
            )
        }

        item {
            FeatureCard(
                Icons.Default.Search,
                "Deep Research",
                "Research and organize information with AI."
            )
        }

        item {
            Spacer(Modifier.height(12.dp))
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

            Spacer(Modifier.width(16.dp))

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedProvider by remember { mutableStateOf("openai") }
    var providerMenuExpanded by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var messages by remember {
        mutableStateOf(
            listOf(
                OpenAiMessage(
                    "assistant",
                    "Hello! Welcome to OmniChat AI. Select a provider and send a message."
                )
            )
        )
    }

    val activeProviderName = if (selectedProvider == "custom") {
        "Custom Provider"
    } else {
        "OpenAI"
    }

    val sendMessage: () -> Unit = {
        val userMessage = message.trim()

        if (userMessage.isNotBlank() && !isSending) {
            val apiKey = SecureApiKeyStore.get(
                context,
                selectedProvider
            )

            if (apiKey.isNullOrBlank()) {
                errorMessage =
                    "No API key saved for $activeProviderName. Open Settings and save it first."
            } else {
                val customConfig = if (selectedProvider == "custom") {
                    CustomApiConfigStore.get(context)
                } else {
                    null
                }

                if (
                    selectedProvider == "custom" &&
                    (
                        customConfig == null ||
                            customConfig.baseUrl.isBlank() ||
                            customConfig.model.isBlank()
                        )
                ) {
                    errorMessage =
                        "Custom API URL and Model ID are required. Configure them in Settings."
                } else {
                    message = ""
                    errorMessage = ""
                    isSending = true

                    val requestMessages = messages + OpenAiMessage(
                        "user",
                        userMessage
                    )

                    messages = requestMessages

                    scope.launch {
                        try {
                            val answer = withContext(Dispatchers.IO) {
                                if (selectedProvider == "custom") {
                                    CustomApiChatClient.sendMessage(
                                        apiKey = apiKey,
                                        config = customConfig!!,
                                        messages = requestMessages
                                    )
                                } else {
                                    OpenAiChatClient.sendMessage(
                                        apiKey = apiKey,
                                        messages = requestMessages
                                    )
                                }
                            }

                            messages = messages + OpenAiMessage(
                                "assistant",
                                answer
                            )
                        } catch (e: Exception) {
                            errorMessage = e.message
                                ?: "Something went wrong. Please try again."
                        } finally {
                            isSending = false
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "AI Chat",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    if (selectedProvider == "custom") {
                        "Custom • ${CustomApiConfigStore.get(context)?.model ?: "Model not configured"}"
                    } else {
                        "OpenAI • gpt-4o-mini"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                OutlinedButton(
                    onClick = { providerMenuExpanded = true },
                    enabled = !isSending
                ) {
                    Text(activeProviderName)
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Choose provider"
                    )
                }

                DropdownMenu(
                    expanded = providerMenuExpanded,
                    onDismissRequest = { providerMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("OpenAI") },
                        onClick = {
                            selectedProvider = "openai"
                            providerMenuExpanded = false
                            errorMessage = ""
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Custom Provider") },
                        onClick = {
                            selectedProvider = "custom"
                            providerMenuExpanded = false
                            errorMessage = ""
                        }
                    )
                }
            }

            IconButton(
                onClick = {
                    if (!isSending) {
                        messages = listOf(
                            OpenAiMessage(
                                "assistant",
                                "New conversation started. How can I help you?"
                            )
                        )
                        errorMessage = ""
                    }
                },
                enabled = !isSending
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "New chat"
                )
            }
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { chatMessage ->
                ChatMessageBubble(chatMessage)
            }

            if (isSending) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )

                        Spacer(Modifier.width(10.dp))

                        Text(
                            "OmniChat is thinking...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        if (errorMessage.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    errorMessage,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message OmniChat...") },
                shape = RoundedCornerShape(22.dp),
                maxLines = 5,
                enabled = !isSending
            )

            Spacer(Modifier.width(8.dp))

            FilledIconButton(
                onClick = sendMessage,
                enabled = message.isNotBlank() && !isSending,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send message"
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: OpenAiMessage) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Card(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    if (isUser) "You" else "OmniChat AI",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    message.content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun AgentsScreen() {
    SimpleScreen(
        "AI Agents",
        "Your personal AI assistants will live here.",
        Icons.Default.Psychology
    )
}

@Composable
private fun ToolsScreen() {
    SimpleScreen(
        "AI Tools",
        "Coding, research, image and data tools will be added here.",
        Icons.Default.Build
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

        Spacer(Modifier.height(16.dp))

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedProvider by remember { mutableStateOf("openai") }
    var apiKey by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    var saved by remember(selectedProvider) {
        mutableStateOf(
            SecureApiKeyStore.hasKey(context, selectedProvider)
        )
    }

    var statusMessage by remember { mutableStateOf("") }
    var testingConnection by remember { mutableStateOf(false) }

    var customBaseUrl by remember {
        mutableStateOf(
            CustomApiConfigStore.get(context)?.baseUrl ?: ""
        )
    }

    var customModel by remember {
        mutableStateOf(
            CustomApiConfigStore.get(context)?.model ?: ""
        )
    }

    val providerName = aiProviders.first {
        it.id == selectedProvider
    }.name

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(Modifier.height(4.dp))

            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Text(
                "AI Provider Setup",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Connect your AI provider using an API key.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Select Provider",
                        fontWeight = FontWeight.SemiBold
                    )

                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                providerName,
                                modifier = Modifier.weight(1f)
                            )

                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select provider"
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            aiProviders.forEach { provider ->
                                DropdownMenuItem(
                                    text = { Text(provider.name) },
                                    onClick = {
                                        selectedProvider = provider.id
                                        apiKey = ""
                                        statusMessage = ""
                                        expanded = false

                                        if (provider.id == "custom") {
                                            val config =
                                                CustomApiConfigStore.get(context)

                                            customBaseUrl =
                                                config?.baseUrl ?: ""

                                            customModel =
                                                config?.model ?: ""
                                        }
                                    },
                                    leadingIcon = {
                                        if (selectedProvider == provider.id) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (selectedProvider == "custom") {
                        Text(
                            "Custom API Configuration",
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = customBaseUrl,
                            onValueChange = {
                                customBaseUrl = it
                                statusMessage = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("API Base URL") },
                            placeholder = {
                                Text("https://api.example.com/v1")
                            },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customModel,
                            onValueChange = {
                                customModel = it
                                statusMessage = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Model ID") },
                            placeholder = {
                                Text("your-model-name")
                            },
                            singleLine = true
                        )

                        Text(
                            "Endpoint must support the OpenAI-compatible /chat/completions format.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                val result = CustomApiConfigStore.save(
                                    context,
                                    customBaseUrl.trim(),
                                    customModel.trim()
                                )

                                statusMessage = if (result) {
                                    "Custom API URL and Model ID saved."
                                } else {
                                    "Enter a valid http:// or https:// API URL and Model ID."
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = customBaseUrl.isNotBlank() &&
                                customModel.isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = null
                            )

                            Spacer(Modifier.width(8.dp))

                            Text("Save Custom API Settings")
                        }
                    }

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            statusMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API Key") },
                        placeholder = {
                            Text("Enter your $providerName API key")
                        },
                        visualTransformation =
                            PasswordVisualTransformation(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val result = SecureApiKeyStore.save(
                                context,
                                selectedProvider,
                                apiKey.trim()
                            )

                            if (result) {
                                saved = true
                                apiKey = ""
                                statusMessage =
                                    "$providerName API key saved securely."
                            } else {
                                statusMessage =
                                    "Could not save API key. Check the key and try again."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = apiKey.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(8.dp))

                        Text("Save API Key")
                    }

                    if (saved) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(
                                "$providerName API key is saved",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (selectedProvider == "custom") {
                            OutlinedButton(
                                onClick = {
                                    val key = SecureApiKeyStore.get(
                                        context,
                                        "custom"
                                    )

                                    val config =
                                        CustomApiConfigStore.get(context)

                                    if (key.isNullOrBlank() || config == null) {
                                        statusMessage =
                                            "Save the Custom API URL, Model ID and API key first."
                                    } else {
                                        testingConnection = true
                                        statusMessage = "Testing connection..."

                                        scope.launch {
                                            try {
                                                val response =
                                                    withContext(Dispatchers.IO) {
                                                        CustomApiChatClient.sendMessage(
                                                            apiKey = key,
                                                            config = config,
                                                            messages = listOf(
                                                                OpenAiMessage(
                                                                    "user",
                                                                    "Reply with OK."
                                                                )
                                                            )
                                                        )
                                                    }

                                                statusMessage =
                                                    "Connection successful. Model replied: ${response.take(160)}"

                                            } catch (e: Exception) {
                                                statusMessage =
                                                    "Connection failed: ${e.message ?: "Unknown error"}"
                                            } finally {
                                                testingConnection = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !testingConnection
                            ) {
                                if (testingConnection) {
                                    CircularProgressIndicator(
                                        Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.NetworkCheck,
                                        contentDescription = null
                                    )
                                }

                                Spacer(Modifier.width(8.dp))

                                Text(
                                    if (testingConnection) {
                                        "Testing..."
                                    } else {
                                        "Test Connection"
                                    }
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val deleted = SecureApiKeyStore.delete(
                                    context,
                                    selectedProvider
                                )

                                if (deleted) {
                                    saved = false
                                    statusMessage =
                                        "$providerName API key removed."
                                } else {
                                    statusMessage =
                                        "Could not remove API key."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null
                            )

                            Spacer(Modifier.width(8.dp))

                            Text("Remove API Key")
                        }
                    } else {
                        Text(
                            "No API key saved for $providerName.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (statusMessage.isNotBlank()) {
                        Text(
                            statusMessage,
                            color = if (
                                statusMessage.startsWith("Connection failed") ||
                                statusMessage.startsWith("Enter a valid")
                            ) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Security & Privacy",
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        "API keys are encrypted using Android Keystore before being stored on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        "Never share your API keys or commit them to GitHub.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
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

                    Spacer(Modifier.width(14.dp))

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
        }

        item {
            Text(
                "OmniChat AI • Version 0.1.0",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
