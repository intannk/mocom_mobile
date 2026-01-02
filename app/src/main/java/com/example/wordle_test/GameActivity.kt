package com.example.wordle_test

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.gridlayout.widget.GridLayout

class GameActivity : ComponentActivity() {

    private enum class LetterState {
        CORRECT, PRESENT, ABSENT
    }

    // state game
    private lateinit var words: List<String>
    private var currentGuess = ""
    private var guessCount = 0
    private val maxGuesses = 6
    private lateinit var answerWord: String
    private var gameOver = false

    // view
    private lateinit var gridGuesses: GridLayout
    private lateinit var keyboardContainer: LinearLayout
    private lateinit var messageText: TextView
    private lateinit var guessCountText: TextView
    private lateinit var titleText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // ambil daftar kata 5 huruf dari strings.xml
        words = resources.getStringArray(R.array.words)
            .map { it.trim().uppercase() }

        answerWord = words.random()

        gridGuesses = findViewById(R.id.grid_guesses)
        keyboardContainer = findViewById(R.id.keyboard_container)
        messageText = findViewById(R.id.message_text)
        guessCountText = findViewById(R.id.guess_count_text)
        titleText = findViewById(R.id.title_text)

        setupUI()
    }

    private fun setupUI() {
        setupGrid()
        setupKeyboard()
        updateGuessDisplay()
    }

    private fun setupGrid() {
        gridGuesses.columnCount = 5
        gridGuesses.rowCount = maxGuesses
        gridGuesses.removeAllViews()

        repeat(maxGuesses * 5) { index ->
            val tile = TextView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    leftMargin = 4
                    topMargin = 4
                    rightMargin = 4
                    bottomMargin = 4
                }
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24f)
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.tile_background)
                tag = "tile_$index"
            }
            gridGuesses.addView(tile)
        }
    }

    private fun setupKeyboard() {
        keyboardContainer.removeAllViews()

        val rows = listOf(
            "QWERTYUIOP".chunked(1),
            "ASDFGHJKL".chunked(1),
            listOf("DEL") + "ZXCVBNM".chunked(1) + listOf("OK")
        )

        for (row in rows) {
            val rowLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    48
                ).apply { bottomMargin = 6 }
                orientation = LinearLayout.HORIZONTAL
                weightSum = row.size.toFloat()
            }

            for (key in row) {
                val btn = Button(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1f
                    ).apply { marginEnd = 4 }
                    text = key
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
                    setTypeface(null, Typeface.BOLD)
                    setBackgroundResource(R.drawable.key_background)
                    tag = "key_$key"
                    setOnClickListener { onKeyPressed(key) }
                }
                rowLayout.addView(btn)
            }
            keyboardContainer.addView(rowLayout)
        }
    }

    private fun onKeyPressed(key: String) {
        if (gameOver) return

        when {
            key == "DEL" -> {
                if (currentGuess.isNotEmpty()) {
                    currentGuess = currentGuess.dropLast(1)
                    updateGuessDisplay()
                }
            }
            key == "OK" -> submitGuess()
            currentGuess.length < 5 -> {
                currentGuess += key
                updateGuessDisplay()
            }
        }
    }

    private fun updateGuessDisplay() {
        for (i in 0 until 5) {
            val tile = gridGuesses.findViewWithTag<TextView>(
                "tile_${guessCount * 5 + i}"
            )
            tile?.text = if (i < currentGuess.length) currentGuess[i].toString() else ""
        }
    }

    private fun submitGuess() {
        if (currentGuess.length != 5) {
            messageText.text = "Kata harus 5 huruf!"
            messageText.setTextColor(Color.parseColor("#FF6B6B"))
            return
        }

        if (currentGuess !in words) {
            messageText.text = "Kata tidak valid!"
            messageText.setTextColor(Color.parseColor("#FF6B6B"))
            return
        }

        val results = checkGuess(currentGuess, answerWord)
        updateTileColors(results)
        updateKeyboardColors(results)

        if (currentGuess == answerWord) {
            gameOver = true
            messageText.text = "🎉 MENANG! Jawaban: $answerWord"
            messageText.setTextColor(Color.parseColor("#6AAA64"))
            return
        }

        guessCount++
        guessCountText.text = "Tebakan: $guessCount / $maxGuesses"

        if (guessCount >= maxGuesses) {
            gameOver = true
            messageText.text = "😢 KALAH! Jawaban: $answerWord"
            messageText.setTextColor(Color.parseColor("#FF6B6B"))
            return
        }

        currentGuess = ""
        updateGuessDisplay()
        messageText.text = ""
    }

    private fun checkGuess(guess: String, answer: String): List<LetterState> {
        val result = MutableList(guess.length) { LetterState.ABSENT }
        val answerChars = answer.toMutableList()

        // hijau
        for (i in guess.indices) {
            if (guess[i] == answer[i]) {
                result[i] = LetterState.CORRECT
                answerChars[i] = '\u0000'
            }
        }

        // kuning
        for (i in guess.indices) {
            if (result[i] == LetterState.ABSENT) {
                val idx = answerChars.indexOf(guess[i])
                if (idx != -1) {
                    result[i] = LetterState.PRESENT
                    answerChars[idx] = '\u0000'
                }
            }
        }

        return result
    }

    private fun updateTileColors(results: List<LetterState>) {
        for (i in results.indices) {
            val tile = gridGuesses.findViewWithTag<TextView>(
                "tile_${guessCount * 5 + i}"
            ) ?: continue

            val color = when (results[i]) {
                LetterState.CORRECT -> Color.parseColor("#6AAA64")
                LetterState.PRESENT -> Color.parseColor("#C9B458")
                LetterState.ABSENT  -> Color.parseColor("#787C7E")
            }
            tile.setBackgroundColor(color)
        }
    }

    private fun updateKeyboardColors(results: List<LetterState>) {
        for (i in results.indices) {
            val letter = currentGuess[i]
            val key = keyboardContainer.findViewWithTag<Button>("key_$letter") ?: continue

            val color = when (results[i]) {
                LetterState.CORRECT -> Color.parseColor("#6AAA64")
                LetterState.PRESENT -> Color.parseColor("#C9B458")
                LetterState.ABSENT  -> Color.parseColor("#787C7E")
            }
            key.setBackgroundColor(color)
        }
    }
}
