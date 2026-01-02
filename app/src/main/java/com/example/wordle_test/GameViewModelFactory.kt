package com.example.wordle_test
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GameViewModelFactory(
    private val wordBank: Array<String>,
    private val repository: GameRepository,
    private val isDailyGame: Boolean = false,
    private val dailyWord: String? = null
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(GameViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                GameViewModel(wordBank, repository, isDailyGame, dailyWord) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
