package world.seoraw.plugin.contributor.seoraw

import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Schedule
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.command
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common.util.random
import taboolib.module.configuration.Type
import taboolib.module.configuration.createLocal
import taboolib.module.configuration.util.getLocation
import taboolib.module.configuration.util.setLocation
import taboolib.platform.BukkitPlugin
import taboolib.platform.util.giveItem
import taboolib.platform.util.toBukkitLocation
import taboolib.platform.util.toProxyLocation
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 1. 由于默认开启 PVP 模式，因此随机所有玩家的出生点
 * 2. 自然死亡后保留物品到下次重启
 *
 * @author Seoraw
 */
object SeorawFeatures {

    const val range = 200

    val data by lazy { createLocal("data.yml", type = Type.FAST_JSON) }

    val deathSoul = CopyOnWriteArrayList<DeathSoul>()

    @Awake(LifeCycle.ENABLE)
    fun init() {
        command("migrate", permissionDefault = PermissionDefault.TRUE) {
            execute<Player> { sender, _, _ ->
                val name = sender.name
                when (sender.world.name) {
                    "world" -> {
                        data.setLocation("migrate.$name.old", sender.location.toProxyLocation())
                        val spawnLocation = if (data.contains("migrate.$name.new")) {
                            data.getLocation("migrate.$name.new")!!.toBukkitLocation()
                        } else {
                            sender.addPotionEffect(PotionEffect(PotionEffectType.WATER_BREATHING, 1200, 9))
                            sender.addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 1200, 9))
                            Bukkit.getWorld("world_new")!!.spawnLocation
                        }
                        sender.sendMessage("正在前往新世界。")
                        sender.teleport(spawnLocation)
                    }
                    "world_new" -> {
                        data.setLocation("migrate.$name.new", sender.location.toProxyLocation())
                        val spawnLocation = if (data.contains("migrate.$name.old")) {
                            data.getLocation("migrate.$name.old")!!.toBukkitLocation()
                        } else {
                            sender.sendMessage("你无法返回旧世界。")
                            return@execute
                        }
                        sender.sendMessage("正在回到旧世界。")
                        sender.teleport(spawnLocation)
                    }
                    else -> {
                        sender.sendMessage("不在世界迁移范围内。")
                    }
                }
            }
        }
    }

    @Schedule(period = 20)
    fun check() {
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
    }

    @SubscribeEvent
    fun e(e: PlayerJoinEvent) {
        // 老玩家进行世界检测
        if (e.player.hasPlayedBefore()) {
            if (e.player.world.name == "world") {
                e.player.sendMessage("§aSeoraw's World 正在进行世界迁移，当前世界将在 1月18日23时59分 停止使用。")
                e.player.sendMessage("§a准备完成后通过 /migrate 命令前往新世界，再次使用可返回当前位置。")
            }
        }
        // 新玩家将直接传送到新世界
        else {
            val spawnLocation = Bukkit.getWorld("world_new")!!.spawnLocation
            randomSpawnLocation(spawnLocation)
            e.player.teleport(spawnLocation)
        }
    }

    @SubscribeEvent
    fun e(e: PlayerRespawnEvent) {
        if (!e.isBedSpawn && !e.isAnchorSpawn) {
            if (e.player.hasMetadata("world_new")) {
                e.player.removeMetadata("world_new", BukkitPlugin.getInstance())
                e.respawnLocation.world = Bukkit.getWorld("world_new")
            }
            randomSpawnLocation(e.respawnLocation)
        }
    }

    @SubscribeEvent
    fun e(e: PlayerDeathEvent) {
        if (e.player.world.name == "world_new") {
            e.player.setMetadata("world_new", FixedMetadataValue(BukkitPlugin.getInstance(), true))
        }
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
        submit(delay = 20) { e.player.spigot().respawn() }
    }

    @SubscribeEvent
    fun e(e: CreatureSpawnEvent) {
        if (e.entity is Villager && e.spawnReason != CreatureSpawnEvent.SpawnReason.EGG) {
            e.isCancelled = true
        }
    }
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