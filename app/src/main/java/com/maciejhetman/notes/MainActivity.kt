package com.maciejhetman.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.maciejhetman.notes.data.AppSettings
import com.maciejhetman.notes.navigation.NotesNavHost
import com.maciejhetman.notes.ui.theme.LocalAppSettings
import com.maciejhetman.notes.ui.theme.NotesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as NotesApplication
            val settings by app.settingsRepository.settings.collectAsState(initial = AppSettings())

            NotesTheme(
                themeMode = settings.themeMode,
                themeColor = settings.themeColor,
                dynamicColor = settings.dynamicColor,
                amoledBlack = settings.amoledBlack
            ) {
                CompositionLocalProvider(LocalAppSettings provides settings) {
                    NotesNavHost()
                }
            }
        }
    }
}
