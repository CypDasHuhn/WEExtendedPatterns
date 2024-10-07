package de.CypDasHuhn.extendedPatterns.commands

import de.CypDasHuhn.extendedPatterns.database.GroupManager
import de.CypDasHuhn.extendedPatterns.globalAsDefault
import de.CypDasHuhn.extendedPatterns.ui.SelectGroupInterface
import de.cypdashuhn.rooster.commands.RoosterCommand
import de.cypdashuhn.rooster.commands.argument_constructors.*
import de.cypdashuhn.rooster.commands.utility_argument_constructors.SimpleArgument
import de.cypdashuhn.rooster.localization.tSend
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

fun groupsCommand(argumentDetails: ArgumentDetails): CentralArgument {
    return CentralArgument(
        key = "group",
        tabCompletions = {
            val worldNames = getWorldNames(it)

            GroupManager.getGroupsAllMatching(worldNames).map { it.name }
        },
        argumentDetails = argumentDetails,
        errorMissing = { it.sender.tSend("missing_group_name") },
        isValid = { argInfo ->
            if (GroupManager.getGroupsAllMatching(getWorldNames(argInfo)).none { it.name == argInfo.arg }) {
                false to { it.sender.tSend("group_not_found", "groupName" to argInfo.arg) }
            } else {
                true to {}
            }
        }
    )
}

@RoosterCommand
val command = RootArgument(
    label = "cbt", /* CustomBlockTags */
    errorMissingChildArg = { it.sender.tSend("cbt_missing_arg") },
    followingArguments = ArgumentList({
        val base = mutableListOf(
            SimpleArgument.simple("interface", argumentDetails = ArgumentDetails(invoke = {
                SelectGroupInterface.openInventory(
                    it.sender as Player,
                    SelectGroupInterface.SelectGroupContext(
                        getWorldNames(it),
                        true
                    )
                )
            })),
            SimpleArgument.simple(
                "create", argumentDetails = ArgumentDetails(
                    followingArguments = CentralizedArgumentList(
                        CentralArgument(
                            key = "group",
                            isValid = { argInfo ->
                                val name = argInfo.arg
                                if (GroupManager.getGroups(getWorldNames(argInfo)).any { it.name == argInfo.arg }) {
                                    false to { it.sender.tSend("group_exists", "groupName" to argInfo.arg) }
                                } else {
                                    isValidFileName(name)
                                }
                            },
                            tabCompletions = { listOf("[Group Name]") },
                            errorMissing = { it.sender.tSend("missing_group_name") },
                            followingArguments = CentralizedArgumentList(
                                MaterialCommand.material(
                                    key = "materials",
                                    argumentDetails = ArgumentDetails(invoke = {
                                        val groupName = it.values["group"] as String
                                        val materialList = it.values["materials"] as List<Material>

                                        GroupManager.addGroup(
                                            groupName,
                                            getWorldNames(it),
                                            materialList
                                        )

                                        it.sender.tSend("created_group", "groupName" to groupName)
                                    })
                                )
                            )
                        )
                    )
                )
            ),
            SimpleArgument.simple(
                name = "edit",
                argumentDetails = ArgumentDetails(
                    followingArguments = CentralizedArgumentList(
                        groupsCommand(
                            argumentDetails = ArgumentDetails(
                                followingArguments = CentralizedArgumentList(
                                    MaterialCommand.material(
                                        key = "materials",
                                        argumentDetails = ArgumentDetails(invoke = {
                                            val groupName = it.values["group"] as String
                                            val materialList = it.values["materials"] as List<Material>

                                            GroupManager.modifyGroup(
                                                groupName,
                                                getWorldNames(it),
                                                materialList
                                            )

                                            it.sender.tSend("edited_group", "groupName" to groupName)
                                        })
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            SimpleArgument.simple(
                key = "argTypes",
                names = listOf("delete", "info"),
                argumentDetails = ArgumentDetails(
                    followingArguments = CentralizedArgumentList(
                        groupsCommand(argumentDetails = ArgumentDetails(invoke = {
                            val argType = it.values["argTypes"] as String
                            val groupName = it.values["group"] as String

                            when (argType) {
                                "delete" -> {
                                    GroupManager.deleteGroup(
                                        groupName,
                                        getWorldNames(it),
                                    )
                                    it.sender.tSend("deleted_group", "groupName" to groupName)
                                }

                                "info" -> {
                                    fun List<Material>.joinToString(): String =
                                        this.joinToString("<green>, <gold>") { it.name.lowercase() }

                                    val groups = GroupManager.getGroups(getWorldNames(it))
                                    val materialGroups = groups
                                        .filter { it.name == groupName }
                                        .groupBy { it.newMaterials to it.oldMaterials }

                                    lateinit var messageKey: String
                                    val messageReplacements: MutableList<Pair<String, String>> = mutableListOf()

                                    if (materialGroups.size == 1) {
                                        val (newMaterials, oldMaterials) = materialGroups.keys.first()
                                        if (newMaterials == null) {
                                            messageReplacements.add("materials" to oldMaterials!!.joinToString())
                                            messageKey = "group_list"
                                        } else {
                                            messageReplacements.add("materials" to newMaterials.joinToString())
                                            if (oldMaterials != null) {
                                                messageKey = "group_list_modified"
                                                messageReplacements.add("oldMaterials" to oldMaterials.joinToString())
                                            } else {
                                                messageKey = "group_list_unregistered"
                                            }
                                        }
                                        it.sender.tSend(messageKey, *messageReplacements.toTypedArray())
                                    } else {
                                        it.sender.tSend("group_list_multiple")

                                        groups.forEach { group ->
                                            messageReplacements.clear()

                                            val (newMaterials, oldMaterials) = materialGroups.keys.first()
                                            messageReplacements.add("worldName" to group.worldName)
                                            if (newMaterials == null) {
                                                messageReplacements.add("materials" to oldMaterials!!.joinToString())
                                                messageKey = "group_list_entry"
                                            } else {
                                                messageReplacements.add("materials" to newMaterials.joinToString())
                                                if (oldMaterials != null) {
                                                    messageKey = "group_list_entry_modified"
                                                    messageReplacements.add("oldMaterials" to oldMaterials.joinToString())
                                                } else {
                                                    messageKey = "group_list_entry_unregistered"
                                                }
                                            }

                                            it.sender.tSend(messageKey, *messageReplacements.toTypedArray())
                                        }
                                    }

                                }
                            }
                        }))
                    )
                )
            ),
        ) as MutableList<BaseArgument>

        base.add(
            ModifierArgument(
                key = "worlds",
                tabCompletions = {
                    val list = mutableListOf("-current", "-global")

                    val base = it.arg.substringBeforeLast(",", "").trim()

                    val worldNames = if (base.isEmpty()) {
                        Bukkit.getWorlds().map { "-${it.name}" }
                    } else {
                        if (base.startsWith("-current") || base.startsWith("-global")) {
                            return@ModifierArgument list
                        }
                        val alreadyUsedWorlds = base.drop(1).split(",")

                        Bukkit.getWorlds().filter { it.name !in alreadyUsedWorlds }.map { "$base,${it.name}" }
                    }
                    list.addAll(worldNames)

                    list
                },
                isArgument = { it.arg.startsWith("-") },
                isValid = { argInfo ->
                    if (argInfo.arg == "-current" || argInfo.arg == "-global") return@ModifierArgument true to {}

                    val worldNames = mutableListOf<String>()
                    argInfo.arg.drop(1).split(",").forEach { worldName ->
                        if (Bukkit.getWorlds().none { it.name == worldName }) {
                            return@ModifierArgument false to {
                                argInfo.sender.tSend(
                                    "world_does_not_exist",
                                    "worldName" to worldName
                                )
                            }
                        }

                        if (worldName in worldNames) {
                            return@ModifierArgument false to {
                                argInfo.sender.tSend(
                                    "world_already_used",
                                    "worldName" to worldName
                                )
                            }
                        }
                        worldNames.add(worldName)
                    }

                    true to {}
                },
                isValidCompleter = { it.arg.startsWith("-") },
                argumentHandler = {
                    if (it.arg.startsWith("-current")) {
                        return@ModifierArgument listOf((it.sender as Player).world)
                    } else if (it.arg.startsWith("-global")) {
                        return@ModifierArgument Bukkit.getWorlds()
                    }
                    val worldName = it.arg.drop(1).split(",").map { Bukkit.getWorld(it) }

                    worldName
                }
            )
        )

        base
    }),
)

fun defaultWorlds(sender: CommandSender): List<World> {
    return if (globalAsDefault) Bukkit.getWorlds()
    else listOf((sender as Player).world)
}

fun getWorldNames(argInfo: ArgumentInfo): List<String> {
    return (
            argInfo.values["worlds"] as? List<World> ?: defaultWorlds(argInfo.sender)
            ).map { it.name }
}

fun getWorldNames(invokeInfo: InvokeInfo): List<String> {
    return (
            invokeInfo.values["worlds"] as? List<World> ?: defaultWorlds(invokeInfo.sender)
            ).map { it.name }
}