pluginManagement {
  repositories {
    gradlePluginPortal()

    mavenLocal()
    mavenCentral()

    maven {
      name = "purpurmc"
      url = uri("https://repo.purpurmc.org/snapshots/")
    }
  }

  plugins {
    id("com.diffplug.spotless") version providers.gradleProperty("spotless.version").get()
    id("com.gradleup.shadow") version providers.gradleProperty("shadow.version").get()

    kotlin("jvm") version providers.gradleProperty("kotlin.version").get()
  }
}

dependencyResolutionManagement {

  @Suppress("UnstableApiUsage")
  repositories {
    mavenLocal()
    mavenCentral()

    maven {
      name = "purpurmc"
      url = uri("https://repo.purpurmc.org/snapshots/")
    }
  }
}

include("core")
