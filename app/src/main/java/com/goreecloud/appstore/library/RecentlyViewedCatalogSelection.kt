package com.goreecloud.appstore.library

object RecentlyViewedCatalogSelection {
    const val DEFAULT_LIMIT = 12

    fun normalize(
        ids: Iterable<String>,
        limit: Int = DEFAULT_LIMIT,
    ): List<String> {
        require(limit > 0) { "limit must be positive" }
        val seen = linkedSetOf<String>()
        ids.forEach { raw ->
            val id = raw.trim()
            if (id.isNotEmpty()) seen += id
        }
        return seen.take(limit)
    }

    fun record(
        current: List<String>,
        itemId: String,
        limit: Int = DEFAULT_LIMIT,
    ): List<String> {
        require(limit > 0) { "limit must be positive" }
        val id = itemId.trim()
        require(id.isNotEmpty()) { "itemId must not be blank" }
        return normalize(listOf(id) + current.filterNot { it.trim() == id }, limit)
    }
}
