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
const val EMPTY = -1

enum class Phase { PLAYING, WON, LOST }

data class LevelConfig(val level: Int, val types: Int, val total: Int, val timeSec: Int)

fun levelConfig(level: Int): LevelConfig {
    val types = minOf(6 + (level - 1), MAX_TYPES)
    val total = types * PER_TYPE
    return LevelConfig(level, types, total, 120 + total)
}

/** Trang thai game. slots[i] = chi so loai meo (0..types-1) hoac EMPTY. */
class Game(level: Int) {
    var config by mutableStateOf(levelConfig(level))
        private set

    val slots = mutableStateListOf<Int>()
    val generations = mutableStateListOf<Int>()

    var queue: ArrayDeque<Int> = ArrayDeque()
        private set
    var queueSize by mutableIntStateOf(0)
        private set

    var selectedType by mutableIntStateOf(EMPTY)
        private set
    val selectedSlots = mutableStateListOf<Int>()

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
        repeat(BOARD_SLOTS) {
            slots.add(deck.removeAt(0))
            generations.add(0)
        }
        queue = ArrayDeque(deck)
        queueSize = queue.size
        selectedType = EMPTY
        selectedSlots.clear()
        score = 0
        timeLeft = cfg.timeSec
        refreshLeft = 2
        bonusLeft = 1
        phase = Phase.PLAYING
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

    fun tap(index: Int) {
        if (phase != Phase.PLAYING) return
        val type = slots[index]
        if (type == EMPTY) return
        if (selectedType != type) {
            selectedType = type
            selectedSlots.clear()
            selectedSlots.add(index)
            return
        }
        if (selectedSlots.contains(index)) {
            selectedSlots.remove(index)
            if (selectedSlots.isEmpty()) selectedType = EMPTY
            return
        }
        selectedSlots.add(index)
        if (selectedSlots.size == 3) {
            for (s in selectedSlots) slots[s] = EMPTY
            score += 30
            selectedSlots.clear()
            selectedType = EMPTY
            refill()
            if (queue.isEmpty() && slots.all { it == EMPTY }) phase = Phase.WON
        }
    }

    private fun refill() {
        for (i in slots.indices) {
            if (slots[i] == EMPTY && queue.isNotEmpty()) {
                slots[i] = queue.removeFirst()
                generations[i] = generations[i] + 1
            }
        }
        queueSize = queue.size
    }

    fun refresh() {
        if (phase != Phase.PLAYING || refreshLeft <= 0) return
        refreshLeft--
        selectedType = EMPTY
        selectedSlots.clear()
        val values = slots.filter { it != EMPTY }.shuffled(Random(System.nanoTime()))
        var k = 0
        for (i in slots.indices) {
            if (slots[i] != EMPTY) {
                slots[i] = values[k++]
                generations[i] = generations[i] + 1
            }
        }
    }

    fun addTime() {
        if (phase != Phase.PLAYING || bonusLeft <= 0) return
        bonusLeft--
        timeLeft += 30
    }

    fun remainingCats(): Int = queueSize + slots.count { it != EMPTY }
}
