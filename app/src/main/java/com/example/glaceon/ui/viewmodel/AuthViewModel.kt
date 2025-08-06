package com.example.glaceon.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.glaceon.data.model.User
import com.example.glaceon.data.model.UserData
import com.example.glaceon.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authRepository = AuthRepository(application)
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    init {
        checkAuthStatus()
        restorePendingStates()
    }
    
    private fun checkAuthStatus() {
        viewModelScope.launch {
            if (authRepository.isLoggedIn()) {
                authRepository.getCurrentUser().fold(
                    onSuccess = { user ->
                        _currentUser.value = user
                        _uiState.value = _uiState.value.copy(isAuthenticated = user != null)
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(isAuthenticated = false)
                    }
                )
            }
        }
    }
    
    private fun restorePendingStates() {
        viewModelScope.launch {
            // 未完了の登録状態を復元
            val pendingConfirmation = authRepository.getPendingConfirmation()
            val pendingPasswordReset = authRepository.getPendingPasswordReset()
            
            _uiState.value = _uiState.value.copy(
                needsConfirmation = pendingConfirmation != null,
                pendingUsername = pendingConfirmation,
                needsPasswordReset = pendingPasswordReset != null,
                pendingResetUsername = pendingPasswordReset
            )
        }
    }
    
    fun signUp(
        username: String,
        password: String,
        email: String,
        givenName: String? = null,
        familyName: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            authRepository.signUp(username, password, email, givenName, familyName).fold(
                onSuccess = { message ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        needsConfirmation = true,
                        pendingUsername = username,
                        message = message
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun confirmSignUp(confirmationCode: String) {
        val username = _uiState.value.pendingUsername ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            authRepository.confirmSignUp(username, confirmationCode).fold(
                onSuccess = { message ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        confirmationCompleted = true,
                        message = message
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun resendConfirmationCode() {
        val username = _uiState.value.pendingUsername ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            authRepository.resendConfirmationCode(username).fold(
                onSuccess = { message ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = message
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun forgotPassword(username: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            authRepository.forgotPassword(username).fold(
                onSuccess = { message ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        needsPasswordReset = true,
                        pendingResetUsername = username,
                        message = message
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun resetPassword(confirmationCode: String, newPassword: String) {
        val username = _uiState.value.pendingResetUsername ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            authRepository.resetPassword(username, confirmationCode, newPassword).fold(
                onSuccess = { message ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        passwordResetCompleted = true,
                        message = message
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun signIn(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            authRepository.signIn(username, password).fold(
                onSuccess = { token ->
                    // Get current user info
                    authRepository.getCurrentUser().fold(
                        onSuccess = { user ->
                            _currentUser.value = user
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isAuthenticated = true,
                                message = "Sign in successful"
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = error.message
                            )
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut().fold(
                onSuccess = {
                    _currentUser.value = null
                    _uiState.value = AuthUiState()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
    
    fun resetAuthState() {
        _uiState.value = AuthUiState()
    }
    
    fun resetRegistrationState() {
        viewModelScope.launch {
            // Clear saved pending states
            authRepository.clearPendingConfirmation()
            authRepository.clearPendingPasswordReset()
            
            _uiState.value = _uiState.value.copy(
                needsConfirmation = false,
                confirmationCompleted = false,
                pendingUsername = null,
                needsPasswordReset = false,
                pendingResetUsername = null,
                error = null,
                message = null
            )
        }
    }
    
    fun cancelRegistration() {
        viewModelScope.launch {
            // 未完了の登録状態をクリア
            authRepository.clearPendingConfirmation()
            _uiState.value = _uiState.value.copy(
                needsConfirmation = false,
                pendingUsername = null,
                error = null,
                message = null
            )
        }
    }
    
    fun cancelPasswordReset() {
        viewModelScope.launch {
            // 未完了のパスワードリセット状態をクリア
            authRepository.clearPendingPasswordReset()
            _uiState.value = _uiState.value.copy(
                needsPasswordReset = false,
                pendingResetUsername = null,
                error = null,
                message = null
            )
        }
    }
    
    fun getAccessToken(): String? {
        return authRepository.getAccessToken()
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val needsConfirmation: Boolean = false,
    val confirmationCompleted: Boolean = false,
    val needsPasswordReset: Boolean = false,
    val passwordResetCompleted: Boolean = false,
    val pendingUsername: String? = null,
    val pendingResetUsername: String? = null,
    val error: String? = null,
    val message: String? = null
)