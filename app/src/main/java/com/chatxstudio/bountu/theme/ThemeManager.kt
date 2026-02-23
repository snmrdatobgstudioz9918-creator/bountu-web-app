package com.chatxstudio.bountu.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DataStore extension for theme preferences
private val Context.themeDataStore: androidx.datastore.core.DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

/**
 * Enum representing available themes
 */
enum class AppTheme {
    DARK,
    LIGHT,
    SYSTEM
}

/**
 * Enum representing available color schemes
 */
enum class ColorScheme {
    DEFAULT,      // Original Material 3 scheme
    UBUNTU,       // Ubuntu orange/green colors
    MONOKAI,      // Dark theme with vibrant accents
    SOLARIZED,    // Solarized color palette
    TERMINAL,     // Classic terminal green on black
    CUSTOM        // Loaded from Git JSON (e.g., Bountu)
}

/**
 * Data class to hold theme configuration
 */
data class ThemeConfig(
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val colorScheme: ColorScheme = ColorScheme.DEFAULT,
    val fontSize: Float = 14f,
    val fontFamily: String = "monospace",
    // Raw JSON for custom theme (if any), e.g., Bountu theme JSON from Git
    val customThemeJson: String? = null
)

/**
 * Theme manager for Bountu app
 * Handles theme switching and persistence
 */
class ThemeManager(private val context: Context) {

    private object PreferenceKeys {
        val THEME_TYPE = stringPreferencesKey("theme_type")
        val COLOR_SCHEME = stringPreferencesKey("color_scheme")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val CUSTOM_THEME_JSON = stringPreferencesKey("custom_theme_json")
    }

    /**
     * Get current theme configuration from preferences as a Flow
     */
    fun getThemeConfigFlow(): Flow<ThemeConfig> {
        return context.themeDataStore.data.map { preferences ->
            ThemeConfig(
                appTheme = try {
                    AppTheme.valueOf(preferences[PreferenceKeys.THEME_TYPE] ?: AppTheme.SYSTEM.name)
                } catch (_: IllegalArgumentException) {
                    AppTheme.SYSTEM
                },
                colorScheme = try {
                    ColorScheme.valueOf(preferences[PreferenceKeys.COLOR_SCHEME] ?: ColorScheme.DEFAULT.name)
                } catch (_: IllegalArgumentException) {
                    ColorScheme.DEFAULT
                },
                fontSize = preferences[PreferenceKeys.FONT_SIZE]?.toFloatOrNull() ?: 14f,
                fontFamily = preferences[PreferenceKeys.FONT_FAMILY] ?: "monospace",
                customThemeJson = preferences[PreferenceKeys.CUSTOM_THEME_JSON]
            )
        }
    }

    /**
     * Get current theme configuration from preferences (blocking call)
     */
    suspend fun getThemeConfig(): ThemeConfig {
        val preferences = context.themeDataStore.data.map { preferences ->
            ThemeConfig(
                appTheme = try {
                    AppTheme.valueOf(preferences[PreferenceKeys.THEME_TYPE] ?: AppTheme.SYSTEM.name)
                } catch (_: IllegalArgumentException) {
                    AppTheme.SYSTEM
                },
                colorScheme = try {
                    ColorScheme.valueOf(preferences[PreferenceKeys.COLOR_SCHEME] ?: ColorScheme.DEFAULT.name)
                } catch (_: IllegalArgumentException) {
                    ColorScheme.DEFAULT
                },
                fontSize = preferences[PreferenceKeys.FONT_SIZE]?.toFloatOrNull() ?: 14f,
                fontFamily = preferences[PreferenceKeys.FONT_FAMILY] ?: "monospace",
                customThemeJson = preferences[PreferenceKeys.CUSTOM_THEME_JSON]
            )
        }.first()

        return preferences
    }

    /**
     * Save theme configuration to preferences
     */
    suspend fun saveThemeConfig(config: ThemeConfig) {
        context.themeDataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME_TYPE] = config.appTheme.name
            preferences[PreferenceKeys.COLOR_SCHEME] = config.colorScheme.name
            preferences[PreferenceKeys.FONT_SIZE] = config.fontSize.toString()
            preferences[PreferenceKeys.FONT_FAMILY] = config.fontFamily
            if (config.customThemeJson != null) {
                preferences[PreferenceKeys.CUSTOM_THEME_JSON] = config.customThemeJson
            } else {
                preferences.remove(PreferenceKeys.CUSTOM_THEME_JSON)
            }
        }
    }

    /**
     * Get the appropriate color scheme based on theme and color scheme selection
     */
    @Composable
    fun getColorScheme(themeConfig: ThemeConfig): androidx.compose.material3.ColorScheme {
        val useDarkTheme = when (themeConfig.appTheme) {
            AppTheme.DARK -> true
            AppTheme.LIGHT -> false
            AppTheme.SYSTEM -> isSystemInDarkTheme()
        }

        return when (themeConfig.colorScheme) {
            ColorScheme.DEFAULT -> {
                if (useDarkTheme) darkColorScheme() else lightColorScheme()
            }
            ColorScheme.UBUNTU -> {
                if (useDarkTheme) {
                    darkColorScheme(
                        primary = Color(0xFFE95420),  // Ubuntu orange
                        secondary = Color(0xFF7EB34F), // Ubuntu green
                        tertiary = Color(0xFF5E2750)   // Ubuntu magenta
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFFE95420),
                        secondary = Color(0xFF7EB34F),
                        tertiary = Color(0xFF5E2750)
                    )
                }
            }
            ColorScheme.MONOKAI -> {
                darkColorScheme(
                    primary = Color(0xFFF92672),
                    secondary = Color(0xFFA6E22E),
                    tertiary = Color(0xFFFD971F),
                    background = Color(0xFF272822),
                    surface = Color(0xFF1D1F21),
                    onPrimary = Color(0xFFFFFFFF),
                    onSecondary = Color(0xFFFFFFFF),
                    onTertiary = Color(0xFFFFFFFF),
                    onBackground = Color(0xFFE6E6E6),
                    onSurface = Color(0xFFE6E6E6)
                )
            }
            ColorScheme.SOLARIZED -> {
                if (useDarkTheme) {
                    darkColorScheme(
                        primary = Color(0xFF268BD2),
                        secondary = Color(0xFF2AA198),
                        tertiary = Color(0xFFB58900),
                        background = Color(0xFF002B36),
                        surface = Color(0xFF073642),
                        onPrimary = Color(0xFFEEE8D5),
                        onSecondary = Color(0xFFEEE8D5),
                        onTertiary = Color(0xFFEEE8D5),
                        onBackground = Color(0xFF839496),
                        onSurface = Color(0xFF839496)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF268BD2),
                        secondary = Color(0xFF2AA198),
                        tertiary = Color(0xFFB58900),
                        background = Color(0xFFFDF6E3),
                        surface = Color(0xFFEEE8D5),
                        onPrimary = Color(0xFF073642),
                        onSecondary = Color(0xFF073642),
                        onTertiary = Color(0xFF073642),
                        onBackground = Color(0xFF657B83),
                        onSurface = Color(0xFF657B83)
                    )
                }
            }
            ColorScheme.TERMINAL -> {
                if (useDarkTheme) {
                    darkColorScheme(
                        primary = Color(0xFF00FF00),
                        secondary = Color(0xFF00CC00),
                        tertiary = Color(0xFF009900),
                        background = Color(0xFF000000),
                        surface = Color(0xFF001100),
                        onPrimary = Color(0xFF00FF00),
                        onSecondary = Color(0xFF00FF00),
                        onTertiary = Color(0xFF00FF00),
                        onBackground = Color(0xFF00FF00),
                        onSurface = Color(0xFF00FF00)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF006600),
                        secondary = Color(0xFF004400),
                        tertiary = Color(0xFF002200),
                        background = Color(0xFF000000),
                        surface = Color(0xFF001100),
                        onPrimary = Color(0xFF00FF00),
                        onSecondary = Color(0xFF00FF00),
                        onTertiary = Color(0xFF00FF00),
                        onBackground = Color(0xFF00FF00),
                        onSurface = Color(0xFF00FF00)
                    )
                }
            }
            ColorScheme.CUSTOM -> {
                // Parse minimal subset of the provided JSON using org.json (available on Android)
                val raw = themeConfig.customThemeJson
                if (raw.isNullOrBlank()) {
                    if (useDarkTheme) darkColorScheme() else lightColorScheme()
                } else {
                    fun parseColorSafe(hex: String?, fallback: String): Color {
                        val value = hex ?: fallback
                        return try { Color(android.graphics.Color.parseColor(value)) } catch (_: Exception) { Color(android.graphics.Color.parseColor(fallback)) }
                    }
                    try {
                        val obj = org.json.JSONObject(raw)
                        val colors = obj.optJSONObject("colors")
                        val background = parseColorSafe(obj.optString("background"), "#1A1A1D")
                        val foreground = parseColorSafe(obj.optString("foreground"), "#EAEAEA")
                        val primary = parseColorSafe(colors?.optString("blue"), "#00BFFF")
                        val secondary = parseColorSafe(colors?.optString("magenta"), "#6A0DAD")
                        val tertiary = parseColorSafe(colors?.optString("green"), "#39FF14")
                        darkColorScheme(
                            primary = primary,
                            secondary = secondary,
                            tertiary = tertiary,
                            background = background,
                            surface = background,
                            onBackground = foreground,
                            onSurface = foreground,
                            onPrimary = Color(0xFF000000),
                            onSecondary = Color(0xFFFFFFFF),
                            onTertiary = Color(0xFF000000)
                        )
                    } catch (_: Exception) {
                        if (useDarkTheme) darkColorScheme() else lightColorScheme()
                    }
                }
            }

        }
    }
}

