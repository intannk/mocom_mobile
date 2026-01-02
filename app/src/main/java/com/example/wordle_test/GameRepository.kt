package com.example.wordle_test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class GameRepository(private val dao: GameStatisticDao) {

    // Get total wins
    suspend fun getTotalWins(): Int = withContext(Dispatchers.IO) {
        try {
            val stats = dao.getAllGameStatistics()
            stats.count { it.isWon }
        } catch (e: Exception) {
            0
        }
    }

    // Get total losses
    suspend fun getTotalLosses(): Int = withContext(Dispatchers.IO) {
        try {
            val stats = dao.getAllGameStatistics()
            stats.count { !it.isWon }
        } catch (e: Exception) {
            0
        }
    }

    // Get win rate
    suspend fun getWinRate(): Double = withContext(Dispatchers.IO) {
        try {
            val stats = dao.getAllGameStatistics()
            if (stats.isEmpty()) {
                return@withContext 0.0
            }
            val wins = stats.count { it.isWon }
            (wins.toDouble() / stats.size) * 100.0
        } catch (e: Exception) {
            0.0
        }
    }

    // Get average attempts (only for won games)
    suspend fun getAverageAttempts(): Double = withContext(Dispatchers.IO) {
        try {
            val stats = dao.getAllGameStatistics()
            val wonGames = stats.filter { it.isWon }

            if (wonGames.isEmpty()) {
                return@withContext 0.0
            }

            val totalAttempts = wonGames.sumOf { it.attempts }
            totalAttempts.toDouble() / wonGames.size
        } catch (e: Exception) {
            0.0
        }
    }

    // Get current winning streak
    suspend fun getStreak(): Int = withContext(Dispatchers.IO) {
        try {
            val stats = dao.getAllGameStatistics()
            if (stats.isEmpty()) {
                return@withContext 0
            }

            // Start from most recent and count consecutive wins
            var streak = 0
            for (stat in stats.sortedByDescending { it.id }) {
                if (stat.isWon) {
                    streak++
                } else {
                    break
                }
            }
            streak
        } catch (e: Exception) {
            0
        }
    }

    // Calculate player rank
    suspend fun getPlayerRank(): String = withContext(Dispatchers.IO) {
        try {
            val wins = getTotalWins()
            when {
                wins >= 100 -> "Master"
                wins >= 50 -> "Expert"
                wins >= 30 -> "Advanced"
                wins >= 15 -> "Intermediate"
                wins >= 5 -> "Beginner"
                else -> "Newbie"
            }
        } catch (e: Exception) {
            "Newbie"
        }
    }

    // Check if daily game was played today
    suspend fun getDailyGamePlayed(date: LocalDate): Boolean = withContext(Dispatchers.IO) {
        try {
            val stats = dao.getAllGameStatistics()
            stats.any {
                it.gameType == "DAILY" &&
                        it.date == date
            }
        } catch (e: Exception) {
            false
        }
    }

    // Save game result
    suspend fun saveGameResult(
        word: String,
        isWon: Boolean,
        attempts: Int,
        gameType: String = "NORMAL",
        playedDate: LocalDate = LocalDate.now()
    ) = withContext(Dispatchers.IO) {
        try {
            val statistic = GameStatistic(
                word = word,
                date = playedDate,
                isWon = isWon,
                attempts = attempts,
                gameType = gameType
            )
            dao.insertGameStatistic(statistic)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Get all game statistics
    suspend fun getAllGameStatistics() = withContext(Dispatchers.IO) {
        try {
            dao.getAllGameStatistics()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
