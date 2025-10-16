package com.example.safeher.data.model

import androidx.annotation.StringRes

// Normally display using Toasts or Snack bar when face an error
sealed class ErrorMessage {
    class StringError(val message: String) : ErrorMessage()
    class IdError(@StringRes val message: Int) : ErrorMessage()
}