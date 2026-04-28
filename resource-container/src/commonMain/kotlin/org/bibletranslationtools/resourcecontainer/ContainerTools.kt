package org.bibletranslationtools.resourcecontainer

import java.io.File

object ContainerTools {

    private val TAG = ContainerTools::class.java.name
    private val VERSE_REGEX = "<verse\\s+number=\"(\\d+(-\\d+)?)\"\\s+style=\"v\"\\s*/>".toRegex()
    private val LINK_REGEX = "\"\\\\[[^\\\\]*\\\\]\\\\s*\\\\(([^\\\\()]*)\\\\)".toRegex(
        RegexOption.DOT_MATCHES_ALL
    )

    @Throws(Exception::class)
    fun inspect(containerPath: File): PackageInfo {
        if (!containerPath.exists()) {
            throw Exception("The resource container does not exist at ${containerPath.absolutePath}")
        }

        val container = if (containerPath.isFile) {
            val ext = containerPath.extension
            if (ext != ResourceContainer.FILE_EXTENSION) {
                throw Exception("Invalid resource container file extension")
            }
            val containerDir = File(
                containerPath.parentFile,
                "${containerPath.name}.inspect.tmp"
            )
            ResourceContainer.open(containerPath, containerDir)
                .also { containerDir.deleteRecursively() }
        } else {
            ResourceContainer.load(containerPath)
        }

        return container.info
    }

    fun convertResource(
        content: Content,
        info: PackageInfo,
        wordAssignments: WordAssignments? = null,
        directory: File
    ): ResourceContainer {
        val containerArchive = File(
            directory.parentFile,
            directory.name + "." + ResourceContainer.FILE_EXTENSION
        )
        if(containerArchive.exists()) throw Exception("Resource container already exists")

        try {
            directory.deleteRecursively()
            directory.mkdirs()

            // package.json
            val packageJson = File(directory, "package.json")
            val packageInfo = json.encodeToString(info)
            packageJson.writeText(packageInfo)

            // license
            val licence = File(directory, "LICENSE.md")
            licence.writeText(info.resource.status.license)

            // content
            val contentDir = File(directory, "content")
            contentDir.mkdirs()

            val config = mutableMapOf<String, Any>()
            val toc = mutableListOf<Any>()

            val chunkExt = if (info.contentMimeType == "text/usx") {
                "usx"
            } else "md"

            // front matter
            if (info.resource.type != "help" && info.resource.type != "dict") {
                val frontDir = File(contentDir, "front")
                frontDir.mkdirs()

                File(frontDir, "title.$chunkExt").writeText(info.project.name.trim())
            }

            // main content
            when (content) {
                is Content.Book -> {
                    val contentConfig = mutableMapOf<String, Any>()
                    config["content"] = contentConfig

                    if (info.project.slug == "obs") {
                        // add obs images
                        val mediaConfig = mutableMapOf<String, Any>()
                        mediaConfig["mime_type"] = "image/jpg";
                        mediaConfig["size"] = 37620940;
                        mediaConfig["url"] = "https://api.unfoldingword.org/obs/jpg/1/en/obs-images-360px.zip";
                        config["media"] = mediaConfig;
                    }

                    content.chapters.forEach { chapter ->
                        val chapterConfig = mutableMapOf<String, Any>()
                        val chapterNumber = normalizeSlug(chapter.number)
                        val chapterDir = File(contentDir, chapterNumber)
                        chapterDir.mkdirs()

                        // chapter title
                        val title = chapter.title.ifEmpty {
                            localizeChapterTitle(info.language.slug, chapterNumber)
                        }
                        File(chapterDir, "title.$chunkExt").writeText(title)

                        // frames
                        chapter.frames.forEach { frame ->
                            var frameSlug = normalizeSlug(frame.id.split("-")[1].trim())

                            // fix for chunk 00.txt bug
                            if (frameSlug == "00") {
                                // TRICKY: the JSON encoding escapes double quotes so we need to un-escape them.
                                val frameText = frame.text.replace("\\\"", "\"")
                                val match = VERSE_REGEX.find(frameText)
                                if (match != null) {
                                    // TRICKY: verses can be num-num
                                    val firstVerseRange = match.groupValues[1]
                                    frameSlug = normalizeSlug(firstVerseRange.substringBefore("-"))
                                }
                            }

                            // build chunk config
                            val questions = mutableListOf<String>()
                            val notes = mutableListOf<String>()
                            val images = mutableListOf<String>()

                            // TODO: 9/13/16 add questions, notes, and images to the config for the chunk
                            val words = wordAssignments?.chapters
                                ?.firstOrNull {
                                    it.id == chapterNumber || normalizeSlug(it.id) == chapterNumber
                                }
                                ?.frames?.firstOrNull {
                                    it.id == frameSlug || normalizeSlug(it.id) == frameSlug
                                }
                                ?.items?.map {
                                    val twProjSlug = if (info.project.slug == "obs") {
                                        "bible-obs"
                                    } else "bible"
                                    "//$twProjSlug/tw/${it.id}"
                                } ?: emptyList()

                            if(words.isNotEmpty()) {
                                chapterConfig[frameSlug] = mapOf<String, Any>(
                                    "words" to words
                                )
                            }

                            File(chapterDir, "$frameSlug.$chunkExt").writeText(frame.text)
                        }

                        // chapter reference
                        if (chapter.ref.isNotEmpty()) {
                            File(chapterDir, "title.$chunkExt").writeText(chapter.title)
                        }
                        if (chapterConfig.isNotEmpty()) {
                            contentConfig[chapterNumber] = chapterConfig
                        }
                    }
                }
                is Content.Notes -> {
                    content.frames.forEach { chunk ->
                        val slugs = chunk.id.split("-")
                        if (slugs.size != 2) return@forEach

                        val chapterSlug = normalizeSlug(slugs[0])
                        val chunkSlug = normalizeSlug(slugs[1])


                        // fix for chunk 00.txt bug
                        if (chunkSlug == "00") return@forEach

                        val chapterDir = File(contentDir, chapterSlug)
                        chapterDir.mkdirs()

                        val body = chunk.notes.joinToString("") {
                            "\n\n#" + it.ref + "\n\n" + it.text
                        }.trim()

                        if (body.isNotEmpty()) {
                            File(chapterDir, "$chunkSlug.$chunkExt").writeText(body)
                        }
                    }
                }
                is Content.Questions -> {
                    content.chapters.forEach { chapter ->
                        val chunks = mutableMapOf<String, String>()

                        chapter.questions.forEach { question ->
                            val text = "\n\n#${question.question}\n\n${question.answer}"

                            question.ref.forEach { ref ->
                                val slugs = ref.split("-")
                                if (slugs.size != 2) return@forEach
                                val chunkId = normalizeSlug(slugs[1])

                                chunks[chunkId] = (chunks[chunkId]?.trim() ?: "") + text
                            }
                        }

                        val chapterDir = File(contentDir, chapter.id)

                        chunks.forEach { (id, text) ->
                            val chunkFile = File(chapterDir, "$id.$chunkExt")
                            chunkFile.parentFile?.mkdirs()
                            chunkFile.writeText(text)
                        }
                    }
                }
                is Content.Words -> {
                    content.words.forEach { word ->
                        val wordDir = File(contentDir, word.id)
                        wordDir.mkdirs()

                        val body = "#" + word.term + "\n\n" + word.def
                        File(wordDir, "01.$chunkExt").writeText(body)

                        val wordConfig = mutableMapOf<String, Any?>()
                        wordConfig["def_title"] = word.title

                        if (word.seeAlso.isNotEmpty()) {
                            val seeAlso = mutableListOf<String>()
                            wordConfig["see_also"] = seeAlso
                            word.seeAlso.forEach { cf ->
                                // some id's have the title attached to it like: eve|Eve
                                val parts = cf.split("|")
                                val id = parts[0].lowercase()
                                if (id !in seeAlso) {
                                    seeAlso.add(id)
                                }
                            }
                        }

                        if (word.aliases.isNotEmpty()) {
                            val aliases = mutableListOf<String>()
                            wordConfig["aliases"] = aliases
                            word.aliases.forEach { alias ->
                                val parts = alias.split(",")
                                parts.forEach {
                                    aliases.add(it.trim())
                                }
                            }
                        }

                        if (word.examples.isNotEmpty()) {
                            val examples = mutableListOf<String>()
                            wordConfig["examples"] = examples
                            word.examples.forEach { example ->
                                examples.add(example.ref)
                            }
                        }

                        config[word.id] = wordConfig
                    }
                }
                is Content.Manual -> {
                    val tocConfig = mutableMapOf<String, Any>()
                    val manConfig = mutableMapOf<String, Any>()

                    config["content"] = manConfig

                    content.articles.forEach { article ->
                        val articleConfig = mutableMapOf<String, Any>()
                        val recommended = mutableListOf<String>()
                        val dependencies = mutableListOf<String>()

                        // fix the ids
                        val slug = article.id.replace("\\_", "-")
                        article.recommend?.let { recommend ->
                            recommend.forEach { rec ->
                                recommended.add(rec.replace("\\_", "-"))
                            }
                        }
                        article.depend?.let { depend ->
                            depend.forEach { dep ->
                                dependencies.add(dep.replace("\\_", "-"))
                            }
                        }

                        val articleDir = File(contentDir, slug)
                        articleDir.mkdirs()

                        // article title
                        File(articleDir, "title.$chunkExt").writeText(article.title)

                        // article sub-title
                        File(articleDir, "sub-title.$chunkExt").writeText(article.question)

                        // article body
                        File(articleDir, "01.$chunkExt").writeText(article.text)

                        // only non-empty config
                        if (recommended.isNotEmpty() || dependencies.isNotEmpty()) {
                            articleConfig["recommended"] = recommended
                            articleConfig["dependencies"] = dependencies
                            manConfig[slug] = articleConfig
                        }

                        val articleTOC = mutableMapOf<String, Any>()
                        val chunkTOC = mutableListOf<String>()

                        chunkTOC.add("title")
                        chunkTOC.add("sub-title")
                        chunkTOC.add("01")

                        articleTOC["chapter"] = slug
                        articleTOC["chunks"] = chunkTOC

                        tocConfig[article.id] = articleTOC

                        // build toc from what we see in the api
                        LINK_REGEX.findAll(content.toc).forEach { match ->
                            val key = match.groupValues[1]
                            tocConfig[key]?.let { toc.add(it) }
                        }
                    }
                }
            }

            // write config
            val configFile = File(contentDir, "config.yml")
            yaml.writeValue(configFile, config)

            // write toc
            if (toc.isNotEmpty()) {
                val tocFile = File(contentDir, "toc.yml")
                yaml.writeValue(tocFile, toc)
            }
        } catch (e: Exception) {
            directory.deleteRecursively()
            throw e
        }

        return ResourceContainer.load(directory)
    }

    fun decodeContent(jsonStr: String, resourceType: String, resourceSlug: String): Content {
        return when (resourceType) {
            "book" -> json.decodeFromString<Content.Book>(jsonStr)
            "help" if resourceSlug == "tn" -> {
                Content.Notes(
                    frames = json.decodeFromString<List<Content.Notes.Frame>>(jsonStr)
                )
            }
            "help" if resourceSlug == "tq" -> {
                Content.Questions(
                    chapters = json.decodeFromString<List<Content.Questions.Chapter>>(jsonStr)
                )
            }
            "dict" -> {
                Content.Words(
                    words = json.decodeFromString<List<Content.Words.Word>>(jsonStr)
                )
            }
            "man" -> json.decodeFromString<Content.Manual>(jsonStr)
            else -> throw Exception("Unsupported resource $resourceSlug")
        }
    }

    fun decodeWordAssignments(jsonStr: String): WordAssignments {
        return json.decodeFromString(jsonStr)
    }

    fun localizeChapterTitle(languageSlug: String, chapterNumber: String): String {
        val translations = mapOf(
            "ar" to "الفصل %",
            "en" to "Chapter %",
            "ru" to "Глава %",
            "hu" to "%. fejezet",
            "sr-Latin" to "Поглавље %",
            "default" to "Chapter %",
        )
        val title = translations[languageSlug] ?: translations["default"]!!
        val num = chapterNumber.toIntOrNull()
        return title.replace("%", num?.toString() ?: chapterNumber)
    }

    fun localizeChapterTitle(languageSlug: String, chapterNumber: Int): String =
        localizeChapterTitle(languageSlug, chapterNumber.toString())

    @Throws(Exception::class)
    fun normalizeSlug(slug: String): String {
        if (slug.isEmpty()) throw Exception("slug cannot be an empty string")
        if (!isInteger(slug)) return slug
        return slug.trimStart('0').padStart(2, '0')
    }

    fun isInteger(s: String): Boolean = s.toIntOrNull() != null

    fun makeSlug(languageSlug: String, projectSlug: String, resourceSlug: String): String {
        require(languageSlug.isNotEmpty()
                && projectSlug.isNotEmpty()
                && resourceSlug.isNotEmpty()) {
            "Invalid resource container slug parameters"
        }
        return "$languageSlug${ResourceContainer.SLUG_DELIMITER}" +
                "$projectSlug${ResourceContainer.SLUG_DELIMITER}$resourceSlug"
    }

    fun explodeSlug(resourceContainerSlug: String): List<String> =
        resourceContainerSlug.split(ResourceContainer.SLUG_DELIMITER)

    fun mimeToType(mimeType: String): String = mimeType.split("+")[1]

    fun typeToMime(resourceType: String): String = "${ResourceContainer.BASE_MIME_TYPE}+$resourceType"
}
