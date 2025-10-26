package com.example.safeher.ui.me

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.safeher.R
import com.example.safeher.data.model.User
import com.example.safeher.ui.theme.SafeHerTheme
import com.example.safeher.utils.getFileName

@Composable
fun MeScreen(
    viewModel: MeViewModel = hiltViewModel(),
    onAccountDeleted: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deleteState by viewModel.deleteState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(updateState) {
        if (updateState is UpdateProfileState.Success) {
            snackbarHostState.showSnackbar("Profile updated successfully!")
            viewModel.resetUpdateState()
        }
        if (updateState is UpdateProfileState.Error) {
            snackbarHostState.showSnackbar((updateState as UpdateProfileState.Error).message)
            viewModel.resetUpdateState()
        }
    }

    // This effect handles navigation after account deletion
    LaunchedEffect(deleteState) {
        if (deleteState is DeleteAccountState.Success) {
            onAccountDeleted()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.user != null -> {
                    ProfileContent(
                        user = uiState.user!!,
                        deleteState = deleteState,
                        updateState = updateState,
                        onUpdateProfile = viewModel::updateProfile,
                        onDeleteAccount = viewModel::deleteAccount,
                        onSignOutClick = onSignOutClick
                    )
                }
                uiState.error != null -> {
                    Text(
                        text = uiState.error!!,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileContent(
    user: User,
    deleteState: DeleteAccountState,
    updateState: UpdateProfileState,
    onUpdateProfile: (name: String, imageUrl: String, imageUri: Uri?) -> Unit,
    onDeleteAccount: () -> Unit,
    onSignOutClick: () -> Unit
) {

    var isInEditMode by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        AsyncImage(
            model = user.imageUrl.ifEmpty { R.drawable.ic_launcher_foreground },
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(120.dp)
                .clip(MaterialTheme.shapes.medium),
            placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
            error = painterResource(id = R.drawable.ic_launcher_foreground),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.height(16.dp))

        if (isInEditMode) {
            EditProfileView(
                user = user,
                updateState = updateState,
                selectedImageUri = selectedImageUri,
                onSelectImageClick = { imagePickerLauncher.launch("image/*") },
                onSaveChanges = { newName, newUrl ->
                    onUpdateProfile(newName, newUrl, selectedImageUri)
                },
                onCancel = {
                    isInEditMode = false
                    selectedImageUri = null
                }
            )
        } else {
            DisplayProfileView(
                user = user,
                onEditClick = { isInEditMode = true }
            )
        }

        Spacer(Modifier.weight(1f))

        if (deleteState is DeleteAccountState.Loading) {
            CircularProgressIndicator()
        } else {
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Account")
            }

            TextButton(onClick = onSignOutClick) { Text("Sign Out") }
        }

        if (deleteState is DeleteAccountState.Error) {
            Text(
                text = deleteState.message,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onDeleteAccount()
            }
        )
    }
}

@Composable
fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Account?") },
        text = { Text("This action is permanent. All your data, playlists, and activity will be removed and cannot be recovered.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditProfileView(
    user: User,
    updateState: UpdateProfileState,
    selectedImageUri: Uri?,
    onSelectImageClick: () -> Unit,
    onSaveChanges: (name: String, imageUrl: String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember(user.displayName) { mutableStateOf(user.displayName) }
    var imageUrl by remember(user.imageUrl) { mutableStateOf(user.imageUrl) }
    val context = LocalContext.current

    LaunchedEffect(updateState) {
        if (updateState is UpdateProfileState.Success) {
            onCancel()
        }
    }

    LaunchedEffect(selectedImageUri) {
        if (selectedImageUri != null) {
            imageUrl = ""
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Display Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        // URL Image
//        OutlinedTextField(
//            value = imageUrl,
//            onValueChange = { imageUrl = it },
//            label = { Text("Image URL") },
//            modifier = Modifier.fillMaxWidth(),
//            enabled = selectedImageUri == null // Disable if a local file is chosen
//        )
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.Center
//        ) {
//            Divider(modifier = Modifier.weight(1f))
//            Text("OR", modifier = Modifier.padding(horizontal = 8.dp))
//            Divider(modifier = Modifier.weight(1f))
//        }

        // Fetch Image from Device
        Button(
            onClick = onSelectImageClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select Image from Device")
        }
        selectedImageUri?.let {
            val fileName = getFileName(context, it) ?: "Selected file"
            Text(
                "File: $fileName",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(16.dp))
        Row {
            Button(
                onClick = { onSaveChanges(name, imageUrl) },
                enabled = updateState !is UpdateProfileState.Loading
            ) {
                if (updateState is UpdateProfileState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save Changes")
                }
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun DisplayProfileView(user: User, onEditClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = user.displayName, style = MaterialTheme.typography.headlineSmall)
        IconButton(onClick = onEditClick) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Profile")
        }
    }
    Text(
        text = user.email,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Preview(showBackground = true)
@Composable
fun MeScreenPreview() {
    SafeHerTheme {
        DisplayProfileView(
            user = User(id = "1", displayName = "AndroidDev", email = "dev@android.com"),
            onEditClick = {}
        )
    }
}

