package de.CypDasHuhn.extendedPatterns.database

import de.CypDasHuhn.extendedPatterns.ui.SelectGroupInterface
import de.cypdashuhn.rooster.database.RoosterTable
import org.bukkit.Material
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

object GroupManager {
    @RoosterTable
    object Groups : IntIdTable() {
        val name = varchar("name", 64)
        val worldName = varchar("world_name", 64)
        val new_materials = text("new_materials").nullable()
        val oldMaterials = text("old_materials").nullable()
    }

    class Group(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Group>(Groups)

        var name by Groups.name
        var worldName by Groups.worldName
        var newMaterials by Groups.new_materials.transform(
            { it?.joinToString(",") }, // Serialize
            {
                it?.split(",")?.mapNotNull { materialString ->
                    runCatching { Material.valueOf(materialString) }.getOrNull() // Deserialize with error handling
                }
            }
        )
        var oldMaterials by Groups.oldMaterials.transform(
            { it?.joinToString(",") }, // Serialize
            {
                it?.split(",")?.mapNotNull { materialString ->
                    runCatching { Material.valueOf(materialString) }.getOrNull() // Deserialize with error handling
                }
            }
        )

        fun toDTO() = SelectGroupInterface.GroupDTO(name, worldName, oldMaterials, newMaterials)
    }

    fun addGroup(name: String, worldNames: List<String>, materialList: List<Material>) {
        transaction {
            worldNames.forEach { worldName ->
                Group.new {
                    this.name = name
                    this.worldName = worldName
                    this.newMaterials = materialList
                }
                DatapackManager.createOrOverrideDatapackEntry(name, worldName, materialList)
            }
        }
    }

    fun getGroupsAllMatching(worldNames: List<String>): List<Group> {
        return transaction {
            Group.all()
                .groupBy { it.name }
                .filter { (_, groups) -> groups.all { it.worldName in worldNames } }
                .map { it.value.first() }
        }
    }

    fun getGroups(worldNames: List<String>): List<Group> {
        return transaction {
            Group.find { Groups.worldName inList worldNames }.toList()
        }
    }

    fun deleteGroup(groupName: String, worldNames: List<String>) {
        transaction {
            Group.find { Groups.name eq groupName and (Groups.worldName inList worldNames) }.forEach { it.delete() }
            worldNames.forEach { worldName ->
                DatapackManager.deleteDatapackEntry(groupName, worldName)
            }
        }
    }

    fun modifyGroup(groupName: String, worldNames: List<String>, materials: List<Material>) {
        transaction {
            Group.find { Groups.name eq groupName and (Groups.worldName inList worldNames) }.forEach {
                it.newMaterials = materials
            }
            worldNames.forEach { worldName ->
                DatapackManager.createOrOverrideDatapackEntry(groupName, worldName, materials)
            }
        }
    }

    fun reloadMaterials() {
        transaction {
            Group.all().forEach {
                if (it.newMaterials != null) {
                    it.oldMaterials = it.newMaterials!!
                    it.newMaterials = null
                }
            }
        }
    }
}