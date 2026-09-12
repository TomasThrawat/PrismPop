package com.tomasthrawat.prismpop

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity(), GameListener {

    companion object {
        const val EXTRA_LEVEL = "extra_level"
    }

    private lateinit var level: Level
    private lateinit var gameView: GameView
    private lateinit var levelLabel: TextView
    private lateinit var scoreLabel: TextView
    private lateinit var movesLabel: TextView
    private var movesLeft = 0
    private var currentScore = 0
    private var finished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        SoundManager.enabled = Progress.isSoundEnabled(this)

        val levelNumber = intent.getIntExtra(EXTRA_LEVEL, 1)
        level = Levels.get(levelNumber)
        movesLeft = level.moveLimit

        levelLabel = findViewById(R.id.levelLabel)
        scoreLabel = findViewById(R.id.scoreLabel)
        movesLabel = findViewById(R.id.movesLabel)
        levelLabel.text = getString(R.string.level_format, level.number)
        updateHud()

        gameView = GameView(this)
        gameView.listener = this
        findViewById<FrameLayout>(R.id.gameContainer).addView(gameView)
    }

    private fun updateHud() {
        scoreLabel.text = getString(R.string.score_format, currentScore, level.targetScore)
        movesLabel.text = getString(R.string.moves_format, movesLeft)
    }

    override fun onScoreChanged(score: Int) {
        currentScore = score
        updateHud()
        if (!finished && score >= level.targetScore) {
            finished = true
            Progress.unlockUpTo(this, level.number + 1)
            SoundManager.playLevelWin()
            showResultDialog(won = true)
        }
    }

    override fun onMoveUsed() {
        if (finished) return
        movesLeft -= 1
        updateHud()
        if (movesLeft <= 0 && currentScore < level.targetScore) {
            finished = true
            SoundManager.playLevelLose()
            showResultDialog(won = false)
        }
    }

    private fun showResultDialog(won: Boolean) {
        val isLastLevel = level.number >= Levels.all.size
        val builder = AlertDialog.Builder(this)
            .setTitle(if (won) getString(R.string.level_win_title) else getString(R.string.level_lose_title))
            .setMessage(if (won) getString(R.string.level_win_message) else getString(R.string.level_lose_message))
            .setCancelable(false)

        if (won && !isLastLevel) {
            builder.setPositiveButton(R.string.next_level) { _, _ ->
                startActivity(Intent(this, GameActivity::class.java).putExtra(EXTRA_LEVEL, level.number + 1))
                finish()
            }
            builder.setNegativeButton(R.string.back_to_levels) { _, _ -> finish() }
        } else if (won) {
            builder.setPositiveButton(R.string.back_to_levels) { _, _ -> finish() }
        } else {
            builder.setPositiveButton(R.string.retry) { _, _ ->
                startActivity(Intent(this, GameActivity::class.java).putExtra(EXTRA_LEVEL, level.number))
                finish()
            }
            builder.setNegativeButton(R.string.back_to_levels) { _, _ -> finish() }
        }
        builder.show()
    }
}
