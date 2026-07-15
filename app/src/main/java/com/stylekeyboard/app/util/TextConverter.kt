package com.stylekeyboard.app.util

/**
 * Core font-conversion engine. Used by both the Host App preview panel and the
 * live keyboard input path, so it must be:
 *   - O(n) in the input length
 *   - allocation-light (no per-char builder churn)
 *   - thread-safe (pure function)
 *
 * Behaviour:
 *   - Each char in the input is looked up in [presetMap].
 *   - If a mapping exists, the replacement is emitted (it may be multiple chars).
 *   - If no mapping exists, the original char is emitted verbatim.
 *   - Multi-char source keys are NOT supported in the per-keystroke path
 *     (it would break O(n)); multi-char source keys are reserved for the
 *     emoji-shortcut engine which uses a separate buffer-scan strategy.
 */
object TextConverter {

    /**
     * Convert [input] using [presetMap]. Returns the converted string.
     *
     * Bench target: ~5μs per keystroke on a mid-range device for a typical 1–200
     * character buffer. Implemented with a single StringBuilder pass.
     */
    fun convertText(inputString: String, presetMap: Map<String, String>): String {
        if (inputString.isEmpty()) return ""
        if (presetMap.isEmpty()) return inputString

        val len = inputString.length
        // Pre-size the builder assuming a small expansion factor.
        val out = StringBuilder(len + (len shr 2))

        for (i in 0 until len) {
            val ch = inputString[i]
            val key = ch.toString()
            val replacement = presetMap[key]
            if (replacement != null) {
                out.append(replacement)
            } else {
                out.append(ch)
            }
        }
        return out.toString()
    }

    /**
     * Convert a single character — used by the keyboard service on each keypress
     * so we don't convert the whole input buffer on every key event.
     */
    fun convertChar(ch: Char, presetMap: Map<String, String>): String =
        presetMap[ch.toString()] ?: ch.toString()

    /**
     * Build a fresh mutable mapping covering A-Z, a-z, 0-9, common punctuation
     * and a small set of common emoji — used by the Preset editor as a starting
     * template when the user creates a new preset from scratch.
     */
    fun emptyMapping(): MutableMap<String, String> {
        val map = LinkedHashMap<String, String>()
        for (c in 'A'..'Z') map[c.toString()] = c.toString()
        for (c in 'a'..'z') map[c.toString()] = c.toString()
        for (c in '0'..'9') map[c.toString()] = c.toString()
        for (c in listOf('!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/',
            ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '_', '`', '{', '|', '}', '~')) {
            map[c.toString()] = c.toString()
        }
        // A few common emoji so users can override them too
        listOf("😀", "😂", "❤", "👍", "🔥", "🎉", "😍", "😢", "😘", "💀").forEach {
            map[it] = it
        }
        return map
    }

    /**
     * Bulk-paste helper: take a string of symbols (no spaces, or space-separated)
     * and assign them sequentially to letters a..z, then A..Z, then 0..9.
     * Skips whitespace in the source. Returns a new map (does not mutate input).
     */
    fun bulkPasteAssign(source: String, base: Map<String, String> = emptyMapping()): Map<String, String> {
        val out = LinkedHashMap(base)
        val targets = (0..25).map { ('a' + it).toString() } +
            (0..25).map { ('A' + it).toString() } +
            (0..9).map { ('0' + it).toString() }
        val symbols = source.filter { !it.isWhitespace() }.toList()
        symbols.forEachIndexed { i, sym ->
            if (i < targets.size) {
                out[targets[i]] = sym.toString()
            }
        }
        return out
    }
}
