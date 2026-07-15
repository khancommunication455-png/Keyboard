package com.stylekeyboard.app.util

/**
 * Built-in font presets seeded into Room on first launch.
 *
 * Each preset returns a Map<String,String> from source char -> stylized replacement.
 * Replacements are Unicode characters chosen so the result looks "stylized" in
 * any app that renders standard Unicode (WhatsApp, Instagram, Messenger, etc.).
 *
 * Categories implemented here:
 *   1. Math Sans — Mathematical Alphanumeric Symbols (U+1D400 block)
 *   2. Bold Serif — Mathematical Bold (U+1D400 + 0x10 offset)
 *   3. Upside Down — rotated letters using the "turn upside down" Unicode trick
 *   4. Zalgo — combining diacritics stacked on each char (randomised depth)
 *   5. Bubble Letters — circled Latin letters (U+24B6 / U+24D0)
 */
object DefaultPresets {

    data class Seed(val name: String, val description: String, val mapping: Map<String, String>)

    fun all(): List<Seed> = listOf(
        mathSans(),
        boldSerif(),
        upsideDown(),
        zalgo(),
        bubbleLetters()
    )

    fun mathSans(): Seed {
        val map = LinkedHashMap<String, String>()
        // Mathematical Sans-Serif: uppercase U+1D5A0, lowercase U+1D5BA, digits U+1D7E2
        for (i in 0..25) {
            map[('A' + i).toString()] = (0x1D5A0 + i).toChar().toString()
            map[('a' + i).toString()] = (0x1D5BA + i).toChar().toString()
        }
        for (i in 0..9) {
            map[('0' + i).toString()] = (0x1D7E2 + i).toChar().toString()
        }
        return Seed("Math Sans", "Clean sans-serif mathematical letters", map)
    }

    fun boldSerif(): Seed {
        val map = LinkedHashMap<String, String>()
        // Mathematical Bold: uppercase U+1D400, lowercase U+1D41A, digits U+1D7CE
        for (i in 0..25) {
            map[('A' + i).toString()] = (0x1D400 + i).toChar().toString()
            map[('a' + i).toString()] = (0x1D41A + i).toChar().toString()
        }
        for (i in 0..9) {
            map[('0' + i).toString()] = (0x1D7CE + i).toChar().toString()
        }
        return Seed("Bold Serif", "Bold serif mathematical letters", map)
    }

    fun upsideDown(): Seed {
        val map = LinkedHashMap<String, String>()
        // Build a full A-Z/a-z mapping so the result is fully reversible visually.
        val upside = mapOf(
            'A' to '∀', 'B' to 'ᗺ', 'C' to 'Ↄ', 'D' to 'ᗡ', 'E' to 'Ǝ', 'F' to 'ᖷ',
            'G' to 'ᖵ', 'H' to 'H', 'I' to 'I', 'J' to 'ᒍ', 'K' to 'ʞ', 'L' to '˥',
            'M' to 'W', 'N' to 'N', 'O' to 'O', 'P' to 'Ԁ', 'Q' to 'Ò', 'R' to 'ᴚ',
            'S' to 'S', 'T' to '┴', 'U' to '∩', 'V' to 'Λ', 'W' to 'M', 'X' to 'X',
            'Y' to '⅄', 'Z' to 'Z',
            'a' to 'ɐ', 'b' to 'q', 'c' to 'ɔ', 'd' to 'p', 'e' to 'ǝ', 'f' to 'ɟ',
            'g' to 'ƃ', 'h' to 'ɥ', 'i' to 'ᴉ', 'j' to 'ɾ', 'k' to 'ʞ', 'l' to 'l',
            'm' to 'ɯ', 'n' to 'u', 'o' to 'o', 'p' to 'd', 'q' to 'b', 'r' to 'ɹ',
            's' to 's', 't' to 'ʇ', 'u' to 'n', 'v' to 'ʌ', 'w' to 'ʍ', 'x' to 'x',
            'y' to 'ʎ', 'z' to 'z'
        )
        for (i in 0..25) {
            map[('A' + i).toString()] = (upside['A' + i] ?: ('A' + i)).toString()
            map[('a' + i).toString()] = (upside['a' + i] ?: ('a' + i)).toString()
        }
        for (i in 0..9) {
            map[('0' + i).toString()] = ('0' + i).toString()
        }
        return Seed("Upside Down", "Letters flipped using Unicode", map)
    }

    fun zalgo(): Seed {
        val map = LinkedHashMap<String, String>()
        val combining = listOf(0x0301, 0x0300, 0x0302, 0x0303, 0x0304, 0x0305, 0x0306,
            0x0307, 0x0308, 0x0309, 0x030A, 0x0310, 0x0311, 0x0312, 0x0313, 0x0314,
            0x0315, 0x0316, 0x0317, 0x0318, 0x0319, 0x031A, 0x031B, 0x031C, 0x031D,
            0x031E, 0x031F, 0x0320, 0x0321, 0x0322, 0x0323, 0x0324, 0x0325, 0x0326)
        val rng = java.util.Random(42) // deterministic so seeding is stable
        for (i in 0..25) {
            val up = ('A' + i).toString()
            val lo = ('a' + i).toString()
            map[up] = up + randomCombining(rng, combining, 3)
            map[lo] = lo + randomCombining(rng, combining, 3)
        }
        for (i in 0..9) {
            map[('0' + i).toString()] = ('0' + i).toString()
        }
        return Seed("Zalgo", "Stacked combining diacritics", map)
    }

    fun bubbleLetters(): Seed {
        val map = LinkedHashMap<String, String>()
        // Circled Latin: uppercase U+24B6, lowercase U+24D0
        for (i in 0..25) {
            map[('A' + i).toString()] = (0x24B6 + i).toChar().toString()
            map[('a' + i).toString()] = (0x24D0 + i).toChar().toString()
        }
        for (i in 0..9) {
            map[('0' + i).toString()] = ('0' + i).toString()
        }
        return Seed("Bubble Letters", "Letters inside circles", map)
    }

    private fun randomCombining(rng: java.util.Random, codepoints: List<Int>, count: Int): String {
        val sb = StringBuilder(count * 2)
        repeat(count) {
            sb.appendCodePoint(codepoints[rng.nextInt(codepoints.size)])
        }
        return sb.toString()
    }

    /**
     * Randomize mapping: take a base mapping and assign each key a random symbol
     * from a curated "Unicode art" library. Used by the "Randomize" button in
     * the preset editor.
     */
    fun randomize(base: Map<String, String>): Map<String, String> {
        val library = listOf(
            "★", "☆", "✦", "✧", "✪", "✰", "✫", "✬", "✭", "✮", "✯", "✶", "✷", "✸", "✹",
            "❤", "♡", "♥", "❥", "❣", "❦", "❧",
            "✿", "❀", "❁", "❂", "❃", "❄", "❅", "❆", "❇", "❈", "❉", "❊", "❋",
            "☀", "☁", "☂", "☃", "☄", "★", "☆", "☎", "☑", "☒", "✓", "✔", "✕", "✖",
            "♢", "♦", "♤", "♠", "♧", "♣", "♥", "♡",
            "♭", "♮", "♯", "♩", "♪", "♫", "♬",
            "✠", "✡", "✢", "✣", "✤", "✥", "✦", "✧",
            "Ⓐ", "Ⓑ", "Ⓒ", "Ⓓ", "Ⓔ", "Ⓕ", "Ⓖ", "Ⓗ", "Ⓘ", "Ⓙ", "Ⓚ", "Ⓛ", "Ⓜ",
            "Ⓝ", "Ⓞ", "Ⓟ", "Ⓠ", "Ⓡ", "Ⓢ", "Ⓣ", "Ⓤ", "Ⓥ", "Ⓦ", "Ⓧ", "Ⓨ", "Ⓩ",
            "ⓐ", "ⓑ", "ⓒ", "ⓓ", "ⓔ", "ⓕ", "ⓖ", "ⓗ", "ⓘ", "ⓙ", "ⓚ", "ⓛ", "ⓜ",
            "ⓝ", "ⓞ", "ⓟ", "ⓠ", "ⓡ", "ⓢ", "ⓣ", "ⓤ", "ⓥ", "ⓦ", "ⓧ", "ⓨ", "ⓩ"
        )
        val rng = java.util.Random()
        val out = LinkedHashMap<String, String>()
        for ((k, _) in base) {
            out[k] = library[rng.nextInt(library.size)]
        }
        return out
    }
}
