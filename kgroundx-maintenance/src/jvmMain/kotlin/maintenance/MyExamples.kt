@file:Suppress("unused")

package pl.mareklangiewicz.kgroundx.maintenance

import kotlinx.coroutines.flow.map
import okio.Path
import pl.mareklangiewicz.annotations.DelicateApi
import pl.mareklangiewicz.annotations.ExampleApi
import pl.mareklangiewicz.annotations.ExperimentalApi
import pl.mareklangiewicz.annotations.NotPortableApi
import pl.mareklangiewicz.bad.bad
import pl.mareklangiewicz.bad.chkEq
import pl.mareklangiewicz.kground.io.cd
import pl.mareklangiewicz.kground.io.localUFileSys
import pl.mareklangiewicz.kground.logEach
import pl.mareklangiewicz.kommand.*
import pl.mareklangiewicz.kommand.core.LsOpt
import pl.mareklangiewicz.kommand.core.ls
import pl.mareklangiewicz.kommand.zenity.zenityAskIf
import pl.mareklangiewicz.kommand.zenity.zenityShowWarning
import pl.mareklangiewicz.uctx.uctx
import pl.mareklangiewicz.udata.str
import pl.mareklangiewicz.ulog.hack.UHackySharedFlowLog
import pl.mareklangiewicz.ulog.i
import pl.mareklangiewicz.ulog.localULog
import pl.mareklangiewicz.ulog.w
import pl.mareklangiewicz.ure.*
import pl.mareklangiewicz.usubmit.localUSubmit
import pl.mareklangiewicz.usubmit.xd.*

@ExampleApi
object MyBasicExamples {

  /** Simple example of using KommandLine */
  suspend fun justSomeLS() {
    val log = localULog()
    log.w("Let's play with kground and kommand integration...")
    ls { -LsOpt.LongFormat; -LsOpt.All }.ax().logEach()
    cd("/home/marek/tmp") {
      ls { -LsOpt.LongFormat; -LsOpt.All }
        .ax()
        .logEach()
    }
  }

  suspend fun justSomeIdeDiff() {
    val log = localULog()
    log.w("Let's try some ideDiff...")
    ideDiff(
      PCodeKt / "KGround/template-full/build.gradle.kts",
      PCodeKt / "AbcdK/build.gradle.kts",
    ).ax()
  }

  suspend fun searchAllMyKotlinCode() = searchKotlinCodeInMyProjects(ureText("UReports"))

  private val PTemplateBasicJvmAppSrc = PProjKGround / "template-basic/template-basic-jvm-app/src"

  suspend fun disableBasicKtFile() = (PTemplateBasicJvmAppSrc / "main" / "kotlin/App.jvm.kt").myKotlinFileDisable()

  suspend fun enableBasicKtFile() = (PTemplateBasicJvmAppSrc / "mainDisabled" / "kotlin/App.jvm.kt").myKotlinFileEnable()
}


@OptIn(DelicateApi::class)
@ExampleApi
object MyTemplatesExamples {

  suspend fun tryDiffMyTemplates() {
    tryDiffMyConflictingTemplatesSrc()
  }

  suspend fun tryInjectOneProject() = tryInjectMyTemplatesToProject(PCodeKt / "AbcdK")

  suspend fun tryInjectToItSelf() = tryInjectMyTemplatesToProject(PCodeKt / "KGround")

  suspend fun tryInjectAllMyProjects() {
    tryToInjectMyTemplatesToAllMyProjects(onlyPublic = false, askInteractively = true)
  }
}

@ExampleApi
object MyWorkflowsExamples {

  suspend fun checkAllMDW() = checkMyDWorkflowsInMyProjects(onlyPublic = false)

  suspend fun injectMDWToMyProjects() = injectMyDWorkflowsToMyProjects(onlyPublic = false)

  suspend fun injectDWToExampleProject() = injectDWorkflowsToKotlinProject(projectName = "AbcdK")

  suspend fun injectGenerateDepsWToRefreshDepsRepo() = injectHackyGenerateDepsWorkflowToRefreshDepsRepo()

  suspend fun injectUpdateGeneratedDepsToDepsKtRepo() = injectUpdateGeneratedDepsWorkflowToDepsKtRepo()

  suspend fun updateKGroundTmplSymLinks() = updateKGroundTemplatesSymLinks()

  suspend fun updateGradlewInExampleProject() = updateGradlewFilesInKotlinProject(projectName = "AbcdK")

  suspend fun updateGradlewInMyProjects() = updateGradlewFilesInMyProjects(onlyPublic = false, skipReproducers = true)
}

@OptIn(ExperimentalApi::class)
@ExampleApi
object MyWeirdExamples {

  suspend fun tryToUseImplicitUSubmitAndULog() {
    val log = localULog()
    val submit = localUSubmit()

    submit.showInfo("Some info.")
    submit.showWarning("Some warning.")
    submit.showError("Some Error.")

    val answer = submit.askForAction("How do you feel?", "Fine", "Bad")
    log.w(answer)
    val entry = submit.askForEntry("How do you feel?", "Normal..or..")
    log.w(entry)
    val secret = submit.askForEntry("Tell me a secret", hidden = true)
    log.w(secret)
    val ok = submit.askIf("Everything fine?")
    log.w(ok)
  }

  suspend fun tryToUseAnotherUSubmitAndULog() {
    val log = UHackySharedFlowLog { level, data -> "ANOTHER L ${level.symbol} ${data.str(maxLength = 512)}" }
    val submit = ZenitySupervisor(promptPrefix = "ANOTHER ZENITY")
    uctx(submit + log) {
      tryToUseImplicitUSubmitAndULog()
    }
  }


  suspend fun tryToDiffMySettingsKtsFiles() {
    val log = localULog()
    val fs = localUFileSys()
    val pathLeft = PProjKGround / "settings.gradle.kts"
    fetchMyProjectsNameS(onlyPublic = false)
      .mapFilterLocalKotlinProjectsPathS()
      .collect {
        val pathRight = it / "settings.gradle.kts"
        if (fs.exists(pathRight)) {
          val msgDiff = "ideDiff(\n  \"$pathLeft\",\n  \"$pathRight\"\n)"
          log.i(msgDiff)
          val question = "Try opening diff in IDE?\n$msgDiff"
          val answer = zenityAskIf(question).ax()
          if (answer) ideDiff(pathLeft, pathRight).ax()
        }
        else zenityShowWarning("No settings file: $pathRight").ax()
        zenityAskIf("Continue diffing? (No -> cancel/abort/throw)").ax() || bad { "User cancelled" }
      }
  }

  /**
   * Example how I updated my repos origins after changing username on GitHub
   * Now it does nothing, because all repos have already set remote url to:
   * git@github.com:mareklangiewicz/<project>.git instead of one with "langara"
   * but let's leave it here as an example.
   */
  @OptIn(DelicateApi::class, NotPortableApi::class)
  suspend fun tryToUpdateMyReposOrigins() =
    fetchMyProjectsNameS(onlyPublic = false)
      .map { PCodeKt / it }
      .collect { tryToUpdateMyRepoOrigin(it) }

  @OptIn(DelicateApi::class, NotPortableApi::class)
  private suspend fun tryToUpdateMyRepoOrigin(repoDir: Path) = cd(repoDir) {
    val log = localULog()
    val ureRepoUrl = ure {
      +ureText("git@github.com:")
      +ureIdent().withName("user")
      +ch('/')
      +ureIdent(allowDashesInside = true).withName("project")
      +ureText(".git")
    }
    val kget = kommand("git", "remote", "get-url", "origin")
    fun kset(url: String) = kommand("git", "remote", "set-url", "origin", url)

    log.i(repoDir)
    val repoUrl = kget.ax().single()
    log.i(repoUrl)
    val result = ureRepoUrl.matchEntireOrThrow(repoUrl)
    val vals = result.namedValues
    val user = vals["user"]!!
    val project = vals["project"]!!
    if (user == "mareklangiewicz") {
      log.i("User is already mareklangiewicz.")
      return@cd
    }
    user chkEq "langara"
    val newUrl = "git@github.com:mareklangiewicz/$project.git"
    log.w("setting origin -> $newUrl")
    kset(newUrl).ax()
  }
}
