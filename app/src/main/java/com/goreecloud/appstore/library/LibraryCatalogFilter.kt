package com.goreecloud.appstore.library

import com.goreecloud.appstore.domain.CatalogSearch
import com.goreecloud.appstore.domain.StoreItem
import com.goreecloud.appstore.domain.StoreItemType

enum class LibraryItemTypeFilter {
    ALL,
    APPS,
    SERVICES,
}

/**
 * Presentation-only filtering over an already-entitled, identity-scoped Library
 * collection. Never pass the unfiltered Development catalog to this helper.
 */
object LibraryCatalogFilter {
    fun apply(
        items: List<StoreItem>,
        query: String,
        typeFilter: LibraryItemTypeFilter,
    ): List<StoreItem> = items.filter { item ->
        val typeMatches = when (typeFilter) {
            LibraryItemTypeFilter.ALL -> true
            LibraryItemTypeFilter.APPS -> item.type == StoreItemType.APPLICATION
            LibraryItemTypeFilter.SERVICES -> item.type == StoreItemType.SERVICE
        }
        typeMatches && CatalogSearch.matches(item, query, includeType = true)
    }
}
