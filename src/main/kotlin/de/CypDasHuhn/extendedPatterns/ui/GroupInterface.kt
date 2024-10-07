package de.CypDasHuhn.extendedPatterns.ui

import de.cypdashuhn.rooster.localization.t
import de.cypdashuhn.rooster.ui.interfaces.ClickInfo
import de.cypdashuhn.rooster.ui.interfaces.constructors.indexed_content.ScrollInterface
import de.cypdashuhn.rooster.ui.items.InterfaceItem
import de.cypdashuhn.rooster.util.createItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

object GroupInterface : ScrollInterface<GroupInterface.GroupContext, Material>(
    "GroupInterface", GroupContext::class, ScrollDirection.LEFT_RIGHT
) {
    class GroupContext(
        val group: SelectGroupInterface.GroupDTO,
        val useOldMaterial: Boolean = false,
    ) : ScrollContext()

    override fun contentCreator(
        data: Material,
        context: GroupContext
    ): Pair<ItemStack, (ClickInfo<GroupContext>) -> Unit> {
        return createItem(
            material = data,
        ) to {

        }
    }

    override fun contentProvider(id: Int, context: GroupContext): Material? {
        val material = if (context.useOldMaterial) {
            context.group.oldMaterials ?: context.group.newMaterials!!
        } else context.group.newMaterials ?: context.group.oldMaterials!!

        return material.getOrNull(id)
    }

    override fun defaultContext(player: Player): GroupContext {
        throw IllegalStateException("There is no default Context for Group Interface!")
    }

    override fun getOtherItems(): List<InterfaceItem<GroupContext>> {
        return listOf()
    }

    override fun getInventory(player: Player, context: GroupContext): Inventory {
        return Bukkit.createInventory(
            null,
            6 * 9,
            t("group_interface", player, "row" to (context.position + 1).toString(), "groupName" to context.group.name)
        )
    }
}