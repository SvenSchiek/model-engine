package com.unityrealms.server.modelengine

import org.bukkit.plugin.java.JavaPlugin

/**
 * Main class of the plugin.
 */
class ModelEngine : JavaPlugin() {

  companion object {

    lateinit var javaPlugin: JavaPlugin

    val version: Version
      get() {
        return try {
          Version.fromString(this.javaPlugin.server.version)
        } catch (_: Exception) {
          Version(0, 0, 0)
        }
      }
  }

  /**
   * Called when the plugin is enabled.
   */
  override fun onEnable() {
    javaPlugin = this
  }
}
