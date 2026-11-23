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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ImageEncodingHelper
import com.example.model.PhotoFilter
import com.example.model.PostCategory
import com.example.ui.components.VistagramMediaImage
import com.example.ui.theme.IgBlue
import kotlinx.coroutines.launch

enum class CreateMode(val label: String) {
  POST("Post"),
  STORY("Story"),
  REEL("Reel"),
}

@Composable
fun CreateScreen(
  initialMode: CreateMode = CreateMode.POST,
  isSubmitting: Boolean,
  onPublishPost: (mediaUrls: List<String>, filter: PhotoFilter, caption: String, location: String, category: PostCategory) -> Unit,
  onPublishStory: (mediaUrl: String, overlayText: String, filter: PhotoFilter) -> Unit,
  onPublishReel: (mediaUrl: String, caption: String, audioTrack: String, filter: PhotoFilter) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var mode by rememberSaveable(initialMode) { mutableStateOf(initialMode) }
  val selectedMedia = remember { mutableStateListOf("res:img_sample_sunset") }
  var selectedFilter by remember { mutableStateOf(PhotoFilter.NORMAL) }
  var caption by rememberSaveable { mutableStateOf("") }
  var location by rememberSaveable { mutableStateOf("") }
  var audioTrack by rememberSaveable { mutableStateOf("Golden Hour · Lo-Fi Mix") }
  var selectedCategory by remember { mutableStateOf(PostCategory.PHOTOGRAPHY) }
  var isEncodingPhoto by remember { mutableStateOf(false) }

  val multiPhotoLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(4)) { uris ->
      if (uris.isNotEmpty()) {
        isEncodingPhoto = true
        scope.launch {
          val encoded = uris.mapNotNull { ImageEncodingHelper.encodeUriToDataUrl(context, it) }
          if (encoded.isNotEmpty()) {
            selectedMedia.clear()
            selectedMedia.addAll(encoded)
          }
          isEncodingPhoto = false
        }
      }
    }

  val singlePhotoLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        isEncodingPhoto = true
        scope.launch {
          val encoded = ImageEncodingHelper.encodeUriToDataUrl(context, uri)
          if (encoded != null) {
            selectedMedia.clear()
            selectedMedia.add(encoded)
          }
          isEncodingPhoto = false
        }
      }
    }

  val activePreview = selectedMedia.firstOrNull() ?: "res:img_sample_sunset"

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    SingleChoiceSegmentedButtonRow(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
      CreateMode.entries.forEachIndexed { index, item ->
        SegmentedButton(
          selected = mode == item,
          onClick = { mode = item },
          shape = SegmentedButtonDefaults.itemShape(index = index, count = CreateMode.entries.size),
        ) {
          Text(item.label)
        }
      }
    }

    // Live Preview Canvas
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 16.dp)
          .aspectRatio(if (mode == CreateMode.POST) 1f else 0.78f)
          .clip(MaterialTheme.shapes.large)
          .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
      VistagramMediaImage(
        mediaRef = activePreview,
        contentDescription = "Selected preview",
        filter = selectedFilter,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )

      if (mode == CreateMode.STORY && caption.isNotBlank()) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color.Black.copy(alpha = 0.65f),
          modifier = Modifier.align(Alignment.Center).padding(24.dp),
        ) {
          Text(
            text = caption,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
          )
        }
      }

      if (mode == CreateMode.POST && selectedMedia.size > 1) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color.Black.copy(alpha = 0.65f),
          modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
        ) {
          Text(
            text = "${selectedMedia.size} photos (Carousel)",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
      }

      if (isEncodingPhoto) {
        Box(
          modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(color = Color.White)
        }
      }
    }

    // Pick from Device Photo Picker
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      OutlinedButton(
        onClick = {
          val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          if (mode == CreateMode.POST) multiPhotoLauncher.launch(request)
          else singlePhotoLauncher.launch(request)
        },
        modifier = Modifier.weight(1f).testTag("pick_device_photo_button"),
        shape = RoundedCornerShape(12.dp),
      ) {
        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (mode == CreateMode.POST) "Pick Photos (Gallery)" else "Pick from Gallery")
      }
    }

    // Selected carousel strip
    if (mode == CreateMode.POST && selectedMedia.size > 1) {
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(selectedMedia) { item ->
          Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp))) {
            VistagramMediaImage(
              mediaRef = item,
              contentDescription = "Carousel slide",
              filter = selectedFilter,
              modifier = Modifier.fillMaxSize(),
            )
            Surface(
              shape = CircleShape,
              color = Color.Black.copy(alpha = 0.7f),
              modifier =
                Modifier.align(Alignment.TopEnd)
                  .padding(4.dp)
                  .size(20.dp)
                  .clickable { if (selectedMedia.size > 1) selectedMedia.remove(item) },
            ) {
              Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.padding(3.dp))
            }
          }
        }
      }
    }

    // Studio Camera Roll Presets (so creators can also pick bundled studio photos or gradients)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        text = "Studio Camera Roll",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 16.dp),
      )
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(ImageEncodingHelper.studioPresets) { (ref, label) ->
          val isSelected = ref in selectedMedia
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier =
              Modifier.width(74.dp).clickable {
                if (mode == CreateMode.POST) {
                  if (isSelected && selectedMedia.size > 1) selectedMedia.remove(ref)
                  else if (!isSelected && selectedMedia.size < 4) selectedMedia.add(ref)
                } else {
                  selectedMedia.clear()
                  selectedMedia.add(ref)
                }
              },
          ) {
            Box(
              modifier =
                Modifier.size(70.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) IgBlue else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp),
                  ),
            ) {
              VistagramMediaImage(mediaRef = ref, contentDescription = label, modifier = Modifier.fillMaxSize())
              if (isSelected) {
                Surface(
                  shape = CircleShape,
                  color = IgBlue,
                  modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp),
                ) {
                  Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.padding(2.dp))
                }
              }
            }
          }
        }
        if (mode == CreateMode.STORY) {
          items(ImageEncodingHelper.storyGradients) { (ref, label) ->
            val isSelected = ref in selectedMedia
            Box(
              modifier =
                Modifier.size(70.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) IgBlue else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp),
                  )
                  .clickable {
                    selectedMedia.clear()
                    selectedMedia.add(ref)
                  },
              contentAlignment = Alignment.Center,
            ) {
              VistagramMediaImage(mediaRef = ref, contentDescription = label, modifier = Modifier.fillMaxSize())
              Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
          }
        }
      }
    }

    // Color Filters Strip
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
        Text("Filters", style = MaterialTheme.typography.titleSmall)
      }
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(PhotoFilter.entries) { filter ->
          val chosen = selectedFilter == filter
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clickable { selectedFilter = filter },
          ) {
            Box(
              modifier =
                Modifier.size(58.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .border(
                    width = if (chosen) 2.dp else 1.dp,
                    color = if (chosen) IgBlue else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(10.dp),
                  ),
            ) {
              VistagramMediaImage(
                mediaRef = activePreview,
                contentDescription = filter.label,
                filter = filter,
                modifier = Modifier.fillMaxSize(),
              )
            }
            Text(
              text = filter.label,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
              color = if (chosen) IgBlue else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    // Form Fields
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      OutlinedTextField(
        value = caption,
        onValueChange = { caption = it },
        modifier = Modifier.fillMaxWidth().testTag("create_caption_input"),
        label = {
          Text(
            when (mode) {
              CreateMode.POST -> "Write a caption… (#hashtags, @mentions)"
              CreateMode.STORY -> "Story overlay text (optional)"
              CreateMode.REEL -> "Reel caption…"
            }
          )
        },
        minLines = 2,
        maxLines = 4,
        shape = MaterialTheme.shapes.medium,
      )

      if (mode == CreateMode.POST) {
        OutlinedTextField(
          value = location,
          onValueChange = { location = it },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Add location (optional)") },
          leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) },
          singleLine = true,
          shape = MaterialTheme.shapes.medium,
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(PostCategory.entries) { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(cat.label) },
            )
          }
        }
      }

      if (mode == CreateMode.REEL) {
        OutlinedTextField(
          value = audioTrack,
          onValueChange = { audioTrack = it },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Audio track") },
          leadingIcon = { Icon(Icons.Rounded.MusicNote, contentDescription = null) },
          singleLine = true,
          shape = MaterialTheme.shapes.medium,
        )
      }

      Button(
        onClick = {
          when (mode) {
            CreateMode.POST -> onPublishPost(selectedMedia.toList(), selectedFilter, caption, location, selectedCategory)
            CreateMode.STORY -> onPublishStory(activePreview, caption, selectedFilter)
            CreateMode.REEL -> onPublishReel(activePreview, caption, audioTrack, selectedFilter)
          }
        },
        enabled = !isSubmitting && !isEncodingPhoto && selectedMedia.isNotEmpty(),
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("publish_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
        } else {
          Text(
            text =
              when (mode) {
                CreateMode.POST -> "Share Post"
                CreateMode.STORY -> "Add to Your Story"
                CreateMode.REEL -> "Share Reel"
              },
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}
