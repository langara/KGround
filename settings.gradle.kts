@file:Suppress("UnstableApiUsage")

rootProject.name = "KommandLine"


// Careful with auto publishing fails/stack traces
val buildScanPublishingAllowed =
  System.getenv("GITHUB_ACTIONS") == "true"
  // true
  // false


// gradle.logSomeEventsToFile(rootProject.projectDir.toOkioPath() / "my.gradle.log")

// UreRA|>".*/Deps\.kt"~~>"../DepsKt"<|
// region [My Settings Stuff]

pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }

  val depsDir = File(rootDir, "../DepsKt").normalize()
  val depsInclude =
    // depsDir.exists()
    false
  if (depsInclude) {
    logger.warn("Including local build $depsDir")
    includeBuild(depsDir)
  }
}

plugins {
  id("pl.mareklangiewicz.deps.settings") version "0.2.99" // https://plugins.gradle.org/search?term=mareklangiewicz
  id("com.gradle.develocity") version "3.17.2" // https://docs.gradle.com/enterprise/gradle-plugin/
}

develocity {
  buildScan {
    termsOfUseUrl = "https://gradle.com/terms-of-service"
    termsOfUseAgree = "yes"
    publishing.onlyIf { buildScanPublishingAllowed && it.buildResult.failures.isNotEmpty() }
  }
}

// endregion [My Settings Stuff]

include(":kommandline")
include(":kommandsamples")
include(":kommandjupyter")

val kgroundDir = File(rootDir, "../KGround/kground").normalize()
val kgroundInclude =
  kgroundDir.exists()
  // false
if (kgroundInclude) {
  logger.warn("Adding local kground module.")
  include(":kground")
  project(":kground").projectDir = kgroundDir
}
