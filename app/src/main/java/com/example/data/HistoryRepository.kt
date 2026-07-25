package com.example.data

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {

    val allHistory: Flow<List<HistoryItem>> = historyDao.getAllHistory()
    val favoriteHistory: Flow<List<HistoryItem>> = historyDao.getFavoriteHistory()

    suspend fun insert(original: String, reshaped: String): Long {
        if (original.isBlank()) return -1L
        val item = HistoryItem(
            originalText = original,
            reshapedText = reshaped
        )
        return historyDao.insertHistory(item)
    }

    suspend fun toggleFavorite(item: HistoryItem) {
        historyDao.updateHistory(item.copy(isFavorite = !item.isFavorite))
    }

    suspend fun deleteById(id: Int) {
        historyDao.deleteHistoryById(id)
    }

    suspend fun clearAll() {
        historyDao.clearAllHistory()
    }
}
