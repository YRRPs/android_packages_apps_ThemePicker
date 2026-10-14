/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.customization.picker.color.custom.data

import android.content.theming.ThemeStyle
import com.android.customization.picker.color.custom.shared.model.CustomThemeDefinition
import com.android.systemui.monet.CustomThemeContract
import com.android.systemui.monet.CustomThemeSeeds
import java.util.UUID
import javax.inject.Inject
import org.json.JSONArray
import org.json.JSONObject

sealed interface SavedThemesLoadState {
    data class Valid(val themes: List<CustomThemeDefinition>) : SavedThemesLoadState

    data class Invalid(val raw: String, val reason: String) : SavedThemesLoadState

    data class UnsupportedVersion(val raw: String, val version: Int) : SavedThemesLoadState
}

class SavedThemesCodec @Inject constructor() {
    fun decode(raw: String?): SavedThemesLoadState {
        if (raw.isNullOrBlank()) return SavedThemesLoadState.Valid(emptyList())
        return try {
            val root = JSONObject(raw)
            val version = root.optInt(KEY_VERSION, -1)
            if (version != VERSION) {
                SavedThemesLoadState.UnsupportedVersion(raw, version)
            } else {
                decodeThemes(raw, root.getJSONArray(KEY_THEMES))
            }
        } catch (exception: Exception) {
            SavedThemesLoadState.Invalid(raw, "malformed")
        }
    }

    fun encode(themes: List<CustomThemeDefinition>): String {
        val values = JSONArray()
        themes.forEach { values.put(encodeTheme(it)) }
        return JSONObject().put(KEY_VERSION, VERSION).put(KEY_THEMES, values).toString()
    }

    private fun decodeThemes(raw: String, values: JSONArray): SavedThemesLoadState {
        return try {
            val themes = buildList {
                for (index in 0 until values.length()) add(decodeTheme(values.getJSONObject(index)))
            }
            validateUnique(themes)
            SavedThemesLoadState.Valid(themes)
        } catch (exception: Exception) {
            SavedThemesLoadState.Invalid(raw, "invalid_theme")
        }
    }

    private fun decodeTheme(value: JSONObject): CustomThemeDefinition {
        val separateDark = value.getBoolean(KEY_SEPARATE_DARK)
        val dark = if (separateDark) decodeSeeds(value.getJSONObject(KEY_DARK)) else null
        if (!separateDark && !value.isNull(KEY_DARK)) error("Linked theme must store null dark seeds")
        return CustomThemeDefinition(
            id = UUID.fromString(value.getString(KEY_ID)),
            name = value.getString(KEY_NAME),
            style = ThemeStyle.valueOf(value.getString(KEY_STYLE)),
            separateDark = separateDark,
            light = decodeSeeds(value.getJSONObject(KEY_LIGHT)),
            dark = dark,
        )
    }

    private fun encodeTheme(theme: CustomThemeDefinition): JSONObject =
        JSONObject()
            .put(KEY_ID, theme.id.toString())
            .put(KEY_NAME, theme.name.trim())
            .put(KEY_STYLE, ThemeStyle.name(theme.style))
            .put(KEY_SEPARATE_DARK, theme.separateDark)
            .put(KEY_LIGHT, encodeSeeds(theme.light))
            .put(KEY_DARK, theme.dark?.let(::encodeSeeds) ?: JSONObject.NULL)

    private fun decodeSeeds(value: JSONObject): CustomThemeSeeds =
        CustomThemeSeeds(
            CustomThemeContract.parseRgb(value.getString(KEY_PRIMARY)),
            CustomThemeContract.parseRgb(value.getString(KEY_SECONDARY)),
            CustomThemeContract.parseRgb(value.getString(KEY_TERTIARY)),
            CustomThemeContract.parseRgb(value.getString(KEY_NEUTRAL)),
        )

    private fun encodeSeeds(seeds: CustomThemeSeeds): JSONObject =
        JSONObject()
            .put(KEY_PRIMARY, CustomThemeContract.formatRgb(seeds.primary))
            .put(KEY_SECONDARY, CustomThemeContract.formatRgb(seeds.secondary))
            .put(KEY_TERTIARY, CustomThemeContract.formatRgb(seeds.tertiary))
            .put(KEY_NEUTRAL, CustomThemeContract.formatRgb(seeds.neutral))

    private fun validateUnique(themes: List<CustomThemeDefinition>) {
        require(themes.map { it.id }.toSet().size == themes.size) { "Duplicate theme ID" }
        require(themes.map { it.normalizedName() }.toSet().size == themes.size) {
            "Duplicate theme name"
        }
    }

    private companion object {
        const val VERSION = 1
        const val KEY_VERSION = "version"
        const val KEY_THEMES = "themes"
        const val KEY_ID = "id"
        const val KEY_NAME = "name"
        const val KEY_STYLE = "style"
        const val KEY_SEPARATE_DARK = "separate_dark"
        const val KEY_LIGHT = "light"
        const val KEY_DARK = "dark"
        const val KEY_PRIMARY = "primary"
        const val KEY_SECONDARY = "secondary"
        const val KEY_TERTIARY = "tertiary"
        const val KEY_NEUTRAL = "neutral"
    }
}
