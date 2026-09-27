@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.goreecloud.appstore.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goreecloud.appstore.onboarding.AppStoreGuidanceRepository
import com.goreecloud.appstore.onboarding.AppStoreGuidanceState
import com.goreecloud.appstore.onboarding.SharedPreferencesAppStoreGuidanceStore

internal const val APP_STORE_CATALOG_HINT_ID = "catalog-item-details"

@Composable
fun GoreeCloudAppStoreRoot() {
    val context = LocalContext.current
    val repository = remember(context.applicationContext) {
        AppStoreGuidanceRepository(SharedPreferencesAppStoreGuidanceStore(context))
    }
    var guidanceState by remember(repository) { mutableStateOf(repository.load()) }
    var showGuidanceSettings by remember { mutableStateOf(false) }

    if (!guidanceState.setupCompleted) {
        GlazeTheme {
            AppStoreOnboardingWizard(
                state = guidanceState,
                onPrevious = {
                    guidanceState = repository.previousSetupStep(guidanceState)
                },
                onNext = {
                    guidanceState = repository.nextSetupStep(guidanceState)
                },
                onHintsEnabledChanged = { enabled ->
                    guidanceState = repository.setHintsEnabled(guidanceState, enabled)
                },
                onComplete = {
                    guidanceState = repository.completeSetup(guidanceState)
                },
            )
        }
    } else {
        GoreeCloudAppStore(
            guidanceState = guidanceState,
            onShowGuidanceSettings = { showGuidanceSettings = true },
            onDismissGuidanceHint = { hintId ->
                guidanceState = repository.dismissHint(guidanceState, hintId)
            },
        )

        if (showGuidanceSettings) {
            GlazeTheme {
                AppStoreGuidanceSettingsSheet(
                    state = guidanceState,
                    onHintsEnabledChanged = { enabled ->
                        guidanceState = repository.setHintsEnabled(guidanceState, enabled)
                    },
                    onResetDismissedHints = {
                        guidanceState = repository.resetDismissedHints(guidanceState)
                    },
                    onReplaySetup = {
                        showGuidanceSettings = false
                        guidanceState = repository.replaySetup(guidanceState)
                    },
                    onDismiss = { showGuidanceSettings = false },
                )
            }
        }
    }
}

@Composable
private fun AppStoreOnboardingWizard(
    state: AppStoreGuidanceState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onHintsEnabledChanged: (Boolean) -> Unit,
    onComplete: () -> Unit,
) {
    val step = state.setupStep

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                "GoreeCloud App Store",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Step ${step + 1} of ${AppStoreGuidanceState.LAST_SETUP_STEP + 1}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = GlazeCardShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (step) {
                        0 -> {
                            Text(
                                "Welcome to your GoreeCloud catalog",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Browse applications and services that the active identity is allowed to see. " +
                                    "Your catalog can change when identity, entitlement, or release state changes.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                "This setup explains the current Development boundaries before ordinary use.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        1 -> {
                            Text(
                                "Know what the Store can do today",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "This Development build can present catalog and integration status, but it " +
                                    "does not manufacture Identity, entitlement, package trust, release approval, " +
                                    "or installation authority.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                "Install, update, library, and production service actions remain unavailable " +
                                    "until their authoritative integrations are connected and accepted.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        else -> {
                            Text(
                                "Choose helpful guidance",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Short contextual tips can explain catalog and Development behavior while you " +
                                    "use the Store. You can turn all ordinary tips off, turn them back on, reset " +
                                    "dismissed tips, or replay this setup later.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        "Contextual tips",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        if (state.hintsEnabled) "On" else "Off",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = state.hintsEnabled,
                                    onCheckedChange = onHintsEnabledChanged,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onPrevious,
                    enabled = step > 0,
                ) {
                    Text("Back")
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = if (step == AppStoreGuidanceState.LAST_SETUP_STEP) {
                        onComplete
                    } else {
                        onNext
                    },
                ) {
                    Text(
                        if (step == AppStoreGuidanceState.LAST_SETUP_STEP) {
                            "Finish setup"
                        } else {
                            "Continue"
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun AppStoreCatalogGuidanceHint(
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GlazeSmallCardShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Catalog tip",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                "Tap an item to inspect its channel and delivery availability. Installation stays " +
                    "disabled until trusted release metadata, provenance, verification, and package " +
                    "delivery are connected.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun AppStoreGuidanceSettingsSheet(
    state: AppStoreGuidanceState,
    onHintsEnabledChanged: (Boolean) -> Unit,
    onResetDismissedHints: () -> Unit,
    onReplaySetup: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Guidance & setup",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Control ordinary contextual tips or replay the startup guide. These settings do not " +
                    "change identity, entitlement, package trust, or delivery authority.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "Contextual tips",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (state.hintsEnabled) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.hintsEnabled,
                    onCheckedChange = onHintsEnabledChanged,
                )
            }

            HorizontalDivider()

            TextButton(
                onClick = onResetDismissedHints,
                enabled = state.dismissedHintIds.isNotEmpty(),
            ) {
                Text("Show dismissed tips again")
            }
            TextButton(onClick = onReplaySetup) {
                Text("Replay startup guide")
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss,
            ) {
                Text("Done")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
