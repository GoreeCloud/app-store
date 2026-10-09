package com.goreecloud.appstore.domain

/**
 * Resolves the current item only from the active identity's entitled catalog.
 * A selection from a previous identity or older catalog cannot supply details.
 * The caller must apply entitlement filtering before building [entitledById].
 */
object CatalogDetailSelection {
    fun resolve(
        selected: StoreItem?,
        entitledById: Map<String, StoreItem>,
    ): StoreItem? = selected?.let { entitledById[it.id] }
}
