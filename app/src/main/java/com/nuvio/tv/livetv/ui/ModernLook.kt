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

    val Background = Color(0xFF000000)   // screen: pure OLED black
    val Panel = Color(0xFF111216)        // raised background, panels, modals
    val Surface = Color(0xFF111216)      // menus
    val Card = Color(0xFF181A20)         // cards, pills, idle rows
    val CardRaised = Color(0xFF2A2D35)   // selected row / surface variant (the grey)
    val Divider = Color(0xFF26282F)      // borders and dividers
    val LiveRed = Color(0xFFFF5A5F)      // the LIVE tag
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB3B3B3)
    val TextTertiary = Color(0xFF808080)
    val TextDisabled = Color(0xFF4D4D4D)

    // The player-overlay panels keep some see-through so the picture still shows behind them.
    val PanelDark = Color(0xE6111216)
    val PanelMid = Color(0x99111216)
}

/**
 * Everything inside sees the Modern Interface's neutral grays through NuvioTheme.colors (the guide's
 * rows, pills, info panel and menus all read it), while the theme's accent (Secondary, FocusRing,
 * FocusBackground) is left exactly as the user picked it. Only wraps the Live TV guide and the
 * Sports hub, never the rest of the app. Works on pure-black (AMOLED) screens too: the background stays black, the surfaces get the modern greys.
 */
@Composable
internal fun ModernGuideTheme(content: @Composable () -> Unit) {
    if (!ModernLook.on) {
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
                field = Color(0xFF1B1D23),
                menu = ModernLook.Surface,
                modal = ModernLook.Panel
            )
        )
    }
    CompositionLocalProvider(LocalNuvioColors provides modern) { content() }
}
