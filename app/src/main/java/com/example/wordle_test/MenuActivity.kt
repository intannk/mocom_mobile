package com.example.wordle_test

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
                    color = Color(0xFF0A0E27)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E27),
                        Color(0xFF1A1F3A),
                        Color(0xFF0F1629),
                        Color(0xFF0A0E27)
                    )
                )
            )
    ) {
        // Background circles (static)
        Box(
            modifier = Modifier
                .offset(x = 50.dp, y = (-50).dp)
                .size(200.dp)
                .alpha(0.1f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6366F1),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 100.dp)
                .size(250.dp)
                .alpha(0.08f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFEC4899),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero Section - Logo & Visual
            item {
                HeroSection()
            }

            // Action Buttons Section
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Daily Wordle Button
                    PremiumGradientButton(
                        text = if (dailyGamePlayed) "✓ Daily Selesai" else "🌟 Daily Challenge",
                        subtitle = if (dailyGamePlayed) "Kembali besok!" else "Main sekali per hari",
                        onClick = {
                            val dateHashCode = LocalDate.now().hashCode()
                            val index = (dateHashCode % wordBank.size).let {
                                if (it < 0) it + wordBank.size else it
                            }
                            val dailyWord = wordBank[index]
                            onPlayDaily(dailyWord)
                        },
                        enabled = !dailyGamePlayed,
                        gradientColors = if (dailyGamePlayed) {
                            listOf(Color(0xFF374151), Color(0xFF1F2937))
                        } else {
                            listOf(Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF6366F1))
                        }
                    )

                    // Normal Play Button
                    PremiumGradientButton(
                        text = "⚡ Main Sekarang",
                        subtitle = "Mode bebas tanpa batas",
                        onClick = onPlayNormal,
                        gradientColors = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF6366F1))
                    )
                }
            }

            // Rank Badge
            item {
                GlowingRankCard(playerStats)
            }

            // Statistics Grid
            item {
                EnhancedStatsGrid(playerStats)
            }

            // Rank System Info
            item {
                RankSystemCard()
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun HeroSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .shadow(24.dp, RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF6366F1),
                            Color(0xFF8B5CF6),
                            Color(0xFFEC4899),
                            Color(0xFF06B6D4),
                            Color(0xFF6366F1)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // Logo
                Text(
                    text = "🎯",
                    fontSize = 64.sp
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "WORDLE",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    fontSize = 56.sp,
                    color = Color.White,
                    letterSpacing = 4.sp
                )

                Text(
                    text = "INDONESIA",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    letterSpacing = 6.sp
                )

                Spacer(Modifier.height(28.dp))

                // Wordle Grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(3) { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            repeat(5) { col ->
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .shadow(4.dp, RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                row == 0 && col < 3 -> Color(0xFF10B981)
                                                row == 0 -> Color(0xFF374151)
                                                row == 1 && col == 2 -> Color(0xFF10B981)
                                                row == 1 -> Color(0xFFF59E0B)
                                                else -> Color(0xFF1F2937)
                                            },
                                            RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            2.dp,
                                            Color.White.copy(alpha = 0.2f),
                                            RoundedCornerShape(10.dp)
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Tebak kata dalam 6 percobaan!",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumGradientButton(
    text: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradientColors: List<Color>
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .shadow(
                elevation = if (enabled) 16.dp else 4.dp,
                shape = RoundedCornerShape(20.dp)
            ),
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) {
                        Brush.horizontalGradient(gradientColors)
                    } else {
                        Brush.horizontalGradient(
                            listOf(Color(0xFF374151), Color(0xFF1F2937))
                        )
                    }
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.2f),
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun GlowingRankCard(playerStats: PlayerStats?) {
    val rankColors = when (playerStats?.rank) {
        "Master" -> listOf(Color(0xFFFFD700), Color(0xFFFF8C00), Color(0xFFFFD700))
        "Expert" -> listOf(Color(0xFFC0C0C0), Color(0xFF9CA3AF), Color(0xFFC0C0C0))
        "Advanced" -> listOf(Color(0xFFCD7F32), Color(0xFF92400E), Color(0xFFCD7F32))
        "Intermediate" -> listOf(Color(0xFF3B82F6), Color(0xFF1E40AF), Color(0xFF3B82F6))
        "Beginner" -> listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF10B981))
        else -> listOf(Color(0xFF6B7280), Color(0xFF4B5563), Color(0xFF6B7280))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(20.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(rankColors))
                .border(
                    2.dp,
                    Color.White.copy(alpha = 0.3f),
                    RoundedCornerShape(28.dp)
                )
                .fillMaxWidth()
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (playerStats?.rank) {
                        "Master" -> "👑"
                        "Expert" -> "🏆"
                        "Advanced" -> "⭐"
                        "Intermediate" -> "🎯"
                        "Beginner" -> "🌟"
                        else -> "🆕"
                    },
                    fontSize = 64.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = playerStats?.rank ?: "Newbie",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 40.sp,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "RANK KAMU SAAT INI",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
            }
        }
    }
}

@Composable
fun EnhancedStatsGrid(playerStats: PlayerStats?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlassStatBox(
                title = "Menang",
                value = playerStats?.totalWins.toString(),
                icon = "🎯",
                modifier = Modifier.weight(1f),
                gradientColors = listOf(Color(0xFF10B981), Color(0xFF059669))
            )
            GlassStatBox(
                title = "Kalah",
                value = playerStats?.totalLosses.toString(),
                icon = "💔",
                modifier = Modifier.weight(1f),
                gradientColors = listOf(Color(0xFFEF4444), Color(0xFFDC2626))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlassStatBox(
                title = "Win Rate",
                value = String.format("%.1f%%", playerStats?.winRate ?: 0.0),
                icon = "📊",
                modifier = Modifier.weight(1f),
                gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
            )
            GlassStatBox(
                title = "Streak",
                value = playerStats?.currentStreak.toString(),
                icon = "🔥",
                modifier = Modifier.weight(1f),
                gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
            )
        }

        GlassStatBox(
            title = "Rata-rata Percobaan",
            value = String.format("%.1f", playerStats?.averageAttempts ?: 0.0),
            icon = "🎲",
            modifier = Modifier.fillMaxWidth(),
            gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))
        )
    }
}

@Composable
fun GlassStatBox(
    title: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier,
    gradientColors: List<Color>
) {
    Card(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(gradientColors))
                .border(
                    1.5.dp,
                    Color.White.copy(alpha = 0.25f),
                    RoundedCornerShape(20.dp)
                )
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = icon,
                    fontSize = 36.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 32.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun RankSystemCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1F3A)
        )
    ) {
        Box(
            modifier = Modifier
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.15f),
                    RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏅",
                        fontSize = 28.sp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Sistem Peringkat",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 22.sp
                    )
                }

                Spacer(Modifier.height(20.dp))

                RankInfoItem("Newbie", "0 Kemenangan", Color(0xFF6B7280), "🆕")
                RankInfoItem("Beginner", "5+ Kemenangan", Color(0xFF10B981), "🌟")
                RankInfoItem("Intermediate", "15+ Kemenangan", Color(0xFF3B82F6), "🎯")
                RankInfoItem("Advanced", "30+ Kemenangan", Color(0xFFCD7F32), "⭐")
                RankInfoItem("Expert", "50+ Kemenangan", Color(0xFFC0C0C0), "🏆")
                RankInfoItem("Master", "100+ Kemenangan", Color(0xFFFFD700), "👑")
            }
        }
    }
}

@Composable
fun RankInfoItem(rank: String, requirement: String, rankColor: Color, icon: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        rankColor.copy(alpha = 0.15f),
                        rankColor.copy(alpha = 0.05f)
                    )
                ),
                RoundedCornerShape(14.dp)
            )
            .border(
                1.dp,
                rankColor.copy(alpha = 0.3f),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = icon,
                fontSize = 24.sp
            )
            Text(
                text = rank,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = rankColor,
                fontSize = 17.sp
            )
        }
        Text(
            text = requirement,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8),
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}