package com.example.nextgenecommerce.data.repository

import com.example.nextgenecommerce.data.models.User
import com.example.nextgenecommerce.util.Resource
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val supabaseAuth: Auth,
    private val supabaseDb: Postgrest
) {

    val currentUser: UserInfo?
        get() = supabaseAuth.currentUserOrNull()

    fun isUserLoggedIn(): Boolean = supabaseAuth.currentUserOrNull() != null

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

    suspend fun loginWithGoogle(idToken: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            // Sign in with Google using Supabase
            // Note: For Google OAuth, you typically use the OAuth flow via browser
            // For now, we'll implement basic structure
            // You may need to configure this based on your Google OAuth setup
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
