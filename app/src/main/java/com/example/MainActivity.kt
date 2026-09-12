package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.AppDatabase
import com.example.data.repository.MediaRepository
import com.example.player.PlayerViewModel
import com.example.ui.MainViewModel
import com.example.ui.VlcApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val database = AppDatabase.getInstance(applicationContext)
    val mediaRepository = MediaRepository(applicationContext, database.mediaDao())

    setContent {
      MyApplicationTheme(darkTheme = true) {
        val mainViewModel = remember { MainViewModel(mediaRepository) }
        val playerViewModel = remember { PlayerViewModel(applicationContext, mediaRepository) }

        // Handle incoming video intents (Open With VLC)
        intent?.data?.let { uri ->
          if (intent.action == Intent.ACTION_VIEW) {
            mainViewModel.onLocalFilePicked(uri, uri.lastPathSegment ?: "External Video")
          }
        }

        VlcApp(
          mainViewModel = mainViewModel,
          playerViewModel = playerViewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("Android") }
}
