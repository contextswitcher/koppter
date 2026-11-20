package io.github.koppor.koppter.ui

import androidx.compose.material3.CalendarLocale
import java.util.Locale

internal actual fun mondayFirstLocale(): CalendarLocale = Locale.Builder().setLocale(Locale.getDefault()).setUnicodeLocaleKeyword("fw", "mon").build()
