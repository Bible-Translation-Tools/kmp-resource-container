package org.bibletranslationtools.resourcecontainer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GenericLinkTest {

    @Test
    fun anonymousContainerLink() {
        val l = Link.parseLink("[[language/project/resource]]")
        assertNull(l!!.title)
        assertEquals("language", l.language)
        assertEquals("project", l.project)
        assertEquals("resource", l.resource)
        assertNull(l.arguments)
        assertFalse(l.isExternal)
        assertFalse(l.isMedia)
        assertFalse(l.isPassage)
    }

    @Test
    fun anonymousHttpsLink() {
        val l = Link.parseLink("[[https://www.example.com]]")
        assertNull(l!!.title)
        assertTrue(l.isExternal)
        assertFalse(l.isMedia)
        assertFalse(l.isPassage)
    }

    @Test
    fun anonymousHttpLink() {
        val l = Link.parseLink("[[http://www.example.com]]")
        assertNull(l!!.title)
        assertTrue(l.isExternal)
        assertFalse(l.isMedia)
        assertFalse(l.isPassage)
    }

    @Test
    fun anonymousShorthandResourceLink() {
        val l = Link.parseLink("[[language/project]]")
        assertNull(l!!.title)
        assertEquals("language", l.language)
        assertEquals("project", l.project)
        assertEquals("project", l.resource)
        assertNull(l.arguments)
        assertFalse(l.isExternal)
        assertFalse(l.isMedia)
        assertFalse(l.isPassage)
    }

    @Test
    fun anonymousAbbreviatedLink() {
        val l = Link.parseLink("[[slug]]")
        assertNull(l!!.title)
        assertNull(l.language)
        assertNull(l.project)
        assertNull(l.resource)
        assertEquals("slug", l.arguments)
        assertFalse(l.isExternal)
        assertFalse(l.isMedia)
        assertFalse(l.isPassage)
    }

    @Test
    fun anonymousAnyLanguageLink() {
        val l = Link.parseLink("[[//project/resource/args]]")
        assertNull(l!!.title)
        assertNull(l.language)
        assertEquals("project", l.project)
        assertEquals("resource", l.resource)
        assertEquals("args", l.arguments)
        assertFalse(l.isExternal)
        assertFalse(l.isMedia)
        assertFalse(l.isPassage)
    }

    @Test
    fun anonymousAnyLanguageNoArgsLink() {
        val l = Link.parseLink("[[//project/resource]]")
        assertNull(l!!.title)
        assertNull(l.language)
        assertEquals("project", l.project)
        assertEquals("resource", l.resource)
        assertNull(l.arguments)
        assertFalse(l.isExternal)
        assertFalse(l.isMedia)
        assertFalse(l.isPassage)
    }

    @Test
    fun titledContainerLink() {
        val l = Link.parseLink("[Link Title](language/project/resource)")
        assertEquals("Link Title", l!!.title)
        assertEquals("language", l.language)
        assertEquals("project", l.project)
        assertEquals("resource", l.resource)
        assertNull(l.arguments)
        assertFalse(l.isExternal)
        assertFalse(l.isMedia)
        assertFalse(l.isPassage)
    }

    @Test
    fun titledHttpsLink() {
        val l = Link.parseLink("[Link Title](https://www.example.com)")
        assertEquals("Link Title", l!!.title)
        assertTrue(l.isExternal)
        assertFalse(l.isMedia)
        assertFalse(l.isPassage)
    }

    @Test
    fun titledHttpLink() {
        val l = Link.parseLink("[Link Title](http://www.example.com)")
        assertEquals("Link Title", l!!.title)
        assertTrue(l.isExternal)
        assertFalse(l.isMedia)
        assertFalse(l.isPassage)
    }

    @Test
    fun titledShorthandResourceLink() {
        val l = Link.parseLink("[Link Title](language/project)")
        assertEquals("Link Title", l!!.title)
        assertEquals("language", l.language)
        assertEquals("project", l.project)
        assertEquals("project", l.resource)
        assertNull(l.arguments)
        assertFalse(l.isExternal)
        assertFalse(l.isMedia)
        assertFalse(l.isPassage)
    }

    @Test
    fun titledAbbreviatedLink() {
        val l = Link.parseLink("[Link Title](slug)")
        assertEquals("Link Title", l!!.title)
        assertNull(l.language)
        assertNull(l.project)
        assertNull(l.resource)
        assertEquals("slug", l.arguments)
        assertFalse(l.isExternal)
        assertFalse(l.isMedia)
        assertFalse(l.isPassage)
    }

    @Test
    fun titledAnyLanguageLink() {
        val l = Link.parseLink("[Link Title](//project/resource/args)")
        assertEquals("Link Title", l!!.title)
        assertNull(l.language)
        assertEquals("project", l.project)
        assertEquals("resource", l.resource)
        assertEquals("args", l.arguments)
        assertFalse(l.isExternal)
        assertFalse(l.isMedia)
        assertFalse(l.isPassage)
    }

    @Test
    fun titledAnyLanguageNoArgsLink() {
        val l = Link.parseLink("[Link Title](//project/resource)")
        assertEquals("Link Title", l!!.title)
        assertNull(l.language)
        assertEquals("project", l.project)
        assertEquals("resource", l.resource)
        assertNull(l.arguments)
        assertFalse(l.isExternal)
        assertFalse(l.isMedia)
        assertFalse(l.isPassage)
    }

    @Test
    fun titledMediaLink() {
        val l = Link.parseLink("[Link Title](image:language/project/resource)")
        assertEquals("Link Title", l!!.title)
        assertEquals("language", l.language)
        assertEquals("project", l.project)
        assertEquals("resource", l.resource)
        assertNull(l.arguments)
        assertEquals("image", l.protocol)
        assertFalse(l.isExternal)
        assertTrue(l.isMedia)
        assertFalse(l.isPassage)
    }

    @Test
    fun titledMediaAltLink() {
        val l = Link.parseLink("[Link Title](image:/language/project/resource)")
        assertEquals("Link Title", l!!.title)
        assertEquals("language", l.language)
        assertEquals("project", l.project)
        assertEquals("resource", l.resource)
        assertNull(l.arguments)
        assertEquals("image", l.protocol)
        assertFalse(l.isExternal)
        assertTrue(l.isMedia)
        assertFalse(l.isPassage)
    }

    @Test
    fun titledMediaAnyLanguageLink() {
        val l = Link.parseLink("[Link Title](image://project/resource)")
        assertEquals("Link Title", l!!.title)
        assertNull(l.language)
        assertEquals("project", l.project)
        assertEquals("resource", l.resource)
        assertNull(l.arguments)
        assertEquals("image", l.protocol)
        assertFalse(l.isExternal)
        assertTrue(l.isMedia)
        assertFalse(l.isPassage)
    }
}
