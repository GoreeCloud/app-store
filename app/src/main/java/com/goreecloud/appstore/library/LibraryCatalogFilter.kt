package com.goreecloud.appstore.library

import com.goreecloud.appstore.domain.StoreItem
import com.goreecloud.appstore.domain.StoreItemType

enum class LibraryItemTypeFilter {
    ALL,
    APPS,
    SERVICES,
}

object LibraryCatalogFilter {
    fun apply(
        items: List<StoreItem>,
        query: String,
        typeFilter: LibraryItemTypeFilter,
    ): List<StoreItem> {
        val normalizedQuery = query.trim()
        return items.filter { item ->
            val typeMatches = when (typeFilter) {
                LibraryItemTypeFilter.ALL -> true
                LibraryItemTypeFilter.APPS -> item.type == StoreItemType.APPLICATION
                LibraryItemTypeFilter.SERVICES -> item.type == StoreItemType.SERVICE
            }
            typeMatches &&
                (
                    normalizedQuery.isBlank() ||
                        item.name.contains(normalizedQuery, ignoreCase = true) ||
                        item.summary.contains(normalizedQuery, ignoreCase = true) ||
                        item.category.contains(normalizedQuery, ignoreCase = true)
                )
        }
    }
}
