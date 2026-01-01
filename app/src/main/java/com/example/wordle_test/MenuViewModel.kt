package com.example.wordle_test

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

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
            val wins = repository.getTotalWins()
            val losses = repository.getTotalLosses()
            val winRate = repository.getWinRate()
            val avgAttempts = repository.getAverageAttempts()
            val streak = repository.getStreak()
            val rank = repository.getPlayerRank()

            _playerStats.value = PlayerStats(
                totalWins = wins,
                totalLosses = losses,
                winRate = winRate,
                averageAttempts = avgAttempts,
                currentStreak = streak,
                rank = rank
            )
        }
    }

    private fun checkDailyGameToday() {
        viewModelScope.launch {
            val dailyGame = repository.getDailyGameToday()
            _dailyGamePlayed.value = dailyGame != null
        }
    }

    fun refreshStats() {
        loadPlayerStats()
        checkDailyGameToday()
    }
}
