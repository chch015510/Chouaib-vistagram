package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ImageEncodingHelper
import com.example.model.FollowEdge
import com.example.model.IgPost
import com.example.model.IgStory
import com.example.model.IgUser
import com.example.model.UiState
import com.example.ui.FollowListMode
import com.example.ui.FollowListTarget
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.VerifiedBadge
import com.example.ui.components.VistagramAvatar
import com.example.ui.components.VistagramMediaImage
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.IgBlue
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
  profileUser: IgUser,
  isSelf: Boolean,
  isFollowing: Boolean,
  allPosts: List<IgPost>,
  userStories: List<IgStory>,
  bookmarkedPostIds: Set<String>,
  likedPostIds: Set<String>,
  onEditProfileClick: () -> Unit,
  onToggleFollow: () -> Unit,
  onMessageClick: () -> Unit,
  onOpenStory: () -> Unit,
  onAddStoryClick: () -> Unit,
  onOpenFollowList: (FollowListMode) -> Unit,
  onOpenPost: (IgPost) -> Unit,
  onSignOutClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedTab by rememberSaveable(profileUser.userId) { mutableIntStateOf(0) }

  val userPosts = remember(allPosts, profileUser.userId) { allPosts.filter { it.authorId == profileUser.userId } }
  val savedPosts = remember(allPosts, bookmarkedPostIds) { allPosts.filter { it.id in bookmarkedPostIds } }
  val likedPosts = remember(allPosts, likedPostIds) { allPosts.filter { it.id in likedPostIds } }

  val displayedPosts =
    when (selectedTab) {
      0 -> userPosts
      1 -> if (isSelf) savedPosts else userPosts
      else -> if (isSelf) likedPosts else userPosts
    }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 32.dp),
  ) {
    item {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Avatar + Stats Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          VistagramAvatar(
            name = profileUser.username,
            avatarUrl = profileUser.avatarUrl,
            size = 82.dp,
            hasStory = userStories.isNotEmpty(),
            onClick = { if (userStories.isNotEmpty()) onOpenStory() else if (isSelf) onAddStoryClick() },
          )

          Row(
            modifier = Modifier.weight(1f).padding(start = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            ProfileStatItem(count = userPosts.size.toLong(), label = "Posts", onClick = {})
            ProfileStatItem(
              count = profileUser.followersCount,
              label = "Followers",
              onClick = { onOpenFollowList(FollowListMode.FOLLOWERS) },
            )
            ProfileStatItem(
              count = profileUser.followingCount,
              label = "Following",
              onClick = { onOpenFollowList(FollowListMode.FOLLOWING) },
            )
          }
        }

        // Display Name, Bio, Website
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Text(
              text = profileUser.displayName.ifBlank { profileUser.username },
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground,
            )
            if (profileUser.verified) VerifiedBadge(size = 15.dp)
          }
          if (profileUser.bio.isNotBlank()) {
            Text(
              text = profileUser.bio,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onBackground,
            )
          }
          if (profileUser.website.isNotBlank()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = profileUser.website,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
              )
            }
          }
        }

        // Action Buttons
        if (isSelf) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = onEditProfileClick,
              modifier = Modifier.weight(1f).testTag("edit_profile_button"),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(vertical = 8.dp),
            ) {
              Text("Edit profile", style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
              onClick = onSignOutClick,
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
              modifier = Modifier.testTag("sign_out_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.Logout,
                contentDescription = stringResource(R.string.sign_out),
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(stringResource(R.string.sign_out), style = MaterialTheme.typography.labelLarge)
            }
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (isFollowing) {
              OutlinedButton(
                onClick = onToggleFollow,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
              ) {
                Text("Following", style = MaterialTheme.typography.labelLarge)
              }
            } else {
              Button(
                onClick = onToggleFollow,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IgBlue),
              ) {
                Text("Follow", style = MaterialTheme.typography.labelLarge)
              }
            }
            OutlinedButton(
              onClick = onMessageClick,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
            ) {
              Text("Message", style = MaterialTheme.typography.labelLarge)
            }
          }
        }

        // Highlights Row
        if (userPosts.isNotEmpty() || isSelf) {
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 4.dp),
          ) {
            if (isSelf) {
              item {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                  modifier = Modifier.clickable(onClick = onAddStoryClick),
                ) {
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.size(58.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Rounded.CameraAlt, contentDescription = "New Highlight", modifier = Modifier.size(22.dp))
                    }
                  }
                  Text("New", style = MaterialTheme.typography.labelSmall)
                }
              }
            }
            items(userPosts.take(5), key = { "hl_${it.id}" }) { post ->
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.width(64.dp).clickable { onOpenPost(post) },
              ) {
                Box(
                  modifier =
                    Modifier.size(58.dp)
                      .clip(CircleShape)
                      .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                      .padding(3.dp)
                      .clip(CircleShape),
                ) {
                  VistagramMediaImage(
                    mediaRef = post.mediaUrls.firstOrNull() ?: "res:img_sample_sunset",
                    contentDescription = "Highlight",
                    modifier = Modifier.fillMaxSize(),
                  )
                }
                Text(
                  text = post.resolvedCategory().label,
                  style = MaterialTheme.typography.labelSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }
    }

    // Tabs: Posts / Saved / Liked
    item {
      val tabs =
        if (isSelf) {
          listOf(
            Triple("Posts", Icons.Rounded.GridOn, Icons.Outlined.GridOn),
            Triple("Saved", Icons.Rounded.Bookmark, Icons.Outlined.BookmarkBorder),
            Triple("Liked", Icons.Rounded.Favorite, Icons.Outlined.FavoriteBorder),
          )
        } else {
          listOf(Triple("Posts", Icons.Rounded.GridOn, Icons.Outlined.GridOn))
        }

      TabRow(selectedTabIndex = selectedTab.coerceAtMost(tabs.lastIndex)) {
        tabs.forEachIndexed { index, (title, activeIcon, inactiveIcon) ->
          val selected = selectedTab == index
          Tab(
            selected = selected,
            onClick = { selectedTab = index },
            icon = {
              Icon(
                imageVector = if (selected) activeIcon else inactiveIcon,
                contentDescription = title,
              )
            },
          )
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
    }

    if (displayedPosts.isEmpty()) {
      item {
        EmptyStateView(
          icon = Icons.Outlined.GridOn,
          title =
            when (selectedTab) {
              1 -> "No saved posts"
              2 -> "No liked posts"
              else -> "No posts yet"
            },
          subtitle =
            when (selectedTab) {
              1 -> "Tap the bookmark icon on any post to save it here."
              2 -> "Double-tap photos you love to collect them here."
              else -> "Captured photos and carousels will appear in this grid."
            },
        )
      }
    } else {
      items(displayedPosts.chunked(3)) { rowPosts ->
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
          horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          rowPosts.forEach { post ->
            Box(
              modifier =
                Modifier.weight(1f)
                  .aspectRatio(1f)
                  .background(MaterialTheme.colorScheme.surfaceVariant)
                  .clickable { onOpenPost(post) },
            ) {
              VistagramMediaImage(
                mediaRef = post.mediaUrls.firstOrNull() ?: "res:img_sample_sunset",
                contentDescription = "Grid photo",
                filter = post.resolvedFilter(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
              if (post.mediaUrls.size > 1) {
                Icon(
                  imageVector = Icons.Rounded.Collections,
                  contentDescription = "Carousel",
                  tint = Color.White,
                  modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp),
                )
              }
            }
          }
          repeat(3 - rowPosts.size) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }
  }
}

@Composable
private fun ProfileStatItem(count: Long, label: String, onClick: () -> Unit) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp),
  ) {
    Text(
      text = formatCompactCount(count),
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
fun EditProfileDialog(
  user: IgUser,
  isSubmitting: Boolean,
  onDismiss: () -> Unit,
  onSave: (username: String, displayName: String, bio: String, website: String, avatarUrl: String?) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var username by rememberSaveable { mutableStateOf(user.username) }
  var displayName by rememberSaveable { mutableStateOf(user.displayName) }
  var bio by rememberSaveable { mutableStateOf(user.bio) }
  var website by rememberSaveable { mutableStateOf(user.website) }
  var avatarUrl by rememberSaveable { mutableStateOf(user.avatarUrl) }
  var isEncoding by remember { mutableStateOf(false) }

  val avatarPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        isEncoding = true
        scope.launch {
          val encoded = ImageEncodingHelper.encodeUriToDataUrl(context, uri, maxDim = 320)
          if (encoded != null) avatarUrl = encoded
          isEncoding = false
        }
      }
    }

  val cleanHandle = username.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "")
  val canSave = cleanHandle.length in 2..30 && displayName.trim().length in 2..60 && !isSubmitting && !isEncoding

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edit Profile", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          VistagramAvatar(name = cleanHandle.ifBlank { "V" }, avatarUrl = avatarUrl, size = 60.dp)
          TextButton(
            onClick = {
              avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
          ) {
            Text(if (isEncoding) "Processing…" else "Change profile photo")
          }
        }

        OutlinedTextField(
          value = username,
          onValueChange = { username = it.take(30) },
          label = { Text("Username") },
          prefix = { Text("@") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = displayName,
          onValueChange = { displayName = it.take(60) },
          label = { Text("Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = bio,
          onValueChange = { bio = it.take(300) },
          label = { Text("Bio") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = website,
          onValueChange = { website = it.take(120) },
          label = { Text("Website / Link") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(cleanHandle, displayName, bio, website, avatarUrl) },
        enabled = canSave,
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
        } else {
          Text("Done")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListBottomSheet(
  target: FollowListTarget,
  listState: UiState<List<FollowEdge>>,
  onDismiss: () -> Unit,
  onSelectUser: (String) -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text =
          if (target.mode == FollowListMode.FOLLOWERS) "@${target.username} · Followers"
          else "@${target.username} · Following",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      HorizontalDivider()

      when (listState) {
        UiState.Loading -> LoadingStateView("Loading…")
        is UiState.Error -> Text(listState.message, color = MaterialTheme.colorScheme.error)
        is UiState.Success -> {
          if (listState.data.isEmpty()) {
            Text(
              text =
                if (target.mode == FollowListMode.FOLLOWERS) "No followers yet."
                else "Not following anyone yet.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 20.dp),
            )
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxWidth().height(320.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(listState.data, key = { it.id }) { edge ->
                val targetId =
                  if (target.mode == FollowListMode.FOLLOWERS) edge.userId else edge.targetUserId
                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(10.dp))
                      .clickable { onSelectUser(targetId) }
                      .padding(vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  VistagramAvatar(name = edge.username, avatarUrl = edge.avatarUrl, size = 44.dp)
                  Column {
                    Text(edge.username, style = MaterialTheme.typography.titleSmall)
                    Text(
                      edge.displayName,
                      style = MaterialTheme.typography.bodySmall,
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
  }
}
