package org.bibletranslationtools.resourcecontainer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface Content {

    @Serializable
    data class Book(
        val chapters: List<Chapter>,
        @SerialName("date_modified")
        val modifiedAt: Int
    ) : Content {

        @Serializable
        data class Chapter(
            val number: String,
            val ref: String,
            val title: String,
            val frames: List<Frame>
        )

        @Serializable
        data class Frame(
            val id: String,
            @SerialName("lastvs")
            val lastVerse: String,
            val format: String,
            val img: String,
            val text: String
        )
    }

    @Serializable
    data class Notes(
        val frames: List<Frame>
    ) : Content {

        @Serializable
        data class Frame(
            val id: String,
            @SerialName("tn")
            val notes: List<Note>
        )

        @Serializable
        data class Note(
            val ref: String,
            val text: String
        )
    }

    @Serializable
    data class Questions(
        val chapters: List<Chapter>
    ) : Content {

        @Serializable
        data class Chapter(
            val id: String,
            @SerialName("cq")
            val questions: List<Question>
        )

        @Serializable
        data class Question(
            @SerialName("q")
            val question: String,
            @SerialName("a")
            val answer: String,
            val ref: List<String>
        )
    }

    @Serializable
    data class Words(
        val words: List<Word>
    ) : Content {

        @Serializable
        data class Word(
            val id: String,
            val term: String,
            @SerialName("def_title")
            val title: String?,
            val def: String,
            val sub: String,
            @SerialName("cf")
            val seeAlso: List<String>,
            @SerialName("ex")
            val examples: List<Example>,
            val aliases: List<String>
        )

        @Serializable
        data class Example(
            val ref: String
        )
    }

    @Serializable
    data class Manual(
        val articles: List<Article>,
        val toc: String
    ) : Content {

        @Serializable
        data class Article(
            val id: String,
            val title: String,
            val question: String,
            val text: String,
            val recommend: List<String>?,
            val depend: List<String>?
        )
    }
}




