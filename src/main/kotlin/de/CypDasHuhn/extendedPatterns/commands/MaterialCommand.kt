package de.CypDasHuhn.extendedPatterns.commands

import de.cypdashuhn.rooster.commands.argument_constructors.ArgumentDetails
import de.cypdashuhn.rooster.commands.argument_constructors.CentralArgument
import de.cypdashuhn.rooster.localization.tSend
import org.bukkit.Material

object MaterialCommand {
    fun material(
        key: String,
        argumentDetails: ArgumentDetails,
        materialInvalidKey: String = "material_invalid",
        materialMissingKey: String = "material_missing"
    ): CentralArgument {
        return CentralArgument(
            key = key,
            errorMissing = { it.sender.tSend(materialMissingKey) },
            tabCompletions = {
                val arg = it.arg
                val base = arg.substringBeforeLast(",", "").trim()

                if (base.isEmpty()) {
                    Material.entries
                        .filter { it.isBlock }
                        .map { it.name.lowercase() }
                        .filter { material -> material.startsWith(arg.trim()) }
                } else {
                    val currentMaterials = base.lowercase().split(",")

                    Material.entries
                        .filter { it.name.lowercase() !in currentMaterials && it.isBlock }
                        .map { "$base,${it.name.lowercase()}" }
                        .filter { material -> material.startsWith(it.arg) }
                }

            },
            argumentHandler = {
                it.arg.split(",").map { Material.valueOf(it.uppercase()) }
            },
            isValid = { argInfo ->
                val materials = mutableListOf<String>()
                argInfo.arg.split((",")).forEach { material ->
                    try {
                        Material.valueOf(material.uppercase())

                        if (material.lowercase() in materials) {
                            return@CentralArgument false to {
                                argInfo.sender.tSend(
                                    "material_duplicate",
                                    "material" to material
                                )
                            }
                        }

                        materials.add(material.lowercase())
                    } catch (e: IllegalArgumentException) {
                        return@CentralArgument false to {
                            argInfo.sender.tSend(
                                materialInvalidKey,
                                "material" to material
                            )
                        }
                    }
                }

                true to {}
            },
            argumentDetails = argumentDetails
        )
    }
}