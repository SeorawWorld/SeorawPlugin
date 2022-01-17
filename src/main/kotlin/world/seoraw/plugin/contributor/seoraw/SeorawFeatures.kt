package world.seoraw.plugin.contributor.seoraw

import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.spigotmc.event.player.PlayerSpawnLocationEvent
import world.seoraw.plugin.SeorawKt
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min

/**
 * 1. 由于默认开启 PVP 模式，因此随机所有玩家的出生点
 * 2. 自然死亡后保留物品到下次重启
 *
 * @author Seoraw
 */
object SeorawFeatures : Listener {

    const val range = 200

    val deathSoul = CopyOnWriteArrayList<DeathSoul>()

    init {
        Bukkit.getScheduler().runTaskTimerAsynchronously(SeorawKt.instance, Runnable {
            deathSoul.forEach { soul ->
                val player = soul.player
                if (player != null && !player.isDead && player.world == soul.location.world && player.location.distance(soul.location) < 32) {
                    player.spawnParticle(Particle.CLOUD, soul.location, 5, 0.2, 0.2, 0.2, 0.0)
                    player.spawnParticle(Particle.END_ROD, soul.location, 10, 0.2, 0.2, 0.2, 0.0)
                    if (player.location.distance(soul.location) < 1.5) {
                        player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 1f, 1f)
                        player.giveItem(soul.inventory)
                        soul.inventory.clear()
                        deathSoul.remove(soul)
                    }
                }
            }
        }, 20, 20)
    }

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

    @EventHandler
    fun e(e: PlayerDeathEvent) {
        if (e.drops.isNotEmpty()) {
            val loc = e.player.location
            if (e.player.killer == null) {
                deathSoul += DeathSoul(e.player.location.add(1.0, 1.0, 1.0), e.player.uniqueId, e.drops.toMutableList())
                e.player.sendMessage("你在 §eX:${loc.blockX}, Y:${loc.blockY}, Z:${loc.blockZ} §f死亡, 损失的物品将保留至 Seoraw's World 下次重启。")
                e.drops.clear()
            } else {
                e.player.sendMessage("你在 §eX:${loc.blockX}, Y:${loc.blockY}, Z:${loc.blockZ} §f死亡。")
            }
        }
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

fun randomSpawnLocation(location: Location) {
    var i = 0
    while (i++ !in 1..10 || location.block.type == Material.LAVA) {
        location.add(random(-SeorawFeatures.range, SeorawFeatures.range).toDouble(), 0.0, random(-SeorawFeatures.range, SeorawFeatures.range).toDouble())
        location.y = location.world.getHighestBlockYAt(location).toDouble()
    }
    // 修正最终坐标
    location.add(0.5, 1.0, 0.5)
}