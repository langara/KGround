@file:Suppress("DEPRECATION", "unused", "UnusedVariable")

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import pl.mareklangiewicz.defaults.*
import pl.mareklangiewicz.deps.*
import pl.mareklangiewicz.utils.*

plugins {
    plugAll(plugs.AndroLibNoVer, plugs.KotlinAndro, plugs.MavenPublish, plugs.Signing)
}

defaultBuildTemplateForAndroidLib(
    libNamespace = "pl.mareklangiewicz.templateandrolib",
    publishVariant = "debug",
)

dependencies {
    defaultAndroTestDeps(configuration = "androidTestImplementation", withCompose = true)
        // TODO_someday: investigate why "androidTestImplementation" doesn't inherit from "testImplementation"
}

// region [Kotlin Module Build Template]

fun RepositoryHandler.addRepos(settings: LibReposSettings) = with(settings) {
    if (withMavenLocal) mavenLocal()
    if (withMavenCentral) mavenCentral()
    if (withGradle) gradlePluginPortal()
    if (withGoogle) google()
    if (withKotlinx) maven(repos.kotlinx)
    if (withKotlinxHtml) maven(repos.kotlinxHtml)
    if (withComposeJbDev) maven(repos.composeJbDev)
    if (withComposeCompilerAndroidxDev) maven(repos.composeCompilerAndroidxDev)
    if (withKtorEap) maven(repos.ktorEap)
    if (withJitpack) maven(repos.jitpack)
}

fun TaskCollection<Task>.defaultKotlinCompileOptions(
    jvmTargetVer: String = vers.JvmDefaultVer,
    renderInternalDiagnosticNames: Boolean = false,
) = withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = jvmTargetVer
        if (renderInternalDiagnosticNames) freeCompilerArgs = freeCompilerArgs + "-Xrender-internal-diagnostic-names"
        // useful, for example, to suppress some errors when accessing internal code from some library, like:
        // @file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "EXPOSED_PARAMETER_TYPE", "EXPOSED_PROPERTY_TYPE", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")
    }
}

fun TaskCollection<Task>.defaultTestsOptions(
    printStandardStreams: Boolean = true,
    printStackTraces: Boolean = true,
    onJvmUseJUnitPlatform: Boolean = true,
) = withType<AbstractTestTask>().configureEach {
    testLogging {
        showStandardStreams = printStandardStreams
        showStackTraces = printStackTraces
    }
    if (onJvmUseJUnitPlatform) (this as? Test)?.useJUnitPlatform()
}

// Provide artifacts information requited by Maven Central
fun MavenPublication.defaultPOM(lib: LibDetails) = pom {
    name put lib.name
    description put lib.description
    url put lib.githubUrl

    licenses {
        license {
            name put lib.licenceName
            url put lib.licenceUrl
        }
    }
    developers {
        developer {
            id put lib.authorId
            name put lib.authorName
            email put lib.authorEmail
        }
    }
    scm { url put lib.githubUrl }
}

/** See also: root project template-mpp: addDefaultStuffFromSystemEnvs */
fun Project.defaultSigning(
    keyId: String = rootExtString["signing.keyId"],
    key: String = rootExtReadFileUtf8TryOrNull("signing.keyFile") ?: rootExtString["signing.key"],
    password: String = rootExtString["signing.password"],
) = extensions.configure<SigningExtension> {
    useInMemoryPgpKeys(keyId, key, password)
    sign(extensions.getByType<PublishingExtension>().publications)
}

fun Project.defaultPublishing(
    lib: LibDetails,
    readmeFile: File = File(rootDir, "README.md"),
    withSignErrorWorkaround: Boolean = true,
    withPublishingPrintln: Boolean = false, // FIXME_later: enabling brakes gradle android publications
) {

    val readmeJavadocJar by tasks.registering(Jar::class) {
        from(readmeFile) // TODO_maybe: use dokka to create real docs? (but it's not even java..)
        archiveClassifier put "javadoc"
    }

    extensions.configure<PublishingExtension> {

        // We have at least two cases:
        // 1. With plug.KotlinMulti it creates publications automatically (so no need to create here)
        // 2. With plug.KotlinJvm it does not create publications (so we have to create it manually)
        if (plugins.hasPlugin("org.jetbrains.kotlin.jvm")) {
            publications.create<MavenPublication>("jvm") {
                from(components["kotlin"])
            }
        }

        publications.withType<MavenPublication> {
            artifact(readmeJavadocJar)
            // Adding javadoc artifact generates warnings like:
            // Execution optimizations have been disabled for task ':uspek:signJvmPublication'
            // (UPDATE: now it's errors - see workaround below)
            // It looks like a bug in kotlin multiplatform plugin:
            // https://youtrack.jetbrains.com/issue/KT-46466
            // FIXME_someday: Watch the issue.
            // If it's a bug in kotlin multiplatform then remove this comment when it's fixed.
            // Some related bug reports:
            // https://youtrack.jetbrains.com/issue/KT-47936
            // https://github.com/gradle/gradle/issues/17043

            defaultPOM(lib)
        }
    }
    if (withSignErrorWorkaround) tasks.withSignErrorWorkaround() //very much related to comments above too
    if (withPublishingPrintln) tasks.withPublishingPrintln()
}

/*
Hacky workaround for gradle error with signing+publishing on gradle 8.1-rc-1:

FAILURE: Build failed with an exception.

* What went wrong:
A problem was found with the configuration of task ':template-mpp-lib:signJvmPublication' (type 'Sign').
  - Gradle detected a problem with the following location: '/home/marek/code/kotlin/DepsKt/template-mpp/template-mpp-lib/build/libs/template-mpp-lib-0.0.02-javadoc.jar.asc'.

    Reason: Task ':template-mpp-lib:publishJsPublicationToMavenLocal' uses this output of task ':template-mpp-lib:signJvmPublication' without declaring an explicit or implicit dependency. This can lead to incorrect results being produced, depending on what order the tasks are executed.

    Possible solutions:
      1. Declare task ':template-mpp-lib:signJvmPublication' as an input of ':template-mpp-lib:publishJsPublicationToMavenLocal'.
      2. Declare an explicit dependency on ':template-mpp-lib:signJvmPublication' from ':template-mpp-lib:publishJsPublicationToMavenLocal' using Task#dependsOn.
      3. Declare an explicit dependency on ':template-mpp-lib:signJvmPublication' from ':template-mpp-lib:publishJsPublicationToMavenLocal' using Task#mustRunAfter.

    Please refer to https://docs.gradle.org/8.1-rc-1/userguide/validation_problems.html#implicit_dependency for more details about this problem.

 */
fun TaskContainer.withSignErrorWorkaround() =
    withType<AbstractPublishToMaven>().configureEach { dependsOn(withType<Sign>()) }

fun TaskContainer.withPublishingPrintln() = withType<AbstractPublishToMaven>().configureEach {
    val coordinates = publication.run { "$groupId:$artifactId:$version" }
    when (this) {
        is PublishToMavenRepository -> doFirst {
            println("Publishing $coordinates to ${repository.url}")
        }
        is PublishToMavenLocal -> doFirst {
            val localRepo = System.getenv("HOME")!! + "/.m2/repository"
            val localPath = localRepo + publication.run { "/$groupId/$artifactId".replace('.', '/') }
            println("Publishing $coordinates to $localPath")
        }
    }
}

fun Project.defaultBuildTemplateForJvmLib(
    details: LibDetails = rootExtLibDetails,
    withTestJUnit4: Boolean = false,
    withTestJUnit5: Boolean = true,
    withTestUSpekX: Boolean = true,
    addMainDependencies: KotlinDependencyHandler.() -> Unit = {},
) {
    repositories { addRepos(details.settings.repos) }
    defaultGroupAndVerAndDescription(details)

    kotlin {
        sourceSets {
            val main by getting {
                dependencies {
                    addMainDependencies()
                }
            }
            val test by getting {
                dependencies {
                    if (withTestJUnit4) implementation(JUnit.junit)
                    if (withTestJUnit5) implementation(Org.JUnit.Jupiter.junit_jupiter_engine)
                    if (withTestUSpekX) {
                        implementation(Langiewicz.uspekx)
                        if (withTestJUnit4) implementation(Langiewicz.uspekx_junit4)
                        if (withTestJUnit5) implementation(Langiewicz.uspekx_junit5)
                    }
                }
            }
        }
    }

    configurations.checkVerSync()
    tasks.defaultKotlinCompileOptions()
    tasks.defaultTestsOptions(onJvmUseJUnitPlatform = withTestJUnit5)
    if (plugins.hasPlugin("maven-publish")) {
        defaultPublishing(details)
        if (plugins.hasPlugin("signing")) defaultSigning()
        else println("JVM Module ${name}: signing disabled")
    } else println("JVM Module ${name}: publishing (and signing) disabled")
}

// endregion [Kotlin Module Build Template]

// region [Andro Common Build Template]

fun DependencyHandler.defaultAndroDeps(
    configuration: String = "implementation",
    withCompose: Boolean = false,
    withMDC: Boolean = false,
) {
    addAll(
        configuration,
        AndroidX.Core.ktx,
        AndroidX.AppCompat.appcompat,
        AndroidX.Lifecycle.compiler,
        AndroidX.Lifecycle.runtime_ktx,
    )
    if (withCompose) {
        addAllWithVer(
            configuration,
            Vers.ComposeAndro,
            AndroidX.Compose.Ui.ui,
            AndroidX.Compose.Ui.tooling,
            AndroidX.Compose.Ui.tooling_preview,
            AndroidX.Compose.Material.material,
        )
        addAll(
            configuration,
            AndroidX.Activity.compose,
            AndroidX.Compose.Material3.material3,
        )
    }
    if (withMDC) add(configuration, Com.Google.Android.Material.material)
}

fun DependencyHandler.defaultAndroTestDeps(
    configuration: String = "testImplementation",
    withCompose: Boolean = false,
) {
    addAll(
        configuration,
        Kotlin.test_junit.withVer(Vers.Kotlin),
        JUnit.junit, // FIXME_someday: when will android move to JUnit5?
        Langiewicz.uspekx_junit4,
        AndroidX.Test.Espresso.core,
        Com.Google.Truth.truth,
        AndroidX.Test.rules,
        AndroidX.Test.runner,
        AndroidX.Test.Ext.truth,
        AndroidX.Test.Ext.junit,
        Org.Mockito.Kotlin.mockito_kotlin,
    )
    if (withCompose) addAllWithVer(
        configuration,
        vers.ComposeAndro,
        AndroidX.Compose.Ui.test,
        AndroidX.Compose.Ui.test_junit4,
        AndroidX.Compose.Ui.test_manifest,
    )
}

fun MutableSet<String>.defaultAndroExcludedResources() = addAll(
    listOf(
        "**/*.md",
        "**/attach_hotspot_windows.dll",
        "META-INF/licenses/**",
        "META-INF/AL2.0",
        "META-INF/LGPL2.1",
        "META-INF/kotlinx_coroutines_core.version",
    )
)

fun CommonExtension<*, *, *, *, *>.defaultCompileOptions(
    jvmVersion: String = vers.JvmDefaultVer,
) = compileOptions {
    sourceCompatibility(jvmVersion)
    targetCompatibility(jvmVersion)
}

fun CommonExtension<*, *, *, *, *>.defaultComposeStuff(withComposeCompilerVer: Ver? = Vers.ComposeCompiler) {
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = withComposeCompilerVer?.ver
    }
}

fun CommonExtension<*, *, *, *, *>.defaultPackagingOptions() = packaging {
    resources.excludes.defaultAndroExcludedResources()
}

/** Use template-andro/build.gradle.kts:fun defaultAndroLibPublishAllVariants() to create component with name "default". */
fun Project.defaultPublishingOfAndroLib(
    lib: LibDetails,
    componentName: String = "default",
) {
    afterEvaluate {
        extensions.configure<PublishingExtension> {
            publications.register<MavenPublication>(componentName) {
                from(components[componentName])
                defaultPOM(lib)
            }
        }
    }
}

fun Project.defaultPublishingOfAndroApp(
    lib: LibDetails,
    componentName: String = "release",
) = defaultPublishingOfAndroLib(lib, componentName)


// endregion [Andro Common Build Template]

// region [Andro Lib Build Template]

fun Project.defaultBuildTemplateForAndroidLib(
    libNamespace: String,
    jvmVersion: String = vers.JvmDefaultVer,
    sdkCompile: Int = vers.AndroSdkCompile,
    sdkMin: Int = vers.AndroSdkMin,
    withMDC: Boolean = false,
    details: LibDetails = rootExtLibDetails,
    publishVariant: String? = null, // null means disable publishing to maven repo
) {
    repositories { addRepos(details.settings.repos) }
    // temporary (before moving andro stuff to settings)
    val withCompose = details.settings.compose != null
    val withComposeCompilerVer = details.settings.compose?.withComposeCompilerVer
    extensions.configure<LibraryExtension> {
        defaultAndroLib(libNamespace, jvmVersion, sdkCompile, sdkMin, withCompose, withComposeCompilerVer)
        publishVariant?.let { defaultAndroLibPublishVariant(it) }
    }
    dependencies {
        defaultAndroDeps(withCompose = withCompose, withMDC = withMDC)
        defaultAndroTestDeps(withCompose = withCompose)
        add("debugImplementation", AndroidX.Tracing.ktx) // https://github.com/android/android-test/issues/1755
    }
    configurations.checkVerSync()
    tasks.defaultKotlinCompileOptions()
    defaultGroupAndVerAndDescription(details)
    publishVariant?.let {
        defaultPublishingOfAndroLib(details, it)
        defaultSigning()
    }
}

fun LibraryExtension.defaultAndroLib(
    libNamespace: String,
    jvmVersion: String = vers.JvmDefaultVer,
    sdkCompile: Int = vers.AndroSdkCompile,
    sdkMin: Int = vers.AndroSdkMin,
    withCompose: Boolean = false,
    withComposeCompilerVer: Ver? = Vers.ComposeCompiler,
) {
    compileSdk = sdkCompile
    defaultCompileOptions(jvmVersion)
    defaultDefaultConfig(libNamespace, sdkMin)
    defaultBuildTypes()
    if (withCompose) defaultComposeStuff(withComposeCompilerVer)
    defaultPackagingOptions()
}

fun LibraryExtension.defaultDefaultConfig(
    libNamespace: String,
    sdkMin: Int = vers.AndroSdkMin,
) = defaultConfig {
    namespace = libNamespace
    minSdk = sdkMin
    testInstrumentationRunner = vers.AndroTestRunner
}

fun LibraryExtension.defaultBuildTypes() = buildTypes { release { isMinifyEnabled = false } }

fun LibraryExtension.defaultAndroLibPublishVariant(
    variant: String = "debug",
    withSources: Boolean = true,
    withJavadoc: Boolean = false,
) {
    publishing {
        singleVariant(variant) {
            if (withSources) withSourcesJar()
            if (withJavadoc) withJavadocJar()
        }
    }
}

fun LibraryExtension.defaultAndroLibPublishAllVariants(
    withSources: Boolean = true,
    withJavadoc: Boolean = false,
) {
    publishing {
        multipleVariants {
            allVariants()
            if (withSources) withSourcesJar()
            if (withJavadoc) withJavadocJar()
        }
    }
}

// endregion [Andro Lib Build Template]
