package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Email
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

const val JANZ_BRAND_OPERATOR = "Janz Media"
const val JANZ_SUPPORT_EMAIL = "contact.janzmedia@gmail.com"

fun openSupportEmail(context: Context, subject: String = "JanzFlare Support & Policy Inquiry") {
    runCatching {
        val mailUri = Uri.parse("mailto:$JANZ_SUPPORT_EMAIL?subject=${Uri.encode(subject)}")
        val intent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

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
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 660.dp)
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
                            text = "JanzFlare Legal & Policies",
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

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openSupportEmail(context) }
                            .testTag("legal_contact_support_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email support",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Official Support & Grievance Contact",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "$JANZ_BRAND_OPERATOR • $JANZ_SUPPORT_EMAIL",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
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
        text = "Last Updated: November 2026 • JanzFlare by $JANZ_BRAND_OPERATOR",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary
    )
    PolicySection(
        title = "1. Who We Are",
        body = "JanzFlare is a video and Shorts publishing platform operated by $JANZ_BRAND_OPERATOR. This Privacy Policy explains clearly what information we collect, why we collect it, how we protect it, and the choices you control."
    )
    PolicySection(
        title = "2. Information We Collect & Purpose",
        body = "• Account Details: When you sign in with Google, we receive your name, email address (kept private and never displayed publicly), and profile photo to create and secure your JanzFlare account.\n" +
            "• Creator & Community Content: Videos, Shorts, thumbnails, titles, descriptions, comments, likes, playlists, channel links, and reports you choose to submit.\n" +
            "• Viewing & Discovery Preferences: Watch history, subscriptions, preferred categories, and languages used to personalize your feed fairly.\n" +
            "• App Reliability & Safety Signals: Basic device diagnostics, crash data, and anti-spam metrics to keep playback smooth and prevent fake engagement."
    )
    PolicySection(
        title = "3. Advertising & Device Identifiers (AdMob)",
        body = "JanzFlare displays clearly labeled inline, banner, and in-player advertisements served through Google AdMob to support platform hosting and creator monetization. Advertising partners may use your device's Advertising ID, IP-based approximate region, and ad interaction signals to show relevant or non-personalized ads and prevent ad fraud. You can reset or limit personalized ads anytime in Android Settings -> Privacy -> Ads. We never sell your personal information."
    )
    PolicySection(
        title = "4. Permissions & Privacy-First Access",
        body = "We request only permissions strictly needed for features you use:\n" +
            "• Gallery & Media Selection: Uses Android's privacy-safe Photo Picker so JanzFlare accesses only the specific video or image you pick—never your full gallery.\n" +
            "• Notifications (Optional): Used only for upload progress and creator activity alerts, which you can turn off anytime in Settings.\n" +
            "JanzFlare does not collect your contacts, SMS messages, call logs, or background GPS location."
    )
    PolicySection(
        title = "5. Children's Privacy & Zero Tolerance for CSAE",
        body = "JanzFlare is intended for users aged 13 and older (or the minimum legal age in your country). We do not knowingly collect personal information from children under 13. JanzFlare maintains strict zero tolerance for Child Sexual Abuse and Exploitation (CSAE) or any child endangerment content. Any reported violation is removed immediately, the account is permanently banned, and authorities are notified where required by law."
    )
    PolicySection(
        title = "6. Data Security, Retention & Permanent Deletion",
        body = "All account records and uploaded media are transmitted over encrypted HTTPS/TLS connections and protected by strict access controls. You can permanently delete your account, channel profile, uploaded videos, Shorts, and personal data anytime directly inside the app via Settings & Privacy -> Delete Account Data, or by emailing $JANZ_SUPPORT_EMAIL. Verified deletion requests are completed promptly."
    )
    PolicySection(
        title = "7. Your Rights & Grievance Contact",
        body = "Depending on your region (including India's DPDP/IT Rules, GDPR, and CCPA), you have the right to access, correct, or delete your personal data and opt out of personalized ads. For privacy questions, data deletion help, or safety concerns, contact $JANZ_BRAND_OPERATOR at $JANZ_SUPPORT_EMAIL."
    )
}

@Composable
private fun TermsOfServiceContent() {
    Text(
        text = "Terms of Service & Community Guidelines • JanzFlare ($JANZ_BRAND_OPERATOR)",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary
    )
    PolicySection(
        title = "1. Acceptance & Eligibility",
        body = "By accessing or using JanzFlare, you agree to these Terms of Service and our Privacy Policy. You must be at least 13 years old (and have parental consent if under 18) to create an account or publish content."
    )
    PolicySection(
        title = "2. Your Content & Creator Ownership",
        body = "You keep 100% ownership of the original videos, Shorts, thumbnails, and comments you publish on JanzFlare. By uploading content, you grant $JANZ_BRAND_OPERATOR a worldwide, non-exclusive, royalty-free license to host, optimize, stream, display, and share your public content within JanzFlare and via user share links. Removing your video or deleting your account ends this license for future distribution."
    )
    PolicySection(
        title = "3. Community Safety & User-Generated Content Rules",
        body = "To keep JanzFlare safe for everyone, the following are strictly prohibited:\n" +
            "• Child exploitation, abuse, or harm to minors (zero tolerance)\n" +
            "• Hate speech, harassment, bullying, threats, or impersonation\n" +
            "• Sexually explicit content, graphic violence, gore, or dangerous stunts\n" +
            "• Scams, malware, misleading spam, or artificial traffic (bots, click-farms, fake views)\n" +
            "Every user can report videos, Shorts, or comments and block abusive accounts directly in the app. $JANZ_BRAND_OPERATOR reviews reports and may remove content or suspend/ban accounts that violate these rules."
    )
    PolicySection(
        title = "4. Copyright & Takedown Policy (DMCA / Indian Copyright Act)",
        body = "Upload only content you created or have legal permission to use. If you believe your copyrighted work has been posted on JanzFlare without authorization, email $JANZ_SUPPORT_EMAIL with your name, proof of ownership, and the video title or creator handle. Valid notices result in prompt removal, and repeat infringers lose uploading privileges or their accounts."
    )
    PolicySection(
        title = "5. Views, Engagement & Creator Monetization",
        body = "JanzFlare tracks Public Views separately from Verified Engaged Views (authentic, sustained viewing sessions eligible for monetization). To apply for monetization, creators must maintain an authentic channel in good standing with at least 500 followers plus either 1,000 public watch hours or 100,000 Engaged Shorts Views, and pass manual compliance review. Fraudulent views, invalid ad clicks, or policy strikes disqualify payouts."
    )
    PolicySection(
        title = "6. Advertisements & External Links",
        body = "JanzFlare shows clearly labeled ads to keep the service running and support creators. Encouraging or forcing users to click ads is strictly forbidden. External links added by creators in channel descriptions or official update notices open in your browser and are governed by their respective owners."
    )
    PolicySection(
        title = "7. Service Availability, Liability & Governing Law",
        body = "JanzFlare is provided on an 'as-is' basis. While we work hard to keep playback and uploads reliable, $JANZ_BRAND_OPERATOR is not liable for indirect losses, third-party outages, or user-submitted content. These Terms are governed by the laws of India. Contact $JANZ_SUPPORT_EMAIL for legal or support inquiries."
    )
}

@Composable
private fun CookieAndDataPolicyContent() {
    Text(
        text = "Cookies, Local Storage & Ad Data Policy • JanzFlare ($JANZ_BRAND_OPERATOR)",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary
    )
    PolicySection(
        title = "1. Essential Sign-In & Session Storage",
        body = "JanzFlare stores encrypted authentication session tokens locally on your device so you stay securely signed in without re-entering credentials every time you open the app. Signing out clears your active session."
    )
    PolicySection(
        title = "2. Video & Image Playback Cache",
        body = "To save your mobile data and prevent buffering on slower connections, JanzFlare temporarily caches thumbnails and short playback segments on your device. You can clear this temporary cache anytime from Android Settings -> Apps -> JanzFlare -> Storage -> Clear Cache."
    )
    PolicySection(
        title = "3. Personalization & App Preferences",
        body = "Your chosen theme (Dark, Light, or System), notification toggles, language/interest selections, and watch history are stored securely so JanzFlare remembers your preferences and shows relevant videos and Shorts."
    )
    PolicySection(
        title = "4. Advertising SDKs & Your Controls",
        body = "When advertisements are active, Google AdMob may use device-level identifiers and local storage signals to cap ad frequency, measure performance, prevent invalid traffic, and serve relevant ads. You can manage ad personalization or reset your Advertising ID anytime in Android Settings -> Privacy -> Ads."
    )
    PolicySection(
        title = "5. Policy Updates & Questions",
        body = "We may revise these policies as JanzFlare adds new features or as legal requirements evolve. Significant updates will be reflected in the 'Last Updated' date. Reach $JANZ_BRAND_OPERATOR anytime at $JANZ_SUPPORT_EMAIL."
    )
}
