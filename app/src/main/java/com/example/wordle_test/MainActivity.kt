package com.example.wordle_test

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
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
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.wordle_test.WordleDatabase
import com.example.wordle_test.GameRepository
import com.example.wordle_test.ui.theme.Wordle_TestTheme

class MainActivity : ComponentActivity() {
    private lateinit var db: WordleDatabase
    private lateinit var repository: GameRepository
    private lateinit var gameViewModel: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Database
        db = Room.databaseBuilder(
            applicationContext,
            WordleDatabase::class.java,
            WordleDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

        repository = GameRepository(db.gameStatisticDao())
        val wordBank = resources.getStringArray(R.array.words)

        val isDailyGame = intent.getBooleanExtra("isDailyGame", false)
        val dailyWord = intent.getStringExtra("dailyWord")

        gameViewModel = ViewModelProvider(
            this,
            GameViewModelFactory(wordBank, repository, isDailyGame, dailyWord)
        ).get(GameViewModel::class.java)

        gameViewModel.startNewGame()

        setContent {
            Wordle_TestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameScreen(
                        gameViewModel = gameViewModel,
                        onBackToMenu = { goBackToMenu() }
                    )
                }
            }
        }
    }

    private fun goBackToMenu() {
        val intent = Intent(this, MenuActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    gameViewModel: GameViewModel,
    onBackToMenu: () -> Unit
) {
    var userGuess by rememberSaveable { mutableStateOf("") }
    val pastGuesses by gameViewModel.guessLive.observeAsState(mutableListOf())
    val errorMessage by gameViewModel.errorMessageLive.observeAsState("")
    val gameState by gameViewModel.gameStateLive.observeAsState(WordleGameState.MAIN)
    val gameResult by gameViewModel.gameResult.observeAsState(null)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WORDLE ID",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onBackToMenu) {
                Icon(Icons.Default.Home, contentDescription = "Back to Menu", modifier = Modifier.size(28.dp))
            }
        }

        // Game Content
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Input Field (hanya tampil saat MAIN)
            if (gameState == WordleGameState.MAIN) {
                item {
                    OutlinedTextField(
                        value = userGuess,
                        singleLine = true,
                        onValueChange = { text ->
                            gameViewModel.clearErrorMessage()
                            if (text.length <= 5) {
                                userGuess = text.uppercase()
                            }
                        },
                        label = { Text("Tebak kata (5 huruf)") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (userGuess.length == 5) {
                                    gameViewModel.submitGuess(userGuess)
                                    userGuess = ""
                                }
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Error Message
            if (errorMessage.isNotEmpty()) {
                item {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Guesses Display
            items(pastGuesses.size) { index ->
                WordleGuessRow(guess = pastGuesses[index])
            }

            // Game Over Screen
            if (gameState != WordleGameState.MAIN) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    GameOverScreen(
                        gameState = gameState,
                        gameResult = gameResult,
                        onPlayAgain = {
                            gameViewModel.startNewGame()
                            userGuess = ""
                        },
                        onBackToMenu = onBackToMenu
                    )
                }
            }
        }

        // Keyboard di bawah
        if (gameState == WordleGameState.MAIN) {
            WordleKeyboard(
                onLetterClick = { letter ->
                    if (userGuess.length < 5) {
                        userGuess += letter
                    }
                },
                onBackspace = {
                    if (userGuess.isNotEmpty()) {
                        userGuess = userGuess.dropLast(1)
                    }
                },
                onSubmit = {
                    if (userGuess.length == 5) {
                        gameViewModel.submitGuess(userGuess)
                        userGuess = ""
                    }
                },
                guessedLetters = pastGuesses.flatMap { guess ->
                    guess.chars.mapNotNull { it.character }
                }.toSet(),
                correctLetters = pastGuesses.flatMap { guess ->
                    guess.chars.filter { it.isInCorrectPlace }
                        .mapNotNull { it.character }
                }.toSet(),
                wrongPositionLetters = pastGuesses.flatMap { guess ->
                    guess.chars.filter { it.isInWord && !it.isInCorrectPlace }
                        .mapNotNull { it.character }
                }.toSet(),
                notInWordLetters = pastGuesses.flatMap { guess ->
                    guess.chars.filter { !it.isInWord }
                        .mapNotNull { it.character }
                }.toSet()
            )
        }
    }
}

@Composable
fun WordleKeyboard(
    onLetterClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    guessedLetters: Set<Char>,
    correctLetters: Set<Char>,
    wrongPositionLetters: Set<Char>,
    notInWordLetters: Set<Char>
) {
    val row1 = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
    val row2 = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
    val row3 = listOf("Z", "X", "C", "V", "B", "N", "M")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF121213))
            .padding(horizontal = 6.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            row1.forEach { letter ->
                KeyboardButton(
                    text = letter,
                    onClick = { onLetterClick(letter) },
                    isGuessed = letter[0] in guessedLetters,
                    isCorrect = letter[0] in correctLetters,
                    isWrongPosition = letter[0] in wrongPositionLetters,
                    isNotInWord = letter[0] in notInWordLetters,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                )
            }
        }

        // Row 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            row2.forEach { letter ->
                KeyboardButton(
                    text = letter,
                    onClick = { onLetterClick(letter) },
                    isGuessed = letter[0] in guessedLetters,
                    isCorrect = letter[0] in correctLetters,
                    isWrongPosition = letter[0] in wrongPositionLetters,
                    isNotInWord = letter[0] in notInWordLetters,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                )
            }
        }

        // Row 3 with action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .weight(1.5f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF538D4E)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("ENTER", fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Color.White)
            }

            row3.forEach { letter ->
                KeyboardButton(
                    text = letter,
                    onClick = { onLetterClick(letter) },
                    isGuessed = letter[0] in guessedLetters,
                    isCorrect = letter[0] in correctLetters,
                    isWrongPosition = letter[0] in wrongPositionLetters,
                    isNotInWord = letter[0] in notInWordLetters,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                )
            }

            Button(
                onClick = onBackspace,
                modifier = Modifier
                    .weight(1.5f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF818384)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("⌫", fontSize = 20.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun KeyboardButton(
    text: String,
    onClick: () -> Unit,
    isGuessed: Boolean,
    isCorrect: Boolean,
    isWrongPosition: Boolean,
    isNotInWord: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isCorrect -> Color(0xFF538D4E)       // Hijau (Wordle green)
        isWrongPosition -> Color(0xFFB59F3B) // Kuning (Wordle yellow)
        isNotInWord -> Color(0xFF3A3A3C)     // Abu-abu gelap
        else -> Color(0xFF818384)             // Default abu-abu terang
    }

    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = bgColor),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun GameOverScreen(
    gameState: WordleGameState,
    gameResult: GameViewModel.GameResultInfo?,
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (gameState == WordleGameState.MENANG) {
            Text(
                text = "🎉 SELAMAT! 🎉",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF6AAA64)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Katanya: ${gameResult?.word}",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Rank: ${gameResult?.rank}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6AAA64)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Percobaan: ${gameResult?.attempts}/6",
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Text(
                text = "😔 KALAH",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFFFF6B6B)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Katanya: ${gameResult?.word}",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Percobaan: ${gameResult?.attempts}/6",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPlayAgain,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Main Lagi")
            }

            Button(
                onClick = onBackToMenu,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Menu")
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
        characterGuess.isInWord -> Color(0xFFC9B458) // kuning Wordle
        else -> Color(0xFF787C7E) // abu
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
                textAlign = TextAlign.Center,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}
