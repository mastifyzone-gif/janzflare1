package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CreatorProfile
import com.example.data.model.JanzCatalog
import com.example.data.model.Playlist
import com.example.data.model.Video
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.FlareAmber
import com.example.ui.theme.FlareOrange
import com.google.firebase.Timestamp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun isLocalFileMissing(uriString: String): Boolean {
    val trimmed = uriString.trim()
    if (trimmed.isBlank()) return false
    return when {
        trimmed.startsWith("file://") -> {
            val path = Uri.parse(trimmed).path ?: return true
            val file = File(path)
            !file.exists() || file.length() <= 0L
        }
        trimmed.startsWith("/") -> {
            val file = File(trimmed)
            !file.exists() || file.length() <= 0L
        }
        else -> false
    }
}

fun isMediaUriAvailable(uriString: String): Boolean =
    uriString.isNotBlank() && !isLocalFileMissing(uriString)

@Composable
fun JanzFlareMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    animated: Boolean = false
) {
    val scale = if (animated) {
        val transition = rememberInfiniteTransition(label = "flare")
        val pulse by transition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        pulse
    } else 1f

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(percent = 22)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_janzflare),
            contentDescription = "JanzFlare logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

val VerifiedBlue = Color(0xFF1D9BF0)

@Composable
fun VerifiedAffiliationBadges(
    verified: Boolean,
    affiliatedBrand: String,
    founderAccount: Boolean,
    displayName: String,
    joinedAt: Timestamp? = null,
    iconSize: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    if (!verified && affiliatedBrand.isBlank() && !founderAccount) return

    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { showDialog = true }
            .padding(horizontal = 3.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (verified || founderAccount) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Verified account",
                tint = VerifiedBlue,
                modifier = Modifier.size(iconSize)
            )
        }
        if (affiliatedBrand.isNotBlank() || founderAccount) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, FlareOrange.copy(alpha = 0.75f)),
                modifier = Modifier.size(iconSize)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    JanzFlareMark(size = (iconSize - 3.dp).coerceAtLeast(10.dp), animated = false)
                }
            }
        }
    }

    if (showDialog) {
        AffiliationInfoDialog(
            displayName = displayName,
            verified = verified || founderAccount,
            affiliatedBrand = if (founderAccount && affiliatedBrand.isBlank()) "JanzFlare" else affiliatedBrand,
            founderAccount = founderAccount,
            joinedAt = joinedAt,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun AffiliationInfoDialog(
    displayName: String,
    verified: Boolean,
    affiliatedBrand: String,
    founderAccount: Boolean,
    joinedAt: Timestamp?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (verified) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = VerifiedBlue,
                        modifier = Modifier.size(30.dp)
                    )
                }
                if (affiliatedBrand.isNotBlank()) {
                    JanzFlareMark(size = 32.dp, animated = false)
                }
            }
        },
        title = {
            Text(
                text = if (affiliatedBrand.isNotBlank()) "Affiliated Account" else "Verified Account",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (founderAccount) {
                    Surface(
                        color = FlareOrange.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, FlareOrange.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Official Founder & Owner • JanzFlare का मालिक",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = FlareOrange,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                if (verified) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = VerifiedBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Verified on JanzFlare",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "$displayName's account has a verified blue checkmark.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (affiliatedBrand.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, FlareOrange.copy(alpha = 0.75f)),
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                JanzFlareMark(size = 16.dp, animated = false)
                            }
                        }
                        Column {
                            Text(
                                text = "Affiliated with $affiliatedBrand",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = if (founderAccount) {
                                    "$displayName is the main Founder & Owner affiliated with $affiliatedBrand (Janz Creative Studio)."
                                } else {
                                    "This account is officially affiliated with $affiliatedBrand on JanzFlare."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (joinedAt != null) {
                    Text(
                        text = "Joined JanzFlare on ${formatJoinDate(joinedAt)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}

@Composable
fun JanzBrandHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        JanzFlareMark(size = 36.dp, animated = false)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = "JanzFlare",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun CategoryChipRow(
    categories: List<String>,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onSelectCategory(null) },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("category_chip_all")
            )
        }
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = {
                    onSelectCategory(if (selectedCategory == category) null else category)
                },
                label = { Text(category) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("category_chip_${category.lowercase().replace(" ", "_")}")
            )
        }
    }
}

@Composable
fun VideoThumbnailCard(
    video: Video,
    isLiked: Boolean,
    isSaved: Boolean,
    onPlayClick: () -> Unit,
    onCreatorClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onWatchLaterClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier,
    creatorVerified: Boolean = video.creatorVerified,
    creatorAffiliatedBrand: String = "",
    creatorFounder: Boolean = false,
    creatorJoinedAt: Timestamp? = null,
    creatorAvatarUrl: String = video.creatorAvatarUrl
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val thumbAvailable = remember(video.thumbnailUri) { isMediaUriAvailable(video.thumbnailUri) }
    var thumbError by remember(video.thumbnailUri) { mutableStateOf(false) }
    val localVideoMissing = remember(video.videoUri) { isLocalFileMissing(video.videoUri) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onPlayClick)
            .testTag("video_card_${video.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Thumbnail with duration & category badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (thumbAvailable && !thumbError) {
                    AsyncImage(
                        model = video.thumbnailUri,
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        onError = { thumbError = true },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_empty_studio),
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Subtle bottom gradient for readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )

                // Play indicator
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play video",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Category & Short pill
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (video.shortForm) "SHORT" else video.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (video.featured) {
                        Surface(
                            color = ElectricViolet.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "FEATURED",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (localVideoMissing) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Video unavailable",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFB4AB),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                // Duration & Language pill
                Surface(
                    color = Color.Black.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "${formatDuration(video.durationSec)} • ${video.language}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Video Details + Creator Info
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CreatorAvatar(
                        avatarUrl = creatorAvatarUrl,
                        name = video.creatorName,
                        size = 40.dp,
                        modifier = Modifier.clickable(onClick = onCreatorClick)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(onClick = onCreatorClick)
                        ) {
                            Text(
                                text = video.creatorName,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (creatorVerified || creatorAffiliatedBrand.isNotBlank() || creatorFounder) {
                                Spacer(Modifier.width(4.dp))
                                VerifiedAffiliationBadges(
                                    verified = creatorVerified,
                                    affiliatedBrand = creatorAffiliatedBrand,
                                    founderAccount = creatorFounder,
                                    displayName = video.creatorName,
                                    joinedAt = creatorJoinedAt,
                                    iconSize = 14.dp
                                )
                            }
                            Text(
                                text = " • ${formatCount(video.viewsCount)} views • ${formatRelativeDate(video.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Watch Later") },
                                leadingIcon = { Icon(Icons.Default.WatchLater, null) },
                                onClick = {
                                    menuExpanded = false
                                    onWatchLaterClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add to Playlist") },
                                leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) },
                                onClick = {
                                    menuExpanded = false
                                    onAddToPlaylistClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Report Video") },
                                onClick = {
                                    menuExpanded = false
                                    onReportClick()
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Quick Social Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = onLikeClick) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like video",
                                tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(formatCount(video.likesCount))
                        }
                        TextButton(onClick = onCommentClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Comment,
                                contentDescription = "Comments",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(formatCount(video.commentsCount))
                        }
                        TextButton(onClick = onShareClick) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share video",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Share")
                        }
                    }
                    IconButton(onClick = onSaveClick) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save video",
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreatorAvatar(
    avatarUrl: String,
    name: String,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val canLoad = remember(avatarUrl) { isMediaUriAvailable(avatarUrl) }
    var imageFailed by remember(avatarUrl) { mutableStateOf(false) }

    if (canLoad && !imageFailed) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = "$name avatar",
            contentScale = ContentScale.Crop,
            onError = { imageFailed = true },
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(FlareOrange, ElectricViolet)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.trim().take(1).uppercase().ifBlank { "J" },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

@Composable
fun CreatorSpotlightCard(
    creator: CreatorProfile,
    isFollowing: Boolean,
    isSelf: Boolean,
    badgeLabel: String,
    onClick: () -> Unit,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier,
    notifyOnUpload: Boolean = true,
    onToggleBell: (() -> Unit)? = null
) {
    val cardBorder = if (creator.founderAccount) {
        BorderStroke(1.5.dp, Brush.linearGradient(listOf(FlareAmber, FlareOrange, ElectricViolet)))
    } else null

    Card(
        modifier = modifier
            .width(176.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = cardBorder,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = if (creator.founderAccount) FlareOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (creator.founderAccount) "FOUNDER • मालिक" else badgeLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (creator.founderAccount) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (creator.founderAccount) FlareOrange else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            CreatorAvatar(
                avatarUrl = creator.avatarUrl,
                name = creator.displayName,
                size = 54.dp
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = creator.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (creator.verified || creator.affiliatedBrand.isNotBlank() || creator.founderAccount) {
                    Spacer(Modifier.width(4.dp))
                    VerifiedAffiliationBadges(
                        verified = creator.verified,
                        affiliatedBrand = creator.affiliatedBrand,
                        founderAccount = creator.founderAccount,
                        displayName = creator.displayName,
                        joinedAt = creator.createdAt,
                        iconSize = 14.dp
                    )
                }
            }
            Text(
                text = "${formatCount(creator.followersCount)} subscribers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            if (!isSelf) {
                if (isFollowing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onFollowClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Subscribed", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                        if (onToggleBell != null) {
                            IconButton(
                                onClick = onToggleBell,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (notifyOnUpload) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                    contentDescription = if (notifyOnUpload) "Mute upload alerts" else "Enable upload alerts",
                                    tint = if (notifyOnUpload) FlareOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = onFollowClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Subscribe", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStudioCard(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_studio),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun ReportContentDialog(
    targetTitle: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, details: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf("spam") }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report $targetTitle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                JanzCatalog.reportReasons.forEach { (key, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = key }
                    ) {
                        RadioButton(
                            selected = selectedReason == key,
                            onClick = { selectedReason = key }
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it.take(500) },
                    label = { Text("Additional context (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedReason, details) }) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PlaylistPickerDialog(
    videoId: String,
    playlists: List<Playlist>,
    onTogglePlaylist: (Playlist) -> Unit,
    onCreatePlaylist: (title: String, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newTitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save to Playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (playlists.isEmpty()) {
                    Text(
                        "You don't have any playlists yet. Create one below.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    playlists.forEach { playlist ->
                        val checked = playlist.videoIds.contains(videoId)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTogglePlaylist(playlist) }
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { onTogglePlaylist(playlist) }
                            )
                            Text(
                                "${playlist.title} (${playlist.videoIds.size})",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it.take(80) },
                    label = { Text("New playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onCreatePlaylist(newTitle.trim(), "")
                            newTitle = ""
                        }
                    },
                    enabled = newTitle.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create & Add")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

fun shareVideoIntent(context: Context, video: Video) {
    val shareText = "Watch \"${video.title}\" by ${video.creatorName} on JanzFlare!\nCategory: ${video.category} • ${video.language}"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, video.title)
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share video via"))
}

fun formatDuration(seconds: Long): String {
    if (seconds <= 0L) return "0:00"
    val mins = seconds / 60L
    val secs = seconds % 60L
    val hrs = mins / 60L
    return if (hrs > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hrs, mins % 60L, secs)
    } else {
        String.format(Locale.US, "%d:%02d", mins, secs)
    }
}

fun formatCount(count: Long): String = when {
    count >= 1_000_000L -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
    count >= 1_000L -> String.format(Locale.US, "%.1fK", count / 1_000.0)
    else -> count.toString()
}

fun formatRelativeDate(ts: Timestamp?): String {
    val millis = ts?.toDate()?.time ?: return "Just now"
    val diffSec = ((System.currentTimeMillis() - millis) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60L -> "Just now"
        diffSec < 3600L -> "${diffSec / 60L}m ago"
        diffSec < 86400L -> "${diffSec / 3600L}h ago"
        diffSec < 604800L -> "${diffSec / 86400L}d ago"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
    }
}

fun formatJoinDate(ts: Timestamp?): String {
    val millis = ts?.toDate()?.time ?: System.currentTimeMillis()
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
}
