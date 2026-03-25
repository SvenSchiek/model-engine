package com.unityrealms.server.modelengine.model.resourcepack

/**
 * Converts a string to a valid resource pack file name.
 */
object ResourcePackConverter {

  /**
   * Converts a string to a valid resource pack file name.
   *
   * @param originalInput The original string input to convert.
   *
   * @return The converted string.
   */
  @JvmStatic
  fun convert(originalInput: String): String {
    return originalInput.lowercase().replace("[^a-z0-9._-]".toRegex(), "_")
  }
}
