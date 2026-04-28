package org.bibletranslationtools.resourcecontainer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ContainerToolsTest {

    // region isInteger

    @Test
    fun isInteger_trueForNumericStrings() {
        assertTrue(ContainerTools.isInteger("0"))
        assertTrue(ContainerTools.isInteger("1"))
        assertTrue(ContainerTools.isInteger("123"))
        assertTrue(ContainerTools.isInteger("-1"))
    }

    @Test
    fun isInteger_falseForNonNumericStrings() {
        assertFalse(ContainerTools.isInteger(""))
        assertFalse(ContainerTools.isInteger("abc"))
        assertFalse(ContainerTools.isInteger("12a"))
        assertFalse(ContainerTools.isInteger("1.5"))
    }

    // endregion

    // region makeSlug / explodeSlug

    @Test
    fun makeSlug_joinsPartsWithDelimiter() {
        assertEquals("en_gen_ulb", ContainerTools.makeSlug("en", "gen", "ulb"))
        assertEquals("ar_mat_tit", ContainerTools.makeSlug("ar", "mat", "tit"))
    }

    @Test
    fun makeSlug_throwsOnEmptyLanguageSlug() {
        assertFailsWith<IllegalArgumentException> { ContainerTools.makeSlug("", "gen", "ulb") }
    }

    @Test
    fun makeSlug_throwsOnEmptyProjectSlug() {
        assertFailsWith<IllegalArgumentException> { ContainerTools.makeSlug("en", "", "ulb") }
    }

    @Test
    fun makeSlug_throwsOnEmptyResourceSlug() {
        assertFailsWith<IllegalArgumentException> { ContainerTools.makeSlug("en", "gen", "") }
    }

    @Test
    fun explodeSlug_splitsByDelimiter() {
        assertEquals(listOf("en", "gen", "ulb"), ContainerTools.explodeSlug("en_gen_ulb"))
    }

    @Test
    fun explodeSlug_singlePartReturnsListOfOne() {
        assertEquals(listOf("en"), ContainerTools.explodeSlug("en"))
    }

    // endregion

    // region mimeToType / typeToMime

    @Test
    fun mimeToType_extractsTypeSuffix() {
        assertEquals("book", ContainerTools.mimeToType("application/tsrc+book"))
        assertEquals("help", ContainerTools.mimeToType("application/tsrc+help"))
        assertEquals("dict", ContainerTools.mimeToType("application/tsrc+dict"))
    }

    @Test
    fun typeToMime_prependsBaseMimeType() {
        assertEquals("application/tsrc+book", ContainerTools.typeToMime("book"))
        assertEquals("application/tsrc+help", ContainerTools.typeToMime("help"))
    }

    // endregion

    // region decodeContent

    @Test
    fun decodeContent_book() {
        val jsonStr = """
            {
              "chapters": [
                {
                  "number": "1",
                  "ref": "",
                  "title": "Genesis 1",
                  "frames": [
                    {
                      "id": "01-01",
                      "lastvs": "5",
                      "format": "md",
                      "img": "",
                      "text": "In the beginning"
                    }
                  ]
                }
              ],
              "date_modified": 20230101
            }
        """.trimIndent()

        val content = ContainerTools.decodeContent(jsonStr, "book", "gen")

        assertIs<Content.Book>(content)
        assertEquals(1, content.chapters.size)
        assertEquals("1", content.chapters[0].number)
        assertEquals(1, content.chapters[0].frames.size)
        assertEquals("In the beginning", content.chapters[0].frames[0].text)
    }

    @Test
    fun decodeContent_translationNotes() {
        val jsonStr = """
            [
              {
                "id": "01-01",
                "tn": [
                  {
                    "ref": "General Information:",
                    "text": "Some note text"
                  }
                ]
              }
            ]
        """.trimIndent()

        val content = ContainerTools.decodeContent(jsonStr, "help", "tn")

        assertIs<Content.Notes>(content)
        assertEquals(1, content.frames.size)
        assertEquals("01-01", content.frames[0].id)
        assertEquals("General Information:", content.frames[0].notes[0].ref)
    }

    @Test
    fun decodeContent_translationQuestions() {
        val jsonStr = """
            [
              {
                "id": "01",
                "cq": [
                  {
                    "q": "Who created the heavens?",
                    "a": "God",
                    "ref": ["01-01"]
                  }
                ]
              }
            ]
        """.trimIndent()

        val content = ContainerTools.decodeContent(jsonStr, "help", "tq")

        assertIs<Content.Questions>(content)
        assertEquals(1, content.chapters.size)
        assertEquals("01", content.chapters[0].id)
        assertEquals("God", content.chapters[0].questions[0].answer)
    }

    @Test
    fun decodeContent_words() {
        val jsonStr = """
            [
              {
                "id": "god",
                "term": "God",
                "def_title": null,
                "def": "The creator.",
                "sub": "",
                "cf": [],
                "ex": [],
                "aliases": []
              }
            ]
        """.trimIndent()

        val content = ContainerTools.decodeContent(jsonStr, "dict", "tw")

        assertIs<Content.Words>(content)
        assertEquals(1, content.words.size)
        assertEquals("god", content.words[0].id)
        assertEquals("God", content.words[0].term)
    }

    @Test
    fun decodeContent_manual() {
        val jsonStr = """
            {
              "articles": [
                {
                  "id": "intro",
                  "title": "Introduction",
                  "question": "What is this?",
                  "text": "This is an introduction.",
                  "recommend": null,
                  "depend": null
                }
              ],
              "toc": ""
            }
        """.trimIndent()

        val content = ContainerTools.decodeContent(jsonStr, "man", "ta")

        assertIs<Content.Manual>(content)
        assertEquals(1, content.articles.size)
        assertEquals("intro", content.articles[0].id)
        assertEquals("Introduction", content.articles[0].title)
    }

    @Test
    fun decodeContent_throwsForUnsupportedType() {
        assertFailsWith<Exception> {
            ContainerTools.decodeContent("{}", "unknown", "xyz")
        }
    }

    // endregion

    // region decodeWordAssignments

    @Test
    fun decodeWordAssignments_parsesCorrectly() {
        val jsonStr = """
            {
              "chapters": [
                {
                  "id": "01",
                  "frames": [
                    {
                      "id": "01",
                      "items": [
                        {"id": "god"}
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val assignments = ContainerTools.decodeWordAssignments(jsonStr)

        assertEquals(1, assignments.chapters.size)
        assertEquals("01", assignments.chapters[0].id)
        assertEquals(1, assignments.chapters[0].frames.size)
        assertEquals("god", assignments.chapters[0].frames[0].items[0].id)
    }

    // endregion
}
