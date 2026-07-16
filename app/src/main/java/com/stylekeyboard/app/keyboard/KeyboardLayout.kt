package com.stylekeyboard.app.keyboard

/**
 * Gboard-style keyboard layout model.
 *
 * Layers:
 *  - LETTERS  : QWERTY letters (with optional number row above)
 *  - SYMBOLS  : punctuation + digits
 *  - SYMBOLS2 : more punctuation
 *
 * Long-press alternates: each letter carries a list of secondary chars shown in
 * a popup above the key when the user long-presses (e.g. long-press "a" → à á â ã ä å).
 */
sealed class Key {
    data class Letter(
        val label: String,
        val code: Int = label.firstOrNull()?.code ?: 0,
        val alternates: List<String> = emptyList()
    ) : Key()

    data class Action(
        val type: ActionType,
        val label: String = "",
        val icon: KeyIcon? = null
    ) : Key()

    enum class ActionType {
        Space, Delete, Enter, Shift, Symbol, SwitchKeyboard,
        SwitchPreset, Settings, Suggestion, Emoji, Mic, Search, Clipboard
    }
    enum class KeyIcon { Globe, Settings, Shift, ShiftCaps, Delete, Enter, Backspace, Symbol, Letters, Mic, Emoji, Search }
}

object KeyboardLayout {

    val numberRow = listOf(
        Key.Letter("1", alternates = listOf("!", "1", "¹", "½", "⅓", "¼", "⅛")),
        Key.Letter("2", alternates = listOf("@", "2", "²", "⅔")),
        Key.Letter("3", alternates = listOf("#", "3", "³", "¾", "⅜")),
        Key.Letter("4", alternates = listOf("$", "4", "⁴")),
        Key.Letter("5", alternates = listOf("%", "5", "½")),
        Key.Letter("6", alternates = listOf("^", "6")),
        Key.Letter("7", alternates = listOf("&", "7", "⅞")),
        Key.Letter("8", alternates = listOf("*", "8")),
        Key.Letter("9", alternates = listOf("(", "9")),
        Key.Letter("0", alternates = listOf(")", "0", "°", "∅"))
    )

    val letterRow1 = listOf(
        Key.Letter("q", alternates = listOf("1", "q", "Q", "Ạ", "ạ", "φ", "Ω")),
        Key.Letter("w", alternates = listOf("2", "w", "W", "Ẇ", "ẇ", "ψ", "Ψ", "ω")),
        Key.Letter("e", alternates = listOf("3", "e", "E", "É", "é", "È", "è", "Ê", "ê", "Ë", "ë", "ē", "ė", "ę", "€")),
        Key.Letter("r", alternates = listOf("4", "r", "R", "Ŕ", "ŕ", "ř", "RR", "rg", "®")),
        Key.Letter("t", alternates = listOf("5", "t", "T", "Ť", "ť", "Ţ", "ţ", "þ", "™")),
        Key.Letter("y", alternates = listOf("6", "y", "Y", "Ý", "ý", "ÿ", "Ŷ", "ŷ", "¥")),
        Key.Letter("u", alternates = listOf("7", "u", "U", "Ú", "ú", "Ù", "ù", "Û", "û", "Ü", "ü", "Ű", "ű", "Ũ", "ũ", "Ų", "ų", "ū")),
        Key.Letter("i", alternates = listOf("8", "i", "I", "Í", "í", "Ì", "ì", "Î", "î", "Ï", "ï", "Ī", "ī", "Į", "į", "İ")),
        Key.Letter("o", alternates = listOf("9", "o", "O", "Ó", "ó", "Ò", "ò", "Ô", "ô", "Ö", "ö", "Ő", "ő", "Õ", "õ", "Ø", "ø", "ō", "œ")),
        Key.Letter("p", alternates = listOf("0", "p", "P", "¶", "§", "₱", "π", "Π"))
    )

    val letterRow2 = listOf(
        Key.Letter("a", alternates = listOf("a", "A", "Á", "á", "À", "à", "Â", "â", "Ä", "ä", "Æ", "æ", "Ã", "ã", "Å", "å", "Ā", "ā", "Ą", "ą", "α", "Å")),
        Key.Letter("s", alternates = listOf("s", "S", "Ś", "ś", "Š", "š", "Ş", "ş", "ß", "σ", "ς", "Σ")),
        Key.Letter("d", alternates = listOf("d", "D", "Ď", "ď", "Đ", "đ", "Ð", "ð", "Δ", "δ")),
        Key.Letter("f", alternates = listOf("f", "F", "₣", "ƒ", "Φ", "φ", "φ")),
        Key.Letter("g", alternates = listOf("g", "G", "Ĝ", "ĝ", "Ğ", "ğ", "Ģ", "ģ", "γ", "Γ")),
        Key.Letter("h", alternates = listOf("h", "H", "Ĥ", "ĥ", "Ħ", "ħ", "η", "Η")),
        Key.Letter("j", alternates = listOf("j", "J", "Ĵ", "ĵ")),
        Key.Letter("k", alternates = listOf("k", "K", "Ķ", "ķ", "κ", "Κ")),
        Key.Letter("l", alternates = listOf("l", "L", "Ĺ", "ĺ", "Ł", "ł", "Ļ", "ļ", "λ", "Λ"))
    )

    val letterRow3 = listOf(
        Key.Action(Key.ActionType.Shift, icon = Key.KeyIcon.Shift),
        Key.Letter("z", alternates = listOf("z", "Z", "Ź", "ź", "Ž", "ž", "Ż", "ż", "ξ", "Ζ")),
        Key.Letter("x", alternates = listOf("x", "X", "χ", "Χ")),
        Key.Letter("c", alternates = listOf("c", "C", "Ç", "ç", "Ć", "ć", "Č", "č", "©", "¢")),
        Key.Letter("v", alternates = listOf("v", "V", "ω", "Ω")),
        Key.Letter("b", alternates = listOf("b", "B", "β", "Β")),
        Key.Letter("n", alternates = listOf("n", "N", "Ñ", "ñ", "Ń", "ń", "Ņ", "ņ", "ν")),
        Key.Letter("m", alternates = listOf("m", "M", "μ", "M", "µ")),
        Key.Action(Key.ActionType.Delete, icon = Key.KeyIcon.Backspace)
    )

    val letterRow4 = listOf(
        Key.Action(Key.ActionType.SwitchKeyboard, icon = Key.KeyIcon.Globe),
        Key.Action(Key.ActionType.Symbol, label = "?123"),
        Key.Letter(","),
        Key.Action(Key.ActionType.Space, label = "space"),
        Key.Letter("."),
        Key.Action(Key.ActionType.Enter, icon = Key.KeyIcon.Enter)
    )

    // Symbol layer 1
    val symbolRow1 = listOf(
        Key.Letter("1"), Key.Letter("2"), Key.Letter("3"), Key.Letter("4"), Key.Letter("5"),
        Key.Letter("6"), Key.Letter("7"), Key.Letter("8"), Key.Letter("9"), Key.Letter("0")
    )
    val symbolRow2 = listOf(
        Key.Letter("@"), Key.Letter("#"), Key.Letter("$"),
        Key.Letter("_"), Key.Letter("&"), Key.Letter("-"),
        Key.Letter("+"), Key.Letter("("), Key.Letter(")"), Key.Letter("/")
    )
    val symbolRow3 = listOf(
        Key.Action(Key.ActionType.Symbol, label = "=\\<"),
        Key.Letter("*"), Key.Letter("\""), Key.Letter("'"),
        Key.Letter(":"), Key.Letter(";"), Key.Letter("!"), Key.Letter("?"),
        Key.Action(Key.ActionType.Delete, icon = Key.KeyIcon.Backspace)
    )
    val symbolRow4 = listOf(
        Key.Action(Key.ActionType.SwitchKeyboard, icon = Key.KeyIcon.Globe),
        Key.Action(Key.ActionType.Symbol, label = "ABC"),
        Key.Letter(","),
        Key.Action(Key.ActionType.Space, label = "space"),
        Key.Letter("."),
        Key.Action(Key.ActionType.Enter, icon = Key.KeyIcon.Enter)
    )

    // Symbol layer 2 (more symbols)
    val symbol2Row1 = listOf(
        Key.Letter("~"), Key.Letter("`"), Key.Letter("|"),
        Key.Letter("•"), Key.Letter("√"), Key.Letter("π"),
        Key.Letter("÷"), Key.Letter("×"), Key.Letter("¶"), Key.Letter("∆")
    )
    val symbol2Row2 = listOf(
        Key.Letter("£"), Key.Letter("¢"), Key.Letter("€"),
        Key.Letter("¥"), Key.Letter("₹"), Key.Letter("^"),
        Key.Letter("°"), Key.Letter("="), Key.Letter("{"), Key.Letter("}")
    )
    val symbol2Row3 = listOf(
        Key.Action(Key.ActionType.Symbol, label = "?123"),
        Key.Letter("\\"), Key.Letter("%"), Key.Letter("©"),
        Key.Letter("®"), Key.Letter("™"), Key.Letter("✓"),
        Key.Letter("["), Key.Letter("]"),
        Key.Action(Key.ActionType.Delete, icon = Key.KeyIcon.Backspace)
    )
    val symbol2Row4 = listOf(
        Key.Action(Key.ActionType.SwitchKeyboard, icon = Key.KeyIcon.Globe),
        Key.Action(Key.ActionType.Symbol, label = "ABC"),
        Key.Letter(","),
        Key.Action(Key.ActionType.Space, label = "space"),
        Key.Letter("."),
        Key.Action(Key.ActionType.Enter, icon = Key.KeyIcon.Enter)
    )

    fun letterRows(shifted: Boolean): List<List<Key>> {
        if (!shifted) {
            return listOf(numberRow, letterRow1, letterRow2, letterRow3, letterRow4)
        }
        fun shiftRow(row: List<Key>): List<Key> = row.map { k ->
            when (k) {
                is Key.Letter -> k.copy(label = k.label.uppercase(), alternates = k.alternates)
                else -> k
            }
        }
        return listOf(
            shiftRow(numberRow),
            shiftRow(letterRow1),
            shiftRow(letterRow2),
            letterRow3, // shift key already on
            letterRow4
        )
    }

    val symbolRows: List<List<Key>> = listOf(symbolRow1, symbolRow2, symbolRow3, symbolRow4)
    val symbol2Rows: List<List<Key>> = listOf(symbol2Row1, symbol2Row2, symbol2Row3, symbol2Row4)
}
