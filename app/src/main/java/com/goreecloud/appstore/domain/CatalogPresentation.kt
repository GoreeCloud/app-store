package com.goreecloud.appstore.domain

import java.util.Locale

enum class CatalogSort {
    CATALOG_ORDER,
    NAME,
    CATEGORY,
}

object CatalogPresentation {
    fun filterAndSort(
        items: List<StoreItem>,
        query: String,
        category: String?,
        sort: CatalogSort,
    ): List<StoreItem> {
        val filtered = items.filter { item ->
            val matchesCategory = category == null || item.category == category
            matchesCategory && CatalogSearch.matches(item, query)
        }

        return when (sort) {
            CatalogSort.CATALOG_ORDER -> filtered
            CatalogSort.NAME -> filtered.sortedWith(
                compareBy<StoreItem> { it.name.lowercase(Locale.ROOT) }
                    .thenBy { it.id },
            )
            CatalogSort.CATEGORY -> filtered.sortedWith(
                compareBy<StoreItem> { it.category.lowercase(Locale.ROOT) }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
                    .thenBy { it.id },
            )
        }
    }
}
