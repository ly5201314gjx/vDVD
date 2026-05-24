package com.example.data

import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val database: BrowserDatabase) {
    private val bookmarkDao = database.bookmarkDao()
    private val historyDao = database.historyDao()
    private val tabStateDao = database.tabStateDao()

    val allBookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()
    val allHistory: Flow<List<HistoryEntry>> = historyDao.getAllHistory()
    val allTabs: Flow<List<TabState>> = tabStateDao.getAllTabs()

    suspend fun addBookmark(title: String, url: String) {
        bookmarkDao.insertBookmark(Bookmark(title = title, url = url))
    }

    suspend fun removeBookmark(bookmark: Bookmark) {
        bookmarkDao.deleteBookmark(bookmark)
    }

    suspend fun removeBookmarkByUrl(url: String) {
        bookmarkDao.deleteBookmarkByUrl(url)
    }

    fun isBookmarked(url: String): Flow<Boolean> {
        return bookmarkDao.isBookmarked(url)
    }

    suspend fun addHistory(title: String, url: String) {
        // Simple deduplication or limit check can be done, but keep it robust
        historyDao.insertHistory(HistoryEntry(title = title, url = url))
    }

    suspend fun deleteHistoryById(id: Int) {
        historyDao.deleteHistoryById(id)
    }

    suspend fun clearHistory() {
        historyDao.clearAllHistory()
    }

    suspend fun saveTab(tab: TabState) {
        tabStateDao.insertTab(tab)
    }

    suspend fun saveTabs(tabs: List<TabState>) {
        tabStateDao.insertTabs(tabs)
    }

    suspend fun deleteTabById(id: String) {
        tabStateDao.deleteTabById(id)
    }

    suspend fun clearTabs() {
        tabStateDao.clearAllTabs()
    }
}
