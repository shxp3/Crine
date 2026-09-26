package net.shxp3.crine.ui.client.gui.nova

import net.shxp3.crine.Crine
import net.shxp3.crine.features.module.Module
import net.shxp3.crine.features.module.ModuleCategory
import net.shxp3.crine.features.module.modules.client.CustomClientColor
import net.shxp3.crine.features.module.modules.client.hud.HUDManager
import net.shxp3.crine.features.module.modules.client.hud.HUDModule
import net.shxp3.crine.features.value.BoolValue
import net.shxp3.crine.features.value.ColorValue
import net.shxp3.crine.features.value.FloatRangeValue
import net.shxp3.crine.features.value.FloatValue
import net.shxp3.crine.features.value.FontValue
import net.shxp3.crine.features.value.IntegerRangeValue
import net.shxp3.crine.features.value.IntegerValue
import net.shxp3.crine.features.value.KeyBindValue
import net.shxp3.crine.features.value.ListValue
import net.shxp3.crine.features.value.NumberValue
import net.shxp3.crine.features.value.OptionValue
import net.shxp3.crine.features.value.TextValue
import net.shxp3.crine.features.value.TitleValue
import net.shxp3.crine.features.value.Value
import net.shxp3.crine.ui.client.gui.colortheme.ClientTheme
import net.shxp3.crine.ui.font.Fonts
import net.shxp3.crine.utils.FontUtils
import net.shxp3.crine.utils.KeybindHelper
import net.shxp3.crine.utils.MouseUtils.mouseWithinBounds
import net.shxp3.crine.utils.render.RenderUtils
import net.shxp3.crine.utils.render.RoundedUtil
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.awt.Color
import java.awt.Desktop
import java.io.File
import java.io.IOException
import kotlin.math.abs

/**
 * Crine "Meridian" console — the redesigned ClickGUI.
 *
 * A single sharp-edged command console: masthead + tab strip + dense data
 * table + inspector column. Replaces the old floating dropdown panels
 * entirely; only the functional contracts (Module/Value/ConfigManager)
 * are reused, nothing visual is carried over.
 *
 * Mouse map:
 *  table row left ....... select (double-click toggles, status cell toggles)
 *  table row right ...... toggle module
 *  table row middle ..... bind key
 *  inspector ............ all settings inline
 */
class NovaClickGui : GuiScreen() {

    private enum class Nav(val label: String) {
        ALL("All"), CLIENT("Client"), VISUAL("Visual"), OTHER("Other"),
        CONFIGS("Configs"), THEME("Theme"), HUD("Hud")
    }

    // ── Hit kinds (inspector) ─────────────────────────────────────────────
    companion object {
        const val K_ENABLE = 1
        const val K_BIND = 2
        const val K_BOOL = 3
        const val K_LIST = 4
        const val K_SLIDER = 5
        const val K_RANGE = 6
        const val K_TEXT = 7
        const val K_COLOR = 8
        const val K_FONT = 9
        const val K_KEYVAL = 10
        const val K_RESET = 11
        const val K_CFG_LOAD = 12
        const val K_CFG_SAVE = 13
        const val K_CFG_DEL = 14
        const val K_CFG_CREATE = 15
        const val K_CFG_OPEN = 16
        const val K_HUD_CENTER = 17
        const val K_CFG_REFRESH = 18

        private var instance: NovaClickGui? = null

        @JvmStatic
        fun getInstance(): NovaClickGui = instance ?: NovaClickGui().also { instance = it }
    }

    private data class Hit(
        val kind: Int, val y0: Float, val y1: Float,
        val ref: Any? = null, val x0: Float = 0f, val x1: Float = 0f,
        val tx: Float = 0f, val tw: Float = 0f, val zone: Int = 0
    )

    private data class Layout(
        val sx: Float, val sy: Float, val sw: Float, val sh: Float,
        val mastH: Float, val tabH: Float, val statusH: Float,
        val contentY: Float, val contentH: Float,
        val tableX: Float, val tableW: Float,
        val inspX: Float, val inspW: Float
    )

    private sealed class Row {
        class Mod(val m: Module) : Row()
        class Cfg(val name: String) : Row()
        class Thm(val name: String) : Row()
        class Hud(val h: HUDModule) : Row()
    }

    // ── State ─────────────────────────────────────────────────────────────
    private var nav = Nav.ALL
    private var placed = false
    private var winX = 0f
    private var winY = 0f
    private var dragging = false
    private var dragDX = 0f
    private var dragDY = 0f
    private var openMs = 0L
    private var openT = 0f
    private var closing = false

    private val tabAnim = mutableMapOf<Nav, Float>()
    private var indX = 0f
    private var indW = 0f
    private var indInit = false

    private val searchEdit = NovaTextEdit(32)
    private var tableTarget = 0f
    private var tableScroll = 0f
    private var inspTarget = 0f
    private var inspScroll = 0f
    private var tableContentH = 0f
    private var inspContentH = 0f

    private var rows: List<Row> = emptyList()
    private val selPerNav = mutableMapOf<Nav, Any?>()
    private var lastClickRef: Any? = null
    private var lastClickMs = 0L

    private var listenModule: Module? = null
    private var listenValue: KeyBindValue? = null

    private var openList: ListValue? = null
    private var openFont: FontValue? = null
    private var popX = 0f
    private var popY = 0f
    private var popW = 0f
    private var popH = 0f
    private var popTarget = 0f
    private var popScroll = 0f

    private var colorValue: ColorValue? = null
    private var colH = 0f
    private var colS = 0f
    private var colB = 0f
    private var colA = 255
    private var colDrag = 0 // 0 none, 1 sv, 2 hue, 3 alpha

    private var dragValue: Value<*>? = null
    private var dragSide = 0 // range: -1 min, +1 max
    private var dragTrackX = 0f
    private var dragTrackW = 1f

    private var textTarget: TextValue? = null
    private val textEdit = NovaTextEdit(64)

    private var configNames = listOf<String>()
    private var creatingConfig = false
    private val newConfigEdit = NovaTextEdit(24)
    private var deleteArmMs = 0L
    private var deleteArmName: String? = null

    /** Marker ref for the custom-color override checkbox hit. */
    private val CUSTOM_OVERRIDE = "nova:custom_override"

    private val hits = ArrayList<Hit>()
    private var frameMouseX = 0
    private var frameMouseY = 0

    // ── Lifecycle ─────────────────────────────────────────────────────────

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        openMs = System.currentTimeMillis()
        openT = 0f
        closing = false
        dragging = false
        dragValue = null
        colDrag = 0
        listenModule = null
        listenValue = null
        openList = null
        openFont = null
        colorValue = null
        if (!placed) {
            placed = true
        }
        refreshConfigs()
        ensureSelection()
        super.initGui()
    }

    override fun onGuiClosed() {
        Keyboard.enableRepeatEvents(false)
        searchEdit.clearFocus(commit = false)
        textEdit.clearFocus(commit = false)
        try {
            Crine.fileManager.saveConfigs()
        } catch (_: Throwable) {
        }
    }

    override fun doesGuiPauseGame() = false

    // ── Layout ────────────────────────────────────────────────────────────

    private fun layout(): Layout {
        val sw = (width - 12).toFloat().coerceAtMost(640f).coerceAtLeast(360f)
        val sh = (height - 20).toFloat().coerceAtMost(412f).coerceAtLeast(240f)
        if (!placed) {
            winX = (width - sw) / 2f
            winY = (height - sh) / 2f
            placed = true
        }
        winX = winX.coerceIn(0f, (width - 60).coerceAtLeast(0).toFloat())
        winY = winY.coerceIn(0f, (height - 40).coerceAtLeast(0).toFloat())
        val mastH = 46f
        val tabH = 30f
        val statusH = 22f
        val contentY = winY + mastH + tabH
        val contentH = sh - mastH - tabH - statusH
        val tableW = (sw * 0.40f).coerceIn(170f, 262f)
        return Layout(winX, winY, sw, sh, mastH, tabH, statusH, contentY, contentH, winX, tableW, winX + tableW + 1f, winX + sw - (winX + tableW + 1f))
    }

    // ── Data ──────────────────────────────────────────────────────────────

    private fun moduleList(): List<Module> = try {
        Crine.moduleManager.modules.toList()
    } catch (_: Throwable) {
        emptyList()
    }

    private fun rebuildRows() {
        val q = searchEdit.text.trim()
        val out = ArrayList<Row>()
        if (nav == Nav.CONFIGS) {
            val sorted = configNames.sorted()
            for (n in sorted) {
                if (q.isEmpty() || n.contains(q, ignoreCase = true)) out.add(Row.Cfg(n))
            }
            rows = out
            return
        }
        if (nav == Nav.THEME) {
            for (n in ClientTheme.THEME_NAMES) {
                if (q.isEmpty() || n.contains(q, ignoreCase = true)) out.add(Row.Thm(n))
            }
            rows = out
            return
        }
        if (nav == Nav.HUD) {
            for (h in HUDManager.huds) {
                if (q.isEmpty() || h.name.contains(q, ignoreCase = true)) out.add(Row.Hud(h))
            }
            rows = out
            return
        }
        val mods = moduleList()
        val filtered = mods.filter { m ->
            val inCat = when (nav) {
                Nav.ALL -> true
                Nav.CLIENT -> m.category == ModuleCategory.CLIENT
                Nav.VISUAL -> m.category == ModuleCategory.VISUAL
                Nav.OTHER -> m.category == ModuleCategory.OTHER
                else -> true
            }
            if (!inCat) return@filter false
            if (q.isEmpty()) return@filter true
            m.name.contains(q, ignoreCase = true) ||
                (m.tag?.contains(q, ignoreCase = true) == true) ||
                m.category.displayName.contains(q, ignoreCase = true)
        }
        val sorted = filtered.sortedWith(compareByDescending<Module> { it.state }.thenBy { it.name.lowercase() })
        for (m in sorted) out.add(Row.Mod(m))
        rows = out
    }

    private fun currentSelection(): Any? {
        val saved = selPerNav[nav]
        if (saved != null) {
            when (saved) {
                is Module -> if (moduleList().contains(saved)) return saved
                is HUDModule -> return saved
                is String -> return saved
            }
        }
        return rows.firstOrNull()?.let {
            when (it) {
                is Row.Mod -> it.m
                is Row.Cfg -> it.name
                is Row.Thm -> "theme:" + it.name
                is Row.Hud -> it.h
            }
        }
    }

    private fun ensureSelection() {
        val cur = currentSelection()
        selPerNav[nav] = cur
    }

    private fun refreshConfigs() {
        try {
            val dir = Crine.fileManager.configsDir
            if (!dir.exists()) dir.mkdirs()
            configNames = dir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".json") }
                ?.map { it.name.removeSuffix(".json") }
                ?.sorted() ?: emptyList()
        } catch (_: Throwable) {
            configNames = emptyList()
        }
    }

    // ── Render ────────────────────────────────────────────────────────────

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        NovaTheme.beginFrame()
        frameMouseX = mouseX
        frameMouseY = mouseY
        val accent = NovaTheme.accent()
        val mx = mouseX.toFloat()
        val my = mouseY.toFloat()

        // open / close motion
        openT = NovaTheme.damp(openT, if (closing) 0f else 1f, 16f)
        if (closing && openT <= 0.01f) {
            mc.displayGuiScreen(null)
            return
        }
        val aMul = NovaTheme.easeOutCubic(openT)
        val rise = (1f - aMul) * 12f

        // dim
        Gui.drawRect(0, 0, width, height, Color(3, 5, 8, (170 * aMul).toInt().coerceIn(0, 170)).rgb)

        val l0 = layout()
        val l = l0.copy(sy = l0.sy + rise, contentY = l0.contentY + rise)
        val sx = l.sx
        val sy = l.sy
        hits.clear()
        rebuildRows()

        NovaRender.sheet(sx, sy, l.sw, l.sh, aMul)
        NovaRender.scissorOn(sx, sy, sx + l.sw, sy + l.sh)

        drawMasthead(l, sx, sy, mx, my, accent, aMul)
        drawTabs(l, sx, sy, mx, my, accent, aMul)
        // vertical split
        NovaRender.vline(l.inspX - 1f, l.contentY, l.contentH)
        drawTable(l, sx, sy, mx, my, accent, aMul)
        drawInspector(l, sx, sy, mx, my, accent, aMul)
        drawStatus(l, sx, sy, mx, my, accent, aMul)

        NovaRender.scissorOff()
        drawPopover(l, mx, my, accent, aMul)

        NovaRender.resetColor()
    }

    private fun drawMasthead(l: Layout, sx: Float, sy: Float, mx: Float, my: Float, accent: Color, aMul: Float) {
        val f = NovaTheme.Type.display
        val title = "CRINE"
        f.drawString(title, sx + 12f, sy + 9f, NovaTheme.withAlpha(NovaTheme.TEXT_0, (255 * aMul).toInt()).rgb, false)
        val ver = "v${Crine.CLIENT_VERSION}"
        val vf = NovaTheme.Type.micro
        vf.drawString(ver, sx + 12f + f.getStringWidth(title) + 5f, sy + 9f + f.FONT_HEIGHT - vf.FONT_HEIGHT - 1f,
            NovaTheme.withAlpha(NovaTheme.TEXT_2, (255 * aMul).toInt()).rgb, false)

        // section caption
        val cap = when (nav) {
            Nav.ALL -> "Modules / All"
            Nav.CLIENT -> "Modules / Client"
            Nav.VISUAL -> "Modules / Visual"
            Nav.OTHER -> "Modules / Other"
            Nav.CONFIGS -> "Configs"
            Nav.THEME -> "Theme"
            Nav.HUD -> "Hud layout"
        }
        vf.drawString(NovaTheme.tracked(cap), sx + 12f, sy + 9f + f.FONT_HEIGHT + 1f,
            NovaTheme.withAlpha(NovaTheme.TEXT_2, (200 * aMul).toInt()).rgb, false)

        // search well (right)
        val sw = 168f
        val sh = 20f
        val bx = sx + l.sw - sw - 10f
        val by = sy + (l.mastH - sh) / 2f - 2f
        NovaRender.well(bx, by, sw, sh, aMul)
        if (searchEdit.focused) {
            RoundedUtil.drawRoundOutline(bx, by, sw, sh, NovaTheme.BOX_R, 1f, Color(0, 0, 0, 0),
                NovaTheme.withAlpha(accent, (230 * aMul).toInt()))
        }
        val tf = NovaTheme.Type.body
        val tx = bx + 7f
        val ty = by + sh / 2f - tf.FONT_HEIGHT / 2f
        if (searchEdit.text.isEmpty() && !searchEdit.focused) {
            tf.drawString("Search", tx, ty, NovaTheme.withAlpha(NovaTheme.TEXT_DIM, (255 * aMul).toInt()).rgb, false)
        } else {
            var shown = searchEdit.text
            while (shown.isNotEmpty() && tf.getStringWidth(shown) > sw - 16f) shown = shown.substring(1)
            tf.drawString(shown, tx, ty, NovaTheme.withAlpha(NovaTheme.TEXT_0, (255 * aMul).toInt()).rgb, false)
            if (searchEdit.focused && NovaTextEdit.caretVisible()) {
                val cx = tx + tf.getStringWidth(shown) + 1f
                Gui.drawRect(cx.toInt(), (ty + 1).toInt(), (cx + 1).toInt(), (ty + tf.FONT_HEIGHT - 1).toInt(),
                    NovaTheme.withAlpha(NovaTheme.TEXT_0, (255 * aMul).toInt()).rgb)
            }
        }
        // search hit stored implicitly: masthead region handled in mouseClicked by coords
        NovaRender.hline(sx, sy + l.mastH, l.sw)
    }

    private fun tabDefs(): List<Pair<Nav, String>> {
        val mods = moduleList()
        fun count(cat: ModuleCategory?) = if (cat == null) mods.size else mods.count { it.category == cat }
        return listOf(
            Nav.ALL to "All ${count(null)}",
            Nav.CLIENT to "Client ${count(ModuleCategory.CLIENT)}",
            Nav.VISUAL to "Visual ${count(ModuleCategory.VISUAL)}",
            Nav.OTHER to "Other ${count(ModuleCategory.OTHER)}",
            Nav.CONFIGS to "Configs",
            Nav.THEME to "Theme",
            Nav.HUD to "Hud"
        )
    }

    private fun drawTabs(l: Layout, sx: Float, sy: Float, mx: Float, my: Float, accent: Color, aMul: Float) {
        val y0 = sy + l.mastH
        val f = NovaTheme.Type.tiny
        var x = sx + 10f
        val midY = y0 + l.tabH / 2f
        var selX = 0f
        var selW = 0f
        for ((n, label) in tabDefs()) {
            val tw = f.getStringWidth(NovaTheme.tracked(label)).toFloat() + 18f
            val hovered = mouseWithinBounds(mx.toInt(), my.toInt(), x, y0, x + tw, y0 + l.tabH)
            val target = if (n == nav) 1f else 0f
            val anim = NovaTheme.damp(tabAnim[n] ?: 0f, target, 14f)
            tabAnim[n] = anim
            val col = NovaTheme.blend(
                NovaTheme.withAlpha(NovaTheme.TEXT_2, (255 * aMul).toInt()),
                NovaTheme.withAlpha(NovaTheme.TEXT_0, (255 * aMul).toInt()),
                (anim * 0.8f + if (hovered) 0.2f else 0f).coerceIn(0f, 1f)
            )
            val ty = midY - f.FONT_HEIGHT / 2f
            f.drawString(NovaTheme.tracked(label), x + 9f, ty, col.rgb, false)
            if (n == nav) {
                selX = x + 9f
                selW = tw - 18f
            }
            x += tw + 2f
        }
        // sliding underline
        if (!indInit) {
            indX = selX
            indW = selW
            indInit = true
        }
        indX = NovaTheme.damp(indX, selX, 18f)
        indW = NovaTheme.damp(indW, selW, 18f)
        if (indW > 1f) {
            Gui.drawRect(indX.toInt(), (y0 + l.tabH - 3).toInt(), (indX + indW).toInt(), (y0 + l.tabH - 2).toInt(),
                NovaTheme.withAlpha(accent, (255 * aMul).toInt()).rgb)
        }
        NovaRender.hline(sx, y0 + l.tabH, l.sw)
    }

    // ── Table ─────────────────────────────────────────────────────────────

    private val tableRowH = 30f
    private val tableHeadH = 18f

    private fun drawTable(l: Layout, sx: Float, sy: Float, mx: Float, my: Float, accent: Color, aMul: Float) {
        val x0 = l.tableX
        val w = l.tableW
        val y0 = l.contentY
        val h = l.contentH
        val headF = NovaTheme.Type.tiny
        val headLabel = when (nav) {
            Nav.CONFIGS -> "Profile"
            Nav.THEME -> "Palette"
            Nav.HUD -> "Element"
            else -> "Module"
        }
        headF.drawString(NovaTheme.tracked("State"), x0 + 8f, y0 + 5f,
            NovaTheme.withAlpha(NovaTheme.TEXT_DIM, (255 * aMul).toInt()).rgb, false)
        headF.drawString(NovaTheme.tracked(headLabel), x0 + 52f, y0 + 5f,
            NovaTheme.withAlpha(NovaTheme.TEXT_DIM, (255 * aMul).toInt()).rgb, false)
        if (nav != Nav.CONFIGS && nav != Nav.THEME) {
            val rk = NovaTheme.tracked("Key")
            headF.drawString(rk, x0 + w - 8f - headF.getStringWidth(rk), y0 + 5f,
                NovaTheme.withAlpha(NovaTheme.TEXT_DIM, (255 * aMul).toInt()).rgb, false)
        }
        NovaRender.hline(x0, y0 + tableHeadH, w)

        val listY = y0 + tableHeadH + 2f
        val listH = h - tableHeadH - 4f
        tableContentH = rows.size * tableRowH
        tableTarget = tableTarget.coerceIn(0f, (tableContentH - listH).coerceAtLeast(0f))
        tableScroll = NovaTheme.damp(tableScroll, tableTarget, 14f)

        if (rows.isEmpty()) {
            val ef = NovaTheme.Type.micro
            val msg = if (searchEdit.text.trim().isEmpty()) "NOTHING HERE" else "NO RESULTS"
            ef.drawString(msg, x0 + w / 2f - ef.getStringWidth(msg) / 2f, listY + 24f,
                NovaTheme.withAlpha(NovaTheme.TEXT_2, (255 * aMul).toInt()).rgb, false)
            return
        }

        NovaRender.scissorOn(x0, listY, x0 + w, listY + listH)
        val sel = currentSelection()
        for ((i, r) in rows.withIndex()) {
            val ry = listY + i * tableRowH - tableScroll
            if (ry + tableRowH < listY || ry > listY + listH) continue
            val hovered = mouseWithinBounds(mx.toInt(), my.toInt(), x0, ry, x0 + w, ry + tableRowH)
            val isSel = when (r) {
                is Row.Mod -> sel is Module && sel == r.m
                is Row.Cfg -> sel is String && sel == r.name
                is Row.Thm -> sel is String && sel == ("theme:" + r.name)
                is Row.Hud -> sel is HUDModule && sel == r.h
            }
            if (isSel) NovaRender.wash(x0 + 1f, ry, w - 2f, tableRowH, (NovaTheme.WASH_SELECTED * aMul).toInt())
            else if (hovered) NovaRender.wash(x0 + 1f, ry, w - 2f, tableRowH, (NovaTheme.WASH_HOVER * aMul).toInt())

            when (r) {
                is Row.Mod -> drawModuleRow(r.m, x0, w, ry, hovered, isSel, accent, aMul)
                is Row.Cfg -> drawConfigRow(r.name, x0, w, ry, hovered, isSel, accent, aMul)
                is Row.Thm -> drawThemeRow(r.name, x0, w, ry, hovered, isSel, accent, aMul)
                is Row.Hud -> drawHudRow(r.h, x0, w, ry, hovered, isSel, accent, aMul)
            }
            if (i != rows.size - 1) NovaRender.hline(x0 + 6f, ry + tableRowH - 1f, w - 12f,
                NovaTheme.withAlpha(NovaTheme.LINE, (255 * aMul).toInt()))
        }
        NovaRender.scissorOff()
        NovaRender.scrollbar(x0 + w - 3f, listY + 2f, listH - 4f, listH, tableContentH, tableScroll, accent)
    }

    private fun drawModuleRow(m: Module, x0: Float, w: Float, ry: Float, hovered: Boolean, isSel: Boolean, accent: Color, aMul: Float) {
        val sf = NovaTheme.Type.micro
        val on = m.state
        val stateCol = if (on) NovaTheme.withAlpha(accent, (255 * aMul).toInt())
        else NovaTheme.withAlpha(NovaTheme.TEXT_DIM, (255 * aMul).toInt())
        val stateTxt = if (on) "ON" else "OFF"
        val cy = ry + tableRowH / 2f
        sf.drawString(stateTxt, x0 + 8f, cy - sf.FONT_HEIGHT / 2f, stateCol.rgb, false)

        val nf = NovaTheme.Type.bodyBold
        val nameCol = if (on || isSel || hovered) NovaTheme.withAlpha(NovaTheme.TEXT_0, (255 * aMul).toInt())
        else NovaTheme.withAlpha(NovaTheme.TEXT_1, (255 * aMul).toInt())
        var nx = x0 + 52f
        val tag = m.tag
        val name = m.name
        val maxNameW = w - 52f - 52f
        val dispName = NovaRender.trimTo(nf, name, maxNameW)
        nf.drawString(dispName, nx, cy - nf.FONT_HEIGHT / 2f - 3f, nameCol.rgb, false)
        nx += nf.getStringWidth(dispName).toFloat() + 4f
        if (!tag.isNullOrEmpty()) {
            val tf = NovaTheme.Type.micro
            val tt = NovaRender.trimTo(tf, tag, (x0 + w - 52f - nx).coerceAtLeast(0f))
            tf.drawString(tt, nx, cy - tf.FONT_HEIGHT / 2f + 5f,
                NovaTheme.withAlpha(NovaTheme.TEXT_2, (255 * aMul).toInt()).rgb, false)
        }
        // key
        val kf = NovaTheme.Type.micro
        val keyTxt = KeybindHelper.getDisplayName(m.keyBind)
        val kw = kf.getStringWidth(keyTxt).toFloat()
        val listening = listenModule == m
        kf.drawString(if (listening) "···" else keyTxt, x0 + w - 8f - kw,
            cy - kf.FONT_HEIGHT / 2f,
            (if (listening) NovaTheme.withAlpha(accent, (255 * aMul).toInt())
            else NovaTheme.withAlpha(NovaTheme.TEXT_2, (255 * aMul).toInt())).rgb, false)
    }

    private fun drawConfigRow(name: String, x0: Float, w: Float, ry: Float, hovered: Boolean, isSel: Boolean, accent: Color, aMul: Float) {
        val active = name == Crine.configManager.nowConfig
        val sf = NovaTheme.Type.micro
        val cy = ry + tableRowH / 2f
        sf.drawString(if (active) "LIVE" else "-", x0 + 8f, cy - sf.FONT_HEIGHT / 2f,
            (if (active) NovaTheme.withAlpha(accent, (255 * aMul).toInt())
            else NovaTheme.withAlpha(NovaTheme.TEXT_DIM, (255 * aMul).toInt())).rgb, false)
        val nf = NovaTheme.Type.bodyBold
        nf.drawString(NovaRender.trimTo(nf, name, w - 60f), x0 + 52f, cy - nf.FONT_HEIGHT / 2f,
            NovaTheme.withAlpha(if (active || isSel || hovered) NovaTheme.TEXT_0 else NovaTheme.TEXT_1, (255 * aMul).toInt()).rgb, false)
    }

    private fun drawThemeRow(name: String, x0: Float, w: Float, ry: Float, hovered: Boolean, isSel: Boolean, accent: Color, aMul: Float) {
        val current = ClientTheme.ClientColorMode.get()
        val active = name.equals(current, ignoreCase = true)
        val cy = ry + tableRowH / 2f
        try {
            val c1 = ClientTheme.getColorFromName(name, 0, 255, false)
            val c2 = ClientTheme.getColorFromName(name, 10, 255, false)
            Gui.drawRect((x0 + 8).toInt(), (cy - 6).toInt(), (x0 + 18).toInt(), (cy + 6).toInt(), c1.rgb)
            Gui.drawRect((x0 + 19).toInt(), (cy - 6).toInt(), (x0 + 29).toInt(), (cy + 6).toInt(), c2.rgb)
        } catch (_: Throwable) {
        }
        val nf = NovaTheme.Type.bodyBold
        nf.drawString(NovaRender.trimTo(nf, name, w - 90f), x0 + 52f, cy - nf.FONT_HEIGHT / 2f,
            NovaTheme.withAlpha(if (active || isSel || hovered) NovaTheme.TEXT_0 else NovaTheme.TEXT_1, (255 * aMul).toInt()).rgb, false)
        if (active) {
            val sf = NovaTheme.Type.micro
            sf.drawString("LIVE", x0 + w - 8f - sf.getStringWidth("LIVE"), cy - sf.FONT_HEIGHT / 2f,
                NovaTheme.withAlpha(accent, (255 * aMul).toInt()).rgb, false)
        }
    }

    private fun drawHudRow(h: HUDModule, x0: Float, w: Float, ry: Float, hovered: Boolean, isSel: Boolean, accent: Color, aMul: Float) {
        val sf = NovaTheme.Type.micro
        val cy = ry + tableRowH / 2f
        sf.drawString(if (h.state) "ON" else "OFF", x0 + 8f, cy - sf.FONT_HEIGHT / 2f,
            (if (h.state) NovaTheme.withAlpha(accent, (255 * aMul).toInt())
            else NovaTheme.withAlpha(NovaTheme.TEXT_DIM, (255 * aMul).toInt())).rgb, false)
        val nf = NovaTheme.Type.bodyBold
        nf.drawString(NovaRender.trimTo(nf, h.name, w - 110f), x0 + 52f, cy - nf.FONT_HEIGHT / 2f,
            NovaTheme.withAlpha(if (h.state || isSel || hovered) NovaTheme.TEXT_0 else NovaTheme.TEXT_1, (255 * aMul).toInt()).rgb, false)
        val kf = NovaTheme.Type.micro
        val pos = "${h.posX.toInt()}, ${h.posY.toInt()}"
        kf.drawString(pos, x0 + w - 8f - kf.getStringWidth(pos), cy - kf.FONT_HEIGHT / 2f,
            NovaTheme.withAlpha(NovaTheme.TEXT_2, (255 * aMul).toInt()).rgb, false)
    }

    // ── Inspector ─────────────────────────────────────────────────────────

    private fun drawInspector(l: Layout, sx: Float, sy: Float, mx: Float, my: Float, accent: Color, aMul: Float) {
        val x0 = l.inspX + 10f
        val w = l.inspW - 20f
        if (w < 60f) return
        val y0 = l.contentY + 6f
        val h = l.contentH - 12f

        NovaRender.scissorOn(l.inspX, l.contentY, l.inspX + l.inspW, l.contentY + l.contentH)
        var y = y0 - inspScroll
        val sel = currentSelection()
        when (nav) {
            Nav.ALL, Nav.CLIENT, Nav.VISUAL, Nav.OTHER -> {
                val m = sel as? Module
                y = if (m != null) drawModuleInspector(m, x0, y, w, mx, my, accent, aMul)
                else drawEmptyInspector(x0, y, w, "Select a module", accent, aMul)
            }
            Nav.CONFIGS -> y = drawConfigInspector(sel as? String, x0, y, w, mx, my, accent, aMul)
            Nav.THEME -> y = drawThemeInspector(sel as? String, x0, y, w, mx, my, accent, aMul)
            Nav.HUD -> {
                val hud = sel as? HUDModule
                y = if (hud != null) drawHudInspector(hud, x0, y, w, mx, my, accent, aMul)
                else drawEmptyInspector(x0, y, w, "Select an element", accent, aMul)
            }
        }
        inspContentH = (y + inspScroll) - y0 + 8f
        NovaRender.scissorOff()
        inspTarget = inspTarget.coerceIn(0f, (inspContentH - h).coerceAtLeast(0f))
        inspScroll = NovaTheme.damp(inspScroll, inspTarget, 14f)
        NovaRender.scrollbar(l.inspX + l.inspW - 3f, l.contentY + 4f, l.contentH - 8f, h, inspContentH, inspScroll, accent)
    }

    private fun drawEmptyInspector(x0: Float, y: Float, w: Float, msg: String, accent: Color, aMul: Float): Float {
        val f = NovaTheme.Type.micro
        f.drawString(NovaTheme.tracked(msg), x0, y + 20f,
            NovaTheme.withAlpha(NovaTheme.TEXT_2, (255 * aMul).toInt()).rgb, false)
        return y + 60f
    }

    private fun inspectorHead(x0: Float, y: Float, w: Float, eyebrow: String, title: String, sub: String?, accent: Color, aMul: Float, mx: Float, my: Float): Float {
        var cy = y
        val ef = NovaTheme.Type.tiny
        ef.drawString(NovaTheme.tracked(eyebrow), x0, cy,
            NovaTheme.withAlpha(accent, (255 * aMul).toInt()).rgb, false)
        cy += ef.FONT_HEIGHT + 4f
        val tf = NovaTheme.Type.display
        tf.drawString(NovaRender.trimTo(tf, title, w), x0, cy,
            NovaTheme.withAlpha(NovaTheme.TEXT_0, (255 * aMul).toInt()).rgb, false)
        cy += tf.FONT_HEIGHT + 4f
        if (!sub.isNullOrEmpty()) {
            val sf = NovaTheme.Type.micro
            // wrap sub across width
            var line = ""
            for (word in sub.split(" ")) {
                val test = if (line.isEmpty()) word else "$line $word"
                if (sf.getStringWidth(test) > w && line.isNotEmpty()) {
                    sf.drawString(line, x0, cy, NovaTheme.withAlpha(NovaTheme.TEXT_2, (255 * aMul).toInt()).rgb, false)
                    cy += sf.FONT_HEIGHT + 2f
                    line = word
                } else {
                    line = test
                }
            }
            if (line.isNotEmpty()) {
                sf.drawString(line, x0, cy, NovaTheme.withAlpha(NovaTheme.TEXT_2, (255 * aMul).toInt()).rgb, false)
                cy += sf.FONT_HEIGHT + 2f
            }
            cy += 2f
        }
        return cy + 4f
    }

    private fun actionButton(x0: Float, y: Float, w: Float, label: String, mx: Float, my: Float,
                             accent: Color, aMul: Float, kind: Int, ref: Any?, primary: Boolean = false,
                             danger: Boolean = false, h: Float = 24f): Float {
        val hovered = mouseWithinBounds(mx.toInt(), my.toInt(), x0, y, x0 + w, y + h)
        val fill = when {
            primary -> NovaTheme.withAlpha(accent, (235 * aMul).toInt())
            danger -> Color(24, 13, 15, (235 * aMul).toInt())
            hovered -> Color(255, 255, 255, (NovaTheme.WASH_SELECTED * aMul).toInt() + 8)
            else -> Color(255, 255, 255, (8 * aMul).toInt())
        }
        RenderUtils.drawRect(x0, y, x0 + w, y + h, fill.rgb)
        val border = when {
            primary -> NovaTheme.withAlpha(accent, (255 * aMul).toInt())
            danger -> NovaTheme.withAlpha(NovaTheme.ERR, ((if (hovered) 255 else 140) * aMul).toInt())
            hovered -> NovaTheme.withAlpha(NovaTheme.LINE_STRONG, (255 * aMul).toInt())
            else -> NovaTheme.withAlpha(NovaTheme.LINE, (255 * aMul).toInt())
        }
        RenderUtils.drawRectBasedBorder(x0, y, x0 + w, y + h, 1f, border.rgb)
        if (primary) {
            // thin dark baseline under primary for weight
            Gui.drawRect(x0.toInt(), (y + h - 2).toInt(), (x0 + w).toInt(), (y + h).toInt(),
                NovaTheme.withAlpha(Color(0, 0, 0), (90 * aMul).toInt()).rgb)
        }
        val f = NovaTheme.Type.micro
        val tcol = when {
            primary -> Color(8, 10, 14, (255 * aMul).toInt())
            danger -> NovaTheme.withAlpha(if (hovered) NovaTheme.ERR else NovaTheme.TEXT_1, (255 * aMul).toInt())
            else -> NovaTheme.withAlpha(if (hovered) NovaTheme.TEXT_0 else NovaTheme.TEXT_1, (255 * aMul).toInt())
        }
        val tw = f.getStringWidth(NovaTheme.tracked(label)).toFloat()
        f.drawString(NovaTheme.tracked(label), x0 + w / 2f - tw / 2f, y + h / 2f - f.FONT_HEIGHT / 2f + 0.5f, tcol.rgb, false)
        hits.add(Hit(kind, y, y + h, ref, x0, x0 + w))
        return y + h
    }

    private fun drawModuleInspector(m: Module, x0: Float, y: Float, w: Float, mx: Float, my: Float, accent: Color, aMul: Float): Float {
        var cy = y
        val sub = buildString {
            append(m.category.displayName)
            val t = m.tag
            if (!t.isNullOrEmpty()) append("  ·  $t")
            append(if (m.state) "  ·  enabled" else "  ·  disabled")
        }
        cy = inspectorHead(x0, cy, w, "Module", m.name, sub, accent, aMul, mx, my)

        // enable button
        cy = actionButton(x0, cy, w, if (m.state) "Disable" else "Enable", mx, my, accent, aMul,
            K_ENABLE, m, primary = !m.state) + 4f

        // bind row
        val bf = NovaTheme.Type.body
        val blabel = "Keybind"
        bf.drawString(blabel, x0, cy + 3f, NovaTheme.withAlpha(NovaTheme.TEXT_1, (255 * aMul).toInt()).rgb, false)
        val pillW = 84f
        val pillH = 20f
        val px = x0 + w - pillW
        val py = cy
        val pillHover = mouseWithinBounds(mx.toInt(), my.toInt(), px, py, px + pillW, py + pillH)
        NovaRender.keyPill(px, py, pillW, pillH, KeybindHelper.getDisplayName(m.keyBind), pillHover, listenModule == m, accent)
        hits.add(Hit(K_BIND, py, py + pillH, m, px, px + pillW))
        cy += pillH + 8f

        NovaRender.hline(x0, cy, w)
        cy += 8f
        NovaRender.sectionHead(x0, cy, w, NovaTheme.tracked("Settings"), accent)
        cy += NovaTheme.Type.tiny.FONT_HEIGHT + 8f

        var shown = 0
        for (v in m.values) {
            if (!v.displayable) continue
            cy = drawValueRow(v, x0, cy, w, mx, my, accent, aMul)
            shown++
        }
        if (shown == 0) {
            val f = NovaTheme.Type.micro
            f.drawString("No adjustable settings.", x0, cy,
                NovaTheme.withAlpha(NovaTheme.TEXT_DIM, (255 * aMul).toInt()).rgb, false)
            cy += f.FONT_HEIGHT + 8f
        }
        cy += 4f
        // reset
        val rf = NovaTheme.Type.micro
        val rlabel = "Reset to defaults"
        val rw = rf.getStringWidth(rlabel).toFloat()
        val rhover = mouseWithinBounds(mx.toInt(), my.toInt(), x0, cy, x0 + rw, cy + rf.FONT_HEIGHT + 4f)
        rf.drawString(rlabel, x0, cy,
            NovaTheme.withAlpha(if (rhover) NovaTheme.TEXT_0 else NovaTheme.TEXT_2, (255 * aMul).toInt()).rgb, false)
        if (rhover) NovaRender.hline(x0, cy + rf.FONT_HEIGHT + 1f, rw, NovaTheme.withAlpha(accent, (255 * aMul).toInt()))
        hits.add(Hit(K_RESET, cy - 2f, cy + rf.FONT_HEIGHT + 4f, m))
        cy += rf.FONT_HEIGHT + 10f
        return cy
    }

    // ── Value rows ────────────────────────────────────────────────────────

    private fun valueLabel(x0: Float, y: Float, w: Float, name: String, aMul: Float): Float {
        val f = NovaTheme.Type.body
        f.drawString(NovaRender.trimTo(f, name, w), x0, y,
            NovaTheme.withAlpha(NovaTheme.TEXT_1, (255 * aMul).toInt()).rgb, false)
        return y + f.FONT_HEIGHT
    }

    private fun drawValueRow(v: Value<*>, x0: Float, y: Float, w: Float, mx: Float, my: Float, accent: Color, aMul: Float): Float {
        when (v) {
            is TitleValue -> {
                NovaRender.sectionHead(x0, y + 4f, w, NovaTheme.tracked(v.name), accent)
                return y + NovaTheme.Type.tiny.FONT_HEIGHT + 12f
            }
            is BoolValue, is OptionValue -> {
                val cur = (v as Value<Boolean>).get()
                val f = NovaTheme.Type.body
                val cy = y + 3f
                f.drawString(NovaRender.trimTo(f, v.name, w - 26f), x0, cy,
                    NovaTheme.withAlpha(NovaTheme.TEXT_1, (255 * aMul).toInt()).rgb, false)
                val bs = 11f
                val bx = x0 + w - bs
                val by = cy + f.FONT_HEIGHT / 2f - bs / 2f
                val hovered = mouseWithinBounds(mx.toInt(), my.toInt(), x0, y, x0 + w, y + 24f)
                val target = if (cur) 1f else 0f
                val key = "b:" + System.identityHashCode(v)
                val anim = NovaTheme.damp(checkAnims[key] ?: 0f, target, 16f)
                checkAnims[key] = anim
                NovaRender.checkbox(bx, by, bs, anim, accent, hovered, aMul)
                hits.add(Hit(K_BOOL, y, y + 24f, v, x0, x0 + w))
                return y + 26f
            }
            is ListValue -> return drawListRow(v, null, x0, y, w, mx, my, accent, aMul)
            is FontValue -> {
                val f = NovaTheme.Type.body
                val cy = y + 3f
                f.drawString(NovaRender.trimTo(f, v.name, w - 110f), x0, cy,
                    NovaTheme.withAlpha(NovaTheme.TEXT_1, (255 * aMul).toInt()).rgb, false)
                val cur = fontDisplayName(v)
                val ctlW = 104f
                val cx = x0 + w - ctlW
                drawStepper(cx, cy - 2f, ctlW, cur, mx, my, accent, aMul, f)
                hits.add(Hit(K_FONT, y, y + 24f, v, cx, cx + ctlW))
                return y + 26f
            }
            is FloatValue -> {
                val max = saneMax(v.minimum.toDouble(), v.maximum.toDouble())
                return drawSingleSlider(x0, y, w, v.name,
                    v.get().toDouble(), v.minimum.toDouble(), max, "",
                    mx, my, accent, aMul, v)
            }
            is IntegerValue -> {
                val max = saneMax(v.minimum.toDouble(), v.maximum.toDouble())
                return drawSingleSlider(x0, y, w, v.name,
                    v.get().toDouble(), v.minimum.toDouble(), max, v.suffix,
                    mx, my, accent, aMul, v)
            }
            is NumberValue -> {
                val max = if (v.maximum >= Double.MAX_VALUE / 2) v.minimum + 100.0 else v.maximum
                return drawSingleSlider(x0, y, w, v.name, v.get(), v.minimum, max, "", mx, my, accent, aMul, v)
            }
            is FloatRangeValue -> {
                val max = saneMax(v.minimum.toDouble(), v.maximum.toDouble())
                return drawRangeSlider(x0, y, w, v.name, v.getMin().toDouble(), v.getMax().toDouble(),
                    v.minimum.toDouble(), max, v.suffix, mx, my, accent, aMul, v)
            }
            is IntegerRangeValue -> {
                val max = saneMax(v.minimum.toDouble(), v.maximum.toDouble())
                return drawRangeSlider(x0, y, w, v.name, v.getMin().toDouble(), v.getMax().toDouble(),
                    v.minimum.toDouble(), max, v.suffix, mx, my, accent, aMul, v)
            }
            is TextValue -> {
                var cy = valueLabel(x0, y + 2f, w, v.name, aMul) + 3f
                val fh = 20f
                val editing = textTarget == v
                val hovered = mouseWithinBounds(mx.toInt(), my.toInt(), x0, cy, x0 + w, cy + fh)
                NovaRender.well(x0, cy, w, fh, aMul)
                if (editing || hovered) {
                    RoundedUtil.drawRoundOutline(x0, cy, w, fh, NovaTheme.BOX_R, 1f, Color(0, 0, 0, 0),
                        NovaTheme.withAlpha(accent, ((if (editing) 255 else 150) * aMul).toInt()))
                }
                val f = NovaTheme.Type.body
                val shown = if (editing) textEdit.text else v.get()
                var s = shown
                while (s.isNotEmpty() && f.getStringWidth(s) > w - 14f) s = s.substring(1)
                val ty = cy + fh / 2f - f.FONT_HEIGHT / 2f
                if (s.isEmpty() && !editing) {
                    f.drawString("Empty", x0 + 7f, ty,
                        NovaTheme.withAlpha(NovaTheme.TEXT_DIM, (255 * aMul).toInt()).rgb, false)
                } else {
                    f.drawString(s, x0 + 7f, ty,
                        NovaTheme.withAlpha(NovaTheme.TEXT_0, (255 * aMul).toInt()).rgb, false)
                    if (editing && NovaTextEdit.caretVisible()) {
                        val cx = x0 + 7f + f.getStringWidth(s) + 1f
                        Gui.drawRect(cx.toInt(), (ty + 1).toInt(), (cx + 1).toInt(), (ty + f.FONT_HEIGHT - 1).toInt(),
                            NovaTheme.withAlpha(NovaTheme.TEXT_0, (255 * aMul).toInt()).rgb)
                    }
                }
                hits.add(Hit(K_TEXT, cy, cy + fh, v, x0, x0 + w))
                return cy + fh + 6f
            }
            is ColorValue -> {
                val f = NovaTheme.Type.body
                val cy = y + 3f
                f.drawString(NovaRender.trimTo(f, v.name, w - 30f), x0, cy,
                    NovaTheme.withAlpha(NovaTheme.TEXT_1, (255 * aMul).toInt()).rgb, false)
                val s = 13f
                val bx = x0 + w - s
                val by = cy + f.FONT_HEIGHT / 2f - s / 2f
                val hovered = mouseWithinBounds(mx.toInt(), my.toInt(), x0, y, x0 + w, y + 24f)
                NovaRender.swatch(bx, by, s, v.get(), hovered)
                hits.add(Hit(K_COLOR, y, y + 24f, v, x0, x0 + w))
                return y + 26f
            }
            is KeyBindValue -> {
                val f = NovaTheme.Type.body
                val cy = y + 3f
                f.drawString(NovaRender.trimTo(f, v.name, w - 90f), x0, cy,
                    NovaTheme.withAlpha(NovaTheme.TEXT_1, (255 * aMul).toInt()).rgb, false)
                val pillW = 76f
                val pillH = 18f
                val px = x0 + w - pillW
                val py = cy + f.FONT_HEIGHT / 2f - pillH / 2f
                val hovered = mouseWithinBounds(mx.toInt(), my.toInt(), px, py, px + pillW, py + pillH)
                NovaRender.keyPill(px, py, pillW, pillH, v.keyName, hovered, listenValue == v, accent)
                hits.add(Hit(K_KEYVAL, y, y + 24f, v, px, px + pillW))
                return y + 26f
            }
            else -> {
                val f = NovaTheme.Type.micro
                f.drawString("${v.name}: ${v.get()}", x0, y + 4f,
                    NovaTheme.withAlpha(NovaTheme.TEXT_DIM, (255 * aMul).toInt()).rgb, false)
                return y + 20f
            }
        }
    }

    private val checkAnims = mutableMapOf<String, Float>()

    private fun drawStepper(x: Float, y: Float, w: Float, cur: String, mx: Float, my: Float,
                            accent: Color, aMul: Float, f: GameFontAlias = NovaTheme.Type.body): Float {
        val h = 18f
        val hovered = mouseWithinBounds(mx.toInt(), my.toInt(), x, y, x + w, y + h)
        NovaRender.well(x, y, w, h, aMul)
        if (hovered) {
            RoundedUtil.drawRoundOutline(x, y, w, h, NovaTheme.BOX_R, 1f, Color(0, 0, 0, 0),
                NovaTheme.withAlpha(accent, (150 * aMul).toInt()))
        }
        val chev = NovaTheme.withAlpha(NovaTheme.TEXT_2, (255 * aMul).toInt())
        f.drawString("<", x + 5f, y + h / 2f - f.FONT_HEIGHT / 2f, chev.rgb, false)
        f.drawString(">", x + w - 5f - f.getStringWidth(">"), y + h / 2f - f.FONT_HEIGHT / 2f, chev.rgb, false)
        val label = NovaRender.trimTo(f, cur, w - 30f)
        f.drawString(label, x + w / 2f - f.getStringWidth(label) / 2f, y + h / 2f - f.FONT_HEIGHT / 2f,
            NovaTheme.withAlpha(NovaTheme.TEXT_0, (255 * aMul).toInt()).rgb, false)
        return h
    }

    private fun drawListRow(v: ListValue, fontV: FontValue?, x0: Float, y: Float, w: Float,
                            mx: Float, my: Float, accent: Color, aMul: Float): Float {
        val f = NovaTheme.Type.body
        val cy = y + 3f
        f.drawString(NovaRender.trimTo(f, v.name, w - 130f), x0, cy,
            NovaTheme.withAlpha(NovaTheme.TEXT_1, (255 * aMul).toInt()).rgb, false)
        val ctlW = 118f
        val cx = x0 + w - ctlW
        drawStepper(cx, cy - 2f, ctlW, v.get(), mx, my, accent, aMul, f)
        hits.add(Hit(K_LIST, y, y + 24f, v, cx, cx + ctlW))
        return y + 26f
    }

    private fun fmtNum(d: Double): String {
        return if (d == d.toLong().toDouble()) d.toLong().toString()
        else String.format("%.2f", d).trimEnd('0').trimEnd('.')
    }

    private fun drawSingleSlider(x0: Float, y: Float, w: Float, name: String, cur: Double, min: Double, max: Double,
                                 suffix: String, mx: Float, my: Float, accent: Color, aMul: Float, ref: Value<*>): Float {
        val f = NovaTheme.Type.body
        var cy = y + 2f
        f.drawString(NovaRender.trimTo(f, name, w - 80f), x0, cy,
            NovaTheme.withAlpha(NovaTheme.TEXT_1, (255 * aMul).toInt()).rgb, false)
        val vf = NovaTheme.Type.micro
        val vtxt = fmtNum(cur) + (if (suffix.isNotEmpty()) " $suffix" else "")
        vf.drawString(vtxt, x0 + w - vf.getStringWidth(vtxt), cy + 1f,
            NovaTheme.withAlpha(accent, (255 * aMul).toInt()).rgb, false)
        cy += f.FONT_HEIGHT + 3f
        val span = (max - min).let { if (it <= 0.0) 1.0 else it }
        val frac = ((cur - min) / span).toFloat().coerceIn(0f, 1f)
        val trackY = cy + 6f
        val hovered = mouseWithinBounds(mx.toInt(), my.toInt(), x0, trackY - 8f, x0 + w, trackY + 8f)
        val active = dragValue == ref
        val thumb = NovaRender.fader(x0, trackY, w, frac, accent, hovered, active)
        hits.add(Hit(K_SLIDER, trackY - 8f, trackY + 8f, ref, x0, x0 + w, x0, w))
        return trackY + 10f
    }

    private fun drawRangeSlider(x0: Float, y: Float, w: Float, name: String, curMin: Double, curMax: Double,
                                min: Double, max: Double, suffix: String, mx: Float, my: Float,
                                accent: Color, aMul: Float, ref: Value<*>): Float {
        val f = NovaTheme.Type.body
        var cy = y + 2f
        f.drawString(NovaRender.trimTo(f, name, w - 110f), x0, cy,
            NovaTheme.withAlpha(NovaTheme.TEXT_1, (255 * aMul).toInt()).rgb, false)
        val vf = NovaTheme.Type.micro
        val vtxt = "${fmtNum(curMin)} - ${fmtNum(curMax)}" + (if (suffix.isNotEmpty()) " $suffix" else "")
        vf.drawString(vtxt, x0 + w - vf.getStringWidth(vtxt), cy + 1f,
            NovaTheme.withAlpha(accent, (255 * aMul).toInt()).rgb, false)
        cy += f.FONT_HEIGHT + 3f
        val span = (max - min).let { if (it <= 0.0) 1.0 else it }
        val f0 = ((curMin - min) / span).toFloat().coerceIn(0f, 1f)
        val f1 = ((curMax - min) / span).toFloat().coerceIn(0f, 1f)
        val trackY = cy + 6f
        val hovered = mouseWithinBounds(mx.toInt(), my.toInt(), x0, trackY - 8f, x0 + w, trackY + 8f)
        // base track + filled span
        NovaRender.hline(x0, trackY, w, NovaTheme.withAlpha(NovaTheme.LINE_STRONG, 200))
        val hx0 = x0 + w * f0
        val hx1 = x0 + w * f1
        if (hx1 - hx0 > 1f) {
            Gui.drawRect(hx0.toInt(), trackY.toInt(), hx1.toInt(), (trackY + 1).toInt(),
                NovaTheme.withAlpha(accent, (if (hovered || dragValue == ref) 255 else 170).toInt()).rgb)
        }
        for (fx in floatArrayOf(f0, f1)) {
            val tx = x0 + w * fx
            val thumb = if (hovered || dragValue == ref) Color(240, 242, 247, 255) else Color(170, 176, 190, 255)
            Gui.drawRect((tx - 2.5f).toInt(), (trackY - 5f).toInt(), (tx + 2.5f).toInt(), (trackY + 6f).toInt(), thumb.rgb)
        }
        hits.add(Hit(K_RANGE, trackY - 8f, trackY + 8f, ref, x0, x0 + w, x0, w))
        return trackY + 10f
    }

    private fun fontDisplayName(v: FontValue): String {
        return try {
            val d = Fonts.getFontDetails(v.get())
            if (d != null) "${d[0]} ${d[1]}" else "Default"
        } catch (_: Throwable) {
            "Default"
        }
    }

    // ── Config inspector ──────────────────────────────────────────────────

    private fun drawConfigInspector(sel: String?, x0: Float, y: Float, w: Float, mx: Float, my: Float, accent: Color, aMul: Float): Float {
        var cy = y
        val name = sel ?: Crine.configManager.nowConfig
        cy = inspectorHead(x0, cy, w, "Profile", name,
            if (name == Crine.configManager.nowConfig) "active configuration" else "stored configuration",
            accent, aMul, mx, my)
        if (name != Crine.configManager.nowConfig) {
            cy = actionButton(x0, cy, w, "Load", mx, my, accent, aMul, K_CFG_LOAD, name, primary = true) + 6f
        } else {
            cy = actionButton(x0, cy, w, "Save current", mx, my, accent, aMul, K_CFG_SAVE, name) + 6f
        }
        val half = (w - 6f) / 2f
        // delete + refresh side by side
        val delLabel = if (deleteArmName == name && System.currentTimeMillis() - deleteArmMs < 3000) "Confirm?" else "Delete"
        val yRow = cy
        val hoverDel = mouseWithinBounds(mx.toInt(), my.toInt(), x0, yRow, x0 + half, yRow + 24f)
        drawMiniButton(x0, yRow, half, delLabel, hoverDel, accent, aMul, danger = true)
        hits.add(Hit(K_CFG_DEL, yRow, yRow + 24f, name, x0, x0 + half))
        val hoverRef = mouseWithinBounds(mx.toInt(), my.toInt(), x0 + half + 6f, yRow, x0 + half + 6f + half, yRow + 24f)
        drawMiniButton(x0 + half + 6f, yRow, half, "Refresh", hoverRef, accent, aMul)
        hits.add(Hit(K_CFG_REFRESH, yRow, yRow + 24f, name, x0 + half + 6f, x0 + half + 6f + half))
        cy = yRow + 24f + 10f

        NovaRender.hline(x0, cy, w)
        cy += 8f
        NovaRender.sectionHead(x0, cy, w, NovaTheme.tracked("New profile"), accent)
        cy += NovaTheme.Type.tiny.FONT_HEIGHT + 8f
        if (creatingConfig) {
            val fh = 22f
            NovaRender.well(x0, cy, w, fh, aMul)
            RoundedUtil.drawRoundOutline(x0, cy, w, fh, NovaTheme.BOX_R, 1f, Color(0, 0, 0, 0),
                NovaTheme.withAlpha(accent, (220 * aMul).toInt()))
            val f = NovaTheme.Type.body
            val ty = cy + fh / 2f - f.FONT_HEIGHT / 2f
            if (newConfigEdit.text.isEmpty() && !NovaTextEdit.caretVisible()) {
                f.drawString("name…", x0 + 7f, ty, NovaTheme.withAlpha(NovaTheme.TEXT_DIM, (255 * aMul).toInt()).rgb, false)
            }
            var s = newConfigEdit.text
            while (s.isNotEmpty() && f.getStringWidth(s) > w - 14f) s = s.substring(1)
            f.drawString(s, x0 + 7f, ty, NovaTheme.withAlpha(NovaTheme.TEXT_0, (255 * aMul).toInt()).rgb, false)
            if (NovaTextEdit.caretVisible()) {
                val cx = x0 + 7f + f.getStringWidth(s) + 1f
                Gui.drawRect(cx.toInt(), (ty + 1).toInt(), (cx + 1).toInt(), (ty + f.FONT_HEIGHT - 1).toInt(),
                    NovaTheme.withAlpha(NovaTheme.TEXT_0, (255 * aMul).toInt()).rgb)
            }
            cy += fh + 6f
            cy = actionButton(x0, cy, w, "Create", mx, my, accent, aMul, K_CFG_CREATE, name, primary = true) + 6f
        } else {
            cy = actionButton(x0, cy, w, "New profile", mx, my, accent, aMul, K_CFG_CREATE, name) + 6f
        }
        cy = actionButton(x0, cy, w, "Open folder", mx, my, accent, aMul, K_CFG_OPEN, name) + 6f
        return cy
    }

    private fun drawMiniButton(x: Float, y: Float, w: Float, label: String, hovered: Boolean, accent: Color, aMul: Float, danger: Boolean = false) {
        val h = 24f
        val fill = if (hovered) Color(255, 255, 255, (NovaTheme.WASH_SELECTED * aMul).toInt() + 8)
        else Color(255, 255, 255, (8 * aMul).toInt())
        RenderUtils.drawRect(x, y, x + w, y + h, fill.rgb)
        val border = if (danger) NovaTheme.withAlpha(NovaTheme.ERR, ((if (hovered) 255 else 120) * aMul).toInt())
        else NovaTheme.withAlpha(if (hovered) NovaTheme.LINE_STRONG else NovaTheme.LINE, (255 * aMul).toInt())
        RenderUtils.drawRectBasedBorder(x, y, x + w, y + h, 1f, border.rgb)
        val f = NovaTheme.Type.micro
        val t = NovaTheme.tracked(label)
        val tw = f.getStringWidth(t).toFloat()
        f.drawString(t, x + w / 2f - tw / 2f, y + h / 2f - f.FONT_HEIGHT / 2f + 0.5f,
            NovaTheme.withAlpha(if (danger && hovered) NovaTheme.ERR else NovaTheme.TEXT_1, (255 * aMul).toInt()).rgb, false)
    }

    // ── Theme inspector ───────────────────────────────────────────────────

    private fun drawThemeInspector(sel: String?, x0: Float, y: Float, w: Float, mx: Float, my: Float, accent: Color, aMul: Float): Float {
        var cy = y
        val current = ClientTheme.ClientColorMode.get()
        cy = inspectorHead(x0, cy, w, "Palette", current, "click a palette on the left to apply it", accent, aMul, mx, my)
        // preview strip
        try {
            val c1 = ClientTheme.getColorFromName(current, 0, 255, false)
            val c2 = ClientTheme.getColorFromName(current, 10, 255, false)
            val bw = (w - 4f) / 2f
            Gui.drawRect(x0.toInt(), cy.toInt(), (x0 + bw).toInt(), (cy + 18).toInt(), c1.rgb)
            Gui.drawRect((x0 + bw + 4f).toInt(), cy.toInt(), (x0 + w).toInt(), (cy + 18).toInt(), c2.rgb)
        } catch (_: Throwable) {
        }
        cy += 26f
        NovaRender.sectionHead(x0, cy, w, NovaTheme.tracked("Motion"), accent)
        cy += NovaTheme.Type.tiny.FONT_HEIGHT + 8f
        val vals = listOf(ClientTheme.fadespeed, ClientTheme.index, ClientTheme.gcdFix,
            ClientTheme.smoothRotation, ClientTheme.smoothFactor,
            ClientTheme.smoothRotationSS, ClientTheme.smoothFactorSS, ClientTheme.fullBody)
        for (v in vals) {
            if (!v.displayable) continue
            cy = drawValueRow(v, x0, cy, w, mx, my, accent, aMul)
        }
        cy += 4f
        NovaRender.sectionHead(x0, cy, w, NovaTheme.tracked("Custom color"), accent)
        cy += NovaTheme.Type.tiny.FONT_HEIGHT + 8f
        run {
            val f = NovaTheme.Type.body
            val ly = cy + 3f
            f.drawString("Override", x0, ly, NovaTheme.withAlpha(NovaTheme.TEXT_1, (255 * aMul).toInt()).rgb, false)
            val bs = 11f
            val bx = x0 + w - bs
            val by = ly + f.FONT_HEIGHT / 2f - bs / 2f
            val hovered = mouseWithinBounds(mx.toInt(), my.toInt(), x0, cy, x0 + w, cy + 24f)
            val key = "b:customoverride"
            val anim = NovaTheme.damp(checkAnims[key] ?: 0f, if (CustomClientColor.state) 1f else 0f, 16f)
            checkAnims[key] = anim
            NovaRender.checkbox(bx, by, bs, anim, accent, hovered, aMul)
            hits.add(Hit(K_BOOL, cy, cy + 24f, CUSTOM_OVERRIDE, x0, x0 + w))
            cy += 26f
        }
        for (v in CustomClientColor.values) {
            if (!v.displayable) continue
            if (v is ColorValue) {
                cy = drawValueRow(v, x0, cy, w, mx, my, accent, aMul)
            }
        }
        cy += 6f
        return cy
    }

    // ── HUD inspector ─────────────────────────────────────────────────────

    private fun drawHudInspector(h: HUDModule, x0: Float, y: Float, w: Float, mx: Float, my: Float, accent: Color, aMul: Float): Float {
        var cy = y
        cy = inspectorHead(x0, cy, w, "Hud element", h.name,
            "pos ${h.posX.toInt()}, ${h.posY.toInt()}  ·  ${if (h.state) "visible" else "hidden"}",
            accent, aMul, mx, my)
        cy = actionButton(x0, cy, w, if (h.state) "Hide" else "Show", mx, my, accent, aMul,
            K_ENABLE, h, primary = !h.state) + 4f
        cy = actionButton(x0, cy, w, "Center on screen", mx, my, accent, aMul, K_HUD_CENTER, h) + 8f
        NovaRender.hline(x0, cy, w)
        cy += 8f
        NovaRender.sectionHead(x0, cy, w, NovaTheme.tracked("Settings"), accent)
        cy += NovaTheme.Type.tiny.FONT_HEIGHT + 8f
        var shown = 0
        for (v in h.values) {
            if (!v.displayable) continue
            cy = drawValueRow(v, x0, cy, w, mx, my, accent, aMul)
            shown++
        }
        if (shown == 0) {
            val f = NovaTheme.Type.micro
            f.drawString("No adjustable settings.", x0, cy,
                NovaTheme.withAlpha(NovaTheme.TEXT_DIM, (255 * aMul).toInt()).rgb, false)
            cy += f.FONT_HEIGHT + 8f
        }
        cy += 4f
        val hf = NovaTheme.Type.micro
        val hint = "Tip: open chat in-game and drag elements directly."
        // wrap hint
        var line = ""
        for (word in hint.split(" ")) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (hf.getStringWidth(test) > w && line.isNotEmpty()) {
                hf.drawString(line, x0, cy, NovaTheme.withAlpha(NovaTheme.TEXT_DIM, (255 * aMul).toInt()).rgb, false)
                cy += hf.FONT_HEIGHT + 2f
                line = word
            } else line = test
        }
        if (line.isNotEmpty()) {
            hf.drawString(line, x0, cy, NovaTheme.withAlpha(NovaTheme.TEXT_DIM, (255 * aMul).toInt()).rgb, false)
            cy += hf.FONT_HEIGHT + 2f
        }
        return cy + 8f
    }

    // ── Status bar ────────────────────────────────────────────────────────

    private fun drawStatus(l: Layout, sx: Float, sy: Float, mx: Float, my: Float, accent: Color, aMul: Float) {
        val y0 = sy + l.sh - l.statusH
        NovaRender.hline(sx, y0, l.sw)
        val f = NovaTheme.Type.tiny
        val mods = moduleList()
        val left = when (nav) {
            Nav.CONFIGS -> "${configNames.size} profiles · live: ${Crine.configManager.nowConfig}"
            Nav.THEME -> "palette: ${ClientTheme.ClientColorMode.get()}"
            Nav.HUD -> "${HUDManager.huds.size} elements"
            else -> "${mods.size} modules · ${mods.count { it.state }} on"
        }
        f.drawString(NovaTheme.tracked(left), sx + 10f, y0 + l.statusH / 2f - f.FONT_HEIGHT / 2f,
            NovaTheme.withAlpha(NovaTheme.TEXT_2, (255 * aMul).toInt()).rgb, false)
        val hint = when {
            listenModule != null || listenValue != null -> "press a key · esc clears"
            colorValue != null -> "pick a color · click away closes"
            else -> "L select · R toggle · M bind"
        }
        val hw = f.getStringWidth(NovaTheme.tracked(hint)).toFloat()
        f.drawString(NovaTheme.tracked(hint), sx + l.sw - 10f - hw, y0 + l.statusH / 2f - f.FONT_HEIGHT / 2f,
            NovaTheme.withAlpha(if (listenModule != null || listenValue != null) accent else NovaTheme.TEXT_DIM, (255 * aMul).toInt()).rgb, false)
    }

    // ── Popovers ──────────────────────────────────────────────────────────

    private fun drawPopover(l: Layout, mx: Float, my: Float, accent: Color, aMul: Float) {
        val list = openList
        val font = openFont
        val col = colorValue
        if (list == null && font == null && col == null) return

        if (list != null) {
            val opts = list.values.toList()
            val rowH = 20f
            val maxRows = 8
            val vis = opts.size.coerceAtMost(maxRows)
            popW = 190f
            popH = vis * rowH + 8f
            // anchor near inspector; keep inside sheet
            val inspR = l.inspX + l.inspW
            popX = (inspR - popW - 12f).coerceAtLeast(l.sx + 8f)
            popY = (l.sy + l.sh - popH - 30f).coerceAtLeast(l.contentY + 4f)
            val contentH = opts.size * rowH
            popTarget = popTarget.coerceIn(0f, (contentH - vis * rowH).coerceAtLeast(0f))
            popScroll = NovaTheme.damp(popScroll, popTarget, 16f)

            NovaRender.menu(popX, popY, popW, popH, aMul)
            NovaRender.scissorOn(popX + 2f, popY + 4f, popX + popW - 2f, popY + popH - 4f)
            val f = NovaTheme.Type.body
            for ((i, o) in opts.withIndex()) {
                val ry = popY + 4f + i * rowH - popScroll
                if (ry + rowH < popY + 4f || ry > popY + popH - 4f) continue
                val hovered = mouseWithinBounds(mx.toInt(), my.toInt(), popX + 4f, ry, popX + popW - 4f, ry + rowH)
                if (hovered) NovaRender.wash(popX + 4f, ry, popW - 8f, rowH, (NovaTheme.WASH_SELECTED * aMul).toInt())
                val cur = o.equals(list.get(), ignoreCase = true)
                f.drawString(NovaRender.trimTo(f, o, popW - 24f), popX + 10f, ry + rowH / 2f - f.FONT_HEIGHT / 2f,
                    (if (cur) NovaTheme.withAlpha(accent, (255 * aMul).toInt())
                    else NovaTheme.withAlpha(if (hovered) NovaTheme.TEXT_0 else NovaTheme.TEXT_1, (255 * aMul).toInt())).rgb, false)
            }
            NovaRender.scissorOff()
            return
        }
        if (font != null) {
            val pairs = try {
                FontUtils.getAllFontDetails().toList()
            } catch (_: Throwable) {
                emptyList()
            }
            val rowH = 20f
            val maxRows = 9
            val vis = pairs.size.coerceAtMost(maxRows).coerceAtLeast(1)
            popW = 220f
            popH = vis * rowH + 8f
            val inspR = l.inspX + l.inspW
            popX = (inspR - popW - 12f).coerceAtLeast(l.sx + 8f)
            popY = (l.sy + l.sh - popH - 30f).coerceAtLeast(l.contentY + 4f)
            val contentH = pairs.size * rowH
            popTarget = popTarget.coerceIn(0f, (contentH - vis * rowH).coerceAtLeast(0f))
            popScroll = NovaTheme.damp(popScroll, popTarget, 16f)
            NovaRender.menu(popX, popY, popW, popH, aMul)
            NovaRender.scissorOn(popX + 2f, popY + 4f, popX + popW - 2f, popY + popH - 4f)
            val f = NovaTheme.Type.body
            val curR = try {
                font.get()
            } catch (_: Throwable) {
                null
            }
            for ((i, p) in pairs.withIndex()) {
                val ry = popY + 4f + i * rowH - popScroll
                if (ry + rowH < popY + 4f || ry > popY + popH - 4f) continue
                val hovered = mouseWithinBounds(mx.toInt(), my.toInt(), popX + 4f, ry, popX + popW - 4f, ry + rowH)
                if (hovered) NovaRender.wash(popX + 4f, ry, popW - 8f, rowH, (NovaTheme.WASH_SELECTED * aMul).toInt())
                val cur = curR != null && curR == p.second
                f.drawString(NovaRender.trimTo(f, p.first, popW - 24f), popX + 10f, ry + rowH / 2f - f.FONT_HEIGHT / 2f,
                    (if (cur) NovaTheme.withAlpha(accent, (255 * aMul).toInt())
                    else NovaTheme.withAlpha(if (hovered) NovaTheme.TEXT_0 else NovaTheme.TEXT_1, (255 * aMul).toInt())).rgb, false)
            }
            NovaRender.scissorOff()
            return
        }
        if (col != null) {
            val hasAlpha = try {
                col.getAlpha()
            } catch (_: Throwable) {
                false
            }
            popW = 196f
            popH = if (hasAlpha) 166f else 150f
            val inspR = l.inspX + l.inspW
            popX = (inspR - popW - 12f).coerceAtLeast(l.sx + 8f)
            popY = (l.sy + l.sh - popH - 30f).coerceAtLeast(l.contentY + 4f)
            NovaRender.menu(popX, popY, popW, popH, aMul)
            val pad = 10f
            val svS = 128f
            val svX = popX + pad
            val svY = popY + pad
            // SV field
            for (iy in 0 until 24) {
                for (ix in 0 until 24) {
                    val s = ix / 23f
                    val b = 1f - iy / 23f
                    val c = Color.getHSBColor(colH, s, b)
                    Gui.drawRect((svX + svS * ix / 24).toInt(), (svY + svS * iy / 24).toInt(),
                        (svX + svS * (ix + 1) / 24 + 1).toInt(), (svY + svS * (iy + 1) / 24 + 1).toInt(), c.rgb)
                }
            }
            // SV cursor
            val curX = svX + colS * svS
            val curY = svY + (1f - colB) * svS
            RenderUtils.drawRectBasedBorder(curX - 3f, curY - 3f, curX + 3f, curY + 3f, 1f, Color.WHITE.rgb)
            // hue strip
            val huX = svX + svS + 8f
            val huW = 12f
            for (iy in 0 until 48) {
                val hh = iy / 47f
                Gui.drawRect(huX.toInt(), (svY + svS * iy / 48).toInt(), (huX + huW).toInt(),
                    (svY + svS * (iy + 1) / 48 + 1).toInt(), Color.getHSBColor(hh, 1f, 1f).rgb)
            }
            val huY = svY + colH * svS
            RenderUtils.drawRect(huX - 2f, huY - 1f, huX + huW + 2f, huY + 1f, Color.WHITE.rgb)
            // alpha strip
            if (hasAlpha) {
                val alY = svY + svS + 8f
                val alH = 8f
                RenderUtils.drawCheckerboard(svX, alY, svS, alH, 3)
                for (ix in 0 until 48) {
                    val aa = ix / 47f
                    val base = Color.getHSBColor(colH, colS, colB)
                    Gui.drawRect((svX + svS * ix / 48).toInt(), alY.toInt(),
                        (svX + svS * (ix + 1) / 48 + 1).toInt(), (alY + alH).toInt(),
                        Color(base.red, base.green, base.blue, (aa * 255).toInt()).rgb)
                }
                val alX = svX + (colA / 255f) * svS
                RenderUtils.drawRect(alX - 1f, alY - 2f, alX + 1f, alY + alH + 2f, Color.WHITE.rgb)
            }
            // current color preview
            NovaRender.swatch(huX, svY + svS + 8f, 12f, col.get(), false)
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────

    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        val mx = mouseX.toFloat()
        val my = mouseY.toFloat()
        val l = layout()

        // key listening capture
        if (listenModule != null || listenValue != null) {
            if (mouseButton > 0) {
                val code = KeybindHelper.codeFromMouseButton(mouseButton)
                listenModule?.let { it.keyBind = code }
                listenValue?.let { it.set(code) }
            }
            listenModule = null
            listenValue = null
            return
        }

        // popover routing
        if (openList != null || openFont != null || colorValue != null) {
            if (mouseWithinBounds(mouseX, mouseY, popX, popY, popX + popW, popY + popH)) {
                handlePopoverClick(mx, my, mouseButton)
                return
            } else {
                // click-away closes (commit color live already)
                if (colorValue != null) pushColor()
                openList = null
                openFont = null
                colorValue = null
                colDrag = 0
                if (mouseButton == 0) return
            }
        }

        val sx = l.sx
        val sy = l.sy
        val inside = mouseWithinBounds(mouseX, mouseY, sx, sy, sx + l.sw, sy + l.sh)
        if (!inside) {
            if (mouseButton == 0) {
                commitTextEdit()
                searchEdit.clearFocus(commit = false)
                beginClose()
            }
            return
        }

        // masthead: search focus + window drag
        val mastY0 = sy
        if (my < mastY0 + l.mastH) {
            val sw = 168f
            val sh = 20f
            val bx = sx + l.sw - sw - 10f
            val by = sy + (l.mastH - sh) / 2f - 2f
            if (mouseWithinBounds(mouseX, mouseY, bx, by, bx + sw, by + sh)) {
                if (mouseButton == 0) {
                    commitTextEdit()
                    searchEdit.focused = true
                }
                return
            }
            searchEdit.clearFocus(commit = false)
            if (mouseButton == 0 && my < mastY0 + l.mastH) {
                dragging = true
                dragDX = mx - winX
                dragDY = my - winY
            }
            return
        }

        // tab strip
        val tabY0 = sy + l.mastH
        if (my < tabY0 + l.tabH) {
            if (mouseButton == 0) {
                val f = NovaTheme.Type.tiny
                var x = sx + 10f
                for ((n, label) in tabDefs()) {
                    val tw = f.getStringWidth(NovaTheme.tracked(label)).toFloat() + 18f
                    if (mouseWithinBounds(mouseX, mouseY, x, tabY0, x + tw, tabY0 + l.tabH)) {
                        if (n != nav) {
                            commitTextEdit()
                            nav = n
                            tableTarget = 0f
                            tableScroll = 0f
                            inspTarget = 0f
                            inspScroll = 0f
                            openList = null
                            openFont = null
                            colorValue = null
                            ensureSelection()
                        }
                        break
                    }
                    x += tw + 2f
                }
            }
            return
        }

        // status bar: nothing clickable
        if (my >= sy + l.sh - l.statusH) return

        // table
        if (mx < l.inspX - 1f) {
            handleTableClick(l, mouseX, mouseY, mouseButton)
            return
        }

        // inspector hits
        if (mouseButton == 0 || mouseButton == 1) {
            for (h in hits) {
                if (my >= h.y0 && my < h.y1 && mx >= l.inspX && mx <= l.inspX + l.inspW &&
                    my >= l.contentY && my < l.contentY + l.contentH
                ) {
                    if (handleInspectorHit(h, mx, my, mouseButton)) return
                }
            }
            // click on empty inspector space commits text edits
            if (mouseButton == 0) commitTextEdit()
        }
    }

    private fun handleTableClick(l: Layout, mouseX: Int, mouseY: Int, btn: Int) {
        val listY = l.contentY + tableHeadH + 2f
        val rel = mouseY - listY + tableScroll
        val i = (rel / tableRowH).toInt()
        if (i < 0 || i >= rows.size) return
        if (mouseY < listY - 1 || mouseY > listY + (l.contentH - tableHeadH - 4f)) return
        val row = rows[i]
        val mx = mouseX.toFloat()
        val statusZone = mx < l.tableX + 48f

        when (row) {
            is Row.Mod -> {
                if (btn == 2) {
                    listenModule = row.m
                    return
                }
                if (btn == 1) {
                    row.m.toggle()
                    return
                }
                // left
                if (statusZone) {
                    row.m.toggle()
                    selPerNav[nav] = row.m
                    return
                }
                val now = System.currentTimeMillis()
                if (lastClickRef == row.m && now - lastClickMs < 320) {
                    row.m.toggle()
                    lastClickRef = null
                } else {
                    selPerNav[nav] = row.m
                    lastClickRef = row.m
                    lastClickMs = now
                    inspTarget = 0f
                    inspScroll = 0f
                }
            }
            is Row.Cfg -> {
                if (btn == 0) {
                    val now = System.currentTimeMillis()
                    if (lastClickRef == row.name && now - lastClickMs < 350) {
                        loadConfig(row.name)
                        lastClickRef = null
                    } else {
                        selPerNav[nav] = row.name
                        lastClickRef = row.name
                        lastClickMs = now
                    }
                } else if (btn == 1) {
                    loadConfig(row.name)
                }
            }
            is Row.Thm -> {
                if (btn == 0) {
                    selPerNav[nav] = "theme:" + row.name
                    applyTheme(row.name)
                }
            }
            is Row.Hud -> {
                if (btn == 2) return
                if (btn == 1) {
                    row.h.toggle()
                    return
                }
                if (statusZone) {
                    row.h.toggle()
                    selPerNav[nav] = row.h
                    return
                }
                selPerNav[nav] = row.h
                inspTarget = 0f
                inspScroll = 0f
            }
        }
    }

    private fun handleInspectorHit(h: Hit, mx: Float, my: Float, btn: Int): Boolean {
        when (h.kind) {
            K_ENABLE -> {
                if (btn != 0) return false
                (h.ref as? Module)?.toggle()
                return true
            }
            K_BIND -> {
                if (btn != 0) return false
                commitTextEdit()
                listenModule = h.ref as? Module
                return true
            }
            K_BOOL -> {
                if (btn != 0) return false
                val ref = h.ref
                if (ref === CUSTOM_OVERRIDE) {
                    CustomClientColor.state = !CustomClientColor.state
                    saveTheme()
                    return true
                }
                val v = ref as? Value<Boolean> ?: return false
                v.set(!v.get())
                afterValueChange(v)
                return true
            }
            K_LIST -> {
                val v = h.ref as? ListValue ?: return false
                if (btn == 1) {
                    openListPopover(v)
                    return true
                }
                if (btn != 0) return false
                // zones: left chevron / middle popover / right chevron
                val w = h.x1 - h.x0
                val lx = h.x0 + w - 118f
                return when {
                    mx < lx + 20f -> {
                        v.prevValue()
                        Crine.configManager.smartSave()
                        afterValueChange(v)
                        true
                    }
                    mx > h.x1 - 20f -> {
                        v.nextValue()
                        Crine.configManager.smartSave()
                        afterValueChange(v)
                        true
                    }
                    else -> {
                        openListPopover(v)
                        true
                    }
                }
            }
            K_FONT -> {
                val v = h.ref as? FontValue ?: return false
                if (btn == 1) {
                    openFontPopover(v)
                    return true
                }
                if (btn != 0) return false
                val w = h.x1 - h.x0
                val lx = h.x0 + w - 104f
                if (mx < lx + 20f) {
                    cycleFont(v, -1)
                    return true
                }
                if (mx > h.x1 - 20f) {
                    cycleFont(v, 1)
                    return true
                }
                openFontPopover(v)
                return true
            }
            K_SLIDER -> {
                if (btn != 0) return false
                val v = h.ref ?: return false
                dragValue = v as? Value<*> ?: return false
                dragTrackX = h.tx
                dragTrackW = h.tw
                applySliderDrag(mx)
                return true
            }
            K_RANGE -> {
                if (btn != 0) return false
                val v = h.ref ?: return false
                dragValue = v as? Value<*> ?: return false
                dragTrackX = h.tx
                dragTrackW = h.tw
                // pick nearest thumb
                val frac = ((mx - h.tx) / h.tw).coerceIn(0f, 1f)
                val (f0, f1) = rangeFracs(v)
                dragSide = if (abs(frac - f0) <= abs(frac - f1)) -1 else 1
                applySliderDrag(mx)
                return true
            }
            K_TEXT -> {
                if (btn != 0) return false
                val v = h.ref as? TextValue ?: return false
                if (textTarget != v) {
                    commitTextEdit()
                    textTarget = v
                    textEdit.text = v.get()
                    textEdit.focused = true
                    textEdit.onCommit = {
                        v.set(textEdit.text)
                        afterValueChange(v)
                        if (textTarget == v) textTarget = null
                    }
                }
                return true
            }
            K_COLOR -> {
                if (btn != 0) return false
                openColorPopover(h.ref as? ColorValue ?: return false)
                return true
            }
            K_KEYVAL -> {
                if (btn != 0) return false
                commitTextEdit()
                listenValue = h.ref as? KeyBindValue
                return true
            }
            K_RESET -> {
                if (btn != 0) return false
                val m = h.ref as? Module ?: return false
                for (v in m.values) {
                    try {
                        v.setDefault()
                    } catch (_: Throwable) {
                    }
                }
                Crine.configManager.smartSave()
                return true
            }
            K_CFG_LOAD -> {
                if (btn != 0) return false
                loadConfig(h.ref as? String ?: return false)
                return true
            }
            K_CFG_SAVE -> {
                if (btn != 0) return false
                try {
                    Crine.configManager.save(forceSave = true)
                } catch (_: Throwable) {
                }
                return true
            }
            K_CFG_DEL -> {
                if (btn != 0) return false
                val name = h.ref as? String ?: return false
                val now = System.currentTimeMillis()
                if (deleteArmName == name && now - deleteArmMs < 3000) {
                    deleteConfig(name)
                    deleteArmName = null
                } else {
                    deleteArmName = name
                    deleteArmMs = now
                }
                return true
            }
            K_CFG_CREATE -> {
                if (btn != 0) return false
                if (!creatingConfig) {
                    creatingConfig = true
                    newConfigEdit.text = ""
                    newConfigEdit.focused = true
                    newConfigEdit.onCommit = { commitNewConfig() }
                } else {
                    commitNewConfig()
                }
                return true
            }
            K_CFG_OPEN -> {
                if (btn != 0) return false
                try {
                    val dir = Crine.fileManager.configsDir
                    if (!dir.exists()) dir.mkdirs()
                    Desktop.getDesktop().open(dir)
                } catch (_: Throwable) {
                }
                return true
            }
            K_CFG_REFRESH -> {
                if (btn != 0) return false
                refreshConfigs()
                return true
            }
            K_HUD_CENTER -> {
                if (btn != 0) return false
                val hud = h.ref as? HUDModule ?: return false
                hud.posX = (width / 2f - 60f).coerceAtLeast(0f)
                hud.posY = (height / 2f - 12f).coerceAtLeast(0f)
                try {
                    Crine.fileManager.saveConfigs()
                } catch (_: Throwable) {
                }
                return true
            }
        }
        return false
    }

    private fun handlePopoverClick(mx: Float, my: Float, btn: Int) {
        val list = openList
        if (list != null) {
            if (btn == 0) {
                val rowH = 20f
                val i = ((my - popY - 4f + popScroll) / rowH).toInt()
                val opts = list.values.toList()
                if (i in opts.indices) {
                    list.set(opts[i])
                    afterValueChange(list)
                    openList = null
                }
            }
            return
        }
        val font = openFont
        if (font != null) {
            if (btn == 0) {
                val rowH = 20f
                val i = ((my - popY - 4f + popScroll) / rowH).toInt()
                val pairs = try {
                    FontUtils.getAllFontDetails().toList()
                } catch (_: Throwable) {
                    emptyList()
                }
                if (i in pairs.indices) {
                    font.set(pairs[i].second)
                    afterValueChange(font)
                    openFont = null
                }
            }
            return
        }
        if (colorValue != null) {
            if (btn == 0) {
                val pad = 10f
                val svS = 128f
                val svX = popX + pad
                val svY = popY + pad
                when {
                    mouseWithinBounds(mx.toInt(), my.toInt(), svX, svY, svX + svS, svY + svS) -> {
                        colDrag = 1
                        colS = ((mx - svX) / svS).coerceIn(0f, 1f)
                        colB = (1f - (my - svY) / svS).coerceIn(0f, 1f)
                        pushColor()
                    }
                    mouseWithinBounds(mx.toInt(), my.toInt(), svX + svS + 8f, svY, svX + svS + 8f + 12f, svY + svS) -> {
                        colDrag = 2
                        colH = ((my - svY) / svS).coerceIn(0f, 1f)
                        pushColor()
                    }
                    else -> {
                        val hasAlpha = try {
                            colorValue!!.getAlpha()
                        } catch (_: Throwable) {
                            false
                        }
                        if (hasAlpha && mouseWithinBounds(mx.toInt(), my.toInt(), svX, svY + svS + 8f, svX + svS, svY + svS + 16f)) {
                            colDrag = 3
                            colA = (((mx - svX) / svS).coerceIn(0f, 1f) * 255).toInt()
                            pushColor()
                        }
                    }
                }
            }
        }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        dragging = false
        if (dragValue != null) {
            dragValue = null
            Crine.configManager.smartSave()
        }
        if (colDrag != 0) {
            colDrag = 0
            Crine.configManager.smartSave()
        }
        super.mouseReleased(mouseX, mouseY, state)
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        if (clickedMouseButton == 0) {
            if (dragging) {
                winX = (mouseX - dragDX).coerceIn(0f, (width - 60).coerceAtLeast(0).toFloat())
                winY = (mouseY - dragDY).coerceIn(0f, (height - 40).coerceAtLeast(0).toFloat())
            }
            if (dragValue != null) {
                applySliderDrag(mouseX.toFloat())
            }
            if (colorValue != null && colDrag != 0) {
                val mx = mouseX.toFloat()
                val my = mouseY.toFloat()
                val pad = 10f
                val svS = 128f
                val svX = popX + pad
                val svY = popY + pad
                when (colDrag) {
                    1 -> {
                        colS = ((mx - svX) / svS).coerceIn(0f, 1f)
                        colB = (1f - (my - svY) / svS).coerceIn(0f, 1f)
                        pushColor()
                    }
                    2 -> {
                        colH = ((my - svY) / svS).coerceIn(0f, 1f)
                        pushColor()
                    }
                    3 -> {
                        colA = (((mx - svX) / svS).coerceIn(0f, 1f) * 255).toInt()
                        pushColor()
                    }
                }
            }
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        val delta = Mouse.getEventDWheel()
        if (delta == 0) return
        val mx = Mouse.getEventX() * width / mc.displayWidth
        val my = height - Mouse.getEventY() * height / mc.displayHeight - 1
        val step = 26f
        val dir = if (delta > 0) 1f else -1f
        // popover scroll first
        if ((openList != null || openFont != null) &&
            mouseWithinBounds(mx, my, popX, popY, popX + popW, popY + popH)
        ) {
            popTarget -= dir * step
            return
        }
        val l = layout()
        if (my >= l.contentY && my < l.contentY + l.contentH) {
            if (mx < l.inspX - 1f) {
                tableTarget -= dir * step * 1.4f
            } else {
                inspTarget -= dir * step * 1.4f
            }
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        // key listening capture
        if (listenModule != null || listenValue != null) {
            val code = if (keyCode == Keyboard.KEY_ESCAPE) 0 else keyCode
            listenModule?.let { it.keyBind = code }
            listenValue?.let { it.set(code) }
            listenModule = null
            listenValue = null
            return
        }
        // popover dismiss
        if (openList != null || openFont != null || colorValue != null) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                openList = null
                openFont = null
                colorValue = null
                colDrag = 0
                return
            }
        }
        // text edits
        if (textTarget != null && textEdit.focused) {
            if (textEdit.keyTyped(typedChar, keyCode)) return
        }
        if (creatingConfig && newConfigEdit.focused) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                creatingConfig = false
                newConfigEdit.clearFocus(commit = false)
                return
            }
            if (newConfigEdit.keyTyped(typedChar, keyCode)) {
                if (!newConfigEdit.focused) creatingConfig = false
                return
            }
        }
        if (searchEdit.focused) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                if (searchEdit.text.isNotEmpty()) {
                    searchEdit.text = ""
                } else {
                    searchEdit.clearFocus(commit = false)
                }
                return
            }
            searchEdit.keyTyped(typedChar, keyCode)
            tableTarget = 0f
            return
        }
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RSHIFT) {
            if (openT > 0.5f) beginClose()
            return
        }
        // typing anywhere focuses search and appends (fast filter access)
        if (!GuiScreen.isCtrlKeyDown() && typedChar.code >= 32 && typedChar.code < 127 &&
            (typedChar.isLetterOrDigit() || typedChar == '_' || typedChar == '-')
        ) {
            if (!searchEdit.focused) {
                commitTextEdit()
                searchEdit.focused = true
            }
            searchEdit.keyTyped(typedChar, keyCode)
            tableTarget = 0f
            return
        }
        super.keyTyped(typedChar, keyCode)
    }

    // ── Mutations ─────────────────────────────────────────────────────────

    private fun beginClose() {
        if (!closing) {
            closing = true
            commitTextEdit()
            searchEdit.clearFocus(commit = false)
        }
    }

    private fun commitTextEdit() {
        if (textTarget != null && textEdit.focused) {
            textEdit.clearFocus(commit = true)
        } else if (textTarget != null) {
            textTarget = null
        }
        if (creatingConfig && newConfigEdit.focused) {
            newConfigEdit.clearFocus(commit = false)
            creatingConfig = false
        }
    }

    private fun afterValueChange(v: Value<*>) {
        try {
            if (isThemeValue(v)) saveTheme()
        } catch (_: Throwable) {
        }
    }

    private fun isThemeValue(v: Value<*>): Boolean {
        return v === ClientTheme.fadespeed || v === ClientTheme.index || v === ClientTheme.gcdFix ||
            v === ClientTheme.smoothRotation || v === ClientTheme.smoothFactor ||
            v === ClientTheme.smoothRotationSS || v === ClientTheme.smoothFactorSS ||
            v === ClientTheme.fullBody || v === ClientTheme.ClientColorMode ||
            v === ClientTheme.dropdownTheme
    }

    private fun saveTheme() {
        try {
            Crine.fileManager.saveConfig(Crine.fileManager.themeConfig)
        } catch (_: Throwable) {
        }
    }

    private fun openListPopover(v: ListValue) {
        openFont = null
        colorValue = null
        openList = v
        popTarget = 0f
        popScroll = 0f
    }

    private fun openFontPopover(v: FontValue) {
        openList = null
        colorValue = null
        openFont = v
        popTarget = 0f
        popScroll = 0f
    }

    private fun openColorPopover(v: ColorValue) {
        openList = null
        openFont = null
        colorValue = v
        colDrag = 0
        try {
            val c = v.get()
            val hsb = Color.RGBtoHSB(c.red, c.green, c.blue, null)
            colH = hsb[0]
            colS = hsb[1]
            colB = hsb[2]
            colA = c.alpha
        } catch (_: Throwable) {
            colH = 0f
            colS = 0f
            colB = 1f
            colA = 255
        }
    }

    private fun pushColor() {
        val v = colorValue ?: return
        try {
            val rgb = Color.getHSBColor(colH.coerceIn(0f, 1f), colS.coerceIn(0f, 1f), colB.coerceIn(0f, 1f))
            val hasAlpha = try {
                v.getAlpha()
            } catch (_: Throwable) {
                false
            }
            v.set(if (hasAlpha) Color(rgb.red, rgb.green, rgb.blue, colA.coerceIn(0, 255)) else Color(rgb.red, rgb.green, rgb.blue))
            afterValueChange(v)
        } catch (_: Throwable) {
        }
    }

    private fun cycleFont(v: FontValue, dir: Int) {
        try {
            val pairs = FontUtils.getAllFontDetails()
            if (pairs.isEmpty()) return
            var idx = pairs.indexOfFirst { it.second == v.get() }
            if (idx < 0) idx = 0
            idx = (idx + dir).mod(pairs.size)
            v.set(pairs[idx].second)
            afterValueChange(v)
        } catch (_: Throwable) {
        }
    }

    private fun rangeFracs(v: Value<*>): Pair<Float, Float> {
        return when (v) {
            is FloatRangeValue -> {
                val max = saneMax(v.minimum.toDouble(), v.maximum.toDouble()).toFloat()
                val span = (max - v.minimum).let { if (it <= 0f) 1f else it }
                ((v.getMin() - v.minimum) / span).coerceIn(0f, 1f) to ((v.getMax() - v.minimum) / span).coerceIn(0f, 1f)
            }
            is IntegerRangeValue -> {
                val max = saneMax(v.minimum.toDouble(), v.maximum.toDouble()).toInt()
                val span = (max - v.minimum).let { if (it <= 0) 1 else it }.toFloat()
                ((v.getMin() - v.minimum) / span).coerceIn(0f, 1f) to ((v.getMax() - v.minimum) / span).coerceIn(0f, 1f)
            }
            else -> 0f to 1f
        }
    }

    /** Sliders need a finite track: unbounded maxima collapse to a sane span. */
    private fun saneMax(min: Double, max: Double): Double {
        if (max >= Double.MAX_VALUE / 2 || max >= Float.MAX_VALUE.toDouble()) {
            return min + 100.0
        }
        if (max - min <= 0.0) return min + 1.0
        return max
    }

    private fun applySliderDrag(mx: Float) {
        val v = dragValue ?: return
        val frac = ((mx - dragTrackX) / dragTrackW).coerceIn(0f, 1f)
        try {
            when (v) {
                is FloatValue -> {
                    val max = saneMax(v.minimum.toDouble(), v.maximum.toDouble()).toFloat()
                    v.set((v.minimum + (max - v.minimum) * frac).coerceIn(v.minimum, max))
                }
                is IntegerValue -> {
                    val max = saneMax(v.minimum.toDouble(), v.maximum.toDouble()).toInt()
                    v.set((v.minimum + (max - v.minimum) * frac).toInt().coerceIn(v.minimum, max))
                }
                is NumberValue -> {
                    val max = if (v.maximum >= Double.MAX_VALUE / 2) v.minimum + 100.0 else v.maximum
                    val raw = v.minimum + (max - v.minimum) * frac
                    val stepped = ((raw / v.inc).toLong() * v.inc).coerceIn(v.minimum, max)
                    v.set(stepped)
                }
                is FloatRangeValue -> {
                    val max = saneMax(v.minimum.toDouble(), v.maximum.toDouble()).toFloat()
                    if (dragSide < 0) v.setMin((v.minimum + (max - v.minimum) * frac).coerceIn(v.minimum, max))
                    else v.setMax((v.minimum + (max - v.minimum) * frac).coerceIn(v.minimum, max))
                }
                is IntegerRangeValue -> {
                    val max = saneMax(v.minimum.toDouble(), v.maximum.toDouble()).toInt()
                    if (dragSide < 0) v.setMin((v.minimum + (max - v.minimum) * frac).toInt().coerceIn(v.minimum, max))
                    else v.setMax((v.minimum + (max - v.minimum) * frac).toInt().coerceIn(v.minimum, max))
                }
            }
            afterValueChange(v)
        } catch (_: Throwable) {
        }
    }

    // ── Config ops ────────────────────────────────────────────────────────

    private fun loadConfig(name: String) {
        try {
            Crine.configManager.load(name, true)
            selPerNav[nav] = name
            refreshConfigs()
        } catch (_: Throwable) {
        }
    }

    private fun deleteConfig(name: String) {
        try {
            if (name == "default") return
            val f = File(Crine.fileManager.configsDir, "$name.json")
            if (f.exists()) f.delete()
            if (Crine.configManager.nowConfig == name) {
                Crine.configManager.load("default", false)
            }
            refreshConfigs()
        } catch (_: Throwable) {
        }
    }

    private fun commitNewConfig() {
        val raw = newConfigEdit.text.trim().replace(Regex("[^A-Za-z0-9 _.-]"), "")
        creatingConfig = false
        newConfigEdit.clearFocus(commit = false)
        if (raw.isEmpty()) return
        try {
            Crine.configManager.load(raw, true)
            refreshConfigs()
            selPerNav[nav] = raw
        } catch (_: Throwable) {
        }
    }

    private fun applyTheme(name: String) {
        try {
            ClientTheme.ClientColorMode.set(name)
            saveTheme()
        } catch (_: Throwable) {
        }
    }
}
