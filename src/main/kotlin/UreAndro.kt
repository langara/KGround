package pl.mareklangiewicz.deps

import okio.FileSystem.Companion.RESOURCES
import okio.FileSystem.Companion.SYSTEM
import okio.Path
import okio.Path.Companion.toPath
import pl.mareklangiewicz.ure.*

private val appABTResPath = "andro-app-build-template".toPath()
private val libABTResPath = "andro-lib-build-template".toPath()

private val ureWithABT = ure {
    1 of ureWhateva().withName("before")
    1 of ureRegion(ureWhateva(), ir("Andro Build Template")).withName("abt")
    1 of ureWhateva(reluctant = false).withName("after")
}


private fun injectAndroBuildTemplate(inputResPath: Path, outputPath: Path) {
    val inputMR = RESOURCES.sourceMatchUre(inputResPath, ureWithABT) ?: error("No match $inputResPath")
    val abt by inputMR
    SYSTEM.processFile(outputPath, outputPath) { output ->
        val outputMR = ureWithABT.compile().matchEntire(output) ?: error("No match $outputPath")
        val before by outputMR
        val after by outputMR
        before + abt + after
    }
}

fun injectAndroAppBuildTemplate(outputPath: Path) = injectAndroBuildTemplate(appABTResPath, outputPath)
fun injectAndroLibBuildTemplate(outputPath: Path) = injectAndroBuildTemplate(libABTResPath, outputPath)

fun checkAndroBuildTemplates(vararg buildFiles: Path) {
    val appMR = RESOURCES.sourceMatchUre(appABTResPath, ureWithABT) ?: error("No match $appABTResPath")
    val libMR = RESOURCES.sourceMatchUre(libABTResPath, ureWithABT) ?: error("No match $libABTResPath")
    val appABT = appMR["abt"]
    val libABT = libMR["abt"]
    check(appABT == libABT) { "Templates in app and lib have to be the same! ${appABT.length} ${libABT.length}" }
    println("OK. Templates in template-android are the same.")
    for (file in buildFiles) {
        val abt by SYSTEM.sourceMatchUre(file, ureWithABT) ?: error("No match $file")
        check(abt == appABT) { "Template in $file was modified." }
        println("OK. Template in $file is correct.")
    }
    println("OK. Checked. All templates look good.")
}

// TODO NOW: scan imports and add imports from template-android/lib/build.gradle.kts if needed

