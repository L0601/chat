package com.example.lunadesk.ui.screen.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lunadesk.R
import com.example.lunadesk.data.model.ModelInfo
import com.example.lunadesk.ui.LunaDeskUiState

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    state: LunaDeskUiState,
    onBack: () -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onTemperatureChange: (String) -> Unit,
    onMaxTokensChange: (String) -> Unit,
    onSave: () -> Unit,
    onTestConnection: () -> Unit,
    onRefreshModels: () -> Unit,
    onSwitchModel: (String) -> Unit,
    onModelSearchChange: (String) -> Unit = {}
) {
    val context = LocalContext.current

    LaunchedEffect(state.inlineMessage) {
        state.inlineMessage
            ?.takeIf { it.isNotBlank() }
            ?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Header(onBack = onBack)
        ConfigCard(
            state = state,
            onBaseUrlChange = onBaseUrlChange,
            onApiKeyChange = onApiKeyChange,
            onTemperatureChange = onTemperatureChange,
            onMaxTokensChange = onMaxTokensChange,
            onSave = onSave,
            onTestConnection = onTestConnection,
            onRefreshModels = onRefreshModels
        )
        ModelListCard(
            modifier = Modifier.weight(1f),
            state = state,
            onSwitchModel = onSwitchModel,
            onModelSearchChange = onModelSearchChange
        )
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF223732)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onBack,
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                colors = lightButtonColors(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun ConfigCard(
    state: LunaDeskUiState,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onTemperatureChange: (String) -> Unit,
    onMaxTokensChange: (String) -> Unit,
    onSave: () -> Unit,
    onTestConnection: () -> Unit,
    onRefreshModels: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBF7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = onBaseUrlChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.settings_base_url_label)) },
                placeholder = { Text(stringResource(R.string.settings_base_url_placeholder)) }
            )
            OutlinedTextField(
                value = state.apiKey,
                onValueChange = onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.settings_api_key_label)) },
                placeholder = { Text(stringResource(R.string.settings_api_key_placeholder)) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.temperatureInput,
                    onValueChange = onTemperatureChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_temperature_label)) }
                )
                OutlinedTextField(
                    value = state.maxTokensInput,
                    onValueChange = onMaxTokensChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_max_tokens_label)) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_save))
                }
                Button(
                    onClick = onTestConnection,
                    enabled = !state.isTestingConnection,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(
                            if (state.isTestingConnection) {
                                R.string.settings_testing
                            } else {
                                R.string.settings_test
                            }
                        )
                    )
                }
                Button(
                    onClick = onRefreshModels,
                    enabled = !state.isLoadingModels,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(
                            if (state.isLoadingModels) {
                                R.string.settings_loading_models
                            } else {
                                R.string.settings_select_model
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelListCard(
    modifier: Modifier = Modifier,
    state: LunaDeskUiState,
    onSwitchModel: (String) -> Unit,
    onModelSearchChange: (String) -> Unit
) {
    val filteredModels = if (state.modelSearchQuery.isBlank()) {
        state.models
    } else {
        state.models.filter { it.id.contains(state.modelSearchQuery, ignoreCase = true) }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF7FFFDF8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.settings_model_list),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.settings_model_count, filteredModels.size),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (state.models.isNotEmpty()) {
                OutlinedTextField(
                    value = state.modelSearchQuery,
                    onValueChange = onModelSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text(stringResource(R.string.settings_search_model)) }
                )
            }
            if (state.models.isEmpty()) {
                Text(stringResource(R.string.settings_empty_models))
            } else if (filteredModels.isEmpty()) {
                Text(stringResource(R.string.settings_no_matching_models))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredModels, key = { it.id }) { model ->
                        ModelRow(
                            model = model,
                            selected = model.id == state.selectedModel,
                            onClick = { onSwitchModel(model.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelRow(
    model: ModelInfo,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) Color(0xFFE0ECE5) else Color(0xFFF7F1E0)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        modifier = Modifier.clickable(enabled = !selected, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = model.id,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(
                    if (selected) R.string.settings_model_current
                    else R.string.settings_model_pending
                ),
                color = Color(0xFF2A4B45),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun lightButtonColors() = ButtonDefaults.buttonColors(
    containerColor = Color(0xF6FFFFFF),
    contentColor = Color(0xFF1E2A27),
    disabledContainerColor = Color(0xE8FFFFFF),
    disabledContentColor = Color(0xFF1E2A27)
)
