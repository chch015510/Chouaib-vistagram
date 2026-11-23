package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.signOut
import com.example.model.IgPost
import com.example.model.UiState
import com.example.ui.components.VerifiedBadge
import com.example.ui.screens.ChatThreadScreen
import com.example.ui.screens.CommentsBottomSheet
import com.example.ui.screens.CreateMode
import com.example.ui.screens.CreateScreen
import com.example.ui.screens.DirectMessagesInboxScreen
import com.example.ui.screens.EditPostDialog
import com.example.ui.screens.EditProfileDialog
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FollowListBottomSheet
import com.example.ui.screens.HomeFeedScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PostDetailOverlayScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReelsScreen
import com.example.ui.screens.SharePostSheet
import com.example.ui.screens.StoryViewerScreen
import com.example.ui.theme.IgHeartRed

enum class IgNavTab(
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
) {
  HOME(R.string.tab_home, Icons.Rounded.Home, Icons.Outlined.Home, "nav_tab_home"),
  EXPLORE(R.string.tab_explore, Icons.Rounded.Explore, Icons.Outlined.Explore, "nav_tab_explore"),
  CREATE(R.string.tab_create, Icons.Rounded.AddBox, Icons.Outlined.AddBox, "nav_tab_create"),
  REELS(R.string.tab_reels, Icons.Rounded.Movie, Icons.Outlined.Movie, "nav_tab_reels"),
  PROFILE(R.string.tab_profile, Icons.Rounded.Person, Icons.Outlined.Person, "nav_tab_profile"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistagramApp(viewModel: InstagramViewModel) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val snackbarHostState = remember { SnackbarHostState() }

  val usersState by viewModel.usersState.collectAsStateWithLifecycle()
  val postsState by viewModel.postsState.collectAsStateWithLifecycle()
  val storiesState by viewModel.storiesState.collectAsStateWithLifecycle()
  val reelsState by viewModel.reelsState.collectAsStateWithLifecycle()
  val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
  val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()

  val likedPostIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
  val bookmarkedPostIds by viewModel.bookmarkedPostIds.collectAsStateWithLifecycle()
  val likedReelIds by viewModel.likedReelIds.collectAsStateWithLifecycle()
  val viewedStoryIds by viewModel.viewedStoryIds.collectAsStateWithLifecycle()
  val myFollowingIds by viewModel.myFollowingIds.collectAsStateWithLifecycle()

  val exploreQuery by viewModel.exploreQuery.collectAsStateWithLifecycle()
  val exploreCategory by viewModel.exploreCategory.collectAsStateWithLifecycle()
  val commentTarget by viewModel.commentTarget.collectAsStateWithLifecycle()
  val commentsState by viewModel.commentsState.collectAsStateWithLifecycle()
  val followListTarget by viewModel.followListTarget.collectAsStateWithLifecycle()
  val followListState by viewModel.followListState.collectAsStateWithLifecycle()
  val activeChatPartnerId by viewModel.activeChatPartnerId.collectAsStateWithLifecycle()
  val activeChatMessagesState by viewModel.activeChatMessagesState.collectAsStateWithLifecycle()
  val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

  var currentTab by rememberSaveable { mutableStateOf(IgNavTab.HOME) }
  var createInitialMode by rememberSaveable { mutableStateOf(CreateMode.POST) }
  var viewingUserId by rememberSaveable { mutableStateOf<String?>(null) }
  var activeStoryAuthorId by rememberSaveable { mutableStateOf<String?>(null) }
  var detailPostId by rememberSaveable { mutableStateOf<String?>(null) }
  var showNotifications by rememberSaveable { mutableStateOf(false) }
  var showDmInbox by rememberSaveable { mutableStateOf(false) }
  var showEditProfileDialog by rememberSaveable { mutableStateOf(false) }
  var editingPost by remember { mutableStateOf<IgPost?>(null) }
  var sharingPost by remember { mutableStateOf<IgPost?>(null) }

  LaunchedEffect(Unit) {
    viewModel.snackbarMessages.collect { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  val allUsers = (usersState as? UiState.Success)?.data.orEmpty()
  val allPosts = (postsState as? UiState.Success)?.data.orEmpty()
  val allStories = (storiesState as? UiState.Success)?.data.orEmpty()
  val notifications = (notificationsState as? UiState.Success)?.data.orEmpty()
  val unreadNotificationsCount = remember(notifications) { notifications.count { !it.read } }
  val currentUser = viewModel.currentUser(allUsers)

  // Full-Screen Overlays (with BackHandler inside each screen)
  val storyAuthorId = activeStoryAuthorId
  if (storyAuthorId != null) {
    val authorStories = allStories.filter { it.authorId == storyAuthorId }
    StoryViewerScreen(
      stories = authorStories,
      currentUserId = currentUser.userId,
      onMarkViewed = { viewModel.markStoryViewed(it) },
      onSendReply = { story, text ->
        val recipient = allUsers.firstOrNull { it.userId == story.authorId }
        viewModel.replyToStory(story, currentUser, recipient, text) {}
      },
      onDeleteStory = { story ->
        viewModel.deleteStory(story.id) { activeStoryAuthorId = null }
      },
      onAuthorClick = { uid ->
        activeStoryAuthorId = null
        viewingUserId = uid
      },
      onClose = { activeStoryAuthorId = null },
    )
    return
  }

  val chatPartnerId = activeChatPartnerId
  if (chatPartnerId != null) {
    val partner =
      allUsers.firstOrNull { it.userId == chatPartnerId }
        ?: currentUser.copy(userId = chatPartnerId, username = "creator", displayName = "Creator")
    ChatThreadScreen(
      currentUser = currentUser,
      partner = partner,
      messagesState = activeChatMessagesState,
      onSendMessage = { text -> viewModel.sendMessage(currentUser, partner, text) },
      onOpenAuthor = { uid ->
        viewModel.openChat(null)
        showDmInbox = false
        viewingUserId = uid
      },
      onBack = { viewModel.openChat(null) },
    )
    return
  }

  if (showDmInbox) {
    DirectMessagesInboxScreen(
      currentUser = currentUser,
      chatsState = chatsState,
      allUsers = allUsers,
      onOpenChat = { partnerId -> viewModel.openChat(partnerId) },
      onBack = { showDmInbox = false },
    )
    return
  }

  if (showNotifications) {
    NotificationsScreen(
      notificationsState = notificationsState,
      allUsers = allUsers,
      myFollowingIds = myFollowingIds,
      onMarkRead = viewModel::markNotificationsRead,
      onActorClick = { uid ->
        showNotifications = false
        viewingUserId = uid
      },
      onToggleFollow = { target, following -> viewModel.toggleFollow(currentUser, target, following) },
      onPostClick = { postId ->
        showNotifications = false
        detailPostId = postId
      },
      onBack = { showNotifications = false },
    )
    return
  }

  val selectedDetailPost = detailPostId?.let { id -> allPosts.firstOrNull { it.id == id } }
  if (selectedDetailPost != null) {
    PostDetailOverlayScreen(
      post = selectedDetailPost,
      isLiked = selectedDetailPost.id in likedPostIds,
      isBookmarked = selectedDetailPost.id in bookmarkedPostIds,
      isOwner = selectedDetailPost.authorId == currentUser.userId,
      hasActiveStory = allStories.any { it.authorId == selectedDetailPost.authorId },
      onBack = { detailPostId = null },
      onAuthorClick = { uid ->
        detailPostId = null
        viewingUserId = uid
      },
      onStoryRingClick = {
        if (allStories.any { it.authorId == selectedDetailPost.authorId }) {
          activeStoryAuthorId = selectedDetailPost.authorId
        } else {
          detailPostId = null
          viewingUserId = selectedDetailPost.authorId
        }
      },
      onLikeToggle = {
        viewModel.toggleLikePost(selectedDetailPost, selectedDetailPost.id in likedPostIds, currentUser)
      },
      onDoubleTapLike = {
        if (selectedDetailPost.id !in likedPostIds) {
          viewModel.toggleLikePost(selectedDetailPost, false, currentUser)
        }
      },
      onBookmarkToggle = {
        viewModel.toggleBookmarkPost(selectedDetailPost.id, selectedDetailPost.id in bookmarkedPostIds)
      },
      onOpenComments = {
        viewModel.openComments(
          CommentTarget(
            parentCollection = "ig_posts",
            parentId = selectedDetailPost.id,
            authorId = selectedDetailPost.authorId,
            thumbnailUrl = selectedDetailPost.mediaUrls.firstOrNull(),
          )
        )
      },
      onShareClick = { sharingPost = selectedDetailPost },
      onEditClick = { editingPost = selectedDetailPost },
      onDeleteClick = {
        viewModel.deletePost(selectedDetailPost, currentUser)
        detailPostId = null
      },
    )
  } else if (viewingUserId != null) {
    val targetUser = allUsers.firstOrNull { it.userId == viewingUserId }
    if (targetUser != null) {
      BackHandler { viewingUserId = null }
      Scaffold(
        topBar = {
          TopAppBar(
            navigationIcon = {
              IconButton(onClick = { viewingUserId = null }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
              }
            },
            title = {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(targetUser.username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (targetUser.verified) VerifiedBadge(size = 16.dp)
              }
            },
          )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      ) { innerPadding ->
        ProfileScreen(
          profileUser = targetUser,
          isSelf = targetUser.userId == currentUser.userId,
          isFollowing = targetUser.userId in myFollowingIds,
          allPosts = allPosts,
          userStories = allStories.filter { it.authorId == targetUser.userId },
          bookmarkedPostIds = bookmarkedPostIds,
          likedPostIds = likedPostIds,
          onEditProfileClick = { showEditProfileDialog = true },
          onToggleFollow = {
            viewModel.toggleFollow(currentUser, targetUser, targetUser.userId in myFollowingIds)
          },
          onMessageClick = { viewModel.openChat(targetUser.userId) },
          onOpenStory = { activeStoryAuthorId = targetUser.userId },
          onAddStoryClick = {
            viewingUserId = null
            createInitialMode = CreateMode.STORY
            currentTab = IgNavTab.CREATE
          },
          onOpenFollowList = { mode ->
            viewModel.openFollowList(FollowListTarget(targetUser.userId, targetUser.username, mode))
          },
          onOpenPost = { post -> detailPostId = post.id },
          onSignOutClick = { signOut(credentialManager = credentialManager, scope = scope) },
          modifier = Modifier.padding(innerPadding),
        )
      }
    } else {
      viewingUserId = null
    }
  } else {
    // Primary 5-Tab Navigation Shell
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavRail = maxWidth >= 600.dp

      Row(modifier = Modifier.fillMaxSize()) {
        if (useNavRail) {
          NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            Spacer(modifier = Modifier.size(16.dp))
            IgNavTab.entries.forEach { tab ->
              val selected = currentTab == tab
              NavigationRailItem(
                selected = selected,
                onClick = {
                  if (tab == IgNavTab.CREATE) createInitialMode = CreateMode.POST
                  currentTab = tab
                },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = stringResource(tab.labelRes),
                  )
                },
                label = { Text(stringResource(tab.labelRes)) },
                modifier = Modifier.testTag(tab.testTag),
              )
            }
          }
        }

        Scaffold(
          modifier = Modifier.weight(1f).fillMaxHeight(),
          containerColor =
            if (currentTab == IgNavTab.REELS) Color.Black else MaterialTheme.colorScheme.background,
          snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
          topBar = {
            when (currentTab) {
              IgNavTab.HOME -> {
                TopAppBar(
                  title = {
                    Text(
                      text = stringResource(R.string.app_name),
                      style = MaterialTheme.typography.headlineMedium,
                      fontWeight = FontWeight.Bold,
                    )
                  },
                  actions = {
                    IconButton(
                      onClick = { showNotifications = true },
                      modifier = Modifier.testTag("notifications_button"),
                    ) {
                      BadgedBox(
                        badge = {
                          if (unreadNotificationsCount > 0) {
                            Badge(containerColor = IgHeartRed)
                          }
                        }
                      ) {
                        Icon(
                          imageVector = Icons.Outlined.FavoriteBorder,
                          contentDescription = "Notifications",
                        )
                      }
                    }
                    IconButton(
                      onClick = { showDmInbox = true },
                      modifier = Modifier.testTag("direct_messages_button"),
                    ) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Direct Messages",
                      )
                    }
                  },
                  colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
              }
              IgNavTab.CREATE -> {
                TopAppBar(
                  title = { Text("Create", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                  colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
              }
              IgNavTab.PROFILE -> {
                TopAppBar(
                  title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                      Text(currentUser.username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                      if (currentUser.verified) VerifiedBadge(size = 16.dp)
                    }
                  },
                  actions = {
                    IconButton(onClick = {
                      createInitialMode = CreateMode.POST
                      currentTab = IgNavTab.CREATE
                    }) {
                      Icon(Icons.Outlined.AddBox, contentDescription = "Create post")
                    }
                  },
                  colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
              }
              else -> Unit
            }
          },
          bottomBar = {
            if (!useNavRail) {
              NavigationBar(
                containerColor =
                  if (currentTab == IgNavTab.REELS) Color(0xFF0A0A0C) else MaterialTheme.colorScheme.background,
                contentColor =
                  if (currentTab == IgNavTab.REELS) Color.White else MaterialTheme.colorScheme.onBackground,
                windowInsets = WindowInsets.navigationBars,
                tonalElevation = 0.dp,
              ) {
                IgNavTab.entries.forEach { tab ->
                  val selected = currentTab == tab
                  NavigationBarItem(
                    selected = selected,
                    onClick = {
                      if (tab == IgNavTab.CREATE) createInitialMode = CreateMode.POST
                      currentTab = tab
                    },
                    icon = {
                      Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = stringResource(tab.labelRes),
                        tint =
                          if (currentTab == IgNavTab.REELS) {
                            if (selected) Color.White else Color.White.copy(alpha = 0.6f)
                          } else {
                            if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                          },
                      )
                    },
                    modifier = Modifier.testTag(tab.testTag),
                  )
                }
              }
            }
          },
        ) { innerPadding ->
          val contentModifier =
            if (currentTab == IgNavTab.REELS) {
              Modifier.fillMaxSize().padding(innerPadding)
            } else {
              Modifier.fillMaxSize().padding(innerPadding)
            }

          Box(modifier = contentModifier, contentAlignment = Alignment.TopCenter) {
            Box(
              modifier =
                if (currentTab == IgNavTab.REELS) Modifier.fillMaxSize().widthIn(max = 520.dp)
                else Modifier.fillMaxSize().widthIn(max = 640.dp)
            ) {
              when (currentTab) {
                IgNavTab.HOME -> {
                  HomeFeedScreen(
                    currentUser = currentUser,
                    postsState = postsState,
                    storiesState = storiesState,
                    likedPostIds = likedPostIds,
                    bookmarkedPostIds = bookmarkedPostIds,
                    viewedStoryIds = viewedStoryIds,
                    onOpenStory = { authorId -> activeStoryAuthorId = authorId },
                    onAddStoryClick = {
                      createInitialMode = CreateMode.STORY
                      currentTab = IgNavTab.CREATE
                    },
                    onCreatePostClick = {
                      createInitialMode = CreateMode.POST
                      currentTab = IgNavTab.CREATE
                    },
                    onAuthorClick = { uid ->
                      if (uid == currentUser.userId) currentTab = IgNavTab.PROFILE
                      else viewingUserId = uid
                    },
                    onLikeToggle = { post, liked -> viewModel.toggleLikePost(post, liked, currentUser) },
                    onDoubleTapLike = { post ->
                      if (post.id !in likedPostIds) viewModel.toggleLikePost(post, false, currentUser)
                    },
                    onBookmarkToggle = { post, saved -> viewModel.toggleBookmarkPost(post.id, saved) },
                    onOpenComments = { post ->
                      viewModel.openComments(
                        CommentTarget(
                          parentCollection = "ig_posts",
                          parentId = post.id,
                          authorId = post.authorId,
                          thumbnailUrl = post.mediaUrls.firstOrNull(),
                        )
                      )
                    },
                    onSharePost = { post -> sharingPost = post },
                    onEditPost = { post -> editingPost = post },
                    onDeletePost = { post -> viewModel.deletePost(post, currentUser) },
                  )
                }
                IgNavTab.EXPLORE -> {
                  ExploreScreen(
                    currentUser = currentUser,
                    usersState = usersState,
                    postsState = postsState,
                    myFollowingIds = myFollowingIds,
                    searchQuery = exploreQuery,
                    selectedCategory = exploreCategory,
                    onSearchChange = viewModel::setExploreQuery,
                    onCategorySelect = viewModel::setExploreCategory,
                    onUserClick = { uid ->
                      if (uid == currentUser.userId) currentTab = IgNavTab.PROFILE
                      else viewingUserId = uid
                    },
                    onToggleFollow = { target, following -> viewModel.toggleFollow(currentUser, target, following) },
                    onPostClick = { post -> detailPostId = post.id },
                  )
                }
                IgNavTab.CREATE -> {
                  CreateScreen(
                    initialMode = createInitialMode,
                    isSubmitting = isSubmitting,
                    onPublishPost = { mediaUrls, filter, caption, location, category ->
                      viewModel.createPost(currentUser, mediaUrls, filter, caption, location, category) {
                        currentTab = IgNavTab.HOME
                      }
                    },
                    onPublishStory = { mediaUrl, overlayText, filter ->
                      viewModel.createStory(currentUser, mediaUrl, overlayText, filter) {
                        currentTab = IgNavTab.HOME
                      }
                    },
                    onPublishReel = { mediaUrl, caption, audioTrack, filter ->
                      viewModel.createReel(currentUser, mediaUrl, caption, audioTrack, filter) {
                        currentTab = IgNavTab.REELS
                      }
                    },
                  )
                }
                IgNavTab.REELS -> {
                  ReelsScreen(
                    currentUser = currentUser,
                    reelsState = reelsState,
                    likedReelIds = likedReelIds,
                    myFollowingIds = myFollowingIds,
                    allUsers = allUsers,
                    onLikeReel = { reel, liked -> viewModel.toggleLikeReel(reel, liked) },
                    onOpenComments = { reel ->
                      viewModel.openComments(
                        CommentTarget(
                          parentCollection = "ig_reels",
                          parentId = reel.id,
                          authorId = reel.authorId,
                          thumbnailUrl = reel.mediaUrl,
                        )
                      )
                    },
                    onToggleFollow = { target, following -> viewModel.toggleFollow(currentUser, target, following) },
                    onAuthorClick = { uid ->
                      if (uid == currentUser.userId) currentTab = IgNavTab.PROFILE
                      else viewingUserId = uid
                    },
                    onDeleteReel = { reel -> viewModel.deleteReel(reel.id) },
                    onCreateReelClick = {
                      createInitialMode = CreateMode.REEL
                      currentTab = IgNavTab.CREATE
                    },
                  )
                }
                IgNavTab.PROFILE -> {
                  ProfileScreen(
                    profileUser = currentUser,
                    isSelf = true,
                    isFollowing = false,
                    allPosts = allPosts,
                    userStories = allStories.filter { it.authorId == currentUser.userId },
                    bookmarkedPostIds = bookmarkedPostIds,
                    likedPostIds = likedPostIds,
                    onEditProfileClick = { showEditProfileDialog = true },
                    onToggleFollow = {},
                    onMessageClick = {},
                    onOpenStory = { activeStoryAuthorId = currentUser.userId },
                    onAddStoryClick = {
                      createInitialMode = CreateMode.STORY
                      currentTab = IgNavTab.CREATE
                    },
                    onOpenFollowList = { mode ->
                      viewModel.openFollowList(FollowListTarget(currentUser.userId, currentUser.username, mode))
                    },
                    onOpenPost = { post -> detailPostId = post.id },
                    onSignOutClick = { signOut(credentialManager = credentialManager, scope = scope) },
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  // Global Modal Sheets & Dialogs
  if (commentTarget != null) {
    CommentsBottomSheet(
      currentUser = currentUser,
      commentsState = commentsState,
      isSubmitting = isSubmitting,
      onDismiss = { viewModel.openComments(null) },
      onSubmitComment = { body, onDone -> viewModel.addComment(currentUser, body, onDone) },
      onDeleteComment = viewModel::deleteComment,
      onAuthorClick = { uid ->
        viewModel.openComments(null)
        viewingUserId = uid
      },
    )
  }

  val postToShare = sharingPost
  if (postToShare != null) {
    SharePostSheet(
      currentUser = currentUser,
      allUsers = allUsers,
      post = postToShare,
      onDismiss = { sharingPost = null },
      onSendToUser = { recipient, note ->
        viewModel.sendMessage(currentUser, recipient, note, sharedPost = postToShare) {
          sharingPost = null
        }
      },
    )
  }

  val fTarget = followListTarget
  if (fTarget != null) {
    FollowListBottomSheet(
      target = fTarget,
      listState = followListState,
      onDismiss = { viewModel.openFollowList(null) },
      onSelectUser = { uid ->
        viewModel.openFollowList(null)
        if (uid == currentUser.userId) {
          viewingUserId = null
          currentTab = IgNavTab.PROFILE
        } else {
          viewingUserId = uid
        }
      },
    )
  }

  if (showEditProfileDialog) {
    EditProfileDialog(
      user = currentUser,
      isSubmitting = isSubmitting,
      onDismiss = { showEditProfileDialog = false },
      onSave = { username, displayName, bio, website, avatarUrl ->
        viewModel.updateProfile(username, displayName, bio, website, avatarUrl) {
          showEditProfileDialog = false
        }
      },
    )
  }

  val postToEdit = editingPost
  if (postToEdit != null) {
    EditPostDialog(
      post = postToEdit,
      isSubmitting = isSubmitting,
      onDismiss = { editingPost = null },
      onSave = { caption, location, category ->
        viewModel.editPost(postToEdit.id, caption, location, category) {
          editingPost = null
        }
      },
    )
  }
}
