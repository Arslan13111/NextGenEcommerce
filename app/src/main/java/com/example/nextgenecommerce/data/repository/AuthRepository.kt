package com.example.nextgenecommerce.data.repository

import com.example.nextgenecommerce.data.models.User
import com.example.nextgenecommerce.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    val currentUser get() = firebaseAuth.currentUser

    fun isUserLoggedIn(): Boolean = firebaseAuth.currentUser != null

    suspend fun register(email: String, password: String, name: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                val user = User(
                    id = firebaseUser.uid,
                    email = email,
                    name = name
                )

                // Save user to Firestore
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(user)
                    .await()

                emit(Resource.Success(user))
            } else {
                emit(Resource.Error("Registration failed"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An error occurred"))
        }
    }

    suspend fun login(email: String, password: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // Get user data from Firestore
                val userDoc = firestore.collection("users")
                    .document(firebaseUser.uid)
                    .get()
                    .await()

                val user = userDoc.toObject(User::class.java)
                if (user != null) {
                    emit(Resource.Success(user))
                } else {
                    emit(Resource.Error("User data not found"))
                }
            } else {
                emit(Resource.Error("Login failed"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An error occurred"))
        }
    }

    suspend fun loginWithGoogle(idToken: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // Check if user exists in Firestore
                val userDoc = firestore.collection("users")
                    .document(firebaseUser.uid)
                    .get()
                    .await()

                val user = if (userDoc.exists()) {
                    userDoc.toObject(User::class.java)!!
                } else {
                    // Create new user
                    val newUser = User(
                        id = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        name = firebaseUser.displayName ?: "",
                        profileImageUrl = firebaseUser.photoUrl?.toString()
                    )
                    firestore.collection("users")
                        .document(firebaseUser.uid)
                        .set(newUser)
                        .await()
                    newUser
                }

                emit(Resource.Success(user))
            } else {
                emit(Resource.Error("Google login failed"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An error occurred"))
        }
    }

    suspend fun getCurrentUser(): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                val userDoc = firestore.collection("users")
                    .document(firebaseUser.uid)
                    .get()
                    .await()

                val user = userDoc.toObject(User::class.java)
                if (user != null) {
                    emit(Resource.Success(user))
                } else {
                    emit(Resource.Error("User not found"))
                }
            } else {
                emit(Resource.Error("No user logged in"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An error occurred"))
        }
    }

    suspend fun updateUserProfile(user: User): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            firestore.collection("users")
                .document(user.id)
                .set(user)
                .await()
            emit(Resource.Success(user))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An error occurred"))
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }
}
