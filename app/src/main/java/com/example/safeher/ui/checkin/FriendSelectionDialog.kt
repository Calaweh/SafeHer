package com.example.safeher.ui.checkin

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.safeher.data.model.Friend

@Composable
fun FriendSelectionDialog(
    friends: List<Friend>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var selectedFriendIds by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share With...") },
        text = {
            Column {
                if (friends.isEmpty()) {
                    Text("You have no friends to share your location with.")
                } else {

                    Log.d("FriendSelectionDialog", "friends: $friends")

                    friends.forEach { friend ->

                        Log.d("FriendSelectionDialog", "friend name: ${friend.id}")
                        Log.d("FriendSelectionDialog", "friend name: ${friend.displayName}")

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = {
                                        selectedFriendIds = if (selectedFriendIds.contains(friend.id)) {
                                            selectedFriendIds - friend.id
                                        } else {
                                            selectedFriendIds + friend.id
                                        }
                                    }
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedFriendIds.contains(friend.id),
                                onCheckedChange = { isChecked ->
                                    selectedFriendIds = if (isChecked) {
                                        selectedFriendIds + friend.id
                                    } else {
                                        selectedFriendIds - friend.id
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(friend.displayName)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedFriendIds.toList()) },
                enabled = friends.isNotEmpty() && selectedFriendIds.isNotEmpty()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}