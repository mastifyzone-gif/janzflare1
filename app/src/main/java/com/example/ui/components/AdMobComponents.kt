package com.example.ui.components

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button as AndroidButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.PlatformSettings
import com.example.ui.theme.FlareOrange
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Official Google Mobile Ads (AdMob) manager supporting non-intrusive inline formats:
 * - Native Advanced (Home Feed, Video Player 16:9 Box Pre/Mid/Post-Roll, Shorts swipeable page)
 * - Adaptive Banner (Below Video Player & Explore/Home)
 * Controlled live via Admin PlatformSettings with ZERO intrusive fullscreen popups.
 */
object JanzAdMobManager {
    private const val TAG = "JanzAdMob"

    // Official Google AdMob test unit IDs (safe during testing)
    const val TEST_BANNER_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_NATIVE_VIDEO_UNIT_ID = "ca-app-pub-3940256099942544/1044960115"

    private val initialized = AtomicBoolean(false)
    private var lastPlayerBoxAdShownMs: Long = 0L

    fun ensureInitialized(context: Context) {
        if (initialized.compareAndSet(false, true)) {
            runCatching {
                MobileAds.initialize(context.applicationContext) {
                    Log.d(TAG, "Google Mobile Ads SDK initialized")
                }
            }.onFailure { err ->
                Log.w(TAG, "MobileAds initialization skipped: ${err.message}")
            }
        }
    }

    fun resolveBannerUnitId(settings: PlatformSettings?): String? {
        if (settings == null || !settings.adsEnabled || !settings.admobBannerEnabled) return null
        return if (settings.admobTestMode) {
            TEST_BANNER_UNIT_ID
        } else {
            settings.admobBannerUnitId.trim().takeIf { it.isNotEmpty() }
        }
    }

    fun resolveNativeUnitId(settings: PlatformSettings?): String? {
        if (settings == null || !settings.adsEnabled) return null
        return if (settings.admobTestMode) {
            TEST_NATIVE_VIDEO_UNIT_ID
        } else {
            settings.admobNativeUnitId.trim().takeIf { it.isNotEmpty() }
                ?: settings.admobVideoInterstitialUnitId.trim().takeIf { it.isNotEmpty() }
        }
    }

    fun canShowPlayerBoxAd(settings: PlatformSettings?, ignoreCooldown: Boolean = false): Boolean {
        if (settings == null || !settings.adsEnabled) return false
        if (resolveNativeUnitId(settings) == null) return false
        if (ignoreCooldown) return true
        val cooldownSec = settings.admobCooldownSec.coerceIn(0L, 1800L)
        if (cooldownSec == 0L || lastPlayerBoxAdShownMs == 0L) return true
        return System.currentTimeMillis() - lastPlayerBoxAdShownMs >= cooldownSec * 1000L
    }

    fun markPlayerBoxAdShown() {
        lastPlayerBoxAdShownMs = System.currentTimeMillis()
    }

    fun loadNativeAd(
        context: Context,
        unitId: String,
        startMuted: Boolean = true,
        onLoaded: (NativeAd) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        ensureInitialized(context)
        runCatching {
            val videoOptions = VideoOptions.Builder()
                .setStartMuted(startMuted)
                .build()
            val adOptions = NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                .build()

            val loader = AdLoader.Builder(context.applicationContext, unitId)
                .forNativeAd { nativeAd -> onLoaded(nativeAd) }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "Native ad failed to load: ${error.message}")
                        onFailed()
                    }
                })
                .withNativeAdOptions(adOptions)
                .build()

            loader.loadAd(AdRequest.Builder().build())
        }.onFailure { err ->
            Log.w(TAG, "Native ad request skipped: ${err.message}")
            onFailed()
        }
    }
}

/**
 * Adaptive Google AdMob Banner container controlled live by Admin PlatformSettings.
 */
@Composable
fun AdMobBannerSlot(
    settings: PlatformSettings?,
    modifier: Modifier = Modifier
) {
    val unitId = JanzAdMobManager.resolveBannerUnitId(settings) ?: return
    val context = LocalContext.current

    val adView = remember(unitId) {
        JanzAdMobManager.ensureInitialized(context)
        runCatching {
            AdView(context).apply {
                val displayMetrics = context.resources.displayMetrics
                val adWidthDp = (displayMetrics.widthPixels / displayMetrics.density).toInt().coerceAtLeast(320)
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp))
                adUnitId = unitId
                loadAd(AdRequest.Builder().build())
            }
        }.getOrNull()
    } ?: return

    DisposableEffect(adView) {
        onDispose {
            runCatching { adView.destroy() }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .testTag("admob_banner_slot"),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Inline Native Advanced Ad Card for Home Feed and below Video Player.
 * Blends naturally with video cards, carries a clear "Ad" badge, and collapses cleanly if unavailable.
 */
@Composable
fun AdMobNativeFeedCard(
    settings: PlatformSettings?,
    slotKey: String,
    modifier: Modifier = Modifier
) {
    val unitId = JanzAdMobManager.resolveNativeUnitId(settings) ?: return
    val context = LocalContext.current
    var loadedAd by remember(unitId, slotKey) { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(unitId, slotKey) {
        var disposed = false
        JanzAdMobManager.loadNativeAd(
            context = context,
            unitId = unitId,
            startMuted = true,
            onLoaded = { ad ->
                if (disposed) {
                    runCatching { ad.destroy() }
                } else {
                    loadedAd?.let { old -> runCatching { old.destroy() } }
                    loadedAd = ad
                }
            },
            onFailed = {}
        )
        onDispose {
            disposed = true
            loadedAd?.let { runCatching { it.destroy() } }
            loadedAd = null
        }
    }

    val nativeAd = loadedAd ?: return
    val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()
    val titleColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val accentColor = FlareOrange.toArgb()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admob_native_feed_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        AndroidView(
            factory = { ctx ->
                buildNativeFeedAdView(ctx, nativeAd, surfaceColor, titleColor, bodyColor, accentColor)
            },
            update = { adView ->
                populateNativeFeedAdView(adView, nativeAd)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * In-Player-Box Native Advanced Ad (Pre-Roll / Mid-Roll / Post-Roll).
 * Plays strictly INSIDE the video player's 16:9 / theater container — NEVER a full-screen popup.
 * Displays a clear "Ad" label and a policy-safe Skip button outside the clickable ad area.
 */
@Composable
fun AdMobPlayerBoxNativeAd(
    settings: PlatformSettings?,
    slotLabel: String,
    onAdFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unitId = JanzAdMobManager.resolveNativeUnitId(settings)
    if (unitId == null) {
        LaunchedEffect(Unit) { onAdFinished() }
        return
    }

    val context = LocalContext.current
    val totalDurationSec = (settings?.adDurationSec ?: 10L).coerceIn(5L, 30L).toInt()
    val skipAfterSec = (settings?.adSkipAfterSec ?: 5L).coerceIn(3L, totalDurationSec.toLong()).toInt()

    var loadedAd by remember(unitId, slotLabel) { mutableStateOf<NativeAd?>(null) }
    var loadFailed by remember(unitId, slotLabel) { mutableStateOf(false) }
    var elapsedSec by remember(unitId, slotLabel) { mutableIntStateOf(0) }

    DisposableEffect(unitId, slotLabel) {
        var disposed = false
        JanzAdMobManager.loadNativeAd(
            context = context,
            unitId = unitId,
            startMuted = false,
            onLoaded = { ad ->
                if (disposed) {
                    runCatching { ad.destroy() }
                } else {
                    JanzAdMobManager.markPlayerBoxAdShown()
                    loadedAd = ad
                }
            },
            onFailed = {
                if (!disposed) loadFailed = true
            }
        )
        onDispose {
            disposed = true
            loadedAd?.let { runCatching { it.destroy() } }
            loadedAd = null
        }
    }

    // If ad doesn't load within 4 seconds or fails, resume video seamlessly without blocking user
    LaunchedEffect(loadedAd, loadFailed) {
        if (loadFailed) {
            onAdFinished()
            return@LaunchedEffect
        }
        if (loadedAd == null) {
            delay(4000L)
            if (loadedAd == null) {
                onAdFinished()
            }
            return@LaunchedEffect
        }
        while (elapsedSec < totalDurationSec) {
            delay(1000L)
            elapsedSec += 1
        }
        onAdFinished()
    }

    val nativeAd = loadedAd ?: return
    val canSkip = elapsedSec >= skipAfterSec
    val remainingToSkip = (skipAfterSec - elapsedSec).coerceAtLeast(0)
    val remainingTotal = (totalDurationSec - elapsedSec).coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("admob_player_box_native_ad")
    ) {
        AndroidView(
            factory = { ctx ->
                buildPlayerBoxNativeAdView(ctx, nativeAd, FlareOrange.toArgb())
            },
            update = { adView ->
                populatePlayerBoxNativeAdView(adView, nativeAd)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Policy-safe Ad countdown badge at top-left (outside clickable ad container)
        Surface(
            color = Color.Black.copy(alpha = 0.72f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Text(
                text = "Ad • ${remainingTotal}s",
                color = Color(0xFFFFD54F),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Policy-safe Skip button at top-right (separate from NativeAdView click targets)
        Surface(
            onClick = { if (canSkip) onAdFinished() },
            enabled = canSkip,
            color = if (canSkip) Color.Black.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .testTag("admob_player_skip_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (canSkip) "Skip Ad" else "Skip in ${remainingToSkip}s",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (canSkip) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip ad",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Swipeable Native Advanced Ad Page inside the Shorts VerticalPager.
 * Appears like a sponsored Short so users simply swipe up to keep watching — no fullscreen popup!
 */
@Composable
fun AdMobShortsNativePage(
    settings: PlatformSettings?,
    pageKey: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val unitId = JanzAdMobManager.resolveNativeUnitId(settings)
    val context = LocalContext.current
    var loadedAd by remember(unitId, pageKey) { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(unitId, pageKey) {
        var disposed = false
        if (unitId != null) {
            JanzAdMobManager.loadNativeAd(
                context = context,
                unitId = unitId,
                startMuted = !isActive,
                onLoaded = { ad ->
                    if (disposed) {
                        runCatching { ad.destroy() }
                    } else {
                        loadedAd?.let { old -> runCatching { old.destroy() } }
                        loadedAd = ad
                    }
                }
            )
        }
        onDispose {
            disposed = true
            loadedAd?.let { runCatching { it.destroy() } }
            loadedAd = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E17))
            .testTag("admob_shorts_native_page"),
        contentAlignment = Alignment.Center
    ) {
        val nativeAd = loadedAd
        if (nativeAd != null) {
            AndroidView(
                factory = { ctx ->
                    buildShortsNativeAdView(ctx, nativeAd, FlareOrange.toArgb())
                },
                update = { adView ->
                    populateShortsNativeAdView(adView, nativeAd)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 48.dp)
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Sponsored",
                    color = Color(0xFFFFD54F),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "Swipe up for the next Short",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Swipe-up hint at bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Swipe up for next Short",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun Context.dpToPx(dp: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()

private fun buildNativeFeedAdView(
    context: Context,
    nativeAd: NativeAd,
    surfaceColor: Int,
    titleColor: Int,
    bodyColor: Int,
    accentColor: Int
): NativeAdView {
    val adView = NativeAdView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setBackgroundColor(surfaceColor)
    }

    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    val mediaView = MediaView(context).apply {
        val widthPx = context.resources.displayMetrics.widthPixels - context.dpToPx(32)
        val heightPx = (widthPx * 9 / 16).coerceAtLeast(context.dpToPx(180))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx)
        setImageScaleType(ImageView.ScaleType.CENTER_CROP)
    }
    root.addView(mediaView)

    val infoContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(context.dpToPx(14), context.dpToPx(12), context.dpToPx(14), context.dpToPx(14))
    }

    val headerRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    val iconView = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(context.dpToPx(40), context.dpToPx(40)).apply {
            marginEnd = context.dpToPx(10)
        }
        scaleType = ImageView.ScaleType.CENTER_CROP
    }
    headerRow.addView(iconView)

    val textCol = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }

    val badgeAndAdvertiser = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    val adBadge = TextView(context).apply {
        text = "Ad"
        textSize = 10f
        setTypeface(null, Typeface.BOLD)
        setTextColor(android.graphics.Color.BLACK)
        setPadding(context.dpToPx(6), context.dpToPx(2), context.dpToPx(6), context.dpToPx(2))
        background = GradientDrawable().apply {
            setColor(0xFFFFD54F.toInt())
            cornerRadius = context.dpToPx(4).toFloat()
        }
    }
    val advertiserView = TextView(context).apply {
        textSize = 11f
        setTextColor(bodyColor)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        setPadding(context.dpToPx(6), 0, 0, 0)
    }
    badgeAndAdvertiser.addView(adBadge)
    badgeAndAdvertiser.addView(advertiserView)

    val headlineView = TextView(context).apply {
        textSize = 15f
        setTypeface(null, Typeface.BOLD)
        setTextColor(titleColor)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        setPadding(0, context.dpToPx(2), 0, 0)
    }
    textCol.addView(badgeAndAdvertiser)
    textCol.addView(headlineView)
    headerRow.addView(textCol)

    val ctaButton = AndroidButton(context).apply {
        textSize = 12f
        isAllCaps = false
        setTypeface(null, Typeface.BOLD)
        setTextColor(android.graphics.Color.WHITE)
        background = GradientDrawable().apply {
            setColor(accentColor)
            cornerRadius = context.dpToPx(12).toFloat()
        }
        setPadding(context.dpToPx(14), context.dpToPx(6), context.dpToPx(14), context.dpToPx(6))
        minHeight = context.dpToPx(38)
        minimumHeight = context.dpToPx(38)
    }
    headerRow.addView(ctaButton)
    infoContainer.addView(headerRow)

    val bodyView = TextView(context).apply {
        textSize = 12.5f
        setTextColor(bodyColor)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        setPadding(0, context.dpToPx(8), 0, 0)
    }
    infoContainer.addView(bodyView)

    root.addView(infoContainer)
    adView.addView(root)

    adView.mediaView = mediaView
    adView.iconView = iconView
    adView.headlineView = headlineView
    adView.advertiserView = advertiserView
    adView.bodyView = bodyView
    adView.callToActionView = ctaButton

    populateNativeFeedAdView(adView, nativeAd)
    return adView
}

private fun populateNativeFeedAdView(adView: NativeAdView, nativeAd: NativeAd) {
    (adView.headlineView as? TextView)?.text = nativeAd.headline.orEmpty()
    val body = nativeAd.body
    (adView.bodyView as? TextView)?.apply {
        if (body.isNullOrBlank()) {
            visibility = View.GONE
        } else {
            visibility = View.VISIBLE
            text = body
        }
    }
    val advertiser = nativeAd.advertiser
    (adView.advertiserView as? TextView)?.apply {
        text = if (advertiser.isNullOrBlank()) "Sponsored" else advertiser
    }
    val icon = nativeAd.icon
    (adView.iconView as? ImageView)?.apply {
        if (icon?.drawable != null) {
            visibility = View.VISIBLE
            setImageDrawable(icon.drawable)
        } else {
            visibility = View.GONE
        }
    }
    val cta = nativeAd.callToAction
    (adView.callToActionView as? AndroidButton)?.apply {
        if (cta.isNullOrBlank()) {
            visibility = View.GONE
        } else {
            visibility = View.VISIBLE
            text = cta
        }
    }
    nativeAd.mediaContent?.let { adView.mediaView?.mediaContent = it }
    adView.setNativeAd(nativeAd)
}

private fun buildPlayerBoxNativeAdView(
    context: Context,
    nativeAd: NativeAd,
    accentColor: Int
): NativeAdView {
    val adView = NativeAdView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(android.graphics.Color.BLACK)
    }

    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val mediaView = MediaView(context).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        setImageScaleType(ImageView.ScaleType.FIT_CENTER)
    }
    root.addView(mediaView)

    val bottomBar = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(0xDD111420.toInt())
        setPadding(context.dpToPx(12), context.dpToPx(8), context.dpToPx(12), context.dpToPx(8))
    }

    val iconView = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(context.dpToPx(32), context.dpToPx(32)).apply {
            marginEnd = context.dpToPx(8)
        }
        scaleType = ImageView.ScaleType.CENTER_CROP
    }
    bottomBar.addView(iconView)

    val textCol = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }
    val headlineView = TextView(context).apply {
        textSize = 13f
        setTypeface(null, Typeface.BOLD)
        setTextColor(android.graphics.Color.WHITE)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    val advertiserView = TextView(context).apply {
        textSize = 11f
        setTextColor(0xFFB8C0D4.toInt())
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    textCol.addView(headlineView)
    textCol.addView(advertiserView)
    bottomBar.addView(textCol)

    val ctaButton = AndroidButton(context).apply {
        textSize = 11.5f
        isAllCaps = false
        setTypeface(null, Typeface.BOLD)
        setTextColor(android.graphics.Color.WHITE)
        background = GradientDrawable().apply {
            setColor(accentColor)
            cornerRadius = context.dpToPx(10).toFloat()
        }
        setPadding(context.dpToPx(12), context.dpToPx(4), context.dpToPx(12), context.dpToPx(4))
        minHeight = context.dpToPx(34)
        minimumHeight = context.dpToPx(34)
    }
    bottomBar.addView(ctaButton)

    root.addView(bottomBar)
    adView.addView(root)

    adView.mediaView = mediaView
    adView.iconView = iconView
    adView.headlineView = headlineView
    adView.advertiserView = advertiserView
    adView.callToActionView = ctaButton

    populatePlayerBoxNativeAdView(adView, nativeAd)
    return adView
}

private fun populatePlayerBoxNativeAdView(adView: NativeAdView, nativeAd: NativeAd) {
    (adView.headlineView as? TextView)?.text = nativeAd.headline.orEmpty()
    (adView.advertiserView as? TextView)?.text = nativeAd.advertiser?.takeIf { it.isNotBlank() } ?: "Sponsored"
    val icon = nativeAd.icon
    (adView.iconView as? ImageView)?.apply {
        if (icon?.drawable != null) {
            visibility = View.VISIBLE
            setImageDrawable(icon.drawable)
        } else {
            visibility = View.GONE
        }
    }
    val cta = nativeAd.callToAction
    (adView.callToActionView as? AndroidButton)?.apply {
        if (cta.isNullOrBlank()) {
            visibility = View.GONE
        } else {
            visibility = View.VISIBLE
            text = cta
        }
    }
    nativeAd.mediaContent?.let { adView.mediaView?.mediaContent = it }
    adView.setNativeAd(nativeAd)
}

private fun buildShortsNativeAdView(
    context: Context,
    nativeAd: NativeAd,
    accentColor: Int
): NativeAdView {
    val adView = NativeAdView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        background = GradientDrawable().apply {
            setColor(0xFF151929.toInt())
            cornerRadius = context.dpToPx(20).toFloat()
        }
    }

    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(context.dpToPx(16), context.dpToPx(16), context.dpToPx(16), context.dpToPx(18))
    }

    val badge = TextView(context).apply {
        text = "Sponsored • Ad"
        textSize = 11f
        setTypeface(null, Typeface.BOLD)
        setTextColor(android.graphics.Color.BLACK)
        setPadding(context.dpToPx(8), context.dpToPx(3), context.dpToPx(8), context.dpToPx(3))
        background = GradientDrawable().apply {
            setColor(0xFFFFD54F.toInt())
            cornerRadius = context.dpToPx(6).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = context.dpToPx(12)
        }
    }
    root.addView(badge)

    val mediaView = MediaView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            context.dpToPx(240)
        ).apply {
            bottomMargin = context.dpToPx(14)
        }
        setImageScaleType(ImageView.ScaleType.FIT_CENTER)
    }
    root.addView(mediaView)

    val headlineView = TextView(context).apply {
        textSize = 18f
        setTypeface(null, Typeface.BOLD)
        setTextColor(android.graphics.Color.WHITE)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
    }
    root.addView(headlineView)

    val bodyView = TextView(context).apply {
        textSize = 13.5f
        setTextColor(0xFFC5CCE0.toInt())
        maxLines = 3
        ellipsize = TextUtils.TruncateAt.END
        setPadding(0, context.dpToPx(6), 0, context.dpToPx(14))
    }
    root.addView(bodyView)

    val ctaButton = AndroidButton(context).apply {
        textSize = 14f
        isAllCaps = false
        setTypeface(null, Typeface.BOLD)
        setTextColor(android.graphics.Color.WHITE)
        background = GradientDrawable().apply {
            setColor(accentColor)
            cornerRadius = context.dpToPx(14).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            context.dpToPx(46)
        )
    }
    root.addView(ctaButton)

    adView.addView(root)
    adView.mediaView = mediaView
    adView.headlineView = headlineView
    adView.bodyView = bodyView
    adView.callToActionView = ctaButton

    populateShortsNativeAdView(adView, nativeAd)
    return adView
}

private fun populateShortsNativeAdView(adView: NativeAdView, nativeAd: NativeAd) {
    (adView.headlineView as? TextView)?.text = nativeAd.headline.orEmpty()
    val body = nativeAd.body
    (adView.bodyView as? TextView)?.apply {
        if (body.isNullOrBlank()) {
            visibility = View.GONE
        } else {
            visibility = View.VISIBLE
            text = body
        }
    }
    val cta = nativeAd.callToAction
    (adView.callToActionView as? AndroidButton)?.apply {
        if (cta.isNullOrBlank()) {
            visibility = View.GONE
        } else {
            visibility = View.VISIBLE
            text = cta
        }
    }
    nativeAd.mediaContent?.let { adView.mediaView?.mediaContent = it }
    adView.setNativeAd(nativeAd)
}
