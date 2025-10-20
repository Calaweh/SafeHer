package com.example.safeher.ui.resource

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceHubScreen(
    onStartChat: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val alertRed = Color(0xFFD32F2F)

    Scaffold(
        floatingActionButton = {
            // Floating AI Chat Button
            FloatingActionButton(
                onClick = onStartChat,
                containerColor = Color(0xFF667EEA),
                contentColor = Color.White,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "AI Chat",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Emergency Alert Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
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
                            color = Color(0xFF5D4037)
                        )
                    }
                }
            }

            // AI Chat Quick Access Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable(
                        indication = rememberRipple(),
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onStartChat()
                    },
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
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
                                        Color(0xFF667EEA),
                                        Color(0xFF764BA2)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "AI",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "AI Emotional Support",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Color(0xFF2D3748)
                        )
                        Text(
                            "Chat with AI • Emotion detection",
                            fontSize = 12.sp,
                            color = Color(0xFF718096)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open Chat",
                        tint = Color(0xFF667EEA)
                    )
                }
            }

            // Emergency Hotlines Section
            Text(
                text = "Emergency Hotlines",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = alertRed,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            HotlineItem(
                title = "Police",
                number = "000111",
                description = "For immediate police assistance in cases of crime, assault, or danger.",
                alertRed = alertRed
            )

            Spacer(modifier = Modifier.height(8.dp))

            HotlineItem(
                title = "Ambulance Emergency",
                number = "000222",
                description = "Call for urgent medical help or to request an ambulance during health-related emergencies.",
                alertRed = alertRed
            )

            Spacer(modifier = Modifier.height(8.dp))

            HotlineItem(
                title = "Mental Health Line",
                number = "000333",
                description = "Provides confidential emotional support and mental health counseling for those in distress or crisis.",
                alertRed = alertRed
            )

            Spacer(modifier = Modifier.height(8.dp))

            HotlineItem(
                title = "Women's Aid Organisation",
                number = "000444",
                description = "Offers support, counseling, and shelter for women experiencing domestic violence or abuse.",
                alertRed = alertRed
            )

            Spacer(modifier = Modifier.height(8.dp))

            HotlineItem(
                title = "Talian Kasih",
                number = "000555",
                description = "Government helpline offering support for family, welfare, child protection, and community issues.",
                alertRed = alertRed
            )

            Spacer(modifier = Modifier.height(8.dp))

            HotlineItem(
                title = "CyberSecurity Malaysia",
                number = "000666",
                description = "Report online scams, cyberbullying, hacking, or other internet security issues to protect yourself online.",
                alertRed = alertRed
            )

            Spacer(modifier = Modifier.height(8.dp))

            HotlineItem(
                title = "AADK (Anti-Drugs Agency)",
                number = "000777",
                description = "Provides assistance for drug-related problems, rehabilitation information, and counseling services.",
                alertRed = alertRed
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun HotlineItem(
    title: String,
    number: String,
    description: String,
    alertRed: Color
) {
    val context = LocalContext.current
    val expanded = remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            ) { expanded.value = !expanded.value }
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF2D3748),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                Icon(
                    imageVector = if (expanded.value)
                        Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded.value) "Collapse" else "Expand",
                    tint = Color(0xFF718096)
                )
            }

            if (expanded.value) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color(0xFF4A5568),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Call Button
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = alertRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Call $number",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // SMS Button
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
                        intent.putExtra("sms_body", "I need help.")
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = alertRed
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Message",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send Emergency SMS",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
