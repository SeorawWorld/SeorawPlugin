package world.seoraw.plugin.contributor.seoraw

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerRespawnEvent
import org.spigotmc.event.player.PlayerSpawnLocationEvent
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.max
import kotlin.math.min

/**
 * 由于默认开启 PVP 模式，因此随机所有玩家的出生点
 * @author Seoraw
 */
object SeorawFeatures : Listener {

    const val range = 200

    @EventHandler
    fun e(e: PlayerSpawnLocationEvent) {
        if (!e.player.hasPlayedBefore()) {
            randomSpawnLocation(e.spawnLocation)
        }
    }

    @EventHandler
    fun e(e: PlayerRespawnEvent) {
        if (!e.isBedSpawn && !e.isAnchorSpawn) {
            randomSpawnLocation(e.respawnLocation)
        }
    }

    fun randomSpawnLocation(location: Location) {
        var i = 0
        while (i++ !in 1..10 || location.block.type == Material.LAVA) {
            location.add(random(-range, range).toDouble(), 0.0, random(-range, range).toDouble())
            location.y = location.world.getHighestBlockYAt(location).toDouble()
        }
        // 修正最终坐标
        location.add(0.5, 1.0, 0.5)
    }
}

fun random(num1: Int, num2: Int): Int {
    val min = min(num1, num2)
    val max = max(num1, num2)
    return ThreadLocalRandom.current().nextInt(min, max + 1)
}

fun random(num1: Double, num2: Double): Double {
    val min = min(num1, num2)
    val max = max(num1, num2)
    return if (min == max) max else ThreadLocalRandom.current().nextDouble(min, max)
}