/* Copyright (C) 2026 The Android Open Source Project */
package com.android.customization.picker.color.custom.ui.compose

import android.content.theming.ThemeStyle
import androidx.compose.ui.graphics.toArgb
import com.android.systemui.monet.CustomThemePaletteGenerator
import com.android.systemui.monet.CustomThemeSeeds
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CustomThemePreviewTest {
    @Test
    fun sentinelRoles_doNotFallBackToMaterialDefaults() {
        val seed = 0xFF123456.toInt()
        val palette = CustomThemePaletteGenerator.generate(
            CustomThemeSeeds(seed, seed, seed, seed), null, false, ThemeStyle.VIBRANT, 0.0,
        )
        val roles = palette.lightDynamicColors.keys.withIndex().associate { (index, key) ->
            key to (0xFF000000.toInt() or (index * 1009))
        }
        val scheme = previewColorScheme(roles)
        assertThat(scheme.surfaceContainer.toArgb()).isEqualTo(roles.getValue("surface_container"))
        assertThat(scheme.tertiaryContainer.toArgb()).isEqualTo(roles.getValue("tertiary_container"))
        assertThat(scheme.onTertiaryContainer.toArgb()).isEqualTo(roles.getValue("on_tertiary_container"))
        assertThat(scheme.secondaryContainer.toArgb()).isEqualTo(roles.getValue("secondary_container"))
        assertThat(scheme.onSecondaryContainer.toArgb()).isEqualTo(roles.getValue("on_secondary_container"))
    }

    @Test
    fun yellowPalette_mapsComponentRolesInBothModes() {
        val yellow = 0xFFFFFF66.toInt()
        val palette = CustomThemePaletteGenerator.generate(
            CustomThemeSeeds(yellow, yellow, yellow, yellow), null, false, ThemeStyle.VIBRANT, 0.0,
        )
        for (roles in listOf(palette.lightDynamicColors, palette.darkDynamicColors)) {
            val scheme = previewColorScheme(roles)
            assertThat(scheme.primary.toArgb()).isEqualTo(roles.getValue("primary"))
            assertThat(scheme.tertiary.toArgb()).isEqualTo(roles.getValue("tertiary"))
            assertThat(scheme.surfaceContainer.toArgb()).isEqualTo(roles.getValue("surface_container"))
            assertThat(scheme.surfaceContainerLow.toArgb()).isEqualTo(roles.getValue("surface_container_low"))
            assertThat(scheme.surfaceContainerHigh.toArgb()).isEqualTo(roles.getValue("surface_container_high"))
            assertThat(scheme.surfaceContainerLowest.toArgb()).isEqualTo(roles.getValue("surface_container_lowest"))
            assertThat(scheme.surfaceContainerHighest.toArgb()).isEqualTo(roles.getValue("surface_container_highest"))
            assertThat(scheme.surfaceTint.toArgb()).isEqualTo(roles.getValue("surface_tint"))
            assertThat(scheme.surfaceVariant.toArgb()).isEqualTo(roles.getValue("surface_variant"))
            assertThat(scheme.onSurfaceVariant.toArgb()).isEqualTo(roles.getValue("on_surface_variant"))
            assertThat(scheme.outlineVariant.toArgb()).isEqualTo(roles.getValue("outline_variant"))
            assertThat(scheme.inverseSurface.toArgb()).isEqualTo(roles.getValue("inverse_surface"))
            assertThat(scheme.inverseOnSurface.toArgb()).isEqualTo(roles.getValue("inverse_on_surface"))
            assertThat(scheme.inversePrimary.toArgb()).isEqualTo(roles.getValue("inverse_primary"))
            assertThat(scheme.errorContainer.toArgb()).isEqualTo(roles.getValue("error_container"))
            assertThat(scheme.onErrorContainer.toArgb()).isEqualTo(roles.getValue("on_error_container"))
        }
    }
}
