package com.yourcollege.colormemory

import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import com.yourcollege.colormemory.ui.theme.ColorMemoryTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

fun squircleShape(percent: Int = 80) = RoundedCornerShape(percent)

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ColorMemoryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    GameScreen(uiState, viewModel)
                }
            }
        }
    }
}

@Composable
fun GameScreen(uiState: GameUIState, viewModel: GameViewModel) {
    AnimatedContent(
        targetState = uiState.gameState,
        transitionSpec = {
            fadeIn(animationSpec = tween(400)).togetherWith(fadeOut(animationSpec = tween(300)))
        },
        label = "GameStates"
    ) { state ->
        when (state) {
            GameState.LEVEL_SELECTION -> LevelSelectionScreen(uiState.highScores) { viewModel.selectLevel(it) }
            GameState.PREVIEW_SEQUENCE -> PreviewSequenceScreen(uiState)
            GameState.DELAY -> DelayScreen(uiState)
            GameState.GUESSING -> GuessingScreen(viewModel, uiState)
            GameState.RESULT -> ResultScreen(uiState) { viewModel.nextRound() }
            GameState.GAME_OVER -> GameOverScreen(uiState) { viewModel.resetGame() }
        }
    }
}

@Composable
fun LevelSelectionScreen(highScores: Map<GameLevel, Int>, onLevelSelected: (GameLevel) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "meshGradient")
    val color1 by infiniteTransition.animateColor(
        initialValue = Color(0xFF4A148C),
        targetValue = Color(0xFF311B92),
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse), label = "color1"
    )
    val color2 by infiniteTransition.animateColor(
        initialValue = Color(0xFFFF5252),
        targetValue = Color(0xFFFF1744),
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse), label = "color2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(color1, color2)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(3.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🎨", fontSize = 48.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Chroma Master",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "Match the hex. Master the color.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 6.dp, bottom = 36.dp)
            )
            GameLevel.entries.forEach { level ->
                LevelCard(level, highScores[level] ?: 0) { onLevelSelected(level) }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelCard(level: GameLevel, highScore: Int, onClick: () -> Unit) {
    val previewColor = when (level) {
        GameLevel.EASY -> Color(0xFF81C784)
        GameLevel.MODERATE -> Color(0xFFFFB74D)
        GameLevel.DIFFICULT -> Color(0xFF90A4AE)
    }
    val description = when (level) {
        GameLevel.EASY -> "Single color • 5 rounds"
        GameLevel.MODERATE -> "Same hue • 5 rounds"
        GameLevel.DIFFICULT -> "Sequence memory • 5 rounds"
    }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = level.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1C1B1F)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF49454F)
                )
                if (highScore > 0) {
                    Text(
                        text = "Best: $highScore",
                        style = MaterialTheme.typography.labelLarge,
                        color = previewColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(previewColor)
                    .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            )
        }
    }
}

@Composable
fun PreviewSequenceScreen(uiState: GameUIState) {
    val target = if (uiState.level == GameLevel.DIFFICULT) {
        uiState.colorSequence.getOrNull(uiState.currentSequenceIndex)?.color ?: Color.Gray
    } else {
        uiState.targetColor
    }

    val displayText = if (uiState.level == GameLevel.DIFFICULT) {
        val step = uiState.colorSequence.getOrNull(uiState.currentSequenceIndex)
        if (step != null) "Shade ${step.id}" else "Memorize..."
    } else {
        "Memorize this color"
    }

    Box(modifier = Modifier.fillMaxSize().background(target), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .blur(80.dp)
                .background(target.copy(alpha = 0.6f), CircleShape)
        )

        ElevatedCard(
            modifier = Modifier.padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.level == GameLevel.DIFFICULT) {
                    Text(
                        text = "Round ${uiState.currentRound}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(5) { index ->
                            val isActive = index <= uiState.currentSequenceIndex
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive) Color.White else Color.White.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                    Text(
                        text = "${uiState.currentSequenceIndex + 1} / 5",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Text(
                        text = "Round ${uiState.currentRound}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun DelayScreen(uiState: GameUIState) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse), label = "alpha"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        val bgColor = if (uiState.level == GameLevel.DIFFICULT)
            uiState.colorSequence.lastOrNull()?.color ?: Color.Gray
        else uiState.targetColor

        Box(modifier = Modifier.fillMaxSize().background(bgColor).blur(60.dp))
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(56.dp).scale(scale),
                color = Color.White,
                strokeWidth = 4.dp,
                trackColor = Color.White.copy(alpha = 0.25f)
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                "Get ready...",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White.copy(alpha = alpha),
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun GuessingScreen(viewModel: GameViewModel, uiState: GameUIState) {
    var pickedColor by remember { mutableStateOf(Color.Gray) }
    val timeLeft by remember(uiState.countdownProgress) {
        mutableIntStateOf((uiState.countdownProgress * 5).coerceIn(0f, 5f).toInt())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GuessingHeader(uiState, timeLeft)

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            HeroUIColorPicker(onColorChanged = { pickedColor = it })
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { viewModel.submitGuess(pickedColor) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Submit Guess", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun GuessingHeader(uiState: GameUIState, timeLeft: Int) {
    val progress = uiState.countdownProgress.coerceIn(0f, 1f)
    val sweep = progress
    val isLow = progress < 0.3f
    val timerColor = if (isLow) Color(0xFFFF5252) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            val prompt = if (uiState.level == GameLevel.DIFFICULT) "Guess shade ${uiState.targetId}!" else "Your Guess"
            Text(
                text = prompt,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Round ${uiState.currentRound}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { sweep },
                modifier = Modifier.size(56.dp),
                color = timerColor,
                strokeWidth = 4.dp,
                trackColor = Color.White.copy(alpha = 0.2f)
            )
            Text(
                text = "$timeLeft",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

@Composable
fun HeroUIColorPicker(onColorChanged: (Color) -> Unit, modifier: Modifier = Modifier) {
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0.5f) }
    var value by remember { mutableFloatStateOf(0.5f) }
    val haptic = LocalHapticFeedback.current
    val currentColor = remember(hue, saturation, value) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    }

    LaunchedEffect(currentColor) { onColorChanged(currentColor) }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .drawBehind {
                        val hsvColor = android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                        drawRect(Brush.horizontalGradient(listOf(Color.White, Color(hsvColor))))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                    }
                    .pointerInput(hue) {
                        detectDragGestures { change, _ ->
                            saturation = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            value = (1f - (change.position.y / size.height.toFloat())).coerceIn(0f, 1f)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cursorX = saturation * size.width
                    val cursorY = (1f - value) * size.height
                    drawCircle(Color.White, radius = 10.dp.toPx(), center = Offset(cursorX, cursorY))
                    drawCircle(Color.Black, radius = 10.dp.toPx(), center = Offset(cursorX, cursorY), style = Stroke(width = 2.5.dp.toPx()))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(currentColor)
                        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hue",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = hue,
                        onValueChange = {
                            hue = it
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        valueRange = 0f..360f,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            activeTrackColor = currentColor,
                            thumbColor = currentColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(currentColor)
                        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                )
                OutlinedTextField(
                    value = String.format(Locale.getDefault(), "#%06X", (0xFFFFFF and currentColor.toArgb())),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

data class Particle(val x: Float, val y: Float, val vx: Float, val vy: Float, val color: Color, var life: Float = 1f)

@Composable
fun ResultScreen(uiState: GameUIState, onNext: () -> Unit) {
    var animatedScore by remember { mutableIntStateOf(0) }
    val particles = remember { mutableStateListOf<Particle>() }
    var buttonPosition by remember { mutableStateOf(Offset.Zero) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(uiState.lastRoundScore) {
        if (uiState.lastRoundScore >= 90) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else if (uiState.lastRoundScore <= 30) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            delay(100)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        launch {
            for (i in 0..uiState.lastRoundScore) {
                animatedScore = i
                delay(8)
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (particles.isNotEmpty()) {
                val iterator = particles.listIterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    p.life -= 0.04f
                    if (p.life <= 0) iterator.remove()
                    else iterator.set(p.copy(x = p.x + p.vx, y = p.y + p.vy, life = p.life))
                }
            }
            delay(16)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Round ${uiState.currentRound} Result",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ColorComparisonCircle(uiState.targetColor, "Target")
                        ColorComparisonCircle(uiState.guessedColor, "Your Guess")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "$animatedScore",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Score",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%.2fs", uiState.lastRoundTime / 1000f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(28.dp))
                    Button(
                        onClick = {
                            repeat(30) {
                                particles.add(
                                    Particle(
                                        buttonPosition.x,
                                        buttonPosition.y,
                                        (Random.nextFloat() - 0.5f) * 18f,
                                        (Random.nextFloat() - 0.5f) * 18f,
                                        uiState.guessedColor
                                    )
                                )
                            }
                            onNext()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .onGloballyPositioned { buttonPosition = Offset(it.size.width / 2f, it.size.height / 2f) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Next Round", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                drawCircle(
                    color = p.color,
                    radius = 5.dp.toPx() * p.life,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }
}

@Composable
fun ColorComparisonCircle(color: Color, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = String.format(Locale.getDefault(), "#%06X", (0xFFFFFF and color.toArgb())),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GameOverScreen(uiState: GameUIState, onReset: () -> Unit) {
    val particles = remember { mutableStateListOf<Particle>() }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scores = uiState.roundScores
    val roundsPlayed = scores.size.coerceAtLeast(1)
    val averageScore = if (scores.isNotEmpty()) scores.average().toInt() else 0
    val performanceMessage = when {
        uiState.totalScore >= 900 -> "Color Genius"
        uiState.totalScore >= 700 -> "Sharp Eye"
        uiState.totalScore >= 500 -> "Getting Warmer"
        uiState.totalScore >= 300 -> "Apprentice"
        else -> "Keep Practicing"
    }

    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        repeat(150) {
            particles.add(
                Particle(
                    Random.nextFloat() * 1400f,
                    2000f,
                    (Random.nextFloat() - 0.5f) * 35f,
                    -Random.nextFloat() * 55f,
                    Color(Random.nextInt())
                )
            )
        }
        while (true) {
            if (particles.isNotEmpty()) {
                val iterator = particles.listIterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    p.life -= 0.01f
                    if (p.life <= 0) iterator.remove()
                    else iterator.set(p.copy(x = p.x + p.vx, y = p.y + p.vy, life = p.life))
                }
            }
            delay(16)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                drawCircle(
                    color = p.color,
                    radius = 7.dp.toPx() * p.life,
                    center = Offset(p.x, p.y)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = performanceMessage,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Game Summary",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        val primary = MaterialTheme.colorScheme.primary
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val sweep = (uiState.totalScore / 1000f).coerceIn(0f, 1f)
                            val startAngle = -90f
                            val sweepAngle = sweep * 360f
                            val stroke = 10.dp.toPx()
                            drawArc(
                                color = primary,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = Offset(stroke / 2, stroke / 2),
                                size = Size(size.width - stroke, size.height - stroke),
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${uiState.totalScore}",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "/ 1000",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SummaryChip("Rounds", "$roundsPlayed / 10")
                        SummaryChip("Average", "$averageScore")
                        SummaryChip("Level", uiState.level.name)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (scores.isNotEmpty()) {
                        Text(
                            text = "Round Breakdown",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            scores.forEachIndexed { index, score ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$score",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(16.dp)
                                            .height(score.dp.coerceIn(4.dp, 60.dp))
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when {
                                                    score >= 90 -> Color(0xFF4CAF50)
                                                    score >= 70 -> Color(0xFF8BC34A)
                                                    score >= 50 -> Color(0xFFFFC107)
                                                    else -> Color(0xFFFF5252)
                                                }
                                            )
                                    )
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(uiState.bestGuessHex.toColorInt()))
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Best Guess: ${uiState.bestGuessHex}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onReset,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Refresh, "Play Again")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play Again", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                val chartBitmap = android.graphics.Bitmap.createBitmap(1080, 1920, android.graphics.Bitmap.Config.ARGB_8888)
                                val chartCanvas = android.graphics.Canvas(chartBitmap)
                                chartCanvas.drawColor(android.graphics.Color.WHITE)
                                val paint = android.graphics.Paint().apply {
                                    isFakeBoldText = true
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                                paint.color = android.graphics.Color.BLACK
                                paint.textSize = 96f
                                chartCanvas.drawText("CHROMA MASTER", 540f, 300f, paint)
                                paint.textSize = 72f
                                chartCanvas.drawText("Final Score: ${uiState.totalScore}", 540f, 500f, paint)
                                paint.textSize = 48f
                                chartCanvas.drawText("Best Guess: ${uiState.bestGuessHex}", 540f, 650f, paint)
                                paint.color = uiState.bestGuessHex.toColorInt()
                                chartCanvas.drawCircle(540f, 900f, 220f, paint)

                                try {
                                    val cachePath = File(context.cacheDir, "images")
                                    cachePath.mkdirs()
                                    val stream = FileOutputStream("$cachePath/result.png")
                                    chartBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                                    stream.close()
                                    val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(cachePath, "result.png"))
                                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, contentUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }, "Share Result"))
                                } catch (e: Exception) { e.printStackTrace() }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Icon(Icons.Default.Share, "Share")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
