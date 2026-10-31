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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OmniChatApp() }
    }
}

private enum class MainTab(val label: String) {
    HOME("Home"),
    CHAT("Chat"),
    APIS("APIs"),
    SETTINGS("Settings")
}

private fun isOpenRouterProfile(profile: ApiProfile): Boolean {
    return profile.provider.contains("openrouter", ignoreCase = true) ||
        profile.baseUrl.contains("openrouter.ai", ignoreCase = true)
}

@Composable
fun OmniChatApp() {
    var tab by remember { mutableStateOf(MainTab.HOME) }
    var darkMode by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = if (darkMode) darkColorScheme() else lightColorScheme()
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    MainTab.values().forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = {
                                Icon(
                                    when (item) {
                                        MainTab.HOME -> Icons.Default.Home
                                        MainTab.CHAT -> Icons.Default.Chat
                                        MainTab.APIS -> Icons.Default.Key
                                        MainTab.SETTINGS -> Icons.Default.Settings
                                    },
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
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

                when (tab) {
                    MainTab.HOME -> HomeScreen {
                        tab = MainTab.CHAT
                    }

                    MainTab.CHAT -> ChatScreen()
                    MainTab.APIS -> ApiDashboardScreen()

                    MainTab.SETTINGS -> SettingsScreen(
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
            .padding(horizontal = 20.dp, vertical = 14.dp),
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
                "OmniChat AI",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "One interface, many minds",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomeScreen(onChat: () -> Unit) {
    val context = LocalContext.current
    val profiles = remember { mutableStateListOf<ApiProfile>() }

    LaunchedEffect(Unit) {
        profiles.clear()
        profiles.addAll(ApiProfileStore.getAll(context))
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Your AI workspace",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Manage your AI providers and conversations.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Icon(
                        Icons.Default.Chat,
                        modifier = Modifier.size(34.dp),
                        contentDescription = null
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        "Start a conversation",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text("Chat with your configured AI APIs.")

                    Button(
                        onClick = onChat,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text("Open Chat")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
        }

        item {
            Text(
                "API Overview",
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Configured APIs",
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        "${profiles.size} profiles",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        "${profiles.count { it.enabled }} enabled",
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        "Default: ${
                            profiles.firstOrNull { it.isDefault }?.name
                                ?: "Not configured"
                        }"
                    )
                }
            }
        }

        item {
            Text(
                "Recent conversations",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(
            ChatHistoryStore.getAll(context).take(5),
            key = { it.id }
        ) { conversation ->
            Card(
                onClick = onChat,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        conversation.title,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        "${conversation.messages.size} messages • ${conversation.providerName}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiDashboardScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var refresh by remember { mutableIntStateOf(0) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ApiProfile?>(null) }
    var showImport by remember { mutableStateOf(false) }
    var showOpenRouterSetup by remember { mutableStateOf(false) }

    var importText by remember { mutableStateOf("") }
    var openRouterKey by remember { mutableStateOf("") }
    var showOpenRouterKey by remember { mutableStateOf(false) }

    var status by remember { mutableStateOf("") }
    var testingId by remember { mutableStateOf<String?>(null) }

    val profiles = remember(refresh) {
        ApiProfileStore.getAll(context)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "API Management",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "${profiles.size} configured profiles",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { refresh++ }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }
        }

        // OpenRouter setup shortcut
        OutlinedButton(
            onClick = {
                openRouterKey = ""
                showOpenRouterSetup = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add OpenRouter Free Models")
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    editing = null
                    showEditor = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(" Add API")
            }

            OutlinedButton(
                onClick = { showImport = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Text(" Import")
            }

            OutlinedButton(
                onClick = {
                    val export = JSONArray()

                    profiles.forEach { profile ->
                        export.put(
                            JSONObject().apply {
                                put("name", profile.name)
                                put("provider", profile.provider)
                                put("baseUrl", profile.baseUrl)
                                put("model", profile.model)
                                put("enabled", profile.enabled)
                            }
                        )
                    }

                    clipboard.setText(
                        AnnotatedString(export.toString(2))
                    )

                    status =
                        "Profile metadata copied. API keys are not included."
                }
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Export"
                )
            }
        }

        if (status.isNotBlank()) {
            Text(
                status,
                Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (profiles.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No API profiles yet. Tap Add API or Add OpenRouter Free Models to get started."
                )
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(profiles, key = { it.id }) { profile ->
                    ApiProfileCard(
                        profile = profile,
                        testing = testingId == profile.id,

                        onEnabled = { enabled ->
                            ApiProfileStore.setEnabled(
                                context,
                                profile.id,
                                enabled
                            )
                            refresh++
                        },

                        onDefault = {
                            ApiProfileStore.setDefault(
                                context,
                                profile.id
                            )
                            refresh++
                        },

                        onEdit = {
                            editing = profile
                            showEditor = true
                        },

                        onDelete = {
                            ApiProfileStore.delete(
                                context,
                                profile.id
                            )
                            refresh++
                        },

                        onTest = {
                            val key = ApiProfileStore.getApiKey(
                                context,
                                profile.id
                            )

                            if (key.isNullOrBlank()) {
                                status =
                                    "API key missing for ${profile.name}."
                            } else {
                                testingId = profile.id
                                status = "Testing ${profile.name}..."

                                scope.launch {
                                    try {
                                        val response =
                                            withContext(Dispatchers.IO) {
                                                CustomApiChatClient.sendMessage(
                                                    key,
                                                    CustomApiConfig(
                                                        profile.baseUrl,
                                                        profile.model
                                                    ),
                                                    listOf(
                                                        OpenAiMessage(
                                                            "user",
                                                            "Reply with OK."
                                                        )
                                                    )
                                                )
                                            }

                                        status =
                                            "Connection successful: ${response.take(100)}"
                                    } catch (e: Exception) {
                                        status =
                                            "Connection failed: ${e.message}"
                                    } finally {
                                        testingId = null
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // OpenRouter setup dialog
    if (showOpenRouterSetup) {
        AlertDialog(
            onDismissRequest = {
                showOpenRouterSetup = false
            },
            title = {
                Text("Connect OpenRouter")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Enter your OpenRouter API key. It will be stored using the app's encrypted API key storage."
                    )

                    OutlinedTextField(
                        value = openRouterKey,
                        onValueChange = { openRouterKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("OpenRouter API Key") },
                        placeholder = { Text("sk-or-v1-...") },
                        singleLine = true,
                        visualTransformation =
                            if (showOpenRouterKey) {
                                androidx.compose.ui.text.input.VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    showOpenRouterKey = !showOpenRouterKey
                                }
                            ) {
                                Icon(
                                    if (showOpenRouterKey) {
                                        Icons.Default.VisibilityOff
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                    contentDescription = "Toggle key visibility"
                                )
                            }
                        }
                    )

                    Text(
                        "Base URL: https://openrouter.ai/api/v1",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        "Initial model: openrouter/free",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = openRouterKey.isNotBlank(),
                    onClick = {
                        val result = ApiProfileStore.create(
                            context = context,
                            name = "OpenRouter",
                            provider = "OpenRouter",
                            baseUrl = "https://openrouter.ai/api/v1",
                            model = "openrouter/free",
                            apiKey = openRouterKey.trim(),
                            enabled = true
                        )

                        status = if (result != null) {
                            "OpenRouter profile created successfully."
                        } else {
                            "Could not create OpenRouter profile."
                        }

                        openRouterKey = ""
                        showOpenRouterSetup = false
                        refresh++
                    }
                ) {
                    Text("Save OpenRouter")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showOpenRouterSetup = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditor) {
        ApiProfileEditor(
            profile = editing,
            onDismiss = {
                showEditor = false
            },
            onSave = { name, provider, url, model, key ->
                val result = if (editing == null) {
                    ApiProfileStore.create(
                        context,
                        name,
                        provider,
                        url,
                        model,
                        key
                    ) != null
                } else {
                    ApiProfileStore.save(
                        context,
                        editing!!.copy(
                            name = name,
                            provider = provider,
                            baseUrl = url,
                            model = model
                        ),
                        key.ifBlank { null }
                    )
                }

                status = if (result) {
                    "API profile saved."
                } else {
                    "Could not save. Check fields and API key."
                }

                showEditor = false
                refresh++
            }
        )
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = {
                showImport = false
            },
            title = {
                Text("Import API Profiles")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Paste a JSON array. Each entry must include name, baseUrl, model and apiKey."
                    )

                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        maxLines = 9,
                        label = { Text("JSON") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val result = ApiProfileStore.importJson(
                            context,
                            importText
                        )

                        status =
                            "Imported: ${result.imported}, skipped: ${result.skipped}. " +
                                result.errors.take(2).joinToString(" ")

                        importText = ""
                        showImport = false
                        refresh++
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImport = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ApiProfileCard(
    profile: ApiProfile,
    testing: Boolean,
    onEnabled: (Boolean) -> Unit,
    onDefault: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        profile.name,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "${profile.provider} • ${profile.model}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Switch(
                    checked = profile.enabled,
                    onCheckedChange = onEnabled
                )
            }

            Text(
                profile.baseUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (profile.isDefault) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Default API") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null
                            )
                        }
                    )
                } else {
                    TextButton(
                        onClick = onDefault,
                        enabled = profile.enabled
                    ) {
                        Text("Set Default")
                    }
                }

                Spacer(Modifier.weight(1f))

                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Text("Edit")
                }

                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }

            OutlinedButton(
                onClick = onTest,
                enabled = profile.enabled && !testing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (testing) {
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
                    if (testing) "Testing..." else "Test Connection"
                )
            }
        }
    }
}

@Composable
private fun ApiProfileEditor(
    profile: ApiProfile?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var provider by remember { mutableStateOf(profile?.provider ?: "Custom") }
    var url by remember { mutableStateOf(profile?.baseUrl ?: "") }
    var model by remember { mutableStateOf(profile?.model ?: "") }
    var key by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (profile == null) {
                    "Add API Profile"
                } else {
                    "Edit API Profile"
                }
            )
        },
        text = {
            Column(
                Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text("Profile name") },
                    singleLine = true
                )

                OutlinedTextField(
                    provider,
                    { provider = it },
                    label = { Text("Provider name") },
                    singleLine = true
                )

                OutlinedTextField(
                    url,
                    { url = it },
                    label = { Text("API Base URL") },
                    placeholder = {
                        Text("https://api.example.com/v1")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    model,
                    { model = it },
                    label = { Text("Model ID") },
                    singleLine = true
                )

                OutlinedTextField(
                    key,
                    { key = it },
                    label = {
                        Text(
                            if (profile == null) {
                                "API Key"
                            } else {
                                "New API Key (optional)"
                            }
                        )
                    },
                    visualTransformation =
                        if (showKey) {
                            androidx.compose.ui.text.input.VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                    trailingIcon = {
                        IconButton(
                            onClick = { showKey = !showKey }
                        ) {
                            Icon(
                                if (showKey) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = "Toggle key visibility"
                            )
                        }
                    },
                    singleLine = true
                )

                Text(
                    "Compatible with OpenAI-style /chat/completions endpoints.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() &&
                    url.isNotBlank() &&
                    model.isNotBlank() &&
                    (profile != null || key.isNotBlank()),
                onClick = {
                    onSave(
                        name.trim(),
                        provider.trim(),
                        url.trim().trimEnd('/'),
                        model.trim(),
                        key.trim()
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ChatScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var refresh by remember { mutableIntStateOf(0) }

    var selectedProfileId by remember {
        mutableStateOf(ApiProfileStore.getDefault(context)?.id)
    }

    var conversationId by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf(emptyList<OpenAiMessage>()) }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    var showProfiles by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showMemory by remember { mutableStateOf(false) }

    var memoryMode by remember {
        mutableStateOf(MemoryTransferMode.FULL_HISTORY)
    }

    // OpenRouter model picker state
    var openRouterModels by remember {
        mutableStateOf<List<OpenRouterModel>>(emptyList())
    }

    var loadingOpenRouterModels by remember {
        mutableStateOf(false)
    }

    var showOpenRouterModels by remember {
        mutableStateOf(false)
    }

    var openRouterModelError by remember {
        mutableStateOf("")
    }

    val profiles = remember(refresh) {
        ApiProfileStore.getAll(context).filter {
            it.enabled && ApiProfileStore.hasApiKey(context, it.id)
        }
    }

    val selectedProfile = profiles.firstOrNull {
        it.id == selectedProfileId
    } ?: profiles.firstOrNull()

    LaunchedEffect(selectedProfile?.id) {
        if (selectedProfile != null) {
            selectedProfileId = selectedProfile.id
        }
    }

    fun newConversation() {
        val profile = selectedProfile

        val conversation = ChatHistoryStore.create(
            context,
            providerName = profile?.provider ?: "",
            profileId = profile?.id,
            model = profile?.model ?: ""
        )

        conversationId = conversation?.id
        messages = emptyList()
        error = ""
    }

    fun openConversation(id: String) {
        val conversation = ChatHistoryStore.get(context, id) ?: return

        conversationId = id
        messages = conversation.messages

        if (
            conversation.profileId != null &&
            profiles.any { it.id == conversation.profileId }
        ) {
            selectedProfileId = conversation.profileId
        }

        error = ""
    }

    fun loadOpenRouterModels() {
        if (loadingOpenRouterModels) return

        loadingOpenRouterModels = true
        openRouterModelError = ""
        showOpenRouterModels = true

        scope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    OpenRouterModelsClient.getModels()
                }

                openRouterModels = loaded

                if (loaded.isEmpty()) {
                    openRouterModelError =
                        "No free text models were returned by OpenRouter."
                }
            } catch (e: Exception) {
                openRouterModelError =
                    e.message ?: "Could not load OpenRouter models."
            } finally {
                loadingOpenRouterModels = false
            }
        }
    }

    fun selectOpenRouterModel(model: OpenRouterModel) {
        val profile = selectedProfile ?: return

        val updated = profile.copy(model = model.id)

        val saved = ApiProfileStore.save(
            context,
            updated,
            null
        )

        if (saved) {
            selectedProfileId = updated.id

            conversationId?.let { id ->
                ChatHistoryStore.updateAi(
                    context,
                    id,
                    updated.provider,
                    updated.id,
                    updated.model
                )
            }

            refresh++
            showOpenRouterModels = false
            error = ""
        } else {
            openRouterModelError =
                "Could not save the selected model."
        }
    }

    fun send() {
        val text = input.trim()
        val profile = selectedProfile

        if (text.isBlank() || sending) return

        if (profile == null) {
            error =
                "No enabled API profile with a saved key. Add one in APIs."
            return
        }

        val key = ApiProfileStore.getApiKey(
            context,
            profile.id
        )

        if (key.isNullOrBlank()) {
            error = "API key not found. Check API settings."
            return
        }

        var id = conversationId

        if (id == null) {
            val created = ChatHistoryStore.create(
                context,
                text,
                profile.provider,
                profile.id,
                profile.model
            )

            id = created?.id
            conversationId = id
        }

        val currentId = id ?: run {
            error = "Could not create conversation."
            return
        }

        val conversation = ChatHistoryStore.get(
            context,
            currentId
        )

        val requestHistory = if (conversation == null) {
            messages
        } else {
            ChatHistoryStore.buildContextForAnotherAi(
                conversation,
                memoryMode
            )
        }

        val requestMessages =
            requestHistory + OpenAiMessage("user", text)

        input = ""
        sending = true
        error = ""

        messages = messages + OpenAiMessage("user", text)

        scope.launch {
            try {
                val answer = withContext(Dispatchers.IO) {
                    CustomApiChatClient.sendMessage(
                        key,
                        CustomApiConfig(
                            profile.baseUrl,
                            profile.model
                        ),
                        requestMessages
                    )
                }

                val assistantMessage =
                    OpenAiMessage("assistant", answer)

                messages = messages + assistantMessage

                ChatHistoryStore.addMessage(
                    context,
                    currentId,
                    OpenAiMessage("user", text)
                )

                ChatHistoryStore.addMessage(
                    context,
                    currentId,
                    assistantMessage
                )

                ChatHistoryStore.updateAi(
                    context,
                    currentId,
                    profile.provider,
                    profile.id,
                    profile.model
                )

                refresh++
            } catch (e: Exception) {
                error = e.message ?: "Request failed."
            } finally {
                sending = false
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "AI Chat",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    selectedProfile?.let {
                        "${it.name} • ${it.model}"
                    } ?: "No API selected",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = { showHistory = true }) {
                Icon(
                    Icons.Default.History,
                    contentDescription = "History"
                )
            }

            IconButton(onClick = { showMemory = true }) {
                Icon(
                    Icons.Default.Memory,
                    contentDescription = "Memory"
                )
            }

            IconButton(
                onClick = { newConversation() },
                enabled = !sending
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "New chat"
                )
            }
        }

        OutlinedButton(
            onClick = { showProfiles = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = !sending
        ) {
            Icon(
                Icons.Default.SwapHoriz,
                contentDescription = null
            )

            Spacer(Modifier.width(8.dp))

            Text(
                "Switch AI: ${selectedProfile?.name ?: "Select API"}"
            )

            Spacer(Modifier.weight(1f))

            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }

        // Show model picker only for OpenRouter profiles
        if (selectedProfile != null &&
            isOpenRouterProfile(selectedProfile)
        ) {
            OutlinedButton(
                onClick = { loadOpenRouterModels() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                enabled = !sending && !loadingOpenRouterModels
            ) {
                if (loadingOpenRouterModels) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.ViewList,
                        contentDescription = null
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    if (loadingOpenRouterModels) {
                        "Loading Free Models..."
                    } else {
                        "OpenRouter Free Models"
                    }
                )

                Spacer(Modifier.weight(1f))

                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
        }

        Text(
            "Memory: ${memoryMode.name.replace('_', ' ')}",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(vertical = 5.dp)
        )

        HorizontalDivider()

        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Text(
                        "Start a new conversation. Your messages will be saved on this device.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            items(messages) { message ->
                ChatMessageBubble(message)
            }

            if (sending) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )

                        Spacer(Modifier.width(10.dp))

                        Text("AI is responding...")
                    }
                }
            }
        }

        if (error.isNotBlank()) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message OmniChat...") },
                maxLines = 5,
                enabled = !sending
            )

            Spacer(Modifier.width(8.dp))

            FilledIconButton(
                onClick = { send() },
                enabled = input.isNotBlank() && !sending
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send"
                )
            }
        }
    }

    // AI profile selection
    if (showProfiles) {
        AlertDialog(
            onDismissRequest = {
                showProfiles = false
            },
            title = {
                Text("Choose an AI")
            },
            text = {
                LazyColumn(
                    Modifier.heightIn(max = 420.dp)
                ) {
                    items(
                        profiles,
                        key = { it.id }
                    ) { profile ->
                        TextButton(
                            onClick = {
                                selectedProfileId = profile.id

                                conversationId?.let { id ->
                                    ChatHistoryStore.updateAi(
                                        context,
                                        id,
                                        profile.provider,
                                        profile.id,
                                        profile.model
                                    )
                                }

                                showProfiles = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    profile.name,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    profile.model,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (profile.id == selectedProfileId) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showProfiles = false }
                ) {
                    Text("Close")
                }
            }
        )
    }

    // OpenRouter Free Models dialog
    if (showOpenRouterModels) {
        AlertDialog(
            onDismissRequest = {
                showOpenRouterModels = false
            },
            title = {
                Text("OpenRouter Free Models")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Choose a free model. The selection will be saved to your OpenRouter profile.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (loadingOpenRouterModels) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.width(12.dp))
                            Text("Loading models...")
                        }
                    }

                    if (openRouterModelError.isNotBlank()) {
                        Text(
                            openRouterModelError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )

                        TextButton(
                            onClick = {
                                loadOpenRouterModels()
                            }
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null
                            )
                            Text(" Retry")
                        }
                    }

                    LazyColumn(
                        Modifier.heightIn(max = 420.dp)
                    ) {
                        items(
                            openRouterModels,
                            key = { it.id }
                        ) { model ->
                            val selected =
                                selectedProfile?.model == model.id

                            TextButton(
                                onClick = {
                                    selectOpenRouterModel(model)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    Modifier.weight(1f)
                                ) {
                                    Text(
                                        model.name,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Text(
                                        model.id,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                if (selected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected"
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOpenRouterModels = false
                    }
                ) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        loadOpenRouterModels()
                    },
                    enabled = !loadingOpenRouterModels
                ) {
                    Text("Refresh")
                }
            }
        )
    }

    // Chat history dialog
    if (showHistory) {
        AlertDialog(
            onDismissRequest = {
                showHistory = false
            },
            title = {
                Text("Chat History")
            },
            text = {
                val history = ChatHistoryStore.getAll(context)

                LazyColumn(
                    Modifier.heightIn(max = 440.dp)
                ) {
                    if (history.isEmpty()) {
                        item {
                            Text("No saved conversations.")
                        }
                    }

                    items(
                        history,
                        key = { it.id }
                    ) { conversation ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    openConversation(conversation.id)
                                    showHistory = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(
                                        conversation.title,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Text(
                                        "${conversation.messages.size} messages • ${conversation.providerName}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    ChatHistoryStore.delete(
                                        context,
                                        conversation.id
                                    )
                                    refresh++
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete"
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showHistory = false }
                ) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        ChatHistoryStore.clearAll(context)

                        if (conversationId != null) {
                            conversationId = null
                            messages = emptyList()
                        }

                        showHistory = false
                    }
                ) {
                    Text("Clear All")
                }
            }
        )
    }

    // Conversation memory dialog
    if (showMemory) {
        AlertDialog(
            onDismissRequest = {
                showMemory = false
            },
            title = {
                Text("Conversation Memory")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Choose what context to send when switching AI."
                    )

                    MemoryTransferMode.values().forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = memoryMode == mode,
                                onClick = {
                                    memoryMode = mode
                                }
                            )

                            Column {
                                Text(
                                    mode.name.replace('_', ' ')
                                )

                                Text(
                                    when (mode) {
                                        MemoryTransferMode.FULL_HISTORY ->
                                            "Send all previous messages."

                                        MemoryTransferMode.SUMMARY ->
                                            "Send a compact transcript excerpt. This is not AI-generated summarization."

                                        MemoryTransferMode.SAVED_MEMORY ->
                                            "Send the latest 10 user messages."
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Text(
                        "Messages selected here are sent to the chosen API provider.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showMemory = false }
                ) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun ChatMessageBubble(message: OpenAiMessage) {
    val user = message.role == "user"

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (user) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Card(
            Modifier.widthIn(max = 330.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (user) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(Modifier.padding(13.dp)) {
                Text(
                    if (user) "You" else "AI",
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
private fun SettingsScreen(
    darkMode: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current

    val historyCount = remember {
        ChatHistoryStore.getAll(context).size
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DarkMode,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(12.dp))

                    Text(
                        "Dark mode",
                        Modifier.weight(1f)
                    )

                    Switch(
                        checked = darkMode,
                        onCheckedChange = onThemeChange
                    )
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Security & Privacy",
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "API keys are encrypted with Android Keystore. Profile exports exclude API keys."
                    )

                    Text(
                        "Chat history is stored locally on this device. When you send a message, selected conversation context is transmitted to your chosen API provider."
                    )
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Local Data",
                        fontWeight = FontWeight.Bold
                    )

                    Text("$historyCount saved conversations")

                    Text(
                        "Conversation history is not encrypted separately by this version."
                    )
                }
            }
        }

        item {
            Text(
                "OmniChat AI • Version 0.2.0",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
