package com.example.wordle_test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wordle_test.ui.theme.Wordle_TestTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val wordBank = resources.getStringArray(R.array.words)
        val wordleViewModel = WordleViewModel(wordBank)
        wordleViewModel.startNewGame()

        setContent {
            Wordle_TestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val gameState by wordleViewModel.gameStateLive
                        .observeAsState(WordleGameState.MAIN)

                    if (gameState == WordleGameState.MAIN) {
                        WordleGame(wordleViewModel = wordleViewModel)
                    } else {
                        GameOverScreen(
                            gameState = gameState,
                            word = wordleViewModel.wordLive.value,
                            startNewGame = { wordleViewModel.startNewGame() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameOverScreen(
    gameState: WordleGameState,
    word: String?,
    startNewGame: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (gameState == WordleGameState.MENANG) {
            Text(text = "Selamat, kamu menang! Katanya: $word")
        } else {
            Text(text = "Yah, kamu kalah. Katanya: $word")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = startNewGame) {
            Text(text = "New Game")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordleGame(wordleViewModel: WordleViewModel) {
    var userGuess by rememberSaveable { mutableStateOf("") }
    val pastGuesses by wordleViewModel.guessLive.observeAsState(mutableListOf())
    val errorMessage by wordleViewModel.errorMessageLive.observeAsState("")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "WORDLE ID",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = userGuess,
            singleLine = true,
            onValueChange = { text ->
                wordleViewModel.clearErrorMessage()
                if (text.length <= 5) {
                    userGuess = text.uppercase()
                }
            },
            label = { Text("Tebak kata (5 huruf)") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (userGuess.length == 5) {
                        wordleViewModel.submitGuess(userGuess)
                        userGuess = ""
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                pastGuesses.forEach { guess ->
                    WordleGuessRow(guess = guess)
                }
            }
        }
    }
}

@Composable
fun WordleGuessRow(guess: WordleGuess) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        guess.chars.forEach { characterGuess ->
            LetterTile(characterGuess = characterGuess)
        }
    }
}

@Composable
fun LetterTile(characterGuess: WordleCharacterGuess) {
    val character = characterGuess.character

    val bgColor = when {
        character == null -> MaterialTheme.colorScheme.surfaceVariant
        characterGuess.isInCorrectPlace -> Color(0xFF6AAA64) // hijau Wordle
        characterGuess.isInWord -> Color(0xFFC9B458)        // kuning Wordle
        else -> Color(0xFF787C7E)                           // abu
    }

    Surface(
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = BorderStroke(1.dp, Color.Black)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = character?.toString() ?: "",
                fontSize = 24.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}
