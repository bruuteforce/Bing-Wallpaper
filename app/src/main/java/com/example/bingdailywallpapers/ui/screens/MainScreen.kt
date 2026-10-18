package com.example.bingdailywallpapers.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bingdailywallpapers.data.model.BingImage
import com.example.bingdailywallpapers.ui.viewmodel.BingWallpaperViewModel
import com.example.bingdailywallpapers.util.WallpaperHelper
import com.example.bingdailywallpapers.util.WallpaperPreferences
import com.example.bingdailywallpapers.worker.DailyWallpaperWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: BingWallpaperViewModel = viewModel(),
    themeMode: String = "system",
    onThemeModeChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadFavorites(context)
    }

    var currentTab by remember { mutableStateOf(0) }
    var selectedDetailWallpaper by remember { mutableStateOf<BingImage?>(null) }
    var croppingWallpaper by remember { mutableStateOf<BingImage?>(null) }
    var settingWallpaperImage by remember { mutableStateOf<BingImage?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    if (selectedDetailWallpaper != null) {
        BackHandler {
            selectedDetailWallpaper = null
        }
    }

    val activeSettingImage = settingWallpaperImage
    if (activeSettingImage != null) {
        SetWallpaperChoiceDialog(
            image = activeSettingImage,
            onDismiss = { settingWallpaperImage = null },
            onCropAndApply = {
                settingWallpaperImage = null
                croppingWallpaper = activeSettingImage
            },
            onApplyDirectly = { target ->
                settingWallpaperImage = null
                viewModel.setWallpaper(context, activeSettingImage.fullImageUrl, target) { success, msg ->
                    Toast.makeText(
                        context,
                        if (success) "Wallpaper applied successfully!" else (msg ?: "Failed to set wallpaper"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    if (showSettingsDialog) {
        AppSettingsDialog(
            themeMode = themeMode,
            onThemeModeChanged = onThemeModeChanged,
            onDismiss = { showSettingsDialog = false },
            viewModel = viewModel
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        topBar = {
            if (selectedDetailWallpaper == null && croppingWallpaper == null) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Bing Wallpapers",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { showSettingsDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "App Settings & Theme",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (selectedDetailWallpaper == null && croppingWallpaper == null) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Rounded.Home, contentDescription = "Daily") },
                        label = { Text("Daily") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Rounded.PhotoLibrary, contentDescription = "Gallery") },
                        label = { Text("Gallery") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Rounded.Favorite, contentDescription = "Favorites") },
                        label = { Text("Favorites") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val cropTarget = croppingWallpaper
            val detail = selectedDetailWallpaper

            if (cropTarget != null) {
                CropWallpaperScreen(
                    imageUrl = cropTarget.fullImageUrl,
                    title = cropTarget.title,
                    onBackClick = { croppingWallpaper = null },
                    onApplyWallpaper = { croppedBitmap, wallpaperFlag ->
                        viewModel.applyCroppedWallpaper(context, croppedBitmap, wallpaperFlag) { success, msg ->
                            Toast.makeText(
                                context,
                                if (success) "Wallpaper set successfully!" else (msg ?: "Failed to set wallpaper"),
                                Toast.LENGTH_SHORT
                            ).show()
                            if (success) {
                                croppingWallpaper = null
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                )
            } else if (detail != null) {
                WallpaperDetailScreen(
                    image = detail,
                    imageUrl = detail.fullImageUrl,
                    title = detail.title,
                    copyright = detail.copyright,
                    date = detail.startdate,
                    favoriteWallpapers = uiState.favoriteWallpapers,
                    onBackClick = { selectedDetailWallpaper = null },
                    onSetWallpaperClick = {
                        settingWallpaperImage = detail
                    },
                    onDownloadClick = {
                        viewModel.downloadWallpaper(context, detail.fullImageUrl, detail.title) { success, msg ->
                            Toast.makeText(
                                context,
                                if (success) "Wallpaper downloaded successfully!" else (msg ?: "Download failed"),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onToggleFavorite = {
                        val added = viewModel.toggleFavorite(context, detail)
                        Toast.makeText(
                            context,
                            if (added) "Added to favorites" else "Removed from favorites",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onShareClick = {
                        viewModel.shareWallpaper(context, detail)
                    },
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                )
            } else {
                when (currentTab) {
                    0 -> {
                        val dailyFav = uiState.dailyWallpaper?.let { viewModel.isFavorite(it) } ?: false
                        DailyWallpaperScreen(
                            dailyWallpaper = uiState.dailyWallpaper,
                            isLoading = uiState.isLoading,
                            errorMessage = uiState.errorMessage,
                            favoriteWallpapers = uiState.favoriteWallpapers,
                            isFavorite = dailyFav,
                            onRefresh = { viewModel.fetchWallpapers(isRefresh = true) },
                            onSetWallpaperClick = { image ->
                                settingWallpaperImage = image
                            },
                            onDownloadClick = { image ->
                                viewModel.downloadWallpaper(context, image.fullImageUrl, image.title) { success, msg ->
                                    Toast.makeText(
                                        context,
                                        if (success) "Wallpaper downloaded successfully!" else (msg ?: "Download failed"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onDetailClick = { image ->
                                selectedDetailWallpaper = image
                            },
                            onToggleFavorite = { image ->
                                val added = viewModel.toggleFavorite(context, image)
                                Toast.makeText(
                                    context,
                                    if (added) "Added to favorites" else "Removed from favorites",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onShareClick = { image ->
                                viewModel.shareWallpaper(context, image)
                            },
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                        )
                    }
                    1 -> GalleryScreen(
                        wallpapers = uiState.galleryWallpapers,
                        favoriteWallpapers = uiState.favoriteWallpapers,
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage,
                        onRefresh = { viewModel.fetchWallpapers(isRefresh = true) },
                        onWallpaperClick = { image ->
                            selectedDetailWallpaper = image
                        },
                        onToggleFavorite = { image ->
                            val added = viewModel.toggleFavorite(context, image)
                            Toast.makeText(
                                context,
                                if (added) "Added to favorites" else "Removed from favorites",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                    )
                    2 -> FavoritesScreen(
                        favoriteWallpapers = uiState.favoriteWallpapers,
                        onWallpaperClick = { image ->
                            selectedDetailWallpaper = image
                        },
                        onToggleFavorite = { image ->
                            viewModel.toggleFavorite(context, image)
                            Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                    )
                }
            }
        }
    }
}

@Composable
fun AppSettingsDialog(
    themeMode: String,
    onThemeModeChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: BingWallpaperViewModel = viewModel()
) {
    val context = LocalContext.current
    var autoWallpaperEnabled by remember {
        mutableStateOf(WallpaperPreferences.isAutoWallpaperEnabled(context))
    }
    var selectedMarket by remember {
        mutableStateOf(WallpaperPreferences.getBingMarket(context))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("App Settings", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bing Region & Timezone Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Bing Region & Timezone",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Choose preferred region for tailored wallpapers and timezone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val markets = listOf(
                        Triple("en-IN", "India", "Indian timezone & culture"),
                        Triple("en-US", "United States", "US daily wallpapers"),
                        Triple("en-GB", "United Kingdom", "UK daily wallpapers"),
                        Triple("en-WW", "Global / Worldwide", "Worldwide international wallpapers")
                    )

                    markets.forEach { (code, name, desc) ->
                        RegionOptionRow(
                            title = name,
                            description = desc,
                            selected = selectedMarket == code,
                            onClick = {
                                selectedMarket = code
                                viewModel.setMarketAndFetch(code)
                                Toast.makeText(
                                    context,
                                    "Bing Region updated to $name ($code)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Theme Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Theme & Appearance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Choose your preferred visual appearance and color mode.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    ThemeOptionRow(
                        title = "System Default",
                        description = "Match system appearance",
                        selected = themeMode == "system",
                        onClick = { onThemeModeChanged("system") }
                    )
                    ThemeOptionRow(
                        title = "Dark Mode",
                        description = "Deep charcoal surfaces with vibrant blue accents",
                        selected = themeMode == "dark",
                        onClick = { onThemeModeChanged("dark") }
                    )
                    ThemeOptionRow(
                        title = "Light Mode",
                        description = "Clean light slate appearance",
                        selected = themeMode == "light",
                        onClick = { onThemeModeChanged("light") }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Auto-Sync Daily Wallpaper Toggle
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Sync Daily Wallpaper",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Automatically sync and update daily wallpaper in background",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoWallpaperEnabled,
                            onCheckedChange = { checked ->
                                autoWallpaperEnabled = checked
                                WallpaperPreferences.setAutoWallpaperEnabled(context, checked)
                                if (checked) {
                                    DailyWallpaperWorker.schedule(context)
                                    Toast.makeText(context, "Auto-sync enabled", Toast.LENGTH_SHORT).show()
                                } else {
                                    DailyWallpaperWorker.cancel(context)
                                    Toast.makeText(context, "Auto-sync disabled", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun RegionOptionRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(
                selected = selected,
                onClick = onClick
            )
        }
    }
}

@Composable
fun ThemeOptionRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(
                selected = selected,
                onClick = onClick
            )
        }
    }
}

@Composable
fun SetWallpaperChoiceDialog(
    image: BingImage,
    onDismiss: () -> Unit,
    onCropAndApply: () -> Unit,
    onApplyDirectly: (WallpaperHelper.WallpaperTarget) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Wallpaper", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose how you want to apply this wallpaper:")
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onCropAndApply,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.Wallpaper, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Crop & Apply", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onApplyDirectly(WallpaperHelper.WallpaperTarget.BOTH) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Apply Directly (Both Screen & Lock)")
                }
                OutlinedButton(
                    onClick = { onApplyDirectly(WallpaperHelper.WallpaperTarget.HOME_SCREEN) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Apply Directly (Home Screen Only)")
                }
                OutlinedButton(
                    onClick = { onApplyDirectly(WallpaperHelper.WallpaperTarget.LOCK_SCREEN) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Apply Directly (Lock Screen Only)")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}
