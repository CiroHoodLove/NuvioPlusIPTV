package com.nuvio.tv.livetv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.nuvio.tv.ui.theme.LocalNuvioColors
import com.nuvio.tv.ui.theme.NuvioColorScheme
import com.nuvio.tv.ui.theme.NuvioTheme

/*
 * Modern Interface (Settings → Live TV → Look): a flat charcoal look for the TV guide and the
 * Sports hub. Neutral grays only; the selected theme still supplies the accent (the focus ring,
 * the highlight), so this never replaces a color the user picked.
 *
 * [on] is a Compose state set from the Live TV settings in MainActivity, so screens that read it
 * (directly or through SportsLook) redraw as soon as the switch changes.
 */
internal object ModernLook {
    var on by mutableStateOf(false)

    val Background = Color(0xFF0D0D0D)   // screen
    val Panel = Color(0xFF1A1A1A)        // raised background, panels, modals
    val Surface = Color(0xFF1E1E1E)      // menus
    val Card = Color(0xFF242424)         // cards, pills
    val CardRaised = Color(0xFF2D2D2D)   // selected / surface variant
    val Divider = Color(0xFF333333)      // borders and dividers
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB3B3B3)
    val TextTertiary = Color(0xFF808080)
    val TextDisabled = Color(0xFF4D4D4D)

    // The player-overlay panels keep some see-through so the picture still shows behind them.
    val PanelDark = Color(0xE61A1A1A)
    val PanelMid = Color(0x991A1A1A)
}

/**
 * Everything inside sees the Modern Interface's neutral grays through NuvioTheme.colors (the guide's
 * rows, pills, info panel and menus all read it), while the theme's accent (Secondary, FocusRing,
 * FocusBackground) is left exactly as the user picked it. Only wraps the Live TV guide and the
 * Sports hub, never the rest of the app. Pure-black AMOLED backgrounds are left alone.
 */
@Composable
internal fun ModernGuideTheme(content: @Composable () -> Unit) {
    val current = NuvioTheme.colors
    if (!ModernLook.on || current.Background == Color.Black) {
        content()
        return
    }
    val palette = NuvioTheme.palette
    val modern = remember(palette) {
        NuvioColorScheme(
            palette.copy(
                background = ModernLook.Background,
                backgroundElevated = ModernLook.Panel,
                backgroundCard = ModernLook.Card,
                surface = ModernLook.Surface,
                surfaceVariant = ModernLook.CardRaised,
                panel = ModernLook.Panel,
                field = Color(0xFF222222),
                menu = ModernLook.Surface,
                modal = ModernLook.Panel
            )
        )
    }
    CompositionLocalProvider(LocalNuvioColors provides modern) { content() }
}
