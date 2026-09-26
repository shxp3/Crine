package net.shxp3.crine.ui.client.gui.nova

import net.minecraft.client.gui.GuiScreen
import net.minecraft.util.ChatAllowedCharacters
import org.lwjgl.input.Keyboard

/**
 * Minimal single-line text editor for the Meridian console.
 * No GuiTextField involved: raw buffer + caret, styled by the caller.
 */
class NovaTextEdit(var maxLen: Int = 32) {
    var text = ""
    var focused = false
    var onCommit: (() -> Unit)? = null

    fun clearFocus(commit: Boolean = true) {
        if (!focused) return
        focused = false
        if (commit) onCommit?.invoke()
    }

    /**
     * @return true if the key was consumed.
     */
    fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (!focused) return false
        if (GuiScreen.isCtrlKeyDown()) {
            when (keyCode) {
                Keyboard.KEY_A -> {
                    text = ""
                    return true
                }
                Keyboard.KEY_C -> {
                    if (text.isNotEmpty()) GuiScreen.setClipboardString(text)
                    return true
                }
                Keyboard.KEY_V -> {
                    val clip = try {
                        GuiScreen.getClipboardString()
                    } catch (_: Throwable) {
                        null
                    } ?: return true
                    val filtered = clip.filter { ChatAllowedCharacters.isAllowedCharacter(it) }
                    if (filtered.isNotEmpty()) {
                        text = (text + filtered).take(maxLen)
                    }
                    return true
                }
                Keyboard.KEY_X -> {
                    if (text.isNotEmpty()) {
                        GuiScreen.setClipboardString(text)
                        text = ""
                    }
                    return true
                }
            }
        }
        when (keyCode) {
            Keyboard.KEY_BACK -> {
                if (text.isNotEmpty()) text = text.substring(0, text.length - 1)
                return true
            }
            Keyboard.KEY_RETURN, Keyboard.KEY_NUMPADENTER -> {
                clearFocus(commit = true)
                return true
            }
            Keyboard.KEY_ESCAPE -> {
                clearFocus(commit = false)
                return true
            }
        }
        if (ChatAllowedCharacters.isAllowedCharacter(typedChar) && text.length < maxLen) {
            text += typedChar
            return true
        }
        return keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE ||
            keyCode == Keyboard.KEY_LEFT || keyCode == Keyboard.KEY_RIGHT ||
            keyCode == Keyboard.KEY_HOME || keyCode == Keyboard.KEY_END
    }

    companion object {
        fun caretVisible(): Boolean = (System.currentTimeMillis() / 530L) % 2L == 0L
    }
}
