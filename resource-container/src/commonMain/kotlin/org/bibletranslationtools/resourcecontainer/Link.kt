package org.bibletranslationtools.resourcecontainer


/**
 * Represents a link to a resource container
 */
data class Link(
    val title: String?,
    val url: String?,
    val resource: String?,
    val project: String?,
    val language: String?,
    val arguments: String?,
    val protocol: String?,
    val chapter: String?,
    val chunk: String?,
    val lastChunk: String?
) {

    // External link constructor
    constructor(title: String?, url: String) : this(
        title = title,
        url = url,
        protocol = null,
        resource = null,
        project = null,
        chapter = null,
        chunk = null,
        lastChunk = null,
        arguments = null,
        language = null
    )

    // Resource container link constructor
    constructor(
        title: String?,
        protocol: String?,
        language: String?,
        project: String?,
        resource: String?,
        arguments: String?,
        chapter: String?,
        chunk: String?,
        lastChunk: String?
    ) : this(
        title = title,
        url = null,
        protocol = protocol,
        language = language,
        project = project,
        resource = resource,
        arguments = arguments,
        chapter = chapter,
        chunk = chunk,
        lastChunk = lastChunk
    )

    val isExternal get() = url != null

    val isMedia get() = protocol != null

    val isPassage get() = chapter != null && chunk != null

    fun passageTitle(): String? {
        if (!isPassage) return null
        val tail = if (lastChunk != null) "-${formatNumber(lastChunk)}" else ""
        return "${formatNumber(chapter!!)}:${formatNumber(chunk!!)}$tail"
    }

    private fun formatNumber(value: String): String =
        value.toIntOrNull()?.toString() ?: value.trim().lowercase()

    companion object {
        private val ANONYMOUS_PATTERN = Regex("""\[\[([^]]*)]]""", RegexOption.DOT_MATCHES_ALL)
        private val TITLED_PATTERN = Regex("""\[([^]]*)]\(([^)]*)\)""", RegexOption.DOT_MATCHES_ALL)
        private val PROTOCOL_PATTERN = Regex("""^((\w+):)?/?(.*)""", RegexOption.DOT_MATCHES_ALL)

        @Throws(Exception::class)
        fun parseLink(link: String): Link? {
            var linkTitle: String? = null
            var linkPath: String = link

            val anonymousMatches = ANONYMOUS_PATTERN.findAll(link).toList()
            if (anonymousMatches.size > 1) throw Exception("Invalid link! Multiple links found")
            anonymousMatches.firstOrNull()?.let { linkPath = it.groupValues[1].lowercase() }

            val titledMatches = TITLED_PATTERN.findAll(link).toList()
            if (titledMatches.size > 1) throw Exception("Invalid link! Multiple links found")
            titledMatches.firstOrNull()?.let {
                linkTitle = it.groupValues[1]
                linkPath = it.groupValues[2].lowercase()
            }

            return when {
                linkPath.startsWith("http") -> Link(linkTitle, linkPath)
                else -> parseResourceLink(linkTitle, linkPath)
            }
        }

        @Throws(Exception::class)
        private fun parseResourceLink(title: String?, path: String): Link? {
            var remainingPath = path
            var protocol: String? = null
            var language: String? = null
            var project: String? = null
            var resource: String? = null
            var chapter: String? = null
            var chunk: String? = null
            var lastChunk: String? = null
            var arguments: String? = null

            PROTOCOL_PATTERN.find(remainingPath)?.let { m ->
                protocol = m.groupValues[2].takeIf { it.isNotEmpty() }
                remainingPath = m.groupValues[3]
            }

            val components = remainingPath.split("/")

            if (components.size == 1) arguments = components[0]

            if (components.size > 1) {
                language = components[0]
                project = components[1]
            }

            if (components.size > 2) {
                language = components[0]
                project = components[1]
                resource = components[2]

                if (resource.contains(":")) {
                    arguments = resource
                    resource = null
                }
            }

            if (components.size > 3) {
                language = components[0]
                project = components[1]
                resource = components[2]
                arguments = components.subList(3, components.size).joinToString("/")
            }

            chapter = arguments
            if (arguments != null && arguments.contains(":")) {
                val bits = arguments.split(":")
                chapter = bits[0]
                chunk = bits[1]
            }

            if (chunk != null && chunk.contains("-")) {
                val bits = chunk.split("-")
                chunk = bits[0]
                lastChunk = bits[1]
            }

            if (resource == null && project != null) resource = project

            protocol = protocol?.takeIf { it.isNotEmpty() }
            val cleanTitle = title?.takeIf { it.isNotEmpty() }
            language = language?.takeIf { it.isNotEmpty() }
            project = project?.takeIf { it.isNotEmpty() }
            resource = resource?.takeIf { it.isNotEmpty() }
            arguments = arguments?.takeIf { it.isNotEmpty() }
            chapter = chapter?.takeIf { it.isNotEmpty() }
            chunk = chunk?.takeIf { it.isNotEmpty() }
            lastChunk = lastChunk?.takeIf { it.isNotEmpty() }

            if (chunk?.contains(",") == true || lastChunk?.contains(",") == true) {
                throw Exception("Invalid passage link $path")
            }

            return if (project != null && resource != null || arguments != null) {
                Link(cleanTitle, protocol, language, project, resource, arguments, chapter, chunk, lastChunk)
            } else null
        }
    }
}
