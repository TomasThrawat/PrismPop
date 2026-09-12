package com.tomasthrawat.prismpop

import kotlin.random.Random

/**
 * The six match-3 gem types. Each maps to a vector drawable that was generated
 * procedurally by the custom Engine MCP asset server (CUSTOM_ENGINE_MCP_GENERATE_ICON,
 * kind="gem") and converted from SVG path data into an Android VectorDrawable.
 */
enum class GemType(val colorHex: String, val drawableRes: Int) {
    RED("#e63950", R.drawable.ic_gem_red),
    ORANGE("#f5a623", R.drawable.ic_gem_orange),
    YELLOW("#f4d03f", R.drawable.ic_gem_yellow),
    GREEN("#2ecc71", R.drawable.ic_gem_green),
    BLUE("#3498db", R.drawable.ic_gem_blue),
    PURPLE("#9b59b6", R.drawable.ic_gem_purple);

    companion object {
        fun random(): GemType = entries[Random.nextInt(entries.size)]
    }
}
