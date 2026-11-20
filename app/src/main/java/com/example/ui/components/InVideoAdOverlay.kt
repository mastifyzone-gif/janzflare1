package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.PlatformSettings
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.FlareAmber
import com.example.ui.theme.FlareOrange
import kotlinx.coroutines.delay

enum class AdPlacementSlot(val label: String) {
    PRE_ROLL("Pre-Roll Ad"),
    MID_ROLL("Mid-Roll Ad"),
    POST_ROLL("Post-Roll Ad")
}

/**
 * YouTube-style in-video Sponsored Ad overlay (Pre-roll / Mid-roll / Post-roll)
 * with configurable countdown and Skip button controlled live from Admin Panel.
 */
@Composable
fun InVideoAdOverlay(
    placement: AdPlacementSlot,
    settings: PlatformSettings,
    onAdFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalSec = settings.adDurationSec.toInt().coerceIn(5, 60)
    val skipAfterSec = settings.adSkipAfterSec.toInt().coerceIn(0, totalSec)

    var elapsedSec by remember(placement, settings.adDurationSec) { mutableIntStateOf(0) }
    val remainingSec = (totalSec - elapsedSec).coerceAtLeast(0)
    val canSkip = elapsedSec >= skipAfterSec
    val skipCountdown = (skipAfterSec - elapsedSec).coerceAtLeast(0)

    LaunchedEffect(placement, totalSec) {
        elapsedSec = 0
        while (elapsedSec < totalSec) {
            delay(1000L)
            elapsedSec += 1
        }
        onAdFinished()
    }

    val adTitle = settings.adTitle.ifBlank { "JanzFlare Sponsor Spotlight" }
    val adDescription = settings.adDescription.ifBlank {
        "Supporting independent creators on JanzFlare. Video resumes right after this ad."
    }
    val hasMedia = settings.adMediaUrl.startsWith("https://")
    val hasClickUrl = settings.adClickUrl.startsWith("https://")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0E101A), Color(0xFF1A1432), Color(0xFF0A0A0F))
                )
            )
            .testTag("in_video_ad_overlay")
    ) {
        if (hasMedia) {
            AsyncImage(
                model = settings.adMediaUrl,
                contentDescription = adTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
        }

        // Top-left Sponsored pill + countdown
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = FlareAmber,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Ad",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Sponsored • ${placement.label} • 0:${remainingSec.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Center card if no image media is set
        if (!hasMedia) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = FlareOrange.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, FlareOrange.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Sponsored Ad",
                        tint = FlareOrange,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(28.dp)
                    )
                }
                Text(
                    text = adTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = adDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Bottom info + Learn More CTA + Skip button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasClickUrl) {
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(settings.adClickUrl)))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("ad_cta_button")
                    ) {
                        Text(
                            text = if (hasMedia) adTitle.take(22) else "Learn More",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(14.dp))
                    }
                } else {
                    Text(
                        text = if (hasMedia) adTitle else "Video resumes shortly",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(Modifier.width(8.dp))

                if (canSkip) {
                    Surface(
                        onClick = onAdFinished,
                        color = Color.Black.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("ad_skip_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Skip Ad",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.SkipNext, "Skip ad", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Skip in ${skipCountdown}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { (elapsedSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f) },
                color = FlareAmber,
                trackColor = Color.White.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            )
        }
    }
}
