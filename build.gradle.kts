plugins {
  `java-library`
  `maven-publish`

  id("com.diffplug.spotless")
  id("com.gradleup.shadow")
}

group = "com.unityrealms.server.modelengine"
version = property("project.version").toString()

spotless {
  java {
    licenseHeaderFile(rootProject.file("LICENSE_HEADER"))

    target(
      "src/main/java/**/*.java",
      "src/test/java/**/*.java"
    )
  }

  kotlin {
    licenseHeaderFile(rootProject.file("LICENSE_HEADER"))

    target(
      "src/main/kotlin/**/*.kt",
      "src/test/kotlin/**/*.kt"
    )
  }
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      artifact(tasks.named("shadowJar").get())

      val javadocJar by tasks.registering(Jar::class) {
        archiveClassifier.set("javadoc")

        from(tasks.named("javadoc"))
      }

      val sourcesJar by tasks.registering(Jar::class) {
        archiveClassifier.set("sources")

        from(sourceSets.main.get().allSource)
      }

      artifact(javadocJar)
      artifact(sourcesJar)
    }
  }
}
