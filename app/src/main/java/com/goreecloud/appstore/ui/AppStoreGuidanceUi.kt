@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.goreecloud.appstore.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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

    if (!guidanceState.setupCompleted || guidanceState.replayActive) {
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
                onCancelReplay = {
                    guidanceState = repository.cancelReplay(guidanceState)
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
    onCancelReplay: () -> Unit,
) {
    val step = state.setupStep
    val stepCount = AppStoreGuidanceState.LAST_SETUP_STEP + 1

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "GoreeCloud App Store",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (state.replayActive) {
                    TextButton(onClick = onCancelReplay) {
                        Text("Close")
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(stepCount) { index ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp),
                            shape = GlazeCapsuleShape,
                            color = if (index <= step) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ) {}
                    }
                }

                Text(
                    "${step + 1} of $stepCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = GlazeSmallCardShape,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        when (step) {
                            0 -> {
                                Text(
                                    "Welcome to GoreeCloud App Store",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "Browse the apps and services available to this identity.",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    "Your catalog updates as access and release state change.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            1 -> {
                                Text(
                                    "What works today",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                OnboardingCapabilityRow(
                                    title = "Available now",
                                    body = "Browse, search, sort, manage local collections, and use bounded package download and install on configured Development builds with Android confirmation.",
                                )
                                HorizontalDivider()
                                OnboardingCapabilityRow(
                                    title = "Not connected yet",
                                    body = "Production package delivery and updates, service launch, and account-backed installed history.",
                                )
                            }

                            else -> {
                                Text(
                                    "Helpful tips",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "Show short tips while you browse. You can change this later.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = GlazeSmallCardShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step > 0) {
                    TextButton(onClick = onPrevious) {
                        Text("Back")
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = if (step == AppStoreGuidanceState.LAST_SETUP_STEP) onComplete else onNext,
                ) {
                    Text(
                        if (step == AppStoreGuidanceState.LAST_SETUP_STEP) {
                            "Start browsing"
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
private fun OnboardingCapabilityRow(
    title: String,
    body: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun AppStoreCatalogGuidanceHint(
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Tip: Tap any item for details.",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            modifier = Modifier.heightIn(min = 40.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            onClick = onDismiss,
        ) {
            Text("Dismiss")
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
                "Choose contextual tips or replay first-use guidance. These settings do not change access or delivery authority.",
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
                Text("Replay first-use guide")
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
