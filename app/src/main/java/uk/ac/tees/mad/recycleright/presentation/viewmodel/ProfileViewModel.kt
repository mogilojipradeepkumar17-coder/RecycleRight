package uk.ac.tees.mad.recycleright.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


// user profile with all the info
data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val city: String = "",
    val notificationEnabled: Boolean = true,
)


// ui state
sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val profile: UserProfile) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}


@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {


    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState = _uiState.asStateFlow()


    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val currentUser = firebaseAuth.currentUser
            // guard clause
            if (currentUser == null) {
                return@launch
            }
            firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val profile = UserProfile(
                            uid = document.getString("uid") ?: "",
                            fullName = document.getString("fullName") ?: "",
                            email = document.getString("email") ?: currentUser.email ?: "",
                            city = document.getString("city") ?: "",
                            notificationEnabled = document.getBoolean("notificationsEnabled")
                                ?: true
                        )
                        _uiState.value = ProfileUiState.Success(profile)
                    } else {
                        _uiState.value = ProfileUiState.Error("Profile Error")
                    }
                }
                .addOnFailureListener { exception ->
                    _uiState.value = ProfileUiState.Error(
                        exception.message ?: "Failed to load profile"
                    )
                }
        }
    }

    fun updateProfile(
        fullName:String,
        city:String,
        onSuccess:()->Unit,
        onError:(String)->Unit
    ){
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            onError("No user logged in")
            return
        }

        val updates = hashMapOf<String, Any>(
            "fullName" to fullName,
            "city" to city
        )

        firestore.collection("users")
            .document(currentUser.uid)
            .update(updates)
            .addOnSuccessListener {

                val currentUiState=_uiState.value
                if(currentUiState is ProfileUiState.Success){
                    _uiState.value= ProfileUiState.Success(
                        profile = currentUiState.profile.copy(
                            fullName = fullName,
                            city =city
                        )
                    )
                }
//                loadProfile()
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Failed to update profile")
            }

    }

    fun toggleNotifications(enabled: Boolean) {
        val currentUser = firebaseAuth.currentUser ?: return

        firestore.collection("users")
            .document(currentUser.uid)
            .update("notificationsEnabled", enabled)
            .addOnSuccessListener {
//                loadProfile()
                val currentUiState=_uiState.value
                if(currentUiState is ProfileUiState.Success){
                    _uiState.value= ProfileUiState.Success(
                        profile = currentUiState.profile.copy(
                            notificationEnabled = enabled
                        )
                    )
                }
            }
    }

    fun logout(onSuccess: () -> Unit) {
        firebaseAuth.signOut()
        onSuccess()
    }
}