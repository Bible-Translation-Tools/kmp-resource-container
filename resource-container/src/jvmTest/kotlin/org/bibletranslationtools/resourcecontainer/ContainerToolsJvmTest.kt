package org.bibletranslationtools.resourcecontainer

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ContainerToolsJvmTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    private fun makePackageInfo(
        resourceType: String = "book",
        projectSlug: String = "gen",
        resourceSlug: String = "ulb"
    ) = PackageInfo(
        packageVersion = ResourceContainer.VERSION,
        modifiedAt = 20230101,
        contentMimeType = "text/markdown",
        language = Language(slug = "en", name = "English", direction = "ltr"),
        project = Project(slug = projectSlug, name = "Genesis", sort = 1),
        resource = Resource(
            slug = resourceSlug,
            name = "Unlocked Literal Bible",
            type = resourceType,
            status = Resource.Status(
                translateMode = "all",
                checkingLevel = "3",
                version = "1.0",
                license = "CC BY-SA 4.0"
            )
        )
    )

    // region convertResource - Content.Book

    @Test
    fun convertBook_createsExpectedFileStructure() {
        val content = Content.Book(
            chapters = listOf(
                Content.Book.Chapter(
                    number = "1", ref = "", title = "Genesis 1",
                    frames = listOf(
                        Content.Book.Frame(id = "01-01", lastVerse = "5", format = "md", img = "", text = "In the beginning")
                    )
                )
            ),
            modifiedAt = 20230101
        )
        val dir = File(tempDir.root, "en_gen_ulb")

        val container = ContainerTools.convertResource(content, makePackageInfo(), null, dir)

        assertNotNull(container)
        assertTrue(File(dir, "package.json").exists())
        assertTrue(File(dir, "LICENSE.md").exists())
        assertTrue(File(dir, "content/front/title.md").exists())
        assertTrue(File(dir, "content/01/title.md").exists())
        assertTrue(File(dir, "content/01/01.md").exists())
        assertEquals("In the beginning", File(dir, "content/01/01.md").readText())
    }

    @Test
    fun convertBook_frontTitleMatchesProjectName() {
        val content = Content.Book(
            chapters = listOf(
                Content.Book.Chapter(number = "1", ref = "", title = "Ch 1", frames = emptyList())
            ),
            modifiedAt = 20230101
        )
        val dir = File(tempDir.root, "en_gen_ulb")

        ContainerTools.convertResource(content, makePackageInfo(), null, dir)

        assertEquals("Genesis", File(dir, "content/front/title.md").readText())
    }

    @Test
    fun convertBook_chapterTitleWrittenFromContent() {
        val content = Content.Book(
            chapters = listOf(
                Content.Book.Chapter(number = "1", ref = "", title = "The Beginning", frames = emptyList())
            ),
            modifiedAt = 20230101
        )
        val dir = File(tempDir.root, "en_gen_ulb")

        ContainerTools.convertResource(content, makePackageInfo(), null, dir)

        assertEquals("The Beginning", File(dir, "content/01/title.md").readText())
    }

    @Test
    fun convertBook_emptyChapterTitle_usesLocalizedFallback() {
        val content = Content.Book(
            chapters = listOf(
                Content.Book.Chapter(number = "1", ref = "", title = "", frames = emptyList())
            ),
            modifiedAt = 20230101
        )
        val dir = File(tempDir.root, "en_gen_ulb")

        ContainerTools.convertResource(content, makePackageInfo(), null, dir)

        assertEquals("Chapter 1", File(dir, "content/01/title.md").readText())
    }

    @Test
    fun convertBook_frame00_usesFirstVerseFromText() {
        val content = Content.Book(
            chapters = listOf(
                Content.Book.Chapter(
                    number = "1", ref = "", title = "Ch 1",
                    frames = listOf(
                        Content.Book.Frame(
                            id = "01-00", lastVerse = "3", format = "md", img = "",
                            text = """<verse number="1" style="v" />In the beginning"""
                        )
                    )
                )
            ),
            modifiedAt = 20230101
        )
        val dir = File(tempDir.root, "en_gen_ulb")

        ContainerTools.convertResource(content, makePackageInfo(), null, dir)

        assertTrue(File(dir, "content/01/01.md").exists())
        assertFalse(File(dir, "content/01/00.md").exists())
    }

    @Test
    fun convertBook_withWordAssignments_writesWordsToConfigFile() {
        val content = Content.Book(
            chapters = listOf(
                Content.Book.Chapter(
                    number = "1", ref = "", title = "Ch 1",
                    frames = listOf(
                        Content.Book.Frame(id = "01-01", lastVerse = "1", format = "md", img = "", text = "text")
                    )
                )
            ),
            modifiedAt = 20230101
        )
        val wordAssignments = WordAssignments(
            chapters = listOf(
                WordAssignments.Chapter(
                    id = "01",
                    frames = listOf(
                        WordAssignments.Frame(
                            id = "01",
                            items = listOf(WordAssignments.Item(id = "god"))
                        )
                    )
                )
            )
        )
        val dir = File(tempDir.root, "en_gen_ulb")

        ContainerTools.convertResource(content, makePackageInfo(), wordAssignments, dir)

        val configFile = File(dir, "content/config.yml")
        assertTrue(configFile.exists())
        assertTrue(configFile.readText().contains("god"))
    }

    @Test
    fun convertBook_licenseContentWrittenFromPackageInfo() {
        val info = makePackageInfo()
        val content = Content.Book(chapters = emptyList(), modifiedAt = 20230101)
        val dir = File(tempDir.root, "en_gen_ulb")

        ContainerTools.convertResource(content, info, null, dir)

        assertEquals("CC BY-SA 4.0", File(dir, "LICENSE.md").readText())
    }

    // endregion

    // region convertResource - Content.Notes

    @Test
    fun convertNotes_createsChunkFile() {
        val content = Content.Notes(
            frames = listOf(
                Content.Notes.Frame(
                    id = "01-01",
                    notes = listOf(Content.Notes.Note(ref = "General Information:", text = "Some note"))
                )
            )
        )
        val dir = File(tempDir.root, "en_gen_tn")

        ContainerTools.convertResource(content, makePackageInfo(resourceType = "help", resourceSlug = "tn"), null, dir)

        assertTrue(File(dir, "content/01/01.md").exists())
    }

    @Test
    fun convertNotes_noFrontMatterForHelpType() {
        val content = Content.Notes(frames = emptyList())
        val dir = File(tempDir.root, "en_gen_tn")

        ContainerTools.convertResource(content, makePackageInfo(resourceType = "help", resourceSlug = "tn"), null, dir)

        assertFalse(File(dir, "content/front").exists())
    }

    @Test
    fun convertNotes_skipsChunk00() {
        val content = Content.Notes(
            frames = listOf(
                Content.Notes.Frame(
                    id = "01-00",
                    notes = listOf(Content.Notes.Note(ref = "skip", text = "skip"))
                )
            )
        )
        val dir = File(tempDir.root, "en_gen_tn")

        ContainerTools.convertResource(content, makePackageInfo(resourceType = "help", resourceSlug = "tn"), null, dir)

        assertFalse(File(dir, "content/01/00.md").exists())
    }

    @Test
    fun convertNotes_emptyBodySkipsFile() {
        val content = Content.Notes(
            frames = listOf(
                Content.Notes.Frame(id = "01-01", notes = emptyList())
            )
        )
        val dir = File(tempDir.root, "en_gen_tn")

        ContainerTools.convertResource(content, makePackageInfo(resourceType = "help", resourceSlug = "tn"), null, dir)

        assertFalse(File(dir, "content/01/01.md").exists())
    }

    // endregion

    // region convertResource - Content.Questions

    @Test
    fun convertQuestions_createsChunkFile() {
        val content = Content.Questions(
            chapters = listOf(
                Content.Questions.Chapter(
                    id = "01",
                    questions = listOf(
                        Content.Questions.Question(
                            question = "Who created the heavens?",
                            answer = "God",
                            ref = listOf("01-01")
                        )
                    )
                )
            )
        )
        val dir = File(tempDir.root, "en_gen_tq")

        ContainerTools.convertResource(content, makePackageInfo(resourceType = "help", resourceSlug = "tq"), null, dir)

        assertTrue(File(dir, "content/01/01.md").exists())
    }

    @Test
    fun convertQuestions_questionTextWrittenToFile() {
        val content = Content.Questions(
            chapters = listOf(
                Content.Questions.Chapter(
                    id = "01",
                    questions = listOf(
                        Content.Questions.Question(
                            question = "Who created?",
                            answer = "God",
                            ref = listOf("01-01")
                        )
                    )
                )
            )
        )
        val dir = File(tempDir.root, "en_gen_tq")

        ContainerTools.convertResource(content, makePackageInfo(resourceType = "help", resourceSlug = "tq"), null, dir)

        val chunkText = File(dir, "content/01/01.md").readText()
        assertTrue(chunkText.contains("Who created?"))
        assertTrue(chunkText.contains("God"))
    }

    // endregion

    // region convertResource - Content.Words

    @Test
    fun convertWords_createsWordDirWithFile() {
        val content = Content.Words(
            words = listOf(
                Content.Words.Word(
                    id = "god", term = "God", title = null,
                    def = "The creator.", sub = "",
                    seeAlso = emptyList(), examples = emptyList(), aliases = emptyList()
                )
            )
        )
        val dir = File(tempDir.root, "en_bible_tw")

        ContainerTools.convertResource(content, makePackageInfo(resourceType = "dict", resourceSlug = "tw"), null, dir)

        val wordFile = File(dir, "content/god/01.md")
        assertTrue(wordFile.exists())
        assertTrue(wordFile.readText().contains("God"))
        assertTrue(wordFile.readText().contains("The creator."))
    }

    @Test
    fun convertWords_noFrontMatterForDictType() {
        val content = Content.Words(words = emptyList())
        val dir = File(tempDir.root, "en_bible_tw")

        ContainerTools.convertResource(content, makePackageInfo(resourceType = "dict", resourceSlug = "tw"), null, dir)

        assertFalse(File(dir, "content/front").exists())
    }

    @Test
    fun convertWords_seeAlsoWrittenToConfig() {
        val content = Content.Words(
            words = listOf(
                Content.Words.Word(
                    id = "god", term = "God", title = null,
                    def = "The creator.", sub = "",
                    seeAlso = listOf("jesus"), examples = emptyList(), aliases = emptyList()
                )
            )
        )
        val dir = File(tempDir.root, "en_bible_tw")

        ContainerTools.convertResource(content, makePackageInfo(resourceType = "dict", resourceSlug = "tw"), null, dir)

        val configFile = File(dir, "content/config.yml")
        assertTrue(configFile.exists())
        assertTrue(configFile.readText().contains("jesus"))
    }

    // endregion

    // region convertResource - Content.Manual

    @Test
    fun convertManual_createsArticleFiles() {
        val content = Content.Manual(
            articles = listOf(
                Content.Manual.Article(
                    id = "intro", title = "Introduction",
                    question = "What is this?", text = "This is an introduction.",
                    recommend = null, depend = null
                )
            ),
            toc = ""
        )
        val dir = File(tempDir.root, "en_ta_intro")

        ContainerTools.convertResource(content, makePackageInfo(resourceType = "man", resourceSlug = "ta"), null, dir)

        assertTrue(File(dir, "content/intro/title.md").exists())
        assertTrue(File(dir, "content/intro/sub-title.md").exists())
        assertTrue(File(dir, "content/intro/01.md").exists())
        assertEquals("Introduction", File(dir, "content/intro/title.md").readText())
        assertEquals("What is this?", File(dir, "content/intro/sub-title.md").readText())
        assertEquals("This is an introduction.", File(dir, "content/intro/01.md").readText())
    }

    @Test
    fun convertManual_underscoreInId_replacedWithDash() {
        val content = Content.Manual(
            articles = listOf(
                Content.Manual.Article(
                    id = "some\\_article", title = "Some Article",
                    question = "Q?", text = "body",
                    recommend = null, depend = null
                )
            ),
            toc = ""
        )
        val dir = File(tempDir.root, "en_ta_ta")

        ContainerTools.convertResource(content, makePackageInfo(resourceType = "man", resourceSlug = "ta"), null, dir)

        assertTrue(File(dir, "content/some-article").exists())
    }

    // endregion

    // region convertResource - error cases

    @Test
    fun convertResource_throwsWhenArchiveAlreadyExists() {
        val dir = File(tempDir.root, "en_gen_ulb")
        val archive = File(tempDir.root, "en_gen_ulb.tsrc")
        archive.createNewFile()

        assertFailsWith<Exception> {
            ContainerTools.convertResource(Content.Book(chapters = emptyList(), modifiedAt = 0), makePackageInfo(), null, dir)
        }
    }

    // endregion

    // region inspect

    @Test
    fun inspect_throwsForMissingPath() {
        assertFailsWith<Exception> {
            ContainerTools.inspect(File(tempDir.root, "nonexistent"))
        }
    }

    @Test
    fun inspect_throwsForInvalidFileExtension() {
        val file = tempDir.newFile("container.invalid")

        assertFailsWith<Exception> {
            ContainerTools.inspect(file)
        }
    }

    // endregion
}
