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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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

data class FriendTrackingInfo(
    val friend: Friend,
    val isSharingLocation: Boolean
)

@Composable
fun FriendsLocationDialog(
    friendsInfo: List<FriendTrackingInfo>,
    initiallySelectedIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var selectedFriendIds by remember { mutableStateOf(initiallySelectedIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Track Friends") },
        text = {
            Column {
                if (friendsInfo.isEmpty()) {
                    Text("You have no friends to track.")
                } else {
                    Text("Select friends to see their location on the map.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    LazyColumn {
                        items(friendsInfo, key = { it.friend.id }) { info ->
                            FriendSelectorRow(
                                friendInfo = info,
                                isSelected = selectedFriendIds.contains(info.friend.id),
                                onSelectionChanged = { friendId, isSelected ->
                                    selectedFriendIds = if (isSelected) {
                                        selectedFriendIds + friendId
                                    } else {
                                        selectedFriendIds - friendId
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedFriendIds) },
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun FriendSelectorRow(
    friendInfo: FriendTrackingInfo,
    isSelected: Boolean,
    onSelectionChanged: (String, Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // No ripple effect
                onClick = { onSelectionChanged(friendInfo.friend.id, !isSelected) }
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { checked -> onSelectionChanged(friendInfo.friend.id, checked) }
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (friendInfo.isSharingLocation) Color(0xFF4CAF50) else Color.Gray)
        )
        Spacer(Modifier.width(8.dp))
        Text(text = friendInfo.friend.displayName, modifier = Modifier.weight(1f))
    }
}