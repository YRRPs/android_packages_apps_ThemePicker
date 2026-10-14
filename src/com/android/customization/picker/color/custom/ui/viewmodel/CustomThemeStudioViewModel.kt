/*
 * Copyright (C) 2026 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.android.customization.picker.color.custom.ui.viewmodel

import android.content.theming.ThemeStyle
import androidx.lifecycle.ViewModel
import com.android.customization.picker.color.custom.data.CustomThemeRepository
import com.android.customization.picker.color.custom.data.SavedThemesLoadState
import com.android.customization.picker.color.custom.data.WriteResult
import com.android.customization.picker.color.custom.shared.model.CustomThemeDefinition
import com.android.customization.picker.color.custom.ui.contrast.CustomThemeContrastChecker
import com.android.systemui.monet.CustomThemeContract
import com.android.systemui.monet.CustomThemePalette
import com.android.systemui.monet.CustomThemePaletteGenerator
import com.android.systemui.monet.CustomThemeSeeds
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PaletteMode { LIGHT, DARK }
enum class SeedFamily { PRIMARY, SECONDARY, TERTIARY, NEUTRAL }

data class SeedEditorState(val text: String, val color: Int?, val error: String? = null)

data class SeedSetState(
    val primary: SeedEditorState,
    val secondary: SeedEditorState,
    val tertiary: SeedEditorState,
    val neutral: SeedEditorState,
)

data class CustomThemeStudioUiState(
    val themes: List<CustomThemeDefinition>,
    val storageError: String?,
    val id: UUID,
    val name: String,
    @ThemeStyle.Type val style: Int,
    val separateDark: Boolean,
    val light: SeedSetState,
    val dark: SeedSetState?,
    val previewPalette: CustomThemePalette,
    val warnings: List<String>,
    val canSave: Boolean,
    val awaitingApplyConfirmation: Boolean = false,
    val lastWriteResult: WriteResult? = null,
)

class CustomThemeStudioViewModel(
    private val repository: CustomThemeRepository,
    private val contrastProvider: () -> Double,
    private val contrastChecker: CustomThemeContrastChecker = CustomThemeContrastChecker(),
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(initialState())
    val uiState: StateFlow<CustomThemeStudioUiState> = mutableUiState.asStateFlow()

    fun setName(value: String) =
        update {
            copy(
                name = value,
                canSave = storageError == null && isValid(value, light, dark),
            )
        }

    fun setStyle(@ThemeStyle.Type style: Int) =
        updateAndGenerate { copy(style = style, lastWriteResult = null) }

    fun setSeparateDark(enabled: Boolean) =
        updateAndGenerate {
            copy(separateDark = enabled, dark = if (enabled) light else null, lastWriteResult = null)
        }

    fun setSeed(mode: PaletteMode, family: SeedFamily, text: String) {
        val parsed = parseColor(text)
        val editor = SeedEditorState(text, parsed, if (parsed == null) "Use #RRGGBB" else null)
        val next =
            when (mode) {
                PaletteMode.LIGHT -> mutableUiState.value.copy(
                    light = mutableUiState.value.light.replace(family, editor),
                    lastWriteResult = null,
                )
                PaletteMode.DARK -> mutableUiState.value.copy(
                    dark = mutableUiState.value.dark?.replace(family, editor),
                    lastWriteResult = null,
                )
            }
        mutableUiState.value = regenerate(next)
    }

    fun save() = write(repository::save)

    fun requestApply() {
        if (mutableUiState.value.warnings.isEmpty()) {
            apply()
        } else {
            update { copy(awaitingApplyConfirmation = true) }
        }
    }

    fun confirmApplyAnyway() {
        apply()
        update { copy(awaitingApplyConfirmation = false) }
    }

    fun apply() = write(repository::apply)

    fun loadTheme(theme: CustomThemeDefinition) {
        mutableUiState.value = stateFromTheme(theme)
    }

    fun duplicateTheme(theme: CustomThemeDefinition) {
        val names = currentThemes().map { it.normalizedName() }.toSet()
        var suffix = 1
        var candidate = copyName(theme.name, suffix)
        while (candidate.trim().lowercase(java.util.Locale.ROOT) in names) {
            suffix += 1
            candidate = copyName(theme.name, suffix)
        }
        val copy = theme.copy(id = UUID.randomUUID(), name = candidate)
        val result = repository.save(copy)
        mutableUiState.value =
            if (result == WriteResult.SUCCESS) {
                stateFromTheme(copy).copy(lastWriteResult = result)
            } else {
                mutableUiState.value.copy(lastWriteResult = result)
            }
    }

    fun deleteTheme(theme: CustomThemeDefinition) {
        val result = repository.delete(theme.id)
        mutableUiState.value = mutableUiState.value.copy(
            themes = currentThemes(),
            lastWriteResult = result,
        )
    }

    fun resetSavedThemes() {
        val result = repository.reset()
        mutableUiState.value =
            if (result == WriteResult.SUCCESS) initialState().copy(lastWriteResult = result)
            else mutableUiState.value.copy(lastWriteResult = result)
    }

    private fun write(operation: (CustomThemeDefinition) -> WriteResult) {
        val state = mutableUiState.value
        val definition = state.toDefinition() ?: return
        val result = operation(definition)
        mutableUiState.value = state.copy(themes = currentThemes(), lastWriteResult = result)
    }

    private fun update(transform: CustomThemeStudioUiState.() -> CustomThemeStudioUiState) {
        mutableUiState.value = mutableUiState.value.transform()
    }

    private fun updateAndGenerate(transform: CustomThemeStudioUiState.() -> CustomThemeStudioUiState) {
        mutableUiState.value = regenerate(mutableUiState.value.transform())
    }

    private fun regenerate(state: CustomThemeStudioUiState): CustomThemeStudioUiState {
        val light = state.light.toSeeds()
        val dark = state.dark?.toSeeds()
        val valid = isValid(state.name, state.light, state.dark)
        if (!valid || light == null || (state.separateDark && dark == null)) {
            return state.copy(canSave = false, awaitingApplyConfirmation = false)
        }
        val palette =
            CustomThemePaletteGenerator.generate(
                light,
                dark,
                state.separateDark,
                state.style,
                contrastProvider(),
            )
        return state.copy(
            previewPalette = palette,
            warnings = contrastChecker.check(palette),
            canSave = state.storageError == null,
            awaitingApplyConfirmation = false,
        )
    }

    private fun initialState(): CustomThemeStudioUiState =
        stateFromTheme(
            CustomThemeDefinition(
                UUID.randomUUID(),
                "Custom theme",
                ThemeStyle.VIBRANT,
                false,
                CustomThemeSeeds(0xFFB69CFF.toInt(), 0xFF75D5C7.toInt(), 0xFFFF91B8.toInt(), 0xFF8C8791.toInt()),
                null,
            )
        )

    private fun stateFromTheme(theme: CustomThemeDefinition): CustomThemeStudioUiState {
        val light = theme.light.toEditorState()
        val dark = theme.dark?.toEditorState()
        val palette =
            CustomThemePaletteGenerator.generate(
                theme.light,
                theme.dark,
                theme.separateDark,
                theme.style,
                contrastProvider(),
            )
        val storageError = storageError()
        return CustomThemeStudioUiState(
            themes = currentThemes(),
            storageError = storageError,
            id = theme.id,
            name = theme.name,
            style = theme.style,
            separateDark = theme.separateDark,
            light = light,
            dark = dark,
            previewPalette = palette,
            warnings = contrastChecker.check(palette),
            canSave = storageError == null,
        )
    }

    private fun storageError(): String? =
        when (val saved = repository.state.value) {
            is SavedThemesLoadState.Valid -> null
            is SavedThemesLoadState.Invalid -> "Saved themes are invalid (${saved.reason})"
            is SavedThemesLoadState.UnsupportedVersion ->
                "Saved themes use unsupported version ${saved.version}"
        }

    private fun currentThemes(): List<CustomThemeDefinition> =
        (repository.state.value as? SavedThemesLoadState.Valid)?.themes.orEmpty()

    private fun copyName(base: String, index: Int): String {
        val suffix = if (index == 1) " Copy" else " Copy $index"
        return base.take(64 - suffix.length).trimEnd() + suffix
    }

    private fun editor(text: String) = SeedEditorState(text, parseColor(text))

    private fun parseColor(text: String): Int? =
        try {
            CustomThemeContract.parseRgb(text.trim().removePrefix("#"))
        } catch (exception: IllegalArgumentException) {
            null
        }

    private fun isValid(name: String, light: SeedSetState, dark: SeedSetState?): Boolean =
        name.trim().length in 1..64 && light.toSeeds() != null &&
            (dark == null || dark.toSeeds() != null)
}

private fun CustomThemeSeeds.toEditorState(): SeedSetState =
    SeedSetState(
        SeedEditorState("#${CustomThemeContract.formatRgb(primary)}", primary),
        SeedEditorState("#${CustomThemeContract.formatRgb(secondary)}", secondary),
        SeedEditorState("#${CustomThemeContract.formatRgb(tertiary)}", tertiary),
        SeedEditorState("#${CustomThemeContract.formatRgb(neutral)}", neutral),
    )

private fun SeedSetState.replace(family: SeedFamily, editor: SeedEditorState): SeedSetState =
    when (family) {
        SeedFamily.PRIMARY -> copy(primary = editor)
        SeedFamily.SECONDARY -> copy(secondary = editor)
        SeedFamily.TERTIARY -> copy(tertiary = editor)
        SeedFamily.NEUTRAL -> copy(neutral = editor)
    }

private fun SeedSetState.toSeeds(): CustomThemeSeeds? {
    val colors = listOf(primary.color, secondary.color, tertiary.color, neutral.color)
    if (colors.any { it == null }) return null
    return CustomThemeSeeds(colors[0]!!, colors[1]!!, colors[2]!!, colors[3]!!)
}

private fun CustomThemeStudioUiState.toDefinition(): CustomThemeDefinition? {
    val lightSeeds = light.toSeeds() ?: return null
    val darkSeeds = dark?.toSeeds()
    if (separateDark && darkSeeds == null) return null
    return CustomThemeDefinition(id, name.trim(), style, separateDark, lightSeeds, darkSeeds)
}
