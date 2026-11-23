package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.model.IgNotification
import com.example.model.IgUser
import com.example.model.NotificationType
import com.example.model.UiState
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.VistagramAvatar
import com.example.ui.components.VistagramMediaImage
import com.example.ui.components.formatIgRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
  notificationsState: UiState<List<IgNotification>>,
  allUsers: List<IgUser>,
  myFollowingIds: Set<String>,
  onMarkRead: (List<IgNotification>) -> Unit,
  onActorClick: (String) -> Unit,
  onToggleFollow: (IgUser, Boolean) -> Unit,
  onPostClick: (String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val list = (notificationsState as? UiState.Success)?.data.orEmpty()
  LaunchedEffect(list.size) {
    if (list.any { !it.read }) {
      onMarkRead(list)
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
          Text(
            text = "Notifications",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
        },
      )
    },
  ) { innerPadding ->
    when (notificationsState) {
      UiState.Loading -> Box(Modifier.fillMaxSize().padding(innerPadding)) { LoadingStateView("Loading activity…") }
      is UiState.Error -> Box(Modifier.fillMaxSize().padding(innerPadding)) { ErrorStateView(notificationsState.message) }
      is UiState.Success -> {
        if (list.isEmpty()) {
          Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
            EmptyStateView(
              icon = Icons.Rounded.FavoriteBorder,
              title = "Activity On Your Posts",
              subtitle = "When someone likes or comments on one of your posts or follows you, you'll see it here.",
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(vertical = 8.dp),
          ) {
            items(list, key = { it.id }) { notif ->
              val actorUser = allUsers.firstOrNull { it.userId == notif.actorId }
              val isFollowing = notif.actorId in myFollowingIds
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clickable {
                      if (!notif.postId.isNullOrBlank()) onPostClick(notif.postId)
                      else onActorClick(notif.actorId)
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                VistagramAvatar(
                  name = notif.actorUsername,
                  avatarUrl = notif.actorAvatarUrl,
                  size = 44.dp,
                  onClick = { onActorClick(notif.actorId) },
                )

                val text = buildAnnotatedString {
                  withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(notif.actorUsername) }
                  append(" ")
                  append(notif.previewText)
                  append(" ")
                  withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    append(formatIgRelativeTime(notif.createdAt))
                  }
                }
                Text(
                  text = text,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.weight(1f),
                )

                if (notif.resolvedType() == NotificationType.FOLLOW && actorUser != null) {
                  if (isFollowing) {
                    OutlinedButton(
                      onClick = { onToggleFollow(actorUser, true) },
                      shape = RoundedCornerShape(10.dp),
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                      Text("Following", style = MaterialTheme.typography.labelSmall)
                    }
                  } else {
                    Button(
                      onClick = { onToggleFollow(actorUser, false) },
                      shape = RoundedCornerShape(10.dp),
                      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    ) {
                      Text("Follow", style = MaterialTheme.typography.labelSmall)
                    }
                  }
                } else if (!notif.postThumbnail.isNullOrBlank()) {
                  VistagramMediaImage(
                    mediaRef = notif.postThumbnail,
                    contentDescription = "Post thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
