package com.goreecloud.appstore.library

import android.content.Context
import java.security.MessageDigest

internal object RecentlyViewedCatalogIdentityNamespace {
    private const val KEY_PREFIX = "recently_viewed_items_v1:"

    fun keyFor(subjectId: String): String {
        require(subjectId.isNotBlank()) { "subjectId must not be blank" }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(subjectId.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
        return KEY_PREFIX + digest
    }
}

internal object RecentlyViewedCatalogSelection {
    const val MAX_ITEMS = 20

    fun record(
        current: List<String>,
        itemId: String,
        maxItems: Int = MAX_ITEMS,
    ): List<String> {
        require(maxItems > 0) { "maxItems must be positive" }
        val id = itemId.trim()
        require(id.isNotEmpty()) { "itemId must not be blank" }
        return buildList {
            add(id)
            current.asSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .filterNot { it == id }
                .distinct()
                .take(maxItems - 1)
                .forEach(::add)
        }
    }

    fun encode(ids: List<String>): String =
        ids.asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .take(MAX_ITEMS)
            .joinToString(separator = "") { id -> "${id.length}:$id" }

    fun decode(raw: String?): List<String> {
        if (raw.isNullOrEmpty()) return emptyList()
        val result = mutableListOf<String>()
        var cursor = 0
        while (cursor < raw.length) {
            val colon = raw.indexOf(':', cursor)
            if (colon <= cursor) return emptyList()
            val length = raw.substring(cursor, colon).toIntOrNull() ?: return emptyList()
            if (length <= 0 || length > 4096) return emptyList()
            val start = colon + 1
            val end = start + length
            if (end > raw.length) return emptyList()
            val id = raw.substring(start, end)
            if (id.isBlank()) return emptyList()
            if (id !in result && result.size < MAX_ITEMS) result += id
            cursor = end
        }
        return result
    }
}

/**
 * Device-local, identity-separated recently viewed catalog items.
 *
 * This records only item identifiers and recency order for entitled catalog presentation. It does
 * not establish installation, ownership, purchase, download, account history, or Everkeep authority.
 */
class RecentlyViewedCatalogStore(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(subjectId: String): List<String> =
        RecentlyViewedCatalogSelection.decode(
            preferences.getString(key(subjectId), null),
        )

    fun record(
        subjectId: String,
        itemId: String,
    ): List<String> {
        val next = RecentlyViewedCatalogSelection.record(
            current = load(subjectId),
            itemId = itemId,
        )
        check(
            preferences.edit()
                .putString(key(subjectId), RecentlyViewedCatalogSelection.encode(next))
                .commit(),
        ) { "Failed to persist recently viewed App Store items." }
        return next
    }

    fun clear(subjectId: String): List<String> {
        check(
            preferences.edit()
                .remove(key(subjectId))
                .commit(),
        ) { "Failed to clear recently viewed App Store items." }
        return emptyList()
    }

    private fun key(subjectId: String): String =
        RecentlyViewedCatalogIdentityNamespace.keyFor(subjectId)

    companion object {
        private const val PREFERENCES_NAME = "goreecloud_app_store_recently_viewed_catalog"
    }
}
