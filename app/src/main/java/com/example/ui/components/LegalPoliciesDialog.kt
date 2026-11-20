package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

enum class LegalPolicyTab(val index: Int, val label: String) {
    PRIVACY(0, "Privacy"),
    TERMS(1, "Terms"),
    COOKIES(2, "Cookies")
}

@Composable
fun LegalPoliciesDialog(
    initialTab: LegalPolicyTab = LegalPolicyTab.PRIVACY,
    onDismiss: () -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.index) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 640.dp)
                .testTag("legal_policies_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (selectedTab) {
                                0 -> Icons.Default.PrivacyTip
                                1 -> Icons.Default.Gavel
                                else -> Icons.Default.Cookie
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "JanzFlare Policies & Legal",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close policies")
                    }
                }

                TabRow(selectedTabIndex = selectedTab) {
                    LegalPolicyTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab.index,
                            onClick = { selectedTab = tab.index },
                            text = { Text(tab.label, maxLines = 1) },
                            modifier = Modifier.testTag("legal_tab_${tab.label.lowercase()}")
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> PrivacyPolicyContent()
                        1 -> TermsOfServiceContent()
                        else -> CookieAndDataPolicyContent()
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("legal_close_button")
                    ) {
                        Text("I Understand")
                    }
                }
            }
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PrivacyPolicyContent() {
    Text(
        text = "Effective Date: 2025 • JanzFlare (Janz Creative Studio)",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary
    )
    PolicySection(
        title = "1. Information We Collect",
        body = "When you sign in with Google, JanzFlare stores your Google Account UID, email address (kept strictly private to you and authorized administrators), display name, handle, and optional profile photos/channel links. When you upload videos, Shorts, comments, likes, or playlists, that content is stored to power your channel and discovery feed."
    )
    PolicySection(
        title = "2. Cloud Storage & Third-Party Services",
        body = "JanzFlare uses Google Firebase Authentication & Cloud Firestore (over encrypted HTTPS/TLS) for account data, analytics counters, and moderation, and Cloudinary CDN for secure video, thumbnail, avatar, and cover image delivery. We never sell your personal information."
    )
    PolicySection(
        title = "3. Device Permissions & Least Privilege",
        body = "JanzFlare uses the zero-permission Android Photo Picker when you choose videos or images from your gallery. Notification permission (Android 13+) is optional and used solely for upload progress and creator activity alerts you control in Settings."
    )
    PolicySection(
        title = "4. Account & Data Deletion",
        body = "You may permanently delete your account, channel profile, and uploaded videos at any time from Settings & Privacy -> Delete Account Data. For support or data inquiries, contact Founder & Data Protection Officer at mdayubjanj@gmail.com."
    )
}

@Composable
private fun TermsOfServiceContent() {
    Text(
        text = "JanzFlare Community Guidelines & Creator Terms",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary
    )
    PolicySection(
        title = "1. Your Content & Ownership",
        body = "Creators retain ownership of the original videos and Shorts they upload. By publishing on JanzFlare, you grant JanzFlare a worldwide, non-exclusive license to host, transcode, stream, and share your public content within the platform."
    )
    PolicySection(
        title = "2. Prohibited Content & Copyright (DMCA)",
        body = "You must not upload copyrighted material without permission, hate speech, harassment, sexually explicit content, dangerous violence, spam, or misleading manipulation (such as fake views or automated bots). Violating content may be removed and accounts suspended or permanently banned."
    )
    PolicySection(
        title = "3. Views, Engagement & Monetization Eligibility",
        body = "JanzFlare distinguishes Public Views from Monetization-Eligible Engaged Views (verified sustained watch sessions). Creators must reach at least 500 authentic subscribers plus either 1,000 public watch hours or 100,000 Engaged Shorts Views, and pass manual admin review, before monetization payouts are approved."
    )
    PolicySection(
        title = "4. Sponsored Ads & External Links",
        body = "JanzFlare may display clearly labeled Sponsored In-Video Ads (pre-roll, mid-roll, or post-roll) to support platform hosting and creator monetization. External channel or update links open safely in your browser and are governed by their respective websites."
    )
}

@Composable
private fun CookieAndDataPolicyContent() {
    Text(
        text = "Cookies, Local Cache & Telemetry Policy",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary
    )
    PolicySection(
        title = "1. Authentication & Session Tokens",
        body = "JanzFlare stores encrypted Firebase Authentication tokens locally on your device so you remain securely signed in without re-entering credentials on every launch. Unencrypted cloud backups of app data are disabled (allowBackup=false)."
    )
    PolicySection(
        title = "2. Media & Playback Cache",
        body = "ExoPlayer and Coil cache video segments and thumbnails temporarily in your device's app cache to reduce mobile data usage and prevent buffering on slow networks. You can clear app cache anytime from Android System Settings."
    )
    PolicySection(
        title = "3. Watch History & Fair Recommendations",
        body = "We store your watch history and preferred categories/languages inside your private user record so the Home feed recommends relevant videos and Shorts fairly. We do not use invasive cross-app third-party ad tracking cookies."
    )
    PolicySection(
        title = "4. Contact & Updates",
        body = "We may update these policies as JanzFlare grows. Continued use of the app after updates constitutes acceptance. Reach us anytime at mdayubjanj@gmail.com."
    )
    Spacer(Modifier.height(4.dp))
}
