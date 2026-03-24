plugins {
  `java-library`

  id("com.gradleup.shadow")

  kotlin("jvm")
}

val javaVersion: String = property("java.version").toString()
val minecraftVersion: String = property("minecraft.version").toString()

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  }
}

dependencies {
  compileOnly("org.purpurmc.purpur:purpur-api:${minecraftVersion}-R0.1-SNAPSHOT")
  implementation("commons-io:commons-io:2.21.0")
}

tasks {
  jar {
    enabled = false
  }

  processResources {
    filesMatching("paper-plugin.yml") {
      expand(
        "rootProjectVersion" to rootProject.version.toString(),
        "rootProjectName" to rootProject.name,
        "minecraftVersion" to minecraftVersion
      )
    }
  }

  shadowJar {
    archiveClassifier.set("")
    archiveBaseName.set(rootProject.name)
    archiveVersion.set("${rootProject.version}")

    dependencies {
      exclude(dependency("org.jetbrains.kotlin:kotlin-reflect"))
      exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
      exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk7"))
      exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8"))
    }

    minimize()
  }

  val rootPrepare = rootProject.tasks.findByName("prepareKotlinBuildScriptModel")
  if (rootPrepare != null) {
    register("prepareKotlinBuildScriptModel") {
      dependsOn(rootPrepare)
      group = rootPrepare.group
      description = rootPrepare.description
    }
  }
}
