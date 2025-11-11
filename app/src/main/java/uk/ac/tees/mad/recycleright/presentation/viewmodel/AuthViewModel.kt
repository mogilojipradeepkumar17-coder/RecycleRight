package uk.ac.tees.mad.recycleright.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                val errorMsg = when {
                    exception.message?.contains("network") == true ->
                        "No internet connection. Please check your network."
                    exception.message?.contains("password") == true ->
                        "Incorrect password. Please try again."
                    exception.message?.contains("user") == true ->
                        "No account found with this email."
                    else -> exception.message ?: "Login failed. Please try again."
                }
                onError(errorMsg)
            }
    }

    fun signUp(
        email: String,
        password: String,
        fullName: String,
        city: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: return@addOnSuccessListener

                // Create user profile in Firestore
                val userProfile = hashMapOf(
                    "uid" to userId,
                    "fullName" to fullName,
                    "email" to email,
                    "city" to (city ?: ""),
                    "createdAt" to System.currentTimeMillis()
                )

                firestore.collection("users")
                    .document(userId)
                    .set(userProfile)
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { exception ->
                        onError(exception.message ?: "Failed to create profile")
                    }
            }
            .addOnFailureListener { exception ->
                val errorMsg = when {
                    exception.message?.contains("network") == true ->
                        "No internet connection. Please check your network."
                    exception.message?.contains("already in use") == true ->
                        "This email is already registered."
                    exception.message?.contains("weak password") == true ->
                        "Password is too weak. Use at least 6 characters."
                    exception.message?.contains("badly formatted") == true ->
                        "Invalid email format."
                    else -> exception.message ?: "Sign up failed. Please try again."
                }
                onError(errorMsg)
            }
    }

    fun logout(onSuccess: () -> Unit) {
        firebaseAuth.signOut()
        onSuccess()
    }

    fun getCurrentUser() = firebaseAuth.currentUser
}