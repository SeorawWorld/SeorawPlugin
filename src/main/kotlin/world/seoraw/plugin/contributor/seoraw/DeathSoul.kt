package world.seoraw.plugin.contributor.seoraw

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

data class DeathSoul(val location: Location, val owner: UUID, val inventory: MutableList<ItemStack>) {

    val player: Player?
        get() = Bukkit.getPlayer(owner)
}