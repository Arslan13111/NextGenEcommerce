package com.example.nextgenecommerce.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

private data class LegalSection(
    val title: String,
    val body: String
)

@Composable
fun PrivacyPolicyScreen(navController: NavController) {
    LegalContentScreen(
        navController = navController,
        title = "Privacy Policy",
        subtitle = "Last updated: June 7, 2026",
        sections = listOf(
            LegalSection(
                "Data We Collect",
                "We collect account details such as name, email, phone, profile photo, saved addresses, order history, payment status, device notification token, and support messages. AR try-on may process photos that you choose to upload."
            ),
            LegalSection(
                "How We Use Data",
                "We use this data to create your account, process orders, deliver products, send order notifications, provide support, protect the service from fraud, and improve app reliability."
            ),
            LegalSection(
                "Payments",
                "Payments are handled through the selected payment provider. The app stores order payment status and transaction references, but it should not store full card or wallet credentials."
            ),
            LegalSection(
                "Sharing",
                "We share only the data needed to operate the service with payment processors, delivery partners, retailers, hosting providers, support tools, and legal or security reviewers when required."
            ),
            LegalSection(
                "Your Choices",
                "You can edit your profile, delete saved addresses, manage notification preferences, and delete your account from Settings. You can also request deletion through support@nextgenecommerce.com."
            )
        )
    )
}

@Composable
fun TermsConditionsScreen(navController: NavController) {
    LegalContentScreen(
        navController = navController,
        title = "Terms & Conditions",
        subtitle = "Last updated: June 7, 2026",
        sections = listOf(
            LegalSection(
                "Account Use",
                "You are responsible for keeping your sign-in credentials secure and for activity on your account. You must provide accurate contact, delivery, and payment information."
            ),
            LegalSection(
                "Orders",
                "Orders are subject to product availability, payment confirmation, delivery coverage, and retailer acceptance. Prices and stock are confirmed by the server during checkout."
            ),
            LegalSection(
                "Returns",
                "Return requests must include the required details and images when requested. Approval depends on product condition, return window, and retailer policy."
            ),
            LegalSection(
                "Virtual Try-On",
                "AR and AI try-on results are previews only. Product fit, color, and appearance can vary by device, lighting, camera quality, and product batch."
            ),
            LegalSection(
                "Misuse",
                "Fraud, abusive behavior, unauthorized access, fake orders, payment abuse, or attempts to bypass app security may result in account restrictions or cancellation."
            )
        )
    )
}

@Composable
fun DataPreferencesScreen(navController: NavController) {
    LegalContentScreen(
        navController = navController,
        title = "Data Preferences",
        subtitle = "Manage privacy choices",
        sections = listOf(
            LegalSection(
                "Notifications",
                "Push notification preferences can be changed from Settings. You can also disable app notifications from Android system settings."
            ),
            LegalSection(
                "Saved Data",
                "Saved addresses, profile information, profile photos, wishlist items, and cart items can be updated or removed from their related app screens."
            ),
            LegalSection(
                "Account Deletion",
                "Delete Account in Settings removes your sign-in account, saved addresses, device notification token, and anonymizes your profile details. Some order and payment records may be retained for legal, security, refund, or accounting reasons."
            ),
            LegalSection(
                "Support",
                "For privacy requests that are not available in the app, contact support@nextgenecommerce.com from the email address registered to your account."
            )
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegalContentScreen(
    navController: NavController,
    title: String,
    subtitle: String,
    sections: List<LegalSection>
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            sections.forEachIndexed { index, section ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = section.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                    )
                }
                if (index != sections.lastIndex) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
