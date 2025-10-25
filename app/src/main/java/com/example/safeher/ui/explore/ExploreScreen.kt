package com.example.safeher.ui.explore

import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safeher.ui.checkin.CheckInScreen
import com.example.safeher.ui.checkin.CheckInScreenContent
import com.example.safeher.ui.checkin.CheckInViewModel
import com.example.safeher.ui.map.MapViewModel
import com.example.safeher.ui.navigation.Screen

@Composable
fun ExploreScreen(
    navController: NavController
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmergencySection(navController)

        Spacer(modifier = Modifier.height(24.dp))

        CheckInScreen()
    }
}

@Composable
fun EmergencySection(navController: NavController) {
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

            // The main "Instant Alert" button
            Button(
                onClick = { /* TODO: Implement Instant Alert logic */ },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Alert Icon", tint = MaterialTheme.colorScheme.onError)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Instant Alert", color = MaterialTheme.colorScheme.onError)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row for the other two emergency options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = { /* TODO: Implement Call 999 logic */ }) {
                    Text("Call 999 Now")
                }
                OutlinedButton(onClick = { navController.navigate(Screen.CheckIn.name) }) {
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