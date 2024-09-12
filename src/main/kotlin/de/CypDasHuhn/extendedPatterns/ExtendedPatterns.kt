package de.CypDasHuhn.extendedPatterns

import com.github.CypDasHuhn.rooster.Rooster

import org.bukkit.plugin.java.JavaPlugin

class ExtendedPatterns : JavaPlugin() {

    override fun onEnable() {
        Rooster.initialize()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
