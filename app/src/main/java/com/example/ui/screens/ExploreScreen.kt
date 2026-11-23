package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.IgPost
import com.example.model.IgUser
import com.example.model.PostCategory
import com.example.model.UiState
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.VerifiedBadge
import com.example.ui.components.VistagramAvatar
import com.example.ui.components.VistagramMediaImage

@Composable
fun ExploreScreen(
  currentUser: IgUser,
  usersState: UiState<List<IgUser>>,
  postsState: UiState<List<IgPost>>,
  myFollowingIds: Set<String>,
  searchQuery: String,
  selectedCategory: PostCategory?,
  onSearchChange: (String) -> Unit,
  onCategorySelect: (PostCategory?) -> Unit,
  onUserClick: (String) -> Unit,
  onToggleFollow: (IgUser, Boolean) -> Unit,
  onPostClick: (IgPost) -> Unit,
  modifier: Modifier = Modifier,
) {
  val allUsers = (usersState as? UiState.Success)?.data.orEmpty()
  val allPosts = (postsState as? UiState.Success)?.data.orEmpty()

  val q = searchQuery.trim()
  val matchingUsers =
    remember(allUsers, q) {
      if (q.isEmpty()) emptyList()
      else {
        allUsers.filter {
          it.username.contains(q, ignoreCase = true) ||
            it.displayName.contains(q, ignoreCase = true)
        }
      }
    }

  val filteredPosts =
    remember(allPosts, q, selectedCategory) {
      allPosts.filter { post ->
        val matchesCategory = selectedCategory == null || post.resolvedCategory() == selectedCategory
        val matchesSearch =
          q.isEmpty() ||
            post.caption.contains(q, ignoreCase = true) ||
            post.authorUsername.contains(q, ignoreCase = true) ||
            post.location.contains(q, ignoreCase = true)
        matchesCategory && matchesSearch
      }
    }

  Column(modifier = modifier.fillMaxSize()) {
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchChange,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp).testTag("explore_search_input"),
      placeholder = { Text("Search accounts, tags, places…") },
      leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { onSearchChange("") }) {
            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(14.dp),
      colors =
        OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
          focusedBorderColor = Color.Transparent,
          unfocusedBorderColor = Color.Transparent,
        ),
    )

    LazyRow(
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      item {
        FilterChip(
          selected = selectedCategory == null,
          onClick = { onCategorySelect(null) },
          label = { Text("For You") },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.onBackground,
              selectedLabelColor = MaterialTheme.colorScheme.background,
            ),
        )
      }
      items(PostCategory.entries) { category ->
        FilterChip(
          selected = selectedCategory == category,
          onClick = { onCategorySelect(if (selectedCategory == category) null else category) },
          label = { Text(category.label) },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.onBackground,
              selectedLabelColor = MaterialTheme.colorScheme.background,
            ),
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    if (q.isNotEmpty() && matchingUsers.isNotEmpty()) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item {
          Text(
            text = "Accounts",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        items(matchingUsers, key = { it.userId }) { user ->
          val isSelf = user.userId == currentUser.userId
          val isFollowing = user.userId in myFollowingIds
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable { onUserClick(user.userId) }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.weight(1f),
            ) {
              VistagramAvatar(name = user.username, avatarUrl = user.avatarUrl, size = 46.dp)
              Column {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Text(
                    text = user.username,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                  )
                  if (user.verified) VerifiedBadge(size = 14.dp)
                }
                Text(
                  text = user.displayName,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            if (!isSelf) {
              if (isFollowing) {
                OutlinedButton(
                  onClick = { onToggleFollow(user, true) },
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                  Text("Following", style = MaterialTheme.typography.labelMedium)
                }
              } else {
                Button(
                  onClick = { onToggleFollow(user, false) },
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                ) {
                  Text("Follow", style = MaterialTheme.typography.labelMedium)
                }
              }
            }
          }
        }

        if (filteredPosts.isNotEmpty()) {
          item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Matching Posts",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          items(filteredPosts.chunked(3)) { rowPosts ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              rowPosts.forEach { post ->
                ExploreGridTile(
                  post = post,
                  onClick = { onPostClick(post) },
                  modifier = Modifier.weight(1f).aspectRatio(1f),
                )
              }
              repeat(3 - rowPosts.size) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }
      }
    } else {
      when (postsState) {
        UiState.Loading -> LoadingStateView("Exploring posts…")
        is UiState.Error -> ErrorStateView(postsState.message)
        is UiState.Success -> {
          if (filteredPosts.isEmpty()) {
            EmptyStateView(
              icon = Icons.Rounded.Explore,
              title = "Nothing to explore yet",
              subtitle =
                if (q.isNotEmpty() || selectedCategory != null) {
                  "No posts matched your search or category filter."
                } else {
                  "Posts shared by creators will appear in the Explore grid."
                },
            )
          } else {
            LazyVerticalGrid(
              columns = GridCells.Fixed(3),
              modifier = Modifier.fillMaxSize(),
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp),
              contentPadding = PaddingValues(bottom = 24.dp),
            ) {
              items(
                items = filteredPosts,
                key = { it.id },
                span = { post ->
                  val idx = filteredPosts.indexOf(post)
                  if (idx % 10 == 0 && filteredPosts.size >= 4) GridItemSpan(2) else GridItemSpan(1)
                },
              ) { post ->
                val idx = filteredPosts.indexOf(post)
                val aspect = if (idx % 10 == 0 && filteredPosts.size >= 4) 1.5f else 1f
                ExploreGridTile(
                  post = post,
                  onClick = { onPostClick(post) },
                  modifier = Modifier.fillMaxWidth().aspectRatio(aspect),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ExploreGridTile(
  post: IgPost,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick),
  ) {
    VistagramMediaImage(
      mediaRef = post.mediaUrls.firstOrNull() ?: "res:img_sample_sunset",
      contentDescription = "Explore post by ${post.authorUsername}",
      filter = post.resolvedFilter(),
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )
    if (post.mediaUrls.size > 1) {
      Icon(
        imageVector = Icons.Rounded.Collections,
        contentDescription = "Carousel",
        tint = Color.White,
        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp),
      )
    }
  }
}
