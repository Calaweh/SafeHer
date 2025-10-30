package com.example.safeher.ui.signup

import MainViewModel
import android.util.Patterns
import com.example.safeher.R
import com.example.safeher.data.model.ErrorMessage
import com.example.safeher.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.regex.Pattern
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : MainViewModel() {

    private val _shouldRestartApp = MutableStateFlow(false)
    val shouldRestartApp: StateFlow<Boolean> = _shouldRestartApp.asStateFlow()

    fun signUp(
        email: String,
        name: String,
        contactNumber: String,
        password: String,
        repeatPassword: String,
        showErrorSnackbar: (ErrorMessage) -> Unit
    ) {
        if (!email.isValidEmail()) {
            showErrorSnackbar(ErrorMessage.IdError(R.string.invalid_email))
            return
        }

        if (name.isBlank()) {
            showErrorSnackbar(ErrorMessage.IdError(R.string.invalid_full_name))
            return
        }

        if (!contactNumber.isValidContactNumber()) {
            showErrorSnackbar(ErrorMessage.IdError(R.string.invalid_phone))
            return
        }

        if (!password.isValidPassword()) {
            showErrorSnackbar(ErrorMessage.IdError(R.string.invalid_password))
            return
        }

        if (password != repeatPassword) {
            showErrorSnackbar(ErrorMessage.IdError(R.string.passwords_do_not_match))
            return
        }

        launchCatching(showErrorSnackbar) {
            authRepository.signUp(
                email = email,
                password = password,
                name = name,
                contactNumber = contactNumber
            )
            _shouldRestartApp.value = true
        }

    }
}

// --- Validators ---
private const val MIN_PASSWORD_LENGTH = 8
private const val PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{4,}$"
private val CONTACT_PATTERN = "^[0-9]{7,15}$"

fun String.isValidEmail(): Boolean {
    return this.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.isValidPassword(): Boolean {
    return this.isNotBlank() &&
            this.length >= MIN_PASSWORD_LENGTH &&
            Pattern.compile(PASSWORD_PATTERN).matcher(this).matches()
}

fun String.isValidContactNumber(): Boolean {
    return Pattern.compile(CONTACT_PATTERN).matcher(this).matches()
}
