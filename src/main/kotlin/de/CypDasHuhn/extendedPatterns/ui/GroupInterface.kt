package de.CypDasHuhn.extendedPatterns.ui

import de.CypDasHuhn.extendedPatterns.database.GroupManager
import de.CypDasHuhn.extendedPatterns.util.minimessage
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
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

@RoosterInterface
object GroupInterface : ScrollInterface<GroupInterface.GroupContext, Material>(
    "GroupInterface", GroupContext::class, ScrollDirection.LEFT_RIGHT
) {
    class GroupContext(
        var groups: List<SelectGroupInterface.GroupDTO>,
        var useOldMaterial: Boolean = false,
        var isEditing: Boolean = false
    ) : ScrollContext()

    override fun contentCreator(
        data: Material,
        context: GroupContext
    ): Pair<(InterfaceInfo<GroupContext>) -> ItemStack, (ClickInfo<GroupContext>) -> Unit> {
        return { _: InterfaceInfo<GroupContext> ->
            createItem(
                material = data,
            )
        } to {

        }
    }

    override fun contentProvider(id: Int, context: GroupContext): Material? {
        val first = context.groups.first() /* The list is either singular, or grouped by materials. */

        val material = if (context.useOldMaterial) {
            first.oldMaterials ?: first.newMaterials!!
        } else first.newMaterials ?: first.oldMaterials!!

        return material.getOrNull(id)
    }

    override fun defaultContext(player: Player): GroupContext {
        throw IllegalStateException("There is no default Context for Group Interface!")
    }

    override fun modifiedClickInArea(item: InterfaceItem<GroupContext>): InterfaceItem<GroupContext> {
        return item.also {
            it.action = {
                it.event.isCancelled = !it.context.isEditing
            }
        }
    }

    override fun modifiedContentItem(item: InterfaceItem<GroupContext>): InterfaceItem<GroupContext> {
        return item.also {
            it.action = {
                it.event.isCancelled = !it.context.isEditing
            }
        }
    }

    override fun modifiedScroller(item: ContextModifierItem<GroupContext>): ContextModifierItem<GroupContext> {
        val originalAction = item.action // Store the original action

        return item.also {
            it.action = { if (!it.context.isEditing) originalAction(it) } // Use the stored original action
            it.itemStackCreator = pagerItem({
                if (it.context.isEditing) listOf(t("pager_disabled_editing", it.player) as TextComponent) else listOf()
            })
        }
    }


    override fun getOtherItems(): List<InterfaceItem<GroupContext>> {
        return listOf(
            RouterItem(
                condition = { it.slot == bottomRow },
                itemStackCreator = {
                    createItem(
                        Material.FEATHER,
                        name = t("back_to_selection", it.player) as TextComponent
                    )
                },
                targetInterface = SelectGroupInterface,
                context = { SelectGroupInterface.getContext(it.click.player) }
            ),
            InterfaceItem(
                condition = { it.slot == bottomRow + 1 },
                itemStackCreator = {
                    val description = it.context.groups.map { minimessage("<dark_purple>${it.worldName}") }

                    createItem(
                        Material.GRASS_BLOCK,
                        name = t("worlds", it.player) as TextComponent,
                        description = description
                    )
                },
            ),
            ContextModifierItem(
                slot = bottomRow + 3,
                condition = { !it.context.isEditing && !it.context.useOldMaterial },
                itemStackCreator = {
                    val description =
                        listOf(
                            t(
                                if (it.context.isEditing) "on" else "off",
                                it.player
                            ) as TextComponent
                        )
                    createItem(
                        Material.WRITABLE_BOOK,
                        name = t("is_editing", it.player) as TextComponent,
                        description = description
                    )
                },
                contextModifier = { it.context.also { it.isEditing = true } },
            ),
            ContextModifierItem(
                slot = bottomRow + 3,
                condition = { it.context.isEditing },
                itemStackCreator = {
                    createItem(Material.RED_STAINED_GLASS_PANE, name = t("cancel", it.player) as TextComponent)
                },
                contextModifier = { it.context.also { it.isEditing = false } },
            ),
            InterfaceItem(
                slot = bottomRow + 5,
                condition = { it.context.isEditing },
                itemStackCreator = {
                    createItem(Material.GREEN_STAINED_GLASS_PANE, name = t("save", it.player) as TextComponent)
                },
                action = { clickInfo ->
                    val context = clickInfo.context
                    val items = (0..bottomRow).map {
                        clickInfo.event.inventory.getItem(it)
                    }
                    val newMaterials = GroupManager.updateFromInventory(
                        context.position,
                        items,
                        context.groups.first().name,
                        context.groups.map { it.worldName }
                    )

                    GroupInterface.openInventory(clickInfo.click.player, context.also {
                        it.isEditing = false
                        it.groups = it.groups.map { it.also { it.newMaterials = newMaterials } }
                    })
                }
            ),
            ContextModifierItem(
                slot = bottomRow + 5,
                condition = { !it.context.isEditing && it.context.groups.first().oldMaterials != it.context.groups.first().newMaterials },
                itemStackCreator = {
                    val description =
                        listOf(
                            t(
                                if (it.context.useOldMaterial) "on" else "off",
                                it.player
                            ) as TextComponent
                        )
                    createItem(
                        Material.KNOWLEDGE_BOOK,
                        name = t("use_old_material", it.player) as TextComponent,
                        description = description
                    )
                },
                contextModifier = { it.context.also { it.useOldMaterial = !it.useOldMaterial } },
            ),
            ContextModifierItem(
                slot = bottomRow + 7,
                itemStackCreator = {
                    val useAir = (it.context.groups.first().newMaterials
                        ?: it.context.groups.first().oldMaterials!!).contains(Material.AIR)
                    val description =
                        mutableListOf(
                            t(
                                if (useAir) "on" else "off",
                                it.player
                            ) as TextComponent
                        )
                    if (it.context.useOldMaterial) description.add(t("use_air_old", it.player) as TextComponent)
                    createItem(
                        Material.BARRIER,
                        name = t("use_air", it.player) as TextComponent,
                        description = description
                    )
                },
                contextModifier = { info -> info.context },
                furtherAction = { info ->
                    if (!info.context.useOldMaterial) {
                        val useAir = (info.context.groups.first().newMaterials
                            ?: info.context.groups.first().oldMaterials!!).contains(Material.AIR)
                        GroupManager.changeAirStatus(
                            useAir,
                            info.context.groups.first().name,
                            info.context.groups.map { it.worldName })
                    }
                }
            )
        )
    }

    override fun getInventory(player: Player, context: GroupContext): Inventory {
        return Bukkit.createInventory(
            null,
            6 * 9,
            t(
                "group_interface",
                player,
                "row" to (context.position + 1).toString(),
                "groupName" to context.groups.first().name
            )
        )
    }
}