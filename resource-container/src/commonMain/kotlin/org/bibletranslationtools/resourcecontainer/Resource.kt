package org.bibletranslationtools.resourcecontainer

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Resource(
    val slug: String,
    val name: String,
    val type: String,
    val status: Status,
    val imported: Boolean = false
) {
    @Transient
    private val _formats: MutableList<Format> = mutableListOf()

    @Transient
    private val _legacyData: MutableMap<String, Any> = mutableMapOf()

    @Transient
    var projectSlug: String = ""

    @Transient
    val formats: List<Format> = _formats

    @Transient
    var legacyData: Map<String, Any> = _legacyData

    fun hasImportedFormat() = formats.any { it.imported }

    fun addFormat(format: Format) = _formats.add(format)
    fun addLegacyData(key: String, value: Any) = _legacyData.put(key, value)

    @Serializable
    data class Status(
        @SerialName("translate_mode")
        val translateMode: String,
        @SerialName("checking_level")
        val checkingLevel: String,
        val version: String,
        val license: String = "",
        @SerialName("pub_date")
        @Contextual
        val pubDate: String = "",
        val comments: String = "",
        @SerialName("source_translations")
        val sourceTranslations: List<SourceTranslation> = emptyList()
    )

    @Serializable
    data class Format(
        @SerialName("package_version")
        val packageVersion: String,
        @SerialName("mime_type")
        val mimeType: String,
        @SerialName("modified_at")
        val modifiedAt: Int,
        val url: String,
        val imported: Boolean
    )

    @Serializable
    data class SourceTranslation(
        @SerialName("language_slug")
        val languageSlug: String,
        @SerialName("resource_slug")
        val resourceSlug: String,
        val version: String
    )

    companion object {
        const val ULB_SLUG = "ulb"
        const val UDB_SLUG = "udb"
        const val REGULAR_SLUG = "reg"
        const val OBS_SLUG = "obs"
    }
}