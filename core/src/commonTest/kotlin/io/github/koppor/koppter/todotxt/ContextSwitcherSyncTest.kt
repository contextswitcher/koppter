package io.github.koppor.koppter.todotxt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// [utest->dsn~contextswitcher-sync~1]
class ContextSwitcherSyncTest {
    @Test
    fun aSyncLinkNamesProjectsAndContexts() {
        assertEquals(SyncSelection(setOf("papers>survey"), setOf("desk")), syncOfLink("koppter://sync?project=papers%3Esurvey&context=desk"))
        assertEquals(SyncSelection(setOf("Fair", "Küche"), setOf("phone")), syncOfLink("KOPPTER:sync/?project=%2BFair&project=K%C3%BCche&context=@phone"))
    }

    @Test
    fun aSyncLinkWithoutAKeyClearsIt() {
        assertEquals(SyncSelection(), syncOfLink("koppter://sync"))
        assertEquals(SyncSelection(projects = setOf("Fair")), syncOfLink("koppter://sync?project=Fair&context="))
    }

    @Test
    fun otherTextIsNoSyncLink() {
        assertNull(syncOfLink("koppter://filter?project=Fair"))
        assertNull(syncOfLink("https://example.org/sync?project=Fair"))
    }

    @Test
    fun theLinkToContextSwitcherCarriesTheSelection() {
        assertEquals(
            "contextswitcher://filter?project=papers%3Esurvey&context=desk",
            contextSwitcherLink(SyncSelection(setOf("papers>survey"), setOf("desk"))),
        )
        assertEquals("contextswitcher://filter", contextSwitcherLink(SyncSelection()))
    }

    @Test
    fun withReplacesOnlyProjectsAndContexts() {
        val filter = Filter(text = "mom", projects = setOf("Old"), contexts = setOf("phone"), hashtags = setOf("quick"))
        assertEquals(
            filter.copy(projects = setOf("Fair"), contexts = emptySet()),
            filter.with(SyncSelection(projects = setOf("Fair"))),
        )
        assertEquals(SyncSelection(setOf("Old"), setOf("phone")), filter.selection)
    }

    @Test
    fun theGuardSendsNeitherTwiceNorBack() {
        val guard = SyncGuard(SyncSelection())
        assertFalse(guard.send(SyncSelection()))
        assertTrue(guard.send(SyncSelection(projects = setOf("Fair"))))
        assertFalse(guard.send(SyncSelection(projects = setOf("Fair"))))
        // What arrived from ContextSwitcher is not sent back.
        guard.received(SyncSelection(contexts = setOf("desk")))
        assertFalse(guard.send(SyncSelection(contexts = setOf("desk"))))
        assertTrue(guard.send(SyncSelection()))
    }
}
