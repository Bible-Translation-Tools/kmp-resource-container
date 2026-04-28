package org.bibletranslationtools.resourcecontainer

import org.bibletranslationtools.resourcecontainer.errors.InvalidRCException
import org.bibletranslationtools.resourcecontainer.errors.MissingRCException
import org.bibletranslationtools.resourcecontainer.errors.OutdatedRCException
import org.bibletranslationtools.resourcecontainer.errors.UnsupportedRCException
import java.io.File

class ResourceContainer private constructor(
    val path: File,
    val info: PackageInfo,
    val config: Map<*, *>,
    val toc: Any,
    val language: Language,
    val project: Project,
    val resource: Resource,
) {
    val modifiedAt get() = info.modifiedAt
    val contentMimeType get() = info.contentMimeType
    val slug: String
        get() = ContainerTools.makeSlug(
            language.slug,
            project.slug,
            resource.slug
        )

    companion object {
        const val VERSION = "0.1"
        const val SLUG_DELIMITER = "_"
        const val FILE_EXTENSION = "tsrc"
        const val BASE_MIME_TYPE = "application/tsrc"

        private const val CONTENT_DIR = "content"

        @Throws(Exception::class)
        fun load(containerDirectory: File): ResourceContainer {
            if (!containerDirectory.exists()) {
                throw MissingRCException("The resource container does not exist")
            }

            if (!containerDirectory.isDirectory) {
                throw MissingRCException("Not an open resource container")
            }

            val packageFile = File(containerDirectory, "package.json")
            if (!packageFile.exists()) {
                throw InvalidRCException("Missing package.json file")
            }

            val packageInfo = json.decodeFromString<PackageInfo>(packageFile.readText())

            if (Semver.gt(packageInfo.packageVersion, VERSION)) {
                throw UnsupportedRCException("Unsupported container version")
            }

            if (Semver.lt(packageInfo.packageVersion, VERSION)) {
                throw OutdatedRCException("Outdated container version")
            }

            return build(containerDirectory, packageInfo)
        }

        @Throws(Exception::class)
        fun make(containerDirectory: File, opts: Map<String, Any>): ResourceContainer {
            if (containerDirectory.exists()) {
                throw Exception("Resource container directory already exists")
            }
            throw Exception("Not implemented yet!")
        }

        @Throws(Exception::class)
        fun open(containerArchive: File, containerDirectory: File): ResourceContainer {
            if (containerDirectory.exists()) return load(containerDirectory)
            if (!containerArchive.exists()) {
                throw MissingRCException("Missing resource container")
            }

            val tempFile = File("${containerArchive}.tmp.tar")
            try {
                Archive.bz2Decompress(containerArchive, tempFile)
                containerDirectory.mkdirs()
                Archive.untar(tempFile, containerDirectory)
            } catch (e: Exception) {
                containerDirectory.deleteRecursively()
                throw e
            } finally {
                tempFile.delete()
            }

            return load(containerDirectory)
        }


        @Throws(Exception::class)
        fun close(containerDirectory: File): File {
            if (!containerDirectory.exists()) {
                throw MissingRCException("Missing resource container")
            }

            val tempFile = File("${containerDirectory.absolutePath}.tmp.tar")
            val archive = File("${containerDirectory.absolutePath}.$FILE_EXTENSION")

            try {
                Archive.tar(containerDirectory, tempFile)
                Archive.bz2Compress(tempFile, archive)
            } catch (e: Exception) {
                archive.delete()
                throw e
            } finally {
                tempFile.delete()
            }

            return archive
        }

        private fun build(containerDirectory: File, info: PackageInfo): ResourceContainer {
            val language = Language(
                slug = info.language.slug,
                name = info.language.name,
                direction = info.language.direction,
            )
            val project = Project(
                slug = info.project.slug,
                name = info.project.name,
                sort = info.project.sort,
                icon = info.project.icon,
                description = info.project.description,
                chunksUrl = info.project.chunksUrl,
            ).also { it.languageSlug = language.slug }

            val resource = Resource(
                slug = info.resource.slug,
                name = info.resource.name,
                type = info.resource.type,
                status = Resource.Status(
                    translateMode = info.resource.status.translateMode,
                    checkingLevel = info.resource.status.checkingLevel,
                    version = info.resource.status.version,
                    license = info.resource.status.license,
                    pubDate = info.resource.status.pubDate,
                    comments = info.resource.status.comments
                ),
            ).also { it.projectSlug = project.slug }

            val configFile = File(containerDirectory, "$CONTENT_DIR/config.yml")
            val config: Map<*, *> = if (configFile.exists()) {
                runCatching { yaml.readValue(configFile, Map::class.java) }
                    .getOrDefault(emptyMap<Any, Any>())
            } else emptyMap<Any, Any>()

            val tocFile = File(containerDirectory, "$CONTENT_DIR/toc.yml")
            val toc: Map<*, *> = if (tocFile.exists()) {
                runCatching { yaml.readValue(tocFile, Map::class.java) }
                    .getOrDefault(emptyMap<Any, Any>())
            } else emptyMap<Any, Any>()

            return ResourceContainer(
                path = containerDirectory,
                info = info,
                config = config,
                toc = toc,
                language = language,
                project = project,
                resource = resource,
            )
        }
    }

    fun chapters(): List<String> =
        File(path, CONTENT_DIR).listFiles { file ->
            file.isDirectory && file.name != "config.yml" && file.name != "toc.yml"
        }?.map { it.name } ?: emptyList()

    fun chunks(chapterSlug: String): List<String> =
        File(File(path, CONTENT_DIR), chapterSlug)
            .listFiles()
            ?.map { it.nameWithoutExtension }
            ?: emptyList()

    fun readChunk(chapterSlug: String, chunkSlug: String): String {
        val contentDir = File(path, CONTENT_DIR)
        val chapterDir = File(contentDir, chapterSlug)
        val chunkFile = File(chapterDir, "$chunkSlug.${chunkExt()}")
        return if (chunkFile.exists() && chunkFile.isFile) {
            runCatching { chunkFile.readText() }.getOrDefault("")
        } else ""
    }

    private fun chunkExt(): String = when (contentMimeType) {
        "text/usx" -> "usx"
        "text/usfm" -> "usfm"
        "text/markdown" -> "md"
        else -> "txt"
    }
}
