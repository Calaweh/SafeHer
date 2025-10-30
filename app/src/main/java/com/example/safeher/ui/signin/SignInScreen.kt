package com.example.safeher.ui.signin

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.safeher.R
import com.example.safeher.data.model.ErrorMessage
import com.example.safeher.ui.theme.SafeHerTheme
import com.example.safeher.ui.theme.StandardButton

@Composable
fun SignInScreen(
    openHomeScreen: () -> Unit,
    openSignUpScreen: () -> Unit,
    openForgotPasswordScreen: () -> Unit,
    showError: (ErrorMessage) -> Unit,
    viewModel: SignInViewModel = hiltViewModel()
) {
    val shouldRestartApp by viewModel.shouldRestartApp.collectAsStateWithLifecycle()

    if (shouldRestartApp) {
        // Use a LaunchedEffect to ensure navigation happens in the correct lifecycle state
        LaunchedEffect(Unit) {
            openHomeScreen()
        }
    }

    if (!shouldRestartApp) {
        SignInScreenContent(
            openSignUpScreen = openSignUpScreen,
            openForgotPasswordScreen = openForgotPasswordScreen,
            signIn = viewModel::signIn,
            showErrorSnackbar = showError
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreenContent(
    openSignUpScreen: () -> Unit,
    openForgotPasswordScreen: () -> Unit,
    signIn: (String, String, (ErrorMessage) -> Unit) -> Unit,
    showErrorSnackbar: (ErrorMessage) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val passwordFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()), // Makes the whole screen scrollable
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Logo Section ---
            Spacer(Modifier.height(48.dp))
            Image(
                modifier = Modifier.size(88.dp),
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "App logo"
            )
            Spacer(Modifier.height(48.dp))

            // --- Form Section ---
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() })
            )
            Spacer(Modifier.size(16.dp))
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .focusRequester(passwordFocusRequester),
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    signIn(email, password, showErrorSnackbar)
                })
            )
            Spacer(Modifier.size(24.dp))
            StandardButton(
                label = R.string.sign_in_with_email,
                onButtonClick = {
                    keyboardController?.hide()
                    signIn(email, password, showErrorSnackbar)
                }
            )

            Spacer(Modifier.weight(1f))

            // --- Bottom Links Section ---
            TextButton(onClick = openSignUpScreen) {
                Text(
                    text = stringResource(R.string.sign_up_text),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            }
            TextButton(onClick = openForgotPasswordScreen) {
                Text(
                    text = stringResource(R.string.forgot_password_text),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun SignInScreenPreview() {
    SafeHerTheme (darkTheme = true) {
        SignInScreenContent(
            openSignUpScreen = {},
            openForgotPasswordScreen = {},
            signIn = { _, _, _ -> },
            showErrorSnackbar = {}
        )
    }
}