package com.tomasthrawat.prismpop

import android.content.Context

object Progress {
    private const val PREFS = "prismpop_progress"
    private const val KEY_UNLOCKED = "unlocked_level"
    private const val KEY_SOUND = "sound_enabled"

    fun unlockedLevel(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_UNLOCKED, 1)

    fun unlockUpTo(context: Context, level: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_UNLOCKED, 1)
        if (level > current) prefs.edit().putInt(KEY_UNLOCKED, level).apply()
    }

    fun isSoundEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SOUND, true)

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_SOUND, enabled).apply()
    }
}
