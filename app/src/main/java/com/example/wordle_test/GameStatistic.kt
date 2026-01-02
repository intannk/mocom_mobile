package com.example.wordle_test

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "game_statistics")
data class GameStatistic(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "date")
    val date: LocalDate,
    @ColumnInfo(name = "word")
    val word: String,
    @ColumnInfo(name = "is_won")
    val isWon: Boolean,
    @ColumnInfo(name = "attempts")
    val attempts: Int,
    @ColumnInfo(name = "game_type") // "DAILY" or "NORMAL"
    val gameType: String = "NORMAL"
) {
    fun getRank(): String {
        return when {
            attempts == 1 -> "Genius"
            attempts == 2 -> "Magnificent"
            attempts == 3 -> "Impressive"
            attempts == 4 -> "Splendid"
            attempts == 5 -> "Great"
            attempts == 6 -> "Phew"
            else -> "Unknown"
        }
    }
}
