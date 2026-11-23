package com.example.data

import android.content.Context
import com.example.R
import com.example.model.APP_SCOPE
import com.example.model.FollowEdge
import com.example.model.IgChat
import com.example.model.IgComment
import com.example.model.IgMessage
import com.example.model.IgNotification
import com.example.model.IgPost
import com.example.model.IgReel
import com.example.model.IgStory
import com.example.model.IgUser
import com.example.model.NotificationType
import com.example.model.PhotoFilter
import com.example.model.PostCategory
import com.example.model.UserTargetMarker
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class InstagramRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Vistagram.")

  // --- REAL-TIME STREAMS ---

  fun observeUsers(): Flow<List<IgUser>> = flow {
    val uid = requireUserId()
    val path = "ig_users"
    emitAll(
      db.collection(path)
        .whereEqualTo("appScope", APP_SCOPE)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(IgUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.username.lowercase() }
        }
        .catch { error -> logAndRethrowStreamError(error, OperationType.LIST, path, uid) }
    )
  }

  fun observePosts(): Flow<List<IgPost>> = flow {
    val uid = requireUserId()
    val path = "ig_posts"
    emitAll(
      db.collection(path)
        .whereEqualTo("appScope", APP_SCOPE)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(IgPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error -> logAndRethrowStreamError(error, OperationType.LIST, path, uid) }
    )
  }

  fun observeStories(): Flow<List<IgStory>> = flow {
    val uid = requireUserId()
    val path = "ig_stories"
    emitAll(
      db.collection(path)
        .whereEqualTo("appScope", APP_SCOPE)
        .snapshots()
        .map { snapshot ->
          val now = System.currentTimeMillis()
          snapshot
            .toObjects(IgStory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .filter { it.expiresAtMillis > now }
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error -> logAndRethrowStreamError(error, OperationType.LIST, path, uid) }
    )
  }

  fun observeReels(): Flow<List<IgReel>> = flow {
    val uid = requireUserId()
    val path = "ig_reels"
    emitAll(
      db.collection(path)
        .whereEqualTo("appScope", APP_SCOPE)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(IgReel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error -> logAndRethrowStreamError(error, OperationType.LIST, path, uid) }
    )
  }

  fun observeComments(parentCollection: String, parentId: String): Flow<List<IgComment>> = flow {
    val uid = requireUserId()
    require(parentId.isNotBlank()) { "Parent ID must not be blank" }
    val path = "$parentCollection/$parentId/comments"
    emitAll(
      db.collection(parentCollection)
        .document(parentId)
        .collection("comments")
        .whereEqualTo("appScope", APP_SCOPE)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(IgComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error -> logAndRethrowStreamError(error, OperationType.LIST, path, uid) }
    )
  }

  fun observeFollowers(userId: String): Flow<List<FollowEdge>> = flow {
    val uid = requireUserId()
    require(userId.isNotBlank()) { "User ID must not be blank" }
    val path = "ig_users/$userId/followers"
    emitAll(
      db.collection("ig_users")
        .document(userId)
        .collection("followers")
        .whereEqualTo("appScope", APP_SCOPE)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(FollowEdge::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.username.lowercase() }
        }
        .catch { error -> logAndRethrowStreamError(error, OperationType.LIST, path, uid) }
    )
  }

  fun observeFollowing(userId: String): Flow<List<FollowEdge>> = flow {
    val uid = requireUserId()
    require(userId.isNotBlank()) { "User ID must not be blank" }
    val path = "ig_users/$userId/following"
    emitAll(
      db.collection("ig_users")
        .document(userId)
        .collection("following")
        .whereEqualTo("appScope", APP_SCOPE)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(FollowEdge::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.username.lowercase() }
        }
        .catch { error -> logAndRethrowStreamError(error, OperationType.LIST, path, uid) }
    )
  }

  fun observeTargetIds(userId: String, subcollection: String): Flow<Set<String>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Signed-in user mismatch" }
    val path = "users/$userId/$subcollection"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection(subcollection)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(UserTargetMarker::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .map { it.targetId.ifBlank { it.id } }
            .toSet()
        }
        .catch { error -> logAndRethrowStreamError(error, OperationType.LIST, path, uid) }
    )
  }

  fun observeChats(userId: String): Flow<List<IgChat>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Signed-in user mismatch" }
    val path = "ig_chats"
    emitAll(
      db.collection(path)
        .whereArrayContains("participantIds", userId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(IgChat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error -> logAndRethrowStreamError(error, OperationType.LIST, path, uid) }
    )
  }

  fun observeMessages(chatId: String, userId: String): Flow<List<IgMessage>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Signed-in user mismatch" }
    require(chatId.isNotBlank()) { "Chat ID must not be blank" }
    val path = "ig_chats/$chatId/messages"
    emitAll(
      db.collection("ig_chats")
        .document(chatId)
        .collection("messages")
        .whereArrayContains("participantIds", userId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(IgMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error -> logAndRethrowStreamError(error, OperationType.LIST, path, uid) }
    )
  }

  fun observeNotifications(userId: String): Flow<List<IgNotification>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Signed-in user mismatch" }
    val path = "users/$userId/ig_notifications"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("ig_notifications")
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(IgNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error -> logAndRethrowStreamError(error, OperationType.LIST, path, uid) }
    )
  }

  private fun logAndRethrowStreamError(
    error: Throwable,
    op: OperationType,
    path: String,
    uid: String,
  ): Nothing {
    val firestoreError =
      generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
      handleFirestoreError(exceptionToLog, op, path)
    }
    throw error
  }

  // --- USER PROFILE & FOLLOW ---

  suspend fun ensureCurrentUserProfile(): Result<Unit> {
    val user = auth.currentUser ?: return Result.success(Unit)
    return ensureUserProfile(user)
  }

  private suspend fun ensureUserProfile(user: FirebaseUser): Result<Unit> {
    val uid = user.uid
    val docPath = "ig_users/$uid"
    return try {
      val docRef = db.collection("ig_users").document(uid)
      if (docRef.get().await().exists()) return Result.success(Unit)

      val rawName = user.displayName?.trim().orEmpty().ifBlank { "Creator" }
      val safeDisplayName = rawName.take(60).let { if (it.length < 2) "Creator" else it }
      val sanitizedHandle =
        rawName.lowercase().replace(Regex("[^a-z0-9_.]"), "").take(22).let {
          if (it.length < 2) "user_${uid.take(6).lowercase()}" else it
        }
      val photo = user.photoUrl?.toString()?.takeIf { it.isNotBlank() && it.length <= 200000 }

      val payload =
        mapOf(
          "appScope" to APP_SCOPE,
          "userId" to uid,
          "username" to sanitizedHandle,
          "displayName" to safeDisplayName,
          "bio" to "Visual storyteller on Vistagram",
          "website" to "",
          "avatarUrl" to photo,
          "verified" to false,
          "postsCount" to 0L,
          "followersCount" to 0L,
          "followingCount" to 0L,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }

      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateProfile(
    username: String,
    displayName: String,
    bio: String,
    website: String,
    avatarUrl: String?,
  ): Result<Unit> {
    var docPath = "ig_users/${auth.currentUser?.uid ?: "unauthenticated"}"
    return try {
      val uid = requireUserId()
      val cleanUsername = username.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "")
      val cleanDisplayName = displayName.trim()
      val cleanBio = bio.trim()
      val cleanWebsite = website.trim()

      require(cleanUsername.length in 2..30) { "Username must be 2 to 30 characters" }
      require(cleanDisplayName.length in 2..60) { "Name must be 2 to 60 characters" }
      require(cleanBio.length <= 300) { "Bio must be at most 300 characters" }
      require(cleanWebsite.length <= 120) { "Website must be at most 120 characters" }

      val docRef = db.collection("ig_users").document(uid)
      docPath = docRef.path
      val updates =
        mutableMapOf<String, Any>(
          "username" to cleanUsername,
          "displayName" to cleanDisplayName,
          "bio" to cleanBio,
          "website" to cleanWebsite,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (!avatarUrl.isNullOrBlank() && avatarUrl.length <= 200000) {
        updates["avatarUrl"] = avatarUrl
      }
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun toggleFollowUser(
    currentUser: IgUser,
    targetUser: IgUser,
    currentlyFollowing: Boolean,
  ): Result<Unit> {
    val docPath = "ig_users/${targetUser.userId}"
    return try {
      val uid = requireUserId()
      check(uid == currentUser.userId) { "User mismatch" }
      require(targetUser.userId.isNotBlank() && targetUser.userId != uid) { "Cannot follow self" }

      val targetUserRef = db.collection("ig_users").document(targetUser.userId)
      val myUserRef = db.collection("ig_users").document(uid)
      val followerRef = targetUserRef.collection("followers").document(uid)
      val followingRef = myUserRef.collection("following").document(targetUser.userId)

      val batch = db.batch()
      if (currentlyFollowing) {
        batch.delete(followerRef)
        batch.delete(followingRef)
        batch.update(
          targetUserRef,
          mapOf("followersCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp()),
        )
        if (currentUser.followingCount > 0L) {
          batch.update(
            myUserRef,
            mapOf("followingCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp()),
          )
        }
      } else {
        val followerPayload =
          mapOf(
            "appScope" to APP_SCOPE,
            "userId" to uid,
            "targetUserId" to targetUser.userId,
            "username" to currentUser.username.ifBlank { "creator" }.take(30),
            "displayName" to currentUser.displayName.ifBlank { "Creator" }.take(60),
            "avatarUrl" to currentUser.avatarUrl?.takeIf { it.isNotBlank() && it.length <= 200000 },
            "createdAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }

        val followingPayload =
          mapOf(
            "appScope" to APP_SCOPE,
            "userId" to uid,
            "targetUserId" to targetUser.userId,
            "username" to targetUser.username.ifBlank { "creator" }.take(30),
            "displayName" to targetUser.displayName.ifBlank { "Creator" }.take(60),
            "avatarUrl" to targetUser.avatarUrl?.takeIf { it.isNotBlank() && it.length <= 200000 },
            "createdAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }

        batch.set(followerRef, followerPayload)
        batch.set(followingRef, followingPayload)
        batch.update(
          targetUserRef,
          mapOf("followersCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp()),
        )
        batch.update(
          myUserRef,
          mapOf("followingCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp()),
        )
      }
      batch.commit().await()

      if (!currentlyFollowing) {
        sendNotificationQuietly(
          targetUserId = targetUser.userId,
          actor = currentUser,
          type = NotificationType.FOLLOW,
          postId = null,
          postThumbnail = null,
          previewText = "started following you",
        )
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  // --- POSTS ---

  suspend fun createPost(
    author: IgUser,
    mediaUrls: List<String>,
    filter: PhotoFilter,
    caption: String,
    location: String,
    category: PostCategory,
  ): Result<Unit> {
    var docPath = "ig_posts"
    return try {
      val uid = requireUserId()
      check(uid == author.userId) { "User mismatch" }
      val cleanMedia = mediaUrls.filter { it.isNotBlank() && it.length <= 200000 }.take(5)
      require(cleanMedia.isNotEmpty()) { "Select at least one photo" }

      val cleanCaption = caption.trim().take(2200)
      val cleanLocation = location.trim().take(100)
      val postRef = db.collection("ig_posts").document()
      docPath = postRef.path
      val userRef = db.collection("ig_users").document(uid)

      val payload =
        mapOf(
          "appScope" to APP_SCOPE,
          "authorId" to uid,
          "authorUsername" to author.username.ifBlank { "creator" }.take(30),
          "authorDisplayName" to author.displayName.ifBlank { "Creator" }.take(60),
          "authorAvatarUrl" to author.avatarUrl?.takeIf { it.isNotBlank() && it.length <= 200000 },
          "authorVerified" to author.verified,
          "mediaUrls" to cleanMedia,
          "filterName" to filter.name,
          "caption" to cleanCaption,
          "location" to cleanLocation,
          "category" to category.name,
          "likesCount" to 0L,
          "commentsCount" to 0L,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }

      val batch = db.batch()
      batch.set(postRef, payload)
      batch.update(
        userRef,
        mapOf("postsCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp()),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun editPost(postId: String, caption: String, location: String, category: PostCategory): Result<Unit> {
    val docPath = "ig_posts/$postId"
    return try {
      requireUserId()
      require(postId.isNotBlank()) { "Post ID required" }
      db.collection("ig_posts")
        .document(postId)
        .update(
          mapOf(
            "caption" to caption.trim().take(2200),
            "location" to location.trim().take(100),
            "category" to category.name,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deletePost(postId: String, currentPostsCount: Long): Result<Unit> {
    val docPath = "ig_posts/$postId"
    return try {
      val uid = requireUserId()
      require(postId.isNotBlank()) { "Post ID required" }
      val postRef = db.collection("ig_posts").document(postId)
      val userRef = db.collection("ig_users").document(uid)
      val batch = db.batch()
      batch.delete(postRef)
      if (currentPostsCount > 0L) {
        batch.update(
          userRef,
          mapOf("postsCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp()),
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  suspend fun toggleLikePost(post: IgPost, currentlyLiked: Boolean, actor: IgUser): Result<Unit> {
    val docPath = "ig_posts/${post.id}"
    return try {
      val uid = requireUserId()
      require(post.id.isNotBlank()) { "Post ID required" }
      val postRef = db.collection("ig_posts").document(post.id)
      val likeRef = db.collection("users").document(uid).collection("ig_liked_posts").document(post.id)
      val batch = db.batch()
      if (currentlyLiked) {
        batch.delete(likeRef)
        batch.update(
          postRef,
          mapOf("likesCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp()),
        )
      } else {
        batch.set(
          likeRef,
          mapOf("userId" to uid, "targetId" to post.id, "createdAt" to FieldValue.serverTimestamp()),
        )
        batch.update(
          postRef,
          mapOf("likesCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp()),
        )
      }
      batch.commit().await()

      if (!currentlyLiked && post.authorId != uid) {
        sendNotificationQuietly(
          targetUserId = post.authorId,
          actor = actor,
          type = NotificationType.LIKE,
          postId = post.id,
          postThumbnail = post.mediaUrls.firstOrNull(),
          previewText = "liked your photo",
        )
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  suspend fun toggleBookmarkPost(postId: String, currentlyBookmarked: Boolean): Result<Unit> {
    var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/ig_bookmarks/$postId"
    return try {
      val uid = requireUserId()
      require(postId.isNotBlank()) { "Post ID required" }
      val ref = db.collection("users").document(uid).collection("ig_bookmarks").document(postId)
      docPath = ref.path
      if (currentlyBookmarked) {
        ref.delete().await()
      } else {
        ref.set(mapOf("userId" to uid, "targetId" to postId, "createdAt" to FieldValue.serverTimestamp())).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  // --- COMMENTS ---

  suspend fun addComment(
    parentCollection: String,
    parentId: String,
    parentAuthorId: String,
    parentThumbnail: String?,
    actor: IgUser,
    body: String,
  ): Result<Unit> {
    var docPath = "$parentCollection/$parentId/comments"
    return try {
      val uid = requireUserId()
      val cleanBody = body.trim()
      require(parentId.isNotBlank()) { "Target ID required" }
      require(cleanBody.length in 1..1000) { "Comment must be 1 to 1000 characters" }

      val parentRef = db.collection(parentCollection).document(parentId)
      val commentRef = parentRef.collection("comments").document()
      docPath = commentRef.path

      val payload =
        mapOf(
          "appScope" to APP_SCOPE,
          "parentId" to parentId,
          "authorId" to uid,
          "authorUsername" to actor.username.ifBlank { "creator" }.take(30),
          "authorAvatarUrl" to actor.avatarUrl?.takeIf { it.isNotBlank() && it.length <= 200000 },
          "body" to cleanBody,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }

      val batch = db.batch()
      batch.set(commentRef, payload)
      batch.update(
        parentRef,
        mapOf("commentsCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp()),
      )
      batch.commit().await()

      if (parentAuthorId != uid) {
        sendNotificationQuietly(
          targetUserId = parentAuthorId,
          actor = actor,
          type = NotificationType.COMMENT,
          postId = if (parentCollection == "ig_posts") parentId else null,
          postThumbnail = parentThumbnail,
          previewText = "commented: ${cleanBody.take(80)}",
        )
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteComment(parentCollection: String, parentId: String, commentId: String): Result<Unit> {
    val docPath = "$parentCollection/$parentId/comments/$commentId"
    return try {
      requireUserId()
      require(parentId.isNotBlank() && commentId.isNotBlank()) { "IDs required" }
      val parentRef = db.collection(parentCollection).document(parentId)
      val commentRef = parentRef.collection("comments").document(commentId)
      val batch = db.batch()
      batch.delete(commentRef)
      batch.update(
        parentRef,
        mapOf("commentsCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp()),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  // --- STORIES ---

  suspend fun createStory(
    author: IgUser,
    mediaUrl: String,
    overlayText: String,
    filter: PhotoFilter,
  ): Result<Unit> {
    var docPath = "ig_stories"
    return try {
      val uid = requireUserId()
      require(mediaUrl.isNotBlank() && mediaUrl.length <= 200000) { "Valid story media required" }
      val ref = db.collection("ig_stories").document()
      docPath = ref.path
      val payload =
        mapOf(
          "appScope" to APP_SCOPE,
          "authorId" to uid,
          "authorUsername" to author.username.ifBlank { "creator" }.take(30),
          "authorAvatarUrl" to author.avatarUrl?.takeIf { it.isNotBlank() && it.length <= 200000 },
          "mediaUrl" to mediaUrl,
          "overlayText" to overlayText.trim().take(200),
          "filterName" to filter.name,
          "expiresAtMillis" to (System.currentTimeMillis() + 86_400_000L),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }
      ref.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteStory(storyId: String): Result<Unit> {
    val docPath = "ig_stories/$storyId"
    return try {
      requireUserId()
      db.collection("ig_stories").document(storyId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  suspend fun markStoryViewed(storyId: String): Result<Unit> {
    var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/ig_viewed_stories/$storyId"
    return try {
      val uid = requireUserId()
      if (storyId.isBlank()) return Result.success(Unit)
      val ref = db.collection("users").document(uid).collection("ig_viewed_stories").document(storyId)
      docPath = ref.path
      if (!ref.get().await().exists()) {
        ref.set(mapOf("userId" to uid, "targetId" to storyId, "createdAt" to FieldValue.serverTimestamp())).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  // --- REELS ---

  suspend fun createReel(
    author: IgUser,
    mediaUrl: String,
    caption: String,
    audioTrack: String,
    filter: PhotoFilter,
  ): Result<Unit> {
    var docPath = "ig_reels"
    return try {
      val uid = requireUserId()
      require(mediaUrl.isNotBlank() && mediaUrl.length <= 200000) { "Select visual media for reel" }
      val cleanAudio = audioTrack.trim().ifBlank { "Original Audio - ${author.username}" }.take(100)
      val ref = db.collection("ig_reels").document()
      docPath = ref.path
      val payload =
        mapOf(
          "appScope" to APP_SCOPE,
          "authorId" to uid,
          "authorUsername" to author.username.ifBlank { "creator" }.take(30),
          "authorAvatarUrl" to author.avatarUrl?.takeIf { it.isNotBlank() && it.length <= 200000 },
          "mediaUrl" to mediaUrl,
          "caption" to caption.trim().take(500),
          "audioTrack" to cleanAudio,
          "filterName" to filter.name,
          "likesCount" to 0L,
          "commentsCount" to 0L,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }
      ref.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun toggleLikeReel(reel: IgReel, currentlyLiked: Boolean): Result<Unit> {
    val docPath = "ig_reels/${reel.id}"
    return try {
      val uid = requireUserId()
      val reelRef = db.collection("ig_reels").document(reel.id)
      val likeRef = db.collection("users").document(uid).collection("ig_liked_reels").document(reel.id)
      val batch = db.batch()
      if (currentlyLiked) {
        batch.delete(likeRef)
        batch.update(
          reelRef,
          mapOf("likesCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp()),
        )
      } else {
        batch.set(
          likeRef,
          mapOf("userId" to uid, "targetId" to reel.id, "createdAt" to FieldValue.serverTimestamp()),
        )
        batch.update(
          reelRef,
          mapOf("likesCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp()),
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteReel(reelId: String): Result<Unit> {
    val docPath = "ig_reels/$reelId"
    return try {
      requireUserId()
      db.collection("ig_reels").document(reelId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  // --- DIRECT MESSAGES ---

  fun deterministicChatId(uid1: String, uid2: String): String =
    listOf(uid1, uid2).sorted().joinToString("_")

  suspend fun sendMessage(
    sender: IgUser,
    recipient: IgUser,
    text: String,
    sharedPost: IgPost? = null,
  ): Result<Unit> {
    val chatId = deterministicChatId(sender.userId, recipient.userId)
    val docPath = "ig_chats/$chatId"
    return try {
      val uid = requireUserId()
      check(uid == sender.userId) { "Sender mismatch" }
      val cleanText = text.trim().ifBlank {
        if (sharedPost != null) "Shared a post by @${sharedPost.authorUsername}" else ""
      }.take(1000)
      require(cleanText.isNotEmpty()) { "Message cannot be empty" }

      val sortedIds = listOf(sender.userId, recipient.userId).sorted()
      val userA = if (sortedIds[0] == sender.userId) sender else recipient
      val userB = if (sortedIds[1] == sender.userId) sender else recipient

      val chatRef = db.collection("ig_chats").document(chatId)
      val msgRef = chatRef.collection("messages").document()
      val chatExists = chatRef.get().await().exists()

      val batch = db.batch()
      if (chatExists) {
        batch.update(
          chatRef,
          mapOf(
            "lastMessageText" to cleanText.take(300),
            "lastSenderId" to uid,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      } else {
        val chatPayload =
          mapOf(
            "participantIds" to sortedIds,
            "participantAId" to userA.userId,
            "participantAName" to userA.username.ifBlank { userA.displayName }.take(60),
            "participantAAvatar" to userA.avatarUrl?.takeIf { it.isNotBlank() && it.length <= 200000 },
            "participantBId" to userB.userId,
            "participantBName" to userB.username.ifBlank { userB.displayName }.take(60),
            "participantBAvatar" to userB.avatarUrl?.takeIf { it.isNotBlank() && it.length <= 200000 },
            "lastMessageText" to cleanText.take(300),
            "lastSenderId" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        batch.set(chatRef, chatPayload)
      }

      val msgPayload =
        mapOf(
          "chatId" to chatId,
          "participantIds" to sortedIds,
          "senderId" to uid,
          "senderUsername" to sender.username.ifBlank { "creator" }.take(30),
          "text" to cleanText,
          "sharedPostId" to sharedPost?.id?.takeIf { it.isNotBlank() },
          "sharedPostImage" to sharedPost?.mediaUrls?.firstOrNull()?.takeIf { it.isNotBlank() && it.length <= 200000 },
          "sharedPostAuthor" to sharedPost?.authorUsername?.take(60),
          "createdAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }

      batch.set(msgRef, msgPayload)
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  // --- NOTIFICATIONS ---

  suspend fun sendStoryReplyNotification(
    story: IgStory,
    actor: IgUser,
    replyText: String,
  ) {
    if (story.authorId == actor.userId) return
    sendNotificationQuietly(
      targetUserId = story.authorId,
      actor = actor,
      type = NotificationType.STORY_REPLY,
      postId = null,
      postThumbnail = story.mediaUrl,
      previewText = "replied to your story: ${replyText.take(80)}",
    )
  }

  suspend fun markNotificationsRead(userId: String, unreadIds: List<String>): Result<Unit> {
    if (unreadIds.isEmpty()) return Result.success(Unit)
    val docPath = "users/$userId/ig_notifications"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "User mismatch" }
      val batch = db.batch()
      unreadIds.take(50).forEach { nid ->
        val ref = db.collection("users").document(uid).collection("ig_notifications").document(nid)
        batch.update(ref, mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp()))
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  private suspend fun sendNotificationQuietly(
    targetUserId: String,
    actor: IgUser,
    type: NotificationType,
    postId: String?,
    postThumbnail: String?,
    previewText: String,
  ) {
    if (targetUserId.isBlank() || targetUserId == actor.userId) return
    val docRef = db.collection("users").document(targetUserId).collection("ig_notifications").document()
    try {
      val payload =
        mapOf(
          "targetUserId" to targetUserId,
          "actorId" to actor.userId,
          "actorUsername" to actor.username.ifBlank { "creator" }.take(30),
          "actorAvatarUrl" to actor.avatarUrl?.takeIf { it.isNotBlank() && it.length <= 200000 },
          "type" to type.name,
          "postId" to postId?.takeIf { it.isNotBlank() },
          "postThumbnail" to postThumbnail?.takeIf { it.isNotBlank() && it.length <= 200000 },
          "previewText" to previewText.take(200),
          "read" to false,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
    }
  }
}
