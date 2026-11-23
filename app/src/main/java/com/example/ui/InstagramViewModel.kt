package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.InstagramRepository
import com.example.model.FollowEdge
import com.example.model.IgChat
import com.example.model.IgComment
import com.example.model.IgMessage
import com.example.model.IgNotification
import com.example.model.IgPost
import com.example.model.IgReel
import com.example.model.IgStory
import com.example.model.IgUser
import com.example.model.PhotoFilter
import com.example.model.PostCategory
import com.example.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CommentTarget(
  val parentCollection: String,
  val parentId: String,
  val authorId: String,
  val thumbnailUrl: String?,
)

enum class FollowListMode {
  FOLLOWERS,
  FOLLOWING,
}

data class FollowListTarget(
  val userId: String,
  val username: String,
  val mode: FollowListMode,
)

@OptIn(ExperimentalCoroutinesApi::class)
class InstagramViewModel(
  private val repository: InstagramRepository,
  val currentUserId: String,
  val fallbackDisplayName: String,
  val fallbackPhotoUrl: String?,
) : ViewModel() {

  private val _exploreQuery = MutableStateFlow("")
  val exploreQuery: StateFlow<String> = _exploreQuery.asStateFlow()

  private val _exploreCategory = MutableStateFlow<PostCategory?>(null)
  val exploreCategory: StateFlow<PostCategory?> = _exploreCategory.asStateFlow()

  private val _commentTarget = MutableStateFlow<CommentTarget?>(null)
  val commentTarget: StateFlow<CommentTarget?> = _commentTarget.asStateFlow()

  private val _followListTarget = MutableStateFlow<FollowListTarget?>(null)
  val followListTarget: StateFlow<FollowListTarget?> = _followListTarget.asStateFlow()

  private val _activeChatPartnerId = MutableStateFlow<String?>(null)
  val activeChatPartnerId: StateFlow<String?> = _activeChatPartnerId.asStateFlow()

  private val _isSubmitting = MutableStateFlow(false)
  val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

  private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val snackbarMessages = _snackbarMessages.asSharedFlow()

  init {
    viewModelScope.launch {
      repository.ensureCurrentUserProfile().onFailure { e ->
        Log.w(TAG, "Could not ensure user profile: ${e.message}")
      }
    }
  }

  val usersState: StateFlow<UiState<List<IgUser>>> =
    repository
      .observeUsers()
      .toUiState("Failed to load creators")
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  val postsState: StateFlow<UiState<List<IgPost>>> =
    repository
      .observePosts()
      .toUiState("Failed to load feed")
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  val storiesState: StateFlow<UiState<List<IgStory>>> =
    repository
      .observeStories()
      .toUiState("Failed to load stories")
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  val reelsState: StateFlow<UiState<List<IgReel>>> =
    repository
      .observeReels()
      .toUiState("Failed to load reels")
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  val chatsState: StateFlow<UiState<List<IgChat>>> =
    repository
      .observeChats(currentUserId)
      .toUiState("Failed to load messages")
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  val notificationsState: StateFlow<UiState<List<IgNotification>>> =
    repository
      .observeNotifications(currentUserId)
      .toUiState("Failed to load activity")
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  val likedPostIds: StateFlow<Set<String>> =
    repository
      .observeTargetIds(currentUserId, "ig_liked_posts")
      .catch { e -> handleNonFatalSetStreamError(e, "ig_liked_posts"); emit(emptySet()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

  val bookmarkedPostIds: StateFlow<Set<String>> =
    repository
      .observeTargetIds(currentUserId, "ig_bookmarks")
      .catch { e -> handleNonFatalSetStreamError(e, "ig_bookmarks"); emit(emptySet()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

  val likedReelIds: StateFlow<Set<String>> =
    repository
      .observeTargetIds(currentUserId, "ig_liked_reels")
      .catch { e -> handleNonFatalSetStreamError(e, "ig_liked_reels"); emit(emptySet()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

  val viewedStoryIds: StateFlow<Set<String>> =
    repository
      .observeTargetIds(currentUserId, "ig_viewed_stories")
      .catch { e -> handleNonFatalSetStreamError(e, "ig_viewed_stories"); emit(emptySet()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

  val myFollowingIds: StateFlow<Set<String>> =
    repository
      .observeFollowing(currentUserId)
      .map { edges -> edges.map { it.targetUserId }.toSet() }
      .catch { e -> handleNonFatalSetStreamError(e, "following"); emit(emptySet()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

  val commentsState: StateFlow<UiState<List<IgComment>>> =
    _commentTarget
      .flatMapLatest { target ->
        if (target == null) {
          flowOf(UiState.Success(emptyList()))
        } else {
          repository.observeComments(target.parentCollection, target.parentId)
            .toUiState("Failed to load comments")
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  val followListState: StateFlow<UiState<List<FollowEdge>>> =
    _followListTarget
      .flatMapLatest { target ->
        if (target == null) {
          flowOf(UiState.Success(emptyList()))
        } else {
          val flow = when (target.mode) {
            FollowListMode.FOLLOWERS -> repository.observeFollowers(target.userId)
            FollowListMode.FOLLOWING -> repository.observeFollowing(target.userId)
          }
          flow.toUiState("Failed to load list")
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  val activeChatMessagesState: StateFlow<UiState<List<IgMessage>>> =
    _activeChatPartnerId
      .flatMapLatest { partnerId ->
        if (partnerId.isNullOrBlank()) {
          flowOf(UiState.Success(emptyList()))
        } else {
          val chatId = repository.deterministicChatId(currentUserId, partnerId)
          repository.observeMessages(chatId, currentUserId).toUiState("Failed to load chat")
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  fun fallbackUser(): IgUser {
    val cleanHandle =
      fallbackDisplayName.lowercase().replace(Regex("[^a-z0-9_.]"), "").take(22).let {
        if (it.length < 2) "user_${currentUserId.take(6).lowercase()}" else it
      }
    return IgUser(
      id = currentUserId,
      userId = currentUserId,
      username = cleanHandle,
      displayName = fallbackDisplayName,
      avatarUrl = fallbackPhotoUrl,
    )
  }

  fun currentUser(users: List<IgUser>): IgUser =
    users.firstOrNull { it.userId == currentUserId || it.id == currentUserId } ?: fallbackUser()

  fun setExploreQuery(query: String) {
    _exploreQuery.value = query
  }

  fun setExploreCategory(category: PostCategory?) {
    _exploreCategory.value = category
  }

  fun openComments(target: CommentTarget?) {
    _commentTarget.value = target
  }

  fun openFollowList(target: FollowListTarget?) {
    _followListTarget.value = target
  }

  fun openChat(partnerId: String?) {
    _activeChatPartnerId.value = partnerId
  }

  fun updateProfile(
    username: String,
    displayName: String,
    bio: String,
    website: String,
    avatarUrl: String?,
    onSuccess: () -> Unit,
  ) {
    runMutation("Profile updated") {
      repository.updateProfile(username, displayName, bio, website, avatarUrl).also {
        if (it.isSuccess) onSuccess()
      }
    }
  }

  fun toggleFollow(currentUser: IgUser, targetUser: IgUser, currentlyFollowing: Boolean) {
    viewModelScope.launch {
      repository.toggleFollowUser(currentUser, targetUser, currentlyFollowing).onFailure {
        _snackbarMessages.tryEmit(it.localizedMessage ?: "Couldn't update follow status")
      }
    }
  }

  fun createPost(
    author: IgUser,
    mediaUrls: List<String>,
    filter: PhotoFilter,
    caption: String,
    location: String,
    category: PostCategory,
    onSuccess: () -> Unit,
  ) {
    runMutation("Post shared to feed!") {
      repository.createPost(author, mediaUrls, filter, caption, location, category).also {
        if (it.isSuccess) onSuccess()
      }
    }
  }

  fun editPost(postId: String, caption: String, location: String, category: PostCategory, onSuccess: () -> Unit) {
    runMutation("Post updated") {
      repository.editPost(postId, caption, location, category).also {
        if (it.isSuccess) onSuccess()
      }
    }
  }

  fun deletePost(post: IgPost, currentUser: IgUser) {
    runMutation("Post deleted") {
      repository.deletePost(post.id, currentUser.postsCount)
    }
  }

  fun toggleLikePost(post: IgPost, currentlyLiked: Boolean, actor: IgUser) {
    viewModelScope.launch {
      repository.toggleLikePost(post, currentlyLiked, actor).onFailure {
        _snackbarMessages.tryEmit(it.localizedMessage ?: "Couldn't update like")
      }
    }
  }

  fun toggleBookmarkPost(postId: String, currentlyBookmarked: Boolean) {
    viewModelScope.launch {
      repository.toggleBookmarkPost(postId, currentlyBookmarked).onSuccess {
        _snackbarMessages.tryEmit(if (currentlyBookmarked) "Removed from Saved" else "Saved to collection")
      }.onFailure {
        _snackbarMessages.tryEmit(it.localizedMessage ?: "Couldn't update bookmark")
      }
    }
  }

  fun addComment(actor: IgUser, body: String, onSuccess: () -> Unit) {
    val target = _commentTarget.value ?: return
    runMutation("Comment posted") {
      repository.addComment(
        parentCollection = target.parentCollection,
        parentId = target.parentId,
        parentAuthorId = target.authorId,
        parentThumbnail = target.thumbnailUrl,
        actor = actor,
        body = body,
      ).also { if (it.isSuccess) onSuccess() }
    }
  }

  fun deleteComment(commentId: String) {
    val target = _commentTarget.value ?: return
    runMutation("Comment deleted") {
      repository.deleteComment(target.parentCollection, target.parentId, commentId)
    }
  }

  fun createStory(
    author: IgUser,
    mediaUrl: String,
    overlayText: String,
    filter: PhotoFilter,
    onSuccess: () -> Unit,
  ) {
    runMutation("Added to Your Story!") {
      repository.createStory(author, mediaUrl, overlayText, filter).also {
        if (it.isSuccess) onSuccess()
      }
    }
  }

  fun deleteStory(storyId: String, onSuccess: () -> Unit) {
    runMutation("Story deleted") {
      repository.deleteStory(storyId).also { if (it.isSuccess) onSuccess() }
    }
  }

  fun markStoryViewed(storyId: String) {
    viewModelScope.launch {
      repository.markStoryViewed(storyId)
    }
  }

  fun replyToStory(story: IgStory, sender: IgUser, recipient: IgUser?, text: String, onSuccess: () -> Unit) {
    if (recipient == null) return
    runMutation("Reply sent to @${recipient.username}") {
      val result = repository.sendMessage(sender, recipient, "Replied to story: $text")
      if (result.isSuccess) {
        repository.sendStoryReplyNotification(story, sender, text)
        onSuccess()
      }
      result
    }
  }

  fun createReel(
    author: IgUser,
    mediaUrl: String,
    caption: String,
    audioTrack: String,
    filter: PhotoFilter,
    onSuccess: () -> Unit,
  ) {
    runMutation("Reel published!") {
      repository.createReel(author, mediaUrl, caption, audioTrack, filter).also {
        if (it.isSuccess) onSuccess()
      }
    }
  }

  fun toggleLikeReel(reel: IgReel, currentlyLiked: Boolean) {
    viewModelScope.launch {
      repository.toggleLikeReel(reel, currentlyLiked).onFailure {
        _snackbarMessages.tryEmit(it.localizedMessage ?: "Couldn't like reel")
      }
    }
  }

  fun deleteReel(reelId: String) {
    runMutation("Reel deleted") { repository.deleteReel(reelId) }
  }

  fun sendMessage(
    sender: IgUser,
    recipient: IgUser,
    text: String,
    sharedPost: IgPost? = null,
    onSuccess: () -> Unit = {},
  ) {
    runMutation(if (sharedPost != null) "Sent to @${recipient.username}" else "") {
      repository.sendMessage(sender, recipient, text, sharedPost).also {
        if (it.isSuccess) onSuccess()
      }
    }
  }

  fun markNotificationsRead(notifications: List<IgNotification>) {
    val unreadIds = notifications.filter { !it.read }.map { it.id }.filter { it.isNotBlank() }
    if (unreadIds.isEmpty()) return
    viewModelScope.launch {
      repository.markNotificationsRead(currentUserId, unreadIds)
    }
  }

  private fun runMutation(successMessage: String, block: suspend () -> Result<Unit>) {
    if (_isSubmitting.value) return
    viewModelScope.launch {
      _isSubmitting.value = true
      try {
        block()
          .onSuccess { if (successMessage.isNotEmpty()) _snackbarMessages.tryEmit(successMessage) }
          .onFailure { error ->
            _snackbarMessages.tryEmit(error.localizedMessage ?: "Action failed. Please check your input.")
          }
      } finally {
        _isSubmitting.value = false
      }
    }
  }

  private fun <T> Flow<T>.toUiState(fallbackError: String): Flow<UiState<T>> =
    map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, fallbackError, rootError)
        emit(UiState.Error(rootError.localizedMessage ?: fallbackError))
      }

  private fun handleNonFatalSetStreamError(error: Throwable, streamName: String) {
    val firestoreError =
      generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    Log.w(TAG, "Error observing $streamName", firestoreError ?: error)
  }

  private companion object {
    const val TAG = "InstagramVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
