package org.bibletranslationtools.resourcecontainer

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PackageInfo(
    @SerialName("package_version")
    val packageVersion: String,
    @SerialName("modified_at")
    val modifiedAt: Int,
    @SerialName("content_mime_type")
    val contentMimeType: String,
    @SerialName("language")
    val language: Language,
    @SerialName("project")
    val project: Project,
    @SerialName("resource")
    val resource: Resource,
    @SerialName("chunk_status")
    val chunkStatus: List<String> = emptyList(),
)