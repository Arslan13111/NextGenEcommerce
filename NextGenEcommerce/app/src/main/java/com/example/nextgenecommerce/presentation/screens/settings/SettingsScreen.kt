package com.example.nextgenecommerce.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.AuthViewModel
import com.example.nextgenecommerce.presentation.viewmodel.CartViewModel
import com.example.nextgenecommerce.presentation.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel()
) {
    val darkModeEnabled by themeViewModel.darkMode.collectAsState()
    val notificationsEnabled by themeViewModel.notificationsEnabled.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Appearance Section
            SettingsSection(title = "Appearance")

            SettingsItem(
                icon = Icons.Default.DarkMode,
                title = "Dark Mode",
                subtitle = "Enable dark theme",
                trailing = {
                    Switch(
                        checked = darkModeEnabled,
                        onCheckedChange = { themeViewModel.toggleDarkMode(it) }
                    )
                }
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // Notifications Section
            SettingsSection(title = "Notifications")

            SettingsItem(
                icon = Icons.Default.Notifications,
                title = "Push Notifications",
                subtitle = "Receive order updates and offers",
                trailing = {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { themeViewModel.toggleNotifications(it) }
                    )
                }
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // Account Section
            SettingsSection(title = "Account")

            SettingsItem(
                icon = Icons.Default.Person,
                title = "Edit Profile",
                subtitle = "Update your personal information",
                onClick = { navController.navigate("profile") }
            )

            SettingsItem(
                icon = Icons.Default.Lock,
                title = "Change Password",
                subtitle = "Update your password",
                onClick = { navController.navigate(Screen.ChangePassword.route) }
            )

            SettingsItem(
                icon = Icons.Default.LocationOn,
                title = "Manage Addresses",
                subtitle = "Edit shipping and billing addresses",
                onClick = { navController.navigate(Screen.Addresses.route) }
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // Support Section
            SettingsSection(title = "Support")

            SettingsItem(
                icon = Icons.Default.Help,
                title = "Help Center",
                subtitle = "Get help and support",
                onClick = { navController.navigate(Screen.HelpCenter.route) }
            )

            SettingsItem(
                icon = Icons.Default.Info,
                title = "About",
                subtitle = "App version and information",
                onClick = { navController.navigate(Screen.About.route) }
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // Logout
            SettingsItem(
                icon = Icons.Default.Logout,
                title = "Logout",
                subtitle = "Sign out of your account",
                onClick = {
                    cartViewModel.onUserLogout()
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                titleColor = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    titleColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (titleColor != androidx.compose.ui.graphics.Color.Unspecified)
                    titleColor
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = titleColor.takeIf { it != androidx.compose.ui.graphics.Color.Unspecified }
                        ?: MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Navigate",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
