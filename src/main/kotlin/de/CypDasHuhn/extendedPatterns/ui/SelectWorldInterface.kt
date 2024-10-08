package de.CypDasHuhn.extendedPatterns.ui

import de.CypDasHuhn.extendedPatterns.commands.defaultWorlds
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
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

@RoosterInterface
object SelectWorldInterface : ScrollInterface<SelectWorldInterface.SelectWorldContext, World>(
    "SelectWorldInterface",
    SelectWorldContext::class,
    contentArea = (0 to 0) to (8 to 0)
) {
    class SelectWorldContext(var selectedWorldNames: List<String>) : ScrollContext()

    fun worldBlockFromName(name: String): Material {
        if (name.contains("nether", ignoreCase = true)) return Material.NETHERRACK
        if (name.contains("end", ignoreCase = true)) return Material.END_STONE
        return Material.GRASS_BLOCK
    }

    override fun contentCreator(
        data: World,
        context: SelectWorldContext
    ): Pair<(InterfaceInfo<SelectWorldContext>) -> ItemStack, (ClickInfo<SelectWorldContext>) -> Unit> {

        val isOn = context.selectedWorldNames.contains(data.name)

        return { it: InterfaceInfo<SelectWorldContext> ->
            createItem(
                worldBlockFromName(data.name),
                name = minimessage("<yellow>${data.name}"),
                description = listOf(t(if (isOn) "on" else "off", it.player) as TextComponent),
                additional = {
                    it.setEnchantmentGlintOverride(isOn)
                }
            )
        } to { clickInfo ->
            clickInfo.context.selectedWorldNames = if (isOn) {
                if (clickInfo.context.selectedWorldNames.size > 1) {
                    clickInfo.context.selectedWorldNames - data.name
                } else clickInfo.context.selectedWorldNames
            } else {
                clickInfo.context.selectedWorldNames + data.name
            }
            SelectWorldInterface.openInventory(clickInfo.click.player, clickInfo.context)
        }
    }

    override fun contentProvider(id: Int, context: SelectWorldContext): World? {
        return Bukkit.getWorlds().getOrNull(id)
    }

    override fun defaultContext(player: Player): SelectWorldContext {
        return SelectWorldContext(selectedWorldNames = defaultWorlds(player).map { it.name })
    }

    override fun getOtherItems(): List<InterfaceItem<SelectWorldContext>> {
        return listOf(
            RouterItem(
                condition = { it.slot == 9 },
                itemStackCreator = {
                    createItem(
                        Material.FEATHER,
                        name = t("back_to_selection", it.player) as TextComponent
                    )
                },
                targetInterface = SelectGroupInterface,
                context = {
                    SelectGroupInterface.getContext(it.click.player)
                        .also { context -> context.worldNames = it.context.selectedWorldNames }
                }
            )
        )
    }

    override fun modifiedScroller(item: ContextModifierItem<SelectWorldContext>): ContextModifierItem<SelectWorldContext> {
        return item.also {
            it.itemStackCreator = pagerItem()
        }
    }

    override fun getInventory(player: Player, context: SelectWorldContext): Inventory {
        return Bukkit.createInventory(
            null,
            2 * 9,
            t("select_world_interface", player, "row" to (context.position + 1).toString())
        )
    }
}