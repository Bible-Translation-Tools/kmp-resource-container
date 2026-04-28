package org.bibletranslationtools.resourcecontainer

import kotlinx.serialization.Serializable

@Serializable
data class WordAssignments(
    val chapters: List<Chapter>
) {
    @Serializable
    data class Chapter(
        val id: String,
        val frames: List<Frame>
    )

    @Serializable
    data class Frame(
        val id: String,
        val items: List<Item>
    )

    @Serializable
    data class Item(
        val id: String
    )
}