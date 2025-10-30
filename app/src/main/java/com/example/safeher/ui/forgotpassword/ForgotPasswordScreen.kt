package com.example.safeher.ui.forgotpassword

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.safeher.R
import com.example.safeher.ui.theme.StandardButton

@Composable
fun ForgotPasswordScreen(
    openSignInScreen: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val state by viewModel.forgotPasswordState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetForgotPasswordState()
        }
    }

    ForgotPasswordScreenContent(
        state = state,
        onSendResetLinkClick = { email ->
            viewModel.sendPasswordResetEmail(email)
        },
        openSignInScreen = openSignInScreen
    )
}

@Composable
private fun ForgotPasswordScreenContent(
    state: ForgotPasswordState,
    onSendResetLinkClick: (String) -> Unit,
    openSignInScreen: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Header Section ---
            Spacer(Modifier.height(48.dp))
            Image(
                modifier = Modifier.size(88.dp),
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "App logo"
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.forgot_password_text),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = stringResource(R.string.forgot_password_subtitle),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(48.dp))

            if (state is ForgotPasswordState.Success) {
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = stringResource(R.string.forgot_password_success_message),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.email)) },
                    singleLine = true,
                    isError = state is ForgotPasswordState.Error,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            onSendResetLinkClick(email)
                        }
                    )
                )

                Spacer(Modifier.height(32.dp))

                StandardButton(
                    label = R.string.send_reset_link,
                    isLoading = state is ForgotPasswordState.Loading,
                    onButtonClick = {
                        keyboardController?.hide()
                        onSendResetLinkClick(email)
                    }
                )

                if (state is ForgotPasswordState.Error) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            TextButton(onClick = openSignInScreen) {
                Text(
                    text = stringResource(R.string.back_to_sign_in),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}