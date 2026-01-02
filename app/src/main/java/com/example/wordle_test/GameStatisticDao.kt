package com.example.wordle_test

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import java.time.LocalDate

@Dao
interface GameStatisticDao {
    @Insert
    suspend fun insertGameStatistic(gameStatistic: GameStatistic)

    @Query("SELECT * FROM game_statistics ORDER BY date DESC")
    suspend fun getAllGameStatistics(): List<GameStatistic>

    @Query("SELECT * FROM game_statistics WHERE date = :date AND game_type = :gameType LIMIT 1")
    suspend fun getGameByDateAndType(date: LocalDate, gameType: String): GameStatistic?

    @Query("SELECT COUNT(*) FROM game_statistics WHERE is_won = 1")
    suspend fun getTotalWins(): Int

    @Query("SELECT COUNT(*) FROM game_statistics WHERE is_won = 0")
    suspend fun getTotalLosses(): Int

    @Query("SELECT COUNT(*) FROM game_statistics WHERE date = :date AND is_won = 1")
    suspend fun getWinsByDate(date: LocalDate): Int

    @Query("SELECT AVG(attempts) FROM game_statistics WHERE is_won = 1")
    suspend fun getAverageAttempts(): Double

    @Query("SELECT * FROM game_statistics WHERE game_type = 'DAILY' ORDER BY date DESC LIMIT 1")
    suspend fun getLastDailyGame(): GameStatistic?

    @Query("SELECT DISTINCT DATE(date) FROM game_statistics WHERE game_type = 'DAILY' ORDER BY date DESC")
    suspend fun getAllDailyGameDates(): List<String>
}
