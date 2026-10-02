package com.goreecloud.appstore.library

object SavedCatalogSelection {
    fun normalize(ids: Iterable<String>): Set<String> =
        ids.asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toCollection(linkedSetOf())

    fun setSaved(
        current: Set<String>,
        itemId: String,
        saved: Boolean,
    ): Set<String> {
        val id = itemId.trim()
        require(id.isNotEmpty()) { "itemId must not be blank" }

        val next = normalize(current).toMutableSet()
        if (saved) next += id else next -= id
        return next.toSet()
    }
}
