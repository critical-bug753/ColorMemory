package com.yourcollege.colormemory

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

enum class GameLevel { EASY, MODERATE, DIFFICULT }

enum class GameState {
    LEVEL_SELECTION,
    PREVIEW_SEQUENCE,
    DELAY,
    GUESSING,
    RESULT,
    GAME_OVER
}

data class ColorStep(val color: Color, val id: Int)

data class GameUIState(
    val level: GameLevel = GameLevel.EASY,
    val gameState: GameState = GameState.LEVEL_SELECTION,
    val currentRound: Int = 0,
    val totalScore: Int = 0,
    val targetColor: Color = Color.White,
    val guessedColor: Color = Color.White,
    val colorSequence: List<ColorStep> = emptyList(),
    val currentSequenceIndex: Int = -1,
    val targetId: Int = -1,
    val lastRoundScore: Int = 0,
    val lastRoundTime: Long = 0,
    val countdownProgress: Float = 1f,
    val bestGuessHex: String = "#FFFFFF",
    val highScores: Map<GameLevel, Int> = emptyMap(),
    val roundScores: List<Int> = emptyList()
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ColorRepository(application)
    private val _uiState = MutableStateFlow(GameUIState())
    val uiState: StateFlow<GameUIState> = _uiState.asStateFlow()

    private var roundStartTime: Long = 0
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            val scores = GameLevel.entries.associateWith { repository.getHighScore(it).first() }
            _uiState.value = _uiState.value.copy(highScores = scores)
        }
    }

    fun selectLevel(level: GameLevel) {
        _uiState.value = _uiState.value.copy(
            level = level,
            currentRound = 1,
            totalScore = 0,
            bestGuessHex = "#FFFFFF",
            roundScores = emptyList()
        )
        startRound()
    }

    private fun startRound() {
        timerJob?.cancel()
        val level = _uiState.value.level
        if (level == GameLevel.DIFFICULT) {
            val sequence = (1..5).map { ColorStep(generateRandomColor(level), it) }
            _uiState.value = _uiState.value.copy(
                colorSequence = sequence,
                targetId = Random.nextInt(1, 6),
                gameState = GameState.PREVIEW_SEQUENCE,
                currentSequenceIndex = 0,
                countdownProgress = 1f
            )
            runDifficultSequence()
        } else {
            val target = generateRandomColor(level)
            _uiState.value = _uiState.value.copy(
                targetColor = target,
                gameState = GameState.PREVIEW_SEQUENCE,
                countdownProgress = 1f
            )
            // Preview phase timer: 5s for both as requested
            startTimer(5000L) {
                if (level == GameLevel.MODERATE) {
                    _uiState.value = _uiState.value.copy(gameState = GameState.DELAY)
                    startTimer(5000L) { enterGuessingState() }
                } else enterGuessingState()
            }
        }
    }

    private fun runDifficultSequence() {
        viewModelScope.launch {
            for (i in 0 until 5) {
                _uiState.value = _uiState.value.copy(currentSequenceIndex = i)
                startSequenceItemTimer(2000L)
            }
            _uiState.value = _uiState.value.copy(gameState = GameState.DELAY)
            startTimer(5000L) {
                val target = _uiState.value.colorSequence.find { it.id == _uiState.value.targetId }?.color ?: Color.White
                _uiState.value = _uiState.value.copy(targetColor = target)
                enterGuessingState()
            }
        }
    }

    private suspend fun startSequenceItemTimer(duration: Long) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < duration) {
            val elapsed = System.currentTimeMillis() - startTime
            _uiState.value = _uiState.value.copy(countdownProgress = 1f - (elapsed.toFloat() / duration))
            delay(16)
        }
    }

    private fun startTimer(duration: Long, onFinished: () -> Unit) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < duration) {
                val elapsed = System.currentTimeMillis() - startTime
                _uiState.value = _uiState.value.copy(countdownProgress = 1f - (elapsed.toFloat() / duration))
                delay(16)
            }
            onFinished()
        }
    }

    private fun enterGuessingState() {
        roundStartTime = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(gameState = GameState.GUESSING, countdownProgress = 1f)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            // Guessing timer now lasts 5 seconds as requested
            val duration = 5000f
            while (true) {
                val elapsed = (System.currentTimeMillis() - roundStartTime).toFloat()
                _uiState.value = _uiState.value.copy(countdownProgress = (1f - (elapsed / duration)).coerceAtLeast(0f))
                delay(16)
                if (elapsed >= duration) break
            }
        }
    }

    fun submitGuess(guessedColor: Color) {
        timerJob?.cancel()
        val endTime = System.currentTimeMillis()
        val timeTakenMillis = endTime - roundStartTime
        val timeTakenSec = timeTakenMillis / 1000f
        
        val distance = calculateColorDistance(_uiState.value.targetColor, guessedColor)
        val maxDist = sqrt(255f.pow(2) * 3)
        val accuracy = (1.0 - (distance / maxDist)).coerceIn(0.0, 1.0)
        
        // Bonus for speed: less time taken = better result
        // factor 1.1x at 0s, 1.0x at 2s, 0.8x at 5s
        val speedFactor = if (_uiState.value.level == GameLevel.EASY) 1.0f 
                          else (1.1f - (timeTakenSec * 0.06f)).coerceIn(0.6f, 1.1f)
        
        val roundScore = ((accuracy * 100) * speedFactor).toInt().coerceIn(0, 100)

        val newBestHex = if (roundScore > 85) colorToHex(guessedColor) else _uiState.value.bestGuessHex

        _uiState.value = _uiState.value.copy(
            guessedColor = guessedColor,
            lastRoundScore = roundScore,
            lastRoundTime = timeTakenMillis,
            totalScore = _uiState.value.totalScore + roundScore,
            bestGuessHex = newBestHex,
            roundScores = _uiState.value.roundScores + roundScore,
            gameState = GameState.RESULT
        )
    }

    fun nextRound() {
        if (_uiState.value.currentRound < 10) {
            _uiState.value = _uiState.value.copy(currentRound = _uiState.value.currentRound + 1)
            startRound()
        } else {
            _uiState.value = _uiState.value.copy(gameState = GameState.GAME_OVER)
            saveHighScore()
        }
    }

    private fun saveHighScore() {
        viewModelScope.launch {
            val level = _uiState.value.level
            val currentTotal = _uiState.value.totalScore
            val highScore = _uiState.value.highScores[level] ?: 0
            if (currentTotal > highScore) {
                repository.saveHighScore(level, currentTotal)
                val newScores = _uiState.value.highScores.toMutableMap().apply { put(level, currentTotal) }
                _uiState.value = _uiState.value.copy(highScores = newScores)
            }
        }
    }

    fun resetGame() {
        _uiState.value = _uiState.value.copy(gameState = GameState.LEVEL_SELECTION, roundScores = emptyList())
    }

    private fun generateRandomColor(level: GameLevel): Color {
        return when (level) {
            GameLevel.EASY -> Color(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
            GameLevel.MODERATE -> Color(android.graphics.Color.HSVToColor(floatArrayOf(Random.nextFloat() * 360, 0.7f, 0.8f)))
            else -> if (Random.nextBoolean()) Color(220 + Random.nextInt(36), 220 + Random.nextInt(36), 220 + Random.nextInt(36))
                    else Color(Random.nextInt(40), Random.nextInt(40), Random.nextInt(40))
        }
    }

    private fun calculateColorDistance(c1: Color, c2: Color): Double {
        val r1 = (c1.red * 255).toInt()
        val g1 = (c1.green * 255).toInt()
        val b1 = (c1.blue * 255).toInt()
        val r2 = (c2.red * 255).toInt()
        val g2 = (c2.green * 255).toInt()
        val b2 = (c2.blue * 255).toInt()
        return sqrt((r1 - r2).toDouble().pow(2) + (g1 - g2).toDouble().pow(2) + (b1 - b2).toDouble().pow(2))
    }

    private fun colorToHex(color: Color): String = String.format(Locale.getDefault(), "#%06X", (0xFFFFFF and color.toArgb()))
}
