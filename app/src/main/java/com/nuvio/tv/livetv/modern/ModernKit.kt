package com.nuvio.tv.livetv.modern

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.nuvio.tv.livetv.ui.LiveCellColors
import com.nuvio.tv.livetv.ui.liveCellColors
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
import androidx.compose.foundation.layout.fillMaxSize
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

// ------------------------------------------------------------------ rows, tags

/**
 * Colors for a category / channel row: idle = card, on screen (selected) = the grey with NO outline,
 * focused = the theme's own focus look. Changes fade over 140 ms (instant when Motion is Off).
 */
@Composable
internal fun modernRowColors(focused: Boolean, selected: Boolean): LiveCellColors {
    val f = liveCellColors(true, Color.Transparent)
    val bgTarget = when {
        focused -> f.background
        selected -> ModernLook.CardRaised
        else -> ModernLook.Card
    }
    val textTarget = when {
        focused -> f.text
        selected -> ModernLook.TextPrimary
        else -> ModernLook.TextSecondary
    }
    val bg by animateColorAsState(bgTarget, modernFade(140), label = "modernRowBg")
    val text by animateColorAsState(textTarget, modernFade(140), label = "modernRowText")
    return LiveCellColors(bg, if (focused) f.border else Color.Transparent, text)
}

/** The small red LIVE tag shown on the channel that is playing. */
@Composable
internal fun ModernLiveTag(modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(5.dp)).background(ModernLook.LiveRed)
            .padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        LiveText("LIVE", color = Color.White, size = 9.sp, weight = FontWeight.Black)
    }
}

// ------------------------------------------------------------------ country flags

private val notCountryCodes = setOf("TV", "HD", "SD", "FM", "AM", "PM", "AI", "XX", "AR", "EN", "VIP", "PPV", "UHD", "FHD")

private val isoCodes: Set<String> by lazy { java.util.Locale.getISOCountries().toHashSet() }

/** What playlists call countries besides the plain English name ("Moroccan", "UK", "Holland"…). */
private val countryAliases: Map<String, String> = mapOf(
    "usa" to "US", "america" to "US", "american" to "US", "united states of america" to "US",
    "uk" to "GB", "britain" to "GB", "great britain" to "GB", "british" to "GB", "england" to "GB",
    "english" to "GB", "scotland" to "GB", "scottish" to "GB", "wales" to "GB", "welsh" to "GB",
    "northern ireland" to "GB",
    "turkey" to "TR", "turkiye" to "TR", "turkish" to "TR", "czech republic" to "CZ", "czechia" to "CZ",
    "czech" to "CZ", "uae" to "AE", "emirati" to "AE", "holland" to "NL", "dutch" to "NL",
    "burma" to "MM", "myanmar" to "MM", "hong kong" to "HK", "korea" to "KR", "korean" to "KR",
    "ivory coast" to "CI", "cape verde" to "CV", "palestine" to "PS", "palestinian" to "PS",
    "russia" to "RU", "russian" to "RU",
    "moroccan" to "MA", "algerian" to "DZ", "tunisian" to "TN", "egyptian" to "EG", "libyan" to "LY",
    "saudi" to "SA", "qatari" to "QA", "kuwaiti" to "KW", "lebanese" to "LB", "syrian" to "SY",
    "iraqi" to "IQ", "jordanian" to "JO", "omani" to "OM", "bahraini" to "BH", "yemeni" to "YE",
    "sudanese" to "SD", "mauritanian" to "MR", "somali" to "SO",
    "french" to "FR", "spanish" to "ES", "italian" to "IT", "german" to "DE", "portuguese" to "PT",
    "irish" to "IE", "belgian" to "BE", "swiss" to "CH", "austrian" to "AT", "greek" to "GR",
    "ukrainian" to "UA", "polish" to "PL", "romanian" to "RO", "bulgarian" to "BG", "serbian" to "RS",
    "croatian" to "HR", "albanian" to "AL", "bosnian" to "BA", "slovenian" to "SI", "slovak" to "SK",
    "hungarian" to "HU", "swedish" to "SE", "norwegian" to "NO", "danish" to "DK", "finnish" to "FI",
    "icelandic" to "IS", "lithuanian" to "LT", "latvian" to "LV", "estonian" to "EE", "georgian" to "GE",
    "armenian" to "AM", "azerbaijani" to "AZ", "canadian" to "CA", "mexican" to "MX",
    "brazilian" to "BR", "argentine" to "AR", "argentinian" to "AR", "colombian" to "CO",
    "chilean" to "CL", "peruvian" to "PE", "venezuelan" to "VE", "cuban" to "CU", "indian" to "IN",
    "pakistani" to "PK", "bangladeshi" to "BD", "afghan" to "AF", "iranian" to "IR", "israeli" to "IL",
    "chinese" to "CN", "japanese" to "JP", "thai" to "TH", "vietnamese" to "VN", "filipino" to "PH",
    "indonesian" to "ID", "malaysian" to "MY", "nigerian" to "NG", "ghanaian" to "GH", "kenyan" to "KE",
    "ethiopian" to "ET", "senegalese" to "SN", "cameroonian" to "CM", "ivorian" to "CI",
    "south african" to "ZA", "australian" to "AU", "kosovo" to "XK"
)

/** English country names (lower case) to their two letter code, plus the aliases above. */
private val countryNames: Map<String, String> by lazy {
    val m = HashMap<String, String>()
    for (code in java.util.Locale.getISOCountries()) {
        val name = runCatching {
            java.util.Locale.Builder().setRegion(code).build().getDisplayCountry(java.util.Locale.ENGLISH)
        }.getOrDefault("")
        if (name.isNotBlank() && name != code && '(' !in name) m[name.lowercase()] = code
    }
    m.putAll(countryAliases)
    m
}

private val flagCache = HashMap<String, String?>()

private fun computeFlag(title: String): String? {
    val words = title.split(Regex("[^\\p{L}]+")).filter { it.isNotEmpty() }.take(5)
    if (words.isEmpty()) return null
    // "US | Sports", "FR: Cinema", "DZ - Algerie": a leading two letter code in capitals.
    val first = words[0]
    if (first.length == 2 && first == first.uppercase() && first !in notCountryCodes) {
        val code = if (first == "UK") "GB" else first
        if (code in isoCodes) return code.lowercase()
    }
    // "Algeria", "Moroccan Sports", "Sports France", "South Africa": a country or demonym in the first words.
    val low = words.map { it.lowercase() }
    for (n in 2 downTo 1) {
        for (i in 0..(low.size - n)) {
            countryNames[low.subList(i, i + n).joinToString(" ")]?.let { return it.lowercase() }
        }
    }
    // A code after the first word ("Sports | DZ").
    for (w in words.drop(1)) {
        if (w.length == 2 && w == w.uppercase() && w !in notCountryCodes) {
            val code = if (w == "UK") "GB" else w
            if (code in isoCodes) return code.lowercase()
        }
    }
    return null
}

/** The lower-case country code for a category that names a country (flag file name), or null. */
internal fun modernFlagCode(title: String): String? = synchronized(flagCache) {
    if (flagCache.containsKey(title)) flagCache[title]
    else computeFlag(title).also { flagCache[title] = it }
}

/** The code to draw a flag for on this category, or null (Modern off, flags off, or a global/special group). */
internal fun modernGroupFlag(title: String, special: Boolean): String? {
    if (!ModernLook.on || !ModernPrefs.flags || special) return null
    return modernFlagCode(title)
}

private val flagBitmaps = android.util.LruCache<String, ImageBitmap>(320)

/** A round country flag (bundled 56 px picture, decoded once and kept). */
@Composable
internal fun ModernFlag(code: String, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState<ImageBitmap?>(flagBitmaps.get(code), code) {
        if (value == null) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    context.assets.open("flags/$code.png").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                }.getOrNull()
            }
            if (loaded != null) {
                flagBitmaps.put(code, loaded)
                value = loaded
            }
        }
    }
    val shown = bitmap
    if (shown != null) {
        Image(shown, contentDescription = null, modifier = modifier.size(size).clip(CircleShape))
    } else {
        Spacer(modifier.size(size))
    }
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
    follows: SportsFollows? = null,
    /** Fill the space given (the Sports strip's cards) instead of the 150 dp tall card. */
    fill: Boolean = false,
    /** A line along the bottom (the channel the game is on) instead of the game's status. */
    footer: String? = null
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
            .then(if (fill) Modifier.fillMaxSize() else Modifier.height(150.dp))
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
        Box(Modifier.align(Alignment.TopCenter).padding(top = if (fill) 5.dp else 10.dp)) {
            StateBadge(ev, playing = playing, use24h = use24h)
        }
        sides.forEachIndexed { i, side ->
            val followed = follows?.followsTeam(ev.leagueId, side.team.id) == true
            Column(
                Modifier
                    .align(if (i == 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(start = if (i == 0) 12.dp else 0.dp, end = if (i == 0) 0.dp else 12.dp)
                    .size(width = if (fill) 66.dp else 76.dp, height = if (fill) 64.dp else 96.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TeamLogo(side.team, if (fill) 34.dp else 54.dp)
                LiveText(
                    side.team.abbr.ifBlank { side.team.name } + if (followed) " ★" else "",
                    size = if (fill) 11.sp else 12.sp, weight = FontWeight.Bold, color = Color.White
                )
            }
        }
        Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
            if (!hideScores && ev.hasScore) {
                val a = sides[0].score.orEmpty()
                val b = sides[1].score.orEmpty()
                val da = a.toDoubleOrNull()
                val db = b.toDoubleOrNull()
                ScoreLabel(a, dim = da != null && db != null && da < db, size = if (fill) 21.sp else 26.sp)
                LiveText("-", color = Color.White, size = 22.sp, weight = FontWeight.Black)
                ScoreLabel(b, dim = da != null && db != null && db < da, size = if (fill) 21.sp else 26.sp)
            } else {
                LiveText("vs", color = SportsLook.Dim, size = 20.sp, weight = FontWeight.Black)
            }
        }
        val status = footer?.takeIf { it.isNotBlank() }
            ?: ev.status?.takeIf { it.isNotBlank() && ev.state != GameState.PRE }
        if (status != null) {
            LiveText(
                status,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (fill) 5.dp else 10.dp).fillMaxWidth(if (fill) 0.8f else 0.5f),
                color = ModernLook.TextSecondary, size = if (fill) 10.sp else 12.sp,
                align = TextAlign.Center
            )
        }
    }
}
