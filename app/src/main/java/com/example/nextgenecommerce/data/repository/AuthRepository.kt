package com.example.nextgenecommerce.data.repository

import android.util.Log
import com.example.nextgenecommerce.data.models.AdminConfig
import com.example.nextgenecommerce.data.models.User
import com.example.nextgenecommerce.util.Resource
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val supabaseAuth: Auth,
    private val supabaseDb: Postgrest
) {
    // Shared user state — all ViewModels observe this single source of truth
    private val _sharedUser = MutableStateFlow<User?>(null)
    val sharedUser: StateFlow<User?> = _sharedUser.asStateFlow()

    fun updateSharedUser(user: User?) {
        _sharedUser.value = user
    }

    // Shared admin state — lives in the singleton so it survives screen navigation
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    // True once adminLogin() has confirmed admin — blocks background checks from overwriting
    private var adminVerifiedByLogin = false

    fun setAdminVerified(verified: Boolean) {
        adminVerifiedByLogin = verified
        _isAdmin.value = verified
    }

    fun isAdminVerifiedByLogin() = adminVerifiedByLogin

    companion object {
        private const val TAG = "AuthRepository"
    }

    val currentUser: UserInfo?
        get() = supabaseAuth.currentUserOrNull()

    fun isUserLoggedIn(): Boolean = supabaseAuth.currentUserOrNull() != null

    /**
     * Check if the currently logged-in user is an admin.
     * Returns true if:
     * 1. User has role = 'admin' in users table
     * 2. OR user ID matches the hardcoded admin UID
     */
    suspend fun isCurrentUserAdmin(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val currentUser = supabaseAuth.currentUserOrNull()
                ?: throw Exception("No user logged in")

            val userId = currentUser.id

            // Check if user ID matches hardcoded admin ID
            if (userId == AdminConfig.ADMIN_USER_ID) {
                Log.d(TAG, "User $userId is admin (matched hardcoded ID)")
                emit(Resource.Success(true))
                return@flow
            }

            // Check role in users table
            val userData = supabaseDb.from("users")
                .select(columns = Columns.list("id", "role")) {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<User>()

            val isAdmin = userData?.role?.equals("admin", ignoreCase = true) == true
            Log.d(TAG, "User $userId admin check: role=${userData?.role}, isAdmin=$isAdmin")
            emit(Resource.Success(isAdmin))
        } catch (e: Exception) {
            Log.e(TAG, "Error checking admin status", e)
            emit(Resource.Error(e.message ?: "Failed to check admin status"))
        }
    }

    /**
     * Login specifically for admin users.
     * After successful login, verifies the user has admin privileges.
     */
    suspend fun adminLogin(email: String, password: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            // Sign in with Supabase Auth
            supabaseAuth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val userId = supabaseAuth.currentUserOrNull()?.id
                ?: throw Exception("Login failed")

            // Fetch the user row — if it fails, try email-only fallback
            val userData = try {
                supabaseDb.from("users")
                    .select(columns = Columns.ALL) {
                        filter { eq("id", userId) }
                    }
                    .decodeSingle<User>()
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch user from DB, using email fallback. Error: ${e.message}")
                // Create a synthetic user — email-based check in isAdmin() will still work
                User(id = userId, email = email, role = AdminConfig.ADMIN_ROLE)
            }

            Log.d(TAG, "Admin login check — id=$userId email=${userData.email} role=${userData.role} isAdmin=${userData.isAdmin()}")

            if (!userData.isAdmin()) {
                supabaseAuth.signOut()
                throw Exception(
                    "Access denied. '${userData.email}' is not in the admin list.\n" +
                    "Add this email to AdminConfig.ADMIN_EMAILS in User.kt."
                )
            }

            Log.d(TAG, "Admin login successful: ${userData.email}")
            emit(Resource.Success(userData))
        } catch (e: Exception) {
            Log.e(TAG, "Admin login failed", e)
            emit(Resource.Error(e.message ?: "Admin login failed"))
        }
    }

    suspend fun register(email: String, password: String, name: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            // Register user with Supabase Auth first
            // This will fail if email already exists in auth.users
            val result = try {
                supabaseAuth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
            } catch (authError: Exception) {
                // Check if it's a "user already exists" error
                when {
                    authError.message?.contains("already registered", ignoreCase = true) == true ||
                    authError.message?.contains("already exists", ignoreCase = true) == true ||
                    authError.message?.contains("duplicate", ignoreCase = true) == true ->
                        throw Exception("This email is already registered. Please login instead.")
                    else ->
                        throw authError
                }
            }

            // Get user ID from the result (works even if email confirmation is required)
            val userId = result?.id ?: throw Exception("Failed to get user ID from registration")

            // Create user profile in users table
            val user = User(
                id = userId,
                email = email,
                name = name
            )

            // Insert user into users table
            try {
                supabaseDb.from("users")
                    .insert(user)
            } catch (insertError: Exception) {
                // If insert fails due to unique constraint, provide helpful error
                if (insertError.message?.contains("duplicate", ignoreCase = true) == true ||
                    insertError.message?.contains("unique", ignoreCase = true) == true) {
                    throw Exception("This email is already in use. Please use a different email or login.")
                }
                // For other errors, just proceed (trigger might have created the user)
            }

            emit(Resource.Success(user))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Registration failed"))
        }
    }

    suspend fun login(email: String, password: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            // Sign in with Supabase Auth
            supabaseAuth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val userId = supabaseAuth.currentUserOrNull()?.id
                ?: throw Exception("User not found after login")

            // Get user data from users table
            val userData = supabaseDb.from("users")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<User>()

            emit(Resource.Success(userData))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Login failed"))
        }
    }

    suspend fun loginWithGoogle(token: String, allowCreation: Boolean = true): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            supabaseAuth.signInWith(IDToken) {
                idToken = token
                provider = Google
            }

            val currentUser = supabaseAuth.currentUserOrNull()
                ?: throw Exception("User not found after Google login")

            val userId = currentUser.id

            // Check if user exists in users table
            val existingUsers = supabaseDb.from("users")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeList<User>()

            val user = if (existingUsers.isEmpty()) {
                if (!allowCreation) {
                    // Login-only: no profile found, sign out and reject
                    supabaseAuth.signOut()
                    throw Exception("No account found with this email. Please register first.")
                }
                // Create new user profile
                val newUser = User(
                    id = userId,
                    email = currentUser.email ?: "",
                    name = currentUser.userMetadata?.get("name")?.toString() ?: "",
                    profileImageUrl = currentUser.userMetadata?.get("avatar_url")?.toString()
                )

                supabaseDb.from("users")
                    .insert(newUser)

                newUser
            } else {
                existingUsers.first()
            }

            emit(Resource.Success(user))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Google login failed"))
        }
    }

    suspend fun getCurrentUser(): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val currentUser = supabaseAuth.currentUserOrNull()
                ?: throw Exception("No user logged in")

            val userId = currentUser.id

            // Get user data from users table
            val userData = supabaseDb.from("users")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<User>()

            emit(Resource.Success(userData))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to get current user"))
        }
    }

    suspend fun updateUserProfile(user: User): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            // Update user in users table
            supabaseDb.from("users")
                .update(user) {
                    filter {
                        eq("id", user.id)
                    }
                }

            emit(Resource.Success(user))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update profile"))
        }
    }

    suspend fun logout() {
        try {
            supabaseAuth.signOut()
        } catch (e: Exception) {
            // Handle logout error
        }
    }

    suspend fun resetPassword(email: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            supabaseAuth.resetPasswordForEmail(email)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to send reset email"))
        }
    }

    suspend fun changePassword(newPassword: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            supabaseAuth.modifyUser {
                password = newPassword
            }
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to change password"))
        }
    }
}
