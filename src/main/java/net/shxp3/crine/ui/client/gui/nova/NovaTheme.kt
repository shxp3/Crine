package net.shxp3.crine.ui.client.gui.nova

import net.shxp3.crine.ui.client.gui.ClickGUIModule
import net.shxp3.crine.ui.client.gui.colortheme.ClientTheme
import net.shxp3.crine.ui.font.Fonts
import net.shxp3.crine.utils.render.RenderUtils
import java.awt.Color

/**
 * Crine "Meridian" design tokens.
 *
 * A sharp, console-grade visual language: hairline rules, square markers,
 * uppercase micro-labels, dense data tables. Deliberately different from the
 * previous dropdown-panel and flat-line styles — no pills, no accent ticks,
 * no dot grids, no numbered rows.
 */
object NovaTheme {

    // ── Surfaces ──────────────────────────────────────────────────────────
    /** Main console sheet. */
    val SHEET = Color(9, 11, 15, 246)
    /** Sunken wells: search field, text inputs, popovers. */
    val WELL = Color(5, 7, 10, 255)
    /** Popover / menu surface. */
    val MENU = Color(11, 14, 19, 250)
    /** Hover wash (drawn as white overlay with this alpha). */
    const val WASH_HOVER = 14
    /** Selected-row wash. */
    const val WASH_SELECTED = 22

    // ── Lines ─────────────────────────────────────────────────────────────
    /** Default 1px rule. */
    val LINE = Color(255, 255, 255, 16)
    /** Stronger rule for inputs / focus borders. */
    val LINE_STRONG = Color(255, 255, 255, 42)
    /** Outline around the whole sheet. */
    val SHEET_LINE = Color(255, 255, 255, 30)

    // ── Text ──────────────────────────────────────────────────────────────
    val TEXT_0 = Color(237, 240, 246, 255)
    val TEXT_1 = Color(152, 160, 176, 255)
    val TEXT_2 = Color(95, 104, 120, 255)
    val TEXT_DIM = Color(70, 77, 92, 255)

    // ── Status ────────────────────────────────────────────────────────────
    val OK = Color(61, 220, 132, 255)
    val ERR = Color(255, 92, 92, 255)
    val WARN = Color(255, 178, 36, 255)

    // ── Geometry ──────────────────────────────────────────────────────────
    const val SHEET_R = 5f
    const val INNER_R = 3f
    const val BOX_R = 2f

    // ── Accent (cached per frame — ClientTheme allocates a Color per call) ─
    private var accentCache: Color = Color(206, 58, 98)
    private var accentFrame = -1
    private var frameCounter = 0

    fun beginFrame() {
        frameCounter++
    }

    fun accent(): Color {
        if (accentFrame != frameCounter) {
            accentFrame = frameCounter
            try {
                accentCache = ClientTheme.getColor(0)
            } catch (_: Throwable) {
                accentCache = Color(206, 58, 98)
            }
        }
        return accentCache
    }

    fun withAlpha(c: Color, a: Int): Color =
        Color(c.red, c.green, c.blue, a.coerceIn(0, 255))

    fun blend(a: Color, b: Color, t: Float): Color {
        val i = 1f - t.coerceIn(0f, 1f)
        val k = t.coerceIn(0f, 1f)
        return Color(
            (a.red * i + b.red * k).toInt().coerceIn(0, 255),
            (a.green * i + b.green * k).toInt().coerceIn(0, 255),
            (a.blue * i + b.blue * k).toInt().coerceIn(0, 255),
            (a.alpha * i + b.alpha * k).toInt().coerceIn(0, 255)
        )
    }

    // ── Motion ────────────────────────────────────────────────────────────
    /** True when the user asked for minimal animation overhead. */
    fun fastRender(): Boolean = try {
        ClickGUIModule.fastRenderValue.get()
    } catch (_: Throwable) {
        false
    }

    /**
     * Frame-rate independent approach. [rate] ~ 8..20 (higher = snappier).
     * FastRender mode uses a much higher rate so motion stays near-instant.
     */
    fun damp(cur: Float, target: Float, rate: Float): Float {
        if (cur == target) return target
        val dt = RenderUtils.deltaTime.coerceAtLeast(1)
        val r = if (fastRender()) rate * 3.2f else rate
        val f = (dt * 0.0016f * r).coerceIn(0f, 1f)
        val next = cur + (target - cur) * f
        return if (kotlin.math.abs(next - target) < 0.0015f) target else next
    }

    fun easeOutCubic(x: Float): Float {
        val t = x.coerceIn(0f, 1f)
        return 1f - (1f - t) * (1f - t) * (1f - t)
    }

    /** Staggered entrance 0..1. */
    fun stagger(elapsedSec: Float, index: Int, staggerSec: Float = 0.05f, durationSec: Float = 0.4f): Float {
        val t = ((elapsedSec - index * staggerSec) / durationSec).coerceIn(0f, 1f)
        return easeOutCubic(t)
    }

    // ── Type ──────────────────────────────────────────────────────────────
    object Type {
        /** Console wordmark / large headings. */
        val display get() = Fonts.SFBold40
        /** Module names, section titles. */
        val title get() = Fonts.SFBold35
        /** Body labels, values. */
        val body get() = Fonts.font30
        /** Body emphasis. */
        val bodyBold get() = Fonts.font30Bold
        /** Micro labels: eyebrows, status, hints. */
        val micro get() = Fonts.font24SemiBold
        /** Tiny labels: tab strip, column heads. */
        val tiny get() = Fonts.font20SemiBold
    }

    /** Uppercase micro label. */
    fun tracked(s: String): String = s.uppercase()
}
