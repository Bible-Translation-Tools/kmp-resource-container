package org.bibletranslationtools.resourcecontainer

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import java.io.File

object Archive {

    fun bz2Decompress(source: File, dest: File) {
        source.inputStream().buffered().use { fileIn ->
            BZip2CompressorInputStream(fileIn).use { bzIn ->
                dest.outputStream().use { bzIn.copyTo(it) }
            }
        }
    }

    fun bz2Compress(source: File, dest: File) {
        source.inputStream().buffered().use { fileIn ->
            dest.outputStream().buffered().use { fileOut ->
                BZip2CompressorOutputStream(fileOut).use { bzOut ->
                    fileIn.copyTo(bzOut)
                }
            }
        }
    }

    fun untar(source: File, destDir: File) {
        TarArchiveInputStream(source.inputStream().buffered()).use { tin ->
            var entry = tin.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { tin.copyTo(it) }
                }
                entry = tin.nextEntry
            }
        }
    }

    fun tar(sourceDir: File, dest: File) {
        TarArchiveOutputStream(dest.outputStream().buffered()).use { tout ->
            tout.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
            sourceDir.walkTopDown().filter { it != sourceDir }.forEach { file ->
                val entryName = sourceDir.toURI().relativize(file.toURI()).path
                val entry = TarArchiveEntry(file, entryName)
                tout.putArchiveEntry(entry)
                if (file.isFile) {
                    file.inputStream().use { it.copyTo(tout) }
                }
                tout.closeArchiveEntry()
            }
        }
    }
}