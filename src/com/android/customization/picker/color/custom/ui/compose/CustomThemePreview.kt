/* Copyright (C) 2026 The Android Open Source Project */
package com.android.customization.picker.color.custom.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.systemui.monet.CustomThemePalette

@Composable
fun CustomThemePreview(palette: CustomThemePalette, dark: Boolean, modifier: Modifier = Modifier) {
    val roles = if (dark) palette.darkDynamicColors else palette.lightDynamicColors
    val scheme = previewColorScheme(roles)
    MaterialTheme(colorScheme = scheme) {
        Card(modifier = modifier.padding(4.dp)) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(if (dark) "Dark preview" else "Light preview", style = MaterialTheme.typography.titleMedium)
                Text("Surface card and body text")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = {}) { Text("Primary") }
                    OutlinedButton(onClick = {}) { Text("Outline") }
                }
                AssistChip(
                    onClick = {},
                    label = { Text("Secondary chip") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = scheme.secondaryContainer,
                        labelColor = scheme.onSecondaryContainer,
                    ),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tertiary switch")
                    Switch(
                        checked = true,
                        onCheckedChange = {},
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = scheme.tertiary,
                            checkedThumbColor = scheme.onTertiary,
                        ),
                    )
                }
                OutlinedTextField(value = "Example", onValueChange = {}, label = { Text("Field") })
                FloatingActionButton(
                    onClick = {},
                    containerColor = scheme.tertiaryContainer,
                    contentColor = scheme.onTertiaryContainer,
                ) { Text("+") }
                Text("Error role", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** Maps generated roles without inheriting unrelated Material default colors. */
fun previewColorScheme(roles: Map<String, Int>) =
    lightColorScheme(
        primary = roles.color("primary"),
        onPrimary = roles.color("on_primary"),
        primaryContainer = roles.color("primary_container"),
        onPrimaryContainer = roles.color("on_primary_container"),
        secondary = roles.color("secondary"),
        onSecondary = roles.color("on_secondary"),
        secondaryContainer = roles.color("secondary_container"),
        onSecondaryContainer = roles.color("on_secondary_container"),
        tertiary = roles.color("tertiary"),
        onTertiary = roles.color("on_tertiary"),
        tertiaryContainer = roles.color("tertiary_container"),
        onTertiaryContainer = roles.color("on_tertiary_container"),
        background = roles.color("background"),
        onBackground = roles.color("on_background"),
        surface = roles.color("surface"),
        onSurface = roles.color("on_surface"),
        surfaceVariant = roles.color("surface_variant"),
        onSurfaceVariant = roles.color("on_surface_variant"),
        surfaceTint = roles.color("surface_tint"),
        inverseSurface = roles.color("inverse_surface"),
        inverseOnSurface = roles.color("inverse_on_surface"),
        inversePrimary = roles.color("inverse_primary"),
        surfaceDim = roles.color("surface_dim"),
        surfaceBright = roles.color("surface_bright"),
        surfaceContainerLowest = roles.color("surface_container_lowest"),
        surfaceContainerLow = roles.color("surface_container_low"),
        surfaceContainer = roles.color("surface_container"),
        surfaceContainerHigh = roles.color("surface_container_high"),
        surfaceContainerHighest = roles.color("surface_container_highest"),
        outline = roles.color("outline"),
        outlineVariant = roles.color("outline_variant"),
        scrim = roles.color("scrim"),
        error = roles.color("error"),
        onError = roles.color("on_error"),
        errorContainer = roles.color("error_container"),
        onErrorContainer = roles.color("on_error_container"),
    )

private fun Map<String, Int>.color(name: String): Color = Color(requireNotNull(get(name)))
