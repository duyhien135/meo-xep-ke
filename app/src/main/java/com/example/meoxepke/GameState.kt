package com.example.meoxepke

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

const val SHELF_COUNT = 7
const val SLOTS_PER_SHELF = 3
const val BOARD_SLOTS = SHELF_COUNT * SLOTS_PER_SHELF // 21 o hien thi
const val MAX_TYPES = 30
const val PER_TYPE = 12 // moi loai meo xuat hien 12 con (boi so cua 3)
const val START_CATS = 16 // so meo luc dau tren ban
const val MIN_EMPTY = 5 // luon giu it nhat 5 o trong de co cho di chuyen meo
const val EMPTY = -1

enum class Phase { PLAYING, WON, LOST }

data class LevelConfig(val level: Int, val types: Int, val total: Int, val timeSec: Int)

fun levelConfig(level: Int): LevelConfig {
    val types = minOf(6 + (level - 1), MAX_TYPES)
    val total = types * PER_TYPE
    return LevelConfig(level, types, total, 120 + total)
}

/** Trang thai game. slots[i] = chi so loai meo (0..types-1) hoac EMPTY.
 *  Ke s gom 3 o lien tiep: [3s, 3s+1, 3s+2].
 *  Cach choi: bam 1 con meo de cam len, bam o trong de dat xuong ke khac.
 *  Ke nao du 3 con giong nhau thi 3 con do bien mat. */
class Game(level: Int) {
    var config by mutableStateOf(levelConfig(level))
        private set

    val slots = mutableStateListOf<Int>()
    val generations = mutableStateListOf<Int>()

    var queue: ArrayDeque<Int> = ArrayDeque()
        private set
    var queueSize by mutableIntStateOf(0)
        private set

    /** O dang cam meo len (da chon), EMPTY neu khong cam con nao. */
    var selectedSlot by mutableIntStateOf(EMPTY)
        private set

    var score by mutableIntStateOf(0)
        private set
    var timeLeft by mutableIntStateOf(0)
        private set
    var refreshLeft by mutableIntStateOf(2)
        private set
    var bonusLeft by mutableIntStateOf(1)
        private set
    var phase by mutableStateOf(Phase.PLAYING)
        private set

    init {
        setup(config)
    }

    private fun setup(cfg: LevelConfig) {
        val deck = mutableListOf<Int>()
        for (t in 0 until cfg.types) repeat(PER_TYPE) { deck.add(t) }
        deck.shuffle(Random(System.nanoTime()))
        slots.clear()
        generations.clear()
        // Chi lap day mot phan ban de con o trong ma di chuyen meo.
        val filled = (0 until BOARD_SLOTS).shuffled(Random(System.nanoTime())).take(START_CATS).toSet()
        repeat(BOARD_SLOTS) { i ->
            slots.add(if (i in filled) deck.removeAt(0) else EMPTY)
            generations.add(0)
        }
        queue = ArrayDeque(deck)
        queueSize = queue.size
        selectedSlot = EMPTY
        score = 0
        timeLeft = cfg.timeSec
        refreshLeft = 2
        bonusLeft = 1
        phase = Phase.PLAYING
        // Neu luc dau da co ke du 3 con giong nhau thi cho bien mat luon.
        resolveClears()
    }

    fun restart() = setup(config)

    fun nextLevel() {
        config = levelConfig(config.level + 1)
        setup(config)
    }

    fun tick() {
        if (phase != Phase.PLAYING) return
        if (timeLeft > 0) {
            timeLeft--
            if (timeLeft == 0) phase = Phase.LOST
        }
    }

    /** Bam o: bam meo de cam len, bam o trong de dat xuong, bam meo khac de doi con cam. */
    fun tap(index: Int) {
        if (phase != Phase.PLAYING) return
        val type = slots[index]
        if (selectedSlot == EMPTY) {
            if (type == EMPTY) return
            selectedSlot = index
            return
        }
        if (index == selectedSlot) {
            selectedSlot = EMPTY
            return
        }
        if (type != EMPTY) {
            selectedSlot = index
            return
        }
        val moving = slots[selectedSlot]
        slots[selectedSlot] = EMPTY
        slots[index] = moving
        generations[index] = generations[index] + 1
        selectedSlot = EMPTY
        resolveClears()
    }

    /** Xoa cac ke du 3 con giong nhau, lap day meo moi, lap lai neu lap day lai du ke. */
    private fun resolveClears() {
        while (true) {
            var clearedAny = false
            for (shelf in 0 until SHELF_COUNT) {
                val base = shelf * SLOTS_PER_SHELF
                val a = slots[base]
                val b = slots[base + 1]
                val c = slots[base + 2]
                if (a != EMPTY && a == b && b == c) {
                    slots[base] = EMPTY
                    slots[base + 1] = EMPTY
                    slots[base + 2] = EMPTY
                    score += 30
                    clearedAny = true
                }
            }
            if (!clearedAny) break
            refill()
        }
        if (queue.isEmpty() && slots.all { it == EMPTY }) phase = Phase.WON
    }

    /** Lap day meo tu hang doi vao cac o trong, nhung luon giu it nhat MIN_EMPTY o trong. */
    private fun refill() {
        val empty = slots.indices.filter { slots[it] == EMPTY }.shuffled(Random(System.nanoTime()))
        val toFill = (empty.size - MIN_EMPTY).coerceAtLeast(0)
        for (i in 0 until toFill) {
            if (queue.isEmpty()) break
            val idx = empty[i]
            slots[idx] = queue.removeFirst()
            generations[idx] = generations[idx] + 1
        }
        queueSize = queue.size
    }

    fun refresh() {
        if (phase != Phase.PLAYING || refreshLeft <= 0) return
        refreshLeft--
        selectedSlot = EMPTY
        val values = slots.filter { it != EMPTY }.shuffled(Random(System.nanoTime()))
        var k = 0
        for (i in slots.indices) {
            if (slots[i] != EMPTY) {
                slots[i] = values[k++]
                generations[i] = generations[i] + 1
            }
        }
        resolveClears()
    }

    fun addTime() {
        if (phase != Phase.PLAYING || bonusLeft <= 0) return
        bonusLeft--
        timeLeft += 30
    }

    fun remainingCats(): Int = queueSize + slots.count { it != EMPTY }
}
