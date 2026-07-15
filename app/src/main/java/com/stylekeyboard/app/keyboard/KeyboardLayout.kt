package com.stylekeyboard.app.keyboard

/**
 * Logical representation of a key on the QWERTY layout.
 *
 * - [Letter] keys are converted through the active preset's mapping before
 *   being committed to the input connection.
 * - [Action] keys (space, delete, enter, shift, switch) are handled by the
 *   service directly.
 */
sealed class Key {
    data class Letter(val label: String, val code: Int = label.firstOrNull()?.code ?: 0) : Key()
    data class Action(val type: ActionType, val label: String = "") : Key()

    enum class ActionType { Space, Delete, Enter, Shift, Symbol, SwitchKeyboard, SwitchPreset, Settings, Suggestion }
}

object KeyboardLayout {
    val row1 = listOf(
        Key.Letter("q"), Key.Letter("w"), Key.Letter("e"), Key.Letter("r"), Key.Letter("t"),
        Key.Letter("y"), Key.Letter("u"), Key.Letter("i"), Key.Letter("o"), Key.Letter("p")
    )
    val row2 = listOf(
        Key.Letter("a"), Key.Letter("s"), Key.Letter("d"), Key.Letter("f"), Key.Letter("g"),
        Key.Letter("h"), Key.Letter("j"), Key.Letter("k"), Key.Letter("l")
    )
    val row3 = listOf(
        Key.Action(Key.ActionType.Shift, "⇧"),
        Key.Letter("z"), Key.Letter("x"), Key.Letter("c"), Key.Letter("v"), Key.Letter("b"),
        Key.Letter("n"), Key.Letter("m"),
        Key.Action(Key.ActionType.Delete, "⌫")
    )
    val row4 = listOf(
        Key.Action(Key.ActionType.SwitchPreset, "ABC"),
        Key.Action(Key.ActionType.Symbol, "?123"),
        Key.Letter(","),
        Key.Action(Key.ActionType.Space, "space"),
        Key.Letter("."),
        Key.Action(Key.ActionType.Enter, "⏎")
    )
}
