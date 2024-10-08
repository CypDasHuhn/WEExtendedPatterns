package de.CypDasHuhn.extendedPatterns.util

import de.cypdashuhn.rooster.localization.t
import de.cypdashuhn.rooster.ui.interfaces.Context
import de.cypdashuhn.rooster.ui.interfaces.InterfaceInfo
import de.cypdashuhn.rooster.util.createItem
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

fun <T : Context> pagerItem(extraDescription: (InterfaceInfo<T>) -> List<TextComponent> = { listOf() }): (InterfaceInfo<T>) -> ItemStack =
    {
        createItem(
            Material.COMPASS,
            t("pager", it.player) as TextComponent,
            description = listOf(
                t("pager_description_left", it.player) as TextComponent,
                t("pager_description_right", it.player) as TextComponent,
                *extraDescription(it).toTypedArray()
            )
        )
    }