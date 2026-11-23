package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageEncodingHelper {
  private const val BASE64_PREFIX = "data:image/jpeg;base64,"
  const val RES_PREFIX = "res:"
  const val GRADIENT_PREFIX = "gradient:"

  val studioPresets: List<Pair<String, String>> = listOf(
    "res:img_sample_sunset" to "Mediterranean Golden Hour",
    "res:img_sample_urban" to "Neon Alley Twilight",
    "res:img_sample_matcha" to "Morning Matcha Flatlay",
    "res:img_auth_showcase" to "Editorial Studio Collage",
  )

  val storyGradients: List<Pair<String, String>> = listOf(
    "gradient:sunset" to "Sunset",
    "gradient:ocean" to "Ocean",
    "gradient:neon" to "Neon",
    "gradient:obsidian" to "Obsidian",
  )

  fun resolveDrawableRes(mediaRef: String): Int? {
    if (!mediaRef.startsWith(RES_PREFIX)) return null
    return when (mediaRef.removePrefix(RES_PREFIX)) {
      "img_sample_sunset" -> R.drawable.img_sample_sunset
      "img_sample_urban" -> R.drawable.img_sample_urban
      "img_sample_matcha" -> R.drawable.img_sample_matcha
      "img_auth_showcase" -> R.drawable.img_auth_showcase
      else -> null
    }
  }

  fun decodeBase64Bitmap(mediaRef: String): Bitmap? {
    if (!mediaRef.startsWith("data:image/")) return null
    return try {
      val base64Part = mediaRef.substringAfter("base64,", "")
      if (base64Part.isBlank()) return null
      val bytes = Base64.decode(base64Part, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
      null
    }
  }

  suspend fun encodeUriToDataUrl(context: Context, uri: Uri, maxDim: Int = 640): String? =
    withContext(Dispatchers.IO) {
      try {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val origMax = max(boundsOpts.outWidth, boundsOpts.outHeight)
        if (origMax <= 0) return@withContext null

        var sampleSize = 1
        while (origMax / sampleSize > maxDim * 2) {
          sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return@withContext null

        val scale = if (max(decoded.width, decoded.height) > maxDim) {
          maxDim.toFloat() / max(decoded.width, decoded.height).toFloat()
        } else 1f

        val scaled = if (scale < 1f) {
          Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true,
          ).also { if (it !== decoded) decoded.recycle() }
        } else decoded

        var quality = 78
        var bytes: ByteArray
        do {
          val baos = ByteArrayOutputStream()
          scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
          bytes = baos.toByteArray()
          quality -= 12
        } while (bytes.size > 110_000 && quality >= 30)

        scaled.recycle()
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val result = "$BASE64_PREFIX$encoded"
        if (result.length <= 180_000) result else null
      } catch (_: Exception) {
        null
      }
    }
}
