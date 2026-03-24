package com.unityrealms.server.modelengine.logger

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * A simple logger that prints to stdout.
 */
object Logger {

  private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

  @Volatile
  var debugEnabled: Boolean = false

  @Volatile
  var useColors: Boolean = true

  private const val RESET = "\u001B[0m"
  private const val RED = "\u001B[31m"
  private const val YELLOW = "\u001B[33m"
  private const val BLUE = "\u001B[34m"
  private const val CYAN = "\u001B[36m"

  /**
   * Formats a log message with timestamp, thread name, and color.
   *
   * @return The formatted log message.
   */
  private fun timestamp(): String = LocalDateTime.now().format(dateTimeFormatter)

  private fun format(level: String, color: String?, message: String): String {
    val thread = Thread.currentThread().name
    val coloredLevel = if (useColors && color != null) "$color$level$RESET" else level
    return $$"${timestamp()} [$thread] $coloredLevel - $message"
  }

  /**
   * Prints a log message with the specified message.
   *
   * @param message The log message to print.
   */
  @JvmStatic
  fun information(message: String) {
    println(this.format("INFORMATION", BLUE, message))
  }

  @JvmStatic
  fun warn(message: String) {
    println(format("WARN", YELLOW, message))
  }

  @JvmStatic
  fun error(message: String) {
    println(format("ERROR", RED, message))
  }

  @JvmStatic
  fun debug(message: String) {
    if (debugEnabled) {
      println(format("DEBUG", CYAN, message))
    }
  }

  @JvmStatic
  fun error(message: String, t: Throwable?) {
    if (t == null) {
      error(message)
    } else {
      println(format("ERROR", RED, "$message\n${'$'}{stackTrace(t)}"))
    }
  }

  @JvmStatic
  fun warn(message: String, t: Throwable?) {
    if (t == null) {
      warn(message)
    } else {
      println(format("WARN", YELLOW, "$message\n${'$'}{stackTrace(t)}"))
    }
  }

  private fun stackTrace(t: Throwable): String {
    val sb = StringBuilder()
    sb.append(t.toString())
    for (el in t.stackTrace) {
      sb.append("\n\tat ").append(el)
    }
    t.cause?.let {
      sb.append("\nCaused by: ").append(it.toString())
    }
    return sb.toString()
  }

  @JvmStatic
  fun setDebug(enabled: Boolean) {
    debugEnabled = enabled
  }

  @JvmStatic
  fun setUseColors(enabled: Boolean) {
    useColors = enabled
  }
}

