package com.tomasthrawat.prismpop

data class GemDrop(val col: Int, val toRow: Int, val startRowOffset: Int, val gem: GemType)

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

    fun inBounds(row: Int, col: Int) = row in 0 until rows && col in 0 until cols

    fun isAdjacent(r1: Int, c1: Int, r2: Int, c2: Int): Boolean =
        (kotlin.math.abs(r1 - r2) == 1 && c1 == c2) || (kotlin.math.abs(c1 - c2) == 1 && r1 == r2)

    fun swapCells(r1: Int, c1: Int, r2: Int, c2: Int) {
        val tmp = grid[r1][c1]
        grid[r1][c1] = grid[r2][c2]
        grid[r2][c2] = tmp
    }

    /** Swaps, checks for a match, swaps back if there is none. Returns whether the swap is kept. */
    fun trySwap(r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
        if (!inBounds(r1, c1) || !inBounds(r2, c2) || !isAdjacent(r1, c1, r2, c2)) return false
        swapCells(r1, c1, r2, c2)
        if (findMatches().isEmpty()) {
            swapCells(r1, c1, r2, c2)
            return false
        }
        return true
    }

    fun findMatches(): Set<Pair<Int, Int>> {
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

    /**
     * Clears the given matched cells, applies gravity, refills from the top,
     * updates the score, and returns per-gem drop animation info so the view
     * can animate the fall instead of popping straight to the final layout.
     */
    fun clearMatchesWithDrops(matches: Set<Pair<Int, Int>>): List<GemDrop> {
        score += matches.size * 10
        val drops = mutableListOf<GemDrop>()

        for (c in 0 until cols) {
            val clearedRows = matches.filter { it.second == c }.map { it.first }.toSet()
            if (clearedRows.isEmpty()) continue

            val survivorsOldRows = (0 until rows).filter { it !in clearedRows }
            val survivorsGems = survivorsOldRows.map { grid[it][c] }
            val newCount = rows - survivorsGems.size
            val newGems = List(newCount) { GemType.random() }
            val newColumn = newGems + survivorsGems

            for (r in 0 until rows) grid[r][c] = newColumn[r]

            for ((i, oldRow) in survivorsOldRows.withIndex()) {
                val newRow = newCount + i
                val fallDistance = newRow - oldRow
                if (fallDistance > 0) drops.add(GemDrop(c, newRow, fallDistance, newColumn[newRow]))
            }
            for (j in 0 until newCount) {
                drops.add(GemDrop(c, j, newCount, newColumn[j]))
            }
        }
        return drops
    }

    fun hasAvailableMove(): Boolean {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (c + 1 < cols) {
                    swapCells(r, c, r, c + 1)
                    val found = findMatches().isNotEmpty()
                    swapCells(r, c, r, c + 1)
                    if (found) return true
                }
                if (r + 1 < rows) {
                    swapCells(r, c, r + 1, c)
                    val found = findMatches().isNotEmpty()
                    swapCells(r, c, r + 1, c)
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
