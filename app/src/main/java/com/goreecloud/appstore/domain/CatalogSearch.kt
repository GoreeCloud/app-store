package com.goreecloud.appstore.domain

import java.text.Normalizer
import java.util.Locale

/**
 * Searches only the caller-provided catalog subset. Callers must apply entitlement
 * filtering before passing items to this presentation-only helper.
 */
object CatalogSearch {
    private val marks = Regex("\\p{M}+")
    private val whitespace = Regex("\\s+")

    fun matches(item: StoreItem, query: String, includeType: Boolean = false): Boolean {
        val terms = normalize(query).trim().split(whitespace).filter { it.isNotEmpty() }
        if (terms.isEmpty()) return true

        val fields = listOf(item.name, item.summary, item.category) +
            if (includeType) {
                listOf(
                    when (item.type) {
                        StoreItemType.APPLICATION -> "Application"
                        StoreItemType.SERVICE -> "Service"
                    },
                )
            } else {
                emptyList()
            }
        val searchableFields = fields.map(::normalize)
        return terms.all { term -> searchableFields.any { field -> term in field } }
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKD)
            .replace(marks, "")
            .lowercase(Locale.ROOT)
}
