package com.nuvio.tv.livetv.modern

import android.content.Context
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.tv.R
import com.nuvio.tv.livetv.model.LiveTvSettings
import com.nuvio.tv.livetv.sports.GameState
import com.nuvio.tv.livetv.sports.ScoreLabel
import com.nuvio.tv.livetv.sports.SportsCatalog
import com.nuvio.tv.livetv.sports.SportsEvent
import com.nuvio.tv.livetv.sports.SportsFollows
import com.nuvio.tv.livetv.sports.SportsLook
import com.nuvio.tv.livetv.sports.StateBadge
import com.nuvio.tv.livetv.sports.TeamLogo
import com.nuvio.tv.livetv.sports.bandColor
import com.nuvio.tv.livetv.sports.teamColor
import com.nuvio.tv.livetv.ui.LiveText
import com.nuvio.tv.livetv.ui.ModernLook
import com.nuvio.tv.ui.theme.NuvioTheme

/*
 * The shared pieces of the Modern Interface (Settings > Live TV > Look).
 * Everything here is flat colour and static drawing: no blur, nothing that animates by itself.
 * With Modern Interface off, none of these change anything.
 */

/** The Modern Interface's extra settings, copied in from LiveTvSettings by MainActivity. */
internal object ModernPrefs {
    /** "smooth" (focus scale and fades), "light" (fades only), "off" (no animation at all). */
    var motion by mutableStateOf("smooth")
    var cardGradient by mutableStateOf(true)
    /** The full screen info bar style: "fade", "panel" or "full". */
    var fullBar by mutableStateOf("fade")
    var flags by mutableStateOf(true)

    val animationsOn: Boolean get() = motion != "off"
    val scaleOn: Boolean get() = motion == "smooth"

    fun apply(s: LiveTvSettings) {
        if (motion != s.modernMotion) motion = s.modernMotion
        if (cardGradient != s.modernCardGradient) cardGradient = s.modernCardGradient
        if (fullBar != s.modernFullBar) fullBar = s.modernFullBar
        if (flags != s.modernFlags) flags = s.modernFlags
    }
}

/** The value after [current] in [all], wrapping round (for the settings rows that cycle on OK). */
internal fun modernNext(current: String, all: List<String>): String {
    val i = all.indexOf(current)
    return all[(i + 1).mod(all.size)]
}

/** The words shown for a stored Modern Interface choice. */
internal fun modernOptionLabel(context: Context, value: String): String = when (value) {
    "smooth" -> context.getString(R.string.iptv_settings_modern_motion_smooth)
    "light" -> context.getString(R.string.iptv_settings_modern_motion_light)
    "off" -> context.getString(R.string.iptv_settings_modern_motion_off)
    "fade" -> context.getString(R.string.iptv_settings_modern_bar_fade)
    "panel" -> context.getString(R.string.iptv_settings_modern_bar_panel)
    "full" -> context.getString(R.string.iptv_settings_modern_bar_full)
    else -> value
}

// ------------------------------------------------------------------ motion

/** A colour or alpha change: [ms] long as before; with Modern on and Motion "Off", instant. */
internal fun <T> modernFade(ms: Int): AnimationSpec<T> =
    if (ModernLook.on && !ModernPrefs.animationsOn) snap() else tween(ms)

/** Something that moves or resizes: [ms] long as before; with Modern on, only in "Smooth". */
internal fun <T> modernMove(ms: Int): AnimationSpec<T> =
    if (ModernLook.on && !ModernPrefs.scaleOn) snap() else tween(ms)

/**
 * How the full screen info bar comes in. Modern off: [default] (the bar as it always was).
 * Modern on: only the "Fade" style animates (a plain fade, one alpha layer, nothing slides);
 * "Panel" and "Full width" appear and disappear instantly.
 */
internal fun modernBarEnter(default: EnterTransition): EnterTransition = when {
    !ModernLook.on -> default
    ModernPrefs.fullBar == "fade" && ModernPrefs.animationsOn -> fadeIn(tween(160))
    else -> EnterTransition.None
}

internal fun modernBarExit(default: ExitTransition): ExitTransition = when {
    !ModernLook.on -> default
    ModernPrefs.fullBar == "fade" && ModernPrefs.animationsOn -> fadeOut(tween(160))
    else -> ExitTransition.None
}

/** The surface behind the full screen info bar for each Modern style. */
internal fun Modifier.modernBar(style: String): Modifier = when (style) {
    // A bar across the whole bottom edge.
    "full" -> this.background(ModernLook.PanelDark)
    // No card: the picture simply darkens towards the bottom (one gradient, cheap to draw).
    "fade" -> this.background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000))))
    // A floating rounded panel.
    else -> this
        .background(ModernLook.PanelDark, RoundedCornerShape(26.dp))
        .border(1.dp, ModernLook.Divider, RoundedCornerShape(26.dp))
}

// ------------------------------------------------------------------ country flags

private val notCountryCodes = setOf("TV", "HD", "SD", "FM", "AM", "PM", "AI", "XX")

private val isoCodes: Set<String> by lazy { java.util.Locale.getISOCountries().toHashSet() }

/** English country names (lower case) to their two letter code, plus the short names playlists use. */
private val countryNames: Map<String, String> by lazy {
    val m = HashMap<String, String>()
    for (code in java.util.Locale.getISOCountries()) {
        val name = runCatching {
            java.util.Locale.Builder().setRegion(code).build().getDisplayCountry(java.util.Locale.ENGLISH)
        }.getOrDefault("")
        if (name.isNotBlank() && name != code && '(' !in name) m[name.lowercase()] = code
    }
    m.putAll(
        mapOf(
            "usa" to "US", "america" to "US", "united states of america" to "US",
            "uk" to "GB", "britain" to "GB", "great britain" to "GB", "england" to "GB",
            "scotland" to "GB", "wales" to "GB", "northern ireland" to "GB",
            "turkey" to "TR", "czech republic" to "CZ", "uae" to "AE", "holland" to "NL",
            "burma" to "MM", "myanmar" to "MM", "hong kong" to "HK", "korea" to "KR",
            "ivory coast" to "CI", "cape verde" to "CV", "palestine" to "PS", "russia" to "RU"
        )
    )
    m
}

private val flagCache = HashMap<String, String?>()

private fun flagOf(code: String): String {
    val sb = StringBuilder()
    for (ch in code) sb.appendCodePoint(0x1F1E6 + (ch - 'A'))
    return sb.toString()
}

private fun computeFlag(title: String): String? {
    val words = title.split(Regex("[^\\p{L}]+")).filter { it.isNotEmpty() }.take(4)
    if (words.isEmpty()) return null
    // "US | Sports", "FR: Cinema": a leading two letter code in capitals.
    val first = words[0]
    if (first.length == 2 && first == first.uppercase() && first !in notCountryCodes) {
        val code = if (first == "UK") "GB" else first
        if (code in isoCodes) return flagOf(code)
    }
    // "Germany Sports", "Sports France", "United States": a country name in the first few words.
    val low = words.map { it.lowercase() }
    for (n in 2 downTo 1) {
        for (i in 0..(low.size - n)) {
            countryNames[low.subList(i, i + n).joinToString(" ")]?.let { return flagOf(it) }
        }
    }
    return null
}

/** The flag for a category that names a country, or null. */
internal fun modernFlagFor(title: String): String? = synchronized(flagCache) {
    if (flagCache.containsKey(title)) flagCache[title]
    else computeFlag(title).also { flagCache[title] = it }
}

/** A category's title for the list: with its country flag in front when Modern flags are on. */
internal fun modernGroupTitle(title: String, special: Boolean): String {
    if (!ModernLook.on || !ModernPrefs.flags || special) return title
    val flag = modernFlagFor(title) ?: return title
    return "$flag  $title"
}

// ------------------------------------------------------------------ match card

/**
 * A game as the design's match card: 150 tall, 18 radius, a dark base with a glow from each
 * team's colour, the two logos at the sides and the score (or "vs") in the middle. The width
 * comes from the caller. [focused] only draws the ring; the caller owns focus and clicks.
 */
@Composable
internal fun ModernMatchCard(
    ev: SportsEvent,
    use24h: Boolean,
    hideScores: Boolean,
    focused: Boolean,
    modifier: Modifier = Modifier,
    playing: Boolean = false,
    follows: SportsFollows? = null
) {
    val league = SportsCatalog.league(ev.leagueId)
    val sides = if (league != null) ev.ordered(league.sport) else listOfNotNull(ev.away, ev.home)
    if (sides.size != 2) return
    val left = bandColor(teamColor(sides[0].team))
    val right = bandColor(teamColor(sides[1].team))
    val shape = RoundedCornerShape(18.dp)
    val ring = NuvioTheme.colors.FocusRing
    val accent = NuvioTheme.colors.Secondary
    val scaleOn = ModernPrefs.scaleOn
    Box(
        modifier
            .height(150.dp)
            .graphicsLayer { val s = if (focused && scaleOn) 1.03f else 1f; scaleX = s; scaleY = s }
            .clip(shape)
            .background(ModernLook.Card)
            .drawBehind {
                if (ModernPrefs.cardGradient) {
                    drawRect(
                        Brush.radialGradient(
                            listOf(left.copy(alpha = 0.55f), Color.Transparent),
                            center = Offset(size.width * 0.18f, size.height * 0.5f),
                            radius = size.width * 0.75f
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(right.copy(alpha = 0.55f), Color.Transparent),
                            center = Offset(size.width * 0.82f, size.height * 0.5f),
                            radius = size.width * 0.75f
                        )
                    )
                }
            }
            .then(
                if (focused) Modifier.border(3.dp, ring, shape)
                else if (playing) Modifier.border(2.dp, accent, shape)
                else Modifier
            )
    ) {
        Box(Modifier.align(Alignment.TopCenter).padding(top = 10.dp)) {
            StateBadge(ev, playing = playing, use24h = use24h)
        }
        sides.forEachIndexed { i, side ->
            val followed = follows?.followsTeam(ev.leagueId, side.team.id) == true
            Column(
                Modifier
                    .align(if (i == 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(start = if (i == 0) 12.dp else 0.dp, end = if (i == 0) 0.dp else 12.dp)
                    .size(width = 76.dp, height = 96.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TeamLogo(side.team, 54.dp)
                LiveText(
                    side.team.abbr.ifBlank { side.team.name } + if (followed) " ★" else "",
                    size = 12.sp, weight = FontWeight.Bold, color = Color.White
                )
            }
        }
        Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
            if (!hideScores && ev.hasScore) {
                val a = sides[0].score.orEmpty()
                val b = sides[1].score.orEmpty()
                val da = a.toDoubleOrNull()
                val db = b.toDoubleOrNull()
                ScoreLabel(a, dim = da != null && db != null && da < db, size = 26.sp)
                LiveText("-", color = Color.White, size = 22.sp, weight = FontWeight.Black)
                ScoreLabel(b, dim = da != null && db != null && db < da, size = 26.sp)
            } else {
                LiveText("vs", color = SportsLook.Dim, size = 20.sp, weight = FontWeight.Black)
            }
        }
        val status = ev.status?.takeIf { it.isNotBlank() && ev.state != GameState.PRE }
        if (status != null) {
            LiveText(
                status,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).fillMaxWidth(0.5f),
                color = ModernLook.TextSecondary, size = 12.sp,
                align = TextAlign.Center
            )
        }
    }
}
