package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.IgPost
import com.example.model.IgStory
import com.example.model.IgUser
import com.example.model.UiState
import com.example.ui.components.DoubleTapHeartOverlay
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.RichCaptionText
import com.example.ui.components.VerifiedBadge
import com.example.ui.components.VistagramAvatar
import com.example.ui.components.VistagramMediaImage
import com.example.ui.components.formatCompactCount
import com.example.ui.components.formatIgRelativeTime
import com.example.ui.theme.IgBlue
import com.example.ui.theme.IgHeartRed
import kotlinx.coroutines.delay

@Composable
fun HomeFeedScreen(
  currentUser: IgUser,
  postsState: UiState<List<IgPost>>,
  storiesState: UiState<List<IgStory>>,
  likedPostIds: Set<String>,
  bookmarkedPostIds: Set<String>,
  viewedStoryIds: Set<String>,
  onOpenStory: (String) -> Unit,
  onAddStoryClick: () -> Unit,
  onCreatePostClick: () -> Unit,
  onAuthorClick: (String) -> Unit,
  onLikeToggle: (IgPost, Boolean) -> Unit,
  onDoubleTapLike: (IgPost) -> Unit,
  onBookmarkToggle: (IgPost, Boolean) -> Unit,
  onOpenComments: (IgPost) -> Unit,
  onSharePost: (IgPost) -> Unit,
  onEditPost: (IgPost) -> Unit,
  onDeletePost: (IgPost) -> Unit,
  modifier: Modifier = Modifier,
) {
  val stories = (storiesState as? UiState.Success)?.data.orEmpty()
  val storyAuthors =
    remember(stories, currentUser.userId) {
      val grouped = stories.groupBy { it.authorId }
      val myStories = grouped[currentUser.userId].orEmpty()
      val otherAuthors =
        grouped.entries
          .filter { it.key != currentUser.userId }
          .map { (authorId, list) ->
            val first = list.first()
            Triple(authorId, first.authorUsername, first.authorAvatarUrl) to list
          }
      myStories to otherAuthors
    }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 24.dp),
  ) {
    item(key = "stories_tray") {
      StoriesTrayRow(
        currentUser = currentUser,
        myStories = storyAuthors.first,
        otherAuthors = storyAuthors.second,
        viewedStoryIds = viewedStoryIds,
        onAddStoryClick = onAddStoryClick,
        onOpenStory = onOpenStory,
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
    }

    when (postsState) {
      UiState.Loading -> {
        item { LoadingStateView("Loading your feed…") }
      }
      is UiState.Error -> {
        item { ErrorStateView(postsState.message) }
      }
      is UiState.Success -> {
        val posts = postsState.data
        if (posts.isEmpty()) {
          item {
            EmptyStateView(
              icon = Icons.Rounded.PhotoLibrary,
              title = stringResource(R.string.no_posts_title),
              subtitle = stringResource(R.string.no_posts_subtitle),
              actionLabel = "Create Post",
              onAction = onCreatePostClick,
            )
          }
        } else {
          items(posts, key = { it.id }) { post ->
            FeedPostItem(
              post = post,
              isLiked = post.id in likedPostIds,
              isBookmarked = post.id in bookmarkedPostIds,
              isOwner = post.authorId == currentUser.userId,
              hasActiveStory = stories.any { it.authorId == post.authorId },
              onAuthorClick = { onAuthorClick(post.authorId) },
              onStoryRingClick = {
                if (stories.any { it.authorId == post.authorId }) {
                  onOpenStory(post.authorId)
                } else {
                  onAuthorClick(post.authorId)
                }
              },
              onLikeToggle = { onLikeToggle(post, post.id in likedPostIds) },
              onDoubleTapLike = { onDoubleTapLike(post) },
              onBookmarkToggle = { onBookmarkToggle(post, post.id in bookmarkedPostIds) },
              onCommentClick = { onOpenComments(post) },
              onShareClick = { onSharePost(post) },
              onEditClick = { onEditPost(post) },
              onDeleteClick = { onDeletePost(post) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun StoriesTrayRow(
  currentUser: IgUser,
  myStories: List<IgStory>,
  otherAuthors: List<Pair<Triple<String, String, String?>, List<IgStory>>>,
  viewedStoryIds: Set<String>,
  onAddStoryClick: () -> Unit,
  onOpenStory: (String) -> Unit,
) {
  LazyRow(
    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item(key = "my_story") {
      val hasMine = myStories.isNotEmpty()
      val allMineViewed = hasMine && myStories.all { it.id in viewedStoryIds }
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
          Modifier.width(72.dp).clickable {
            if (hasMine) onOpenStory(currentUser.userId) else onAddStoryClick()
          },
      ) {
        Box(modifier = Modifier.size(68.dp)) {
          VistagramAvatar(
            name = currentUser.username,
            avatarUrl = currentUser.avatarUrl,
            size = 62.dp,
            hasStory = hasMine,
            storyViewed = allMineViewed,
            modifier = Modifier.align(Alignment.Center),
          )
          Surface(
            shape = CircleShape,
            color = IgBlue,
            modifier =
              Modifier.size(22.dp)
                .align(Alignment.BottomEnd)
                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                .clickable(onClick = onAddStoryClick),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add to story",
                tint = Color.White,
                modifier = Modifier.size(14.dp),
              )
            }
          }
        }
        Text(
          text = "Your story",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    items(otherAuthors, key = { it.first.first }) { (meta, authorStories) ->
      val (authorId, username, avatarUrl) = meta
      val allViewed = authorStories.all { it.id in viewedStoryIds }
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(72.dp).clickable { onOpenStory(authorId) },
      ) {
        VistagramAvatar(
          name = username,
          avatarUrl = avatarUrl,
          size = 62.dp,
          hasStory = true,
          storyViewed = allViewed,
        )
        Text(
          text = username,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun FeedPostItem(
  post: IgPost,
  isLiked: Boolean,
  isBookmarked: Boolean,
  isOwner: Boolean,
  hasActiveStory: Boolean,
  onAuthorClick: () -> Unit,
  onStoryRingClick: () -> Unit,
  onLikeToggle: () -> Unit,
  onDoubleTapLike: () -> Unit,
  onBookmarkToggle: () -> Unit,
  onCommentClick: () -> Unit,
  onShareClick: () -> Unit,
  onEditClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showDoubleTapHeart by remember { mutableStateOf(false) }
  var menuExpanded by remember { mutableStateOf(false) }

  LaunchedEffect(showDoubleTapHeart) {
    if (showDoubleTapHeart) {
      delay(750L)
      showDoubleTapHeart = false
    }
  }

  Column(
    modifier = modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("feed_post_${post.id}"),
  ) {
    // Post Header
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f),
      ) {
        VistagramAvatar(
          name = post.authorUsername,
          avatarUrl = post.authorAvatarUrl,
          size = 34.dp,
          hasStory = hasActiveStory,
          onClick = onStoryRingClick,
        )
        Column(modifier = Modifier.clickable(onClick = onAuthorClick)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = post.authorUsername,
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onBackground,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (post.authorVerified) VerifiedBadge(size = 14.dp)
          }
          if (post.location.isNotBlank()) {
            Text(
              text = post.location,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      Box {
        IconButton(onClick = { menuExpanded = true }) {
          Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = "Post options",
            tint = MaterialTheme.colorScheme.onBackground,
          )
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
          DropdownMenuItem(
            text = { Text(if (isBookmarked) "Remove from Saved" else "Save post") },
            onClick = { menuExpanded = false; onBookmarkToggle() },
          )
          DropdownMenuItem(
            text = { Text("Share to Direct") },
            onClick = { menuExpanded = false; onShareClick() },
          )
          if (isOwner) {
            DropdownMenuItem(
              text = { Text("Edit caption") },
              onClick = { menuExpanded = false; onEditClick() },
            )
            DropdownMenuItem(
              text = { Text("Delete post", color = MaterialTheme.colorScheme.error) },
              onClick = { menuExpanded = false; onDeleteClick() },
            )
          }
        }
      }
    }

    // Media Carousel
    val mediaList = post.mediaUrls.ifEmpty { listOf("res:img_sample_sunset") }
    val pagerState = rememberPagerState(pageCount = { mediaList.size })
    val filter = remember(post.filterName) { post.resolvedFilter() }

    Box(
      modifier =
        Modifier.fillMaxWidth()
          .aspectRatio(1f)
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .pointerInput(post.id) {
            detectTapGestures(
              onDoubleTap = {
                showDoubleTapHeart = true
                onDoubleTapLike()
              }
            )
          }
    ) {
      HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        VistagramMediaImage(
          mediaRef = mediaList[page],
          contentDescription = "Post photo ${page + 1} by ${post.authorUsername}",
          filter = filter,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }

      if (mediaList.size > 1) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color.Black.copy(alpha = 0.65f),
          modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
        ) {
          Text(
            text = "${pagerState.currentPage + 1}/${mediaList.size}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
          )
        }
      }

      DoubleTapHeartOverlay(
        visible = showDoubleTapHeart,
        modifier = Modifier.align(Alignment.Center),
      )
    }

    // Action Bar + Carousel Dots
    Box(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
      Row(
        modifier = Modifier.align(Alignment.CenterStart),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = onLikeToggle,
          modifier = Modifier.testTag("like_post_${post.id}"),
        ) {
          Icon(
            imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (isLiked) "Unlike" else "Like",
            tint = if (isLiked) IgHeartRed else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(26.dp),
          )
        }
        IconButton(
          onClick = onCommentClick,
          modifier = Modifier.testTag("comment_post_${post.id}"),
        ) {
          Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = "Comments",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(24.dp),
          )
        }
        IconButton(
          onClick = onShareClick,
          modifier = Modifier.testTag("share_post_${post.id}"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.Send,
            contentDescription = "Share to Direct",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(23.dp),
          )
        }
      }

      if (mediaList.size > 1) {
        Row(
          modifier = Modifier.align(Alignment.Center),
          horizontalArrangement = Arrangement.spacedBy(5.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          repeat(mediaList.size) { index ->
            val selected = pagerState.currentPage == index
            Box(
              modifier =
                Modifier.size(if (selected) 7.dp else 5.dp)
                  .clip(CircleShape)
                  .background(
                    if (selected) IgBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                  )
            )
          }
        }
      }

      IconButton(
        onClick = onBookmarkToggle,
        modifier = Modifier.align(Alignment.CenterEnd).testTag("save_post_${post.id}"),
      ) {
        Icon(
          imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder,
          contentDescription = if (isBookmarked) "Unsave" else "Save",
          tint = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.size(25.dp),
        )
      }
    }

    // Likes, Caption, Comments & Time
    Column(
      modifier = Modifier.padding(horizontal = 14.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      if (post.likesCount > 0) {
        Text(
          text = "${formatCompactCount(post.likesCount)} ${if (post.likesCount == 1L) "like" else "likes"}",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      if (post.caption.isNotBlank()) {
        RichCaptionText(
          username = post.authorUsername,
          caption = post.caption,
          onUsernameClick = onAuthorClick,
        )
      }

      AnimatedVisibility(visible = post.commentsCount > 0) {
        Text(
          text = "View all ${post.commentsCount} ${if (post.commentsCount == 1L) "comment" else "comments"}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.clickable(onClick = onCommentClick).padding(vertical = 2.dp),
        )
      }

      Text(
        text = formatIgRelativeTime(post.createdAt),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
