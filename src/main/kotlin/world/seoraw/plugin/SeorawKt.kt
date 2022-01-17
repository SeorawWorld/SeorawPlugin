package world.seoraw.plugin

import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info

object SeorawKt : Plugin() {

    val javaSide = SeorawJava()

    override fun onLoad() {
        javaSide.onLoad()
    }

    override fun onEnable() {
        javaSide.onEnable()
        info("SeorawPlugin 已就绪")
    }

    override fun onDisable() {
        javaSide.onDisable()
    }
}