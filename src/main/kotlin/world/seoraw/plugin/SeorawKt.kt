package world.seoraw.plugin

import org.bukkit.plugin.java.JavaPlugin
import world.seoraw.plugin.contributor.seoraw.SeorawFeatures

class SeorawKt : JavaPlugin() {

    val javaSide = SeorawJava()

    init {
        instance = this
    }

    override fun onLoad() {
        javaSide.onLoad()
    }

    override fun onEnable() {
        javaSide.onEnable()
        initFeatures()
        logger.info("SeorawPlugin 已就绪")
    }

    override fun onDisable() {
        javaSide.onDisable()
    }
    
    /**
     * 在此处注册 Contributor 的特性
     */
    fun initFeatures() {
        server.pluginManager.registerEvents(SeorawFeatures, this)
    }

    companion object {

        lateinit var instance: SeorawKt
            private set
    }
}