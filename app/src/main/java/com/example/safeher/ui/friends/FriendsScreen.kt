package com.example.safeher.ui.friends

import android.widget.ImageButton
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.safeher.R
import com.example.safeher.data.model.Friend
import com.example.safeher.data.model.Friends
import com.example.safeher.data.model.User
import com.example.safeher.ui.theme.SafeHerTheme
import kotlin.math.roundToInt

@Composable
fun FriendsScreen(
    modifier: Modifier = Modifier,
    viewModel: FriendsViewModel = hiltViewModel()
) {
    val friends by viewModel.friendsState.collectAsState()

    FriendsScreenContent(
        friends = friends,
        onAccept = { friendId -> viewModel.acceptFriendRequest(friendId) },
        onReject = { friendId -> viewModel.rejectFriendRequest(friendId) },
        onRemove = { friendId -> viewModel.removeFriend(friendId) },
        onAddFriend = { email -> viewModel.addFriend(email) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreenContent(
    friends: Friends,
    onReject: (String) -> Unit = {},
    onAccept: (String) -> Unit = {},
    onRemove: (String) -> Unit = {},
    onDismissRequest: () -> Unit = {},
    onAddFriend: (String) -> Unit = {},
    modifier: Modifier
) {
    var isShowFriendsList by rememberSaveable { mutableStateOf(true) }
    var searchText by remember { mutableStateOf("") }
    var showAddFriendDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    if (showAddFriendDialog) {
        AddFriendDialog(
            onDismissRequest = { showAddFriendDialog = false },
            onSendRequest = { email ->
                onAddFriend(email)
                showAddFriendDialog = false
            }
        )
    }

    Scaffold (
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Search friends or requests") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = searchText.isNotEmpty(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        IconButton(onClick = { searchText = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* Handle search submit */ }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clickable(
                                onClick = { showAddFriendDialog = true },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TabSelector(
                isLeftSelected = isShowFriendsList,
                onLeftClick = { isShowFriendsList = true },
                onRightClick = { isShowFriendsList = false },
                leftLabel = "Your Friends",
                rightLabel = "Requests",
                friendsCount = friends.friends.size,
                requestsCount = friends.requestList.size
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Content
            val listItems = if (isShowFriendsList) friends.friends else friends.requestList
            val filteredItems = listItems.filter {
                it.displayName.contains(searchText, ignoreCase = true)
            }

            AnimatedContent(
                targetState = isShowFriendsList,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) +
                            slideInVertically(animationSpec = tween(300)) { it / 4 } togetherWith
                            fadeOut(animationSpec = tween(300)) +
                            slideOutVertically(animationSpec = tween(300)) { -it / 4 }
                },
                label = "list_animation"
            ) { isFriendMode ->
                val listItems = if (isFriendMode) friends.friends else friends.requestList
                val filteredItems = listItems.filter {
                    it.displayName.contains(searchText, ignoreCase = true)
                }

                if (filteredItems.isEmpty()) {
                    EmptyStateView(
                        isFriendMode = isFriendMode,
                        hasSearchQuery = searchText.isNotEmpty()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredItems,
                            key = { it.id }
                        ) { item ->
                            FriendRequestItem(
                                item = item,
                                isFriendMode = isFriendMode,
                                onAccept = { onAccept(item.documentId) },
                                onReject = { onReject(item.documentId) },
                                onRemove = { onRemove(item.documentId) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddFriendDialog(
    onDismissRequest: () -> Unit,
    onSendRequest: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Add a Friend") },
        text = {
            Column {
                Text("Enter your friend's email address to send them a request.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSendRequest(email) })
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendRequest(email) },
                enabled = email.isNotEmpty()
            ) {
                Text("Send Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TabSelector(
    isLeftSelected: Boolean,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    leftLabel: String,
    rightLabel: String,
    friendsCount: Int,
    requestsCount: Int,
    modifier: Modifier = Modifier
) {
    val indicatorOffset by animateFloatAsState(
        targetValue = if (isLeftSelected) 0f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "indicator"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(4.dp)
    ) {
        val tabWidth = this.maxWidth / 2
        val density = LocalDensity.current

        val tabWidthPx = with(density) { tabWidth.toPx() }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(tabWidth)
                .offset { IntOffset((indicatorOffset * tabWidthPx).roundToInt(), 0) }
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                )
                .shadow(4.dp, RoundedCornerShape(12.dp))
        )

        Row(modifier = Modifier.fillMaxSize()) {
            TabButton(
                label = leftLabel,
                count = friendsCount,
                isSelected = isLeftSelected,
                onClick = onLeftClick,
                modifier = Modifier.weight(1f)
            )
            TabButton(
                label = rightLabel,
                count = requestsCount,
                isSelected = !isLeftSelected,
                onClick = onRightClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TabButton(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        label = "text_color"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Badge(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    isFriendMode: Boolean,
    hasSearchQuery: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (hasSearchQuery) Icons.Default.Search else Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when {
                hasSearchQuery -> "No matches found"
                isFriendMode -> "No friends yet"
                else -> "No pending requests"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when {
                hasSearchQuery -> "Try a different search term"
                isFriendMode -> "Add some friends to get started"
                else -> "Friend requests will appear here"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FriendRequestItem(
    item: Friend,
    isFriendMode: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.height(70.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with border
            Box {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = CircleShape
                        ),
                    placeholder = painterResource(R.drawable.ic_launcher_foreground),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!isFriendMode) {
                    Text(
                        text = "Wants to be friends",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            if (isFriendMode) {
                FilledTonalIconButton(
                    onClick = onRemove,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove friend"
                    )
                }

            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(
                        onClick = onReject,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Reject"
                        )
                    }
                    FilledIconButton(
                        onClick = onAccept,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Accept"
                        )
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun FriendsScreenPreview() {
    SafeHerTheme (darkTheme = false) {
        FriendsScreenContent(
            friends = Friends(
                friends = listOf(
                    Friend(
                        id = "1",
                        imageUrl = "https://example.com/avatar1.jpg",
                        displayName = "John Doe",
                    ),
                    Friend(
                        id = "3",
                        imageUrl = "https://example.com/avatar1.jpg",
                        displayName = "John Doe",
                    ),
                    Friend(
                        id = "4",
                        imageUrl = "https://example.com/avatar1.jpg",
                        displayName = "John hh",
                    ),
                    Friend(
                        id = "5",
                        imageUrl = "https://example.com/avatar1.jpg",
                        displayName = "John hh",
                    ),
                    Friend(
                        id = "6",
                        imageUrl = "https://example.com/avatar1.jpg",
                        displayName = "John hh",
                    ),
                    Friend(
                        id = "7",
                        imageUrl = "https://example.com/avatar1.jpg",
                        displayName = "John hh",
                    ),
                    ),
                requestList = listOf(
                    Friend(
                        id = "2",
                        imageUrl = "https://example.com/avatar1.jpg",
                        displayName = "666",
                    )
                )
            ),
            modifier = Modifier
        )
    }
}