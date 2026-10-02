package com.goreecloud.appstore.library

import android.content.Context
import java.security.MessageDigest

internal object FavoriteCatalogIdentityNamespace {
    private const val KEY_PREFIX = "favorite_items_v1:"

    fun keyFor(subjectId: String): String {
        require(subjectId.isNotBlank()) { "subjectId must not be blank" }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(subjectId.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
        return KEY_PREFIX + digest
    }
}

/**
 * Device-local, identity-separated favorites for currently entitled catalog items.
 *
 * This is presentation metadata only. It does not establish installation, ownership, release,
 * purchase, history, account sync, or Everkeep recovery authority.
 */
class FavoriteCatalogStore(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(subjectId: String): Set<String> =
        SavedCatalogSelection.normalize(
            preferences.getStringSet(key(subjectId), emptySet()).orEmpty(),
        )

    fun setFavorite(
        subjectId: String,
        itemId: String,
        favorite: Boolean,
    ): Set<String> {
        val next = SavedCatalogSelection.setSaved(
            current = load(subjectId),
            itemId = itemId,
            saved = favorite,
        )
        check(
            preferences.edit()
                .putStringSet(key(subjectId), next)
                .commit(),
        ) { "Failed to persist favorite App Store items." }
        return next
    }

    fun clear(subjectId: String): Set<String> {
        check(
            preferences.edit()
                .remove(key(subjectId))
                .commit(),
        ) { "Failed to clear favorite App Store items." }
        return emptySet()
    }

    private fun key(subjectId: String): String =
        FavoriteCatalogIdentityNamespace.keyFor(subjectId)

    companion object {
        private const val PREFERENCES_NAME = "goreecloud_app_store_favorite_catalog"
    }
}
