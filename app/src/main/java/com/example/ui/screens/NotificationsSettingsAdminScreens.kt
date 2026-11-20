package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.HorizontalDivider
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.auth.signOut
import com.example.data.model.JanzCatalog
import com.example.data.model.PlatformSettings
import com.example.notifications.JanzNotificationHelper
import com.example.ui.components.AppUpdateBanner
import com.example.ui.components.EmptyStudioCard
import com.example.ui.components.ForceUpdateDialog
import com.example.ui.components.LegalPoliciesDialog
import com.example.ui.components.LegalPolicyTab
import com.example.ui.components.VerifiedAffiliationBadges
import com.example.ui.components.evaluateUpdateBannerStatus
import com.example.ui.components.formatCount
import com.example.ui.components.formatRelativeDate
import com.example.ui.components.isAppUpdateAvailable
import com.example.ui.components.normalizeExternalHttpsUrl
import com.example.ui.components.openExternalHttpsUrl
import com.example.ui.state.JanzViewModel
import com.example.ui.state.UiState
import com.example.ui.theme.FlareOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: JanzViewModel,
    onBack: () -> Unit,
    onOpenVideo: (String) -> Unit
) {
    val context = LocalContext.current
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val userState by viewModel.userAccountState.collectAsStateWithLifecycle()
    val user = (userState as? UiState.Success)?.data
    var hasNotifPerm by remember { mutableStateOf(JanzNotificationHelper.hasNotificationPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPerm = granted
        viewModel.showMessage(if (granted) "System notifications enabled" else "System notification permission denied")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Notifications", style = MaterialTheme.typography.headlineMedium) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (user != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Notification Controls", style = MaterialTheme.typography.titleMedium)
                            NotificationToggleRow("Likes", user.notifyLikes) {
                                viewModel.updateNotificationPrefs(it, user.notifyComments, user.notifyFollowers, user.notifyUploads)
                            }
                            NotificationToggleRow("Comments & Replies", user.notifyComments) {
                                viewModel.updateNotificationPrefs(user.notifyLikes, it, user.notifyFollowers, user.notifyUploads)
                            }
                            NotificationToggleRow("New Subscribers", user.notifyFollowers) {
                                viewModel.updateNotificationPrefs(user.notifyLikes, user.notifyComments, it, user.notifyUploads)
                            }
                            NotificationToggleRow("Creator Uploads", user.notifyUploads) {
                                viewModel.updateNotificationPrefs(user.notifyLikes, user.notifyComments, user.notifyFollowers, it)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPerm) {
                                OutlinedButton(
                                    onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.NotificationsActive, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Allow Mobile Status-Bar Notifications")
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = FlareOrange)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Mobile status-bar notifications are active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FlareOrange
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (notifications.isEmpty()) {
                item {
                    EmptyStudioCard(
                        title = "All Caught Up",
                        subtitle = "Likes, comments, replies, and subscriber upload alerts appear here in real time."
                    )
                }
            } else {
                items(notifications, key = { it.id }) { notif ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (notif.read) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.markNotificationRead(notif.id)
                                if (notif.targetVideoId.isNotBlank()) onOpenVideo(notif.targetVideoId)
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(notif.message, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${notif.type.uppercase()} • ${formatRelativeDate(notif.createdAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPrivacyScreen(
    viewModel: JanzViewModel,
    onBack: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val userState by viewModel.userAccountState.collectAsStateWithLifecycle()
    val allCreators by viewModel.allCreators.collectAsStateWithLifecycle()
    val platformSettings by viewModel.platformSettings.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val user = (userState as? UiState.Success)?.data
    val currentTheme = user?.themeMode ?: "dark"

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var openPolicyTab by remember { mutableStateOf<LegalPolicyTab?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings & Privacy", style = MaterialTheme.typography.headlineMedium) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Theme Selector
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("App Appearance", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("dark" to "Dark Navy", "light" to "Light", "system" to "System").forEach { (mode, label) ->
                                FilterChip(
                                    selected = currentTheme == mode,
                                    onClick = { viewModel.updateThemeMode(mode) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            // Account Security & Privacy
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Privacy & Security", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            "Signed-in Email: ${user?.email ?: "Verified Google Account"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Blocked Users: ${user?.blockedUserIds?.size ?: 0}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        user?.blockedUserIds.orEmpty().forEach { blockedUid ->
                            val blockedCreator = allCreators.firstOrNull { it.creatorId == blockedUid }
                            val blockedLabel = blockedCreator?.displayName?.takeIf { it.isNotBlank() }
                                ?: "Blocked User"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(blockedLabel, style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { viewModel.toggleBlockUser(blockedUid) }) {
                                    Icon(Icons.Default.Block, null)
                                    Text("Unblock")
                                }
                            }
                        }
                    }
                }
            }

            if (isAdmin) {
                item {
                    Button(
                        onClick = onOpenAdmin,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Admin Moderation Dashboard")
                    }
                }
            }

            // Legal, Privacy, Terms, Cookies & App Version
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Legal, Policies & App Info", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "JanzFlare follows Google Play safety policies with zero tolerance for copyright infringement, spam, or unsafe content.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openPolicyTab = LegalPolicyTab.PRIVACY }
                                .padding(vertical = 8.dp)
                                .testTag("settings_privacy_policy_row"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PrivacyTip, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Privacy Policy", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text("View", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openPolicyTab = LegalPolicyTab.TERMS }
                                .padding(vertical = 8.dp)
                                .testTag("settings_terms_row"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Gavel, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Terms of Service & Guidelines", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text("View", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openPolicyTab = LegalPolicyTab.COOKIES }
                                .padding(vertical = 8.dp)
                                .testTag("settings_cookies_row"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Cookie, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Cookie & Local Data Policy", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text("View", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isAdmin) {
                                    "JanzFlare v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
                                } else {
                                    "JanzFlare v${BuildConfig.VERSION_NAME}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isAppUpdateAvailable(platformSettings, context)) {
                                TextButton(onClick = {
                                    platformSettings?.updateUrl?.let { openExternalHttpsUrl(context, it) }
                                }) {
                                    Icon(Icons.Default.SystemUpdate, null, tint = FlareOrange, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Update", color = FlareOrange)
                                }
                            }
                        }
                    }
                }
            }

            // Sign Out & Delete Account
            item {
                OutlinedButton(
                    onClick = { signOut(credentialManager, scope) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out")
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteForever, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Account Data")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account Data?") },
            text = {
                Text("This permanently deletes your JanzFlare creator profile, uploaded videos, and account preferences.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteOwnAccount {
                            signOut(credentialManager, scope)
                        }
                    }
                ) { Text("Delete Permanently") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    openPolicyTab?.let { tab ->
        LegalPoliciesDialog(
            initialTab = tab,
            onDismiss = { openPolicyTab = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: JanzViewModel,
    onBack: () -> Unit
) {
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val creators by viewModel.adminCreators.collectAsStateWithLifecycle()
    val videos by viewModel.adminVideos.collectAsStateWithLifecycle()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val monetizationRequests by viewModel.adminMonetizationRequests.collectAsStateWithLifecycle()
    val platformSettings by viewModel.platformSettings.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Users", "Videos", "Monetize", "Reports", "Update & Share", "Ads", "Categories")

    if (!isAdmin) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Admin Panel") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
            EmptyStudioCard(
                title = "Restricted Access",
                subtitle = "Only verified platform administrators can access the Admin Dashboard.",
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Admin Moderation Panel", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
            tabs.forEachIndexed { idx, title ->
                Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(title) })
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    item {
                        Text("Live Platform Statistics", style = MaterialTheme.typography.titleLarge)
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            AdminStatCard("Total Creators", creators.size.toString(), Modifier.weight(1f))
                            AdminStatCard("Total Videos", videos.size.toString(), Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            AdminStatCard("Open Reports", reports.count { it.status == "open" }.toString(), Modifier.weight(1f))
                            AdminStatCard("Total Views", formatCount(videos.sumOf { it.viewsCount }), Modifier.weight(1f))
                        }
                    }
                }

                1 -> {
                    items(creators, key = { it.creatorId }) { creator ->
                        val isAffiliated = creator.affiliatedBrand.isNotBlank()
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${creator.displayName} (@${creator.username})",
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    VerifiedAffiliationBadges(
                                        verified = creator.verified,
                                        affiliatedBrand = creator.affiliatedBrand,
                                        founderAccount = creator.founderAccount,
                                        displayName = creator.displayName,
                                        joinedAt = creator.createdAt,
                                        iconSize = 18.dp
                                    )
                                }
                                Text(
                                    buildString {
                                        if (creator.founderAccount) append("FOUNDER • ")
                                        append("Status: ${creator.accountStatus} • ${formatCount(creator.followersCount)} subscribers")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (creator.founderAccount) FlareOrange else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        viewModel.adminSetCreatorVerified(creator.creatorId, !creator.verified)
                                    }) {
                                        Text(if (creator.verified) "Unverify" else "Verify")
                                    }
                                    OutlinedButton(onClick = {
                                        viewModel.adminSetCreatorAffiliation(
                                            creator.creatorId,
                                            if (isAffiliated) "" else "JanzFlare"
                                        )
                                    }) {
                                        Text(if (isAffiliated) "Unaffiliate" else "Affiliate JF")
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        val next = if (creator.accountStatus == "active") "suspended" else "active"
                                        viewModel.adminSetCreatorStatus(creator.creatorId, next)
                                    }) {
                                        Text(if (creator.accountStatus == "active") "Suspend" else "Activate")
                                    }
                                    TextButton(onClick = {
                                        viewModel.adminSetCreatorStatus(creator.creatorId, "banned")
                                    }) {
                                        Text("Ban", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    items(videos, key = { it.id }) { video ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(video.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "by ${video.creatorName} • ${video.moderationStatus} • Featured=${video.featured} • Trending=${video.trendingPinned}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        viewModel.adminModerateVideo(video.id, video.moderationStatus, !video.featured, video.trendingPinned)
                                    }) {
                                        Icon(Icons.Default.Star, null)
                                        Text(if (video.featured) "Unfeature" else "Feature")
                                    }
                                    OutlinedButton(onClick = {
                                        viewModel.adminModerateVideo(video.id, video.moderationStatus, video.featured, !video.trendingPinned)
                                    }) {
                                        Text(if (video.trendingPinned) "Unpin" else "Pin Trend")
                                    }
                                    TextButton(onClick = {
                                        val nextMod = if (video.moderationStatus == "removed") "approved" else "removed"
                                        viewModel.adminModerateVideo(video.id, nextMod, video.featured, video.trendingPinned)
                                    }) {
                                        Text(if (video.moderationStatus == "removed") "Approve" else "Remove")
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    if (monetizationRequests.isEmpty()) {
                        item {
                            EmptyStudioCard(
                                title = "No Monetization Requests",
                                subtitle = "Creator partner applications appear here once creators qualify and apply."
                            )
                        }
                    } else {
                        items(monetizationRequests, key = { it.creatorId }) { req ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${req.displayName} (@${req.username})", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${req.followersCount} subs • ${req.watchTimeHours}h watch • ${formatCount(req.shortsEngagementViews)} Shorts views",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "Payout: ${req.payoutMethod} • Status: ${req.status.uppercase()}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = FlareOrange
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { viewModel.adminUpdateMonetizationStatus(req.creatorId, "approved") }) {
                                            Icon(Icons.Default.CheckCircle, null)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Approve")
                                        }
                                        OutlinedButton(onClick = { viewModel.adminUpdateMonetizationStatus(req.creatorId, "rejected") }) {
                                            Text("Reject")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    if (reports.isEmpty()) {
                        item {
                            EmptyStudioCard(title = "No Reports", subtitle = "Reported videos, comments, and users appear here.")
                        }
                    } else {
                        items(reports, key = { it.id }) { report ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${report.targetType.uppercase()} Report • ${report.reason}", style = MaterialTheme.typography.titleMedium)
                                    if (report.details.isNotBlank()) {
                                        Text(report.details, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("Status: ${report.status}", style = MaterialTheme.typography.labelMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { viewModel.adminUpdateReportStatus(report.id, "reviewed") }) {
                                            Text("Review")
                                        }
                                        Button(onClick = { viewModel.adminUpdateReportStatus(report.id, "resolved") }) {
                                            Icon(Icons.Default.CheckCircle, null)
                                            Text("Resolve")
                                        }
                                        TextButton(onClick = { viewModel.adminUpdateReportStatus(report.id, "dismissed") }) {
                                            Text("Dismiss")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                5 -> {
                    item {
                        val currentCfg = platformSettings ?: PlatformSettings()
                        var versionCodeInput by remember(platformSettings) {
                            mutableStateOf(currentCfg.latestVersionCode.toString())
                        }
                        var versionNameInput by remember(platformSettings) {
                            mutableStateOf(currentCfg.latestVersionName)
                        }
                        var updateUrlInput by remember(platformSettings) {
                            mutableStateOf(currentCfg.updateUrl)
                        }
                        var updateNotesInput by remember(platformSettings) {
                            mutableStateOf(currentCfg.updateNotes)
                        }
                        var forceUpdateToggle by remember(platformSettings) {
                            mutableStateOf(currentCfg.forceUpdate)
                        }
                        var shareAppUrlInput by remember(platformSettings) {
                            mutableStateOf(currentCfg.shareAppUrl)
                        }
                        var sharePromoInput by remember(platformSettings) {
                            mutableStateOf(currentCfg.sharePromoText)
                        }
                        var showBannerPreview by remember { mutableStateOf(false) }
                        var showForcePopupPreview by remember { mutableStateOf(false) }

                        val parsedPreviewCode = versionCodeInput.toLongOrNull() ?: currentCfg.latestVersionCode
                        val liveStatus = evaluateUpdateBannerStatus(
                            latestVersionCode = parsedPreviewCode,
                            updateUrl = updateUrlInput,
                            forceUpdate = forceUpdateToggle
                        )
                        val draftSettings = currentCfg.copy(
                            latestVersionCode = parsedPreviewCode,
                            latestVersionName = versionNameInput.ifBlank { "1.0.$parsedPreviewCode" },
                            updateUrl = normalizeExternalHttpsUrl(updateUrlInput),
                            updateNotes = updateNotesInput,
                            forceUpdate = forceUpdateToggle,
                            shareAppUrl = normalizeExternalHttpsUrl(shareAppUrlInput),
                            sharePromoText = sharePromoInput.ifBlank { "Download JanzFlare App" }
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.SystemUpdate, null, tint = FlareOrange)
                                        Spacer(Modifier.width(8.dp))
                                        Text("In-App Update Banner (Telegram / APK / Play)", style = MaterialTheme.typography.titleMedium)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (liveStatus.isLiveForOlderBuilds && !liveStatus.isLiveOnThisDevice) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            FlareOrange.copy(alpha = 0.16f)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "This Installed App: Build ${BuildConfig.VERSION_CODE} (v${BuildConfig.VERSION_NAME})",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = liveStatus.summaryText,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            if (parsedPreviewCode != BuildConfig.VERSION_CODE.toLong()) {
                                                OutlinedButton(
                                                    onClick = {
                                                        versionCodeInput = BuildConfig.VERSION_CODE.toString()
                                                        versionNameInput = BuildConfig.VERSION_NAME
                                                    }
                                                ) {
                                                    Text("Set to This Release (Build ${BuildConfig.VERSION_CODE}) — Hide on New App")
                                                }
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = versionCodeInput,
                                            onValueChange = { versionCodeInput = it.filter { ch -> ch.isDigit() }.take(7) },
                                            label = { Text("Latest Version Code") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = versionNameInput,
                                            onValueChange = { versionNameInput = it.take(30) },
                                            label = { Text("Version Name") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    OutlinedTextField(
                                        value = updateUrlInput,
                                        onValueChange = { updateUrlInput = it.take(500) },
                                        label = { Text("Telegram / Update Download URL") },
                                        placeholder = { Text("https://t.me/your_channel_or_apk_link") },
                                        supportingText = { Text("t.me/... links automatically get https:// added on save") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = updateNotesInput,
                                        onValueChange = { updateNotesInput = it.take(500) },
                                        label = { Text("What's New / Update Message") },
                                        maxLines = 3,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Force Update (Mandatory)", style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                "Shows mandatory update popup on older builds (Admins always have Bypass)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(checked = forceUpdateToggle, onCheckedChange = { forceUpdateToggle = it })
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { showBannerPreview = !showBannerPreview },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(if (showBannerPreview) "Hide Banner Preview" else "Preview Banner")
                                        }
                                        OutlinedButton(
                                            onClick = { showForcePopupPreview = true },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Preview Force Popup")
                                        }
                                    }

                                    if (showBannerPreview) {
                                        AppUpdateBanner(
                                            settings = draftSettings,
                                            dismissed = false,
                                            onDismiss = { showBannerPreview = false },
                                            previewMode = true
                                        )
                                    }
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Text("External Share App Link (Non-Play Store)", style = MaterialTheme.typography.titleMedium)
                                    }
                                    Text(
                                        "Whenever any user shares a video or Short outside JanzFlare, this link is automatically attached so friends can download the app.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedTextField(
                                        value = shareAppUrlInput,
                                        onValueChange = { shareAppUrlInput = it.take(500) },
                                        label = { Text("App Download / Telegram Link") },
                                        placeholder = { Text("https://t.me/JanzFlareOfficial") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = sharePromoInput,
                                        onValueChange = { sharePromoInput = it.take(120) },
                                        label = { Text("Share Call-to-Action Label") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val parsedCode = versionCodeInput.toLongOrNull() ?: currentCfg.latestVersionCode
                                    viewModel.adminUpdatePlatformSettings(
                                        currentCfg.copy(
                                            latestVersionCode = parsedCode,
                                            latestVersionName = versionNameInput.ifBlank { "1.0.$parsedCode" },
                                            updateUrl = updateUrlInput,
                                            updateNotes = updateNotesInput,
                                            forceUpdate = forceUpdateToggle,
                                            shareAppUrl = shareAppUrlInput,
                                            sharePromoText = sharePromoInput.ifBlank { "Download JanzFlare App" }
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_save_update_share_button")
                            ) {
                                Text("Save Update & Share Settings")
                            }
                        }

                        if (showForcePopupPreview) {
                            ForceUpdateDialog(
                                settings = draftSettings,
                                isAdmin = true,
                                previewMode = true,
                                onDismissAdminOrPreview = { showForcePopupPreview = false }
                            )
                        }
                    }
                }

                6 -> {
                    item {
                        val currentCfg = platformSettings ?: PlatformSettings()
                        var adsEnabled by remember(platformSettings) { mutableStateOf(currentCfg.adsEnabled) }
                        var testMode by remember(platformSettings) { mutableStateOf(currentCfg.admobTestMode) }
                        var bannerEnabled by remember(platformSettings) { mutableStateOf(currentCfg.admobBannerEnabled) }
                        var homeFeedNative by remember(platformSettings) { mutableStateOf(currentCfg.adHomeFeedEnabled) }
                        var shortsNative by remember(platformSettings) { mutableStateOf(currentCfg.adShortsNativeEnabled) }
                        var preRoll by remember(platformSettings) { mutableStateOf(currentCfg.adPreRollEnabled) }
                        var midRoll by remember(platformSettings) { mutableStateOf(currentCfg.adMidRollEnabled) }
                        var postRoll by remember(platformSettings) { mutableStateOf(currentCfg.adPostRollEnabled) }
                        var bannerUnitId by remember(platformSettings) { mutableStateOf(currentCfg.admobBannerUnitId) }
                        var nativeUnitId by remember(platformSettings) { mutableStateOf(currentCfg.admobNativeUnitId) }
                        var skipAfterSec by remember(platformSettings) { mutableStateOf(currentCfg.adSkipAfterSec) }
                        var adDurationSec by remember(platformSettings) { mutableStateOf(currentCfg.adDurationSec) }
                        var shortsInterval by remember(platformSettings) { mutableStateOf(currentCfg.admobShortsInterval) }
                        var cooldownSec by remember(platformSettings) { mutableStateOf(currentCfg.admobCooldownSec) }
                        val isSampleAppId = BuildConfig.ADMOB_APP_ID == "ca-app-pub-3940256099942544~3347511713"

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Campaign, null, tint = FlareOrange)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Google AdMob (Native + Banner)", style = MaterialTheme.typography.titleMedium)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = if (isSampleAppId) {
                                                    "AdMob App ID: Test App ID Active"
                                                } else {
                                                    "AdMob App ID: Custom Live App ID Connected"
                                                },
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = if (isSampleAppId) FlareOrange else MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "No intrusive fullscreen popups! Uses Native Advanced (inline Home cards, 16:9 Player Box Pre/Mid/Post-Roll, swipeable Shorts) + Adaptive Banner.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Enable Google AdMob Ads", style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                "Master switch for all ads across JanzFlare",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(checked = adsEnabled, onCheckedChange = { adsEnabled = it })
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Safe Test Mode", style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                "ON = Google official test Native & Banner ads (safe while testing). Turn OFF when using your real Ad Unit IDs.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(checked = testMode, onCheckedChange = { testMode = it })
                                    }

                                    HorizontalDivider()
                                    Text("Non-Fullscreen Ad Placements", style = MaterialTheme.typography.labelLarge)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Home Feed Native Cards (Every 4 Videos)")
                                        Switch(checked = homeFeedNative, onCheckedChange = { homeFeedNative = it })
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Video Start Ad (Pre-Roll in Player Box)")
                                        Switch(checked = preRoll, onCheckedChange = { preRoll = it })
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Video Mid-Roll Ad (50% in Player Box)")
                                        Switch(checked = midRoll, onCheckedChange = { midRoll = it })
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Video End Ad (Post-Roll in Player Box)")
                                        Switch(checked = postRoll, onCheckedChange = { postRoll = it })
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Shorts Swipeable Native Ad Page")
                                        Switch(checked = shortsNative, onCheckedChange = { shortsNative = it })
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Banner Ads (Below Video & Home)")
                                        Switch(checked = bannerEnabled, onCheckedChange = { bannerEnabled = it })
                                    }

                                    HorizontalDivider()
                                    Text("Live AdMob Ad Unit IDs (Only 2 Needed!)", style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        "Create 'Native advanced' and 'Banner' units in AdMob and paste IDs here (used when Safe Test Mode is OFF).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedTextField(
                                        value = nativeUnitId,
                                        onValueChange = { nativeUnitId = it.trim().take(40) },
                                        label = { Text("Native Advanced Ad Unit ID") },
                                        placeholder = { Text("ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = bannerUnitId,
                                        onValueChange = { bannerUnitId = it.trim().take(40) },
                                        label = { Text("Banner Ad Unit ID") },
                                        placeholder = { Text("ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    HorizontalDivider()
                                    Text("Player Box Ad Skip Button Delay:", style = MaterialTheme.typography.labelLarge)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(3L to "3s", 5L to "5s", 8L to "8s").forEach { (sec, label) ->
                                            FilterChip(
                                                selected = skipAfterSec == sec,
                                                onClick = { skipAfterSec = sec },
                                                label = { Text(label) }
                                            )
                                        }
                                    }

                                    Text("Player Box Ad Max Duration:", style = MaterialTheme.typography.labelLarge)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(10L to "10s", 15L to "15s", 20L to "20s").forEach { (sec, label) ->
                                            FilterChip(
                                                selected = adDurationSec == sec,
                                                onClick = { adDurationSec = sec },
                                                label = { Text(label) }
                                            )
                                        }
                                    }

                                    Text("Gap Between In-Player Box Ads:", style = MaterialTheme.typography.labelLarge)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(0L to "Every Video", 30L to "30s", 60L to "1m", 180L to "3m").forEach { (sec, label) ->
                                            FilterChip(
                                                selected = cooldownSec == sec,
                                                onClick = { cooldownSec = sec },
                                                label = { Text(label) }
                                            )
                                        }
                                    }

                                    Text("Shorts Native Ad Interval:", style = MaterialTheme.typography.labelLarge)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(4L to "4", 5L to "5", 6L to "6", 8L to "8").forEach { (count, label) ->
                                            FilterChip(
                                                selected = shortsInterval == count,
                                                onClick = { shortsInterval = count },
                                                label = { Text("$label Shorts") }
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.adminUpdatePlatformSettings(
                                        currentCfg.copy(
                                            adsEnabled = adsEnabled,
                                            adPreRollEnabled = preRoll,
                                            adMidRollEnabled = midRoll,
                                            adPostRollEnabled = postRoll,
                                            adHomeFeedEnabled = homeFeedNative,
                                            adShortsNativeEnabled = shortsNative,
                                            adSkipAfterSec = skipAfterSec,
                                            adDurationSec = adDurationSec,
                                            admobTestMode = testMode,
                                            admobBannerEnabled = bannerEnabled,
                                            admobBannerUnitId = bannerUnitId,
                                            admobNativeUnitId = nativeUnitId,
                                            admobShortsInterval = shortsInterval,
                                            admobCooldownSec = cooldownSec
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_save_ads_button")
                            ) {
                                Text("Save Google AdMob Settings")
                            }
                        }
                    }
                }

                7 -> {
                    item {
                        val currentCfg = platformSettings ?: PlatformSettings()
                        var bannerInput by remember(platformSettings) {
                            mutableStateOf(currentCfg.featuredBannerText)
                        }
                        var noticeInput by remember(platformSettings) {
                            mutableStateOf(currentCfg.maintenanceNotice)
                        }
                        var disabledCats by remember(platformSettings) {
                            mutableStateOf(currentCfg.disabledCategories.toSet())
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Platform Notice & Categories", style = MaterialTheme.typography.titleLarge)
                            OutlinedTextField(
                                value = bannerInput,
                                onValueChange = { bannerInput = it.take(200) },
                                label = { Text("Featured Platform Banner") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = noticeInput,
                                onValueChange = { noticeInput = it.take(300) },
                                label = { Text("Maintenance / Policy Notice") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("Toggle Enabled Categories", style = MaterialTheme.typography.titleMedium)
                            JanzCatalog.allCategories.forEach { cat ->
                                val enabled = cat !in disabledCats
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(cat)
                                    Switch(
                                        checked = enabled,
                                        onCheckedChange = { isEnabled ->
                                            disabledCats = if (isEnabled) disabledCats - cat else disabledCats + cat
                                        }
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    viewModel.adminUpdatePlatformSettings(
                                        currentCfg.copy(
                                            disabledCategories = disabledCats.toList(),
                                            featuredBannerText = bannerInput,
                                            maintenanceNotice = noticeInput
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Platform Settings")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}
