package io.github.koppor.koppter.ui

import android.content.ClipData
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.material3.CalendarLocale
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.toClipEntry
import java.util.Locale

internal actual fun mondayFirstLocale(): CalendarLocale = Locale.Builder().setLocale(Locale.getDefault()).setUnicodeLocaleKeyword("fw", "mon").build()

// Takes effect where the text menu of Compose is enabled; elsewhere the calendar of the field remains.
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal actual fun TextMenu(items: List<Pair<String, () -> Unit>>, content: @Composable () -> Unit) =
    Box(Modifier.appendTextContextMenuComponents { for ((label, action) in items) item(key = label, label = label) { action(); close() } }) { content() }

internal actual fun plainText(text: String) = ClipData.newPlainText("plain text", text).toClipEntry()
