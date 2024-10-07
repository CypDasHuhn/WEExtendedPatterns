package de.CypDasHuhn.extendedPatterns.ui

import de.CypDasHuhn.extendedPatterns.commands.defaultWorlds
import de.CypDasHuhn.extendedPatterns.database.GroupManager
import de.cypdashuhn.rooster.localization.t
import de.cypdashuhn.rooster.ui.interfaces.ClickInfo
import de.cypdashuhn.rooster.ui.interfaces.constructors.indexed_content.ScrollInterface
import de.cypdashuhn.rooster.ui.items.InterfaceItem
import de.cypdashuhn.rooster.util.createItem
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object SelectGroupInterface :
    ScrollInterface<SelectGroupInterface.SelectGroupContext, SelectGroupInterface.GroupDTO>(
        "SelectGroupInterface",
        SelectGroupContext::class,
        ScrollDirection.LEFT_RIGHT
    ) {
    class SelectGroupContext(
        val worldNames: List<String>,
        val requireOverlappingWorlds: Boolean
    ) : ScrollContext()

    class GroupDTO(
        val name: String,
        val worldName: String,
        val oldMaterials: List<Material>?,
        val newMaterials: List<Material>?
    )

    override fun contentCreator(
        data: GroupDTO,
        context: SelectGroupContext
    ): Pair<ItemStack, (ClickInfo<SelectGroupContext>) -> Unit> {

        return createItem(
            material = (data.newMaterials ?: data.oldMaterials!!).first(),
            name = MiniMessage.miniMessage().deserialize("<yellow>${data.name}") as TextComponent,

            ) to {
            GroupInterface.openInventory(it.click.player, GroupInterface.GroupContext(data))
        }
    }

    override fun contentProvider(id: Int, context: SelectGroupContext): GroupDTO? {
        return transaction {
            if (context.requireOverlappingWorlds) {
                val result =
                    GroupManager.Groups.selectAll().limit(1, id.toLong()).firstOrNull() ?: return@transaction null
                GroupManager.Group.wrapRow(result)
            } else {
                val result =
                    GroupManager.Groups.selectAll().limit(1, id.toLong()).firstOrNull() ?: return@transaction null
                GroupManager.Group.wrapRow(result)
            }
        }?.toDTO()
    }

    override fun defaultContext(player: Player): SelectGroupContext {
        return SelectGroupContext(
            worldNames = defaultWorlds(player).map { it.name },
            requireOverlappingWorlds = true
        )
    }

    override fun getOtherItems(): List<InterfaceItem<SelectGroupContext>> {
        return listOf()
    }

    override fun getInventory(player: Player, context: SelectGroupContext): Inventory {
        return Bukkit.createInventory(
            null,
            6 * 9,
            t("select_group_interface", player, "row" to (context.position + 1).toString())
        )
    }
}