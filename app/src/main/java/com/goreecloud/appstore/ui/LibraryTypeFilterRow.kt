package com.goreecloud.appstore.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.goreecloud.appstore.library.LibraryItemTypeFilter

@Composable
internal fun LibraryTypeFilterRow(
    selected: LibraryItemTypeFilter,
    onSelected: (LibraryItemTypeFilter) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(end = 12.dp),
    ) {
        items(LibraryItemTypeFilter.entries, key = { it.name }) { filter ->
            FilterChip(
                modifier = Modifier.heightIn(min = 48.dp),
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = {
                    Text(
                        when (filter) {
                            LibraryItemTypeFilter.ALL -> "All"
                            LibraryItemTypeFilter.APPS -> "Apps"
                            LibraryItemTypeFilter.SERVICES -> "Services"
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
            )
        }
    }
}
