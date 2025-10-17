package com.example.resourcehub

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ResourceHubScreen(
    onBackClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val alertRed = Color(0xFFD32F2F)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }

        // Title
        Text(
            text = "Resource Hub",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // AI Chat Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "AI Assistance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF1565C0)
                )
                Text(
                    text = "Chat with AI that senses your emotions and connects you to support when needed.",
                    fontSize = 14.sp
                )
                Button(
                    onClick = { /* Navigate to chat screen */ },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Text("Start Chat", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Emergency Hotlines Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Emergency Hotlines",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = alertRed
                )
                Text(
                    "Tap any service to see contact options.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                HotlineItem(
                    title = "🚓 Police",
                    number = "000111",
                    description = "For immediate police assistance in cases of crime, assault, or danger. \nAvailable 24/7 to ensure your safety.",
                    alertRed = alertRed
                )
                HotlineItem(
                    title = "🚑 Ambulance Emergency",
                    number = "000222",
                    description = "Call for urgent medical help or to request an ambulance during health-related emergencies.",
                    alertRed = alertRed
                )
                HotlineItem(
                    title = "🧠 Mental Health Line",
                    number = "000333",
                    description = "Provides confidential emotional support and mental health counseling for those in distress or crisis.",
                    alertRed = alertRed
                )
                HotlineItem(
                    title = "👩‍🦰 Women's Aid Organisation (WAO)",
                    number = "000444",
                    description = "Offers support, counseling, and shelter for women experiencing domestic violence or abuse.",
                    alertRed = alertRed
                )
                HotlineItem(
                    title = "🤝 Talian Kasih",
                    number = "000555",
                    description = "Government helpline offering support for family, welfare, child protection, and community issues. \nAvailable 24/7.",
                    alertRed = alertRed
                )
                HotlineItem(
                    title = "💻 CyberSecurity Malaysia (Cyber999)",
                    number = "000666",
                    description = "Report online scams, cyberbullying, hacking, or other internet security issues to protect yourself online.",
                    alertRed = alertRed
                )

                HotlineItem(
                    title = "💊 AADK (National Anti-Drugs Agency)",
                    number = "000777",
                    description = "Provides assistance for drug-related problems, rehabilitation information, and counseling services.",
                    alertRed = alertRed
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .clickable { expanded.value = !expanded.value }
            .animateContentSize()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Icon(
                imageVector = if (expanded.value)
                    Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded.value) "Collapse" else "Expand",
                tint = Color.Gray
            )
        }

        if (expanded.value) {
            Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Description text
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Call row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF9F9))
                        .clickable {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call $title",
                        tint = alertRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Call them ($number)",
                        fontSize = 15.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Message row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF9F9))
                        .clickable {
                            val intent =
                                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
                            intent.putExtra("sms_body", "I need help.")
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Message $title",
                        tint = alertRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Send emergency message",
                        fontSize = 15.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
