package com.povarup.domain

fun Shift.workTypeDescription(): String? {
    val resolvedWorkType = workType ?: return null
    return when (resolvedWorkType) {
        WorkType.COOK -> buildList {
            add("Cook")
            cookCuisine?.let { add("${it.name.lowercase().replaceFirstChar { c -> c.uppercase() }} cuisine") }
            cookStation?.let { add("${it.name.lowercase().replaceFirstChar { c -> c.uppercase() }} station") }
            if (isBanquet == true) {
                add("Banquet")
            } else if (isBanquet == false) {
                add("Non-banquet")
            }
        }.joinToString(" · ")

        WorkType.WAITER -> buildList {
            add("Waiter")
            if (isBanquet == true) {
                add("Banquet")
            } else if (isBanquet == false) {
                add("Non-banquet")
            }
        }.joinToString(" · ")

        WorkType.BARTENDER -> buildList {
            add("Bartender")
            if (isBanquet == true) {
                add("Banquet")
            } else if (isBanquet == false) {
                add("Non-banquet")
            }
        }.joinToString(" · ")

        WorkType.DISHWASHER -> buildList {
            add("Dishwasher")
            dishwasherZone?.let { add("${it.name.lowercase().replaceFirstChar { c -> c.uppercase() }} zone") }
        }.joinToString(" · ")
    }
}
