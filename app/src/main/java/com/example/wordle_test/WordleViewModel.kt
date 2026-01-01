package com.example.wordle_test

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WordleViewModel(private val wordBank: Array<String>) : ViewModel() {

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

    fun startNewGame() {
        _word.value = wordBank.random().uppercase()
        Log.d("WordGuess", "Target word: ${_word.value}")

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

        val isWin = guessesNow.any { guess ->
            guess.chars.all { it.isInCorrectPlace }
        }

        if (isWin) {
            _gameStatus.value = WordleGameState.MENANG
            return
        }

        if (nonBlankGuesses.size > 5) {
            _gameStatus.value = WordleGameState.KALAH
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = ""
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
