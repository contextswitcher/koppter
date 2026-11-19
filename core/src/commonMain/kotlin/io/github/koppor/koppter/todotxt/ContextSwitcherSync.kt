package io.github.koppor.koppter.todotxt

/**
 * The part of the filter that koppter and ContextSwitcher keep alike: the projects and the contexts.
 * Empty sets restrict nothing.
 */
// [impl->dsn~contextswitcher-sync~1]
data class SyncSelection(val projects: Set<String> = emptySet(), val contexts: Set<String> = emptySet())

/** The projects and contexts of the filter. */
// [impl->dsn~contextswitcher-sync~1]
val Filter.selection: SyncSelection get() = SyncSelection(projects, contexts)

/** The filter with the projects and contexts of [selection]; its other criteria stay. */
// [impl->dsn~contextswitcher-sync~1]
fun Filter.with(selection: SyncSelection): Filter = copy(projects = selection.projects, contexts = selection.contexts)

private val SYNC_LINK = Regex("^koppter:(?://)?sync/?(?:\\?(.*))?$", RegexOption.IGNORE_CASE)
private val ENCODED = Regex("(?:%[0-9A-Fa-f]{2})+")

/**
 * The projects and contexts that a link of ContextSwitcher sets, such as `koppter://sync?project=papers%3Esurvey&context=desk`;
 * `null` if the text is no such link.
 * A key that the link does not name is cleared: `koppter://sync` restricts neither projects nor contexts.
 */
// [impl->dsn~contextswitcher-sync~1]
fun syncOfLink(link: String): SyncSelection? {
    val match = SYNC_LINK.matchEntire(link.trim()) ?: return null
    val values = match.groupValues[1].split('&').filter(String::isNotEmpty).map { part ->
        part.substringBefore('=').lowercase() to ENCODED.replace(part.substringAfter('=', "")) { encoded ->
            encoded.value.split('%').drop(1).map { it.toInt(16).toByte() }.toByteArray().decodeToString()
        }
    }
    fun named(key: String, sign: String) = values.filter { it.first == key }.map { it.second.removePrefix(sign) }.filter(String::isNotEmpty).toSet()
    return SyncSelection(named(LINK_PROJECT, "+"), named(LINK_CONTEXT, "@"))
}

/**
 * The link that tells ContextSwitcher the projects and contexts, such as `contextswitcher://filter?project=papers%3Esurvey&context=desk`;
 * one without either key clears its filter.
 */
// [impl->dsn~contextswitcher-sync~1]
fun contextSwitcherLink(selection: SyncSelection): String {
    val parameters = selection.projects.sorted().map { LINK_PROJECT to it } + selection.contexts.sorted().map { LINK_CONTEXT to it }
    return "contextswitcher://filter" + if (parameters.isEmpty()) "" else "?" + parameters.joinToString("&") { (key, value) -> "$key=${encoded(value)}" }
}

/**
 * Keeps the two applications from sending each other the same selection back and forth:
 * a selection is sent only if it differs from the last one that was sent or received.
 */
// [impl->dsn~contextswitcher-sync~1]
class SyncGuard(private var last: SyncSelection) {
    /** Whether [selection] is to be sent; it is then the last one. */
    fun send(selection: SyncSelection): Boolean = (selection != last).also { last = selection }

    /** Takes the selection that arrived, so that it is not sent back. */
    fun received(selection: SyncSelection) {
        last = selection
    }
}
