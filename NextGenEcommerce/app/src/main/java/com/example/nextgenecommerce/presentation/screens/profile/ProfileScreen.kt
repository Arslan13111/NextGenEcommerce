package com.example.nextgenecommerce.presentation.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.AuthViewModel
import com.example.nextgenecommerce.presentation.viewmodel.CartViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    var showImageDialog by remember { mutableStateOf(false) }

    // Check admin status when screen loads
    LaunchedEffect(Unit) {
        viewModel.checkAdminStatus()
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadProfilePicture(it)
        }
    }

    // Show dialog for image options
    if (showImageDialog) {
        AlertDialog(
            onDismissRequest = { showImageDialog = false },
            title = { Text("Profile Picture") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            imagePickerLauncher.launch("image/*")
                            showImageDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Choose from Gallery")
                    }
                    if (currentUser?.profileImageUrl != null) {
                        TextButton(
                            onClick = {
                                viewModel.deleteProfilePicture()
                                showImageDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remove Photo", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Profile") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { showImageDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentUser?.profileImageUrl != null) {
                            AsyncImage(
                                model = currentUser?.profileImageUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        // Camera icon overlay
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Change photo",
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .clip(CircleShape)
                                .padding(4.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        currentUser?.name ?: "Guest User",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        currentUser?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (currentUser?.profileImageUrl == null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add a profile photo to use it for Virtual Try-On",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            ProfileMenuItem(
                icon = Icons.Default.Receipt,
                title = "My Orders",
                onClick = { navController.navigate(Screen.Orders.route) }
            )
            ProfileMenuItem(
                icon = Icons.Default.LocationOn,
                title = "Addresses",
                onClick = { navController.navigate(Screen.Addresses.route) }
            )
            ProfileMenuItem(
                icon = Icons.Default.Settings,
                title = "Settings",
                onClick = { navController.navigate(Screen.Settings.route) }
            )

            // Admin Panel - Only visible for admin users
            if (isAdmin) {
                ProfileMenuItem(
                    icon = Icons.Default.AdminPanelSettings,
                    title = "Admin Panel",
                    onClick = { navController.navigate(Screen.AdminDashboard.route) }
                )
            }

            ProfileMenuItem(
                icon = Icons.Default.Logout,
                title = "Logout",
                onClick = {
                    cartViewModel.onUserLogout()
                    viewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}
