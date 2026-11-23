package com.example.model

import androidx.annotation.Keep
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val APP_SCOPE = "vistagram-main"

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

enum class PostCategory(val label: String) {
  PHOTOGRAPHY("Photography"),
  TRAVEL("Travel"),
  FOOD("Food"),
  ART("Art"),
  TECH("Tech"),
  FASHION("Fashion"),
  LIFESTYLE("Lifestyle"),
}

enum class PhotoFilter(val label: String) {
  NORMAL("Normal"),
  CLARENDON("Clarendon"),
  GINGHAM("Gingham"),
  MOON("Moon"),
  LARK("Lark"),
  JUNO("Juno"),
  WARM("Warm");

  fun toColorFilter(): ColorFilter? {
    return when (this) {
      NORMAL -> null
      CLARENDON -> {
        val m = ColorMatrix().apply { setToSaturation(1.25f) }
        ColorFilter.colorMatrix(m)
      }
      GINGHAM -> {
        val m = ColorMatrix(
          floatArrayOf(
            0.92f, 0.06f, 0.04f, 0f, 12f,
            0.03f, 0.94f, 0.05f, 0f, 8f,
            0.04f, 0.06f, 0.88f, 0f, 14f,
            0f, 0f, 0f, 1f, 0f,
          )
        )
        ColorFilter.colorMatrix(m)
      }
      MOON -> {
        val m = ColorMatrix().apply { setToSaturation(0f) }
        ColorFilter.colorMatrix(m)
      }
      LARK -> {
        val m = ColorMatrix(
          floatArrayOf(
            1.05f, 0f, 0f, 0f, 8f,
            0f, 1.08f, 0f, 0f, 6f,
            0f, 0f, 1.12f, 0f, 4f,
            0f, 0f, 0f, 1f, 0f,
          )
        )
        ColorFilter.colorMatrix(m)
      }
      JUNO -> {
        val m = ColorMatrix(
          floatArrayOf(
            1.16f, 0f, 0f, 0f, 6f,
            0f, 1.06f, 0f, 0f, 2f,
            0f, 0f, 0.92f, 0f, -4f,
            0f, 0f, 0f, 1f, 0f,
          )
        )
        ColorFilter.colorMatrix(m)
      }
      WARM -> {
        val m = ColorMatrix(
          floatArrayOf(
            1.18f, 0.04f, 0f, 0f, 10f,
            0.02f, 1.04f, 0f, 0f, 4f,
            0f, 0f, 0.84f, 0f, -6f,
            0f, 0f, 0f, 1f, 0f,
          )
        )
        ColorFilter.colorMatrix(m)
      }
    }
  }
}

enum class NotificationType(val label: String) {
  LIKE("liked your photo"),
  COMMENT("commented on your post"),
  FOLLOW("started following you"),
  STORY_REPLY("replied to your story"),
}

@Keep
@IgnoreExtraProperties
data class IgUser(
  @DocumentId val id: String = "",
  val appScope: String = APP_SCOPE,
  val userId: String = "",
  val username: String = "",
  val displayName: String = "",
  val bio: String = "",
  val website: String = "",
  val avatarUrl: String? = null,
  val verified: Boolean = false,
  val postsCount: Long = 0L,
  val followersCount: Long = 0L,
  val followingCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class FollowEdge(
  @DocumentId val id: String = "",
  val appScope: String = APP_SCOPE,
  val userId: String = "",
  val targetUserId: String = "",
  val username: String = "",
  val displayName: String = "",
  val avatarUrl: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class IgPost(
  @DocumentId val id: String = "",
  val appScope: String = APP_SCOPE,
  val authorId: String = "",
  val authorUsername: String = "",
  val authorDisplayName: String = "",
  val authorAvatarUrl: String? = null,
  val authorVerified: Boolean = false,
  val mediaUrls: List<String> = emptyList(),
  val filterName: String = PhotoFilter.NORMAL.name,
  val caption: String = "",
  val location: String = "",
  val category: String = PostCategory.PHOTOGRAPHY.name,
  val likesCount: Long = 0L,
  val commentsCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun resolvedCategory(): PostCategory =
    PostCategory.entries.firstOrNull { it.name == category } ?: PostCategory.PHOTOGRAPHY

  fun resolvedFilter(): PhotoFilter =
    PhotoFilter.entries.firstOrNull { it.name == filterName } ?: PhotoFilter.NORMAL
}

@Keep
@IgnoreExtraProperties
data class IgComment(
  @DocumentId val id: String = "",
  val appScope: String = APP_SCOPE,
  val parentId: String = "",
  val authorId: String = "",
  val authorUsername: String = "",
  val authorAvatarUrl: String? = null,
  val body: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class IgStory(
  @DocumentId val id: String = "",
  val appScope: String = APP_SCOPE,
  val authorId: String = "",
  val authorUsername: String = "",
  val authorAvatarUrl: String? = null,
  val mediaUrl: String = "",
  val overlayText: String = "",
  val filterName: String = PhotoFilter.NORMAL.name,
  val expiresAtMillis: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun resolvedFilter(): PhotoFilter =
    PhotoFilter.entries.firstOrNull { it.name == filterName } ?: PhotoFilter.NORMAL
}

@Keep
@IgnoreExtraProperties
data class IgReel(
  @DocumentId val id: String = "",
  val appScope: String = APP_SCOPE,
  val authorId: String = "",
  val authorUsername: String = "",
  val authorAvatarUrl: String? = null,
  val mediaUrl: String = "",
  val caption: String = "",
  val audioTrack: String = "",
  val filterName: String = PhotoFilter.NORMAL.name,
  val likesCount: Long = 0L,
  val commentsCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun resolvedFilter(): PhotoFilter =
    PhotoFilter.entries.firstOrNull { it.name == filterName } ?: PhotoFilter.NORMAL
}

@Keep
@IgnoreExtraProperties
data class IgChat(
  @DocumentId val id: String = "",
  val participantIds: List<String> = emptyList(),
  val participantAId: String = "",
  val participantAName: String = "",
  val participantAAvatar: String? = null,
  val participantBId: String = "",
  val participantBName: String = "",
  val participantBAvatar: String? = null,
  val lastMessageText: String = "",
  val lastSenderId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class IgMessage(
  @DocumentId val id: String = "",
  val chatId: String = "",
  val participantIds: List<String> = emptyList(),
  val senderId: String = "",
  val senderUsername: String = "",
  val text: String = "",
  val sharedPostId: String? = null,
  val sharedPostImage: String? = null,
  val sharedPostAuthor: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class UserTargetMarker(
  @DocumentId val id: String = "",
  val userId: String = "",
  val targetId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class IgNotification(
  @DocumentId val id: String = "",
  val targetUserId: String = "",
  val actorId: String = "",
  val actorUsername: String = "",
  val actorAvatarUrl: String? = null,
  val type: String = NotificationType.LIKE.name,
  val postId: String? = null,
  val postThumbnail: String? = null,
  val previewText: String = "",
  val read: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun resolvedType(): NotificationType =
    NotificationType.entries.firstOrNull { it.name == type } ?: NotificationType.LIKE
}
