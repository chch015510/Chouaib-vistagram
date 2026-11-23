package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.IgReel
import com.example.model.IgUser
import com.example.model.UiState
import com.example.ui.components.DoubleTapHeartOverlay
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.VistagramAvatar
import com.example.ui.components.VistagramMediaImage
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.IgHeartRed
import kotlinx.coroutines.delay

@Composable
fun ReelsScreen(
  currentUser: IgUser,
  reelsState: UiState<List<IgReel>>,
  likedReelIds: Set<String>,
  myFollowingIds: Set<String>,
  allUsers: List<IgUser>,
  onLikeReel: (IgReel, Boolean) -> Unit,
  onOpenComments: (IgReel) -> Unit,
  onToggleFollow: (IgUser, Boolean) -> Unit,
  onAuthorClick: (String) -> Unit,
  onDeleteReel: (IgReel) -> Unit,
  onCreateReelClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  when (reelsState) {
    UiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingStateView("Loading reels…") }
    is UiState.Error -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ErrorStateView(reelsState.message) }
    is UiState.Success -> {
      val reels = reelsState.data
      if (reels.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          EmptyStateView(
            icon = Icons.Rounded.Movie,
            title = stringResource(R.string.no_reels_title),
            subtitle = stringResource(R.string.no_reels_subtitle),
            actionLabel = "Create Reel",
            onAction = onCreateReelClick,
          )
        }
      } else {
        val pagerState = rememberPagerState(pageCount = { reels.size })
        VerticalPager(
          state = pagerState,
          modifier = modifier.fillMaxSize().background(Color.Black),
        ) { page ->
          val reel = reels[page]
          val authorUser = allUsers.firstOrNull { it.userId == reel.authorId }
          ReelPageItem(
            reel = reel,
            isLiked = reel.id in likedReelIds,
            isOwner = reel.authorId == currentUser.userId,
            isFollowing = reel.authorId in myFollowingIds,
            onLikeToggle = { onLikeReel(reel, reel.id in likedReelIds) },
            onDoubleTapLike = { if (reel.id !in likedReelIds) onLikeReel(reel, false) },
            onCommentClick = { onOpenComments(reel) },
            onAuthorClick = { onAuthorClick(reel.authorId) },
            onFollowClick = { authorUser?.let { onToggleFollow(it, reel.authorId in myFollowingIds) } },
            onDeleteClick = { onDeleteReel(reel) },
          )
        }
      }
    }
  }
}

@Composable
private fun ReelPageItem(
  reel: IgReel,
  isLiked: Boolean,
  isOwner: Boolean,
  isFollowing: Boolean,
  onLikeToggle: () -> Unit,
  onDoubleTapLike: () -> Unit,
  onCommentClick: () -> Unit,
  onAuthorClick: () -> Unit,
  onFollowClick: () -> Unit,
  onDeleteClick: () -> Unit,
) {
  var isPaused by remember { mutableStateOf(false) }
  var isMuted by remember { mutableStateOf(false) }
  var showDoubleTapHeart by remember { mutableStateOf(false) }

  LaunchedEffect(showDoubleTapHeart) {
    if (showDoubleTapHeart) {
      delay(750L)
      showDoubleTapHeart = false
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "reel_motion")
  val kenBurnsScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = if (isPaused) 1.0f else 1.12f,
    animationSpec = infiniteRepeatable(animation = tween(6000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
    label = "ken_burns",
  )
  val discAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = if (isPaused) 0f else 360f,
    animationSpec = infiniteRepeatable(animation = tween(3200, easing = LinearEasing), repeatMode = RepeatMode.Restart),
    label = "disc_spin",
  )

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color.Black)
        .pointerInput(reel.id) {
          detectTapGestures(
            onTap = { isPaused = !isPaused },
            onDoubleTap = {
              showDoubleTapHeart = true
              onDoubleTapLike()
            },
          )
        }
  ) {
    VistagramMediaImage(
      mediaRef = reel.mediaUrl,
      contentDescription = "Reel by ${reel.authorUsername}",
      filter = reel.resolvedFilter(),
      contentScale = ContentScale.Crop,
      modifier =
        Modifier.fillMaxSize().graphicsLayer {
          scaleX = kenBurnsScale
          scaleY = kenBurnsScale
        },
    )

    // Bottom Gradient Scrim
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(240.dp)
          .align(Alignment.BottomCenter)
          .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
    )

    // Top Audio Mute Toggle
    IconButton(
      onClick = { isMuted = !isMuted },
      modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
    ) {
      Icon(
        imageVector = if (isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
        contentDescription = if (isMuted) "Unmute" else "Mute",
        tint = Color.White,
      )
    }

    if (isPaused) {
      Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.55f),
        modifier = Modifier.align(Alignment.Center).size(72.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Pause,
          contentDescription = "Paused",
          tint = Color.White,
          modifier = Modifier.padding(18.dp),
        )
      }
    }

    DoubleTapHeartOverlay(
      visible = showDoubleTapHeart,
      modifier = Modifier.align(Alignment.Center),
    )

    // Right Action Rail
    Column(
      modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onLikeToggle) {
          Icon(
            imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "Like reel",
            tint = if (isLiked) IgHeartRed else Color.White,
            modifier = Modifier.size(30.dp),
          )
        }
        Text(
          text = formatCompactCount(reel.likesCount),
          style = MaterialTheme.typography.labelSmall,
          color = Color.White,
        )
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onCommentClick) {
          Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = "Comments",
            tint = Color.White,
            modifier = Modifier.size(28.dp),
          )
        }
        Text(
          text = formatCompactCount(reel.commentsCount),
          style = MaterialTheme.typography.labelSmall,
          color = Color.White,
        )
      }

      if (isOwner) {
        IconButton(onClick = onDeleteClick) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = "Delete reel",
            tint = Color.White,
            modifier = Modifier.size(26.dp),
          )
        }
      }

      // Spinning Audio Disc
      Surface(
        shape = CircleShape,
        color = Color(0xFF1F2937),
        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.75f)),
        modifier = Modifier.size(38.dp).rotate(discAngle),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
      }
    }

    // Bottom Creator & Caption Overlay
    Column(
      modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(0.78f).padding(start = 16.dp, bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        VistagramAvatar(
          name = reel.authorUsername,
          avatarUrl = reel.authorAvatarUrl,
          size = 34.dp,
          onClick = onAuthorClick,
        )
        Text(
          text = "@${reel.authorUsername}",
          style = MaterialTheme.typography.titleSmall,
          color = Color.White,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.clickable(onClick = onAuthorClick),
        )
        if (!isOwner) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isFollowing) Color.White.copy(alpha = 0.2f) else Color.Transparent,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
            modifier = Modifier.clickable(onClick = onFollowClick),
          ) {
            Text(
              text = if (isFollowing) "Following" else "Follow",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }
      }

      if (reel.caption.isNotBlank()) {
        Text(
          text = reel.caption,
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Surface(
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.45f),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
          Text(
            text = reel.audioTrack.ifBlank { "Original Audio" },
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}
