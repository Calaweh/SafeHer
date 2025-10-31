package com.example.safeher.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.safeher.data.model.Friend
import com.example.safeher.data.model.LiveLocation

data class FriendTrackingInfo(
    val friend: Friend,
    val isSharingLocation: Boolean
)

@Composable
fun FriendsLocationDialog(
    friendsInfo: List<FriendTrackingInfo>,
    currentUserLocation: LiveLocation?,
    initiallySelectedId: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var selectedUserId by remember { mutableStateOf(initiallySelectedId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Navigate to Location") },
        text = {
            Column {
                Text(
                    "Select a location to navigate to on the map.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))

                LazyColumn {

                    if (currentUserLocation != null) {
                        item(key = "current_user") {
                            UserLocationRow(
                                userLocation = currentUserLocation,
                                isSelected = selectedUserId == currentUserLocation.userId,
                                onSelectionChanged = { userId ->
                                    selectedUserId = userId
                                }
                            )
                        }
                    }

                    if (friendsInfo.isEmpty()) {
                        item {
                            Text(
                                "No friends are currently sharing their location.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(friendsInfo, key = { it.friend.id }) { info ->
                            FriendSelectorRow(
                                friendInfo = info,
                                isSelected = selectedUserId == info.friend.id,
                                onSelectionChanged = { userId ->
                                    selectedUserId = userId
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedUserId) },
                enabled = selectedUserId != null
            ) {
                Text("Navigate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun UserLocationRow(
    userLocation: LiveLocation,
    isSelected: Boolean,
    onSelectionChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onSelectionChanged(userLocation.userId) }
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = { onSelectionChanged(userLocation.userId) }
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "My Location",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Currently sharing",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FriendSelectorRow(
    friendInfo: FriendTrackingInfo,
    isSelected: Boolean,
    onSelectionChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (friendInfo.isSharingLocation) {
                        onSelectionChanged(friendInfo.friend.id)
                    }
                }
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = {
                if (friendInfo.isSharingLocation) {
                    onSelectionChanged(friendInfo.friend.id)
                }
            },
            enabled = friendInfo.isSharingLocation
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (friendInfo.isSharingLocation) Color(0xFF4CAF50) else Color.Gray)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friendInfo.friend.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (friendInfo.isSharingLocation)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!friendInfo.isSharingLocation) {
                Text(
                    text = "Not sharing location",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}