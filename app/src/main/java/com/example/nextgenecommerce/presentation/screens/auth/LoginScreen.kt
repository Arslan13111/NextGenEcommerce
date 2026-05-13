package com.example.nextgenecommerce.presentation.screens.auth

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.nextgenecommerce.R
import com.example.nextgenecommerce.data.config.SupabaseConfig
import com.example.nextgenecommerce.data.models.AdminConfig
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.AuthViewModel
import com.example.nextgenecommerce.presentation.viewmodel.CartViewModel
import com.example.nextgenecommerce.util.Resource
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private data class LoginRoleOption(
    val role: String,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel()
) {
    var selectedRole by remember { mutableStateOf(AdminConfig.CUSTOMER_ROLE) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isDetectingRole by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(SupabaseConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                viewModel.loginWithGoogle(idToken)
            } else {
                errorMessage = "Google Sign-In failed: No ID token received"
            }
        } catch (e: ApiException) {
            Log.e("LoginScreen", "Google Sign-In failed: ${e.statusCode}", e)
            errorMessage = when (e.statusCode) {
                10 -> "Developer Error (10): SHA-1 fingerprint not registered or incorrect Web Client ID."
                7 -> "Network Error (7): Please check your internet connection."
                12501 -> null
                else -> "Google Sign-In failed: ${e.localizedMessage ?: "Code ${e.statusCode}"}"
            }
        }
    }

    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val adminLoginState by viewModel.adminLoginState.collectAsStateWithLifecycle()
    val detectedRoleResource by viewModel.detectedRole.collectAsStateWithLifecycle()

    // ── Automatic Role Detection ─────────────────────────────────────────────
    LaunchedEffect(email) {
        if (email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            kotlinx.coroutines.delay(500) // Debounce for 500ms
            viewModel.detectUserRole(email)
        } else {
            viewModel.resetDetectedRole()
        }
    }

    LaunchedEffect(detectedRoleResource) {
        when (detectedRoleResource) {
            is Resource.Success -> {
                val detected = (detectedRoleResource as Resource.Success<String>).data
                if (detected != null) {
                    selectedRole = detected
                }
                isDetectingRole = false
            }
            is Resource.Error -> {
                // If not found, it might be a new customer or typo
                selectedRole = AdminConfig.CUSTOMER_ROLE
                isDetectingRole = false
            }
            is Resource.Loading -> {
                isDetectingRole = true
            }
            null -> {
                // Keep current selectedRole if null (e.g. during reset) 
                // but don't force it to customer if we already detected something
                isDetectingRole = false
            }
        }
    }

    fun roleName(role: String) = when (role) {
        AdminConfig.RETAILER_ROLE -> "Retailer"
        AdminConfig.DELIVERY_ROLE -> "Delivery Partner"
        AdminConfig.ADMIN_ROLE -> "Administrator"
        else -> "Customer"
    }

    fun roleIcon(role: String): ImageVector = when (role) {
        AdminConfig.RETAILER_ROLE -> Icons.Default.Store
        AdminConfig.DELIVERY_ROLE -> Icons.Default.LocalShipping
        AdminConfig.ADMIN_ROLE -> Icons.Default.AdminPanelSettings
        else -> Icons.Default.ShoppingBag
    }

    fun roleColor(role: String): Color = when (role) {
        AdminConfig.RETAILER_ROLE -> Color(0xFF00BFA5) // Teal
        AdminConfig.DELIVERY_ROLE -> Color(0xFFFF9100) // Orange
        AdminConfig.ADMIN_ROLE -> Color(0xFFD50000) // Red
        else -> Color(0xFF6200EE) // Purple/Primary
    }

    fun formatError(error: String?): String {
        if (error == null) return "An error occurred"
        return when {
            error.contains("Invalid login credentials", ignoreCase = true) ||
            error.contains("wrong password", ignoreCase = true) ->
                "Invalid email or password. Please try again."
            error.contains("Email not confirmed", ignoreCase = true) ->
                "Please confirm your email before logging in."
            error.contains("Too many requests", ignoreCase = true) ->
                "Too many login attempts. Please try again later."
            error.contains("User not found", ignoreCase = true) ->
                "No account found with this email."
            error.contains("network", ignoreCase = true) || error.contains("connection", ignoreCase = true) ->
                "Network error. Please check your connection."
            error.length > 120 -> error.substring(0, 117) + "..."
            else -> error
        }
    }

    // Regular login result handler
    LaunchedEffect(authState) {
        when (authState) {
            is Resource.Success -> {
                val user = (authState as Resource.Success).data
                val actualRole = when {
                    user?.isAdmin() == true -> AdminConfig.ADMIN_ROLE
                    user?.isRetailer() == true -> AdminConfig.RETAILER_ROLE
                    user?.isDeliveryPartner() == true -> AdminConfig.DELIVERY_ROLE
                    else -> AdminConfig.CUSTOMER_ROLE
                }
                
                cartViewModel.onUserLogin()
                val destination = when (actualRole) {
                    AdminConfig.ADMIN_ROLE -> Screen.AdminDashboard.route
                    AdminConfig.RETAILER_ROLE -> Screen.RetailerDashboard.route
                    AdminConfig.DELIVERY_ROLE -> Screen.DeliveryDashboard.route
                    else -> Screen.Home.route
                }
                navController.navigate(destination) { popUpTo(0) { inclusive = true } }
            }
            is Resource.Error -> {
                isLoading = false
                errorMessage = formatError((authState as Resource.Error).message)
            }
            is Resource.Loading -> {
                isLoading = true
                errorMessage = null
            }
            null -> isLoading = false
        }
    }

    // Admin login result handler
    LaunchedEffect(adminLoginState) {
        when (adminLoginState) {
            is Resource.Success -> {
                navController.navigate(Screen.AdminDashboard.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            is Resource.Error -> {
                isLoading = false
                errorMessage = formatError((adminLoginState as Resource.Error).message)
                viewModel.resetAdminLoginState()
            }
            is Resource.Loading -> {
                isLoading = true
                errorMessage = null
            }
            null -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "NextGen Logo",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Login to your account",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Credentials ───────────────────────────────────────────────────
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, "Email") },
                trailingIcon = {
                    if (isDetectingRole) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp)
            )

            // Detected Role Badge
            AnimatedVisibility(
                visible = email.isNotBlank() && !isDetectingRole,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val currentRoleColor = roleColor(selectedRole)
                Surface(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.Start),
                    color = currentRoleColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, currentRoleColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = roleIcon(selectedRole),
                            contentDescription = null,
                            tint = currentRoleColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Detected Role: ${roleName(selectedRole)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = currentRoleColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, "Password") },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            "Toggle"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Login Button ──────────────────────────────────────────────────
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please enter your email and password"
                        return@Button
                    }
                    if (selectedRole == AdminConfig.ADMIN_ROLE) {
                        viewModel.adminLogin(email, password)
                    } else {
                        viewModel.login(email, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading && !isDetectingRole,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = roleColor(selectedRole)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = roleIcon(selectedRole),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Login as ${roleName(selectedRole)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row {
                Text(
                    text = "Don't have an account? ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Register",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        navController.navigate(Screen.Register.route)
                    }
                )
            }

            // Google sign-in only for non-admin roles
            if (selectedRole != AdminConfig.ADMIN_ROLE) {
                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = "  or continue with  ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = {
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading && !isDetectingRole,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Sign in with Google", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
