package com.goreecloud.appstore.library

import android.content.Context
import java.security.MessageDigest

internal object SavedCatalogIdentityNamespace {
    private const val KEY_PREFIX = "saved_items_v1:"

    fun keyFor(subjectId: String): String {
        require(subjectId.isNotBlank()) { "subjectId must not be blank" }

        // Identity subjects are opaque identifiers. Preserve the exact value rather than
        // normalizing whitespace, which could collapse two distinct subjects into one namespace.
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(subjectId.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
        return KEY_PREFIX + digest
    }
}

class SavedCatalogStore(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(subjectId: String): Set<String> =
        SavedCatalogSelection.normalize(
            preferences.getStringSet(key(subjectId), emptySet()).orEmpty(),
        )

    fun setSaved(
        subjectId: String,
        itemId: String,
        saved: Boolean,
    ): Set<String> {
        val next = SavedCatalogSelection.setSaved(
            current = load(subjectId),
            itemId = itemId,
            saved = saved,
        )
        check(
            preferences.edit()
                .putStringSet(key(subjectId), next)
                .commit(),
        ) { "Failed to persist saved App Store items." }
        return next
    }

    fun clear(subjectId: String): Set<String> {
        check(
            preferences.edit()
                .remove(key(subjectId))
                .commit(),
        ) { "Failed to clear saved App Store items." }
        return emptySet()
    }

    private fun key(subjectId: String): String =
        SavedCatalogIdentityNamespace.keyFor(subjectId)

    companion object {
        private const val PREFERENCES_NAME = "goreecloud_app_store_saved_catalog"
    }
}
