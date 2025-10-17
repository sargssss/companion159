package com.lifelover.companion159.domain.models

import com.lifelover.companion159.R

enum class StorageCategory {
    AMMUNITION,
    EQUIPMENT;

    fun iconRes(): Int = when (this) {
        AMMUNITION -> R.drawable.bomb
        EQUIPMENT -> R.drawable.tool
    }
}

enum class QuantityType {
    AVAILABLE,
    NEEDED
}

sealed class DisplayCategory(
    val name: String,
    val titleRes: Int,
    val storageCategory: StorageCategory,
    val quantityType: QuantityType
) {
    data object Availability : DisplayCategory(
        name = "AVAILABILITY",
        titleRes = R.string.availability,
        storageCategory = StorageCategory.EQUIPMENT,
        quantityType = QuantityType.AVAILABLE
    )

    data object Ammunition : DisplayCategory(
        name = "AMMUNITION",
        titleRes = R.string.ammo,
        storageCategory = StorageCategory.AMMUNITION,
        quantityType = QuantityType.AVAILABLE
    )

    data object Needs : DisplayCategory(
        name = "NEEDS",
        titleRes = R.string.needs,
        storageCategory = StorageCategory.EQUIPMENT,
        quantityType = QuantityType.NEEDED
    )

    companion object {
        val entries: List<DisplayCategory> = listOf(
            Availability,
            Ammunition,
            Needs
        )

        fun valueOf(name: String): DisplayCategory = when (name.uppercase()) {
            "AVAILABILITY" -> Availability
            "AMMUNITION" -> Ammunition
            "NEEDS" -> Needs
            else -> throw IllegalArgumentException("Unknown DisplayCategory: $name")
        }
    }
}

fun DisplayCategory.toStorageCategory(): StorageCategory = this.storageCategory
fun DisplayCategory.titleRes(): Int = this.titleRes