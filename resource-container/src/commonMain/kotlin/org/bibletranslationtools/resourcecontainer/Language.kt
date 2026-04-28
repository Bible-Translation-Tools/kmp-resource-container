package org.bibletranslationtools.resourcecontainer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Language(
    @SerialName("slug")
    val slug: String,
    @SerialName("name")
    val name: String,
    @SerialName("direction")
    val direction: String
) {

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is String -> slug.equals(other, ignoreCase = true)
            is Language -> slug.equals(other.slug, ignoreCase = true)
            else -> false
        }
    }

    override fun hashCode(): Int {
        var result = slug.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + direction.hashCode()
        return result
    }
}