package de.CypDasHuhn.extendedPatterns.ui

import de.CypDasHuhn.extendedPatterns.commands.defaultWorlds
import de.CypDasHuhn.extendedPatterns.database.GroupManager
import de.CypDasHuhn.extendedPatterns.util.pagerItem
import de.cypdashuhn.rooster.localization.t
import de.cypdashuhn.rooster.ui.interfaces.ClickInfo
import de.cypdashuhn.rooster.ui.interfaces.InterfaceInfo
import de.cypdashuhn.rooster.ui.interfaces.RoosterInterface
import de.cypdashuhn.rooster.ui.interfaces.constructors.indexed_content.ScrollInterface
import de.cypdashuhn.rooster.ui.items.InterfaceItem
import de.cypdashuhn.rooster.ui.items.constructors.ContextModifierItem
import de.cypdashuhn.rooster.ui.items.constructors.RouterItem
import de.cypdashuhn.rooster.util.createItem
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

@RoosterInterface
object SelectGroupInterface :
    ScrollInterface<SelectGroupInterface.SelectGroupContext, List<SelectGroupInterface.GroupDTO>>(
        "SelectGroupInterface",
        SelectGroupContext::class,
        ScrollDirection.LEFT_RIGHT
    ) {
    class SelectGroupContext(
        var worldNames: List<String>,
        var groupFilter: GroupFilter,
    ) : ScrollContext()

    enum class GroupFilter {
        ALL,
        IF_DIFFERENT
    }

    class GroupDTO(
        val name: String,
        val worldName: String,
        val oldMaterials: List<Material>?,
        var newMaterials: List<Material>?
    )

    override fun contentCreator(
        data: List<GroupDTO>,
        context: SelectGroupContext
    ): Pair<(InterfaceInfo<SelectGroupContext>) -> ItemStack, (ClickInfo<SelectGroupContext>) -> Unit> {
        val description = mutableListOf<TextComponent>()
        data.forEach { group ->
            description.add(MiniMessage.miniMessage().deserialize("<light_purple>${group.worldName}") as TextComponent)
        }
        val first = data.first()

        return { _: InterfaceInfo<SelectGroupContext> ->
            createItem(
                material = (first.newMaterials ?: first.oldMaterials!!).first(),
                name = MiniMessage.miniMessage().deserialize("<yellow>${first.name}") as TextComponent,
                description = description
            )
        } to {
            GroupInterface.openInventory(it.click.player, GroupInterface.GroupContext(data))
        }
    }

    override fun contentProvider(id: Int, context: SelectGroupContext): List<GroupDTO>? {
        val result = transaction {
            val base = GroupManager.Groups.selectAll().where { GroupManager.Groups.worldName inList context.worldNames }
            when (context.groupFilter) {
                GroupFilter.ALL -> listOf(
                    GroupManager.Group.wrapRow(
                        base.orderBy(GroupManager.Groups.name to SortOrder.DESC).limit(1, id.toLong()).firstOrNull()
                            ?: return@transaction null
                    ).toDTO()
                )

                GroupFilter.IF_DIFFERENT -> {
                    val groups = base
                        .orderBy(GroupManager.Groups.name to SortOrder.DESC)
                        .groupBy(GroupManager.Groups.newMaterials, GroupManager.Groups.oldMaterials)

                    val entry = groups.limit(1, id.toLong()).firstOrNull() ?: return@transaction null

                    val sameGroupEntries = GroupManager.Groups.selectAll()
                        .where {
                            (GroupManager.Groups.worldName inList context.worldNames) and
                                    (GroupManager.Groups.newMaterials eq entry[GroupManager.Groups.newMaterials]) and
                                    (GroupManager.Groups.oldMaterials eq entry[GroupManager.Groups.oldMaterials])
                        }
                        .orderBy(GroupManager.Groups.name to SortOrder.DESC)
                        .map { GroupManager.Group.wrapRow(it).toDTO() }

                    return@transaction sameGroupEntries
                }
            }


        }

        return result
    }

    override fun defaultContext(player: Player): SelectGroupContext {
        return SelectGroupContext(
            worldNames = defaultWorlds(player).map { it.name },
            GroupFilter.IF_DIFFERENT,
        )
    }

    override fun getOtherItems(): List<InterfaceItem<SelectGroupContext>> {
        return listOf(
            RouterItem(
                condition = { it.slot == bottomRow + 4 },
                itemStackCreator = {
                    createItem(
                        Material.GRASS_BLOCK,
                        name = t("select_world", it.player) as TextComponent
                    )
                },
                targetInterface = SelectWorldInterface,
                context = { SelectWorldInterface.SelectWorldContext(it.context.worldNames) }
            ),
            ContextModifierItem(
                slot = bottomRow,
                itemStackCreator = {
                    createItem(
                        Material.SPYGLASS,
                        name = t(
                            if (it.context.groupFilter == GroupFilter.IF_DIFFERENT) "change_to_all" else "change_to_different",
                            it.player
                        ) as TextComponent
                    )
                },
                contextModifier = { clickInfo ->
                    clickInfo.context.also {
                        it.groupFilter = when (it.groupFilter) {
                            GroupFilter.ALL -> GroupFilter.IF_DIFFERENT
                            GroupFilter.IF_DIFFERENT -> GroupFilter.ALL
                        }
                    }
                }
            )
        )
    }

    override fun modifiedScroller(item: ContextModifierItem<SelectGroupContext>): ContextModifierItem<SelectGroupContext> {
        return item.also {
            it.itemStackCreator = pagerItem()
        }
    }

    override fun getInventory(player: Player, context: SelectGroupContext): Inventory {
        return Bukkit.createInventory(
            null,
            6 * 9,
            t("select_group_interface", player, "row" to (context.position + 1).toString())
        )
    }
}