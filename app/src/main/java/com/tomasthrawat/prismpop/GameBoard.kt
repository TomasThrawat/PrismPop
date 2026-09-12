package com.tomasthrawat.prismpop

/**
 * Pure game-state model for the match-3 board: swapping, match detection,
 * cascading clears with gravity, refill and scoring. No Android drawing code lives here.
 */
class GameBoard(val rows: Int = 8, val cols: Int = 8) {

    var grid: Array<Array<GemType>> = Array(rows) { Array(cols) { GemType.random() } }
        private set

    var score: Int = 0
        private set

    init {
        var matches = findMatches()
        while (matches.isNotEmpty()) {
            for ((r, c) in matches) grid[r][c] = GemType.random()
            matches = findMatches()
        }
    }

    fun gemAt(row: Int, col: Int): GemType = grid[row][col]

    private fun inBounds(row: Int, col: Int) = row in 0 until rows && col in 0 until cols

    /** Attempts to swap two adjacent cells. Reverts and returns false if no match results. */
    fun trySwap(r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
        if (!inBounds(r1, c1) || !inBounds(r2, c2)) return false
        val isAdjacent = (kotlin.math.abs(r1 - r2) == 1 && c1 == c2) ||
            (kotlin.math.abs(c1 - c2) == 1 && r1 == r2)
        if (!isAdjacent) return false

        swap(r1, c1, r2, c2)
        if (findMatches().isEmpty()) {
            swap(r1, c1, r2, c2)
            return false
        }
        resolveCascade()
        return true
    }

    private fun swap(r1: Int, c1: Int, r2: Int, c2: Int) {
        val tmp = grid[r1][c1]
        grid[r1][c1] = grid[r2][c2]
        grid[r2][c2] = tmp
    }

    private fun findMatches(): Set<Pair<Int, Int>> {
        val matched = mutableSetOf<Pair<Int, Int>>()

        for (r in 0 until rows) {
            var runStart = 0
            for (c in 1..cols) {
                val sameAsPrev = c < cols && grid[r][c] == grid[r][runStart]
                if (!sameAsPrev) {
                    if (c - runStart >= 3) for (k in runStart until c) matched.add(r to k)
                    runStart = c
                }
            }
        }

        for (c in 0 until cols) {
            var runStart = 0
            for (r in 1..rows) {
                val sameAsPrev = r < rows && grid[r][c] == grid[runStart][c]
                if (!sameAsPrev) {
                    if (r - runStart >= 3) for (k in runStart until r) matched.add(k to c)
                    runStart = r
                }
            }
        }
        return matched
    }

    /** Clears matches, drops remaining gems down, refills from the top, repeats until stable. */
    fun resolveCascade(): Int {
        var totalCleared = 0
        var matches = findMatches()
        while (matches.isNotEmpty()) {
            totalCleared += matches.size
            score += matches.size * 10
            clearAndDrop(matches)
            matches = findMatches()
        }
        return totalCleared
    }

    private fun clearAndDrop(matches: Set<Pair<Int, Int>>) {
        for (c in 0 until cols) {
            val clearedRows = matches.filter { it.second == c }.map { it.first }.toSet()
            if (clearedRows.isEmpty()) continue

            val remaining = (0 until rows).filter { it !in clearedRows }.map { grid[it][c] }
            val newGems = List(rows - remaining.size) { GemType.random() }
            val newColumn = newGems + remaining

            for (r in 0 until rows) grid[r][c] = newColumn[r]
        }
    }

    fun hasAvailableMove(): Boolean {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (c + 1 < cols) {
                    swap(r, c, r, c + 1)
                    val found = findMatches().isNotEmpty()
                    swap(r, c, r, c + 1)
                    if (found) return true
                }
                if (r + 1 < rows) {
                    swap(r, c, r + 1, c)
                    val found = findMatches().isNotEmpty()
                    swap(r, c, r + 1, c)
                    if (found) return true
                }
            }
        }
        return false
    }

    fun reshuffle() {
        do {
            grid = Array(rows) { Array(cols) { GemType.random() } }
        } while (findMatches().isNotEmpty() || !hasAvailableMove())
    }
}
