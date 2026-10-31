
package com.omnichat.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun OpenRouterModelScanner(
    models: List<OpenRouterModel>,
    apiKey: String,
    selectedModelId: String?,
    loading: Boolean,
    loadError: String,
    onRefresh: () -> Unit,
    onSelectModel: (OpenRouterModel) -> Unit
) {
    val scope = rememberCoroutineScope()

    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }
    var typeFilter by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("Name") }

    var results by remember {
        mutableStateOf<Map<String, OpenRouterModelTestResult>>(emptyMap())
    }

    var runningModelId by remember {
        mutableStateOf<String?>(null)
    }

    val types = remember(models) {
        listOf("All") + models
            .map { it.modelType }
            .distinct()
            .sorted()
    }

    val filteredModels = remember(
        models,
        results,
        search,
        statusFilter,
        typeFilter,
        sortBy
    ) {
        val filtered = models.filter { model ->
            val result = results[model.id]

            val matchesSearch =
                search.isBlank() ||
                model.name.contains(search, ignoreCase = true) ||
                model.id.contains(search, ignoreCase = true)

            val matchesType =
                typeFilter == "All" ||
                model.modelType == typeFilter

            val currentStatus = result?.status
                ?: ModelTestStatus.NOT_TESTED

            val matchesStatus = when (statusFilter) {
                "Working" ->
                    currentStatus == ModelTestStatus.WORKING

                "Failed" ->
                    currentStatus == ModelTestStatus.FAILED

                "Not Tested" ->
                    currentStatus == ModelTestStatus.NOT_TESTED

                else -> true
            }

            matchesSearch && matchesType && matchesStatus
        }

        when (sortBy) {
            "Working First" -> filtered.sortedBy {
                when (results[it.id]?.status) {
                    ModelTestStatus.WORKING -> 0
                    ModelTestStatus.FAILED -> 1
                    else -> 2
                }
            }

            "Recently Tested" -> filtered.sortedByDescending {
                results[it.id]?.testedAt ?: 0L
            }

            else -> filtered.sortedBy { it.name.lowercase() }
        }
    }

    val workingCount = results.values.count {
        it.status == ModelTestStatus.WORKING
    }

    val failedCount = results.values.count {
        it.status == ModelTestStatus.FAILED
    }

    val notTestedCount = models.size - workingCount - failedCount

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 620.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search model name or ID") }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatusCount(
                label = "Working",
                count = workingCount,
                color = MaterialTheme.colorScheme.primary
            )

            StatusCount(
                label = "Failed",
                count = failedCount,
                color = MaterialTheme.colorScheme.error
            )

            StatusCount(
                label = "Not Tested",
                count = notTestedCount,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            "Status Filter",
            style = MaterialTheme.typography.labelLarge
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            listOf(
                "All",
                "Working",
                "Failed",
                "Not Tested"
            ).forEach { option ->
                FilterChip(
                    selected = statusFilter == option,
                    onClick = { statusFilter = option },
                    label = { Text(option) }
                )
            }
        }

        Text(
            "Model Type",
            style = MaterialTheme.typography.labelLarge
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            types.forEach { type ->
                FilterChip(
                    selected = typeFilter == type,
                    onClick = { typeFilter = type },
                    label = { Text(type) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Sort",
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(Modifier.width(8.dp))

            listOf(
                "Name",
                "Working First",
                "Recently Tested"
            ).forEach { option ->
                FilterChip(
                    selected = sortBy == option,
                    onClick = { sortBy = option },
                    label = {
                        Text(
                            when (option) {
                                "Working First" -> "Working"
                                "Recently Tested" -> "Recent"
                                else -> "Name"
                            }
                        )
                    }
                )
            }
        }

        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (loadError.isNotBlank()) {
            Text(
                loadError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${filteredModels.size} of ${models.size} Free Models",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )

            TextButton(
                onClick = onRefresh,
                enabled = !loading && runningModelId == null
            ) {
                Text("Refresh")
            }
        }

        if (filteredModels.isEmpty()) {
            Text(
                "No models match the selected filters.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                filteredModels,
                key = { it.id }
            ) { model ->

                val result = results[model.id]

                val status = result?.status
                    ?: ModelTestStatus.NOT_TESTED

                val statusText = when (status) {
                    ModelTestStatus.WORKING -> "Working"
                    ModelTestStatus.FAILED -> "Failed"
                    ModelTestStatus.NOT_TESTED -> "Not Tested"
                }

                val statusColor = when (status) {
                    ModelTestStatus.WORKING ->
                        MaterialTheme.colorScheme.primary

                    ModelTestStatus.FAILED ->
                        MaterialTheme.colorScheme.error

                    ModelTestStatus.NOT_TESTED ->
                        MaterialTheme.colorScheme.onSurfaceVariant
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            if (status == ModelTestStatus.WORKING) {
                                MaterialTheme.colorScheme.primaryContainer
                                    .copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                                    .copy(alpha = 0.35f)
                            }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
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

                            Text(
                                statusText,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        Text(
                            "Type: ${model.modelType}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            "Input: ${model.inputModalities.joinToString().ifBlank { "Unknown" }}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            "Output: ${model.outputModalities.joinToString().ifBlank { "Unknown" }}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        model.contextLength?.let {
                            Text(
                                "Context: $it tokens",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (result != null) {
                            Text(
                                result.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = statusColor
                            )
                        }

                        if (
                            model.modelType == "Chat" ||
                            model.outputModalities.any {
                                it.equals("text", ignoreCase = true)
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (runningModelId == null) {
                                            runningModelId = model.id

                                            scope.launch {
                                                val testResult =
                                                    withContext(Dispatchers.IO) {
                                                        OpenRouterModelTester
                                                            .testChatModel(
                                                                apiKey,
                                                                model.id
                                                            )
                                                    }

                                                results = results +
                                                    (model.id to testResult)

                                                runningModelId = null
                                            }
                                        }
                                    },
                                    enabled =
                                        runningModelId == null &&
                                        apiKey.isNotBlank()
                                ) {
                                    if (runningModelId == model.id) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            if (result == null) "Test"
                                            else "Retest"
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        onSelectModel(model)
                                    }
                                ) {
                                    Text(
                                        if (selectedModelId == model.id) {
                                            "Selected"
                                        } else {
                                            "Select"
                                        }
                                    )
                                }
                            }
                        } else {
                            Text(
                                "No compatible test is available yet. " +
                                    "This model is not marked Working.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        if (apiKey.isBlank()) {
            Text(
                "OpenRouter API key is missing. Add it in API settings to test models.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun StatusCount(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 9.dp,
                vertical = 6.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                count.toString(),
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                color = color,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
