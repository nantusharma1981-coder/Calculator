package com.example.data

import kotlinx.coroutines.flow.Flow

class CalculationRepository(private val calculationDao: CalculationDao) {
    val allHistory: Flow<List<CalculationHistory>> = calculationDao.getAllHistory()

    suspend fun insert(history: CalculationHistory) {
        calculationDao.insertHistory(history)
    }

    suspend fun clearAll() {
        calculationDao.clearHistory()
    }

    suspend fun deleteById(id: Long) {
        calculationDao.deleteHistoryById(id)
    }
}
