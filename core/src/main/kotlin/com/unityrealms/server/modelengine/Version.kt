package com.unityrealms.server.modelengine

/**
 * Simple semantic version class (major.minor.patch) with comparison support.
 */
data class Version(val major: Int, val minor: Int = 0, val patch: Int = 0) : Comparable<Version> {
  override fun compareTo(other: Version): Int {
    if (major != other.major) return major - other.major
    if (minor != other.minor) return minor - other.minor
    return patch - other.patch
  }

  companion object {
    private val REGEX = Regex("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?")

    /**
     * Parse a version string like "1.21.6-R0.1-SNAPSHOT" or "1.21.6".
     */
    fun fromString(versionString: String?): Version {
      if (versionString == null) return Version(0, 0, 0)
      val match = REGEX.find(versionString)
      return if (match != null) {
        val g1 = match.groups[1]?.value?.toIntOrNull() ?: 0
        val g2 = match.groups[2]?.value?.toIntOrNull() ?: 0
        val g3 = match.groups[3]?.value?.toIntOrNull() ?: 0
        Version(g1, g2, g3)
      } else {
        Version(0, 0, 0)
      }
    }
  }
}

