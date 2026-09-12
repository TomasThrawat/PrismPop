package com.tomasthrawat.prismpop

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        SoundManager.enabled = Progress.isSoundEnabled(this)

        findViewById<Button>(R.id.playButton).setOnClickListener {
            startActivity(Intent(this, LevelSelectActivity::class.java))
        }

        val soundButton = findViewById<Button>(R.id.soundButton)
        fun refreshSoundLabel() {
            soundButton.text = if (SoundManager.enabled) getString(R.string.sound_on) else getString(R.string.sound_off)
        }
        refreshSoundLabel()
        soundButton.setOnClickListener {
            SoundManager.enabled = !SoundManager.enabled
            Progress.setSoundEnabled(this, SoundManager.enabled)
            refreshSoundLabel()
        }
    }
}
