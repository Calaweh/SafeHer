package com.example.safeher.ui.checkintimer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CheckInTimerSettingsCard(
    viewModel: CheckInTimerViewModel = hiltViewModel()
) {
    val hasPinSet by viewModel.hasPinSet.collectAsState()
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDeletePinDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Check-In Timer PIN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (hasPinSet) "PIN configured" else "No PIN set",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (hasPinSet) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (hasPinSet) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }

            Divider()

            if (hasPinSet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showChangePinDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Change PIN")
                    }
                    OutlinedButton(
                        onClick = { showDeletePinDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove")
                    }
                }
            } else {
                Button(
                    onClick = { showChangePinDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set Up PIN")
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "This PIN is required to mark yourself safe when using the Check-In Timer feature.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            onDismiss = { showChangePinDialog = false },
            onPinChanged = { newPin ->
                viewModel.savePin(newPin)
                showChangePinDialog = false
            },
            hasPinSet = hasPinSet,
            viewModel = viewModel
        )
    }

    if (showDeletePinDialog) {
        DeletePinConfirmDialog(
            onDismiss = { showDeletePinDialog = false },
            onConfirm = {
                // Remove PIN from SharedPreferences
                viewModel.savePin("")
                showDeletePinDialog = false
            }
        )
    }
}

@Composable
fun ChangePinDialog(
    onDismiss: () -> Unit,
    onPinChanged: (String) -> Unit,
    hasPinSet: Boolean,
    viewModel: CheckInTimerViewModel
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (hasPinSet) Icons.Default.Edit else Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                if (hasPinSet) "Change PIN" else "Set Up PIN",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (hasPinSet) {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                currentPin = it
                                showError = false
                            }
                        },
                        label = { Text("Current PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = showError && currentPin.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            newPin = it
                            showError = false
                        }
                    },
                    label = { Text("New PIN (4-6 digits)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            confirmPin = it
                            showError = false
                        }
                    },
                    label = { Text("Confirm New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        hasPinSet && currentPin.isEmpty() -> {
                            showError = true
                            errorMessage = "Enter current PIN"
                        }
                        newPin.length < 4 -> {
                            showError = true
                            errorMessage = "PIN must be at least 4 digits"
                        }
                        newPin != confirmPin -> {
                            showError = true
                            errorMessage = "PINs don't match"
                        }
                        else -> {
                            if (hasPinSet) {
                            }
                            onPinChanged(newPin)
                        }
                    }
                },
                enabled = (!hasPinSet || currentPin.length >= 4) && 
                         newPin.length >= 4 && 
                         confirmPin.length >= 4
            ) {
                Text("Save")
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
fun DeletePinConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                "Remove PIN?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "You won't be able to use the Check-In Timer feature until you set up a new PIN.\n\nAre you sure you want to remove your PIN?",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
