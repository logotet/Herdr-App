package io.github.vladimirvasilev.herdrapp.ui.terminal

/**
 * True when [new] is [old] with exactly one line feed typed into it, i.e. the user pressed Enter
 * on the keyboard. Pasted multi-line text changes more than one character and is kept as is.
 */
fun isEnterPress(old: String, new: String): Boolean {
    if (new.length != old.length + 1) return false
    var i = 0
    while (i < old.length && old[i] == new[i]) i++
    return new[i] == '\n' && new.regionMatches(i + 1, old, i, old.length - i)
}

/** Normalizes compose-box text before it is sent to an agent. */
fun normalizePrompt(text: String): String = text.replace("\r\n", "\n").replace('\r', '\n').trimEnd('\n')
