package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.InstagramRepository
import com.example.data.authStateFlow
import com.example.ui.InstagramViewModel
import com.example.ui.VistagramApp
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.VistagramTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      VistagramTheme {
        AppContent()
      }
    }
  }
}

@Composable
fun AppContent() {
  val currentUser by
    Firebase.auth
      .authStateFlow()
      .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

  val user = currentUser
  if (user == null) {
    AuthScreen(onAuthSuccess = {})
  } else {
    val uid = user.uid
    val fallbackName = user.displayName?.trim().orEmpty().ifBlank { "Creator" }
    val fallbackPhoto = user.photoUrl?.toString()

    key(uid) {
      val viewModel: InstagramViewModel =
        viewModel(
          key = uid,
          factory =
            viewModelFactory {
              initializer {
                val app =
                  checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                  }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                InstagramViewModel(
                  repository = InstagramRepository(db),
                  currentUserId = uid,
                  fallbackDisplayName = fallbackName,
                  fallbackPhotoUrl = fallbackPhoto,
                )
              }
            },
        )
      VistagramApp(viewModel = viewModel)
    }
  }
}
