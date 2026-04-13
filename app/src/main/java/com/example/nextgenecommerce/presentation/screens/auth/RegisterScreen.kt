package com.example.nextgenecommerce.presentation.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.AuthViewModel
import com.example.nextgenecommerce.util.Resource

data class PasswordStrength(
    val strength: Int, // 0 = weak, 1 = medium, 2 = strong
    val hasMinLength: Boolean,
    val hasUpperCase: Boolean,
    val hasLowerCase: Boolean,
    val hasDigit: Boolean,
    val hasSpecialChar: Boolean
)

fun validatePasswordStrength(password: String): PasswordStrength {
    val hasMinLength = password.length >= 8
    val hasUpperCase = password.any { it.isUpperCase() }
    val hasLowerCase = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecialChar = password.any { !it.isLetterOrDigit() }

    val strength = when {
        hasMinLength && hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar -> 2 // Strong
        hasMinLength && ((hasUpperCase && hasLowerCase) || hasDigit || hasSpecialChar) -> 1 // Medium
        else -> 0 // Weak
    }

    return PasswordStrength(
        strength = strength,
        hasMinLength = hasMinLength,
        hasUpperCase = hasUpperCase,
        hasLowerCase = hasLowerCase,
        hasDigit = hasDigit,
        hasSpecialChar = hasSpecialChar
    )
}

@Composable
fun PasswordRequirementItem(
    text: String,
    isMet: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.Circle,
            contentDescription = null,
            tint = if (isMet) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isMet) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val passwordStrength = remember(password) { validatePasswordStrength(password) }

    // Clear error when user changes input
    LaunchedEffect(name, email, password, confirmPassword) {
        if (errorMessage != null && !isLoading) {
            errorMessage = null
        }
    }

    val authState by viewModel.authState.collectAsState()

    // Helper function to format error messages
    fun formatErrorMessage(error: String?): String {
        if (error == null) return "An error occurred"

        return when {
            error.contains("User already registered", ignoreCase = true) ->
                "This email is already registered. Please login instead."
            error.contains("duplicate key", ignoreCase = true) ||
            error.contains("unique", ignoreCase = true) ->
                "This email is already in use. Please use a different email or login."
            error.contains("invalid email", ignoreCase = true) ->
                "Please enter a valid email address."
            error.contains("password", ignoreCase = true) && error.contains("weak", ignoreCase = true) ->
                "Password is too weak. Use at least 6 characters."
            error.contains("network", ignoreCase = true) || error.contains("connection", ignoreCase = true) ->
                "Network error. Please check your internet connection."
            error.length > 100 ->
                error.substring(0, 97) + "..."
            else -> error
        }
    }

    LaunchedEffect(authState) {
        when (authState) {
            is Resource.Success -> {
                isLoading = false
                showSuccessDialog = true
                // Reset auth state so it doesn't auto-login
                viewModel.resetAuthState()
            }
            is Resource.Error -> {
                isLoading = false
                val rawError = (authState as Resource.Error).message
                errorMessage = formatErrorMessage(rawError)
            }
            is Resource.Loading -> {
                isLoading = true
                errorMessage = null
            }
            null -> {
                isLoading = false
            }
        }
    }

    // Email confirmation dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { /* Don't allow dismissing by clicking outside */ },
            icon = {
                Icon(
                    Icons.Default.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Registration Successful!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Please confirm your email from the link sent to:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        email,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Check your inbox and spam folder for the confirmation email.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    }
                ) {
                    Text("Go to Login")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sign up to get started",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, "Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, "Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            // Password Strength Indicator
            if (password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Password Strength: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when (passwordStrength.strength) {
                                0 -> "Weak"
                                1 -> "Medium"
                                else -> "Strong"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = when (passwordStrength.strength) {
                                0 -> Color(0xFFE53935) // Red
                                1 -> Color(0xFFFFA726) // Orange
                                else -> Color(0xFF4CAF50) // Green
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Strength Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(3) { index ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .background(
                                        color = if (index <= passwordStrength.strength) {
                                            when (passwordStrength.strength) {
                                                0 -> Color(0xFFE53935)
                                                1 -> Color(0xFFFFA726)
                                                else -> Color(0xFF4CAF50)
                                            }
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        shape = MaterialTheme.shapes.small
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Password Requirements
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Password must contain:",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            PasswordRequirementItem("At least 8 characters", passwordStrength.hasMinLength)
                            PasswordRequirementItem("One uppercase letter", passwordStrength.hasUpperCase)
                            PasswordRequirementItem("One lowercase letter", passwordStrength.hasLowerCase)
                            PasswordRequirementItem("One number", passwordStrength.hasDigit)
                            PasswordRequirementItem("One special character (!@#$%^&*)", passwordStrength.hasSpecialChar)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Default.Lock, "Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
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

            Button(
                onClick = {
                    when {
                        name.isBlank() || email.isBlank() || password.isBlank() -> {
                            errorMessage = "Please fill all fields"
                        }
                        !passwordStrength.hasMinLength -> {
                            errorMessage = "Password must be at least 8 characters"
                        }
                        !passwordStrength.hasUpperCase -> {
                            errorMessage = "Password must contain at least one uppercase letter"
                        }
                        !passwordStrength.hasLowerCase -> {
                            errorMessage = "Password must contain at least one lowercase letter"
                        }
                        !passwordStrength.hasDigit -> {
                            errorMessage = "Password must contain at least one number"
                        }
                        !passwordStrength.hasSpecialChar -> {
                            errorMessage = "Password must contain at least one special character"
                        }
                        passwordStrength.strength < 2 -> {
                            errorMessage = "Password is not strong enough. Please meet all requirements"
                        }
                        password != confirmPassword -> {
                            errorMessage = "Passwords do not match"
                        }
                        else -> {
                            viewModel.register(email, password, name)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Register", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Text(
                    text = "Already have an account? ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Login",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
