package org.bibletranslationtools.resourcecontainer

/**
 * This utility compares variable length semver styled strings
 * 0.1.0, 10.0.1, 1.0, 1, 1.2.3.4
 *
 * All non-numeric characters will be removed. e.g. v1.0 will become 1.0
 * 1.0-alpha.1 will become 1.0.1
 */
object Semver {

    fun gt(v1: String, v2: String) = compare(v1, v2) == 1
    fun lt(v1: String, v2: String) = compare(v1, v2) == -1
    fun eq(v1: String, v2: String) = compare(v1, v2) == 0

    fun compare(v1: String, v2: String): Int {
        val ver1 = Version(v1)
        val ver2 = Version(v2)

        for (i in 0 until maxOf(ver1.size, ver2.size)) {
            if (ver1.isWild(i) || ver2.isWild(i)) continue
            if (ver1[i] > ver2[i]) return 1
            if (ver1[i] < ver2[i]) return -1
        }
        return 0
    }

    private class Version(v: String) {
        private val slices = v.split(".")

        val size get() = slices.size

        operator fun get(index: Int): Int =
            if (index in slices.indices) clean(slices[index]).toIntOrNull() ?: 0
            else 0

        fun isWild(index: Int): Boolean {
            if (slices.isEmpty()) return false
            val clamped = index.coerceIn(0, slices.lastIndex)
            return clean(slices[clamped]) == "*"
        }

        private fun clean(value: String): String =
            value.replace(Regex("[^\\d*]"), "").trim().ifEmpty { "0" }
    }
}
