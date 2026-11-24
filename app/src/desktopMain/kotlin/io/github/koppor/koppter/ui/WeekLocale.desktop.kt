package io.github.koppor.koppter.ui

import androidx.compose.foundation.ContextMenuDataProvider
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.material3.CalendarLocale
import androidx.compose.runtime.Composable
import java.util.Locale

internal actual fun mondayFirstLocale(): CalendarLocale = Locale.Builder().setLocale(Locale.getDefault()).setUnicodeLocaleKeyword("fw", "mon").build()

@Composable
internal actual fun TextMenu(items: List<Pair<String, () -> Unit>>, content: @Composable () -> Unit) =
    ContextMenuDataProvider(items = { items.map { (label, action) -> ContextMenuItem(label, action) } }, content = content)
