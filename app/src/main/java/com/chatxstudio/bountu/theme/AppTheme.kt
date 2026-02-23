package com.chatxstudio.bountu.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.chatxstudio.bountu.ui.theme.Typography

@Composable
fun AppTheme(
    themeManager: ThemeManager,
    themeConfig: ThemeConfig,
    content: @Composable () -> Unit
) {
    val colorScheme = themeManager.getColorScheme(themeConfig)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
