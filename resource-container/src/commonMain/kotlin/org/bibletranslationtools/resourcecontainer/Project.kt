package org.bibletranslationtools.resourcecontainer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Project(
    @SerialName("slug")
    val slug: String,
    @SerialName("name")
    val name: String,
    @SerialName("sort")
    val sort: Int,
    @SerialName("icon")
    val icon: String = "",
    @SerialName("desc")
    val description: String = "",
    @SerialName("chunks_url")
    val chunksUrl: String = "",
    @SerialName("category_slug")
    val categorySlug: String? = null,
    @SerialName("categories")
    val categories: List<String> = emptyList()
) {
    /**
     * The language this project belongs to.
     * This is a convenience property, not serialized.
     */
    @Transient
    var languageSlug: String = ""
}
