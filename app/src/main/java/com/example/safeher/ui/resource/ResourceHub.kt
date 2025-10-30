package com.example.safeher.ui.resource

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.draw.shadow

// Data class for hotlines
data class Hotline(
    val id: String,
    val title: String,
    val number: String,
    val description: String,
    val category: HotlineCategory
)

enum class HotlineCategory(val displayName: String, val color: Color) {
    EMERGENCY("Emergency", Color(0xFFD32F2F)),
    MENTAL_HEALTH("Mental Health", Color(0xFFFF6F00)),
    SUPPORT("Support Services", Color(0xFF7B1FA2)),
    CYBER("Cyber Security", Color(0xFF1976D2)),
    MEDICAL("Medical", Color(0xFFC62828))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceHubScreen(
    onStartChat: () -> Unit = {},
    viewModel: ResourceHubViewModel = viewModel() // Inject the ViewModel
) {
    val scrollState = rememberScrollState()
    val alertRed = MaterialTheme.colorScheme.error

    // Collect state from the ViewModel. The UI will automatically recompose when these change.
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val filteredHotlines by viewModel.filteredHotlines.collectAsState()

    var showEmergencyBanner by remember { mutableStateOf(true) }

    val categoryPriority = listOf(
        HotlineCategory.EMERGENCY,
        HotlineCategory.MEDICAL,
        HotlineCategory.MENTAL_HEALTH,
        HotlineCategory.SUPPORT,
        HotlineCategory.CYBER
    )

    // Sort hotlines by priority (red first)
    val sortedHotlines = filteredHotlines.sortedBy { categoryPriority.indexOf(it.category) }

    val favoriteHotlines = sortedHotlines.filter { favoriteIds.contains(it.id) }
    val otherHotlines = sortedHotlines.filter { !favoriteIds.contains(it.id) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onStartChat,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                ),
                containerColor = Color.Transparent // Important for gradient background
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF4A90E2), // SafeHer Blue
                                    Color(0xFF8E6CE8), // SafeHer Purple
                                    Color(0xFF4DD0AE)  // SafeHer Green
                                )
                            ),
                            shape = RoundedCornerShape(16.dp) // <-- changed here only
                        )
                        .size(56.dp), // same FAB size
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "AI Chat",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            // Emergency Alert Banner
            if (showEmergencyBanner) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = alertRed,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "In Crisis?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = alertRed
                            )
                            Text(
                                "Help is available 24/7. Tap any service below.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        IconButton(
                            onClick = { showEmergencyBanner = false }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Close",
                                tint = alertRed
                            )
                        }
                    }
                }
            }

            // AI Chat Quick Access Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onStartChat()
                    },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF4A90E2), // SafeHer Blue
                                        Color(0xFF8E6CE8), // SafeHer Purple
                                        Color(0xFF4DD0AE)  // SafeHer Green (subtle third tone)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "AI",
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "AI Emotional Support",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Chat with AI • Emotion detection",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open Chat",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .shadow(2.dp, RoundedCornerShape(10.dp)) // <--- adds shadow
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp)
                    ),
                placeholder = {
                    Text(
                        "Search hotline title/related keywords...",
                        fontSize = 12.sp
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)},
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )


            val categories = listOf(
                "All" to null,
                HotlineCategory.EMERGENCY.displayName to HotlineCategory.EMERGENCY,
                HotlineCategory.MEDICAL.displayName to HotlineCategory.MEDICAL,
                HotlineCategory.MENTAL_HEALTH.displayName to HotlineCategory.MENTAL_HEALTH,
                HotlineCategory.SUPPORT.displayName to HotlineCategory.SUPPORT,
                HotlineCategory.CYBER.displayName to HotlineCategory.CYBER
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { (label, category) ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.onCategorySelected(category) },
                        label = { Text(label, fontSize = 12.sp) },
                        leadingIcon = if (selectedCategory == category) {
                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                        } else null,
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,      // 💙 blue background when selected
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,        // white text
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface,              // light bg when unselected
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,         // normal text color
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selectedCategory == category)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                }
            }

            // Favorites Section
            if (favoriteHotlines.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFA500))
                    Spacer(Modifier.width(6.dp))
                    Text("Favorites", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                favoriteHotlines.forEach { hotline ->
                    HotlineItem(
                        hotline = hotline,
                        isFavorite = true,
                        onFavoriteToggle = { viewModel.onFavoriteToggle(hotline.id) }
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(6.dp))
            }

            // Other Hotlines Section
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (favoriteHotlines.isEmpty() && selectedCategory == null) "All Hotlines" else "Results",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (filteredHotlines.isEmpty()) {
                Text(
                    text = "No results found.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally)
                )
            } else {
                otherHotlines.forEach { hotline ->
                    HotlineItem(
                        hotline = hotline,
                        isFavorite = false,
                        onFavoriteToggle = { viewModel.onFavoriteToggle(hotline.id) }
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}


@Composable
fun HotlineItem(
    hotline: Hotline,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() }
            ) { expanded = !expanded }
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(hotline.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(hotline.category.displayName, fontSize = 11.sp, color = hotline.category.color)
                }
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Default.StarBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (isFavorite) Color(0xFFFFA500) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand/Collapse"
                )
            }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                Text(hotline.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${hotline.number}"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = hotline.category.color),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Call, "Call", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Call ${hotline.number}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${hotline.number}"))
                        intent.putExtra("sms_body", "I need help.")
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = hotline.category.color),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, hotline.category.color.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Email, "Message", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Send Emergency SMS", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}