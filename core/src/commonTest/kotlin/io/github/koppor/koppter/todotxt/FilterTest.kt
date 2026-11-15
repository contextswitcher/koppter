package io.github.koppor.koppter.todotxt

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class FilterTest {
    private val today = LocalDate(2026, 11, 12)
    private val file = TodoFile.parse(
        """
        (B) Schedule pickup +GarageSale @phone
        Post signs +GarageSale
        (A) Thank Mom @phone due:2026-11-20
        x Call Mom +Family @phone
        Plan holiday t:2026-12-01
        (A) Pay bill due:2026-11-13
        """.trimIndent(),
    )

    private fun descriptions(filter: Filter) =
        file.entries.filter { filter.matches(it.task, today) }.map { it.task.description }

    // [utest->dsn~filter~1]
    @Test
    fun theDefaultShowsOpenTasksThatAreNotInTheFuture() {
        assertEquals(listOf("Schedule pickup", "Post signs", "Thank Mom", "Pay bill"), descriptions(Filter()))
    }

    // [utest->dsn~filter~1]
    @Test
    fun filtersByCompletion() {
        assertEquals(listOf("Call Mom"), descriptions(Filter(completion = Completion.COMPLETED)))
        assertEquals(6, descriptions(Filter(completion = Completion.ALL, hideFuture = false)).size)
    }

    // [utest->dsn~filter~1]
    @Test
    fun criteriaAreCombinedWithAndValuesWithOr() {
        assertEquals(
            listOf("Schedule pickup", "Thank Mom"),
            descriptions(Filter(contexts = setOf("phone"), priorities = setOf('A', 'B'))),
        )
        assertEquals(listOf("Schedule pickup"), descriptions(Filter(contexts = setOf("phone"), projects = setOf("GarageSale"))))
    }

    // [utest->dsn~filter~1]
    @Test
    fun textMatchesTheWholeLineIgnoringCase() {
        assertEquals(listOf("Schedule pickup", "Post signs"), descriptions(Filter(text = "garagesale")))
    }

    // [utest->dsn~filter~1]
    @Test
    fun futureTasksAreShownOnRequest() {
        assertEquals(listOf("Plan holiday"), descriptions(Filter(text = "holiday", hideFuture = false)))
    }

    // [utest->dsn~context-hierarchy~1]
    @Test
    fun aContextIncludesTheContextsBelowIt() {
        val tasks = listOf("Read the survey @research>reading", "Review a paper @research", "Read the manual @researching", "Call @phone").map(Task::parse)
        fun matching(vararg contexts: String) = tasks.filter { Filter(contexts = contexts.toSet()).matches(it, today) }.map { it.description }

        assertEquals(listOf("Read the survey", "Review a paper"), matching("research"))
        assertEquals(listOf("Read the survey"), matching("research>reading"))
        assertEquals(listOf("a", "a>b", "a>b>c"), "a>b>c".withAncestors())
        assertEquals("c", "a>b>c".lastLevel)
        // A context stands directly before the contexts below it, not behind a name that only starts alike.
        assertEquals(listOf("a", "a>b", "a1"), listOf("a1", "a>b", "a").sortedWith(hierarchyOrder))
    }

    // [utest->dsn~project-hierarchy~1]
    @Test
    fun aProjectIncludesTheProjectsBelowIt() {
        val tasks = listOf("Outline the related work +papers>survey", "Pick a venue +papers", "Print +paperwork").map(Task::parse)
        fun matching(vararg projects: String) = tasks.filter { Filter(projects = projects.toSet()).matches(it, today) }.map { it.description }

        assertEquals(listOf("Outline the related work", "Pick a venue"), matching("papers"))
        assertEquals(listOf("Outline the related work"), matching("papers>survey"))
    }

    // [utest->dsn~due-time~1]
    @Test
    fun onTheSameDueDateTheTaskThatIsDueAtAnEarlierTimeComesFirst() {
        val tasks = TodoFile.parse("Pack due:2026-11-16\nPrepare the slides due:2026-11-16 dueby:14:00\nPrint the agenda due:2026-11-16 dueby:9:00\nBook a room due:2026-11-15")
        assertEquals(listOf("Book a room", "Print the agenda", "Prepare the slides", "Pack"), tasks.entries.sortedWith(listOrder(today)).map { it.task.description })
    }

    // [utest->dsn~sort-order~1]
    @Test
    fun ordersByCompletionPriorityDueDateAndPosition() {
        assertEquals(
            listOf("Pay bill", "Thank Mom", "Schedule pickup", "Post signs", "Plan holiday", "Call Mom"),
            file.entries.sortedWith(listOrder(today)).map { it.task.description },
        )
    }

    // [utest->dsn~sort-order~1]
    @Test
    fun overdueTasksComeFirstWhateverTheirPriorityIs() {
        val tasks = TodoFile.parse("(A) Write the report\n(C) Return the books due:2026-11-10\n(B) Pay the rent due:2026-11-11\nx (A) Send the form due:2026-11-01\nWater the plants due:2026-11-12")
        assertEquals(
            listOf("Pay the rent", "Return the books", "Write the report", "Water the plants", "Send the form"),
            tasks.entries.sortedWith(listOrder(today)).map { it.task.description },
        )
    }

    // [utest->dsn~filter~1]
    @Test
    fun hashtagsRestrictLikeProjectsAndContexts() {
        val tasks = listOf("Call Mom #family", "Pay bill #money #urgent", "Post signs").map(Task::parse)
        fun matching(filter: Filter) = tasks.filter { filter.matches(it, today) }.map { it.description }

        assertEquals(listOf("Call Mom", "Pay bill"), matching(Filter(hashtags = setOf("family", "urgent"))))
        assertEquals(listOf("Call Mom", "Pay bill", "Post signs"), matching(Filter()))
    }

    // [utest->dsn~next-actions~1]
    @Test
    fun theNextActionOfAProjectIsItsFirstTaskThatCanBeWorkedOn() {
        val today = LocalDate(2026, 11, 12)
        val entries = TodoFile.parse(
            """
            Ask the printer for an offer +Fair @waiting
            Book the stand +Fair @phone
            Print the flyers +Fair
            The stand is ready +Fair id:fair
            Build the stand +Fair p:fair t:2026-12-01
            Outline the survey +papers>survey #someday
            Collect the reviews +papers>draft @waiting>reviewers
            Fix the bike
            Learn to sail +Sailing t:2027-06-01
            x Call the fair office +Fair
            """.trimIndent(),
        ).entries
        fun texts(found: List<Entry>) = found.map { it.task.description }

        assertEquals(listOf("Book the stand", "Print the flyers", "Fix the bike"), texts(actionable(entries, today).sortedBy { it.index }))
        // One task per project, and each task without project.
        assertEquals(listOf("Book the stand", "Fix the bike"), texts(nextActions(entries, entries, today)))
        // Of the listed tasks: with the task at the phone filtered out, the next one of the project takes its place.
        assertEquals(listOf("Print the flyers", "Fix the bike"), texts(nextActions(entries.filterNot { "phone" in it.task.contexts }, entries, today)))
        // A task with two projects is the next action of both.
        val two = TodoFile.parse("Order the cake +Party +Family\nSend the invitations +Party\nCall Mom +Family\n").entries
        assertEquals(listOf("Order the cake"), texts(nextActions(two, two, today)))

        // Stuck are the projects whose tasks all wait or are for someday; a project that only has tasks before their threshold date is not.
        assertEquals(listOf("papers>draft", "papers>survey"), projectsWithoutNextAction(entries, today))
        val moving = TodoFile.parse("Outline the survey +papers>survey #someday\nWrite the draft +papers>draft\nSubmit +papers @waiting\n").entries
        // A project is not stuck while one below it moves.
        assertEquals(listOf("papers>survey"), projectsWithoutNextAction(moving, today))
    }

    // [utest->dsn~app-links~1]
    @Test
    fun aLinkNamesTheProjectsContextsAndHashtagsOfAFilter() {
        assertEquals(Filter(projects = setOf("Fair")), filterOfLink("koppter://filter?project=Fair"))
        assertEquals(Filter(contexts = setOf("phone")), filterOfLink("koppter://filter?context=phone"))
        assertEquals(Filter(hashtags = setOf("someday")), filterOfLink("koppter://filter?hashtag=someday"))
        assertEquals(
            Filter(projects = setOf("papers>survey", "Fair"), contexts = setOf("phone")),
            filterOfLink("koppter://filter?project=papers>survey&project=Fair&context=phone"),
        )
        // As other programs pass it on: with a slash after the name, encoded characters, the sign of the name, or without the slashes.
        assertEquals(Filter(projects = setOf("papers>survey")), filterOfLink("koppter://filter/?project=papers%3Esurvey"))
        assertEquals(Filter(projects = setOf("Küche")), filterOfLink("KOPPTER://filter?project=K%C3%BCche"))
        assertEquals(Filter(projects = setOf("Fair"), contexts = setOf("phone"), hashtags = setOf("someday")), filterOfLink("koppter:filter?project=%2BFair&context=@phone&hashtag=%23someday "))

        // No link of the application, or one that names nothing.
        assertEquals(null, filterOfLink("https://example.org/?project=Fair"))
        assertEquals(null, filterOfLink("koppter://filter"))
        assertEquals(null, filterOfLink("koppter://filter?project="))
        assertEquals(null, filterOfLink("koppter://filter?color=red"))
        assertEquals(null, filterOfLink("koppter://other?project=Fair"))
    }

    // [utest->dsn~app-link-copy~1]
    @Test
    fun theLinkToAProjectIsReadBackAsItsFilter() {
        assertEquals("koppter://filter?project=Fair", linkTo(LINK_PROJECT, "Fair"))
        assertEquals("koppter://filter?project=papers%3Esurvey", linkTo(LINK_PROJECT, "papers>survey"))
        assertEquals("koppter://filter?context=K%C3%BCche", linkTo(LINK_CONTEXT, "Küche"))
        assertEquals("koppter://filter?hashtag=R%26D", linkTo(LINK_HASHTAG, "R&D"))

        assertEquals(Filter(projects = setOf("papers>survey")), filterOfLink(linkTo(LINK_PROJECT, "papers>survey")))
        assertEquals(Filter(contexts = setOf("Küche")), filterOfLink(linkTo(LINK_CONTEXT, "Küche")))
        assertEquals(Filter(hashtags = setOf("R&D")), filterOfLink(linkTo(LINK_HASHTAG, "R&D")))
    }

    // [utest->dsn~filter-exclusion~1]
    @Test
    fun excludedProjectsContextsAndHashtagsLeaveTheirTasksOut() {
        val today = LocalDate(2026, 11, 12)
        fun shown(filter: Filter) = listOf(
            "Book the stand +Fair @phone #import",
            "Outline the survey +papers>survey @writing",
            "Fix the bike @home>garage",
            "Call Mom",
        ).filter { filter.matches(Task.parse(it), today) }.map { it.substringBefore(" +").substringBefore(" @") }

        assertEquals(listOf("Outline the survey", "Fix the bike", "Call Mom"), shown(Filter(excludedHashtags = setOf("import"))))
        // A project or a context is excluded with those below it.
        assertEquals(listOf("Book the stand", "Fix the bike", "Call Mom"), shown(Filter(excludedProjects = setOf("papers"))))
        assertEquals(listOf("Book the stand", "Outline the survey", "Call Mom"), shown(Filter(excludedContexts = setOf("home"))))
        // The exclusion holds against what is selected.
        assertEquals(emptyList(), shown(Filter(projects = setOf("Fair"), excludedHashtags = setOf("import"))))
        assertEquals(false, Filter(excludedHashtags = setOf("import")).isDefault)
    }

    // [utest->dsn~filter-remembered~1]
    @Test
    fun theLinkOfAFilterIsReadBackAsThatFilter() {
        val filter = Filter(
            completion = Completion.ALL,
            text = "call mom",
            priorities = setOf('A', 'C'),
            projects = setOf("papers>survey"),
            contexts = setOf("phone"),
            hashtags = setOf("quick"),
            excludedProjects = setOf("Old"),
            excludedContexts = setOf("waiting"),
            excludedHashtags = setOf("import"),
            hideFuture = false,
        )
        assertEquals(
            "koppter://filter?project=papers%3Esurvey&context=phone&hashtag=quick&not-project=Old&not-context=waiting&not-hashtag=import" +
                "&priority=A&priority=C&text=call%20mom&show=all&future=1",
            filter.toLink(),
        )
        assertEquals(filter, filterOfLink(filter.toLink()))
        assertEquals(Filter(completion = Completion.COMPLETED), filterOfLink(Filter(completion = Completion.COMPLETED).toLink()))
        assertEquals(Filter(excludedHashtags = setOf("import")), filterOfLink("koppter://filter?not-hashtag=import"))
        // The filter that restricts nothing names nothing.
        assertEquals(null, filterOfLink(Filter().toLink()))
        // A value that is no priority is left out.
        assertEquals(Filter(priorities = setOf('B')), filterOfLink("koppter://filter?priority=B&priority=high&priority=b"))
    }
}
