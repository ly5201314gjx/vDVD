package com.example.ui

import android.app.Activity
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EngineDial
import com.example.ui.components.QuickDeck
import com.example.ui.components.SmartAddressBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserWorkspace(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val selectedTabId by viewModel.selectedTabId.collectAsStateWithLifecycle()
    val selectedEngine by viewModel.selectedEngine.collectAsStateWithLifecycle()
    val clipboardUrl by viewModel.clipboardUrl.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    val splitTabId by viewModel.splitTabId.collectAsStateWithLifecycle()
    val filterMode by viewModel.filterMode.collectAsStateWithLifecycle()
    val desktopTabs by viewModel.desktopTabs.collectAsStateWithLifecycle()
    val textZoomTabs by viewModel.textZoomTabs.collectAsStateWithLifecycle()

    val currentTab = tabs.firstOrNull { it.id == selectedTabId }
    val splitTab = tabs.firstOrNull { it.id == splitTabId }

    // Inter-tab sheet states
    var showEngineDial by remember { mutableStateOf(false) }
    var showTabCarousel by remember { mutableStateOf(false) }
    var showAddSiteModal by remember { mutableStateOf(false) }
    var showHistoryBookmarksModal by remember { mutableStateOf(false) }
    var showAssistDisk by remember { mutableStateOf(false) }

    // Navigation state helper variables
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    val activeWebView = remember(selectedTabId) {
        if (selectedTabId.isNotEmpty()) {
            viewModel.getWebViewForTab(selectedTabId, context)
        } else null
    }

    // Checking if we can go back / forward reactively
    LaunchedEffect(currentTab?.url, currentTab?.progress) {
        activeWebView?.let { view ->
            canGoBack = view.canGoBack()
            canGoForward = view.canGoForward()
        }
    }

    // Detect browser viewport clip and triggers Clipboard suggestions periodically
    LaunchedEffect(Unit) {
        viewModel.detectClipboardUrl(context)
    }

    // Handle physical back button ergonomically
    BackHandler(enabled = true) {
        if (showTabCarousel) {
            showTabCarousel = false
        } else if (showEngineDial) {
            showEngineDial = false
        } else if (showHistoryBookmarksModal) {
            showHistoryBookmarksModal = false
        } else if (activeWebView?.canGoBack() == true) {
            viewModel.navigateBack()
        } else {
            (context as? Activity)?.finish()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(), // Clean Notch padding
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
            ) {
                // Expanding Dial Switcher Panel (Frosted Overlay)
                AnimatedVisibility(
                    visible = showEngineDial,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x33FFFFFF),
                                        Color(0xFAFAF7F2)
                                    )
                                ),
                                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(Color.White.copy(0.6f), Color.White.copy(0.1f))
                                ),
                                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                            )
                    ) {
                        EngineDial(
                            selectedEngine = selectedEngine,
                            onEngineSelected = { engine ->
                                viewModel.selectSearchEngine(engine)
                                showEngineDial = false
                            }
                        )
                    }
                }

                // Expanding Tabs Swiper Desk Overlay (Zen Glass Shelf)
                AnimatedVisibility(
                    visible = showTabCarousel,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    com.example.ui.components.TabCarousel(
                        tabs = tabs,
                        selectedTabId = selectedTabId,
                        onTabSelected = { tabId ->
                            viewModel.selectTab(tabId)
                            showTabCarousel = false
                        },
                        onTabClosed = { tabId ->
                            viewModel.closeTab(tabId)
                        },
                        onNewTabClick = {
                            viewModel.createNewTab()
                            showTabCarousel = false
                        }
                    )
                }

                // Floating Action Smart Controller Capsule
                SmartAddressBar(
                    currentUrl = currentTab?.url ?: "",
                    progress = currentTab?.progress ?: 0,
                    scrollProgress = currentTab?.scrollProgress ?: 0f,
                    engine = selectedEngine,
                    tabCount = tabs.size,
                    isBookmarked = bookmarks.any { it.url == currentTab?.url },
                    onNavigateTo = { query ->
                        viewModel.loadUrlInActiveTab(query)
                    },
                    onBackClick = { viewModel.navigateBack() },
                    onForwardClick = { viewModel.navigateForward() },
                    onReloadClick = { viewModel.reloadActiveTab() },
                    onHomeClick = { viewModel.goHome() },
                    onToggleBookmark = { viewModel.toggleBookmarkOfActiveTab() },
                    onTabToggleClick = { showTabCarousel = !showTabCarousel },
                    onEngineBadgeClick = { showEngineDial = !showEngineDial },
                    canGoBack = canGoBack,
                    canGoForward = canGoForward,
                    modifier = Modifier.navigationBarsPadding() // Soft gestures padding compatibility
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .drawWithContent {
                    drawContent()
                    if (filterMode != FilterMode.NONE) {
                        drawRect(color = Color(filterMode.color))
                    }
                }
        ) {
            // Web view viewport / Started Dashboards switch
            if (currentTab != null) {
                if (splitTab != null) {
                    // Split mode active: Stacked viewports
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Top Pane (Active Tab)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectTab(currentTab.id)
                                }
                                .border(
                                    width = if (selectedTabId == currentTab.id) 2.dp else 1.dp,
                                    color = if (selectedTabId == currentTab.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                )
                        ) {
                            if (currentTab.isHome) {
                                QuickDeck(
                                    clipboardUrl = clipboardUrl,
                                    bookmarks = bookmarks,
                                    onUrlClick = { url -> viewModel.loadUrlInActiveTab(url) },
                                    onAddBookmarkClick = { showAddSiteModal = true },
                                    onDeleteBookmarkClick = { bookmark -> viewModel.deleteBookmark(bookmark) },
                                    onClearClipboard = { viewModel.clearClipboardSuggestion() },
                                    modifier = Modifier.fillMaxSize().padding(vertical = 4.dp)
                                )
                            } else {
                                AndroidView(
                                    factory = { ctx ->
                                        viewModel.getWebViewForTab(currentTab.id, ctx) ?: WebView(ctx)
                                    },
                                    modifier = Modifier.fillMaxSize().testTag("webview_viewport_top")
                                )
                            }
                            
                            // Visual Badge for Top Panel
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .background(
                                        if (selectedTabId == currentTab.id) MaterialTheme.colorScheme.primary else Color.Gray,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("主舱视窗", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Compact Divider Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Swap visual button
                            IconButton(
                                onClick = {
                                    val temp = selectedTabId
                                    viewModel.selectTab(splitTab.id)
                                    viewModel.setSplitTabId(temp)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Swap",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(32.dp))

                            // Close split screen visual button
                            IconButton(
                                onClick = { viewModel.closeSplitMode() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Split",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Bottom Pane (Split Tab)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectTab(splitTab.id)
                                }
                                .border(
                                    width = if (selectedTabId == splitTab.id) 2.dp else 1.dp,
                                    color = if (selectedTabId == splitTab.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                )
                        ) {
                            if (splitTab.isHome) {
                                QuickDeck(
                                    clipboardUrl = null,
                                    bookmarks = bookmarks,
                                    onUrlClick = { url ->
                                        val prevId = selectedTabId
                                        viewModel.selectTab(splitTab.id)
                                        viewModel.loadUrlInActiveTab(url)
                                        viewModel.selectTab(prevId)
                                    },
                                    onAddBookmarkClick = { showAddSiteModal = true },
                                    onDeleteBookmarkClick = { bookmark -> viewModel.deleteBookmark(bookmark) },
                                    onClearClipboard = {},
                                    modifier = Modifier.fillMaxSize().padding(vertical = 4.dp)
                                )
                            } else {
                                AndroidView(
                                    factory = { ctx ->
                                        viewModel.getWebViewForTab(splitTab.id, ctx) ?: WebView(ctx)
                                    },
                                    modifier = Modifier.fillMaxSize().testTag("webview_viewport_bottom")
                                )
                            }

                            // Visual Badge for Bottom Panel
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .background(
                                        if (selectedTabId == splitTab.id) MaterialTheme.colorScheme.primary else Color.Gray,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("副舱分屏", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Normal single window
                    if (currentTab.isHome) {
                        QuickDeck(
                            clipboardUrl = clipboardUrl,
                            bookmarks = bookmarks,
                            onUrlClick = { url -> viewModel.loadUrlInActiveTab(url) },
                            onAddBookmarkClick = { showAddSiteModal = true },
                            onDeleteBookmarkClick = { bookmark -> viewModel.deleteBookmark(bookmark) },
                            onClearClipboard = { viewModel.clearClipboardSuggestion() },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 12.dp)
                        )
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                viewModel.getWebViewForTab(currentTab.id, ctx) ?: WebView(ctx)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("webview_viewport")
                        )
                    }
                }
            }

            // High Class: Floating Bookmarks and History overlay trigger
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(12.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .background(Color.White.copy(alpha = 0.85f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        .clickable { showHistoryBookmarksModal = true }
                        .testTag("history_logs_icon"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "Logs",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Floating Smart Assist Disk button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(16.dp, CircleShape, spotColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            ),
                            shape = CircleShape
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                        .clickable { showAssistDisk = !showAssistDisk }
                        .testTag("floating_assist_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (showAssistDisk) Icons.Default.Close else Icons.Default.Widgets,
                        contentDescription = "Assist",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded Smart Assist Center Layout
            AnimatedVisibility(
                visible = showAssistDisk,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 135.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.96f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(24.dp, RoundedCornerShape(24.dp), clip = false)
                        .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "智能阅读舒享舱",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { showAssistDisk = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Split pane & Desktop layouts cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .background(
                                        if (splitTabId != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                        else MaterialTheme.colorScheme.background,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (splitTabId != null) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        viewModel.toggleSplitMode()
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.VerticalSplit,
                                        contentDescription = null,
                                        tint = if (splitTabId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "分屏共读",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (splitTabId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            val isDesktop = desktopTabs.contains(selectedTabId)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .background(
                                        if (isDesktop) MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                                        else MaterialTheme.colorScheme.background,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isDesktop) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        viewModel.toggleDesktopMode(selectedTabId)
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = if (isDesktop) Icons.Default.Laptop else Icons.Default.Smartphone,
                                        contentDescription = null,
                                        tint = if (isDesktop) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        if (isDesktop) "电脑版" else "极简手机版",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDesktop) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        // Warm comforting filters
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "护眼柔光色温",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterMode.values().forEach { mode ->
                                    val isSelected = filterMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                                else MaterialTheme.colorScheme.background,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                viewModel.setFilterMode(mode)
                                            }
                                            .padding(horizontal = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(
                                                        if (mode == FilterMode.NONE) Color.LightGray else Color(mode.color).copy(alpha = 1f),
                                                        CircleShape
                                                    )
                                                    .border(1.dp, Color.White, CircleShape)
                                            )
                                            Text(
                                                mode.label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Web Text Scaling
                        val currentZoom = textZoomTabs[selectedTabId] ?: 100
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "视界排版缩放",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(14.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f), RoundedCornerShape(14.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        val nextZoom = (currentZoom - 15).coerceAtLeast(50)
                                        viewModel.setTextZoom(selectedTabId, nextZoom)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Zoom out",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FormatSize,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "$currentZoom%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val nextZoom = (currentZoom + 15).coerceAtMost(200)
                                        viewModel.setTextZoom(selectedTabId, nextZoom)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Zoom in",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Extreme instant cache cleanup eraser
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    viewModel.clearHistory()
                                    viewModel.getWebViewForTab(selectedTabId, context)?.let { view ->
                                        try {
                                            view.clearCache(true)
                                            view.clearHistory()
                                            view.clearFormData()
                                        } catch (e: Exception) {}
                                    }
                                    showAssistDisk = false
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "一键无痕隐私净化",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Drawer 1: Adding Bookmark / Favorites manually
    if (showAddSiteModal) {
        var newTitle by remember { mutableStateOf("") }
        var newUrl by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSiteModal = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUrl.isNotEmpty()) {
                            val finalUrl = if (newUrl.startsWith("http://") || newUrl.startsWith("https://")) newUrl else "https://$newUrl"
                            val finalTitle = newTitle.ifEmpty { viewModel.getCleanDomain(finalUrl) }
                            viewModel.addBookmarkManual(finalTitle, finalUrl)
                        }
                        showAddSiteModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSiteModal = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                }
            },
            title = {
                Text("添加快捷网站 / Dial addition", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("标题 (选填)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newUrl,
                        onValueChange = { newUrl = it },
                        label = { Text("网址 (URL)") },
                        placeholder = { Text("example.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Modal Drawer 2: Reading Log center (List tabs of bookmarks and history entries)
    if (showHistoryBookmarksModal) {
        var activeTabIdx by remember { mutableStateOf(0) } // 0 is History logs, 1 is Bookmarks

        AlertDialog(
            onDismissRequest = { showHistoryBookmarksModal = false },
            confirmButton = {
                TextButton(
                    onClick = { showHistoryBookmarksModal = false }
                ) {
                    Text("关闭", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // History Log tab select
                    Column(
                        modifier = Modifier
                            .clickable { activeTabIdx = 0 }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "搜研历史",
                            fontWeight = if (activeTabIdx == 0) FontWeight.Black else FontWeight.Normal,
                            color = if (activeTabIdx == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            fontSize = 15.sp
                        )
                        if (activeTabIdx == 0) {
                            Box(modifier = Modifier.size(24.dp, 3.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                        }
                    }

                    // Bookmarks tab select
                    Column(
                        modifier = Modifier
                            .clickable { activeTabIdx = 1 }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "极简收藏",
                            fontWeight = if (activeTabIdx == 1) FontWeight.Black else FontWeight.Normal,
                            color = if (activeTabIdx == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            fontSize = 15.sp
                        )
                        if (activeTabIdx == 1) {
                            Box(modifier = Modifier.size(24.dp, 3.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                        }
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    if (activeTabIdx == 0) {
                        // History Log contents
                        if (history.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("无痕探寻 / No active history", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                                }
                            }
                        } else {
                            Column {
                                // Clear History trigger
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { viewModel.clearHistory() }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("清空历史", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(history) { entry ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                                                .clickable {
                                                    viewModel.loadUrlInActiveTab(entry.url)
                                                    showHistoryBookmarksModal = false
                                                }
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = entry.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(text = entry.url, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }

                                            IconButton(onClick = { viewModel.deleteHistoryEntry(entry.id) }, modifier = Modifier.size(24.dp)) {
                                                Icon(imageVector = Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Bookmark contents
                        if (bookmarks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Outlined.StarBorder, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("虚位以待 / Create some bookmarks", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(bookmarks) { bookmark ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                                            .clickable {
                                                viewModel.loadUrlInActiveTab(bookmark.url)
                                                showHistoryBookmarksModal = false
                                            }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = bookmark.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(text = bookmark.url, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }

                                        IconButton(onClick = { viewModel.deleteBookmark(bookmark) }, modifier = Modifier.size(24.dp)) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
