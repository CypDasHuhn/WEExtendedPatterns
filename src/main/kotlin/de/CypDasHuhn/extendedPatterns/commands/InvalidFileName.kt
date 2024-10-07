package de.CypDasHuhn.extendedPatterns.commands

import de.cypdashuhn.rooster.commands.argument_constructors.ArgumentInfo
import de.cypdashuhn.rooster.localization.tSend

fun isValidFileName(name: String): Pair<Boolean, (ArgumentInfo) -> Unit> {
    val invalidChars = listOf('\\', '/', ':', '*', '?', '"', '<', '>', '|')
    val reservedNames = listOf(
        "CON",
        "PRN",
        "AUX",
        "NUL",
        "COM1",
        "COM2",
        "COM3",
        "COM4",
        "COM5",
        "COM6",
        "COM7",
        "COM8",
        "COM9",
        "LPT1",
        "LPT2",
        "LPT3",
        "LPT4",
        "LPT5",
        "LPT6",
        "LPT7",
        "LPT8",
        "LPT9"
    )

    if (name in reservedNames) return false to { argInfo ->
        argInfo.sender.tSend(
            "reserved_name_not_allowed",
            "name" to name
        )
    }
    if (name.endsWith('.')) return false to { argInfo ->
        argInfo.sender.tSend(
            "name_ends_with_dot",
            "name" to name
        )
    }

    name.forEach { char ->
        if (char in invalidChars) {
            return false to { argInfo ->
                argInfo.sender.tSend(
                    "invalid_character_in_name",
                    "char" to char.toString()
                )
            }
        }
    }

    return true to {}
}