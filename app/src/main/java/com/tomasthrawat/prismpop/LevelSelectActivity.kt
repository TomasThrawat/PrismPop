package com.tomasthrawat.prismpop

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity

class LevelSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_select)

        val grid = findViewById<GridLayout>(R.id.levelGrid)
        val unlocked = Progress.unlockedLevel(this)

        for (level in Levels.all) {
            val button = Button(this)
            button.text = level.number.toString()
            button.isEnabled = level.number <= unlocked
            button.alpha = if (button.isEnabled) 1f else 0.4f
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(12, 12, 12, 12)
            }
            button.layoutParams = params
            button.setOnClickListener {
                startActivity(Intent(this, GameActivity::class.java).putExtra(GameActivity.EXTRA_LEVEL, level.number))
            }
            grid.addView(button)
        }
    }
}
