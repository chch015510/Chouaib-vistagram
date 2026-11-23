package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.IgStory
import com.example.ui.components.VistagramAvatar
import com.example.ui.components.VistagramMediaImage
import com.example.ui.components.formatIgRelativeTime
import kotlinx.coroutines.delay

@Composable
fun StoryViewerScreen(
  stories: List<IgStory>,
  currentUserId: String,
  onMarkViewed: (String) -> Unit,
  onSendReply: (IgStory, String) -> Unit,
  onDeleteStory: (IgStory) -> Unit,
  onAuthorClick: (String) -> Unit,
  onClose: () -> Unit,
) {
  BackHandler(onBack = onClose)

  if (stories.isEmpty()) {
    onClose()
    return
  }

  var index by remember { mutableIntStateOf(0) }
  val safeIndex = index.coerceIn(0, stories.lastIndex)
  val story = stories[safeIndex]

  var progress by remember(story.id) { mutableFloatStateOf(0f) }
  var isPaused by remember { mutableStateOf(false) }
  var replyText by remember { mutableStateOf("") }

  LaunchedEffect(story.id) {
    onMarkViewed(story.id)
  }

  LaunchedEffect(story.id, isPaused) {
    val stepMillis = 50L
    val totalMillis = 5000L
    while (progress < 1f) {
      if (!isPaused && replyText.isEmpty()) {
        progress += stepMillis.toFloat() / totalMillis.toFloat()
      }
      delay(stepMillis)
    }
    if (safeIndex < stories.lastIndex) {
      index = safeIndex + 1
    } else {
      onClose()
    }
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color.Black)
        .windowInsetsPadding(WindowInsets.safeDrawing),
  ) {
    VistagramMediaImage(
      mediaRef = story.mediaUrl,
      contentDescription = "Story by ${story.authorUsername}",
      filter = story.resolvedFilter(),
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )

    // Left / Right Tap Zones + Press-and-Hold Pause
    Row(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier =
          Modifier.weight(0.35f).fillMaxHeight().pointerInput(safeIndex) {
            detectTapGestures(
              onPress = {
                isPaused = true
                tryAwaitRelease()
                isPaused = false
              },
              onTap = {
                if (safeIndex > 0) index = safeIndex - 1 else progress = 0f
              },
            )
          }
      )
      Box(
        modifier =
          Modifier.weight(0.65f).fillMaxHeight().pointerInput(safeIndex) {
            detectTapGestures(
              onPress = {
                isPaused = true
                tryAwaitRelease()
                isPaused = false
              },
              onTap = {
                if (safeIndex < stories.lastIndex) index = safeIndex + 1 else onClose()
              },
            )
          }
      )
    }

    // Top Gradient + Progress Bars + Author Header
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .align(Alignment.TopCenter)
          .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)))
          .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        stories.forEachIndexed { idx, _ ->
          val barProgress =
            when {
              idx < safeIndex -> 1f
              idx == safeIndex -> progress
              else -> 0f
            }
          LinearProgressIndicator(
            progress = { barProgress },
            modifier = Modifier.weight(1f).height(2.5.dp).clip(RoundedCornerShape(2.dp)),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.3f),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.clickable { onAuthorClick(story.authorId) },
        ) {
          VistagramAvatar(name = story.authorUsername, avatarUrl = story.authorAvatarUrl, size = 34.dp)
          Text(
            text = story.authorUsername,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = formatIgRelativeTime(story.createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f),
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (story.authorId == currentUserId) {
            IconButton(onClick = { onDeleteStory(story) }) {
              Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete story", tint = Color.White)
            }
          }
          IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = "Close story", tint = Color.White)
          }
        }
      }
    }

    // Center Story Overlay Text
    if (story.overlayText.isNotBlank()) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.6f),
        modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
      ) {
        Text(
          text = story.overlayText,
          style = MaterialTheme.typography.titleLarge,
          color = Color.White,
          modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
        )
      }
    }

    // Bottom Quick Reactions & Reply Bar (for other users' stories)
    if (story.authorId != currentUserId) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .align(Alignment.BottomCenter)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
          listOf("❤️", "🔥", "😂", "😮", "👏").forEach { emoji ->
            Text(
              text = emoji,
              style = MaterialTheme.typography.headlineMedium,
              modifier =
                Modifier.clip(RoundedCornerShape(12.dp))
                  .clickable { onSendReply(story, emoji) }
                  .padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = replyText,
            onValueChange = { replyText = it.take(300) },
            placeholder = { Text("Send message…", color = Color.White.copy(alpha = 0.75f)) },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.7f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.45f),
                focusedContainerColor = Color.Black.copy(alpha = 0.35f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.35f),
              ),
            modifier = Modifier.weight(1f),
          )
          IconButton(
            onClick = {
              if (replyText.isNotBlank()) {
                onSendReply(story, replyText.trim())
                replyText = ""
              }
            },
          ) {
            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send reply", tint = Color.White)
          }
        }
      }
    }
  }
}
