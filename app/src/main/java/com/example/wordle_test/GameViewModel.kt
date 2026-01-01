package com.example.wordle_test

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.util.Log

class GameViewModel(
    private val wordBank: Array<String>,
    private val repository: GameRepository,
    private val isDailyGame: Boolean = false,
    private val dailyWord: String? = null
) : ViewModel() {

    private val _word = MutableLiveData("")
    val wordLive: LiveData<String> get() = _word

    private val _guesses = MutableLiveData(mutableListOf<WordleGuess>())
    val guessLive: LiveData<MutableList<WordleGuess>> get() = _guesses

    private val nonBlankGuesses: List<WordleGuess>
        get() = _guesses.value?.filter { guess ->
            guess.chars.any { it.character != null }
        } ?: emptyList()

    private val _gameStatus = MutableLiveData(WordleGameState.MAIN)
    val gameStateLive: LiveData<WordleGameState> get() = _gameStatus

    private val _errorMessage = MutableLiveData("")
    val errorMessageLive: LiveData<String> get() = _errorMessage

    private val _showStatisticsDialog = MutableLiveData(false)
    val showStatisticsDialog: LiveData<Boolean> get() = _showStatisticsDialog

    private val _gameResult = MutableLiveData<GameResultInfo?>(null)
    val gameResult: LiveData<GameResultInfo?> get() = _gameResult

    data class GameResultInfo(
        val isWon: Boolean,
        val word: String,
        val attempts: Int,
        val rank: String
    )

    fun startNewGame() {
        _word.value = if (isDailyGame && dailyWord != null) {
            dailyWord.uppercase()
        } else {
            wordBank.random().uppercase()
        }

        Log.d("WordGuess", "Target word: ${_word.value}, isDailyGame: $isDailyGame")

        _guesses.value = mutableListOf(
            WordleGuess.generateBlank(),
            WordleGuess.generateBlank(),
            WordleGuess.generateBlank(),
            WordleGuess.generateBlank(),
            WordleGuess.generateBlank(),
            WordleGuess.generateBlank()
        )

        _gameStatus.value = WordleGameState.MAIN
        _errorMessage.value = ""
    }

    fun submitGuess(guess: String) {
        val upperGuess = guess.uppercase()

        if (!wordBank.contains(upperGuess)) {
            _errorMessage.value = "Kata salah atau tidak ada di kamus."
            return
        }

        val target = _word.value ?: return

        val alreadyGuessed = nonBlankGuesses.any { g ->
            g.chars.joinToString(separator = "") { it.character?.toString() ?: "" } == upperGuess
        }

        if (alreadyGuessed) {
            _errorMessage.value = "Kamu sudah menebak kata ini."
            return
        }

        val wordleGuess = WordleGuess(arrayListOf())

        upperGuess.forEachIndexed { index, char ->
            val characterGuess = WordleCharacterGuess(
                character = char,
                isInWord = target.contains(char),
                isInCorrectPlace = target.getOrNull(index) == char
            )
            wordleGuess.chars.add(characterGuess)
        }

        val oldValues = nonBlankGuesses.toMutableList()
        oldValues.add(wordleGuess)

        val newGuesses = mutableListOf<WordleGuess>()
        newGuesses.addAll(oldValues)

        repeat(6 - newGuesses.size) {
            newGuesses.add(WordleGuess.generateBlank())
        }

        _guesses.value = newGuesses
        checkGameStatus()
    }

    private fun checkGameStatus() {
        val guessesNow = _guesses.value ?: return
        val target = _word.value ?: return

        val isWin = guessesNow.any { guess ->
            guess.chars.all { it.isInCorrectPlace }
        }

        if (isWin) {
            val attempts = nonBlankGuesses.size
            val rank = getAttemptRank(attempts)

            _gameResult.value = GameResultInfo(
                isWon = true,
                word = target,
                attempts = attempts,
                rank = rank
            )

            _gameStatus.value = WordleGameState.MENANG

            saveGameResult(isWon = true, attempts = attempts)
            return
        }

        if (nonBlankGuesses.size > 5) {
            _gameResult.value = GameResultInfo(
                isWon = false,
                word = target,
                attempts = nonBlankGuesses.size,
                rank = "Failed"
            )

            _gameStatus.value = WordleGameState.KALAH
            saveGameResult(isWon = false, attempts = nonBlankGuesses.size)
        }
    }

    private fun getAttemptRank(attempts: Int): String {
        return when (attempts) {
            1 -> "Genius"
            2 -> "Magnificent"
            3 -> "Impressive"
            4 -> "Splendid"
            5 -> "Great"
            6 -> "Phew"
            else -> "Unknown"
        }
    }

    private fun saveGameResult(isWon: Boolean, attempts: Int) {
        viewModelScope.launch {
            val word = _word.value ?: return@launch
            val gameType = if (isDailyGame) "DAILY" else "NORMAL"

            repository.saveGameResult(
                word = word,
                isWon = isWon,
                attempts = attempts,
                gameType = gameType
            )
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = ""
    }

    fun showStatisticsDialog(show: Boolean) {
        _showStatisticsDialog.value = show
    }
}

enum class WordleGameState {
    MAIN,
    MENANG,
    KALAH
}

data class WordleCharacterGuess(
    val character: Char? = null,
    val isInWord: Boolean = false,
    val isInCorrectPlace: Boolean = false
)

data class WordleGuess(
    val chars: ArrayList<WordleCharacterGuess>
) {
    companion object {
        fun generateBlank(): WordleGuess {
            val chars = arrayListOf<WordleCharacterGuess>()
            repeat(5) {
                chars.add(WordleCharacterGuess())
            }
            return WordleGuess(chars)
        }
    }
}
