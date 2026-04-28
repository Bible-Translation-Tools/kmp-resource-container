package org.bibletranslationtools.resourcecontainer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BookLinkTest {

    @Test
    fun chapterVerseLink() {
        val l = Link.parseLink("[[language/project/resource/01:02]]")
        assertEquals("language", l!!.language)
        assertEquals("project", l.project)
        assertEquals("resource", l.resource)
        assertEquals("01:02", l.arguments)
        assertEquals("01", l.chapter)
        assertEquals("02", l.chunk)
        assertFalse(l.isExternal)
        assertFalse(l.isMedia)
        assertTrue(l.isPassage)
    }

    @Test
    fun chapterLink() {
        val l = Link.parseLink("[[language/project/resource/01]]")
        assertEquals("language", l!!.language)
        assertEquals("project", l.project)
        assertEquals("resource", l.resource)
        assertEquals("01", l.arguments)
        assertEquals("01", l.chapter)
        assertFalse(l.isExternal)
        assertFalse(l.isMedia)
        // TRICKY: we can't actually determine if this is a passage link without a :
        assertFalse(l.isPassage)
    }

    @Test
    fun chapterVerseRangeLink() {
        val l = Link.parseLink("[[language/project/resource/01:02-06]]")
        assertEquals("language", l!!.language)
        assertEquals("project", l.project)
        assertEquals("resource", l.resource)
        assertEquals("01:02-06", l.arguments)
        assertEquals("01", l.chapter)
        assertEquals("02", l.chunk)
        assertEquals("06", l.lastChunk)
        assertFalse(l.isExternal)
        assertFalse(l.isMedia)
        assertTrue(l.isPassage)
    }

    @Test
    fun invalidChapterVerseLink() {
        try {
            val l = Link.parseLink("[[language/project/resource/01:02,06]]")
            assertNull(l)
        } catch (e: Exception) {
            assertNotNull(e)
        }
    }

    @Test
    fun titledMediaPassageLink() {
        val l = Link.parseLink("[Link Title](image:/language/project/resource/01:02)")
        assertEquals("Link Title", l!!.title)
        assertEquals("language", l.language)
        assertEquals("project", l.project)
        assertEquals("resource", l.resource)
        assertEquals("01:02", l.arguments)
        assertEquals("image", l.protocol)
        assertEquals("01", l.chapter)
        assertEquals("02", l.chunk)
        assertFalse(l.isExternal)
        assertTrue(l.isMedia)
        assertTrue(l.isPassage)
    }

    @Test
    fun titledMediaNoResourcePassageLink() {
        val l = Link.parseLink("[Link Title](image:/language/project/01:02)")
        assertEquals("Link Title", l!!.title)
        assertEquals("language", l.language)
        assertEquals("project", l.project)
        assertEquals("project", l.resource)
        assertEquals("01:02", l.arguments)
        assertEquals("image", l.protocol)
        assertEquals("01", l.chapter)
        assertEquals("02", l.chunk)
        assertFalse(l.isExternal)
        assertTrue(l.isMedia)
        assertTrue(l.isPassage)
    }

    @Test
    fun titledShorthandPassageLink() {
        val l = Link.parseLink("[Link Title](language/project/01:02)")
        assertEquals("Link Title", l!!.title)
        assertEquals("language", l.language)
        assertEquals("project", l.project)
        assertEquals("project", l.resource)
        assertEquals("01:02", l.arguments)
        assertFalse(l.isExternal)
        assertFalse(l.isMedia)
        assertTrue(l.isPassage)
    }

    @Test
    fun anonymousShorthandPassageLink() {
        val l = Link.parseLink("[[language/project/01:02]]")
        assertNull(l!!.title)
        assertEquals("language", l.language)
        assertEquals("project", l.project)
        assertEquals("project", l.resource)
        assertEquals("01:02", l.arguments)
        assertFalse(l.isExternal)
        assertFalse(l.isMedia)
        assertTrue(l.isPassage)
    }
}
