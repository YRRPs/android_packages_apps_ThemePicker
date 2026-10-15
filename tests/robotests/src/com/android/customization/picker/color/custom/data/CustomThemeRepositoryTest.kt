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
import androidx.test.filters.SmallTest
import com.android.customization.picker.color.custom.shared.model.CustomThemeDefinition
import com.android.systemui.monet.CustomThemeContract
import com.android.systemui.monet.CustomThemeSeeds
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@SmallTest
@RunWith(RobolectricTestRunner::class)
class CustomThemeRepositoryTest {
    private val codec = SavedThemesCodec()
    private val theme =
        CustomThemeDefinition(
            UUID.fromString("11111111-1111-4111-8111-111111111111"),
            "Aurora",
            ThemeStyle.VIBRANT,
            false,
            CustomThemeSeeds(0xFFB69CFF.toInt(), 0xFF75D5C7.toInt(), 0xFFFF91B8.toInt(), 0xFF8C8791.toInt()),
            null,
        )

    @Test
    fun codec_roundTrip_preservesTheme() {
        val decoded = codec.decode(codec.encode(listOf(theme)))

        assertThat(decoded).isEqualTo(SavedThemesLoadState.Valid(listOf(theme)))
    }

    @Test
    fun codec_invalidJson_preservesRaw() {
        val decoded = codec.decode("{broken")

        assertThat(decoded).isEqualTo(SavedThemesLoadState.Invalid("{broken", "malformed"))
    }

    @Test
    fun codec_duplicateNormalizedNames_rejectsWholeDocument() {
        val duplicate = theme.copy(id = UUID.randomUUID(), name = " aurora ")
        val raw = codec.encode(listOf(theme, duplicate))

        val decoded = codec.decode(raw)

        assertThat(decoded).isInstanceOf(SavedThemesLoadState.Invalid::class.java)
    }

    @Test
    fun apply_writesSavedDocumentBeforeMergedActiveContract() {
        val store = FakeStore(active = "{\"android.theme.customization.font\":\"font.pkg\"}")
        val repository = CustomThemeRepositoryImpl(store, codec)

        val result = repository.apply(theme)

        assertThat(result).isEqualTo(WriteResult.SUCCESS)
        assertThat(store.operations).containsExactly("saved", "active").inOrder()
        val active = JSONObject(store.active)
        assertThat(active.getString("android.theme.customization.font")).isEqualTo("font.pkg")
        assertThat(active.getString(CustomThemeContract.KEY_COLOR_SOURCE))
            .isEqualTo(CustomThemeContract.COLOR_SOURCE_CUSTOM_THEME)
    }

    @Test
    fun apply_savedWriteFails_doesNotWriteActiveContract() {
        val store = FakeStore(savedWriteSucceeds = false)
        val repository = CustomThemeRepositoryImpl(store, codec)

        val result = repository.apply(theme)

        assertThat(result).isEqualTo(WriteResult.SAVED_WRITE_FAILED)
        assertThat(store.operations).containsExactly("saved")
    }

    @Test
    fun refresh_invalidDocument_doesNotRewriteIt() {
        val store = FakeStore(saved = "{broken")
        val repository = CustomThemeRepositoryImpl(store, codec)

        repository.refresh()

        assertThat(repository.state.value)
            .isEqualTo(SavedThemesLoadState.Invalid("{broken", "malformed"))
        assertThat(store.operations).isEmpty()
    }

    private class FakeStore(
        var saved: String? = null,
        var active: String? = null,
        private val savedWriteSucceeds: Boolean = true,
        private val activeWriteSucceeds: Boolean = true,
    ) : CustomThemeSettingsStore {
        val operations = mutableListOf<String>()

        override fun readSavedThemes(): String? = saved

        override fun writeSavedThemes(value: String): Boolean {
            operations += "saved"
            if (savedWriteSucceeds) saved = value
            return savedWriteSucceeds
        }

        override fun readActiveTheme(): String? = active

        override fun writeActiveTheme(value: String): Boolean {
            operations += "active"
            if (activeWriteSucceeds) active = value
            return activeWriteSucceeds
        }
    }
}
