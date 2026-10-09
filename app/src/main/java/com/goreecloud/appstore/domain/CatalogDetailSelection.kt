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
    /**
     * A new opening of the same item is not the same request. Revision binding
     * prevents a delayed previous opening from changing the current detail state.
     */
    fun isCurrentRequest(
        requestRevision: Long,
        currentRevision: Long,
        requestSession: IdentitySession,
        currentSession: IdentitySession,
        selectedItemId: String?,
        requestItemId: String,
    ): Boolean =
        requestRevision == currentRevision &&
            requestSession == currentSession &&
            selectedItemId == requestItemId

}
