package com.example.wordle_test

import androidx.room.Dao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate

class GameRepository(private val gameStatisticDao: GameStatisticDao) {

    private val _statistics = MutableStateFlow<List<GameStatistic>>(emptyList())
    val statistics: StateFlow<List<GameStatistic>> = _statistics

    suspend fun saveGameResult(
        word: String,
        isWon: Boolean,
        attempts: Int,
        gameType: String = "NORMAL"
    ) {
        val stat = GameStatistic(
            date = LocalDate.now(),
            word = word,
            isWon = isWon,
            attempts = attempts,
            gameType = gameType
        )
        gameStatisticDao.insertGameStatistic(stat)
        loadStatistics()
    }

    suspend fun loadStatistics() {
        _statistics.emit(gameStatisticDao.getAllGameStatistics())
    }

//    suspend fun getTotalWins(): Int = gameStatisticDao.getTotalWins()
//
//    suspend fun getTotalLosses(): Int = gameStatisticDao.getTotalLosses()
//
//    suspend fun getAverageAttempts(): Double = gameStatisticDao.getAverageAttempts()

    suspend fun getAverageAttempts(): Double {
        return gameStatisticDao.getAverageAttempts() ?: 0.0  // Return 0.0 kalau null
    }

    suspend fun getTotalWins(): Int {
        return try {
            gameStatisticDao.getTotalWins() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getTotalLosses(): Int {
        return try {
            gameStatisticDao.getTotalLosses() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getDailyGameToday(): GameStatistic? {
        return gameStatisticDao.getGameByDateAndType(LocalDate.now(), "DAILY")
    }

    suspend fun getPlayerRank(): String {
        val totalWins = getTotalWins()
        val totalLosses = getTotalLosses()
        val totalGames = totalWins + totalLosses

        return when {
            totalGames == 0 -> "Newbie"
            totalWins < 5 -> "Beginner"
            totalWins < 15 -> "Intermediate"
            totalWins < 30 -> "Advanced"
            totalWins < 50 -> "Expert"
            totalWins >= 50 -> "Master"
            else -> "Unknown"
        }
    }

    suspend fun getWinRate(): Double {
        val totalWins = getTotalWins()
        val totalLosses = getTotalLosses()
        val totalGames = totalWins + totalLosses

        return if (totalGames > 0) {
            (totalWins.toDouble() / totalGames) * 100
        } else {
            0.0
        }
    }

    suspend fun getStreak(): Int {
        val stats = gameStatisticDao.getAllGameStatistics()
        var streak = 0

        for (stat in stats) {
            if (stat.isWon) {
                streak++
            } else {
                break
            }
        }

        return streak
    }
}
