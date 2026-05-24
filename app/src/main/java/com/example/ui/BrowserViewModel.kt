package com.example.ui

import android.app.Application
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.graphics.Bitmap
import android.widget.FrameLayout
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class SearchEngine(
    val id: String,
    val displayName: String,
    val queryUrl: String,
    val baseUrl: String,
    val logoChar: String,
    val hexColor: Long
) {
    GOOGLE("google", "Google", "https://www.google.com/search?q=", "google.com", "G", 0xFF4285F4),
    BING("bing", "Bing", "https://www.bing.com/search?q=", "bing.com", "B", 0xFF008080),
    DUCKDUCKGO("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=", "duckduckgo.com", "D", 0xFFDE5833),
    BAIDU("baidu", "百度", "https://www.baidu.com/s?wd=", "baidu.com", "度", 0xFF2319DC)
}

enum class FilterMode(val label: String, val color: Long) {
    NONE("原画", 0x00000000),
    AMBER("琥珀暖熙", 0x24FF9800), // warm amber screen tint
    FOREST("松绿幽护", 0x184CAF50), // soft forest green-comfort 
    EINK("温润墨色", 0x2D212121)   // paper gray-eink tone
}

data class TabModel(
    val id: String,
    val title: String,
    val url: String,
    val progress: Int = 0,
    val isHome: Boolean = true,
    val scrollProgress: Float = 0f, // 0.0 to 1.0 representing scroll percentage
    val themeColor: Long = 0xFFFAF8F5 // matching our default premium ivory color
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BrowserDatabase.getDatabase(application)
    private val repository = BrowserRepository(db)

    // Active in-memory Tabs
    private val _tabs = MutableStateFlow<List<TabModel>>(emptyList())
    val tabs = _tabs.asStateFlow()

    private val _selectedTabId = MutableStateFlow<String>("")
    val selectedTabId = _selectedTabId.asStateFlow()

    // Active Split Tab Id (Null means split mode is inactive)
    private val _splitTabId = MutableStateFlow<String?>(null)
    val splitTabId = _splitTabId.asStateFlow()

    // Eye Comfort Tint Mode
    private val _filterMode = MutableStateFlow(FilterMode.NONE)
    val filterMode = _filterMode.asStateFlow()

    // Desktop mode tracking per tab
    private val _desktopTabs = MutableStateFlow<Set<String>>(emptySet())
    val desktopTabs = _desktopTabs.asStateFlow()

    // Font scaling textZoom per tab
    private val _textZoomTabs = MutableStateFlow<Map<String, Int>>(emptyMap())
    val textZoomTabs = _textZoomTabs.asStateFlow()

    // Active webview cache mapping tabId to WebView
    private val webViewCache = mutableMapOf<String, WebView>()

    // Current Search Engine
    private val _selectedEngine = MutableStateFlow(SearchEngine.GOOGLE)
    val selectedEngine = _selectedEngine.asStateFlow()

    // Clipboard suggest url
    private val _clipboardUrl = MutableStateFlow<String?>(null)
    val clipboardUrl = _clipboardUrl.asStateFlow()

    // Database flows
    val bookmarks = repository.allBookmarks.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val history = repository.allHistory.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    init {
        // Load persist tabs if some existed, otherwise create initial tab
        viewModelScope.launch {
            repository.allTabs.first().let { savedTabs ->
                if (savedTabs.isNotEmpty()) {
                    val loaded = savedTabs.map { entity ->
                        TabModel(
                            id = entity.id,
                            title = entity.title,
                            url = entity.url,
                            isHome = entity.url.isEmpty() || entity.url == "about:blank"
                        )
                    }
                    _tabs.value = loaded
                    val selected = savedTabs.firstOrNull { it.isSelected } ?: savedTabs.first()
                    _selectedTabId.value = selected.id
                } else {
                    createNewTab()
                }
            }
        }
    }

    // Helper to extract clean site name
    fun getCleanDomain(urlStr: String): String {
        return try {
            val stripped = urlStr.replace("https://", "").replace("http://", "").replace("www.", "")
            val index = stripped.indexOf('/')
            if (index != -1) stripped.substring(0, index) else stripped
        } catch (e: Exception) {
            "New Web"
        }
    }

    // Tab Management
    fun createNewTab(initialUrl: String = "") {
        val newId = UUID.randomUUID().toString()
        val isHome = initialUrl.isEmpty() || initialUrl == "about:blank"
        val newTab = TabModel(
            id = newId,
            title = if (isHome) "Aura Home" else getCleanDomain(initialUrl),
            url = initialUrl,
            isHome = isHome
        )
        val currentList = _tabs.value.toMutableList()
        currentList.add(newTab)
        _tabs.value = currentList
        _selectedTabId.value = newId
        persistTabs()
    }

    fun selectTab(tabId: String) {
        if (_selectedTabId.value == tabId) return
        _selectedTabId.value = tabId
        persistTabs()
    }

    fun closeTab(tabId: String) {
        val currentList = _tabs.value.toMutableList()
        if (currentList.size <= 1) {
            // Last tab setup back to home
            val lastId = currentList.firstOrNull()?.id ?: return
            destroyWebView(lastId)
            currentList.clear()
            _tabs.value = currentList
            createNewTab()
            return
        }

        val idx = currentList.indexOfFirst { it.id == tabId }
        if (idx != -1) {
            destroyWebView(tabId)
            currentList.removeAt(idx)
            _tabs.value = currentList
            if (_selectedTabId.value == tabId) {
                val newActiveIndex = if (idx >= currentList.size) currentList.size - 1 else idx
                _selectedTabId.value = currentList[newActiveIndex].id
            }
            persistTabs()
        }
    }

    fun clearAllTabs() {
        // Destroy all WebViews
        webViewCache.forEach { (id, view) ->
            try {
                view.destroy()
            } catch (e: Exception) {}
        }
        webViewCache.clear()
        _tabs.value = emptyList()
        createNewTab()
    }

    private fun destroyWebView(tabId: String) {
        val webView = webViewCache.remove(tabId)
        webView?.post {
            try {
                webView.stopLoading()
                webView.clearHistory()
                webView.loadUrl("about:blank")
                webView.destroy()
            } catch (e: Exception) {}
        }
    }

    private fun persistTabs() {
        viewModelScope.launch {
            repository.clearTabs()
            val entities = _tabs.value.map { tab ->
                TabState(
                    id = tab.id,
                    title = tab.title,
                    url = tab.url,
                    isSelected = tab.id == _selectedTabId.value
                )
            }
            repository.saveTabs(entities)
        }
    }

    // Web Navigation & Url Input Process
    fun loadUrlInActiveTab(input: String) {
        val activeId = _selectedTabId.value
        if (activeId.isEmpty()) return

        var processedUrl = input.trim()
        if (processedUrl.isEmpty()) return

        // Check if it's a valid URL or needs search
        val isUrl = (processedUrl.startsWith("http://") || processedUrl.startsWith("https://")) ||
                (processedUrl.contains(".") && !processedUrl.contains(" ") && processedUrl.indexOf('.') < processedUrl.length - 1)

        if (!isUrl) {
            // Build search link
            processedUrl = _selectedEngine.value.queryUrl + java.net.URLEncoder.encode(processedUrl, "UTF-8")
        } else if (!processedUrl.startsWith("http://") && !processedUrl.startsWith("https://")) {
            processedUrl = "https://$processedUrl"
        }

        updateActiveTabUrl(processedUrl)
        val cachedWebView = webViewCache[activeId]
        if (cachedWebView != null) {
            cachedWebView.loadUrl(processedUrl)
        }
    }

    private fun updateActiveTabUrl(newUrl: String) {
        val activeId = _selectedTabId.value
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == activeId) {
                tab.copy(
                    url = newUrl,
                    isHome = newUrl.isEmpty() || newUrl == "about:blank",
                    title = if (newUrl.isEmpty() || newUrl == "about:blank") "Aura Home" else getCleanDomain(newUrl)
                )
            } else tab
        }
        persistTabs()
    }

    fun navigateBack() {
        val activeId = _selectedTabId.value
        val webView = webViewCache[activeId]
        if (webView?.canGoBack() == true) {
            webView.goBack()
        }
    }

    fun navigateForward() {
        val activeId = _selectedTabId.value
        val webView = webViewCache[activeId]
        if (webView?.canGoForward() == true) {
            webView.goForward()
        }
    }

    fun reloadActiveTab() {
        val activeId = _selectedTabId.value
        webViewCache[activeId]?.reload()
    }

    fun goHome() {
        val activeId = _selectedTabId.value
        if (activeId.isNotEmpty()) {
            _tabs.value = _tabs.value.map { tab ->
                if (tab.id == activeId) {
                    tab.copy(url = "", isHome = true, title = "Aura Home")
                } else tab
            }
            webViewCache[activeId]?.loadUrl("about:blank")
            persistTabs()
        }
    }

    // Engine Selection Dial
    fun selectSearchEngine(engine: SearchEngine) {
        _selectedEngine.value = engine
    }

    // Clipboard Suggestions
    fun detectClipboardUrl(context: Context) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            if (clipboard.hasPrimaryClip()) {
                val clipData = clipboard.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text?.toString() ?: ""
                    if (text.startsWith("http://") || text.startsWith("https://") ||
                        (text.contains(".") && !text.contains(" ") && text.length > 4)) {
                        _clipboardUrl.value = text
                        return
                    }
                }
            }
        } catch (e: Exception) {}
        _clipboardUrl.value = null
    }

    fun clearClipboardSuggestion() {
        _clipboardUrl.value = null
    }

    // Bookmarks and History DB ops
    fun toggleBookmarkOfActiveTab() {
        val activeId = _selectedTabId.value
        val tab = _tabs.value.firstOrNull { it.id == activeId } ?: return
        if (tab.isHome || tab.url.isEmpty()) return

        viewModelScope.launch {
            repository.isBookmarked(tab.url).first().let { alreadyBookmarked ->
                if (alreadyBookmarked) {
                    repository.removeBookmarkByUrl(tab.url)
                } else {
                    repository.addBookmark(tab.title, tab.url)
                }
            }
        }
    }

    fun isBookmarkedLive(url: String): Flow<Boolean> {
        return repository.isBookmarked(url)
    }

    fun addBookmarkManual(title: String, url: String) {
        viewModelScope.launch {
            repository.addBookmark(title, url)
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.removeBookmark(bookmark)
        }
    }

    fun deleteHistoryEntry(id: Int) {
        viewModelScope.launch {
            repository.deleteHistoryById(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // WebView management & events binding
    fun getWebViewForTab(tabId: String, context: Context): WebView? {
        if (tabId.isEmpty()) return null
        var webView = webViewCache[tabId]
        if (webView == null) {
            webView = createWebView(tabId, context)
            webViewCache[tabId] = webView
        }
        return webView
    }

    private fun createWebView(tabId: String, context: Context): WebView {
        val webView = WebView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            val currentDesktop = _desktopTabs.value.contains(tabId)
            if (currentDesktop) {
                settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36"
            }
            settings.textZoom = _textZoomTabs.value[tabId] ?: 100

            // Client settings
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, urlStr: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, urlStr, favicon)
                    urlStr?.let { safeUrl ->
                        updateTabState(tabId, url = safeUrl, isHome = safeUrl == "about:blank" || safeUrl.isEmpty())
                    }
                }

                override fun onPageFinished(view: WebView?, urlStr: String?) {
                    super.onPageFinished(view, urlStr)
                    val pageTitle = view?.title ?: "Web Page"
                    urlStr?.let { safeUrl ->
                        if (safeUrl != "about:blank" && safeUrl.isNotEmpty()) {
                            updateTabState(tabId, title = pageTitle, url = safeUrl)
                            // Record to History
                            viewModelScope.launch {
                                repository.addHistory(pageTitle, safeUrl)
                            }
                        }
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    updateTabProgress(tabId, newProgress)
                }
            }

            // High Quality Design - Let's listen to horizontal/vertical scroll to trigger reading scroll indicators in main composable.
            // Custom WebView scroll tracking
            setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                // Approximate current scroll percentage
                val contentHeight = contentHeight * scale
                val viewHeight = height
                if (contentHeight > viewHeight) {
                    val ratio = scrollY.toFloat() / (contentHeight - viewHeight)
                    updateTabScrollProgress(tabId, ratio.coerceIn(0f, 1f))
                }
            }
        }
        val currentTab = _tabs.value.firstOrNull { it.id == tabId }
        if (currentTab != null && currentTab.url.isNotEmpty() && !currentTab.isHome) {
            webView.loadUrl(currentTab.url)
        }
        return webView
    }

    private fun updateTabState(tabId: String, title: String? = null, url: String? = null, isHome: Boolean? = null) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) {
                tab.copy(
                    title = title ?: tab.title,
                    url = url ?: tab.url,
                    isHome = isHome ?: tab.isHome
                )
            } else tab
        }
        persistTabs()
    }

    private fun updateTabProgress(tabId: String, progress: Int) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) {
                tab.copy(progress = progress)
            } else tab
        }
    }

    private fun updateTabScrollProgress(tabId: String, ratio: Float) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) {
                tab.copy(scrollProgress = ratio)
            } else tab
        }
    }

    // Toggle and selection helpers
    fun setSplitTabId(id: String?) {
        _splitTabId.value = id
    }

    fun setFilterMode(mode: FilterMode) {
        _filterMode.value = mode
    }

    fun toggleSplitMode() {
        if (_splitTabId.value != null) {
            _splitTabId.value = null
        } else {
            val currentSelected = _selectedTabId.value
            val otherTab = _tabs.value.firstOrNull { it.id != currentSelected }
            if (otherTab != null) {
                _splitTabId.value = otherTab.id
            } else {
                // Create a side/sub tab immediately for split dual window
                val newId = UUID.randomUUID().toString()
                val newTab = TabModel(
                    id = newId,
                    title = "主页副舱",
                    url = "",
                    isHome = true
                )
                val currentList = _tabs.value.toMutableList()
                currentList.add(newTab)
                _tabs.value = currentList
                _splitTabId.value = newId
                persistTabs()
            }
        }
    }

    fun closeSplitMode() {
        _splitTabId.value = null
    }

    fun toggleDesktopMode(tabId: String) {
        if (tabId.isEmpty()) return
        val currentSet = _desktopTabs.value.toMutableSet()
        val enable = if (currentSet.contains(tabId)) {
            currentSet.remove(tabId)
            false
        } else {
            currentSet.add(tabId)
            true
        }
        _desktopTabs.value = currentSet

        webViewCache[tabId]?.let { webView ->
            val settings = webView.settings
            if (enable) {
                settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36"
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
            } else {
                settings.userAgentString = null
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
            }
            webView.reload()
        }
    }

    fun setTextZoom(tabId: String, zoomValue: Int) {
        if (tabId.isEmpty()) return
        val currentMap = _textZoomTabs.value.toMutableMap()
        currentMap[tabId] = zoomValue
        _textZoomTabs.value = currentMap

        webViewCache[tabId]?.let { webView ->
            webView.settings.textZoom = zoomValue
        }
    }
}
