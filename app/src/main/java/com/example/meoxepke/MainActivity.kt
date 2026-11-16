package com.example.meoxepke

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

val CAT_DRAWABLES = listOf(
    R.drawable.cat_01, R.drawable.cat_02, R.drawable.cat_03,
    R.drawable.cat_04, R.drawable.cat_05, R.drawable.cat_06,
    R.drawable.cat_07, R.drawable.cat_08, R.drawable.cat_09,
    R.drawable.cat_10, R.drawable.cat_11, R.drawable.cat_12,
    R.drawable.cat_13, R.drawable.cat_14, R.drawable.cat_15,
    R.drawable.cat_16, R.drawable.cat_17, R.drawable.cat_18,
    R.drawable.cat_19, R.drawable.cat_20, R.drawable.cat_21,
    R.drawable.cat_22, R.drawable.cat_23, R.drawable.cat_24,
    R.drawable.cat_25, R.drawable.cat_26, R.drawable.cat_27,
    R.drawable.cat_28, R.drawable.cat_29, R.drawable.cat_30,
)

private val BG_COLOR = Color(0xFFFFF8E7)
private val TILE_COLOR = Color(0xFFFFF3D6)
private val PLANK_COLOR = Color(0xFF8D5A2B)
private val SELECT_COLOR = Color(0xFF4CAF50)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GameScreen()
            }
        }
    }
}

@Composable
fun GameScreen() {
    val game = remember { Game(1) }

    LaunchedEffect(game.phase, game.config.level) {
        while (game.phase == Phase.PLAYING) {
            delay(1000)
            game.tick()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BG_COLOR,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            // Hang thong tin: man, diem, gio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Màn ${game.config.level}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Điểm: ${game.score}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = formatTime(game.timeLeft),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (game.timeLeft <= 30 && game.phase == Phase.PLAYING) Color.Red else Color.Black,
                )
            }
            Text(
                text = "Còn ${game.remainingCats()} con mèo - Bấm 3 con giống nhau để xóa",
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Ban choi hinh chu U: trai 3 ke, phai 3 ke, giua duoi 1 ke
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.weight(0.42f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Shelf(game, listOf(0, 1, 2))
                        Shelf(game, listOf(3, 4, 5))
                        Shelf(game, listOf(6, 7, 8))
                    }
                    Spacer(modifier = Modifier.weight(0.16f))
                    Column(
                        modifier = Modifier.weight(0.42f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Shelf(game, listOf(9, 10, 11))
                        Shelf(game, listOf(12, 13, 14))
                        Shelf(game, listOf(15, 16, 17))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(0.29f))
                    Box(modifier = Modifier.weight(0.42f)) {
                        Shelf(game, listOf(18, 19, 20))
                    }
                    Spacer(modifier = Modifier.weight(0.29f))
                }
            }

            // Nut tro giup
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { game.refresh() },
                    enabled = game.phase == Phase.PLAYING && game.refreshLeft > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Xáo (${game.refreshLeft})")
                }
                Button(
                    onClick = { game.addTime() },
                    enabled = game.phase == Phase.PLAYING && game.bonusLeft > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("+30s (${game.bonusLeft})")
                }
                Button(
                    onClick = { game.restart() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Chơi lại")
                }
            }
        }

        if (game.phase == Phase.WON) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Thắng rồi!") },
                text = { Text("Màn ${game.config.level} xong, được ${game.score} điểm.") },
                confirmButton = {
                    TextButton(onClick = { game.nextLevel() }) { Text("Màn tiếp") }
                },
                dismissButton = {
                    TextButton(onClick = { game.restart() }) { Text("Chơi lại") }
                },
            )
        }
        if (game.phase == Phase.LOST) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Hết giờ!") },
                text = { Text("Được ${game.score} điểm. Thử lại nhé.") },
                confirmButton = {
                    TextButton(onClick = { game.restart() }) { Text("Chơi lại") }
                },
            )
        }
    }
}

@Composable
fun Shelf(game: Game, indices: List<Int>) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            indices.forEach { idx ->
                CatTile(game, idx, Modifier.weight(1f))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PLANK_COLOR),
        )
    }
}

@Composable
fun CatTile(game: Game, index: Int, modifier: Modifier) {
    val type = game.slots[index]
    val gen = game.generations[index]
    val selected = game.selectedSlots.contains(index)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(TILE_COLOR)
            .then(
                if (selected) Modifier.border(3.dp, SELECT_COLOR, RoundedCornerShape(10.dp))
                else Modifier,
            )
            .clickable { game.tap(index) },
        contentAlignment = Alignment.Center,
    ) {
        if (type != EMPTY) {
            key(gen) {
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(initialScale = 0.3f) + fadeIn(),
                ) {
                    Image(
                        painter = painterResource(CAT_DRAWABLES[type]),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .scale(if (selected) 1.12f else 1f),
                    )
                }
            }
        }
    }
}

fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
