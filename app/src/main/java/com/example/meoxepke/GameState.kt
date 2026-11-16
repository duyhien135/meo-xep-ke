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
const val MAX_TYPES = 13
const val PER_TYPE = 6 // moi loai meo xuat hien 6 con (boi so cua 3)
const val START_EMPTY = 6 // so o trong luc dau de co cho di chuyen meo
const val EMPTY = -1

enum class Phase { PLAYING, WON, LOST }

data class LevelConfig(val level: Int, val types: Int, val total: Int, val timeSec: Int)

fun levelConfig(level: Int): LevelConfig {
    val types = minOf(6 + (level - 1), MAX_TYPES)
    val total = types * PER_TYPE
    return LevelConfig(level, types, total, 120 + total)
}

/** Trang thai game. Moi o la mot chong meo: stacks[i][0] la con truoc mat,
 *  cac con sau hien dang bong den. Ke s gom 3 o lien tiep: [3s, 3s+1, 3s+2].
 *  Cach choi: keo (hoac bam chon roi bam o trong) de chuyen meo sang o khac.
 *  Ke nao du 3 con giong nhau o mat truoc thi 3 con do bien mat. */
class Game(level: Int) {
    var config by mutableStateOf(levelConfig(level))
        private set

    val stacks = mutableStateListOf<List<Int>>()
    val generations = mutableStateListOf<Int>()

    var selectedSlot by mutableIntStateOf(EMPTY)
        private set

    /** Ke vua duoc don + so lan don (de UI hien hieu ung no sang). */
    var clearShelf by mutableIntStateOf(EMPTY)
        private set
    var clearGen by mutableIntStateOf(0)
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

    /** Het o trong de di chuyen (ban bi ket). */
    var stuck by mutableStateOf(false)
        private set

    init {
        setup(config)
    }

    fun frontAt(i: Int): Int = stacks[i].firstOrNull() ?: EMPTY
    fun depthAt(i: Int): Int = stacks[i].size
    fun nextAt(i: Int): Int = stacks[i].getOrNull(1) ?: EMPTY

    private fun setup(cfg: LevelConfig) {
        val deck = mutableListOf<Int>()
        for (t in 0 until cfg.types) repeat(PER_TYPE) { deck.add(t) }
        deck.shuffle(Random(System.nanoTime()))
        // Chia deu bai vao cac chong (tru 5 o de trong).
        val emptySlots = (0 until BOARD_SLOTS).shuffled(Random(System.nanoTime())).take(START_EMPTY).toSet()
        val targets = (0 until BOARD_SLOTS).filter { it !in emptySlots }
        val piles = Array(BOARD_SLOTS) { mutableListOf<Int>() }
        for ((k, cat) in deck.withIndex()) {
            piles[targets[k % targets.size]].add(cat)
        }
        stacks.clear()
        stacks.addAll(piles.map { it.toList() })
        generations.clear()
        repeat(BOARD_SLOTS) { generations.add(0) }
        selectedSlot = EMPTY
        clearShelf = EMPTY
        clearGen = 0
        score = 0
        timeLeft = cfg.timeSec
        refreshLeft = 2
        bonusLeft = 1
        phase = Phase.PLAYING
        stuck = false
        // Neu luc dau da co ke du 3 con giong nhau thi cho bien mat luon.
        resolveClears()
    }

    fun restart() = setup(config)

    fun nextLevel() {
        config = levelConfig(config.level + 1)
        setup(config)
    }

    fun tick() {
        if (phase != Phase.PLAYING || stuck) return
        if (timeLeft > 0) {
            timeLeft--
            if (timeLeft == 0) phase = Phase.LOST
        }
    }

    fun clearSelection() {
        selectedSlot = EMPTY
    }

    /** Bam o: bam meo de cam len, bam o trong de dat xuong, bam meo khac de doi con cam. */
    fun tap(index: Int) {
        if (phase != Phase.PLAYING) return
        if (selectedSlot == EMPTY) {
            if (frontAt(index) == EMPTY) return
            selectedSlot = index
            return
        }
        if (index == selectedSlot) {
            selectedSlot = EMPTY
            return
        }
        if (frontAt(index) != EMPTY) {
            selectedSlot = index
            return
        }
        move(selectedSlot, index)
    }

    /** Chuyen con meo truoc mat tu o [from] sang o trong [to]. */
    fun move(from: Int, to: Int) {
        if (phase != Phase.PLAYING) return
        val moving = stacks[from].firstOrNull() ?: return
        if (stacks[to].isNotEmpty()) return
        stacks[from] = stacks[from].drop(1)
        stacks[to] = listOf(moving)
        generations[from] = generations[from] + 1
        generations[to] = generations[to] + 1
        selectedSlot = EMPTY
        resolveClears()
    }

    /** Xoa cac ke du 3 con giong nhau o mat truoc, lap lai neu mo ra lai du ke. */
    private fun resolveClears() {
        while (true) {
            var clearedAny = false
            var lastShelf = EMPTY
            for (shelf in 0 until SHELF_COUNT) {
                val base = shelf * SLOTS_PER_SHELF
                val a = frontAt(base)
                val b = frontAt(base + 1)
                val c = frontAt(base + 2)
                if (a != EMPTY && a == b && b == c) {
                    stacks[base] = stacks[base].drop(1)
                    stacks[base + 1] = stacks[base + 1].drop(1)
                    stacks[base + 2] = stacks[base + 2].drop(1)
                    generations[base] = generations[base] + 1
                    generations[base + 1] = generations[base + 1] + 1
                    generations[base + 2] = generations[base + 2] + 1
                    score += 30
                    clearedAny = true
                    lastShelf = shelf
                }
            }
            if (!clearedAny) break
            if (lastShelf != EMPTY) {
                clearShelf = lastShelf
                clearGen++
            }
        }
        if (stacks.all { it.isEmpty() }) phase = Phase.WON
        // Ket khi khong con o trong nao de di chuyen (va chua thang).
        stuck = phase == Phase.PLAYING && stacks.none { it.isEmpty() }
    }

    fun refresh() {
        if (phase != Phase.PLAYING || refreshLeft <= 0) return
        refreshLeft--
        selectedSlot = EMPTY
        // Xao cac con mat truoc giua cac chong (giu nguyen do sau moi chong).
        val values = stacks.mapNotNull { it.firstOrNull() }.shuffled(Random(System.nanoTime()))
        var k = 0
        for (i in stacks.indices) {
            if (stacks[i].isNotEmpty()) {
                stacks[i] = listOf(values[k++]) + stacks[i].drop(1)
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

    fun remainingCats(): Int = stacks.sumOf { it.size }
}
