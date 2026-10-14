/* Copyright (C) 2026 The Android Open Source Project */
package com.android.customization.picker.color.custom.ui.compose

import android.content.theming.ThemeStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.customization.picker.color.custom.ui.viewmodel.CustomThemeStudioViewModel
import com.android.customization.picker.color.custom.ui.viewmodel.PaletteMode
import com.android.customization.picker.color.custom.ui.viewmodel.SeedEditorState
import com.android.customization.picker.color.custom.ui.viewmodel.SeedFamily
import com.android.customization.picker.color.custom.ui.viewmodel.SeedSetState

@Composable
fun CustomThemeStudioScreen(viewModel: CustomThemeStudioViewModel, onClose: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Custom color studio", style = MaterialTheme.typography.headlineSmall)
        Text("Saved themes", style = MaterialTheme.typography.titleMedium)
        if (state.themes.isEmpty()) {
            Text("No saved themes yet")
        }
        state.themes.forEach { theme ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(theme.name, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = { viewModel.loadTheme(theme) }) { Text("Edit") }
                    OutlinedButton(onClick = { viewModel.duplicateTheme(theme) }) { Text("Copy") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = { viewModel.deleteTheme(theme) }) { Text("Delete") }
                    Button(
                        onClick = {
                            viewModel.loadTheme(theme)
                            viewModel.requestApply()
                        }
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            label = { Text("Theme name") },
            modifier = Modifier.fillMaxWidth(),
        )
        SeedSetEditor("Light palette", PaletteMode.LIGHT, state.light, viewModel::setSeed)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Separate dark palette")
            Switch(checked = state.separateDark, onCheckedChange = viewModel::setSeparateDark)
        }
        state.dark?.let {
            SeedSetEditor("Dark palette", PaletteMode.DARK, it, viewModel::setSeed)
        }
        OutlinedButton(onClick = { viewModel.setStyle(nextStyle(state.style)) }) {
            Text("Palette style: ${ThemeStyle.name(state.style)}")
        }
        Text("Rendered component preview", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth()) {
            CustomThemePreview(state.previewPalette, dark = false, modifier = Modifier.weight(1f))
            CustomThemePreview(state.previewPalette, dark = true, modifier = Modifier.weight(1f))
        }
        if (state.warnings.isNotEmpty()) {
            Text(
                "Contrast warning: ${state.warnings.joinToString()}",
                color = MaterialTheme.colorScheme.error,
            )
        }
        state.lastWriteResult?.let { Text("Result: $it") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onClose) { Text("Close") }
            OutlinedButton(onClick = viewModel::save, enabled = state.canSave) { Text("Save") }
            Button(
                onClick =
                    if (state.awaitingApplyConfirmation) {
                        viewModel::confirmApplyAnyway
                    } else {
                        viewModel::requestApply
                    },
                enabled = state.canSave,
            ) {
                Text(if (state.awaitingApplyConfirmation) "Apply anyway" else "Apply")
            }
        }
    }
}

@Composable
private fun SeedSetEditor(
    title: String,
    mode: PaletteMode,
    seeds: SeedSetState,
    onChanged: (PaletteMode, SeedFamily, String) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    SeedEditor("Primary", seeds.primary) { onChanged(mode, SeedFamily.PRIMARY, it) }
    SeedEditor("Secondary", seeds.secondary) { onChanged(mode, SeedFamily.SECONDARY, it) }
    SeedEditor("Tertiary", seeds.tertiary) { onChanged(mode, SeedFamily.TERTIARY, it) }
    SeedEditor("Neutral", seeds.neutral) { onChanged(mode, SeedFamily.NEUTRAL, it) }
}

@Composable
private fun SeedEditor(label: String, state: SeedEditorState, onChanged: (String) -> Unit) {
    OutlinedTextField(
        value = state.text,
        onValueChange = onChanged,
        label = { Text(label) },
        isError = state.error != null,
        supportingText = state.error?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun nextStyle(@ThemeStyle.Type current: Int): Int {
    val styles =
        intArrayOf(
            ThemeStyle.TONAL_SPOT,
            ThemeStyle.VIBRANT,
            ThemeStyle.EXPRESSIVE,
            ThemeStyle.SPRITZ,
            ThemeStyle.RAINBOW,
            ThemeStyle.FRUIT_SALAD,
        )
    return styles[(styles.indexOf(current) + 1) % styles.size]
}
