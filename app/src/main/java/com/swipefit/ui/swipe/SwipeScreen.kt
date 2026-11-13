package com.swipefit.ui.swipe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.swipefit.data.Outfit
import com.swipefit.ui.common.ErrorBox
import com.swipefit.ui.common.LoadingBox
import com.swipefit.ui.common.MessagePane
import com.swipefit.ui.common.SwipeFitTopBar
import com.swipefit.ui.theme.HotGradient
import com.swipefit.ui.theme.HotPink
import com.swipefit.ui.theme.NotGray
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun SwipeScreen(viewModel: SwipeViewModel, onBack: () -> Unit, onLeaderboard: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val current = state.current
    val error = state.error

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            SwipeFitTopBar(title = "Swipe", onBack = onBack, actions = {
                if (state.total > 0) CountChip(reviewed = state.reviewed, total = state.total)
            })
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                state.loading -> LoadingBox()
                error != null -> ErrorBox(error, onBack)
                state.total == 0 -> MessagePane(
                    title = "No outfits yet",
                    body = "Upload an outfit or add sample outfits from the group dashboard.",
                    buttonLabel = "Back to group",
                    onButtonClick = onBack,
                    icon = Icons.Filled.Favorite
                )
                current == null -> MessagePane(
                    title = "You're all caught up!",
                    body = "You've rated all ${state.total} outfits. New ones will show up here automatically.",
                    buttonLabel = "View rankings",
                    onButtonClick = onLeaderboard,
                    icon = Icons.Filled.Star
                )
                else -> Column(Modifier.fillMaxSize()) {
                    ProgressBar(reviewed = state.reviewed, total = state.total)
                    SwipeDeck(
                        current = current,
                        next = state.next,
                        showHint = state.reviewed == 0,
                        onVote = viewModel::vote,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CountChip(reviewed: Int, total: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = CircleShape,
        modifier = Modifier.padding(end = 12.dp)
    ) {
        Text(
            "$reviewed / $total",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ProgressBar(reviewed: Int, total: Int) {
    val progress by animateFloatAsState(
        targetValue = if (total == 0) 0f else reviewed / total.toFloat(),
        label = "reviewProgress"
    )
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .height(6.dp)
            .clip(CircleShape),
        color = HotPink,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

/**
 * The swipeable card stack. The top card follows the finger horizontally, rotates a little, tints, and shows a
 * HOT or NOT stamp. Past the threshold (or when a button is tapped) it animates off screen and [onVote] is called.
 */
@Composable
private fun SwipeDeck(
    current: Outfit,
    next: Outfit?,
    showHint: Boolean,
    onVote: (Outfit, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val threshold = screenWidthPx * 0.28f

    // New state for every card, so each outfit starts centered.
    val offsetX = remember(current.id) { Animatable(0f) }
    var leaving by remember(current.id) { mutableStateOf(false) }
    var pastThreshold by remember(current.id) { mutableStateOf(false) }

    fun flingOff(hot: Boolean) {
        if (leaving) return
        leaving = true
        scope.launch {
            offsetX.animateTo(
                targetValue = if (hot) screenWidthPx * 1.4f else -screenWidthPx * 1.4f,
                animationSpec = tween(durationMillis = 260, easing = FastOutLinearInEasing)
            )
            onVote(current, hot)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp)
        ) {
            // Card underneath: grows to full size as the top card is dragged away.
            if (next != null) {
                OutfitCard(
                    outfit = next,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = (abs(offsetX.value) / threshold).coerceIn(0f, 1f)
                            val s = 0.93f + 0.07f * p
                            scaleX = s
                            scaleY = s
                            translationY = (1f - p) * 18.dp.toPx()
                        }
                )
            }

            // Top card.
            OutfitCard(
                outfit = current,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = offsetX.value
                        rotationZ = (offsetX.value / screenWidthPx) * 16f
                    }
                    .pointerInput(current.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                when {
                                    offsetX.value > threshold -> flingOff(true)
                                    offsetX.value < -threshold -> flingOff(false)
                                    else -> scope.launch {
                                        offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                    }
                                }
                            },
                            onDragCancel = { scope.launch { offsetX.animateTo(0f) } },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                if (!leaving) {
                                    val newX = offsetX.value + dragAmount
                                    // A small buzz the moment a release would count as a vote.
                                    val past = abs(newX) > threshold
                                    if (past != pastThreshold) {
                                        pastThreshold = past
                                        if (past) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    scope.launch { offsetX.snapTo(newX) }
                                }
                            }
                        )
                    },
                overlay = {
                    // Color wash that strengthens as the card moves.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val strength = (abs(offsetX.value) / threshold).coerceIn(0f, 1f) * 0.35f
                                drawRect(color = if (offsetX.value >= 0f) HotPink else Color.Black, alpha = strength)
                            }
                    )
                    Stamp(
                        text = "HOT",
                        color = HotPink,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(28.dp)
                            .graphicsLayer {
                                rotationZ = -14f
                                alpha = (offsetX.value / threshold).coerceIn(0f, 1f)
                            }
                    )
                    Stamp(
                        text = "NOT",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(28.dp)
                            .graphicsLayer {
                                rotationZ = 14f
                                alpha = (-offsetX.value / threshold).coerceIn(0f, 1f)
                            }
                    )
                }
            )
        }

        Text(
            if (showHint) "Swipe right for Hot, left for Not" else " ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NotButton { flingOff(false) }
            HotButton { flingOff(true) }
        }
    }
}

@Composable
private fun OutfitCard(
    outfit: Outfit,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier = modifier
            .shadow(14.dp, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(outfit.imageUrl).crossfade(true).build(),
            contentDescription = outfit.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                .padding(start = 24.dp, end = 24.dp, top = 64.dp, bottom = 24.dp)
        ) {
            Text(outfit.name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        }
        overlay()
    }
}

@Composable
private fun Stamp(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        fontSize = 40.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        modifier = modifier
            .border(4.dp, color, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun NotButton(onClick: () -> Unit) {
    RoundAction(
        icon = Icons.Filled.Close,
        description = "Not",
        size = 64.dp,
        iconTint = NotGray,
        backgroundModifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape),
        onClick = onClick
    )
}

@Composable
private fun HotButton(onClick: () -> Unit) {
    RoundAction(
        icon = Icons.Filled.Favorite,
        description = "Hot",
        size = 76.dp,
        iconTint = Color.White,
        backgroundModifier = Modifier.background(HotGradient, CircleShape),
        onClick = onClick
    )
}

@Composable
private fun RoundAction(
    icon: ImageVector,
    description: String,
    size: Dp,
    iconTint: Color,
    backgroundModifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(10.dp, CircleShape)
            .then(backgroundModifier)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = iconTint, modifier = Modifier.size(size * 0.45f))
    }
}
