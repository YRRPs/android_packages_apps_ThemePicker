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

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Resetter

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [TestShadowSecureSettings::class])
class SettingsSecureCustomThemeSettingsStoreTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var context: Context
    @Mock private lateinit var resolver: ContentResolver

    private lateinit var store: SettingsSecureCustomThemeSettingsStore

    @Before
    fun setUp() {
        TestShadowSecureSettings.reset()
        `when`(context.contentResolver).thenReturn(resolver)
        `when`(resolver.userId).thenReturn(USER_ID)
        store = SettingsSecureCustomThemeSettingsStore(context)
    }

    @Test
    fun readSavedThemes_usesResolverUser() {
        store.readSavedThemes()

        assertThat(TestShadowSecureSettings.lastCall)
            .isEqualTo(
                SecureSettingsCall(
                    Operation.READ,
                    Settings.Secure.THEME_CUSTOMIZATION_SAVED_THEMES,
                    null,
                    USER_ID,
                )
            )
    }

    @Test
    fun writeSavedThemes_usesResolverUser() {
        store.writeSavedThemes(SAVED_THEMES)

        assertThat(TestShadowSecureSettings.lastCall)
            .isEqualTo(
                SecureSettingsCall(
                    Operation.WRITE,
                    Settings.Secure.THEME_CUSTOMIZATION_SAVED_THEMES,
                    SAVED_THEMES,
                    USER_ID,
                )
            )
    }

    @Test
    fun readActiveTheme_usesResolverUser() {
        store.readActiveTheme()

        assertThat(TestShadowSecureSettings.lastCall)
            .isEqualTo(
                SecureSettingsCall(
                    Operation.READ,
                    Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                    null,
                    USER_ID,
                )
            )
    }

    @Test
    fun writeActiveTheme_usesResolverUser() {
        store.writeActiveTheme(ACTIVE_THEME)

        assertThat(TestShadowSecureSettings.lastCall)
            .isEqualTo(
                SecureSettingsCall(
                    Operation.WRITE,
                    Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                    ACTIVE_THEME,
                    USER_ID,
                )
            )
    }

    private companion object {
        const val USER_ID = 10
        const val SAVED_THEMES = "saved themes"
        const val ACTIVE_THEME = "active theme"
    }
}

enum class Operation {
    READ,
    WRITE,
}

data class SecureSettingsCall(
    val operation: Operation,
    val key: String,
    val value: String?,
    val userId: Int,
)

@Implements(Settings.Secure::class)
class TestShadowSecureSettings {
    companion object {
        var lastCall: SecureSettingsCall? = null
            private set

        @Implementation
        @JvmStatic
        fun getString(resolver: ContentResolver, key: String): String? {
            lastCall = SecureSettingsCall(Operation.READ, key, null, resolver.userId)
            return null
        }

        @Implementation
        @JvmStatic
        fun getStringForUser(resolver: ContentResolver, key: String, userId: Int): String? {
            lastCall = SecureSettingsCall(Operation.READ, key, null, userId)
            return null
        }

        @Implementation
        @JvmStatic
        fun putString(resolver: ContentResolver, key: String, value: String?): Boolean {
            lastCall = SecureSettingsCall(Operation.WRITE, key, value, resolver.userId)
            return true
        }

        @Implementation
        @JvmStatic
        fun putStringForUser(
            resolver: ContentResolver,
            key: String,
            value: String?,
            userId: Int,
        ): Boolean {
            lastCall = SecureSettingsCall(Operation.WRITE, key, value, userId)
            return true
        }

        @Resetter
        @JvmStatic
        fun reset() {
            lastCall = null
        }
    }
}
