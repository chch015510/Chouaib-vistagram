package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.IgComment
import com.example.model.IgPost
import com.example.model.IgUser
import com.example.model.PostCategory
import com.example.model.UiState
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.VistagramAvatar
import com.example.ui.components.formatIgRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
  currentUser: IgUser,
  commentsState: UiState<List<IgComment>>,
  isSubmitting: Boolean,
  onDismiss: () -> Unit,
  onSubmitComment: (String, onDone: () -> Unit) -> Unit,
  onDeleteComment: (String) -> Unit,
  onAuthorClick: (String) -> Unit,
) {
  var body by rememberSaveable { mutableStateOf("") }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 20.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "Comments",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.align(Alignment.CenterHorizontally),
      )
      HorizontalDivider()

      when (commentsState) {
        UiState.Loading -> LoadingStateView("Loading comments…")
        is UiState.Error -> ErrorStateView(commentsState.message)
        is UiState.Success -> {
          val comments = commentsState.data
          if (comments.isEmpty()) {
            Text(
              text = "No comments yet. Start the conversation.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 24.dp).align(Alignment.CenterHorizontally),
            )
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxWidth().height(280.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              items(comments, key = { it.id }) { comment ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.Top,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  VistagramAvatar(
                    name = comment.authorUsername,
                    avatarUrl = comment.authorAvatarUrl,
                    size = 34.dp,
                    onClick = { onAuthorClick(comment.authorId) },
                  )
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      Text(
                        text = comment.authorUsername,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.clickable { onAuthorClick(comment.authorId) },
                      )
                      Text(
                        text = formatIgRelativeTime(comment.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    Text(
                      text = comment.body,
                      style = MaterialTheme.typography.bodyMedium,
                    )
                  }
                  if (comment.authorId == currentUser.userId) {
                    IconButton(onClick = { onDeleteComment(comment.id) }, modifier = Modifier.size(32.dp)) {
                      Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete comment",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      HorizontalDivider()

      // Quick emoji reaction bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        listOf("❤️", "🙌", "🔥", "👏", "😍", "😮", "😂").forEach { emoji ->
          Text(
            text = emoji,
            style = MaterialTheme.typography.titleLarge,
            modifier =
              Modifier.clip(RoundedCornerShape(8.dp))
                .clickable { body += emoji }
                .padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        VistagramAvatar(name = currentUser.username, avatarUrl = currentUser.avatarUrl, size = 36.dp)
        OutlinedTextField(
          value = body,
          onValueChange = { body = it.take(1000) },
          placeholder = { Text("Add a comment…") },
          singleLine = true,
          shape = RoundedCornerShape(50),
          modifier = Modifier.weight(1f).testTag("comment_input"),
        )
        IconButton(
          onClick = {
            if (body.isNotBlank()) {
              onSubmitComment(body.trim()) { body = "" }
            }
          },
          enabled = body.isNotBlank() && !isSubmitting,
          modifier = Modifier.testTag("send_comment_button"),
        ) {
          Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Post comment")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePostSheet(
  currentUser: IgUser,
  allUsers: List<IgUser>,
  post: IgPost,
  onDismiss: () -> Unit,
  onSendToUser: (IgUser, String) -> Unit,
) {
  var note by remember { mutableStateOf("") }
  val recipients = remember(allUsers, currentUser.userId) { allUsers.filter { it.userId != currentUser.userId } }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Share to Direct",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.align(Alignment.CenterHorizontally),
      )
      OutlinedTextField(
        value = note,
        onValueChange = { note = it.take(300) },
        placeholder = { Text("Write a message (optional)…") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      HorizontalDivider()
      if (recipients.isEmpty()) {
        Text(
          text = "No other accounts to share with yet.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 16.dp),
        )
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().height(260.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(recipients, key = { it.userId }) { user ->
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                VistagramAvatar(name = user.username, avatarUrl = user.avatarUrl, size = 42.dp)
                Column {
                  Text(user.username, style = MaterialTheme.typography.titleSmall)
                  Text(user.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
              Button(
                onClick = { onSendToUser(user, note) },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
              ) {
                Text("Send")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun EditPostDialog(
  post: IgPost,
  isSubmitting: Boolean,
  onDismiss: () -> Unit,
  onSave: (caption: String, location: String, category: PostCategory) -> Unit,
) {
  var caption by rememberSaveable { mutableStateOf(post.caption) }
  var location by rememberSaveable { mutableStateOf(post.location) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edit Post") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = caption,
          onValueChange = { caption = it.take(2200) },
          label = { Text("Caption") },
          minLines = 2,
          maxLines = 5,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = location,
          onValueChange = { location = it.take(100) },
          label = { Text("Location") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(caption, location, post.resolvedCategory()) },
        enabled = !isSubmitting,
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailOverlayScreen(
  post: IgPost,
  isLiked: Boolean,
  isBookmarked: Boolean,
  isOwner: Boolean,
  hasActiveStory: Boolean,
  onBack: () -> Unit,
  onAuthorClick: (String) -> Unit,
  onStoryRingClick: () -> Unit,
  onLikeToggle: () -> Unit,
  onDoubleTapLike: () -> Unit,
  onBookmarkToggle: () -> Unit,
  onOpenComments: () -> Unit,
  onShareClick: () -> Unit,
  onEditClick: () -> Unit,
  onDeleteClick: () -> Unit,
) {
  BackHandler(onBack = onBack)

  Scaffold(
    modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
    topBar = {
      TopAppBar(
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        title = { Text("Post", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
      )
    },
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())) {
      FeedPostItem(
        post = post,
        isLiked = isLiked,
        isBookmarked = isBookmarked,
        isOwner = isOwner,
        hasActiveStory = hasActiveStory,
        onAuthorClick = { onAuthorClick(post.authorId) },
        onStoryRingClick = onStoryRingClick,
        onLikeToggle = onLikeToggle,
        onDoubleTapLike = onDoubleTapLike,
        onBookmarkToggle = onBookmarkToggle,
        onCommentClick = onOpenComments,
        onShareClick = onShareClick,
        onEditClick = onEditClick,
        onDeleteClick = onDeleteClick,
      )
    }
  }
}
