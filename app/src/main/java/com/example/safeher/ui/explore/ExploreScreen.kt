package com.example.safeher.ui.explore

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.BlendMode.Companion.Screen
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safeher.ui.checkin.CheckInScreen
import com.example.safeher.ui.navigation.Screen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import com.example.safeher.ui.checkintimer.CheckInTimerOverlay
import com.example.safeher.ui.checkintimer.CheckInTimerScreen
import com.example.safeher.ui.checkintimer.CheckInTimerViewModel

@Composable
fun ExploreScreen(
    navController: NavController,
    viewModel: ExploreViewModel = hiltViewModel(),
    checkInTimerViewModel: CheckInTimerViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showCheckInTimerSetup by remember { mutableStateOf(false) }

    val alertMessage by viewModel.alertMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val countdownSeconds by viewModel.countdownSeconds.collectAsState()
    val isTimerActive by checkInTimerViewModel.timerStateManager.isTimerActive.collectAsState()

    LaunchedEffect(alertMessage) {
        alertMessage?.let { message ->
            val text = when (message) {
                is AlertMessage.Success -> message.message
                is AlertMessage.Error -> message.message
                is AlertMessage.Warning -> message.message
            }
            snackbarHostState.showSnackbar(text)
            viewModel.clearAlertMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmergencySection(
            onInstantAlertClick = { viewModel.startAlertCountdown() },
            onCall999Click = {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:999")
                }
                context.startActivity(intent)
            },
            onCheckInClick = {
                if (!isTimerActive) {
                    showCheckInTimerSetup = true
                }
            },
            isTimerActive = isTimerActive
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { navController.navigate(com.example.safeher.ui.navigation.Screen.AlertHistory.name) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.History, contentDescription = "Alert History")
            Spacer(modifier = Modifier.width(8.dp))
            Text("View Alert History")
        }

        Spacer(modifier = Modifier.height(24.dp))

        CheckInScreen(
            navigateToLiveMap = { userId ->
                navController.navigate("liveMap/$userId")
            }
        )
    }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = when (alertMessage) {
                    is AlertMessage.Success -> MaterialTheme.colorScheme.primary
                    is AlertMessage.Error -> MaterialTheme.colorScheme.error
                    is AlertMessage.Warning -> MaterialTheme.colorScheme.tertiary
                    null -> MaterialTheme.colorScheme.surface
                }
            )
        }
        if (countdownSeconds != null) {
            CountdownDialog(
                secondsRemaining = countdownSeconds!!,
                onCancel = { viewModel.cancelCountdown() }
            )
        }
        if (showCheckInTimerSetup) {
            CheckInTimerScreen(
                onDismiss = {
                    showCheckInTimerSetup = false
                }
            )
        }

        if (isTimerActive) {
            CheckInTimerOverlay(viewModel = checkInTimerViewModel)
        }
    }
}

@Composable
fun CountdownDialog(
    secondsRemaining: Int,
    onCancel: () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = (6 - secondsRemaining) / 5f,
        animationSpec = tween(durationMillis = 300),
        label = "countdown_progress"
    )

    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(80.dp),
                    strokeWidth = 6.dp,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = secondsRemaining.toString(),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        title = {
            Text(
                "Sending Emergency Alert",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Your emergency alert will be sent to all your friends in $secondsRemaining second${if (secondsRemaining != 1) "s" else ""}.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Tap Cancel to stop",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Cancel, contentDescription = "Cancel")
                Spacer(modifier = Modifier.width(4.dp))
                Text("CANCEL", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun EmergencySection(
    onInstantAlertClick: () -> Unit,
    onCall999Click: () -> Unit,
    onCheckInClick: () -> Unit,
    isLoading: Boolean = false,
    isTimerActive: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Emergency",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onInstantAlertClick,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(24.dp).height(24.dp),
                        color = MaterialTheme.colorScheme.onError
                    )
                } else {
                    Icon(Icons.Default.Warning, contentDescription = "Alert Icon", tint = MaterialTheme.colorScheme.onError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Instant Alert", color = MaterialTheme.colorScheme.onError)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCall999Click,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Call 999")
                }

                OutlinedButton(
                    onClick = onCheckInClick,
                    modifier = Modifier.weight(1f),
                    colors = if (isTimerActive) {
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    }
                ) {
                    if (isTimerActive) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Active", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("Check-In Timer")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureCard(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = "$title Icon", modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        }
    }
}