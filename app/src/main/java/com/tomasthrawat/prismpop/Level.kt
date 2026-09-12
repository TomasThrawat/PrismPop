package com.tomasthrawat.prismpop

data class Level(val number: Int, val targetScore: Int, val moveLimit: Int)

object Levels {
    val all: List<Level> = (1..10).map { n ->
        Level(number = n, targetScore = 400 + (n - 1) * 250, moveLimit = 20 + (n - 1) * 2)
    }

    fun get(number: Int): Level = all.firstOrNull { it.number == number } ?: all.first()
}
