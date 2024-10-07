package de.CypDasHuhn.extendedPatterns.util

import org.bukkit.Bukkit
import org.bukkit.World

fun forEachWorld(action: (World) -> Unit) {
    Bukkit.getWorlds().forEach(action)
}