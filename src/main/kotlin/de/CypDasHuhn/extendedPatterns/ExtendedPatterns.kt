package de.CypDasHuhn.extendedPatterns

import de.CypDasHuhn.extendedPatterns.database.DatapackManager
import de.CypDasHuhn.extendedPatterns.database.GroupManager
import de.cypdashuhn.rooster.core.Rooster
import de.cypdashuhn.rooster.core.Rooster.registeredRootArguments
import de.cypdashuhn.rooster.core.RoosterPlugin

class ExtendedPatterns : RoosterPlugin("ExtendedPatterns") {
    override fun onInitialize() {
        DatapackManager.initializeDatapacks()
        GroupManager.reloadMaterials()

        if (Rooster.plugin.config.get("globalAsDefault") as? Boolean? == null) {
            Rooster.plugin.config.set("globalAsDefault", true)
        }

        if (Rooster.plugin.config.get("datapackName") as? String? == null) {
            Rooster.plugin.config.set("datapackName", "cbt")
        }

        Rooster.plugin.saveConfig()

        val s = registeredRootArguments.firstOrNull { it.labels.any { it.lowercase() == "cbt".lowercase() } }
    }
}

val globalAsDefault = Rooster.plugin.config.get("globalAsDefault", true) as Boolean
val datapackName = Rooster.plugin.config.get("datapackName", "cbt")
