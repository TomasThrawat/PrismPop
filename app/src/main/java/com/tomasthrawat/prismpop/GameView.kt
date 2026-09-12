package com.tomasthrawat.prismpop

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat

interface GameListener {
    fun onScoreChanged(score: Int)
    fun onMoveUsed()
}

/**
 * Renders the board and handles tap/swipe input, with real swap / clear / fall
 * animations (no instant pops). Gem pieces are drawn using the VectorDrawables
 * generated from the custom Engine MCP server's SVG output.
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val board = GameBoard()
    var listener: GameListener? = null

    private var cellSize = 0f
    private var boardTop = 0f
    private var selectedRow = -1
    private var selectedCol = -1
    private var downX = 0f
    private var downY = 0f
    private var busy = false

    private var swapR1 = -1; private var swapC1 = -1
    private var swapR2 = -1; private var swapC2 = -1
    private var swapGem1: GemType? = null
    private var swapGem2: GemType? = null
    private var swapProgress = 0f
    private var swapping = false

    private var clearingCells: Set<Pair<Int, Int>> = emptySet()
    private var clearProgress = 0f
    private var clearing = false

    private var fallingDrops: List<GemDrop> = emptyList()
    private var fallProgress = 0f
    private var falling = false

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#26263f") }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; alpha = 60 }
    private val drawableCache = mutableMapOf<GemType, Drawable?>()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        boardTop = h * 0.04f
        val boardHeight = h - boardTop
        cellSize = minOf(w.toFloat() / board.cols, boardHeight / board.rows)
    }

    private fun cellRect(row: Int, col: Int): RectF {
        val left = col * cellSize
        val top = boardTop + row * cellSize
        return RectF(left + 4, top + 4, left + cellSize - 4, top + cellSize - 4)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (r in 0 until board.rows) {
            for (c in 0 until board.cols) {
                val rect = cellRect(r, c)
                canvas.drawRoundRect(rect, 16f, 16f, cellPaint)
                if (r == selectedRow && c == selectedCol && !busy) {
                    canvas.drawRoundRect(rect, 16f, 16f, selectedPaint)
                }
            }
        }

        val skip = mutableSetOf<Pair<Int, Int>>()

        if (swapping) {
            skip.add(swapR1 to swapC1)
            skip.add(swapR2 to swapC2)
            val rect1 = cellRect(swapR1, swapC1)
            val rect2 = cellRect(swapR2, swapC2)
            val cx1 = rect1.centerX() + (rect2.centerX() - rect1.centerX()) * swapProgress
            val cy1 = rect1.centerY() + (rect2.centerY() - rect1.centerY()) * swapProgress
            val cx2 = rect2.centerX() + (rect1.centerX() - rect2.centerX()) * swapProgress
            val cy2 = rect2.centerY() + (rect1.centerY() - rect2.centerY()) * swapProgress
            swapGem1?.let { drawGemAt(canvas, it, cx1, cy1, rect1.width()) }
            swapGem2?.let { drawGemAt(canvas, it, cx2, cy2, rect2.width()) }
        }

        if (clearing) {
            for ((r, c) in clearingCells) skip.add(r to c)
            val scale = 1f - clearProgress
            val alpha = ((1f - clearProgress) * 255).toInt()
            for ((r, c) in clearingCells) {
                val rect = cellRect(r, c)
                drawGemAt(canvas, board.gemAt(r, c), rect.centerX(), rect.centerY(), rect.width() * scale, alpha)
            }
        }

        if (falling) {
            for (drop in fallingDrops) skip.add(drop.toRow to drop.col)
            for (drop in fallingDrops) {
                val targetRect = cellRect(drop.toRow, drop.col)
                val startRect = cellRect(drop.toRow - drop.startRowOffset, drop.col)
                val cy = startRect.centerY() + (targetRect.centerY() - startRect.centerY()) * fallProgress
                drawGemAt(canvas, drop.gem, targetRect.centerX(), cy, targetRect.width())
            }
        }

        for (r in 0 until board.rows) {
            for (c in 0 until board.cols) {
                if (skip.contains(r to c)) continue
                val rect = cellRect(r, c)
                drawGemAt(canvas, board.gemAt(r, c), rect.centerX(), rect.centerY(), rect.width())
            }
        }
    }

    private fun drawGemAt(canvas: Canvas, gem: GemType, cx: Float, cy: Float, size: Float, alpha: Int = 255) {
        val drawable = drawableCache.getOrPut(gem) { ContextCompat.getDrawable(context, gem.drawableRes) } ?: return
        val half = size / 2f * 0.92f
        drawable.alpha = alpha
        drawable.setBounds((cx - half).toInt(), (cy - half).toInt(), (cx + half).toInt(), (cy + half).toInt())
        drawable.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (busy) return true
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x; downY = event.y
                val cell = cellFor(event.x, event.y) ?: return true
                selectedRow = cell.first; selectedCol = cell.second
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                if (selectedRow == -1) return true
                val cell = cellFor(event.x, event.y)
                val dx = event.x - downX
                val dy = event.y - downY

                val target = if (kotlin.math.abs(dx) > cellSize / 4 || kotlin.math.abs(dy) > cellSize / 4) {
                    when {
                        kotlin.math.abs(dx) > kotlin.math.abs(dy) && dx > 0 -> selectedRow to selectedCol + 1
                        kotlin.math.abs(dx) > kotlin.math.abs(dy) -> selectedRow to selectedCol - 1
                        dy > 0 -> selectedRow + 1 to selectedCol
                        else -> selectedRow - 1 to selectedCol
                    }
                } else cell

                if (target != null && board.inBounds(target.first, target.second) &&
                    board.isAdjacent(selectedRow, selectedCol, target.first, target.second)
                ) {
                    startSwap(selectedRow, selectedCol, target.first, target.second)
                } else {
                    selectedRow = -1; selectedCol = -1
                    invalidate()
                }
            }
        }
        return true
    }

    private fun cellFor(x: Float, y: Float): Pair<Int, Int>? {
        if (y < boardTop) return null
        val c = (x / cellSize).toInt()
        val r = ((y - boardTop) / cellSize).toInt()
        if (r !in 0 until board.rows || c !in 0 until board.cols) return null
        return r to c
    }

    private fun startSwap(r1: Int, c1: Int, r2: Int, c2: Int) {
        busy = true
        selectedRow = -1; selectedCol = -1
        swapR1 = r1; swapC1 = c1; swapR2 = r2; swapC2 = c2
        swapGem1 = board.gemAt(r1, c1); swapGem2 = board.gemAt(r2, c2)
        swapping = true
        SoundManager.playSwap()

        val anim = runValueAnimation(180) { progress ->
            swapProgress = progress
            invalidate()
        }
        anim.doOnEndCompat { finishSwapMove(r1, c1, r2, c2) }
    }

    private fun finishSwapMove(r1: Int, c1: Int, r2: Int, c2: Int) {
        swapping = false
        val kept = board.trySwap(r1, c1, r2, c2)
        if (!kept) {
            SoundManager.playInvalidSwap()
            swapR1 = r2; swapC1 = c2; swapR2 = r1; swapC2 = c1
            swapGem1 = board.gemAt(r2, c2); swapGem2 = board.gemAt(r1, c1)
            swapping = true
            val anim = runValueAnimation(180) { progress ->
                swapProgress = progress
                invalidate()
            }
            anim.doOnEndCompat {
                swapping = false
                busy = false
                invalidate()
            }
            return
        }
        runCascade(0)
    }

    private fun runCascade(comboIndex: Int) {
        val matches = board.findMatches()
        if (matches.isEmpty()) {
            listener?.onScoreChanged(board.score)
            listener?.onMoveUsed()
            if (!board.hasAvailableMove()) board.reshuffle()
            busy = false
            invalidate()
            return
        }

        SoundManager.playMatch(comboIndex)
        clearingCells = matches
        clearing = true
        val clearAnim = runValueAnimation(180) { progress ->
            clearProgress = progress
            invalidate()
        }
        clearAnim.doOnEndCompat {
            clearing = false
            val drops = board.clearMatchesWithDrops(matches)
            listener?.onScoreChanged(board.score)
            fallingDrops = drops
            falling = true
            val fallAnim = runValueAnimation(220) { progress ->
                fallProgress = progress
                invalidate()
            }
            fallAnim.doOnEndCompat {
                falling = false
                runCascade(comboIndex + 1)
            }
        }
    }

    private fun runValueAnimation(durationMs: Long, onUpdate: (Float) -> Unit): ValueAnimator {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = durationMs
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { onUpdate(it.animatedValue as Float) }
        animator.start()
        return animator
    }

    private fun ValueAnimator.doOnEndCompat(action: () -> Unit) {
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                action()
            }
        })
    }
}
