package com.tomasthrawat.prismpop

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat

/**
 * Renders the board and handles tap/swipe input. Gem pieces are drawn using the
 * VectorDrawables generated from the custom Engine MCP server's SVG output
 * (see res/drawable/ic_gem_*.xml).
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val board = GameBoard()

    private var cellSize = 0f
    private var boardTop = 0f
    private var selectedRow = -1
    private var selectedCol = -1
    private var downX = 0f
    private var downY = 0f

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#26263f")
    }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 60
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 56f
        textAlign = Paint.Align.CENTER
    }

    private val drawableCache = mutableMapOf<GemType, Drawable?>()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        boardTop = h * 0.12f
        val boardHeight = h - boardTop
        cellSize = minOf(w.toFloat() / board.cols, boardHeight / board.rows)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawText("Score: ${board.score}", width / 2f, boardTop * 0.6f, textPaint)

        for (r in 0 until board.rows) {
            for (c in 0 until board.cols) {
                val left = c * cellSize
                val top = boardTop + r * cellSize
                val rect = RectF(left + 4, top + 4, left + cellSize - 4, top + cellSize - 4)
                canvas.drawRoundRect(rect, 16f, 16f, cellPaint)

                if (r == selectedRow && c == selectedCol) {
                    canvas.drawRoundRect(rect, 16f, 16f, selectedPaint)
                }

                drawGem(canvas, board.gemAt(r, c), rect)
            }
        }
    }

    private fun drawGem(canvas: Canvas, gem: GemType, rect: RectF) {
        val drawable = drawableCache.getOrPut(gem) {
            ContextCompat.getDrawable(context, gem.drawableRes)
        } ?: return
        val pad = rect.width() * 0.08f
        drawable.setBounds(
            (rect.left + pad).toInt(),
            (rect.top + pad).toInt(),
            (rect.right - pad).toInt(),
            (rect.bottom - pad).toInt()
        )
        drawable.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                val cell = cellFor(event.x, event.y) ?: return true
                selectedRow = cell.first
                selectedCol = cell.second
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                if (selectedRow == -1) return true
                val cell = cellFor(event.x, event.y)
                val dx = event.x - downX
                val dy = event.y - downY

                if (kotlin.math.abs(dx) > cellSize / 4 || kotlin.math.abs(dy) > cellSize / 4) {
                    val target = when {
                        kotlin.math.abs(dx) > kotlin.math.abs(dy) && dx > 0 -> selectedRow to selectedCol + 1
                        kotlin.math.abs(dx) > kotlin.math.abs(dy) -> selectedRow to selectedCol - 1
                        dy > 0 -> selectedRow + 1 to selectedCol
                        else -> selectedRow - 1 to selectedCol
                    }
                    board.trySwap(selectedRow, selectedCol, target.first, target.second)
                } else if (cell != null && (cell.first != selectedRow || cell.second != selectedCol)) {
                    board.trySwap(selectedRow, selectedCol, cell.first, cell.second)
                }

                if (!board.hasAvailableMove()) board.reshuffle()

                selectedRow = -1
                selectedCol = -1
                invalidate()
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
}
