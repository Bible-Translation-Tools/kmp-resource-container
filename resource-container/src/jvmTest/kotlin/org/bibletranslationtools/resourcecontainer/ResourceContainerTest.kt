package org.bibletranslationtools.resourcecontainer

import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResourceContainerTest {

    @get:Rule
    val resourceDir = TemporaryFolder()

    private val classLoader get() = this::class.java.classLoader!!

    private fun resourceFile(name: String): File =
        File(classLoader.getResource(name)!!.path)

    @Test
    fun loadContainer() {
        val container = ResourceContainer.load(resourceFile("open-en_tit_ulb"))
        assertNotNull(container)
        assertEquals(4, container.chapters().size)
        assertEquals(8, container.chunks("01").size)
        assertEquals("Titus", container.readChunk("front", "title").trim())
        assertEquals(container.info.packageVersion, ResourceContainer.VERSION)
        assertNotNull(container.toc)
        assertNotNull(container.config)
    }

    @Test
    fun loadBigContainer() {
        val start = System.nanoTime()
        val container = ResourceContainer.load(resourceFile("en_psa_ulb"))
        val time = (System.nanoTime() - start) / 1_000_000
        println("Execution time: $time ms")

        assertNotNull(container)
        assertEquals("Psalms", container.readChunk("front", "title").trim())
        assertNotNull(container.toc)
        assertNotNull(container.config)
    }

    @Test
    fun closeResourceContainer() {
        val archive = ResourceContainer.close(resourceFile("open-en_tit_ulb"))
        assertTrue(archive.exists())
    }

    @Test
    fun failClosingMissingContainer() {
        try {
            val archive = ResourceContainer.close(File("missing_file"))
            assertNull(archive)
        } catch (e: Exception) {
            assertNotNull(e)
        }
    }

    @Test
    fun reopenContainer() {
        val archive = ResourceContainer.close(resourceFile("open-en_tit_ulb"))
        assertTrue(archive.exists())

        val dir = File(archive.parentFile, "reopen-en_tit_ulb")
        val container = ResourceContainer.open(archive, dir)
        assertNotNull(container)
        assertEquals(dir.absolutePath, container.path.absolutePath)
        assertTrue(dir.exists())
        assertNotNull(container.toc)
        assertNotNull(container.config)
        assertEquals(container.info.packageVersion, ResourceContainer.VERSION)
    }

    @Test
    fun openResourceContainerArchive() {
        val archivePath = resourceFile("closed-en_tit_ulb.tsrc")
        val dir = File(archivePath.parentFile, "closed-en_tit_ulb").also {
            it.deleteRecursively()
        }

        val container = ResourceContainer.open(archivePath, dir)
        assertNotNull(container)
        assertTrue(dir.exists())
        assertNotNull(container.toc)
        assertNotNull(container.config)
        assertEquals(container.info.packageVersion, ResourceContainer.VERSION)
    }

    @Test
    fun failOpeningInvalidContainer() {
        val archivePath = resourceFile("raw_source.json")
        val dir = File(archivePath.parentFile, "closed-en_tit_ulb").also {
            it.deleteRecursively()
        }

        try {
            val container = ResourceContainer.open(archivePath, dir)
            assertNull(container)
        } catch (e: Exception) {
            assertNotNull(e)
        }

        try {
            val container = ResourceContainer.open(
                File("missing_file"),
                dir
            )
            assertNull(container)
        } catch (e: Exception) {
            assertNotNull(e)
        }
    }

    @Test
    fun inspectClosedContainer() {
        val info = ContainerTools.inspect(resourceFile("closed-en_tit_ulb.tsrc"))
        assertNotNull(info)
        assertEquals(info.packageVersion, ResourceContainer.VERSION)
    }

    @Test
    fun inspectOpenedContainer() {
        val info = ContainerTools.inspect(resourceFile("open-en_tit_ulb"))
        assertNotNull(info)
        assertEquals(info.packageVersion, ResourceContainer.VERSION)
    }

    @Test
    fun semverComparison() {
        val equal = 0
        val greater = 1
        val less = -1

        assertEquals(equal, Semver.compare("10.0.1", "10.0.1"))
        assertEquals(equal, Semver.compare("10.0", "10.0.0"))
        assertEquals(equal, Semver.compare("10.*", "10.0.0"))
        assertEquals(equal, Semver.compare("10.*", "10.9.0"))
        assertEquals(equal, Semver.compare("10.0.0", "10.0-alpha.0"))
        assertEquals(equal, Semver.compare("10.0.0", "v10.0.0"))
        assertEquals(equal, Semver.compare("10.*.1", "10.9.1"))
        assertEquals(equal, Semver.compare("0.8.1", "0.8.1"))
        assertEquals(equal, Semver.compare("*", "0.8.1"))
        assertEquals(equal, Semver.compare("0.8.1", "*"))

        assertEquals(greater, Semver.compare("10.0.0", "1.0.0"))
        assertEquals(greater, Semver.compare("10.1.0", "10.0.0"))
        assertEquals(greater, Semver.compare("10", "9.9.0"))
        assertEquals(greater, Semver.compare("10.1-alpha.0", "10.0.0"))
        assertEquals(greater, Semver.compare("10.9.6", "10.*.1"))
        assertEquals(greater, Semver.compare("0.9.6", "0.9.1"))
        assertEquals(greater, Semver.compare("0.10.0", "0.9.*"))

        assertEquals(less, Semver.compare("1.0.0", "10.0.0"))
        assertEquals(less, Semver.compare("10.0.0", "10.1.0"))
        assertEquals(less, Semver.compare("9.9.0", "10"))
        assertEquals(less, Semver.compare("10.0.0", "10.1-alpha.0"))
        assertEquals(less, Semver.compare("10.*.1", "10.9.6"))
        assertEquals(less, Semver.compare("0.9.1", "0.9.6"))
        assertEquals(less, Semver.compare("0.9.*", "0.10.0"))
    }

    @Test
    fun localizeChapterTitles() {
        assertEquals("Chapter 1", ContainerTools.localizeChapterTitle("en", 1))
        assertEquals("Chapter 1", ContainerTools.localizeChapterTitle("en", "01"))
        assertEquals("Chapter invalid", ContainerTools.localizeChapterTitle("en", "invalid"))
        assertEquals("Chapter 20", ContainerTools.localizeChapterTitle("en", 20))
        assertEquals("الفصل 1", ContainerTools.localizeChapterTitle("ar", 1))
        assertEquals("الفصل 20", ContainerTools.localizeChapterTitle("ar", 20))
        assertEquals("Глава 1", ContainerTools.localizeChapterTitle("ru", 1))
        assertEquals("1. fejezet", ContainerTools.localizeChapterTitle("hu", 1))
        assertEquals("Поглавље 1", ContainerTools.localizeChapterTitle("sr-Latin", 1))
        assertEquals("Chapter 1", ContainerTools.localizeChapterTitle("missing", 1))
    }

    @Test
    fun normalizeSlug() {
        assertThrows(Exception::class.java) { ContainerTools.normalizeSlug("") }

        assertEquals("01", ContainerTools.normalizeSlug("1"))
        assertEquals("01", ContainerTools.normalizeSlug("0001"))
        assertEquals("00", ContainerTools.normalizeSlug("0"))
        assertEquals("00", ContainerTools.normalizeSlug("00"))
        assertEquals("12", ContainerTools.normalizeSlug("12"))
        assertEquals("123", ContainerTools.normalizeSlug("123"))
        assertEquals("123", ContainerTools.normalizeSlug("00123"))
        assertEquals("a", ContainerTools.normalizeSlug("a"))
        assertEquals("word", ContainerTools.normalizeSlug("word"))
        assertEquals("00word", ContainerTools.normalizeSlug("00word"))
    }
}