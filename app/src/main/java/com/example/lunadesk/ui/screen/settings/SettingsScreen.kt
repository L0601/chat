package com.example.lunadesk.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lunadesk.R
import com.example.lunadesk.data.model.ModelInfo
import com.example.lunadesk.ui.LunaDeskUiState
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
    onModelSearchChange: (String) -> Unit = {}
) {
    val filteredModels = if (state.modelSearchQuery.isBlank()) {
        state.models
    } else {
        state.models.filter { it.id.contains(state.modelSearchQuery, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Header(onBack = onBack)

        SectionHeader(title = "服务配置")

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

        SectionHeader(
            title = stringResource(R.string.settings_model_list),
            trailing = stringResource(R.string.settings_model_count, filteredModels.size)
        )

        ModelListCard(
            modifier = Modifier.weight(1f),
            state = state,
            filteredModels = filteredModels,
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
private fun SectionHeader(title: String, trailing: String? = null) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        trailing?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary
            )
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = onBaseUrlChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.settings_base_url_label)) },
                placeholder = { Text(stringResource(R.string.settings_base_url_placeholder)) },
                leadingIcon = {
                    Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = state.apiKey,
                onValueChange = onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.settings_api_key_label)) },
                placeholder = { Text(stringResource(R.string.settings_api_key_placeholder)) },
                leadingIcon = {
                    Icon(Icons.Outlined.VpnKey, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                shape = RoundedCornerShape(14.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp)
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.settings_save), maxLines = 1)
                }
                FilledTonalButton(
                    onClick = onTestConnection,
                    enabled = !state.isTestingConnection,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        stringResource(
                            if (state.isTestingConnection) R.string.settings_testing
                            else R.string.settings_test
                        ),
                        maxLines = 1
                    )
                }
            }
            OutlinedButton(
                onClick = onRefreshModels,
                enabled = !state.isLoadingModels,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    stringResource(
                        if (state.isLoadingModels) R.string.settings_loading_models
                        else R.string.settings_select_model
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ModelListCard(
    modifier: Modifier = Modifier,
    state: LunaDeskUiState,
    filteredModels: List<ModelInfo>,
    onSwitchModel: (String) -> Unit,
    onModelSearchChange: (String) -> Unit
) {
    val colors = LocalAppColors.current
    val focusManager = LocalFocusManager.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.modelListCardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.models.isNotEmpty()) {
                OutlinedTextField(
                    value = state.modelSearchQuery,
                    onValueChange = onModelSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text(stringResource(R.string.settings_search_model)) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        autoCorrect = false
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )
            }
            when {
                state.models.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.settings_empty_models),
                            color = colors.textTertiary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                filteredModels.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.settings_no_matching_models),
                            color = colors.textTertiary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                else -> {
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
                .height(50.dp)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = colors.modelStatusText
                )
            }
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
                style = MaterialTheme.typography.labelMedium
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
