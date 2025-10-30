package com.example.safeher.ui.explore

import android.content.Intent
import android.net.Uri
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

@Composable
fun ExploreScreen(
    navController: NavController,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val alertMessage by viewModel.alertMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

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
            onInstantAlertClick = { viewModel.sendInstantAlert() },
            onCall999Click = {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:999")
                }
                context.startActivity(intent)
            },
            onCheckInClick = { /* TODO */ }
        )

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
    }
}

@Composable
fun EmergencySection(
    onInstantAlertClick: () -> Unit,
    onCall999Click: () -> Unit,
    onCheckInClick: () -> Unit,
    isLoading: Boolean = false
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
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = onCall999Click) {
                    Text("Call 999 Now")
                }
                OutlinedButton(onClick = onCheckInClick) {
                    Text("Check-In Timer")
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