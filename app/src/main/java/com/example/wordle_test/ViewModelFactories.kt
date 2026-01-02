package com.example.wordle_test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

//class GameViewModelFactory(
//    private val wordBank: Array<String>,
//    private val repository: GameRepository,
//    private val isDailyGame: Boolean = false,
//    private val dailyWord: String? = null
//) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        @Suppress("UNCHECKED_CAST")
//        return GameViewModel(wordBank, repository, isDailyGame, dailyWord) as T
//    }
//}

//class MenuViewModelFactory(
//    private val repository: GameRepository
//) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        @Suppress("UNCHECKED_CAST")
//        return MenuViewModel(repository) as T
//    }
//}
