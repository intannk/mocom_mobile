package com.example.wordle_test

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.wordle_test.WordleDatabase
import com.example.wordle_test.GameRepository
import com.example.wordle_test.ui.theme.Wordle_TestTheme
import java.time.LocalDate

class MenuActivity : ComponentActivity() {
    private lateinit var db: WordleDatabase
    private lateinit var repository: GameRepository
    private lateinit var menuViewModel: MenuViewModel

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

        menuViewModel = ViewModelProvider(
            this,
            MenuViewModelFactory(repository)
        ).get(MenuViewModel::class.java)

        setContent {
            Wordle_TestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MenuScreen(
                        menuViewModel = menuViewModel,
                        wordBank = wordBank,
                        onPlayNormal = { startGame(false, null) },
                        onPlayDaily = { dailyWord ->
                            startGame(true, dailyWord)
                        }
                    )
                }
            }
        }
    }

    private fun startGame(isDailyGame: Boolean, dailyWord: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("isDailyGame", isDailyGame)
            if (isDailyGame && dailyWord != null) {
                putExtra("dailyWord", dailyWord)
            }
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        menuViewModel.refreshStats()
    }
}

@Composable
fun MenuScreen(
    menuViewModel: MenuViewModel,
    wordBank: Array<String>,
    onPlayNormal: () -> Unit,
    onPlayDaily: (String) -> Unit
) {
    val playerStats = menuViewModel.playerStats.observeAsState().value
    val dailyGamePlayed = menuViewModel.dailyGamePlayed.observeAsState(false).value

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = "🎮 WORDLE ID 🎮",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Indonesian Wordle Game",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Player Rank Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            color = when (playerStats?.rank) {
                                "Master" -> Color(0xFF6AAA64)
                                "Expert" -> Color(0xFFC9B458)
                                "Advanced" -> Color(0xFF787C7E)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = playerStats?.rank ?: "Newbie",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 28.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Current Rank",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }

        // Statistics Grid
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Wins and Losses
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox(
                        title = "Menang",
                        value = playerStats?.totalWins.toString(),
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFF6AAA64)
                    )
                    StatBox(
                        title = "Kalah",
                        value = playerStats?.totalLosses.toString(),
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFFF6B6B)
                    )
                }

                // Row 2: Win Rate and Streak
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox(
                        title = "Win Rate",
                        value = String.format("%.1f%%", playerStats?.winRate ?: 0.0),
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFF3B82F6)
                    )
                    StatBox(
                        title = "Streak",
                        value = playerStats?.currentStreak.toString(),
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFFB923C)
                    )
                }

                // Row 3: Average Attempts
                StatBox(
                    title = "Rata-rata Percobaan",
                    value = String.format("%.1f", playerStats?.averageAttempts ?: 0.0),
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFF8B5CF6)
                )
            }
        }

        // Daily Wordle Button
        item {
            Button(
                onClick = {
                    val dateHashCode = LocalDate.now().hashCode()
                    val index = (dateHashCode % wordBank.size).let { 
                        if (it < 0) it + wordBank.size else it 
                    }
                    val dailyWord = wordBank[index]
                    onPlayDaily(dailyWord)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                enabled = !dailyGamePlayed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6AAA64),
                    disabledContainerColor = Color(0xFF787C7E)
                )
            ) {
                Text(
                    text = if (dailyGamePlayed) "✓ Daily Wordle Sudah Dimainkan" else "🌟 Daily Wordle",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Play Normal Wordle Button
        item {
            Button(
                onClick = onPlayNormal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6)
                )
            ) {
                Text(
                    text = "▶ Main Wordle",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Rank Distribution
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "🏆 Rank System",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    RankInfoItem("Newbie", "0 Kemenangan")
                    RankInfoItem("Beginner", "5+ Kemenangan")
                    RankInfoItem("Intermediate", "15+ Kemenangan")
                    RankInfoItem("Advanced", "30+ Kemenangan")
                    RankInfoItem("Expert", "50+ Kemenangan")
                    RankInfoItem("Master", "100+ Kemenangan")
                }
            }
        }

        // Spacer at bottom
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .background(backgroundColor)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
    }
}

@Composable
fun RankInfoItem(rank: String, requirement: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = rank,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = requirement,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
