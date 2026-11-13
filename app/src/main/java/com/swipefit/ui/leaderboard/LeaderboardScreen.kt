package com.swipefit.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.swipefit.data.LeaderboardEntry
import com.swipefit.ui.common.ErrorBox
import com.swipefit.ui.common.LoadingBox
import com.swipefit.ui.common.MessagePane
import com.swipefit.ui.common.SwipeFitTopBar
import com.swipefit.ui.theme.Bronze
import com.swipefit.ui.theme.Gold
import com.swipefit.ui.theme.HotPink
import com.swipefit.ui.theme.Silver
import kotlin.math.roundToInt

private fun medalFor(rank: Int): Color = when (rank) {
    1 -> Gold
    2 -> Silver
    else -> Bronze
}

@Composable
fun LeaderboardScreen(viewModel: LeaderboardViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val error = state.error

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SwipeFitTopBar(title = "Rankings", onBack = onBack) }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                state.loading -> LoadingBox()
                error != null -> ErrorBox(error, onBack)
                state.entries.isEmpty() -> MessagePane(
                    title = "No outfits yet",
                    body = "Upload an outfit or add sample outfits from the group dashboard.",
                    buttonLabel = null,
                    onButtonClick = null,
                    icon = Icons.Filled.Star
                )
                else -> {
                    // Only rated outfits can stand on the podium; entries are already sorted with rated first.
                    val podium = state.entries.takeWhile { it.isRated }.take(3)
                    val rest = state.entries.drop(podium.size)
                    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp + bottomInset),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "live") { LiveLabel() }
                        if (podium.isNotEmpty()) {
                            item(key = "podium") { Podium(podium) }
                        }
                        itemsIndexed(rest, key = { _, entry -> entry.outfit.id }) { index, entry ->
                            LeaderboardRow(
                                rank = podium.size + index + 1,
                                entry = entry,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveLabel() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        Box(
            Modifier
                .size(8.dp)
                .background(HotPink, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "LIVE · updates as your group votes",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Top three, arranged 2 · 1 · 3 with pedestals of different heights. */
@Composable
private fun Podium(top: List<LeaderboardEntry>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(1, 0, 2).forEach { i ->
            val entry = top.getOrNull(i)
            if (entry == null) {
                Spacer(Modifier.weight(1f))
            } else {
                PodiumSpot(rank = i + 1, entry = entry, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PodiumSpot(rank: Int, entry: LeaderboardEntry, modifier: Modifier = Modifier) {
    val medal = medalFor(rank)
    val photo = if (rank == 1) 96.dp else 76.dp
    val pedestal = when (rank) {
        1 -> 72.dp
        2 -> 52.dp
        else -> 36.dp
    }

    Column(modifier.padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomCenter) {
            AsyncImage(
                model = entry.outfit.imageUrl,
                contentDescription = entry.outfit.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(photo)
                    .border(3.dp, medal, CircleShape)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(medal, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(rank.toString(), color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            entry.outfit.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            "${entry.hotPercent.roundToInt()}% hot",
            style = MaterialTheme.typography.labelLarge,
            color = HotPink
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(pedestal)
                .background(medal.copy(alpha = 0.22f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${entry.totalVotes} ${if (entry.totalVotes == 1) "vote" else "votes"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, entry: LeaderboardEntry, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (entry.isRated) rank.toString() else "–",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            AsyncImage(
                model = entry.outfit.imageUrl,
                contentDescription = entry.outfit.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entry.outfit.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                if (entry.isRated) {
                    LinearProgressIndicator(
                        progress = { (entry.hotPercent / 100.0).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = HotPink,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${entry.hotVotes} hot · ${entry.totalVotes} ${if (entry.totalVotes == 1) "vote" else "votes"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text("Not rated yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (entry.isRated) {
                Spacer(Modifier.width(12.dp))
                Text(
                    "${entry.hotPercent.roundToInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = HotPink
                )
            }
        }
    }
}
