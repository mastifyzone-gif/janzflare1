package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.PlatformSettings
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Official Google Mobile Ads (AdMob) runtime manager controlled live via Admin PlatformSettings.
 * Supports Test Mode (Google official sample unit IDs) and Live Mode (Admin-configured unit IDs)
 * with built-in frequency capping to stay strictly compliant with Google AdMob policies.
 */
object JanzAdMobManager {
    private const val TAG = "JanzAdMob"

    // Official Google AdMob test unit IDs (safe for testing without risking account suspension)
    const val TEST_BANNER_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_VIDEO_UNIT_ID = "ca-app-pub-3940256099942544/8691691433"

    private val initialized = AtomicBoolean(false)
    private var videoInterstitial: InterstitialAd? = null
    private var videoLoadedUnitId: String? = null
    private var isLoadingVideoAd = false

    private var shortsInterstitial: InterstitialAd? = null
    private var shortsLoadedUnitId: String? = null
    private var isLoadingShortsAd = false

    private var lastFullscreenAdShownMs: Long = 0L

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

    fun resolveVideoInterstitialUnitId(settings: PlatformSettings?): String? {
        if (settings == null || !settings.adsEnabled) return null
        return if (settings.admobTestMode) {
            TEST_INTERSTITIAL_VIDEO_UNIT_ID
        } else {
            settings.admobVideoInterstitialUnitId.trim().takeIf { it.isNotEmpty() }
        }
    }

    fun resolveShortsInterstitialUnitId(settings: PlatformSettings?): String? {
        if (settings == null || !settings.adsEnabled) return null
        return if (settings.admobTestMode) {
            TEST_INTERSTITIAL_VIDEO_UNIT_ID
        } else {
            settings.admobShortsInterstitialUnitId.trim().takeIf { it.isNotEmpty() }
                ?: settings.admobVideoInterstitialUnitId.trim().takeIf { it.isNotEmpty() }
        }
    }

    fun canShowFullscreenAd(settings: PlatformSettings?): Boolean {
        if (settings == null || !settings.adsEnabled) return false
        val cooldownMs = settings.admobCooldownSec.coerceIn(30L, 1800L) * 1000L
        val elapsed = System.currentTimeMillis() - lastFullscreenAdShownMs
        return lastFullscreenAdShownMs == 0L || elapsed >= cooldownMs
    }

    fun preloadVideoInterstitial(context: Context, settings: PlatformSettings?) {
        val unitId = resolveVideoInterstitialUnitId(settings) ?: return
        ensureInitialized(context)
        if (isLoadingVideoAd) return
        if (videoInterstitial != null && videoLoadedUnitId == unitId) return

        isLoadingVideoAd = true
        runCatching {
            InterstitialAd.load(
                context.applicationContext,
                unitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        isLoadingVideoAd = false
                        videoInterstitial = ad
                        videoLoadedUnitId = unitId
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        isLoadingVideoAd = false
                        videoInterstitial = null
                        videoLoadedUnitId = null
                        Log.w(TAG, "Video interstitial failed to load: ${error.message}")
                    }
                }
            )
        }.onFailure {
            isLoadingVideoAd = false
        }
    }

    fun preloadShortsInterstitial(context: Context, settings: PlatformSettings?) {
        val unitId = resolveShortsInterstitialUnitId(settings) ?: return
        ensureInitialized(context)
        if (isLoadingShortsAd) return
        if (shortsInterstitial != null && shortsLoadedUnitId == unitId) return

        isLoadingShortsAd = true
        runCatching {
            InterstitialAd.load(
                context.applicationContext,
                unitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        isLoadingShortsAd = false
                        shortsInterstitial = ad
                        shortsLoadedUnitId = unitId
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        isLoadingShortsAd = false
                        shortsInterstitial = null
                        shortsLoadedUnitId = null
                        Log.w(TAG, "Shorts interstitial failed to load: ${error.message}")
                    }
                }
            )
        }.onFailure {
            isLoadingShortsAd = false
        }
    }

    /**
     * Displays a preloaded Google AdMob skippable video interstitial ad if available and off cooldown.
     * Returns true if the ad started showing (and will invoke [onFinished] on close/failure),
     * or false if skipped so the caller can immediately resume playback.
     */
    fun showVideoInterstitialIfReady(
        activity: Activity?,
        settings: PlatformSettings?,
        onFinished: () -> Unit
    ): Boolean {
        if (activity == null || activity.isFinishing || activity.isDestroyed) return false
        val unitId = resolveVideoInterstitialUnitId(settings) ?: return false
        if (!canShowFullscreenAd(settings)) return false

        val ad = videoInterstitial?.takeIf { videoLoadedUnitId == unitId }
        if (ad == null) {
            preloadVideoInterstitial(activity, settings)
            return false
        }

        videoInterstitial = null
        videoLoadedUnitId = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastFullscreenAdShownMs = System.currentTimeMillis()
            }

            override fun onAdDismissedFullScreenContent() {
                preloadVideoInterstitial(activity, settings)
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Video interstitial failed to show: ${adError.message}")
                preloadVideoInterstitial(activity, settings)
                onFinished()
            }
        }
        return runCatching {
            ad.show(activity)
            true
        }.getOrElse {
            preloadVideoInterstitial(activity, settings)
            false
        }
    }

    fun showShortsInterstitialIfReady(
        activity: Activity?,
        settings: PlatformSettings?,
        onFinished: () -> Unit = {}
    ): Boolean {
        if (activity == null || activity.isFinishing || activity.isDestroyed) return false
        val unitId = resolveShortsInterstitialUnitId(settings) ?: return false
        if (!canShowFullscreenAd(settings)) return false

        val ad = shortsInterstitial?.takeIf { shortsLoadedUnitId == unitId }
        if (ad == null) {
            preloadShortsInterstitial(activity, settings)
            return false
        }

        shortsInterstitial = null
        shortsLoadedUnitId = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastFullscreenAdShownMs = System.currentTimeMillis()
            }

            override fun onAdDismissedFullScreenContent() {
                preloadShortsInterstitial(activity, settings)
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Shorts interstitial failed to show: ${adError.message}")
                preloadShortsInterstitial(activity, settings)
                onFinished()
            }
        }
        return runCatching {
            ad.show(activity)
            true
        }.getOrElse {
            preloadShortsInterstitial(activity, settings)
            false
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
