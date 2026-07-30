package com.maciejhetman.notes.ui.screens

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maciejhetman.notes.data.EditorLineSpacing
import com.maciejhetman.notes.data.LineNumberMode
import com.maciejhetman.notes.data.NoteFontFamily
import com.maciejhetman.notes.data.SyntaxTheme
import com.maciejhetman.notes.data.ThemeMode
import com.maciejhetman.notes.ui.util.tap
import com.maciejhetman.notes.ui.viewmodel.SettingsViewModel
import kotlin.math.roundToInt
import com.maciejhetman.notes.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val haptics = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            haptics.tap()
                            onBack()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            SettingsSectionHeader("Preview")
            SettingsLivePreviewCard(settings = settings)

            SettingsSectionHeader("Appearance")
            SettingsCard {
                SettingsMenuRow(
                    title = "Theme",
                    options = ThemeMode.entries,
                    selected = settings.themeMode,
                    labelFor = ::themeModeLabel,
                    onSelect = viewModel::setThemeMode
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = "Material You",
                        checked = settings.dynamicColor,
                        onCheckedChange = viewModel::setDynamicColor
                    )
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !settings.dynamicColor) {
                    SettingsDivider()
                    SettingsMenuRow(
                        title = "Color",
                        options = com.maciejhetman.notes.data.AppThemeColor.entries,
                        selected = settings.themeColor,
                        labelFor = { it.label },
                        onSelect = viewModel::setThemeColor
                    )
                }

                SettingsDivider()
                SettingsSwitchRow(
                    title = "AMOLED black",
                    checked = settings.amoledBlack,
                    onCheckedChange = viewModel::setAmoledBlack
                )
            }

            SettingsSectionHeader("Typography & Font")
            SettingsCard {
                SettingsMenuRow(
                    title = "Font style",
                    options = NoteFontFamily.entries,
                    selected = settings.fontFamily,
                    labelFor = { it.label },
                    onSelect = viewModel::setFontFamily
                )

                SettingsDivider()

                val fontPercent = (settings.fontSizeScale * 100).roundToInt()
                SettingsSliderRow(
                    title = "Font size",
                    value = settings.fontSizeScale,
                    valueRange = 0.70f..1.50f,
                    valueLabel = "$fontPercent%",
                    onValueChange = viewModel::setFontSizeScale
                )

                SettingsDivider()

                SettingsMenuRow(
                    title = "Line spacing",
                    options = EditorLineSpacing.entries,
                    selected = settings.lineSpacing,
                    labelFor = { it.label },
                    onSelect = viewModel::setLineSpacing
                )
            }

            SettingsSectionHeader("Code & Line Numbers")
            SettingsCard {
                SettingsMenuRow(
                    title = "Syntax highlighting theme",
                    options = SyntaxTheme.entries,
                    selected = settings.syntaxTheme,
                    labelFor = { it.label },
                    onSelect = viewModel::setSyntaxTheme
                )

                SettingsDivider()

                SettingsMenuRow(
                    title = "Line numbers",
                    options = LineNumberMode.entries,
                    selected = settings.lineNumberMode,
                    labelFor = ::lineNumberModeLabel,
                    onSelect = viewModel::setLineNumberMode
                )

                SettingsDivider()

                val countLabel = if (settings.enabledLanguages.isEmpty()) "All languages (${SUPPORTED_LANGUAGES.size})" else "${settings.enabledLanguages.size} of ${SUPPORTED_LANGUAGES.size} enabled"
                SettingsClickableRow(
                    title = "Visible programming languages",
                    subtitle = countLabel,
                    onClick = { showLanguageDialog = true }
                )
            }
        }
    }

    if (showLanguageDialog) {
        val currentSelected = remember(settings.enabledLanguages) {
            if (settings.enabledLanguages.isEmpty()) SUPPORTED_LANGUAGES.map { it.tag }.toSet()
            else settings.enabledLanguages
        }
        var tempSelected by remember { mutableStateOf(currentSelected) }

        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Visible Languages", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Select All",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { tempSelected = SUPPORTED_LANGUAGES.map { it.tag }.toSet() }
                                .padding(4.dp)
                        )
                        Text(
                            "Deselect All",
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { tempSelected = setOf("") }
                                .padding(4.dp)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SUPPORTED_LANGUAGES.forEach { lang ->
                        val isChecked = tempSelected.contains(lang.tag)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tempSelected = if (isChecked) {
                                        if (tempSelected.size > 1) tempSelected - lang.tag else tempSelected
                                    } else {
                                        tempSelected + lang.tag
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = null
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = lang.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setEnabledLanguages(tempSelected)
                    showLanguageDialog = false
                }) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


