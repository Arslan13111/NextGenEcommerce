package com.example.nextgenecommerce.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.AuthViewModel
import com.example.nextgenecommerce.presentation.viewmodel.ThemeViewModel
import com.example.nextgenecommerce.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val darkModeEnabled by themeViewModel.darkMode.collectAsState()
    val useSystemTheme by themeViewModel.useSystemTheme.collectAsState()
    val notificationsEnabled by themeViewModel.notificationsEnabled.collectAsState()
    val deleteAccountState by authViewModel.deleteAccountState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    val isDeletingAccount = deleteAccountState is Resource.Loading<*>

    LaunchedEffect(deleteAccountState) {
        when (val state = deleteAccountState) {
            is Resource.Success -> {
                authViewModel.resetDeleteAccountState()
                navController.navigate(Screen.Login.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(state.message ?: "Failed to delete account")
                authViewModel.resetDeleteAccountState()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Appearance ───────────────────────────────────────────────────
            SettingsSectionHeader("Appearance")

            SettingsToggleItem(
                icon = Icons.Default.SettingsSuggest,
                title = "Follow System",
                subtitle = "Match app appearance with your device settings",
                checked = useSystemTheme,
                onCheckedChange = { themeViewModel.toggleUseSystemTheme(it) }
            )

            SettingsToggleItem(
                icon = Icons.Default.DarkMode,
                title = "Dark Mode",
                subtitle = "Manually switch to dark theme",
                checked = darkModeEnabled,
                enabled = !useSystemTheme,
                onCheckedChange = { themeViewModel.toggleDarkMode(it) }
            )

            // ── Notifications ────────────────────────────────────────────────
            SettingsSectionHeader("Notifications")

            SettingsToggleItem(
                icon = Icons.Default.Notifications,
                title = "Push Notifications",
                subtitle = "Order updates, promotions and new arrivals",
                checked = notificationsEnabled,
                onCheckedChange = { themeViewModel.toggleNotifications(it) }
            )

            SettingsToggleItem(
                icon = Icons.Default.Email,
                title = "Email Notifications",
                subtitle = "Receive weekly newsletters and offers",
                checked = true,
                onCheckedChange = { /* future */ }
            )

            // ── Privacy ──────────────────────────────────────────────────────
            SettingsSectionHeader("Privacy & Data")

            SettingsLinkItem(
                icon = Icons.Default.Security,
                title = "Privacy Policy",
                subtitle = "How we handle your personal data",
                onClick = { navController.navigate(Screen.PrivacyPolicy.route) }
            )

            SettingsLinkItem(
                icon = Icons.Default.Description,
                title = "Terms & Conditions",
                subtitle = "App usage terms",
                onClick = { navController.navigate(Screen.TermsConditions.route) }
            )

            SettingsLinkItem(
                icon = Icons.Default.Tune,
                title = "Data Preferences",
                subtitle = "Manage analytics and tracking",
                onClick = { navController.navigate(Screen.DataPreferences.route) }
            )

            DestructiveSettingsLinkItem(
                icon = Icons.Default.DeleteForever,
                title = "Delete Account",
                subtitle = "Permanently remove your sign-in account",
                enabled = !isDeletingAccount,
                onClick = { showDeleteAccountDialog = true }
            )

            // ── About ─────────────────────────────────────────────────────────
            SettingsSectionHeader("About")

            SettingsLinkItem(
                icon = Icons.Default.Info,
                title = "About NextGen",
                subtitle = "App version and team info",
                onClick = { navController.navigate(Screen.About.route) }
            )

            SettingsLinkItem(
                icon = Icons.Default.Help,
                title = "Help Center",
                subtitle = "FAQs and support",
                onClick = { navController.navigate(Screen.HelpCenter.route) }
            )

            // Version chip
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("App Version", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("1.0.0", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeletingAccount) showDeleteAccountDialog = false
            },
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Account") },
            text = {
                Text("This removes your sign-in account, saved addresses, device notification token, and anonymizes your profile details. Order records may remain where required for payment, refund, or legal reasons.")
            },
            confirmButton = {
                TextButton(
                    enabled = !isDeletingAccount,
                    onClick = {
                        showDeleteAccountDialog = false
                        authViewModel.deleteAccount()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeletingAccount,
                    onClick = { showDeleteAccountDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            icon, 
            null, 
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, 
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                subtitle, 
                style = MaterialTheme.typography.bodySmall, 
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF111111),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun SettingsLinkItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun DestructiveSettingsLinkItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            icon,
            null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.error
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (enabled) {
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
