package net.shxp3.crine.features.module.modules.visual



import net.shxp3.crine.features.module.Module
import net.shxp3.crine.features.module.ModuleCategory
import net.shxp3.crine.features.module.ModuleInfo
import net.shxp3.crine.features.value.ListValue
import net.minecraft.util.ResourceLocation
import java.util.*

@ModuleInfo(name = "Cape", category = ModuleCategory.VISUAL)
class Cape : Module() {

    val styleValue = ListValue(
        "Style",
        arrayOf(
            "Crine",
            "Astolfo",
            "Black",
            "Rise",
            "Novoline",
            "Styles",
            "None"
        ),
        "None"
    )

    private val capeCache = hashMapOf<String, CapeStyle>()
    fun getCapeLocation(value: String): ResourceLocation {
        if (capeCache[value.uppercase(Locale.getDefault())] == null) {
            try {
                capeCache[value.uppercase(Locale.getDefault())] =
                    CapeStyle.valueOf(value.uppercase(Locale.getDefault()))
            } catch (e: Exception) {
                capeCache[value.uppercase(Locale.getDefault())] = CapeStyle.CRINE
            }
        }
        return capeCache[value.uppercase(Locale.getDefault())]!!.location
    }

    enum class CapeStyle(val location: ResourceLocation) {
        CRINE(ResourceLocation("crine/cape/crine.png")),
        ASTOLFO(ResourceLocation("crine/cape/astolfo.png")),
        BLACK(ResourceLocation("crine/cape/black.png")),
        RISE(ResourceLocation("crine/cape/risecape.png")),
        NOVOLINE(ResourceLocation("crine/cape/novoline.png")),
        STYLES(ResourceLocation("crine/cape/styles.png")),
    }

    override val tag: String
        get() = styleValue.get()

    init {
        state = true
    }
}