package com.example.wordle_test

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PlayerStats(
    val totalWins: Int = 0,
    val totalLosses: Int = 0,
    val winRate: Double = 0.0,
    val averageAttempts: Double = 0.0,
    val currentStreak: Int = 0,
    val rank: String = "Newbie"
)

class MenuViewModel(private val repository: GameRepository) : ViewModel() {
    private val _playerStats = MutableLiveData<PlayerStats>()
    val playerStats: LiveData<PlayerStats> get() = _playerStats

    private val _dailyGamePlayed = MutableLiveData(false)
    val dailyGamePlayed: LiveData<Boolean> get() = _dailyGamePlayed

    init {
        loadPlayerStats()
        checkDailyGameToday()
    }

    private fun loadPlayerStats() {
        viewModelScope.launch {
            try {
                val wins = repository.getTotalWins() ?: 0
                val losses = repository.getTotalLosses() ?: 0

                // Calculate winRate safely - handle null dan division by zero
                val winRate = if ((wins + losses) > 0) {
                    (wins.toDouble() / (wins + losses)) * 100.0
                } else {
                    0.0
                }

                // Get average attempts safely - handle null
                val avgAttempts = repository.getAverageAttempts() ?: 0.0

                // Get streak safely
                val streak = repository.getStreak() ?: 0

                // Calculate rank based on wins
                val rank = when {
                    wins >= 100 -> "Master"
                    wins >= 50 -> "Expert"
                    wins >= 30 -> "Advanced"
                    wins >= 15 -> "Intermediate"
                    wins >= 5 -> "Beginner"
                    else -> "Newbie"
                }

                _playerStats.value = PlayerStats(
                    totalWins = wins,
                    totalLosses = losses,
                    winRate = winRate,
                    averageAttempts = avgAttempts,
                    currentStreak = streak,
                    rank = rank
                )
            } catch (e: Exception) {
                // Handle error gracefully - set default values
                _playerStats.value = PlayerStats()
            }
        }
    }

    private fun checkDailyGameToday() {
        viewModelScope.launch {
            try {
                val todayDate = LocalDate.now()
                val dailyGame = repository.getDailyGamePlayed(todayDate)
                _dailyGamePlayed.value = dailyGame
            } catch (e: Exception) {
                _dailyGamePlayed.value = false
            }
        }
    }

    fun refreshStats() {
        loadPlayerStats()
        checkDailyGameToday()
    }
}
