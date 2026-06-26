package com.example.gamelock.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamelock.data.local.AppDatabase
import com.example.gamelock.data.local.PreferencesManager
import com.example.gamelock.data.local.UserEntity
import com.example.gamelock.utils.PasswordHasher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val username: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getDatabase(app)
    private val userDao = db.userDao()
    private val prefs = PreferencesManager(app)

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    val isLoggedIn: Boolean get() = prefs.isLoggedIn
    val currentUsername: String? get() = prefs.currentUsername
    val currentUserId: Int get() = prefs.currentUserId

    fun register(username: String, email: String, password: String, confirmPassword: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _authState.value = AuthUiState.Error("Заполните все поля")
            return
        }
        if (password != confirmPassword) {
            _authState.value = AuthUiState.Error("Пароли не совпадают")
            return
        }
        if (password.length < 4) {
            _authState.value = AuthUiState.Error("Пароль должен быть минимум 4 символа")
            return
        }
        _authState.value = AuthUiState.Loading
        viewModelScope.launch {
            if (userDao.getUserByUsername(username) != null) {
                _authState.value = AuthUiState.Error("Пользователь с таким именем уже существует")
                return@launch
            }
            if (userDao.getUserByEmail(email) != null) {
                _authState.value = AuthUiState.Error("Этот email уже зарегистрирован")
                return@launch
            }
            val hash = PasswordHasher.hash(password)
            val user = UserEntity(username = username, email = email, passwordHash = hash)
            val id = userDao.insertUser(user)
            if (id > 0) {
                prefs.currentUserId = id.toInt()
                prefs.currentUsername = username
                _authState.value = AuthUiState.Success(username)
            } else {
                _authState.value = AuthUiState.Error("Ошибка регистрации")
            }
        }
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _authState.value = AuthUiState.Error("Введите имя пользователя и пароль")
            return
        }
        _authState.value = AuthUiState.Loading
        viewModelScope.launch {
            val user = userDao.getUserByUsername(username)
            if (user == null) {
                _authState.value = AuthUiState.Error("Пользователь не найден")
                return@launch
            }
            if (!PasswordHasher.verify(password, user.passwordHash)) {
                _authState.value = AuthUiState.Error("Неверный пароль")
                return@launch
            }
            prefs.currentUserId = user.id
            prefs.currentUsername = user.username
            _authState.value = AuthUiState.Success(user.username)
        }
    }

    fun logout() {
        prefs.clear()
        _authState.value = AuthUiState.Idle
    }

    fun resetState() {
        _authState.value = AuthUiState.Idle
    }
}
