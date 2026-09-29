package com.goreecloud.appstore.library

import android.content.Context

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

    private fun key(subjectId: String): String {
        val normalizedSubject = subjectId.trim()
        require(normalizedSubject.isNotEmpty()) { "subjectId must not be blank" }
        return "$KEY_PREFIX$normalizedSubject"
    }

    companion object {
        private const val PREFERENCES_NAME = "goreecloud_app_store_saved_catalog"
        private const val KEY_PREFIX = "saved_items_v1:"
    }
}
