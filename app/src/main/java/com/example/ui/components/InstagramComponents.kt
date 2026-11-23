package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.ImageEncodingHelper
import com.example.model.PhotoFilter
import com.example.ui.theme.IgAmber
import com.example.ui.theme.IgBlue
import com.example.ui.theme.IgIndigo
import com.example.ui.theme.IgMagenta
import com.example.ui.theme.IgStoryGradient
import com.example.ui.theme.IgViolet
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun VistagramAvatar(
  name: String,
  avatarUrl: String?,
  size: Dp = 40.dp,
  hasStory: Boolean = false,
  storyViewed: Boolean = false,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val ringPadding = if (hasStory) 3.dp else 0.dp
  val ringModifier =
    if (hasStory) {
      if (storyViewed) {
        Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
      } else {
        Modifier.border(2.5.dp, IgStoryGradient, CircleShape)
      }
    } else {
      Modifier
    }

  val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

  Box(
    modifier = modifier.size(size + ringPadding * 2).clip(CircleShape).then(ringModifier).then(clickMod).padding(ringPadding),
    contentAlignment = Alignment.Center,
  ) {
    val decodedBitmap = remember(avatarUrl) { avatarUrl?.let { ImageEncodingHelper.decodeBase64Bitmap(it) } }
    val resId = remember(avatarUrl) { avatarUrl?.let { ImageEncodingHelper.resolveDrawableRes(it) } }

    Box(
      modifier =
        Modifier.fillMaxSize()
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .border(0.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      when {
        decodedBitmap != null -> {
          Image(
            bitmap = decodedBitmap.asImageBitmap(),
            contentDescription = "$name avatar",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
        }
        resId != null -> {
          Image(
            painter = painterResource(id = resId),
            contentDescription = "$name avatar",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
        }
        !avatarUrl.isNullOrBlank() && avatarUrl.startsWith("http") -> {
          AsyncImage(
            model = avatarUrl,
            contentDescription = "$name avatar",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
        }
        else -> {
          val initials =
            name.trim().split(" ").filter { it.isNotEmpty() }.take(2).joinToString("") {
              it.first().uppercase()
            }.ifBlank { "V" }
          Text(
            text = initials,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
fun VistagramMediaImage(
  mediaRef: String,
  contentDescription: String,
  filter: PhotoFilter = PhotoFilter.NORMAL,
  contentScale: ContentScale = ContentScale.Crop,
  modifier: Modifier = Modifier,
) {
  val colorFilter = remember(filter) { filter.toColorFilter() }
  val resId = remember(mediaRef) { ImageEncodingHelper.resolveDrawableRes(mediaRef) }
  val bitmap = remember(mediaRef) { ImageEncodingHelper.decodeBase64Bitmap(mediaRef) }

  when {
    mediaRef.startsWith(ImageEncodingHelper.GRADIENT_PREFIX) -> {
      val brush =
        when (mediaRef.removePrefix(ImageEncodingHelper.GRADIENT_PREFIX)) {
          "sunset" -> Brush.linearGradient(listOf(IgAmber, IgMagenta, IgViolet))
          "ocean" -> Brush.linearGradient(listOf(Color(0xFF0EA5E9), Color(0xFF2563EB), Color(0xFF1E1B4B)))
          "neon" -> Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF06B6D4)))
          else -> Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617)))
        }
      Box(modifier = modifier.background(brush))
    }
    bitmap != null -> {
      Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        colorFilter = colorFilter,
      )
    }
    resId != null -> {
      Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        colorFilter = colorFilter,
      )
    }
    mediaRef.startsWith("http") -> {
      AsyncImage(
        model = mediaRef,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        colorFilter = colorFilter,
      )
    }
    else -> {
      Box(
        modifier = modifier.background(Brush.linearGradient(listOf(IgViolet, IgIndigo))),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "Photo",
          style = MaterialTheme.typography.labelMedium,
          color = Color.White.copy(alpha = 0.7f),
        )
      }
    }
  }
}

@Composable
fun VerifiedBadge(modifier: Modifier = Modifier, size: Dp = 15.dp) {
  Icon(
    imageVector = Icons.Rounded.Verified,
    contentDescription = "Verified account",
    tint = IgBlue,
    modifier = modifier.size(size),
  )
}

@Composable
fun DoubleTapHeartOverlay(visible: Boolean, modifier: Modifier = Modifier) {
  val scale by animateFloatAsState(
    targetValue = if (visible) 1f else 0.5f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    label = "heart_scale",
  )
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn() + scaleIn(initialScale = 0.5f),
    exit = fadeOut() + scaleOut(targetScale = 1.25f),
    modifier = modifier,
  ) {
    Icon(
      imageVector = Icons.Rounded.Favorite,
      contentDescription = null,
      tint = Color.White.copy(alpha = 0.94f),
      modifier = Modifier.size((88 * scale).dp),
    )
  }
}

@Composable
fun RichCaptionText(
  username: String,
  caption: String,
  onUsernameClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val accentColor = MaterialTheme.colorScheme.primary
  val textColor = MaterialTheme.colorScheme.onBackground
  val annotated =
    remember(username, caption, accentColor, textColor) {
      buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor)) {
          append(username)
        }
        if (caption.isNotBlank()) {
          append(" ")
          val tokens = caption.split(" ")
          tokens.forEachIndexed { index, token ->
            if (token.startsWith("#") || token.startsWith("@")) {
              withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Medium)) {
                append(token)
              }
            } else {
              withStyle(SpanStyle(color = textColor)) {
                append(token)
              }
            }
            if (index < tokens.lastIndex) append(" ")
          }
        }
      }
    }
  Text(
    text = annotated,
    style = MaterialTheme.typography.bodyMedium,
    modifier = modifier.clickable(onClick = onUsernameClick),
  )
}

@Composable
fun EmptyStateView(
  icon: ImageVector,
  title: String,
  subtitle: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.surfaceVariant,
      border = BorderStroke(1.5.dp, IgStoryGradient),
      modifier = Modifier.size(68.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.size(30.dp),
        )
      }
    }
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center,
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    if (actionLabel != null && onAction != null) {
      Spacer(modifier = Modifier.height(4.dp))
      Button(
        onClick = onAction,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
      ) {
        Text(actionLabel)
      }
    }
  }
}

@Composable
fun ErrorStateView(message: String, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.fillMaxWidth().padding(16.dp),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.ErrorOutline,
        contentDescription = "Error",
        tint = MaterialTheme.colorScheme.error,
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
      )
    }
  }
}

@Composable
fun LoadingStateView(label: String = "Loading…", modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxWidth().padding(40.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    CircularProgressIndicator(strokeWidth = 2.5.dp, color = MaterialTheme.colorScheme.secondary)
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

fun formatIgRelativeTime(timestamp: Timestamp?): String {
  val date = timestamp?.toDate() ?: return "Just now"
  val diffMillis = (System.currentTimeMillis() - date.time).coerceAtLeast(0L)
  val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
  val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
  val days = TimeUnit.MILLISECONDS.toDays(diffMillis)
  return when {
    minutes < 1 -> "Just now"
    minutes < 60 -> "${minutes}m ago"
    hours < 24 -> "${hours}h ago"
    days < 7 -> "${days}d ago"
    else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
  }
}

fun formatCompactCount(count: Long): String {
  return when {
    count >= 1_000_000L -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
    count >= 1_000L -> String.format(Locale.US, "%.1fK", count / 1_000.0)
    else -> count.toString()
  }
}

fun formatChatTimestamp(timestamp: Timestamp?): String {
  val date = timestamp?.toDate() ?: Date()
  return SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
}
