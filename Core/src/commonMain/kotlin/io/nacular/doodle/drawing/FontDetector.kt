package io.nacular.doodle.drawing

import io.nacular.doodle.drawing.Font.Companion.Normal
import io.nacular.doodle.drawing.Font.Style

/**
 * @property size of the Font
 * @property style of the Font
 * @property weight of the Font
 * @property family of the Font, NOTE: should only be a single family and NOT a comma-separated list
 */
class FontInfo(
        var size  : Int        = -1,
        var style : Set<Style> = setOf(),
        var weight: Int        = Normal,
        var family: String     = "")

/**
 * Provides a way of determining whether a Font has been loaded.  Callers simply
 * provide a [FontInfo] and wait for the [Font] to be returned--or abandon based on an external
 * timer.
 */
interface FontDetector {
    /**
     * Tries to find a loaded font matching the given info.
     *
     * @param info of the font
     * @return the font IFF found
     */
    suspend operator fun invoke(info: FontInfo.() -> Unit): Font

    /**
     * Tries to find a loaded font matching the given font with info overrides.
     *
     * ```kotlin
     * val font = ...
     *
     * detector(font) {
     *  weight = 700 // looks for variant of font with weight == 700
     * }
     * ```
     *
     * @param info of the font
     * @return the font IFF found
     */
    suspend operator fun invoke(font: Font, info: FontInfo.() -> Unit) = invoke {
        size   = font.size
        style  = font.style
        weight = font.weight
        family = font.family
        apply(info)
    }
}