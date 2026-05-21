package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.theme.MyApplicationTheme
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class PlayerRoute(val uri: String, val title: String, val isAudioOnly: Boolean = false)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            viewModel.fetchLocalVideos()
        } else {
            Toast.makeText(this, "Permission denied. Cannot load videos.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestStoragePermissions()
        
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = HomeRoute) {
                        composable<HomeRoute> {
                            HomeScreen(
                                viewModel = viewModel,
                                onPlayLocalVideo = { video ->
                                    navController.navigate(PlayerRoute(video.uri.toString(), video.name))
                                },
                                onPlayYoutube = { url, title, isAudioOnly ->
                                    navController.navigate(PlayerRoute(url, title, isAudioOnly))
                                }
                            )
                        }
                        
                        composable<PlayerRoute> { backStackEntry ->
                            val route = backStackEntry.arguments?.let {
                                // Manual extraction since Navigation serialization needs a bit more setup in some cases,
                                // but we are using type-safe compose nav 2.8.0.
                            }
                            
                            // Let's use the explicit savedStateHandle extraction for 2.8.0
                            val args = backStackEntry.toRoute<PlayerRoute>()
                            PlayerScreen(
                                videoUri = args.uri,
                                title = args.title,
                                isAudioOnly = args.isAudioOnly,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestStoragePermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO // If we want local audio too
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        } else {
            viewModel.fetchLocalVideos()
        }
    }
}
