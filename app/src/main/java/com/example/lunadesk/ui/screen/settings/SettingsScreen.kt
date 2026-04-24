package com.example.lunadesk.ui.screen.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lunadesk.R
import com.example.lunadesk.data.model.ModelInfo
import com.example.lunadesk.ui.LunaDeskUiState
import com.example.lunadesk.ui.components.InlineNotice
import com.example.lunadesk.ui.theme.LocalAppColors

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
    onModelSearchChange: (String) -> Unit = {},
    onDismissMessage: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Header(onBack = onBack)

        state.inlineMessage?.let {
            InlineNotice(message = it, onDismiss = onDismissMessage)
        }

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
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary
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
    val colors = LocalAppColors.current
    val tempFloat = state.temperatureInput.toFloatOrNull()
    val tempError = tempFloat != null && (tempFloat < 0f || tempFloat > 2f)
            || (state.temperatureInput.isNotBlank() && tempFloat == null)
    val maxInt = state.maxTokensInput.toIntOrNull()
    val maxError = maxInt != null && (maxInt < 1 || maxInt > 200000)
            || (state.maxTokensInput.isNotBlank() && maxInt == null)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.configCardBg)
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
                    label = { Text(stringResource(R.string.settings_temperature_label)) },
                    isError = tempError,
                    supportingText = if (tempError) {
                        { Text("范围 0 ~ 2") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = state.maxTokensInput,
                    onValueChange = onMaxTokensChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_max_tokens_label)) },
                    isError = maxError,
                    supportingText = if (maxError) {
                        { Text("范围 1 ~ 200000") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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
    val colors = LocalAppColors.current
    val filteredModels = if (state.modelSearchQuery.isBlank()) {
        state.models
    } else {
        state.models.filter { it.id.contains(state.modelSearchQuery, ignoreCase = true) }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.modelListCardBg)
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
    val colors = LocalAppColors.current
    val background = if (selected) colors.modelRowSelected else colors.modelRowUnselected

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
                color = colors.modelStatusText,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun lightButtonColors() = run {
    val colors = LocalAppColors.current
    ButtonDefaults.buttonColors(
        containerColor = colors.surfaceOverlay,
        contentColor = colors.textOnButton,
        disabledContainerColor = colors.surfaceOverlayDisabled,
        disabledContentColor = colors.textOnButton
    )
}
