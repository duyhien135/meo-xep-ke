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
import androidx.compose.foundation.layout.fillMaxHeight
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
private val DROP_COLOR = Color(0xFFDCEDC8)
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
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Điểm: ${game.score}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = formatTime(game.timeLeft),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (game.timeLeft <= 30 && game.phase == Phase.PLAYING) Color.Red else Color.Black,
                )
            }
            Text(
                text = "Bấm 1 con mèo rồi bấm ô trống để chuyển nó. 3 con giống nhau trong 1 kệ sẽ biến mất.",
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Còn ${game.remainingCats()} con mèo",
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 7 ke xep chong, moi ke rong het man hinh
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                for (shelf in 0 until SHELF_COUNT) {
                    Shelf(game, shelf, Modifier.weight(1f))
                }
            }

            // Nut tro giup
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { game.refresh() },
                    enabled = game.phase == Phase.PLAYING && game.refreshLeft > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Text("Xáo (${game.refreshLeft})", fontSize = 17.sp)
                }
                Button(
                    onClick = { game.addTime() },
                    enabled = game.phase == Phase.PLAYING && game.bonusLeft > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Text("+30s (${game.bonusLeft})", fontSize = 17.sp)
                }
                Button(
                    onClick = { game.restart() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Text("Chơi lại", fontSize = 17.sp)
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
fun Shelf(game: Game, shelf: Int, modifier: Modifier) {
    val base = shelf * SLOTS_PER_SHELF
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (k in 0 until SLOTS_PER_SHELF) {
                CatTile(game, base + k, Modifier.weight(1f))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PLANK_COLOR),
        )
    }
}

@Composable
fun CatTile(game: Game, index: Int, modifier: Modifier) {
    val type = game.slots[index]
    val gen = game.generations[index]
    val selected = game.selectedSlot == index
    val droppable = game.selectedSlot != EMPTY && type == EMPTY
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(if (droppable) DROP_COLOR else TILE_COLOR)
            .then(
                if (selected) Modifier.border(3.dp, SELECT_COLOR, RoundedCornerShape(12.dp))
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
                            .padding(4.dp)
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
