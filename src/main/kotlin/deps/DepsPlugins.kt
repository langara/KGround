package pl.mareklangiewicz.deps

import org.gradle.api.*
import org.gradle.api.initialization.*

class DepsSettingsPlugin : Plugin<Settings> {
    override fun apply(target: Settings) = println("DepsSettingsPlugin.apply(settings for ${target.rootProject.name})")
}

class DepsPlugin : Plugin<Project> {
    override fun apply(target: Project) = println("DepsPlugin.apply(project ${target.name})")
}

