package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.IgChat
import com.example.model.IgMessage
import com.example.model.IgUser
import com.example.model.UiState
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.VistagramAvatar
import com.example.ui.components.VistagramMediaImage
import com.example.ui.components.formatChatTimestamp
import com.example.ui.components.formatIgRelativeTime
import com.example.ui.theme.IgBlue
import com.example.ui.theme.IgViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectMessagesInboxScreen(
  currentUser: IgUser,
  chatsState: UiState<List<IgChat>>,
  allUsers: List<IgUser>,
  onOpenChat: (String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  var showNewChatSheet by remember { mutableStateOf(false) }

  Scaffold(
    modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
    topBar = {
      TopAppBar(
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        title = {
          Text(
            text = currentUser.username,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
        },
        actions = {
          IconButton(onClick = { showNewChatSheet = true }) {
            Icon(Icons.Rounded.Edit, contentDescription = "New message")
          }
        },
      )
    },
  ) { innerPadding ->
    when (chatsState) {
      UiState.Loading -> Box(Modifier.fillMaxSize().padding(innerPadding)) { LoadingStateView("Loading messages…") }
      is UiState.Error -> Box(Modifier.fillMaxSize().padding(innerPadding)) { ErrorStateView(chatsState.message) }
      is UiState.Success -> {
        val chats = chatsState.data
        if (chats.isEmpty()) {
          Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
            EmptyStateView(
              icon = Icons.Rounded.ChatBubbleOutline,
              title = stringResource(R.string.no_messages_title),
              subtitle = stringResource(R.string.no_messages_subtitle),
              actionLabel = "New Message",
              onAction = { showNewChatSheet = true },
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(vertical = 8.dp),
          ) {
            items(chats, key = { it.id }) { chat ->
              val isA = chat.participantAId == currentUser.userId
              val partnerId = if (isA) chat.participantBId else chat.participantAId
              val partnerName = if (isA) chat.participantBName else chat.participantAName
              val partnerAvatar = if (isA) chat.participantBAvatar else chat.participantAAvatar

              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clickable { onOpenChat(partnerId) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                VistagramAvatar(name = partnerName, avatarUrl = partnerAvatar, size = 52.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = partnerName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                  )
                  Text(
                    text = chat.lastMessageText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Text(
                  text = formatIgRelativeTime(chat.updatedAt ?: chat.createdAt),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }

  if (showNewChatSheet) {
    val candidates = remember(allUsers, currentUser.userId) { allUsers.filter { it.userId != currentUser.userId } }
    ModalBottomSheet(onDismissRequest = { showNewChatSheet = false }) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text("New Message", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        HorizontalDivider()
        if (candidates.isEmpty()) {
          Text(
            "No other creators have joined yet. Once another account signs in, you can message them directly.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp),
          )
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(candidates, key = { it.userId }) { user ->
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                      showNewChatSheet = false
                      onOpenChat(user.userId)
                    }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                VistagramAvatar(name = user.username, avatarUrl = user.avatarUrl, size = 44.dp)
                Column {
                  Text(user.username, style = MaterialTheme.typography.titleSmall)
                  Text(user.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
  currentUser: IgUser,
  partner: IgUser,
  messagesState: UiState<List<IgMessage>>,
  onSendMessage: (String) -> Unit,
  onOpenAuthor: (String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  var draft by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  val messages = (messagesState as? UiState.Success)?.data.orEmpty()

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
    topBar = {
      TopAppBar(
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.clickable { onOpenAuthor(partner.userId) },
          ) {
            VistagramAvatar(name = partner.username, avatarUrl = partner.avatarUrl, size = 34.dp)
            Column {
              Text(partner.displayName.ifBlank { partner.username }, style = MaterialTheme.typography.titleSmall)
              Text("@${partner.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        },
      )
    },
    bottomBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = draft,
            onValueChange = { draft = it.take(1000) },
            placeholder = { Text("Message…") },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
              ),
            modifier = Modifier.weight(1f),
          )
          IconButton(
            onClick = {
              if (draft.isNotBlank()) {
                onSendMessage(draft.trim())
                draft = ""
              }
            },
            enabled = draft.isNotBlank(),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.Send,
              contentDescription = "Send",
              tint = if (draft.isNotBlank()) IgBlue else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    },
  ) { innerPadding ->
    when (messagesState) {
      UiState.Loading -> Box(Modifier.fillMaxSize().padding(innerPadding)) { LoadingStateView("Loading conversation…") }
      is UiState.Error -> Box(Modifier.fillMaxSize().padding(innerPadding)) { ErrorStateView(messagesState.message) }
      is UiState.Success -> {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize().padding(innerPadding),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (messages.isEmpty()) {
            item {
              Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                VistagramAvatar(name = partner.username, avatarUrl = partner.avatarUrl, size = 72.dp)
                Text(partner.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                  "@${partner.username} · Vistagram",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
          items(messages, key = { it.id }) { msg ->
            val isMine = msg.senderId == currentUser.userId
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            ) {
              Column(
                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
                modifier = Modifier.widthIn(max = 280.dp),
              ) {
                val bubbleShape = RoundedCornerShape(
                  topStart = 18.dp,
                  topEnd = 18.dp,
                  bottomStart = if (isMine) 18.dp else 4.dp,
                  bottomEnd = if (isMine) 4.dp else 18.dp,
                )
                Box(
                  modifier =
                    Modifier.clip(bubbleShape)
                      .then(
                        if (isMine) {
                          Modifier.background(Brush.linearGradient(listOf(IgBlue, IgViolet)))
                        } else {
                          Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        }
                      )
                      .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!msg.sharedPostImage.isNullOrBlank()) {
                      Column(
                        modifier =
                          Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.25f)),
                      ) {
                        VistagramMediaImage(
                          mediaRef = msg.sharedPostImage,
                          contentDescription = "Shared post",
                          contentScale = ContentScale.Crop,
                          modifier = Modifier.fillMaxWidth().height(140.dp),
                        )
                        if (!msg.sharedPostAuthor.isNullOrBlank()) {
                          Text(
                            text = "@${msg.sharedPostAuthor}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(8.dp),
                          )
                        }
                      }
                    }
                    Text(
                      text = msg.text,
                      style = MaterialTheme.typography.bodyMedium,
                      color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                  }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = formatChatTimestamp(msg.createdAt),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }
}
