package com.example.bingdailywallpapers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.bingdailywallpapers.ui.screens.MainScreen
import com.example.bingdailywallpapers.ui.theme.BingDailyWallpapersTheme
import com.example.bingdailywallpapers.util.WallpaperPreferences
import com.example.bingdailywallpapers.worker.DailyWallpaperWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            DailyWallpaperWorker.schedule(applicationContext)
        } catch (_: Exception) {
            // Prevent startup crash if WorkManager fails
        }
        setContent {
            val context = LocalContext.current
            var themeMode by remember { mutableStateOf(WallpaperPreferences.getThemeMode(context)) }
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            BingDailyWallpapersTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        themeMode = themeMode,
                        onThemeModeChanged = { newMode ->
                            themeMode = newMode
                            WallpaperPreferences.setThemeMode(context, newMode)
                        }
                    )
                }
            }
        }
    }
}
