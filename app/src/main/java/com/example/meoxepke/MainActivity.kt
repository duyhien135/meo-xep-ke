package com.example.meoxepke

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

val CAT_DRAWABLES = listOf(
    R.drawable.cat_01, R.drawable.cat_02, R.drawable.cat_03,
    R.drawable.cat_04, R.drawable.cat_05, R.drawable.cat_06,
    R.drawable.cat_07, R.drawable.cat_08, R.drawable.cat_09,
    R.drawable.cat_10, R.drawable.cat_11, R.drawable.cat_12,
    R.drawable.cat_13,
)

private val BG_COLOR = Color(0xFFFFF8E7)
private val TILE_COLOR = Color(0xFFFFF3D6)
private val DROP_COLOR = Color(0xFFDCEDC8)
private val PLANK_COLOR = Color(0xFF8D5A2B)
private val SELECT_COLOR = Color(0xFF4CAF50)
private val SPARKLE_COLOR = Color(0xFFFFC107)

private data class DragInfo(val source: Int, val type: Int, val offset: Offset)

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
    val appContext = LocalContext.current.applicationContext
    val game = remember { Game(1) { CatStore.totalTypes(appContext) } }
    var rootRect by remember { mutableStateOf(Rect.Zero) }
    val tileRects = remember { mutableStateMapOf<Int, Rect>() }
    var drag by remember { mutableStateOf<DragInfo?>(null) }
    var hoverSlot by remember { mutableIntStateOf(EMPTY) }
    var sparkleShelf by remember { mutableIntStateOf(EMPTY) }
    var extraBitmaps by remember { mutableStateOf<Map<Int, Bitmap>>(emptyMap()) }
    var customCount by remember { mutableIntStateOf(0) }
    var remoteCount by remember { mutableIntStateOf(0) }
    var statusText by remember { mutableStateOf("") }
    var pickerOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Tai bitmap cua cac meo them vao bo nho.
    suspend fun loadExtras() {
        val (custom, remote) = withContext(Dispatchers.IO) {
            CatStore.customCatFiles(appContext) to CatStore.remoteCatFiles(appContext)
        }
        val files = custom + remote
        val map = withContext(Dispatchers.IO) {
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            val m = mutableMapOf<Int, Bitmap>()
            for ((i, file) in files.withIndex()) {
                BitmapFactory.decodeFile(file.absolutePath, opts)?.let {
                    m[CatStore.BUNDLED_TYPES + i] = it
                }
            }
            m
        }
        extraBitmaps = map
        customCount = custom.size
        remoteCount = remote.size
    }

    // Mo app: tai meo them, roi tu dong tai meo moi tu mang ve.
    LaunchedEffect(Unit) {
        loadExtras()
        val added = withContext(Dispatchers.IO) { CatStore.syncRemoteCats(appContext) }
        if (added > 0) {
            loadExtras()
            statusText = "Đã tải thêm $added mèo từ mạng"
        }
    }

    LaunchedEffect(game.phase, game.config.level, pickerOpen) {
        while (game.phase == Phase.PLAYING) {
            delay(1000)
            if (!pickerOpen) game.tick()
        }
    }

    LaunchedEffect(game.clearGen) {
        if (game.clearGen > 0) {
            sparkleShelf = game.clearShelf
            delay(700)
            sparkleShelf = EMPTY
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        pickerOpen = false
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val added = withContext(Dispatchers.IO) {
                var n = 0
                for (uri in uris) if (CatStore.importCat(appContext, uri)) n++
                n
            }
            if (added > 0) {
                loadExtras()
                statusText = "Đã thêm $added mèo"
                game.restart()
            } else {
                statusText = "Không thêm được mèo nào"
            }
        }
    }

    fun slotAt(point: Offset): Int {
        for ((idx, rect) in tileRects) {
            if (rect.contains(point)) return idx
        }
        return EMPTY
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BG_COLOR,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { rootRect = it.boundsInRoot() }
                .pointerInput(game) {
                    // Keo ngay khi cham va di chuyen (khong can bam giu).
                    detectDragGestures(
                        onDragStart = { offset ->
                            val idx = slotAt(offset)
                            if (idx != EMPTY && game.frontAt(idx) != EMPTY) {
                                game.clearSelection()
                                drag = DragInfo(idx, game.frontAt(idx), offset)
                                hoverSlot = EMPTY
                            }
                        },
                        onDrag = { change, _ ->
                            drag?.let {
                                val next = change.position
                                drag = it.copy(offset = next)
                                hoverSlot = slotAt(next)
                            }
                        },
                        onDragEnd = {
                            drag?.let {
                                val target = hoverSlot
                                if (target != EMPTY && target != it.source && game.frontAt(target) == EMPTY) {
                                    game.move(it.source, target)
                                }
                            }
                            drag = null
                            hoverSlot = EMPTY
                        },
                        onDragCancel = {
                            drag = null
                            hoverSlot = EMPTY
                        },
                    )
                },
        ) {
            // Anh nen toan man hinh
            Image(
                painter = painterResource(R.drawable.bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Lop phu mo de chu va o meo van doc duoc
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.55f)),
            )
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
                    text = "Kéo mèo sang ô trống (hoặc bấm mèo rồi bấm ô trống). 3 con giống nhau trong 1 kệ sẽ biến mất.",
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
                Text(
                    text = "Mèo: 13 gốc + $customCount tự thêm + $remoteCount tải về",
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    textAlign = TextAlign.Center,
                )
                if (statusText.isNotEmpty()) {
                    Text(
                        text = statusText,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 7 ke xep chong, moi ke rong het man hinh
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (shelf in 0 until SHELF_COUNT) {
                        Shelf(
                            game = game,
                            shelf = shelf,
                            modifier = Modifier.weight(1f),
                            dragSource = drag?.source ?: EMPTY,
                            hoverSlot = hoverSlot,
                            sparkling = sparkleShelf == shelf,
                            bitmaps = extraBitmaps,
                            rootRect = rootRect,
                            onTilePosition = { idx, rect -> tileRects[idx] = rect },
                        )
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
                // Nut them / xoa meo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            pickerOpen = true
                            picker.launch("image/*")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                    ) {
                        Text("Thêm mèo", fontSize = 17.sp)
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) { CatStore.clearCustomCats(appContext) }
                                loadExtras()
                                statusText = "Đã xóa mèo tự thêm"
                                game.restart()
                            }
                        },
                        enabled = customCount > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                    ) {
                        Text("Xóa mèo thêm", fontSize = 17.sp)
                    }
                }
            }

            // Con meo dang keo theo ngon tay
            drag?.let { d ->
                CatImage(
                    type = d.type,
                    bitmaps = extraBitmaps,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (d.offset.x - 55.dp.toPx()).roundToInt(),
                                (d.offset.y - 55.dp.toPx()).roundToInt(),
                            )
                        }
                        .size(110.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .scale(1.1f),
                )
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
        if (game.stuck) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Kẹt rồi!") },
                text = { Text("Hết chỗ di chuyển. Xáo lại hoặc chơi lại nhé.") },
                confirmButton = {
                    TextButton(onClick = { game.restart() }) { Text("Chơi lại") }
                },
                dismissButton = {
                    if (game.refreshLeft > 0) {
                        TextButton(onClick = { game.refresh() }) { Text("Xáo lại (${game.refreshLeft})") }
                    }
                },
            )
        }
    }
}

/** Ve 1 con meo: loai < 13 la anh mac dinh trong app, loai >= 13 la anh
 *  nguoi choi tu them hoac tai ve (bitmap da tai san). */
@Composable
fun CatImage(
    type: Int,
    bitmaps: Map<Int, Bitmap>,
    modifier: Modifier,
    colorFilter: ColorFilter? = null,
    alpha: Float = 1f,
) {
    if (type < CatStore.BUNDLED_TYPES) {
        Image(
            painter = painterResource(CAT_DRAWABLES[type]),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = colorFilter,
            modifier = modifier.alpha(alpha),
        )
    } else {
        val bmp = bitmaps[type]
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = modifier.alpha(alpha),
            )
        } else {
            Box(modifier.background(Color(0xFFEEEEEE)))
        }
    }
}

@Composable
fun Shelf(
    game: Game,
    shelf: Int,
    modifier: Modifier,
    dragSource: Int,
    hoverSlot: Int,
    sparkling: Boolean,
    bitmaps: Map<Int, Bitmap>,
    rootRect: Rect,
    onTilePosition: (Int, Rect) -> Unit,
) {
    val base = shelf * SLOTS_PER_SHELF
    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (k in 0 until SLOTS_PER_SHELF) {
                    CatTile(
                        game = game,
                        index = base + k,
                        modifier = Modifier.weight(1f),
                        dragSource = dragSource,
                        hoverSlot = hoverSlot,
                        bitmaps = bitmaps,
                        rootRect = rootRect,
                        onTilePosition = onTilePosition,
                    )
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
        AnimatedVisibility(
            visible = sparkling,
            enter = scaleIn(initialScale = 0.5f) + fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                text = "✦ +30 ✦",
                color = SPARKLE_COLOR,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun CatTile(
    game: Game,
    index: Int,
    modifier: Modifier,
    dragSource: Int,
    hoverSlot: Int,
    bitmaps: Map<Int, Bitmap>,
    rootRect: Rect,
    onTilePosition: (Int, Rect) -> Unit,
) {
    val front = game.frontAt(index)
    val depth = game.depthAt(index)
    val next = game.nextAt(index)
    val gen = game.generations[index]
    val selected = game.selectedSlot == index
    val ghost = dragSource == index
    val droppable = front == EMPTY && (game.selectedSlot != EMPTY || index == hoverSlot)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(if (droppable) DROP_COLOR else TILE_COLOR)
            .then(
                if (selected) Modifier.border(3.dp, SELECT_COLOR, RoundedCornerShape(12.dp))
                else Modifier,
            )
            .onGloballyPositioned { coords ->
                val r = coords.boundsInRoot()
                onTilePosition(index, r.translate(-rootRect.left, -rootRect.top))
            }
            .clickable { game.tap(index) },
        contentAlignment = Alignment.Center,
    ) {
        // Bong den cua con meo ngay phia sau (neu con)
        if (depth > 1 && next != EMPTY) {
            CatImage(
                type = next,
                bitmaps = bitmaps,
                modifier = Modifier
                    .fillMaxSize()
                    .offset((-6).dp, (-8).dp)
                    .scale(0.92f),
                colorFilter = ColorFilter.tint(Color.Black),
                alpha = 0.45f,
            )
        }
        if (front != EMPTY) {
            key(gen) {
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(initialScale = 0.3f) + fadeIn(),
                ) {
                    CatImage(
                        type = front,
                        bitmaps = bitmaps,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .scale(if (selected) 1.12f else 1f),
                        alpha = if (ghost) 0.3f else 1f,
                    )
                }
            }
        }
        // So meo con trong chong
        if (depth > 1) {
            Text(
                text = "×$depth",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Color(0x88000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
    }
}

fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
