
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
        setContent { OmniChatApp() }
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
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
            ) {
                AppHeader()

                when (selectedTab) {
                    0 -> HomeScreen { selectedTab = 1 }
                    1 -> ChatScreen()
                    2 -> SimpleScreen(
                        "AI Agents",
                        "Your personal AI assistants will live here.",
                        Icons.Default.Psychology
                    )
                    3 -> SimpleScreen(
                        "AI Tools",
                        "Coding, research, image and data tools will be added here.",
                        Icons.Default.Build
                    )
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
        Modifier
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
            Text("OmniChat", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                "AI • One interface, many minds",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
    }
}

@Composable
private fun HomeScreen(onOpenChat: () -> Unit) {
    LazyColumn(
        Modifier
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
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(22.dp)) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Start a conversation",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Talk to your AI models from one place.",
                        Modifier.padding(top = 6.dp)
                    )
                    Button(
                        onClick = onOpenChat,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Open Chat")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
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
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            Modifier.padding(18.dp),
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

    var selectedProvider by remember { mutableStateOf("openrouter") }
    var selectedModel by remember { mutableStateOf("openrouter/free") }
    var providerExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var freeOnly by remember { mutableStateOf(true) }

    var models by remember {
        mutableStateOf(
            listOf(
                OpenRouterModel(
                    "openrouter/free",
                    "Auto Select Free Model",
                    true
                )
            )
        )
    }

    var loadingModels by remember { mutableStateOf(false) }
    var modelError by remember { mutableStateOf("") }

    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var messages by remember {
        mutableStateOf(
            listOf(
                OpenAiMessage(
                    "assistant",
                    "Welcome to OmniChat AI. Select a provider and model."
                )
            )
        )
    }

    val providerName = when (selectedProvider) {
        "openai" -> "OpenAI"
        "openrouter" -> "OpenRouter"
        else -> "Custom Provider"
    }

    fun loadModels() {
        if (loadingModels) return
        loadingModels = true
        modelError = ""

        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    OpenRouterModelsClient.getModels()
                }
                models = result
                if (models.none { it.id == selectedModel }) {
                    selectedModel = "openrouter/free"
                }
            } catch (e: Exception) {
                modelError = e.message ?: "Could not load models."
            } finally {
                loadingModels = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadModels()
    }

    val visibleModels = models.filter { model ->
        (!freeOnly || model.isFree) &&
            (
                model.name.contains(search, ignoreCase = true) ||
                    model.id.contains(search, ignoreCase = true)
                )
    }

    val sendMessage: () -> Unit = {
        val userText = message.trim()

        if (userText.isNotBlank() && !isSending) {
            val key = SecureApiKeyStore.get(context, selectedProvider)

            if (key.isNullOrBlank()) {
                errorMessage =
                    "No API key saved for $providerName. Open Settings and save it first."
            } else {
                val config = when (selectedProvider) {
                    "openrouter" -> CustomApiConfig(
                        "https://openrouter.ai/api/v1",
                        selectedModel
                    )
                    "custom" -> CustomApiConfigStore.get(context)
                    else -> null
                }

                if (selectedProvider != "openai" &&
                    (
                        config == null ||
                            config.baseUrl.isBlank() ||
                            config.model.isBlank()
                        )
                ) {
                    errorMessage = "Configure the API URL and model in Settings."
                } else {
                    message = ""
                    errorMessage = ""
                    isSending = true

                    val requestMessages = messages +
                        OpenAiMessage("user", userText)
                    messages = requestMessages

                    val providerSnapshot = selectedProvider
                    val configSnapshot = config
                    val modelSnapshot = selectedModel

                    scope.launch {
                        try {
                            val answer = withContext(Dispatchers.IO) {
                                when (providerSnapshot) {
                                    "openai" -> OpenAiChatClient.sendMessage(
                                        key,
                                        requestMessages
                                    )
                                    "openrouter" -> CustomApiChatClient.sendMessage(
                                        key,
                                        configSnapshot!!,
                                        requestMessages
                                    )
                                    else -> CustomApiChatClient.sendMessage(
                                        key,
                                        configSnapshot!!,
                                        requestMessages
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
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            Modifier
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
                    if (selectedProvider == "openrouter") {
                        "OpenRouter • $selectedModel"
                    } else if (selectedProvider == "custom") {
                        "Custom • ${CustomApiConfigStore.get(context)?.model ?: "Not configured"}"
                    } else {
                        "OpenAI • gpt-4o-mini"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                OutlinedButton(
                    onClick = { providerExpanded = true },
                    enabled = !isSending
                ) {
                    Text(providerName)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = providerExpanded,
                    onDismissRequest = { providerExpanded = false }
                ) {
                    listOf(
                        "openai" to "OpenAI",
                        "openrouter" to "OpenRouter",
                        "custom" to "Custom Provider"
                    ).forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                selectedProvider = id
                                providerExpanded = false
                                errorMessage = ""
                            }
                        )
                    }
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
                Icon(Icons.Default.Add, contentDescription = "New chat")
            }
        }

        if (selectedProvider == "openrouter") {
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Select AI Model",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = { loadModels() },
                            enabled = !loadingModels
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text("Refresh")
                        }
                    }

                    OutlinedButton(
                        onClick = { modelExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loadingModels && models.isNotEmpty()
                    ) {
                        Text(
                            models.firstOrNull { it.id == selectedModel }?.name
                                ?: selectedModel,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = modelExpanded,
                        onDismissRequest = { modelExpanded = false }
                    ) {
                        Column(
                            Modifier
                                .widthIn(min = 280.dp, max = 340.dp)
                                .heightIn(max = 420.dp)
                                .padding(8.dp)
                        ) {
                            OutlinedTextField(
                                value = search,
                                onValueChange = { search = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("Search models") },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                }
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = freeOnly,
                                    onCheckedChange = { freeOnly = it }
                                )
                                Text("Free models only")
                            }

                            HorizontalDivider()

                            LazyColumn(
                                Modifier.heightIn(max = 300.dp)
                            ) {
                                items(visibleModels) { model ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    model.name,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    model.id,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (model.isFree) {
                                                    Text(
                                                        "FREE",
                                                        color = MaterialTheme.colorScheme.primary,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedModel = model.id
                                            modelExpanded = false
                                            errorMessage = ""
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (loadingModels) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("Loading available models...")
                    }

                    if (modelError.isNotBlank()) {
                        Text(
                            modelError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        "${models.size} chat models loaded",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider()

        LazyColumn(
            Modifier
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("OmniChat is thinking...")
                    }
                }
            }
        }

        if (errorMessage.isNotBlank()) {
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    errorMessage,
                    Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Row(
            Modifier
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
                Icon(Icons.Default.Send, contentDescription = "Send message")
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: OpenAiMessage) {
    val isUser = message.role == "user"

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            Modifier.widthIn(max = 320.dp),
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
                Text(message.content)
            }
        }
    }
}

@Composable
private fun SimpleScreen(
    title: String,
    description: String,
    icon: ImageVector
) {
    Column(
        Modifier
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
            Modifier.padding(top = 8.dp),
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

    var selectedProvider by remember { mutableStateOf("openrouter") }
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
        Modifier
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
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Select Provider", fontWeight = FontWeight.SemiBold)

                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(providerName, Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
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
                                            customBaseUrl = config?.baseUrl ?: ""
                                            customModel = config?.model ?: ""
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
                            placeholder = { Text("https://api.example.com/v1") },
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
                            placeholder = { Text("your-model-name") },
                            singleLine = true
                        )

                        Text(
                            "Endpoint must support OpenAI-compatible /chat/completions.",
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
                                    "Custom API settings saved."
                                } else {
                                    "Enter a valid API URL and Model ID."
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = customBaseUrl.isNotBlank() &&
                                customModel.isNotBlank()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
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
                        placeholder = { Text("Enter your $providerName API key") },
                        visualTransformation = PasswordVisualTransformation(),
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
                                statusMessage = "Could not save API key."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = apiKey.isNotBlank()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save API Key")
                    }

                    if (saved) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "$providerName API key is saved",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (selectedProvider == "custom" ||
                            selectedProvider == "openrouter"
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val key = SecureApiKeyStore.get(
                                        context,
                                        selectedProvider
                                    )

                                    val config = if (selectedProvider == "openrouter") {
                                        CustomApiConfig(
                                            "https://openrouter.ai/api/v1",
                                            "openrouter/free"
                                        )
                                    } else {
                                        CustomApiConfigStore.get(context)
                                    }

                                    if (key.isNullOrBlank() || config == null) {
                                        statusMessage =
                                            "Save the API key and configuration first."
                                    } else {
                                        testingConnection = true
                                        statusMessage = "Testing connection..."

                                        scope.launch {
                                            try {
                                                val response = withContext(Dispatchers.IO) {
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
                                                    "Connection successful: ${response.take(120)}"
                                            } catch (e: Exception) {
                                                statusMessage =
                                                    "Connection failed: ${e.message}"
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
                                    if (testingConnection) "Testing..."
                                    else "Test Connection"
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
                                    statusMessage = "$providerName API key removed."
                                } else {
                                    statusMessage = "Could not remove API key."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
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
                            color = if (statusMessage.startsWith("Connection failed")) {
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
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Security & Privacy", fontWeight = FontWeight.SemiBold)
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
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DarkMode, contentDescription = null)
                    Spacer(Modifier.width(14.dp))
                    Text("Dark mode", Modifier.weight(1f))
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
