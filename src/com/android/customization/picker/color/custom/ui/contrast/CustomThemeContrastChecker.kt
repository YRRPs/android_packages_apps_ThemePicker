/* Copyright (C) 2026 The Android Open Source Project */
package com.android.customization.picker.color.custom.ui.contrast

import androidx.core.graphics.ColorUtils
import com.android.systemui.monet.CustomThemePalette

class CustomThemeContrastChecker {
    fun check(palette: CustomThemePalette): List<String> =
        (checkRoles(palette.lightDynamicColors) + checkRoles(palette.darkDynamicColors)).distinct()

    fun checkRoles(roles: Map<String, Int>): List<String> =
        PAIRS.mapNotNull { pair ->
            val foreground = roles[pair.foreground] ?: return@mapNotNull null
            val background = roles[pair.background] ?: return@mapNotNull null
            if (ColorUtils.calculateContrast(foreground, background) < pair.minimum) {
                pair.component
            } else {
                null
            }
        }

    private data class RolePair(
        val component: String,
        val foreground: String,
        val background: String,
        val minimum: Double,
    )

    private companion object {
        val PAIRS =
            listOf(
                RolePair("Filled button", "on_primary", "primary", 4.5),
                RolePair("Surface text", "on_surface", "surface", 4.5),
                RolePair("Secondary chip", "on_secondary_container", "secondary_container", 4.5),
                RolePair("Tertiary switch", "on_tertiary", "tertiary", 3.0),
                RolePair("Outline", "outline", "surface", 3.0),
            )
    }
}
